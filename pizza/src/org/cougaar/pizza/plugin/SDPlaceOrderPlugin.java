/*
 * <copyright>
 *
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * </copyright>
 */
package org.cougaar.pizza.plugin;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.pizza.Constants;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.Entity;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.SubTaskResult;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.Filters;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.TimeSpans;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * This Plugin manages ordering pizzas at customer agents.
 */
public class SDPlaceOrderPlugin extends PlaceOrderPlugin {
  private IncrementalSubscription disposedFindProvidersSub;
  private IncrementalSubscription expansionSub;
  private Subscription taskSub;

  /**
   * Create our blackboard subscriptions.
   */
  protected void setupSubscriptions() {
    super.setupSubscriptions();
    disposedFindProvidersSub = (IncrementalSubscription) blackboard.subscribe(FIND_PROVIDERS_DISPOSITION_PRED);
    expansionSub = (IncrementalSubscription) blackboard.subscribe(EXPANSION_PRED);
    taskSub = blackboard.subscribe(TASK_PRED);
  }

  protected void execute() {
    // Create pizza orders when we find a new PizzaPreference object on our getBlackboardService().
    // The Pizza Preference object serves as a repository for party invitation responses.
    for (Iterator iterator = pizzaPrefSub.getAddedCollection().iterator(); iterator.hasNext();) {
      PizzaPreferences pizzaPrefs = (PizzaPreferences) iterator.next();
      if (logger.isDebugEnabled()) {
        logger.debug(" found pizzaPrefs " + pizzaPrefSub);
      }
      publishFindProvidersTask(null);
      Task orderTask = makeTask(Constants.Verbs.ORDER, planningFactory.createInstance(Constants.PIZZA));
      getBlackboardService().publishAdd(orderTask);
      Collection subtasks = makeSubtasks(pizzaPrefs, orderTask);
      makeExpansionAndPublish(orderTask, subtasks);
      allocateTasks(subtasks, null);
    }

    // Check for updates on our findProviders tasks. Once this task is successfully disposed, we
    // should have new provider relationships.
    for (Iterator i = disposedFindProvidersSub.getAddedCollection().iterator(); i.hasNext();) {
      Disposition disposition = (Disposition) i.next();
      if (disposition.isSuccess() && disposition.getEstimatedResult().getConfidenceRating() == 1.0) {
        Collection tasks = getUnallocatedTasks();
        if (!tasks.isEmpty()) {
          // check the find providers task for exclusions
          Entity exclusion = checkFindProvidersTask(disposition.getTask());
          allocateTasks(getUnallocatedTasks(), exclusion);
        }
      }
    }

    // Check our allocation subscription for changes.  Changes usually indicate that we have received a
    // new allocation result from our provider.  These results should be checked and then copied to
    // the estimated allocation result slot so that the results can continue to flow up the planelement chain.
    if (!allocationSub.getChangedCollection().isEmpty()) {
      for (Iterator i = allocationSub.iterator(); i.hasNext();) {
        PlanElement pe = (PlanElement) i.next();
        if (PluginHelper.updatePlanElement(pe)) {
          getBlackboardService().publishChange(pe);
        }
      }
    }

    // Check the results on the pizza order expansion.  If it is failed, find a new provider and replan
    // our pizza order tasks.
    if (!expansionSub.getChangedCollection().isEmpty()) {
      for (Iterator i = expansionSub.iterator(); i.hasNext();) {
        Expansion exp = (Expansion) i.next();
        if (!exp.getReportedResult().isSuccess()) {
          processFailedExpansion(exp);
        }
      }
    }
  }

  /**
   * Create the Find Providers task that is used by service discovery to find our pizza providers.
   *
   * @param excludePhrase A Prepositional Phrase that tells the service discovery mechanism that we need a provider
   *                      other than the one listed in the phrase.
   */
  private void publishFindProvidersTask(PrepositionalPhrase excludePhrase) {
    NewTask newTask = makeTask(Constants.Verbs.FIND_PROVIDERS, getSelfEntity());
    Vector prepPhrases = new Vector();
    NewPrepositionalPhrase pp = planningFactory.newPrepositionalPhrase();
    pp.setPreposition(org.cougaar.planning.Constants.Preposition.AS);
    pp.setIndirectObject(Constants.Roles.PIZZAPROVIDER);
    prepPhrases.add(pp);
    if (excludePhrase != null) {
      prepPhrases.add(excludePhrase);
    }
    newTask.setPrepositionalPhrases(prepPhrases.elements());
    getBlackboardService().publishAdd(newTask);
  }

  /**
   * Allocate our pizza order tasks to a pizza provider.
   *
   * @param tasks           The tasks to send to the pizza provider.
   * @param excludeProvider A provider that has previously failed our pizza order.  This parameter can be null if we
   *                        don't have any providers that we need to exclude from our provider list.
   */
  private void allocateTasks(Collection tasks, Entity excludeProvider) {
    Entity provider = getProvider(excludeProvider);
    if (provider != null) {
      for (Iterator i = tasks.iterator(); i.hasNext();) {
        Task newTask = (Task) i.next();
        AllocationResult ar = PluginHelper.createEstimatedAllocationResult(newTask, planningFactory, 1.0, true);
        Allocation alloc = planningFactory.createAllocation(newTask.getPlan(), newTask, (Asset) provider, ar,
                                                            Constants.Roles.PIZZAPROVIDER);
        getBlackboardService().publishAdd(alloc);
        if (logger.isDebugEnabled()) {
          logger.debug(" allocating task " + newTask);
        }
      }
    }
  }

  /**
   * Find the pizza provider entities that we can send our pizza orders to.
   *
   * @return A collection of pizza providers to use.
   */
  public Entity getProvider(Entity excludeProvider) {
    TimeSpan timeSpan = TimeSpans.getSpan(TimeSpan.MIN_VALUE, TimeSpan.MAX_VALUE);
    RelationshipSchedule relSched = getSelfEntity().getRelationshipSchedule();
    Collection relationships = relSched.getMatchingRelationships(Constants.Roles.PIZZAPROVIDER, timeSpan);
    Entity provider = null;
    for (Iterator iterator = relationships.iterator(); iterator.hasNext();) {
      Relationship r = (Relationship) iterator.next();
      provider = (Entity) relSched.getOther(r);
      if (provider.equals(excludeProvider))
        continue;
      break;
    }
    return provider;
  }

  /**
   * If our order task fails because one or both of our pizza order subtasks fail, request a new provider from service
   * discovery that is not the provider that failed our task(s).  Also, rescind the orders that we placed with that
   * provider so that we can order all of our pizza from a single provider.
   *
   * @param exp The order task expansion that contains our pizza orders.
   */
  private void processFailedExpansion(Expansion exp) {
    // Find the supplier to that failed and rescind the allocations
    Asset failedSupplier = null;
    for (Iterator resultsIt = exp.getWorkflow().getSubtaskResults().iterator(); resultsIt.hasNext();) {
      SubTaskResult result = (SubTaskResult) resultsIt.next();
      if (!result.getAllocationResult().isSuccess()) {
        failedSupplier = ((Allocation) result.getTask().getPlanElement()).getAsset();
      }
      //rescind all task allocations since we will replan with a new provider.
      //Note we are rescinding the successful meat pizza order because we want our entire order to
      //be delivered by one pizza provider.
      getBlackboardService().publishRemove(result.getTask().getPlanElement());
    }
    NewPrepositionalPhrase excludePP = planningFactory.newPrepositionalPhrase();
    excludePP.setPreposition(Constants.Prepositions.NOT);
    excludePP.setIndirectObject(failedSupplier);
    publishFindProvidersTask(excludePP);
  }

  /**
   * Look at the FindProviders task to see if it has the exclusion prepositional phrase that alerts us not to use that
   * provider. Presumably that provider has been unable to satisfy previous pizza orders.
   *
   * @param findProvidersTask The task to look at.
   * @return The entity (provider) that we wish to exclude.  Note that this value can be null if the find providers task
   *         did not contain the special exclude phrase.
   */
  private Entity checkFindProvidersTask(Task findProvidersTask) {
    Entity excludedEntity = null;
    PrepositionalPhrase notPP = findProvidersTask.getPrepositionalPhrase(Constants.Prepositions.NOT);
    if (notPP != null) {
      excludedEntity = (Entity) notPP.getIndirectObject();
    }
    return excludedEntity;
  }

  /**
   * This method filters the task subscription for any tasks that are not disposed of or allocated. Tasks might be
   * unallocated to a provider if we were waiting for a provider or we rescinded our orders from a provider that could
   * not fill our order.
   *
   * @return A collection of tasks that are ready to be allocated.
   */
  private Collection getUnallocatedTasks() {
    return Filters.filter((Collection) taskSub, UNDISPOSED_TASKS);
  }

  /**
   * A predicate that matches dispositions of "FindProviders" tasks
   */
  private static UnaryPredicate FIND_PROVIDERS_DISPOSITION_PRED = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Disposition) {
        Task task = ((Disposition) o).getTask();
        return task.getVerb().equals(Constants.Verbs.FIND_PROVIDERS);
      }
      return false;
    }
  };

  /**
   * This predicate matches tasks that have no plan element.
   */
  private static UnaryPredicate UNDISPOSED_TASKS = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        return (task.getPlanElement() == null);
      }
      return false;
    }
  };

  /**
   * This predicate matches expansions of "Order" tasks.
   */
  private static UnaryPredicate EXPANSION_PRED = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Expansion) {
        Task task = ((Expansion) o).getTask();
        return task.getVerb().equals(Constants.Verbs.ORDER);
      }
      return false;
    }
  };

  /**
   * This predicate matches "Order" tasks.
   */
  private static UnaryPredicate TASK_PRED = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        return (task.getVerb().equals(Constants.Verbs.ORDER));
      }
      return false;
    }
  };
}
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
import org.cougaar.util.TimeSpan;
import org.cougaar.util.TimeSpans;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * This Plugin manages ordering pizzas at customer agents. It extends the PlaceOrderPlugin adding a request to Service
 * Discover to find pizza providers.
 * <p/>
 * The SDPlaceOrderPlugin publishes a Find Providers task with the role of PizzaProvider.  This task will be handled by
 * Service Discovery.  Once completed, Service Discovery will add a Dispostion PlanElement on the Find Providers task
 * indicating that a provider(s) was found.  The plugin can now allocate Order pizza tasks to a provider.
 * <p/>
 * The plugin monitors the AllocationResults on the allocated tasks and determines if the Order tasks were successfully
 * completed.  If an allocation is not successful, the plugin will issue a new request to find another pizza provider.
 * To avoid getting the original provider that was unable to satisfy the pizza order, the plugin adds a prepositional
 * phrase of "Not" with the name of the provider to exclude as the indirect object on the Find Providers task.
 */
public class SDPlaceOrderPlugin extends PlaceOrderPlugin {
  private IncrementalSubscription disposedFindProvidersSub;
  private IncrementalSubscription expansionSub;
  private IncrementalSubscription unallocatedTaskSub;

  /**
   * Create our blackboard subscriptions.
   */
  protected void setupSubscriptions() {
    super.setupSubscriptions();
    disposedFindProvidersSub = (IncrementalSubscription) blackboard.subscribe(DISPOSED_FIND_PROVIDERS_PRED);
    expansionSub = (IncrementalSubscription) blackboard.subscribe(EXPANSION_PRED);
    unallocatedTaskSub = (IncrementalSubscription) blackboard.subscribe(UNALLOCATED_TASK_PRED);
  }

  protected void execute() {
    // Create pizza orders when we find a new PizzaPreference object on our getBlackboardService().
    // The Pizza Preference object contains the party invitation responses.
    for (Iterator iterator = pizzaPrefSub.getAddedCollection().iterator(); iterator.hasNext();) {
      PizzaPreferences pizzaPrefs = (PizzaPreferences) iterator.next();
      if (logger.isDebugEnabled()) {
        logger.debug(" found pizzaPrefs " + pizzaPrefSub);
      }
      publishFindProvidersTask(null);
      Task orderTask = makeTask(Constants.Verbs.ORDER, makePizzaAsset(Constants.PIZZA));
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
        if (!unallocatedTaskSub.isEmpty()) {
          // check the find providers task for providers to be excluded
          Entity excludeProvider = checkFindProvidersTask(disposition.getTask());
          allocateTasks(unallocatedTaskSub, excludeProvider);
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
   * Create and plublish the Find Providers task that is used by service discovery to find pizza providers.
   *
   * @param excludePhrase a Prepositional Phrase that tells the service discovery to exclude the given provider
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
   * @param tasks           tasks to send to the pizza provider.
   * @param excludeProvider a provider that has previously failed a pizza order.  This parameter can be null if we don't
   *                        have any providers that we need to exclude from our provider list.
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
   * Find a pizza provider entity that we can send our pizza orders to.
   *
   * @return entity representing a pizza provider
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
   * If one or both of pizza order subtasks fail, request a new provider from service discovery that is not the provider
   * that failed our task(s).  Rescind the both pizza orders that we placed with that provider so that we can order all
   * of our pizza from a single provider.
   *
   * @param exp order task expansion that contains our pizza orders.
   */
  private void processFailedExpansion(Expansion exp) {
    // Find the provider that failed and rescind the allocations
    Asset failedProvider = null;
    for (Iterator resultsIt = exp.getWorkflow().getSubtaskResults().iterator(); resultsIt.hasNext();) {
      SubTaskResult result = (SubTaskResult) resultsIt.next();
      if (!result.getAllocationResult().isSuccess()) {
        failedProvider = ((Allocation) result.getTask().getPlanElement()).getAsset();
      }
      //rescind all task allocations since we will replan with a new provider.
      //Note we are rescinding the successful meat pizza order because we want our entire order to
      //be delivered by one pizza provider.
      getBlackboardService().publishRemove(result.getTask().getPlanElement());
    }
    NewPrepositionalPhrase excludePP = planningFactory.newPrepositionalPhrase();
    excludePP.setPreposition(Constants.Prepositions.NOT);
    excludePP.setIndirectObject(failedProvider);
    publishFindProvidersTask(excludePP);
  }

  /**
   * Check the FindProviders task to see if it has the "Not" prepositional indicating the provider to exclude.
   * Presumably this provider is unable to satisfy previous pizza orders.
   *
   * @param findProvidersTask the task to look at.
   * @return The entity (provider) to exclude.  Will return null if the find providers task does not contain the "Not"
   *         preposition (exclude phrase).
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
   * A predicate that matches a "FindProviders" task with a Disposition PlanElement.
   */
  private static UnaryPredicate DISPOSED_FIND_PROVIDERS_PRED = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Disposition) {
        Task task = ((Disposition) o).getTask();
        return task.getVerb().equals(Constants.Verbs.FIND_PROVIDERS);
      }
      return false;
    }
  };

  /**
   * This predicate matches an "Order" task with an Expansion PlanElement.
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
   * This predicate matches an "Order" task without a PlanElement.  There are three types of PlanElements: Expansions,
   * Allocations and Dispostions -- see the CDG for more details.  This plugin will not allocate tasks(add an Allocation
   * PlanElement) until it has found a provider.
   */
  private static UnaryPredicate UNALLOCATED_TASK_PRED = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        return (task.getVerb().equals(Constants.Verbs.ORDER) && task.getPlanElement() == null);
      }
      return false;
    }
  };
}
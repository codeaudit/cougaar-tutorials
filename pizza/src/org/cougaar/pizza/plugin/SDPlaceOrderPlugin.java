/*
 * <copyright>
 *
 *  Copyright 2002-2004 BBNT Solutions, LLC
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
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.SubTaskResult;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.Filters;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * The SDPlaceOrderPlugin extends the PlaceOrderPlugin.  The plugin uses Service Discovery
 * to find pizza providers dynamically. Once the plugin receives the PizzaPreferences
 * object, it publishes a FindProviders task with a Role of PizzaProvider. This task
 * will be handled by Service Discovery.  The plugin then creates and expands the order
 * task as done in the super class.  However, unlike the super class, this plugin waits
 * until Servivce Discovery has finished finding a pizza provider and has added a
 * Disposition on the FindProviders task before it can allocate the pizza subtasks.
 *
 * If the Expansion on the order task fails, i.e., the provider could not complete the
 * pizza order, the plugin will remove the Allocations on the subtasks of the Expansion and
 * request a new provider from Service Discovery.  To exclude the provider that
 * previously failed, the plugin adds a prepositional phrase of "Not" with the name of the
 * provider as the indirect object on the FindProviders task.  Once a new provider is
 * found the tasks will be reallocated.
 */
public class SDPlaceOrderPlugin extends PlaceOrderPlugin {
  // Subscription to Dispositions on FindProviders tasks
  private IncrementalSubscription dispositionSub;
  // Subscription to Order tasks, entire collection not just incremental changes
  private Subscription taskSub;

  /**
   * Overrides the super class method. Adds additional subscriptions.
   */
  protected void setupSubscriptions() {
    // Initialize subscriptions in the super class.n
    super.setupSubscriptions();
    // Set up additional subscriptions.
    dispositionSub = (IncrementalSubscription) blackboard.subscribe(DISPOSITION_PRED);
    expansionSub = (IncrementalSubscription) blackboard.subscribe(EXPANSION_PRED);
    taskSub = blackboard.subscribe(TASK_PRED);
  }

  /**
   * Execute is called when there are changes to the plugins's subscriptions.
   */
  protected void execute() {
    // The PizzaPreferences object contains the party invitation responses.
    PizzaPreferences pizzaPrefs = getPizzaPreferences();
    if ( pizzaPrefs != null) {
      // Use service discovery to find a provider, null means do not exclude any providers
      publishFindProvidersTask(null);
      // Create the parent order task.
      Task orderTask = createOrderTask();
      // Create a subtask for each pizza preference.
      Collection pizzaSubtasks = createPizzaSubtasks(pizzaPrefs, orderTask);
      // Expand the order task and add the subtasks to the workflow.
      makeExpansion(orderTask, pizzaSubtasks);
    }
    // Service Discovery adds a Disposition to the FindProviders task when it is done.
    Disposition disposition = getDisposedFindProvider();
    if (disposition != null) {
      /**
       * Overrides super method.  Uses the Disposition to get to the FindProviders task to
       * check for excluded providers.  An exluded provider is one that was previously
       * unable to complete an order task.
       */
      Entity pizzaProvider = getProvider(disposition);
      if (pizzaProvider != null) {
        // Allocate all pizza subtasks to a single pizza provider.
        allocateSubtasks(getUnallocatedSubtasks(), pizzaProvider);
      }
    }

    // Update changes to the results of the allocation.
    updateOrderAllocationResults();
    // Check for changes on the Expansion
    Expansion exp = getChangedExpansion();
    if (exp != null) {
      if (logger.isDebugEnabled()) {
        logger.debug(" Change received on the expansion " + printExpansionResults(exp));
      }
      if (! exp.getReportedResult().isSuccess() ) {
        /**
         * Publish remove the Allocations on all subtasks in the Expansion and return the
         * provider that failed.  The subtasks will get reallocated when a new provider is
         * found.
         */
        Entity failedProvider = processFailedSubtasks(exp);
        // Request a new provider from Service Discovery, excluding the one that failed.
        publishFindProvidersTask(failedProvider);
	logger.shout("Initial Expansion FAILed. Redo Service Discovery.");
      } else {
        // Expansion isSuccess is true and is completed
        if (exp.getReportedResult().getConfidenceRating() == 1.0) {
	  logger.shout("SUCCESS ORDER Task planned: 2 Meat and 2 Veggie Pizzas from Dominos. Ready for the party!");
        }
      }
    }
  }

  /**
   * Creates and publishes a FindProviders task.  The Task is created with the Verb
   * FindProviders and the direct object is set to the agent's self entity.  The type of
   * provider is defined by creating an "As" Preposition with the indirect object set to a
   * PizzaProvider Role.  If the failedProvider is not null, a "Not" Preposition is also
   * created and the indirect object is set to the failed provider asset.  The "Not"
   * Preposition represents a provider to be excluded from Service Discovery.  The
   * task is then published to the blackboard.
   *
   * @param failedProvider the provider to exclude in service discovery.  Can be null if
   * there is no provider to exclude.
   */
  private void publishFindProvidersTask(Asset failedProvider) {
    NewTask newTask = makeTask(Constants.Verbs.FIND_PROVIDERS, getSelfEntity());
    Vector prepPhrases = new Vector();
    NewPrepositionalPhrase pp = planningFactory.newPrepositionalPhrase();
    pp.setPreposition(org.cougaar.planning.Constants.Preposition.AS);
    pp.setIndirectObject(Constants.Roles.PIZZAPROVIDER);
    prepPhrases.add(pp);
    if (failedProvider != null) {
      NewPrepositionalPhrase excludePhrase = planningFactory.newPrepositionalPhrase();
      excludePhrase.setPreposition(Constants.Prepositions.NOT);
      excludePhrase.setIndirectObject(failedProvider);
      prepPhrases.add(excludePhrase);
    }
    newTask.setPrepositionalPhrases(prepPhrases.elements());
    getBlackboardService().publishAdd(newTask);
  }

  /**
   * Returns a provider Entity.  First, retrieve the excluded provider from the
   * FindProviders task of the Disposition.  Get all relationships that match the Role
   * of PizzaProvider from the RelationshipSchedule. Return the first provider found that
   * is not the excluded provider.  Returns null if a provider is not found.
   * @param disposition
   * @return a pizza provider Entity
   */
  protected Entity getProvider(Disposition disposition) {
    // Get the excluded provider from the FindProviders task of the disposition.
    Entity excludeProvider = getExcludedProvider(disposition.getTask());
    // Get the RelationshipSchedule for this agent.
    RelationshipSchedule relSched = getSelfEntity().getRelationshipSchedule();
    // Find all Relationships matching the Role of PizzaProvider.
    Collection relationships = relSched.getMatchingRelationships(Constants.Roles.PIZZAPROVIDER);
    Entity provider = null;
    for (Iterator iterator = relationships.iterator(); iterator.hasNext();) {
      Relationship r = (Relationship) iterator.next();
      provider = (Entity) relSched.getOther(r);
      // Return the first provider found that is not the excluded provider
      if (! provider.equals(excludeProvider))  {
        return provider;
      }
    }
    return null;
  }

  /**
   * Returns a successful Disposition from the dispostion subscription.  Checks the added
   * collection on the subscription and returns a successful Disposition.  Returns null if the
   * collection is empty or the disposition was not successful.
   * @return a Disposition
   */
  private Disposition getDisposedFindProvider() {
    for (Iterator i = dispositionSub.getAddedCollection().iterator(); i.hasNext();) {
      Disposition disposition = (Disposition) i.next();
      if (disposition.isSuccess() && disposition.getEstimatedResult().getConfidenceRating() == 1.0) {
        return disposition;
      }
    }
    return null;
  }

  /**
   * Returns a collection of tasks that do not have PlanElements.  Filters the
   * task subscription by applying the tasks with the no PlanElement Predicate.  When this
   * Filter is applied, the assumption is that the only tasks without PlanElements are the
   * subtasks that need to be allocated.
   *
   * @return a collection of tasks that do not have Allocation PlanElements.
   */
  private Collection getUnallocatedSubtasks() {
     return Filters.filter((Collection) taskSub, TASK_NO_PE_PRED);
  }

  /**
   * Processes the subtasks of the failed Expansion and returns the provider that failed.
   * If any of the subtasks in the expansion are failed by the provider, the subtask
   * Allocations are published removed so that they can be reallocated to a new provider.
   * Returns the provider that failed.
   *
   * @param exp order task expansion that contains our pizza orders.
   */
  private Entity processFailedSubtasks(Expansion exp) {
    Asset failedProvider = null;
    for (Iterator resultsIt = exp.getWorkflow().getSubtaskResults().iterator(); resultsIt.hasNext();) {
      SubTaskResult result = (SubTaskResult) resultsIt.next();
      // Grab the provider to be excluded
      failedProvider = ((Allocation) result.getTask().getPlanElement()).getAsset();
      // Rescind all task allocations
      getBlackboardService().publishRemove(result.getTask().getPlanElement());
    }
    return (Entity) failedProvider;
  }

  /**
   * Returns the excluded provider Entity.  Looks for the "Not" Preposition on the
   * FindProviders task and gets the indirect object which is the provider entity.
   * Presumably this provider is unable to satisfy previous pizza orders. Returns null if
   * the FindProviders task does not contain the "Not" Preposition.
   *
   * @param findProvidersTask the task to look at.
   * @return The entity (provider) to exclude.
   */
  private Entity getExcludedProvider(Task findProvidersTask) {
    Entity excludedEntity = null;
    PrepositionalPhrase notPP = findProvidersTask.getPrepositionalPhrase(Constants.Prepositions.NOT);
    if (notPP != null) {
      excludedEntity = (Entity) notPP.getIndirectObject();
    }
    return excludedEntity;
  }

  /**
   * This predicate matches Dispositions on FindProviders tasks.
   */
  private static final UnaryPredicate DISPOSITION_PRED = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Disposition) {
        Task task = ((Disposition) o).getTask();
        return task.getVerb().equals(Constants.Verbs.FIND_PROVIDERS);
      }
      return false;
    }
  };

  /**
   * This predicate matches Order tasks.
   */
  private static final UnaryPredicate TASK_PRED = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        return (task.getVerb().equals(Constants.Verbs.ORDER));
      }
      return false;
    }
  };

  /**
   * This predicate matches tasks without PlanElements.  It is used on a local collection
   * of order tasks.  It should be noted that this predicate is testing a mutable attribute
   * on a task and is therefore not recommended for use with blackboard subscriptions.
   */
  private static final UnaryPredicate TASK_NO_PE_PRED = new UnaryPredicate() {
    public boolean execute(Object o) {
        return (((Task) o).getPlanElement() == null);
    }
  };
}

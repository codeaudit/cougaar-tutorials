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
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.asset.PizzaAsset;
import org.cougaar.pizza.asset.PropertyGroupFactory;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.Entity;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * This plugin orders pizza for a pizza party. It subscribes to a PizzaPreferences object
 * that is published by the InvitePlugin.  The PizzaPreferences object contains the types
 * of pizzas to order and the number of individuals requesting each pizza type.  After the
 * plugin receives a PizzaPreferences object it creates and publishes a Task with the Verb
 * "Order" and a direct object of type Pizza (Asset).  Next, it expands the order task
 * into a workflow of two subtasks, one subtask per type of pizza with a quantity
 * Preference for the number of requests.  To place the order, it allocates the substasks
 * to its pizza provider, Joes Local Pizza Shack.  This customer/provider relationship is
 * defined in the XML configuration files and is established when the agents start up. See
 * PizzaNode1.xml and PizzaNode2.xml for details.
 *
 * In this example, the plugin cannot successfully complete the pizza order because Joe's
 * Local Pizza Shack doesn't make veggie pizzas.  The final result is a failed Expansion
 * on the parent order Task due to the failed Allocation of the veggie pizza subtask.
 */
public class PlaceOrderPlugin extends ComponentPlugin {
  // Logger for the plugin
  protected LoggingService logger;
  // DomainService is needed to get the Planning Factory
  protected DomainService domainService;
  // PlanningFactory is needed to create tasks and task-related components
  protected PlanningFactory planningFactory;

  // Subscription to this agent's Entity
  protected IncrementalSubscription selfSub;
  // Subscription to PizzaPreferences
  protected IncrementalSubscription pizzaPrefSub;
  // Subscription to Allocations on pizza order tasks
  protected IncrementalSubscription allocationSub;
  // Subscription to Expansions on order tasks
  protected IncrementalSubscription expansionSub;

  /**
   * Services that are absolutely required by the plugin can be loaded via introspection
   * by the binding utility instead of explicitly getting each service from the
   * ServiceBroker in load(). The setter methods are called after the component is
   * constructed but before the state methods such as initialize, load,
   * setupSubscriptions, etc. If the service is not available at that time, the component
   * will be unloaded.
   */
  public void setDomainService(DomainService aDomainService) {
    domainService = aDomainService;
  }

  /**
   * Loads services used by the plugin.
   */
  public void load() {
    super.load();
    // ServiceBroker handles the getting and releasing of services.
    ServiceBroker sb = getServiceBroker();
    logger = (LoggingService) sb.getService(this, LoggingService.class, null);
    planningFactory = (PlanningFactory)
        domainService.getFactory(org.cougaar.planning.ldm.PlanningDomain.PLANNING_NAME);
    // No longer need the DomainService, release it.
    sb.releaseService(this, DomainService.class, domainService);
  }

  /**
   * Initialize the subcriptions the plugin is interested in.
   */
  protected void setupSubscriptions() {
    selfSub = (IncrementalSubscription) getBlackboardService().subscribe(SELF_PRED);
    pizzaPrefSub = (IncrementalSubscription) getBlackboardService().subscribe(PIZZA_PREF_PRED);
    allocationSub = (IncrementalSubscription) getBlackboardService().subscribe(ALLOCATION_PRED);
    expansionSub = (IncrementalSubscription) blackboard.subscribe(EXPANSION_PRED);
  }

  /**
   * Execute is called when there are changes to the plugins's subscriptions.
   */
  protected void execute() {
    // The PizzaPreferences object contains the party invitation responses.
    PizzaPreferences pizzaPrefs = getPizzaPreferences();
    if ( pizzaPrefs != null) {
      // Create the parent order task.
      Task orderTask = createOrderTask();
      // Create a subtask for each pizza preference.
      Collection pizzaSubtasks = createPizzaSubtasks(pizzaPrefs, orderTask);
      // Expand the order task and add the subtasks to the workflow.
      makeExpansion(orderTask, pizzaSubtasks);
      // Allocate all pizza subtasks to the agent's pizza provider.
      allocateSubtasks(pizzaSubtasks, getProvider());
    }

    // Update changes to the results of the allocation.
    updateOrderAllocationResults();
    // Check for changes on the Expansion
    Expansion exp = getChangedExpansion();
    if (exp != null) {
      if (logger.isDebugEnabled()) {
        logger.debug(" Change received on the expansion " + printExpansionResults(exp));
      }
      // If the Expansion was not successful print party order results
      if (exp.getReportedResult().isSuccess() == false
          && exp.getReportedResult().getConfidenceRating() == 1.0) {
        logger.shout(printPartyOrderResults());
      }
    }
  }

  /**
   * Returns a PizzaPreferences object from the PizzaPreferences Subscription.  Checks the
   * added collection on the subscription and returns the first element in the
   * collection.  Will return null if the added collection is empty.
   *
   * @return a PizzaPrefereneces object from the subscription
   */
  protected PizzaPreferences getPizzaPreferences() {
    for (Iterator i = pizzaPrefSub.getAddedCollection().iterator(); i.hasNext();) {
      if (logger.isDebugEnabled()) {
        logger.debug(" found pizzaPrefs " + pizzaPrefSub);
      }
      // This plugin expects only one PizzaPreferences object
      return (PizzaPreferences) i.next();
    }
    return null;
  }

  /**
   * Returns a Task for ordering pizza.  Creates a Task with Verb "Order", makes a Pizza
   * asset and sets it as the direct object of the task. Next, it publishes the task to
   * the blackboard.
   *
   * @return a task for ordering pizza
   */
  protected Task createOrderTask() {
    Task orderTask = makeTask(Constants.Verbs.ORDER, makePizzaAsset(Constants.PIZZA));
    getBlackboardService().publishAdd(orderTask);
    return orderTask;
  }

  /**
   * Returns a Collection of subtasks for ordering meat and veggie pizzas.  Creates a meat
   * pizza subtask and a veggie pizza subtask.  The number of individuals requesting meat
   * and veggie pizzas are obtained from the PizzaPreferences object.  A quantity
   * Preference is created to represent the number of requests and is added to the
   * appropriate subtask. The parentTask is set as the parent of the subtasks.
   *
   * @param pizzaPrefs contains the number of people requesting meat or veggie pizzas
   * @param parentTask the parent of the subtasks
   * @return a Collection of pizza subtasks
   */
  protected Collection createPizzaSubtasks(PizzaPreferences pizzaPrefs, Task parentTask) {
    ArrayList subtasks = new ArrayList(2);
    // Create the meat pizza subtask
    PizzaAsset meatPizza = makePizzaAsset(Constants.MEAT_PIZZA);
    // Add meat properties to the pizza
    meatPizza.addOtherPropertyGroup(PropertyGroupFactory.newMeatPG());
    NewTask meatPizzaTask = makeTask(Constants.Verbs.ORDER, meatPizza);
    // Make a quantity Preference to represent the number of requests for meat pizza
    Preference meatPref = makeQuantityPreference(pizzaPrefs.getNumMeat());
    meatPizzaTask.setPreference(meatPref);
    meatPizzaTask.setParentTask(parentTask);
    subtasks.add(meatPizzaTask);

    // Create the veggie pizza subtask
    PizzaAsset veggiePizza = makePizzaAsset(Constants.VEGGIE_PIZZA);
    // Add veggie properties to the pizza
    veggiePizza.addOtherPropertyGroup(PropertyGroupFactory.newVeggiePG());
    NewTask veggiePizzaTask = makeTask(Constants.Verbs.ORDER, veggiePizza);
    // Make a quantity Preference to represent the number of requests for veggie pizza
    Preference veggiePref = makeQuantityPreference(pizzaPrefs.getNumVeg());
    veggiePizzaTask.setPreference(veggiePref);
    veggiePizzaTask.setParentTask(parentTask);
    subtasks.add(veggiePizzaTask);
    return subtasks;
  }

  /**
   * Creates and adds an Expansion on a Task and publishes it to the blackboard. An
   * Expansion is created containing a Workflow of the specified subtasks.  This Expansion
   * is set to the parent task. The subtasks are published to the blackboard first, then
   * the Expansion is published.
   *
   * @param parentTask the parent task to be expanded
   * @param subtasks   the collection of subtasks to be added to the Workflow of Expansion
   */
  protected void makeExpansion(Task parentTask, Collection subtasks) {
    /**
     * This helper method creates the Expansion, adds the subtasks to the Workflow, sets
     * the parent task, and sets the estimated AllocationResult to null.
     */
    Expansion expansion = PluginHelper.wireExpansion(parentTask, new Vector(subtasks), planningFactory);
    // Logical order of publish:  tasks first, then the expansion that contains them.
    for (Iterator iterator = subtasks.iterator(); iterator.hasNext();) {
      Task subtask = (Task) iterator.next();
      getBlackboardService().publishAdd(subtask);
    }
    getBlackboardService().publishAdd(expansion);
    if (logger.isDebugEnabled()) {
      logger.debug(" publishing subtasks and expansion ");
    }
  }

  /**
   * Creates and publishes Allocations for each subtask in the collection.  The Allocation
   * includes: the provider assigned to compelete the task, an estimated AllocationResult,
   * and the Role of PizzaProvider.  The estimated AllocationResult includes: an
   * estimated confidence rating of 0.25 and success is true. Valid range for the
   * confidence rating is between 0 and 1. The assumption is, as the subtasks are
   * completed, the allocation results will become closer to 1.  The Allocations are then
   * published to the blackboard.
   *
   * @param subtasks a collection of subtasks to be allocated
   * @param provider the provider to which the subtasks are allocated
   */
  protected void allocateSubtasks(Collection subtasks, Entity provider) {
    if (provider == null) {
      if (logger.isErrorEnabled()) {
        logger.error(" Provider is null, check config files for provider relationship");
      }
      return;
    }
    for (Iterator iter = subtasks.iterator(); iter.hasNext();) {
      Task task = (Task) iter.next();
      AllocationResult ar = PluginHelper.createEstimatedAllocationResult(task, planningFactory,
                                                                         0.25, true);
      Allocation alloc = planningFactory.createAllocation(task.getPlan(), task, (Asset) provider,
                                                          ar, Constants.Roles.PIZZAPROVIDER);
      getBlackboardService().publishAdd(alloc);
      if (logger.isDebugEnabled()) {
        logger.debug(" allocating task " + task + " to pizza provider: " + provider);
      }
    }
  }

  /**
   * Updates the estimated AllocationResults with reported AllocationResults. Checks the
   * changed collection on the Allocation subscription.  If the collection is not empty,
   * it retrieves the Allocation PlanElement from the subscription.  If the reported
   * AllocationResults have changed, the results are udpated and a publish change is
   * called on the PlanElement.  The reported AllocationResults are changed by the
   * provider assinged to the task.
   */
  protected void updateOrderAllocationResults() {
    for (Iterator i = allocationSub.getChangedCollection().iterator(); i.hasNext();) {
      PlanElement pe = (PlanElement) i.next();
      if (logger.isDetailEnabled()) {
        logger.detail("Updating the allocation results on " + printAllocationResults(pe));
      }
      /**
       * Looks for differences between reported and estimated allocation results. If they
       * are not equal the reported value is copied into the estimated value.  The updating
       * of results should be done in order for the results to continue to flow up the
       * PlanElement chain.
       */
      if (PluginHelper.updatePlanElement(pe)) {
        getBlackboardService().publishChange(pe);
      }
    }
  }

  /**
   * Returns the Expansion that was changed.  Checks the changed collection on the
   * Subscription.  If the collection is not empty, it returns the first element. If empty
   * it returns null.
   *
   * @return the Expansion that was changed
   */
  protected Expansion getChangedExpansion() {
    for (Iterator i = expansionSub.getChangedCollection().iterator(); i.hasNext();) {
      //  This plugin expects only one expansion
      return (Expansion) i.next();
    }
    return null;
  }

  /**
   * Returns a NewTask.  Creates a task with the specified verb and sets the Asset as the
   * direct object.
   * @param verb the verb of the task
   * @param directObject the asset
   * @return a NewTask
   */
  protected NewTask makeTask(Verb verb, Asset directObject) {
    NewTask newTask = planningFactory.newTask();
    newTask.setVerb(verb);
    newTask.setPlan(planningFactory.getRealityPlan());
    newTask.setDirectObject(directObject);
    return newTask;
  }

  /**
   * Returns an instance of a PizzaAsset.  Creates an instance of a PizzaAsset of the
   * specified asset type.  Adds ItemIdentification property groups to the pizzaAsset
   * instance.
   *
   * @param assetType the name of the type of asset
   * @return a PizzaAsset
   */
  protected PizzaAsset makePizzaAsset(String assetType) {
    PizzaAsset pizzaAsset = (PizzaAsset) planningFactory.createInstance(Constants.PIZZA);
    NewItemIdentificationPG itemIDPG = PropertyGroupFactory.newItemIdentificationPG();
    itemIDPG.setItemIdentification(assetType);
    pizzaAsset.setItemIdentificationPG(itemIDPG);
    return pizzaAsset;
  }

  /**
   * Returns a quantity Preference.  Creates a Preferenece with a ScoringFunction and a
   * quantity AspectType from the specified value.
   *
   * @param value the value of the quantity preference
   * @return a Preference representing a quantity
   */
  protected Preference makeQuantityPreference(int value) {
    ScoringFunction sf = ScoringFunction.createStrictlyAtValue(AspectValue.newAspectValue(AspectType.QUANTITY, value));
    Preference pref = planningFactory.newPreference(AspectType.QUANTITY, sf);
    return pref;
  }

  /**
   * Returns a pizza provider for this agent.  Get all relationships that match the Role
   * of PizzaProvider from the RelationshipSchedule. Return the provider Entity from
   * the first Relationship found. If there are no pizza provider relationships, null
   * will be returned.
   *
   * @return a pizza provider Entity
   */
  protected Entity getProvider() {
    Entity provider = null;
    // Get the RelationshipSchedule for this agent
    RelationshipSchedule relSched = getSelfEntity().getRelationshipSchedule();
    // Find all relationships matching the role of pizza provider
    Collection relationships = relSched.getMatchingRelationships(Constants.Roles.PIZZAPROVIDER);
    for (Iterator iterator = relationships.iterator(); iterator.hasNext();) {
      Relationship r = (Relationship) iterator.next();
      provider = (Entity) relSched.getOther(r);
      break; // we only need one
    }
    return provider;
  }

  /**
   * Returns the Entity representing the agent.  Checks the self entity subscription and
   * returns the first element.  In this example, there should be only one self entity.
   * Will return null if the subscription is empty.
   *
   * @return the Entity representing the agent.
   */
  protected Entity getSelfEntity() {
    if (selfSub.isEmpty()) {
      if (logger.isErrorEnabled()) {
        logger.error(" Self Entity subscription is empty, this should not happen!!!!");
      }
      return null;
    }
    return (Entity) selfSub.first();
  }

  protected String printExpansionResults(Expansion exp) {
    return "the reported AllocationResults are [isSuccess: " +
        exp.getReportedResult().isSuccess() + " confidence rating: "
        + exp.getReportedResult().getConfidenceRating() + " ]";
  }

  protected String printAllocationResults(PlanElement pe) {
    return "task UID: " + pe.getTask().getUID() + " estimated: " +
        pe.getEstimatedResult() + " reported: " + pe.getReportedResult();
  }

  protected String printPartyOrderResults() {
    return "Pizza ORDER task failed: Joe's Local Pizza Shack can't make veggie pizzas! " +
        "We need to find another pizza provider.";
  }

  /**
   * This predicate matches the Entity object of the agent.
   */
  protected static final UnaryPredicate SELF_PRED = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Entity) {
        return ((Entity) o).isSelf();
      }
      return false;
    }
  };

  /**
   * This predicate matches PizzaPreferences objects.
   */
  protected static final UnaryPredicate PIZZA_PREF_PRED = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof PizzaPreferences);
    }
  };

  /**
   * This predicate matches Allocations on "Order" tasks.
   */
  protected static final UnaryPredicate ALLOCATION_PRED = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Allocation) {
        Task task = ((Allocation) o).getTask();
        return task.getVerb().equals(Constants.Verbs.ORDER);
      }
      return false;
    }
  };

  /**
   * This predicate matches Expansions on "Order" tasks.
   */
  protected static final UnaryPredicate EXPANSION_PRED = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Expansion) {
        Task task = ((Expansion) o).getTask();
        return task.getVerb().equals(Constants.Verbs.ORDER);
      }
      return false;
    }
  };
}
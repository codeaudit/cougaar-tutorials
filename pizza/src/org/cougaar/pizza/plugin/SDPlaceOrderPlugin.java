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

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.asset.PizzaAsset;
import org.cougaar.pizza.util.PGCreator;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.util.*;
import org.cougaar.planning.ldm.asset.Entity;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.plugin.util.PluginHelper;

import java.util.*;

/**
 * Plugin for ordering pizzas -- more documentation later
 */
public class SDPlaceOrderPlugin extends ComponentPlugin {
  private LoggingService logger;
  private DomainService domainService;
  private IncrementalSubscription selfSub;
  private IncrementalSubscription pizzaPrefSub;
  private IncrementalSubscription findProvidersDispositionSub;
  private IncrementalSubscription allocationSub;
  private IncrementalSubscription expansionSub;
  private Subscription taskSub;
  private Entity self;
  private PlanningFactory planningFactory;

  private static final String AS = "AS";
  private static final String VEGGIE = "Veggie";
  private static final String MEAT = "Meat";
  private static final String VEGGIE_PIZZA = "Veggie Pizza";
  private static final String MEAT_PIZZA = "Meat Pizza";

  /**
   * Create our blackboard subscriptions.
   */
  protected void setupSubscriptions() {
    selfSub = (IncrementalSubscription) blackboard.subscribe(selfPred);
    pizzaPrefSub = (IncrementalSubscription) blackboard.subscribe(pizzaPrefPred);
    findProvidersDispositionSub = (IncrementalSubscription) blackboard.subscribe(findProvidersDispositionPred);
    expansionSub = (IncrementalSubscription) blackboard.subscribe(expansionPred);
    taskSub = blackboard.subscribe(taskPred);
    allocationSub = (IncrementalSubscription) blackboard.subscribe(allocationPred);
    getServices();
  }

  /**
   * Setup all of our core services.
   */
  private void getServices () {
    domainService = getDomainService();
    LoggingService ls = (LoggingService) getServiceBroker().getService(this, LoggingService.class, null);
    logger = LoggingServiceWithPrefix.add(ls, getAgentIdentifier() + ": ");
    planningFactory = (PlanningFactory) domainService.getFactory("planning");
  }


  /**
   * Used by the binding utility through reflection to set my DomainService
   */
  public void setDomainService(DomainService aDomainService) {
    domainService = aDomainService;
  }

  /**
   * Used by the binding utility through reflection to get my DomainService
   */
  public DomainService getDomainService() {
    return domainService;
  }

  protected void execute() {

    //TODO: this is wrong, need to change
    if (self == null) {
      if ( selfSub.getAddedCollection().isEmpty()) {
        //cannot do anything until our self org is set
        return;
      } else {
        self = (Entity) selfSub.getAddedList().nextElement();
      }
    }

    // Create pizza orders when we find a new PizzaPreference object on our blackboard.
    // The Pizza Preference object serves as a repository for party invitation responses.
    for (Iterator iterator = pizzaPrefSub.getAddedCollection().iterator(); iterator.hasNext();) {
      PizzaPreferences pizzaPrefs = (PizzaPreferences) iterator.next();
      if (logger.isDebugEnabled()) {
        logger.debug(" found pizzaPrefs " + pizzaPrefSub);
      }
      // For now we assume only one, but we should enhance to accommodate many
      publishFindProvidersTask(null);
      Task orderTask = makeOrderTask();
      Collection subtasks = expandTask(pizzaPrefs, orderTask);
      allocateTasks(subtasks, null);
    }

    // Check for updates on our findProviders tasks. Once this task is successfully disposed, we
    // should have new provider relationships.
    for (Iterator i = findProvidersDispositionSub.getAddedCollection().iterator(); i.hasNext();) {
      Disposition disposition = (Disposition) i.next();
      if (disposition.isSuccess() && disposition.getEstimatedResult().getConfidenceRating() == 1.0) {
        Collection tasks = getUnallocatedTasks();
        if (! tasks.isEmpty()) {
          // check the find providers task for exclusions
          Entity exclusion = checkFindProvidersTask(disposition.getTask());
          allocateTasks(getUnallocatedTasks(), exclusion);
        }
      }
    }

    // Check our allocation subscription for changes.  Changes usually indicate that we have received a
    // new allocation result from our provider.  These results should be checked and then copied to
    // the estimated allocation result slot so that the results can continue to flow up the planelement chain.
    if ( ! allocationSub.getChangedCollection().isEmpty()) {
      for (Iterator i = allocationSub.iterator(); i.hasNext();) {
        PlanElement pe = (PlanElement) i.next();
        if (PluginHelper.updatePlanElement(pe)) {
          blackboard.publishChange(pe);
        }
      }
    }

    // Check the results on the pizza order expansion.  If it is failed, find a new provider and replan
    // our pizza order tasks.
    if (! expansionSub.getChangedCollection().isEmpty()) {
      for (Iterator i = expansionSub.iterator(); i.hasNext();) {
        Expansion exp = (Expansion) i.next();
        if (! exp.getReportedResult().isSuccess()) {
          processFailedExpansion(exp);
        }
      }
    }
  }

  /**
   * Allocate our pizza order tasks to a pizza provider.
   * @param tasks The tasks to send to the pizza provider.
   * @param excludeProvider A provider that has previously failed our pizza order.  This parameter can be null if
   *  we don't have any providers that we need to exclude from our provider list.
   */
  private void allocateTasks(Collection tasks, Entity excludeProvider) {
    Collection providers = getProviderOrgAssets();
    Entity provider = null;
    for (Iterator iterator = providers.iterator(); iterator.hasNext();) {
      provider = (Entity) iterator.next();
      if (provider.equals(excludeProvider)) continue;
    }
    if (provider != null) {
      for (Iterator i = tasks.iterator(); i.hasNext();) {
        Task newTask = (Task) i.next();
        AllocationResult ar = PluginHelper.createEstimatedAllocationResult(newTask, planningFactory, 1.0, true);
        Allocation alloc = planningFactory.createAllocation(newTask.getPlan(), newTask, (Asset) provider, ar,
                                                            Role.getRole(Constants.PIZZA_PROVIDER));
        blackboard.publishAdd(alloc);
        if (logger.isDebugEnabled()) {
          logger.debug(" allocating task " + newTask);
        }
      }
    }
  }

  /**
   * Expand our order pizza task into specific meat and veggie pizza orders.
   * @param pizzaPrefs The preferences of the party guests.
   * @param parentTask The order task that is the parent of the specific pizza order tasks.
   * @return A collection of pizza order tasks.
   */
  private Collection expandTask(PizzaPreferences pizzaPrefs, Task parentTask) {
    ArrayList tasksToAllocate = new ArrayList();
    NewTask meatPizzaTask = makePizzaTask(MEAT);
    Preference meatPref = makeQuantityPreference(pizzaPrefs.getNumMeat());
    meatPizzaTask.setPreference(meatPref);
    meatPizzaTask.setParentTask(parentTask);

    NewTask veggiePizzaTask = makePizzaTask(VEGGIE);
    Preference veggiePref = makeQuantityPreference(pizzaPrefs.getNumVeg());
    veggiePizzaTask.setPreference(veggiePref);
    veggiePizzaTask.setParentTask(parentTask);

    NewWorkflow wf = planningFactory.newWorkflow();
    wf.setParentTask(parentTask);
    wf.setIsPropagatingToSubtasks(true);
    wf.addTask(meatPizzaTask);
    wf.addTask(veggiePizzaTask);
    meatPizzaTask.setWorkflow(wf);
    veggiePizzaTask.setWorkflow(wf);
    Expansion expansion = planningFactory.createExpansion(parentTask.getPlan(), parentTask, wf, null);

    if (logger.isDebugEnabled()) {
      logger.debug(" publishing expansion and subtasks ");
    }
    blackboard.publishAdd(expansion);
    blackboard.publishAdd(meatPizzaTask);
    blackboard.publishAdd(veggiePizzaTask);
    tasksToAllocate.add(meatPizzaTask);
    tasksToAllocate.add(veggiePizzaTask);
    return tasksToAllocate;
  }

  /**
   * Utility method to create a task preference representing the quantity of pizza.
   * @param num The preferred number of pizzas (TODO: Is that right or is it the number of people that want this type of pizza??)
   * @return A Task quantity preference.
   */
  private Preference makeQuantityPreference(int num) {
    //logger.debug(" what is the quantity " + num);
    ScoringFunction sf = ScoringFunction.createStrictlyAtValue(AspectValue.newAspectValue(AspectType.QUANTITY, num));
    Preference pref = planningFactory.newPreference(AspectType.QUANTITY, sf);
    return pref;
  }

  /**
   * Create the Find Providers task that is used by service discovery to find our pizza providers.
   * @param excludePhrase A Prepositional Phrase that tells the service discovery mechanism that we need
   * a provider other then the one listed in the phrase.
   */
  private void publishFindProvidersTask(PrepositionalPhrase excludePhrase) {
    NewTask newTask = planningFactory.newTask();
    newTask.setVerb(Verb.get(Constants.FIND_PROVIDERS));
    newTask.setPlan(planningFactory.getRealityPlan());
    newTask.setDirectObject(self);
    Vector prepPhrases = new Vector();
    NewPrepositionalPhrase pp = planningFactory.newPrepositionalPhrase();
    pp.setPreposition(AS);
    pp.setIndirectObject(Role.getRole(Constants.PIZZA_PROVIDER));
    prepPhrases.add(pp);
    if (excludePhrase != null) {
      prepPhrases.add(excludePhrase);
    }
    newTask.setPrepositionalPhrases(prepPhrases.elements());
    blackboard.publishAdd(newTask);
  }

  /**
   * Utility method to make a pizza task
   * @param pizzaType The type of pizza we want - meat or veggie
   * @return The new pizza task.
   */
  private NewTask makePizzaTask(String pizzaType) {
    NewTask newTask = planningFactory.newTask();
    newTask.setVerb(Verb.get(Constants.ORDER));
    newTask.setPlan(planningFactory.getRealityPlan());
    newTask.setDirectObject(makePizzaAsset(pizzaType));
    return newTask;
  }

  /**
   * Utility method to create the top level order task.
   * @return The order pizza task.
   */
  private Task makeOrderTask() {
    NewTask newTask = planningFactory.newTask();
    newTask.setVerb(Verb.get(Constants.ORDER));
    newTask.setPlan(planningFactory.getRealityPlan());
    newTask.setDirectObject(planningFactory.createInstance(Constants.PIZZA));
    blackboard.publishAdd(newTask);
    return newTask;
  }

  /**
   * If our order task fails because one or both of our pizza order subtasks fail, request a new provider
   * from service discovery that is not the provider that failed our task(s).  Also, rescind the orders that
   * we placed with that provider so that we can order all of our pizza from a single provider.
   * @param exp The order task expansion that contains our pizza orders.
   */
  private void processFailedExpansion(Expansion exp) {
    // Find the supplier to that failed and rescind the allocations
    Asset failedSupplier = null;
    for (Iterator resultsIt = exp.getWorkflow().getSubtaskResults().iterator(); resultsIt.hasNext();) {
      SubTaskResult result = (SubTaskResult) resultsIt.next();
      if (! result.getAllocationResult().isSuccess()) {
        failedSupplier = ((Allocation)result.getTask().getPlanElement()).getAsset();
      }
      //rescind all task allocations since we will replan with a new provider.
      //Note we are rescinding the successful meat pizza order because we want our entire order to
      //be delivered by one pizza provider.
      blackboard.publishRemove(result.getTask().getPlanElement());
    }
    NewPrepositionalPhrase excludePP = planningFactory.newPrepositionalPhrase();
    excludePP.setPreposition(Constants.NOT);
    excludePP.setIndirectObject(failedSupplier);
    publishFindProvidersTask(excludePP);
  }

  /**
   * Look at the FindProviders task to see if it has the exclusion prepositional phrase that alerts us
   * not to use that provider. Presumably that provider has been unable to satisfy previous pizza orders.
   * @param findProvidersTask The task to look at.
   * @return The entity (provider) that we wish to exclude.  Note that this value can be null if the find providers
   * task did not contain the special exclude phrase.
   */
  private Entity checkFindProvidersTask(Task findProvidersTask) {
    Entity excludedEntity = null;
    PrepositionalPhrase notPP = findProvidersTask.getPrepositionalPhrase(Constants.NOT);
    if (notPP != null) {
      excludedEntity = (Entity) notPP.getIndirectObject();
    }
    return excludedEntity;
  }

  /**
   * Create the pizza asset that we use for the direct object in our pizza order tasks.  This tells the pizza
   * provider what kind of pizza we are ordering.
   * @param pizzaType Kind of pizza - either meat or veggie.
   * @return A pizza.
   */
  private PizzaAsset makePizzaAsset (String pizzaType) {
    // Create a Veggie Pizza Asset based on the existing pizza prototype
    PizzaAsset pizzaAsset = (PizzaAsset) planningFactory.createInstance(Constants.PIZZA);

    if (pizzaType.equals(VEGGIE)) {
      pizzaAsset.addOtherPropertyGroup(PGCreator.makeAVeggiePG(planningFactory, true));
      pizzaAsset.setItemIdentificationPG(PGCreator.makeAItemIdentificationPG(planningFactory, VEGGIE_PIZZA));
    }

    // Create a Meat Pizza Asset based on the existing pizza prototype
    if (pizzaType.equals("Meat")) {
      pizzaAsset.addOtherPropertyGroup(PGCreator.makeAMeatPG(planningFactory, true));
      pizzaAsset.setItemIdentificationPG(PGCreator.makeAItemIdentificationPG(planningFactory, MEAT_PIZZA));
    }
    return pizzaAsset;
  }

  /**
   * Find the pizza provider entities that we can send our pizza orders to.
   * @return A collection of pizza providers to use.
   */
  public Collection getProviderOrgAssets() {
    TimeSpan timeSpan = TimeSpans.getSpan(TimeSpan.MIN_VALUE, TimeSpan.MAX_VALUE);
    RelationshipSchedule relSched = self.getRelationshipSchedule();
    Collection relationships = relSched.getMatchingRelationships(Role.getRole(Constants.PIZZA_PROVIDER), timeSpan);
    List providers = new ArrayList();
    for (Iterator iterator = relationships.iterator(); iterator.hasNext();) {
      Relationship r = (Relationship) iterator.next();
      providers.add(relSched.getOther(r));
    }
    return providers;
  }

  /**
   * Utility method to filter our task subscription for any tasks that are not disposed of or allocated.
   * Tasks might be unallocated to a provider if we were waiting for a provider or we rescinded our orders
   * from a provider that could not fill our order.
   * @return A collection of tasks that are ready to be allocated.
   */
  private Collection getUnallocatedTasks() {
    return Filters.filter((Collection) taskSub, undisposedTasks);
  }

  /**
   * A predicate that matches our agent's entity asset.  This entity represent our agent and can be used to
   * find relationships with other agents.
   */
  private static UnaryPredicate selfPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Entity) {
        return ((Entity) o).isSelf();
      }
      return false;
    }
  };

  /**
   * A predicate that matches PizzaPreferences objects
   */
  private static UnaryPredicate pizzaPrefPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof PizzaPreferences);
    }
  };

  /**
   * A predicate that matches dispositions of "FindProviders" tasks
   */
  private static UnaryPredicate findProvidersDispositionPred = new UnaryPredicate () {
    public boolean execute(Object o) {
      if (o instanceof Disposition) {
        Task task = ((Disposition) o).getTask();
        return task.getVerb().equals(Verb.get(Constants.FIND_PROVIDERS));
      }
      return false;
    }
  };

  /**
   * A predicate that matches tasks that have no planelement.
   */
  private static UnaryPredicate undisposedTasks = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task)  {
        Task task = (Task) o;
        return (task.getPlanElement() == null);
      }
      return false;
    }
  };

  /**
   * A predicate that matches expansions of "ORDER" tasks
   */
  private static UnaryPredicate expansionPred = new UnaryPredicate () {
    public boolean execute(Object o) {
      if (o instanceof Expansion) {
        Task task = ( (Expansion) o).getTask();
        return task.getVerb().equals(Verb.get(Constants.ORDER));
      }
      return false;
    }
  };

  /**
   * A predicate that matches "ORDER" tasks.
   */
  private static UnaryPredicate taskPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        return (task.getVerb().equals(Constants.ORDER));
      }
      return false;
    }
  };

  /**
   * A predicate that matches allocationes of "ORDER" tasks.
   */
   private static UnaryPredicate allocationPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Allocation) {
        Task task = ((Allocation) o).getTask();
        return (task.getVerb().equals(Constants.ORDER));
      }
      return false;
    }
  };
}

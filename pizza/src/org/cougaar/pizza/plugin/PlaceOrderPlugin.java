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
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
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
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.Filters;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.TimeSpans;
import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * This Plugin manages ordering pizzas at customer agents.  
 */
public class PlaceOrderPlugin extends ComponentPlugin {
  private static final String VEGGIE = "Veggie";
  private static final String MEAT = "Meat";
  private static final String VEGGIE_PIZZA = "Veggie Pizza";
  private static final String MEAT_PIZZA = "Meat Pizza";

  private LoggingService logger;
  private DomainService domainService;
  private PlanningFactory planningFactory;

  private IncrementalSubscription selfSub;
  private IncrementalSubscription pizzaPrefSub;
  private IncrementalSubscription allocationSub;
  private Subscription taskSub;

  private Entity self;
  /**
   * Used by the binding utility through introspection to set my DomainService
   * Services that are required for plugin usage should be set through reflection instead of explicitly
   * getting each service from your ServiceBroker in the load method. The setter methods are called after
   * the component is constructed but before the state methods such as initialize, load, setupSubscriptions, etc.
   * If the service is not available at that time the component will be unloaded.
   */
  public void setDomainService(DomainService aDomainService) {
    domainService = aDomainService;
  }

  public void load() {
    super.load();
    ServiceBroker sb = getServiceBroker();
    logger = (LoggingService) sb.getService(this, LoggingService.class, null);
    // prefix all logging calls with our agent name
    logger = LoggingServiceWithPrefix.add(logger, agentId + ": ");
    planningFactory = (PlanningFactory) domainService.getFactory("planning");

  }

  protected void setupSubscriptions() {
    selfSub = (IncrementalSubscription) blackboard.subscribe(selfPred);
    pizzaPrefSub = (IncrementalSubscription) blackboard.subscribe(pizzaPrefPred);
    taskSub = blackboard.subscribe(taskPred);
    allocationSub = (IncrementalSubscription) blackboard.subscribe(allocationPred);
  }

  protected void execute() {
    if (self == null) {
      if (selfSub.getAddedCollection().isEmpty()) {
        //cannot do anything until our self org is set
        return;
      } else {
        self = (Entity) selfSub.getAddedList().nextElement();
      }
    }

    for (Iterator i = pizzaPrefSub.getAddedCollection().iterator(); i.hasNext();) {
      PizzaPreferences pizzaPrefs = (PizzaPreferences) i.next();
      if (logger.isDebugEnabled()) {
        logger.debug(" found pizzaPrefs " + pizzaPrefSub);
      }
      // For now we assume only one, but we should enhance to accommodate many
      Task orderTask = makeOrderTaskAndPublish();
      Collection subtasks = makeExpansionAndPublish(pizzaPrefs, orderTask);
      allocateTasks(subtasks);
    }

    if (selfSub.getChangedList().hasMoreElements()) {
      if (logger.isDebugEnabled()) {
        logger.debug(" self entity changed,  trying to allocate ");
      }
      Collection tasks = getUnallocatedTasks();
      if (!tasks.isEmpty()) {
        allocateTasks(tasks);
      }
    }

    if (!allocationSub.getChangedCollection().isEmpty()) {
      for (Iterator i = allocationSub.iterator(); i.hasNext();) {
        PlanElement pe = (PlanElement) i.next();
        if (PluginHelper.updatePlanElement(pe)) {
          blackboard.publishChange(pe);
        }
      }
    }
  }

  /**
   * This method allocates order pizza tasks to pizza providers.
   * @param tasks
   */
  private void allocateTasks(Collection tasks) {
    Collection providers = getProviders();
    Entity provider = null;
    for (Iterator iterator = providers.iterator(); iterator.hasNext();) {
      provider = (Entity) iterator.next();
    }
    if (provider != null) {
      for (Iterator iter = tasks.iterator(); iter.hasNext();) {
        Task newTask = (Task) iter.next();
        AllocationResult ar = PluginHelper.createEstimatedAllocationResult(newTask, planningFactory, 0.25, true);
        Allocation alloc = planningFactory.createAllocation(newTask.getPlan(), newTask, (Asset) provider,
                                                            ar, Constants.Role.PIZZAPROVIDER);
        blackboard.publishAdd(alloc);
        if (logger.isDebugEnabled()) {
          logger.debug(" allocating task " + newTask);
        }
      }
    }
  }

  /**
   * This method expands order pizza tasks into two substasks:  meat and veggie.
   * @param pizzaPrefs
   * @param parentTask
   * @return
   */
  private Collection makeExpansionAndPublish(PizzaPreferences pizzaPrefs, Task parentTask) {
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

    blackboard.publishAdd(expansion);
    blackboard.publishAdd(meatPizzaTask);
    blackboard.publishAdd(veggiePizzaTask);
    if (logger.isDebugEnabled()) {
      logger.debug(" publishing expansion and subtasks ");
    }

    ArrayList tasksToAllocate = new ArrayList();
    tasksToAllocate.add(meatPizzaTask);
    tasksToAllocate.add(veggiePizzaTask);
    return tasksToAllocate;
  }

  /**
   * This method makes a quantity preference.  This quantity represents the number of people requesting a specific type
   * pizza.
   * @param num
   * @return
   */
  private Preference makeQuantityPreference(int num) {
    ScoringFunction sf = ScoringFunction.createStrictlyAtValue(AspectValue.newAspectValue(AspectType.QUANTITY, num));
    Preference pref = planningFactory.newPreference(AspectType.QUANTITY, sf);
    return pref;
  }

  /**
   * This method creates a task for a specific type of pizza.
   * @param pizzaType a String representing the type of pizza.
   * @return NewTask
   */
  private NewTask makePizzaTask(String pizzaType) {
    NewTask newTask = planningFactory.newTask();
    newTask.setVerb(Verb.get(Constants.ORDER));
    newTask.setPlan(planningFactory.getRealityPlan());
    newTask.setDirectObject(makePizzaAsset(pizzaType));
    return newTask;
  }

  /**
   * This method creates an "Order" pizza task and publishes it to the blackboard.
   * @return NewTask
   */
  private NewTask makeOrderTaskAndPublish() {
    NewTask newTask = planningFactory.newTask();
    newTask.setVerb(Verb.get(Constants.ORDER));
    newTask.setPlan(planningFactory.getRealityPlan());
    newTask.setDirectObject(planningFactory.createInstance(Constants.PIZZA));
    blackboard.publishAdd(newTask);
    return newTask;
  }

  /**
   * This method makes the pizza asset.
   * @param pizzaType a String representing the type of pizza.
   * @return a PizzaAsset.
   */
  private PizzaAsset makePizzaAsset(String pizzaType) {
    // Create a Veggie Pizza Asset based on the existing pizza prototype
    PizzaAsset pizzaAsset = (PizzaAsset) planningFactory.createInstance(Constants.PIZZA);

    if (pizzaType.equals(VEGGIE)) {
      pizzaAsset.addOtherPropertyGroup(PropertyGroupFactory.newVeggiePG());
      NewItemIdentificationPG itemIDPG = PropertyGroupFactory.newItemIdentificationPG();
      itemIDPG.setItemIdentification(VEGGIE_PIZZA);
      pizzaAsset.setItemIdentificationPG(itemIDPG);
    }

    // Create a Meat Pizza Asset based on the existing pizza prototype
    if (pizzaType.equals("Meat")) {
      pizzaAsset.addOtherPropertyGroup(PropertyGroupFactory.newMeatPG());
      NewItemIdentificationPG itemIDPG = PropertyGroupFactory.newItemIdentificationPG();
      itemIDPG.setItemIdentification(MEAT_PIZZA);
      pizzaAsset.setItemIdentificationPG(itemIDPG);
    }
    return pizzaAsset;
  }

  /**
   * This method finds pizza providers.
   * @return a Collection of pizza providers.
   */
  public Collection getProviders() {
    TimeSpan timeSpan = TimeSpans.getSpan(TimeSpan.MIN_VALUE, TimeSpan.MAX_VALUE);
    RelationshipSchedule relSched = self.getRelationshipSchedule();
    Collection relationships = relSched.getMatchingRelationships(Constants.Role.PIZZAPROVIDER, timeSpan);
    List providers = new ArrayList();
    for (Iterator iterator = relationships.iterator(); iterator.hasNext();) {
      Relationship r = (Relationship) iterator.next();
      providers.add(relSched.getOther(r));
    }
    return providers;
  }

  /**
   * This method uses a filter to extract all unallocated tasks from the task subscription.
   * @return a Collection of tasks that have not been allocated.
   */
  private Collection getUnallocatedTasks() {
    return Filters.filter((Collection) taskSub, undisposedTasks);
  }

  /**
   *  This predicate matches the Entity object of the local agent
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
   * This predicate matches PizzaPreferences objects.
   */
  private static UnaryPredicate pizzaPrefPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof PizzaPreferences);
    }
  };

  /**
   * This predicate matches tasks that have not been disposed.
   */
  private static UnaryPredicate undisposedTasks = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        return (task.getPlanElement() == null);
      }
      return false;
    }
  };

  /**
   * This predicate matches "Order" tasks.
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
   * This predicate matches Allocations on "Order" tasks.
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

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
public class PlaceOrderPlugin extends ComponentPlugin {
  private LoggingService logger;
  private DomainService domainService;
  private IncrementalSubscription selfSub;
  private IncrementalSubscription pizzaPrefSub;
  private IncrementalSubscription allocationSub;
  private Subscription taskSub;
  private Entity self;
  private PlanningFactory planningFactory;

  private static final String VEGGIE = "Veggie";
  private static final String MEAT = "Meat";
  private static final String VEGGIE_PIZZA = "Veggie Pizza";
  private static final String MEAT_PIZZA = "Meat Pizza";

  protected void setupSubscriptions() {
    selfSub = (IncrementalSubscription) blackboard.subscribe(selfPred);
    pizzaPrefSub = (IncrementalSubscription) blackboard.subscribe(pizzaPrefPred);
    taskSub = blackboard.subscribe(taskPred);
    allocationSub = (IncrementalSubscription) blackboard.subscribe(allocationPred);
    getServices();
  }

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
    //TODO: fix this
    if (self == null) {
      if ( selfSub.getAddedCollection().isEmpty()) {
        //cannot do anything until our self org is set
        return;
      } else {
        self = (Entity) selfSub.getAddedList().nextElement();
      }
    }

    for (Iterator iterator = pizzaPrefSub.getAddedCollection().iterator(); iterator.hasNext();) {
      PizzaPreferences pizzaPrefs = (PizzaPreferences) iterator.next();
      if (logger.isDebugEnabled()) {
        logger.debug(" found pizzaPrefs " + pizzaPrefSub);
      }
      // For now we assume only one, but we should enhance to accommodate many
      Task orderTask = makeOrderTask();
      Collection subtasks = expandTask(pizzaPrefs, orderTask);
      allocateTasks(subtasks);
    }

    if (selfSub.getChangedList().hasMoreElements()) {
      if (logger.isDebugEnabled()) {
        logger.debug(" self entity changed,  trying to allocate ");
      }
      Collection tasks = getUnallocatedTasks();
      if (! tasks.isEmpty()) {
        allocateTasks(tasks);
      }
    }

    if ( ! allocationSub.getChangedCollection().isEmpty()) {
      for (Iterator i = allocationSub.iterator(); i.hasNext();) {
        PlanElement pe = (PlanElement) i.next();
        if (PluginHelper.updatePlanElement(pe)) {
          blackboard.publishChange(pe);
        }
      }
    }
  }

  private void allocateTasks(Collection tasks) {
    Collection providers = getProviderOrgAssets();
    Entity provider = null;
    for (Iterator iterator = providers.iterator(); iterator.hasNext();) {
      provider = (Entity) iterator.next();
    }
    if (provider != null) {
      for (Iterator i = tasks.iterator(); i.hasNext();) {
        Task newTask = (Task) i.next();
        AllocationResult ar = 
	  PluginHelper.createEstimatedAllocationResult(newTask, 
						       planningFactory, 0.25, 
						       true);
        Allocation alloc = 
	  planningFactory.createAllocation(newTask.getPlan(), newTask, 
					   (Asset) provider, ar,
					   Constants.Role.PIZZAPROVIDER);
        blackboard.publishAdd(alloc);
        if (logger.isDebugEnabled()) {
          logger.debug(" allocating task " + newTask);
        }
      }
    }
  }

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

  private Preference makeQuantityPreference(int num) {
    //logger.debug(" what is the quantity " + num);
    ScoringFunction sf = ScoringFunction.createStrictlyAtValue(AspectValue.newAspectValue(AspectType.QUANTITY, num));
    Preference pref = planningFactory.newPreference(AspectType.QUANTITY, sf);
    return pref;
  }

  private NewTask makePizzaTask(String pizzaType) {
    NewTask newTask = planningFactory.newTask();
    newTask.setVerb(Verb.get(Constants.ORDER));
    newTask.setPlan(planningFactory.getRealityPlan());
    newTask.setDirectObject(makePizzaAsset(pizzaType));
    return newTask;
  }

  private Task makeOrderTask() {
    NewTask newTask = planningFactory.newTask();
    newTask.setVerb(Verb.get(Constants.ORDER));
    newTask.setPlan(planningFactory.getRealityPlan());
    newTask.setDirectObject(planningFactory.createInstance(Constants.PIZZA));
    blackboard.publishAdd(newTask);
    return newTask;
  }

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

  public Collection getProviderOrgAssets() {
    TimeSpan timeSpan = TimeSpans.getSpan(TimeSpan.MIN_VALUE, TimeSpan.MAX_VALUE);
    RelationshipSchedule relSched = self.getRelationshipSchedule();
    Collection relationships = 
      relSched.getMatchingRelationships(Constants.Role.PIZZAPROVIDER, 
					timeSpan);
    List providers = new ArrayList();
    for (Iterator iterator = relationships.iterator(); iterator.hasNext();) {
      Relationship r = (Relationship) iterator.next();
      providers.add(relSched.getOther(r));
    }
    return providers;
  }

  private Collection getUnallocatedTasks() {
    return Filters.filter((Collection) taskSub, undisposedTasks);
  }

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

  private static UnaryPredicate undisposedTasks = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task)  {
        Task task = (Task) o;
        return (task.getPlanElement() == null);
      }
      return false;
    }
  };

  private static UnaryPredicate taskPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        return (task.getVerb().equals(Constants.ORDER));
      }
      return false;
    }
  };

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

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
 * This Plugin manages ordering pizzas at customer agents.
 */
public class PlaceOrderPlugin extends ComponentPlugin {
  protected LoggingService logger;
  protected DomainService domainService;
  protected PlanningFactory planningFactory;

  protected IncrementalSubscription selfSub;
  protected IncrementalSubscription pizzaPrefSub;
  protected IncrementalSubscription allocationSub;

  /**
   * Used by the binding utility through introspection to set my DomainService Services that are required for plugin
   * usage should be set through reflection instead of explicitly getting each service from your ServiceBroker in the
   * load method. The setter methods are called after the component is constructed but before the state methods such as
   * initialize, load, setupSubscriptions, etc. If the service is not available at that time the component will be
   * unloaded.
   */
  public void setDomainService(DomainService aDomainService) {
    domainService = aDomainService;
  }

  public void load() {
    super.load();
    ServiceBroker sb = getServiceBroker();
    logger = (LoggingService) sb.getService(this, LoggingService.class, null);
    planningFactory = (PlanningFactory) domainService.getFactory(org.cougaar.planning.ldm.PlanningDomain.PLANNING_NAME);
    sb.releaseService(this, DomainService.class, domainService);
  }

  protected void setupSubscriptions() {
    selfSub = (IncrementalSubscription) getBlackboardService().subscribe(SELF_PRED);
    pizzaPrefSub = (IncrementalSubscription) getBlackboardService().subscribe(PIZZA_PREF_PRED);
    allocationSub = (IncrementalSubscription) getBlackboardService().subscribe(ALLOCATION_PRED);
  }

  protected void execute() {
    for (Iterator i = pizzaPrefSub.getAddedCollection().iterator(); i.hasNext();) {
      PizzaPreferences pizzaPrefs = (PizzaPreferences) i.next();
      if (logger.isDebugEnabled()) {
        logger.debug(" found pizzaPrefs " + pizzaPrefSub);
      }

      Task orderTask = makeTask(Constants.Verbs.ORDER, makePizzaAsset(Constants.PIZZA));
      getBlackboardService().publishAdd(orderTask);
      Collection subtasks = makeSubtasks(pizzaPrefs, orderTask);
      makeExpansionAndPublish(orderTask, subtasks);
      allocateTasks(subtasks);
    }

    if (!allocationSub.getChangedCollection().isEmpty()) {
      for (Iterator i = allocationSub.iterator(); i.hasNext();) {
        PlanElement pe = (PlanElement) i.next();
        // TODO
        if (PluginHelper.updatePlanElement(pe)) {
          getBlackboardService().publishChange(pe);
        }
      }
    }
  }

  protected NewTask makeTask(Verb verb, Asset directObject) {
    NewTask newTask = planningFactory.newTask();
    newTask.setVerb(verb);
    newTask.setPlan(planningFactory.getRealityPlan());
    newTask.setDirectObject(directObject);
    return newTask;
  }

  /**
   * This method expands order pizza tasks into two subtasks:  meat and veggie.
   *
   * @param pizzaPrefs
   * @param parentTask
   * @return
   */
  protected Collection makeSubtasks(PizzaPreferences pizzaPrefs, Task parentTask) {
    ArrayList subtasks = new ArrayList(2);

    PizzaAsset meatPizza = makePizzaAsset(Constants.MEAT_PIZZA);
    meatPizza.addOtherPropertyGroup(PropertyGroupFactory.newMeatPG());
    NewTask meatPizzaTask = makeTask(Constants.Verbs.ORDER, meatPizza);
    Preference meatPref = makeQuantityPreference(pizzaPrefs.getNumMeat());
    meatPizzaTask.setPreference(meatPref);
    meatPizzaTask.setParentTask(parentTask);
    subtasks.add(meatPizzaTask);

    PizzaAsset veggiePizza = makePizzaAsset(Constants.VEGGIE_PIZZA);
    veggiePizza.addOtherPropertyGroup(PropertyGroupFactory.newVeggiePG());
    NewTask veggiePizzaTask = makeTask(Constants.Verbs.ORDER, veggiePizza);
    Preference veggiePref = makeQuantityPreference(pizzaPrefs.getNumVeg());
    veggiePizzaTask.setPreference(veggiePref);
    veggiePizzaTask.setParentTask(parentTask);
    subtasks.add(veggiePizzaTask);
    return subtasks;
  }

  protected void makeExpansionAndPublish(Task orderTask, Collection subtasks) {
    // Wire expansion will create an expansion plan element, a new workflow, add the subtasks and set the intial
    // allocation result to null.
    Expansion expansion = PluginHelper.wireExpansion(orderTask, new Vector(subtasks), planningFactory);
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
   * This method allocates order pizza tasks to pizza providers.
   *
   * @param tasks
   */
  protected void allocateTasks(Collection tasks) {
    Entity provider = getProvider();
    if (provider == null) {
      if (logger.isErrorEnabled()) {
        logger.error(" Provider is null, check config files for provider relationship");
      }
      return;
    }
    for (Iterator iter = tasks.iterator(); iter.hasNext();) {
      Task newTask = (Task) iter.next();
      // TODO:  not zero and not 1 any thing between 0 and 1
      // TODO:
      AllocationResult ar = PluginHelper.createEstimatedAllocationResult(newTask, planningFactory, 0.25, true);
      Allocation alloc = planningFactory.createAllocation(newTask.getPlan(), newTask, (Asset) provider, ar,
                                                          Constants.Roles.PIZZAPROVIDER);
      getBlackboardService().publishAdd(alloc);
      if (logger.isDebugEnabled()) {
        logger.debug(" allocating task " + newTask); // to provider
      }
    }
  }

  protected Entity getSelfEntity() {
    if (selfSub.isEmpty()) {
      if (logger.isErrorEnabled()) {
        logger.error(" Self Entity subscription is empty, this should not happen!!!!");
      }
      return null;
    }
    return (Entity) selfSub.first();
  }

  protected PizzaAsset makePizzaAsset(String assetType) {
    PizzaAsset pizzaAsset = (PizzaAsset) planningFactory.createInstance(Constants.PIZZA);
    NewItemIdentificationPG itemIDPG = PropertyGroupFactory.newItemIdentificationPG();
    itemIDPG.setItemIdentification(assetType);
    pizzaAsset.setItemIdentificationPG(itemIDPG);
    return pizzaAsset;
  }

  /**
   * This method makes a quantity preference.  This quantity represents the number of people requesting a specific type
   * pizza.
   *
   * @param num
   * @return
   */
  protected Preference makeQuantityPreference(int num) {
    ScoringFunction sf = ScoringFunction.createStrictlyAtValue(AspectValue.newAspectValue(AspectType.QUANTITY, num));
    Preference pref = planningFactory.newPreference(AspectType.QUANTITY, sf);
    return pref;
  }

  /**
   * change this to getProvider return Entity This method finds pizza providers.
   *
   * @return a Collection of pizza providers.
   */
  protected Entity getProvider() {
    // TODO: timeless provider
    Entity provider = null;
    RelationshipSchedule relSched = getSelfEntity().getRelationshipSchedule();
    Collection relationships = relSched.getMatchingRelationships(Constants.Roles.PIZZAPROVIDER);

    for (Iterator iterator = relationships.iterator(); iterator.hasNext();) {
      Relationship r = (Relationship) iterator.next();
      provider = (Entity) relSched.getOther(r);
      break; // we only need one
    }
    return provider;
  }

  /**
   * This predicate matches the Entity object of the local agent
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
}

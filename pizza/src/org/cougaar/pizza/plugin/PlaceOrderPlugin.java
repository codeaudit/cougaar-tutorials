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
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.asset.PizzaAsset;
import org.cougaar.pizza.util.PGCreator;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.util.*;
import org.cougaar.planning.ldm.asset.Entity;

import java.util.*;

/**
 * Plugin for ordering pizzas -- more documentation later
 */
public class PlaceOrderPlugin extends ComponentPlugin {
  private LoggingService logger;
  private DomainService domainService;
  private IncrementalSubscription selfSub;
  private IncrementalSubscription pizzaPrefSub;
  private IncrementalSubscription findProvidersSub;
  private IncrementalSubscription taskSub;
  private IncrementalSubscription allocationSub;
  private IncrementalSubscription expansionSub;
  private Entity self;
  private PlanningFactory planningFactory;
  private List tasksToAllocate = new ArrayList();

  public void load() {
    super.load();
    LoggingService ls = (LoggingService) getServiceBroker().getService(this, LoggingService.class, null);
    logger = LoggingServiceWithPrefix.add(ls, getAgentIdentifier() + ": ");
    planningFactory = getPlanningFactory();
  }

  protected void setupSubscriptions() {
    logger.error(" setupSubscriptions called");
    domainService = getDomainService();
    selfSub = (IncrementalSubscription) blackboard.subscribe(selfPred);
    pizzaPrefSub = (IncrementalSubscription) blackboard.subscribe(pizzaPrefPred);
    findProvidersSub = (IncrementalSubscription) blackboard.subscribe(findProvidersPred);
    allocationSub = (IncrementalSubscription) blackboard.subscribe(allocationPred);

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
    if (self == null) {
      if ( selfSub.getAddedCollection().isEmpty()) {
        //cannot do anything until our self org is set
        return;
      } else {
        self = (Entity) selfSub.getAddedList().nextElement();
      }
    }

    for (Enumeration enum = pizzaPrefSub.getAddedList(); enum.hasMoreElements();) {

      PizzaPreferences pizzaPrefs = (PizzaPreferences) enum.nextElement();
      logger.error(" found pizzaPrefs "  + pizzaPrefSub);
      // For now we assume only one, but we should enhance to accommodate many
      publishFindProvidersTask();
      createOrderTaskAndExpand(pizzaPrefs);
    }

    for (Enumeration enum = findProvidersSub.getAddedList(); enum.hasMoreElements();) {
      Disposition disposition = (Disposition) enum.nextElement();
      if (disposition.isSuccess() && disposition.getEstimatedResult().getConfidenceRating() == 1.0) {
        allocateTasks();
      }
    }
  }

  private void allocateTasks() {
    //To change body of created methods use File | Settings | File Templates.
  }

  private void createOrderTaskAndExpand(PizzaPreferences pizzaPrefs) {
    Task parentTask = makeOrderTask();
    NewTask meatPizzaTask = makeTask("Meat");
    meatPizzaTask.setParentTask(parentTask);
    NewTask veggiePizzaTask = makeTask("Veggie");
    veggiePizzaTask.setParentTask(parentTask);
    blackboard.publishAdd(parentTask);

    NewWorkflow wf = planningFactory.newWorkflow();
    wf.setParentTask(parentTask);
    wf.setIsPropagatingToSubtasks(true);
    wf.addTask(meatPizzaTask);
    wf.addTask(veggiePizzaTask);
    meatPizzaTask.setWorkflow(wf);
    veggiePizzaTask.setWorkflow(wf);
    Expansion expansion = planningFactory.createExpansion(parentTask.getPlan(), parentTask, wf, null);

    logger.error(" publishing expansion and subtasks ");
    blackboard.publishAdd(expansion);
    blackboard.publishAdd(meatPizzaTask);
    blackboard.publishAdd(veggiePizzaTask);
    tasksToAllocate.add(meatPizzaTask);
    tasksToAllocate.add(veggiePizzaTask);
  }

  private PlanningFactory getPlanningFactory() {
    PlanningFactory factory = null;
    if (domainService != null) {
      factory = (PlanningFactory) domainService.getFactory("planning");
    }
    return factory;
  }

  private void publishFindProvidersTask() {
    PlanningFactory planningFactory = getPlanningFactory();
    NewTask newTask = planningFactory.newTask();
    newTask.setVerb(Verb.get(Constants.FIND_PROVIDERS));
    newTask.setPlan(planningFactory.getRealityPlan());
    newTask.setDirectObject(self);
    NewPrepositionalPhrase pp = planningFactory.newPrepositionalPhrase();
    pp.setPreposition("AS");
    pp.setIndirectObject(Role.getRole(Constants.PIZZA_PROVIDER));
    newTask.setPrepositionalPhrases(pp);
    blackboard.publishAdd(newTask);
  }

  // TODO:  placeholder for now, may need to change significantly
  private NewTask makeTask(String pizzaType) {
    NewTask newTask = planningFactory.newTask();
    newTask.setVerb(Verb.get(Constants.ORDER));
    newTask.setPlan(planningFactory.getRealityPlan());
    newTask.setDirectObject(makePizzaAsset(pizzaType));
    return newTask;
  }

// TODO:  placeholder for now, may need to change significantly
  private Task makeOrderTask() {
    NewTask newTask = planningFactory.newTask();
    newTask.setVerb(Verb.get(Constants.ORDER));
    newTask.setPlan(planningFactory.getRealityPlan());
    newTask.setDirectObject(planningFactory.createInstance("pizza"));
    return newTask;
  }

  private PizzaAsset makePizzaAsset (String pizzaType) {
    // Create a Veggie Pizza Asset based on the existing pizza prototype
    PizzaAsset pizzaAsset = (PizzaAsset) planningFactory.createInstance("pizza");

    if (pizzaType.equals("Veggie")) {
      pizzaAsset.addOtherPropertyGroup(PGCreator.makeAVeggiePG(planningFactory, true));
      pizzaAsset.setItemIdentificationPG(PGCreator.makeAItemIdentificationPG(planningFactory, "Veggie Pizza"));
    }

    // Create a Meat Pizza Asset based on the existing pizza prototype
    if (pizzaType.equals("Meat")) {
      pizzaAsset.addOtherPropertyGroup(PGCreator.makeAMeatPG(planningFactory, true));
      pizzaAsset.setItemIdentificationPG(PGCreator.makeAItemIdentificationPG(planningFactory, "Meat Pizza"));
    }
    return pizzaAsset;
  }

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
  private static UnaryPredicate findProvidersPred = new UnaryPredicate (){
    public boolean execute(Object o) {
      if (o instanceof Disposition) {
        Task task = ((Disposition) o).getTask();
        return task.getVerb().equals(Verb.get(Constants.FIND_PROVIDERS));
      }
      return false;
    }
  };

   /**
   * A predicate that matches expansions of "ORDER" tasks
   */
  private static UnaryPredicate expansionPred = new UnaryPredicate (){
    public boolean execute(Object o) {
      if (o instanceof Expansion) {
        Task task = ( (Expansion) o).getTask();
        return task.getVerb().equals(Verb.get(Constants.ORDER));
      }
      return false;
    }
  };

  /**
   * A predicate that matches allocations of "ORDER" tasks
   */
  private static UnaryPredicate allocationPred = new UnaryPredicate (){
    public boolean execute(Object o) {
      if (o instanceof Allocation) {
        Task t = ((Allocation)o).getTask();
        if (t != null) {
          return t.getVerb().equals(Verb.get(Constants.ORDER));
        }
      }
      return false;
    }
  };
}

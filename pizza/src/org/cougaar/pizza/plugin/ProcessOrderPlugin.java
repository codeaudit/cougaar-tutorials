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
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.asset.KitchenAsset;
import org.cougaar.pizza.asset.PizzaAsset;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Iterator;

/**
 * This plugin processes pizza orders at the pizza provider agents.
 */
public class ProcessOrderPlugin extends ComponentPlugin {
  private LoggingService logger;
  private DomainService domainService;
  private IncrementalSubscription tasksSubscription;
  private IncrementalSubscription kitchenAssetSubscription;
  private PlanningFactory pFactory = null;
  private KitchenAsset kitchen = null;

  /**
   * Set up our services and our factory.
   */
  public void load() {
    super.load();

    logger = (LoggingService)
        getServiceBroker().getService(this, LoggingService.class, null);
    domainService = (DomainService)
        getServiceBroker().getService(this, DomainService.class, null);
    pFactory = (PlanningFactory) domainService.getFactory("planning");
  }

  /**
   * Create the subscriptions to my tasks and kitchen assets
   */
  protected void setupSubscriptions() {
    tasksSubscription = (IncrementalSubscription)
        getBlackboardService().subscribe(OrderTasksPred);
    kitchenAssetSubscription = (IncrementalSubscription)
        getBlackboardService().subscribe(KitchenAssetPred);
  }

  /**
   * Process the subscriptions
   */
  protected void execute() {
    // Make sure we have a kitchen asset before we allocate our tasks.
    // We only expect 1 kitchen asset so we'll exit if we don't have
    // one and set it when we do.
    if (kitchen == null) {
      if (!kitchenAssetSubscription.isEmpty()) {
        kitchen = (KitchenAsset) kitchenAssetSubscription.first();
        // allocate all of the tasks on our subscription so far in case we
        // missed some on the added list while our kitchen asset was null
        allocateOrderTasks(tasksSubscription.getCollection());
      }
      //if the kitchen asset is still not there return out of the
      // execute cycle
      if (kitchen == null) { return; }
    } else {
      //if we have our kitchen asset process our tasks
      //Right now assume we only get new tasks and no changes
      Collection newOrderTasks = tasksSubscription.getAddedCollection();
      // Try to allocate the new Order tasks
      allocateOrderTasks(newOrderTasks);
    }
  }

  /**
   * Allocate the new order tasks to the pizza kitchen asset
   * @param newOrderTasks The tasks that are ordering pizzas from our
   * kitchen.
   */
  private void allocateOrderTasks(Collection newOrderTasks) {
    for (Iterator i = newOrderTasks.iterator(); i.hasNext();) {
      Task newTask = (Task) i.next();
      // See if our kitchen can make the type of pizza requested and then
      // make a successful or unsuccessful allocation result.
      boolean kitchenCanMake = checkWithKitchen(newTask);
      AllocationResult ar;
      if (kitchenCanMake) {
        ar = PluginHelper.createEstimatedAllocationResult(newTask,
                                                          pFactory, 1.0,
                                                          kitchenCanMake);
      } else {
        // if we can't make the pizza provide a failed allocation result
        // with a quantity of zero.
        AspectValue qtyAspectValue =
            AspectValue.newAspectValue(AspectType.QUANTITY, 0);
        AspectValue[] aspectValueArray = {qtyAspectValue};
        ar = pFactory.newAllocationResult(1.0, kitchenCanMake,
                                          aspectValueArray);
      }
      Allocation alloc =
          pFactory.createAllocation(newTask.getPlan(), newTask, kitchen,
                                    ar, Constants.Role.PIZZAPROVIDER);
      getBlackboardService().publishAdd(alloc);
    }
  }

  /**
   * Check with our kitchen asset to see if it can make the requested
   * type of pizza
   * @param newTask The order task.
   * @return boolean If we can make the pizza - determines if the
   * AllocationResult is successful or not.
   */
  private boolean checkWithKitchen(Task newTask) {
    boolean canMakePizza = true;
    PizzaAsset directObject = (PizzaAsset) newTask.getDirectObject();
    // Compare PGs on the pizza to PGs on the kitchen
    // check the veggie pg
    boolean vegPG = directObject.hasVeggiePG();
    if (vegPG) {
      if (!kitchen.hasVeggiePG()) {
        canMakePizza = false;
        //TODO: Turn logging down to debug
        if (logger.isErrorEnabled()) {
          logger.error("Provider " + getAgentIdentifier().toString() +
                       " can't make the VeggiePizza that was ordered!");
        }
      }
    }
    //check the meat pg
    boolean meatPG = directObject.hasMeatPG();
    if (meatPG) {
      if (!kitchen.hasMeatPG()) {
        canMakePizza = false;
        //TODO: Turn logging down to debug
        if (logger.isErrorEnabled()) {
          logger.error("Provider " + getAgentIdentifier().toString() +
                       " can't make the MeatPizza that was ordered!");
        }
      }
    }
    return canMakePizza;
  }

  /**
   * A predicate that filters for "ORDER" tasks
   */
  private static UnaryPredicate OrderTasksPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        return ((Task) o).getVerb().equals(Verb.get(Constants.ORDER));
      }
      return false;
    }
  };

  /**
   * A predicate that filters for KitchenAsset objects
   */
  private static UnaryPredicate KitchenAssetPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof KitchenAsset) {
        return true;
      }
      return false;
    }
  };

}

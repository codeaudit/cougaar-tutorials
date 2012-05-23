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

import java.util.Collection;
import java.util.Iterator;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.asset.KitchenAsset;
import org.cougaar.pizza.asset.PizzaAsset;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.UnaryPredicate;

/**
 * This plugin processes incoming pizza Order Tasks at the pizza provider
 * agents. It matches the topping requirements (PropertyGroups on the
 * DirectObject of the Task) with the capabilities of its Kitchen (again,
 * PropertyGroups on the Asset) to decide if it can handle the Order.
 */
public class ProcessOrderPlugin
      extends ComponentPlugin {
   private LoggingService logger;
   private DomainService domainService;
   private IncrementalSubscription tasksSubscription; // orders
   private IncrementalSubscription pesSubscription; // our responses
   private IncrementalSubscription kitchenAssetSubscription; // the kitchen
   private PlanningFactory planningFactory = null;

   // local pointer to kitchen
   // Note that this is transient local state, that we must
   // re-initialize from our subscription any time the plugin re-starts,
   // which it may do on agent mobility or node restart for example
   private transient KitchenAsset kitchen = null;

   /**
    * Used by the binding utility through introspection to set my DomainService
    * Services that are required for plugin usage should be set through
    * reflection instead of explicitly getting each service from your
    * ServiceBroker in the load method. The setter methods are called after the
    * component is constructed but before the state methods such as initialize,
    * load, setupSubscriptions, etc. If the service is not available at that
    * time the component will be unloaded.
    */
   public void setDomainService(DomainService aDomainService) {
      domainService = aDomainService;
   }

   /**
    * Set up our services and our factory.
    */
   @Override
   public void load() {
      super.load();
      logger = getServiceBroker().getService(this, LoggingService.class, null);

      planningFactory = (PlanningFactory) domainService.getFactory("planning");
      getServiceBroker().releaseService(this, DomainService.class, domainService);
   }

   /**
    * Create the subscriptions to my Order Tasks, PlanElements for those, and
    * Kitchen Asset
    */
   @Override
   protected void setupSubscriptions() {
      tasksSubscription = (IncrementalSubscription) getBlackboardService().subscribe(ORDER_TASKS_PRED);
      // Subscribe to PlanElements handling the Order Tasks -
      // to tell when we've processed a Task
      pesSubscription = (IncrementalSubscription) getBlackboardService().subscribe(ORDER_TASKS_PES_PRED);
      kitchenAssetSubscription = (IncrementalSubscription) getBlackboardService().subscribe(KITCHEN_ASSET_PRED);
   }

   /**
    * Process the subscriptions: match topping PG on Order Tasks with the PGs on
    * the KitchenAsset, and if the Kitchen has the needed PGs, respond with
    * SUCCESS.
    */
   @Override
   protected void execute() {
      // Make sure we have a kitchen asset before we allocate our tasks.
      // We only expect 1 kitchen asset so we'll exit if we don't have
      // one and set it when we do.
      if (kitchenAssetSubscription.isEmpty()) {
         return;
      }

      // If get here, the Kitchen asset has been created
      if (kitchen == null) {
         // Need to get a pointer to Kitchen asset, and then
         // try to Allocate any Tasks
         kitchen = (KitchenAsset) kitchenAssetSubscription.first();

         // allocate all of the tasks on our subscription so far in case we
         // missed some on the added list while our kitchen asset was null
         allocateOrderTasks(tasksSubscription.getCollection());
      } else {
         // if we had our kitchen asset already, process any new tasks
         // Right now assume we only get new tasks and no changes
         allocateOrderTasks(tasksSubscription.getAddedCollection());
      }
   }

   /**
    * Allocate the new order tasks to the pizza kitchen asset
    * 
    * @param newOrderTasks The tasks that are ordering pizzas from our kitchen.
    */
   private void allocateOrderTasks(Collection newOrderTasks) {
      for (Iterator i = newOrderTasks.iterator(); i.hasNext();) {
         Task newTask = (Task) i.next();

         // Has this Order Task already been processed?
         // This would happen if the agent restarted, for example.
         // 2 approaches:
         // 1: Check task.getPlanElement()
         // -- quick, but mixes Subscription view with direct view, so
         // frowned upon (may cause inconsistent state, errors)
         // 1B: BlackBoard Query -- really just the same as the direct approach
         // 2: Standing subscription to PEs
         // -- Check if there's a PlanElement on our subscription that points to
         // this Task

         // So we check to see if this Task was already planned, and avoid
         // double-planning it
         if (taskWasPlanned(newTask, pesSubscription)) {
            // We apparently already handled this Task. Skip it for now
            if (logger.isDebugEnabled()) {
               logger.debug("Order already processed: " + newTask);
            }
            continue;
         }

         // See if our kitchen can make the type of pizza requested and then
         // make a successful or unsuccessful allocation result.
         boolean kitchenCanMake = canMakePizza(newTask);
         AllocationResult ar;
         if (kitchenCanMake) {
            // This helper method makes an allocation result containing all
            // aspect values that match the current task's
            // preferences. Set the confidence value of the allocation result to
            // 1.0 indicating a completed result and set
            // isSuccess to true.
            ar = PluginHelper.createEstimatedAllocationResult(newTask, planningFactory, 1.0, true);
         } else {
            // Since we can't make the pizza we create a new aspect value to
            // represent the zero quantity -- no pizzas provided
            AspectValue qtyAspectValue = AspectValue.newAspectValue(AspectType.QUANTITY, 0);
            AspectValue[] aspectValueArray = {
               qtyAspectValue
            };
            // Use the planning factory to create a new allocation result with a
            // confidence of 1.0 and isSuccess is false.
            ar = planningFactory.newAllocationResult(1.0, false, aspectValueArray);
         }
         // Design choice: The final processing of this task ends as an
         // allocation to the kitchen asset.
         // Another option would be to create a Disposition as the plan element
         // instead of an
         // allocation to an asset.
         Allocation alloc =
               planningFactory.createAllocation(newTask.getPlan(), newTask, kitchen, ar, Constants.Roles.PIZZAPROVIDER);
         getBlackboardService().publishAdd(alloc);
      }
   }

   /**
    * Re-usable method to search a Collection (ie, Subscription) of PlanElements
    * to see if any handle the given Task. Use this to decide whether a Task
    * needs to be planned by this Plugin.
    * 
    * @param t Task for which to search for a PlanElement
    * @param planelements Collection of PlanElements to look through, typically
    *        a subscription
    * @return true if a PlanElement in the collection points to this Task
    */
   private boolean taskWasPlanned(Task t, Collection planelements) {
      if (t == null || planelements == null || planelements.isEmpty()) {
         return false;
      }

      Iterator iter = pesSubscription.iterator();
      while (iter.hasNext()) {
         PlanElement pe = (PlanElement) iter.next();
         if (pe.getTask().equals(t)) {
            if (logger.isDebugEnabled()) {
               logger.debug("Order Task already planned. Task: " + t);
            }
            return true;
         }
      } // end of while loop

      // No PlanElement on our subscription handles this Task
      if (logger.isDebugEnabled()) {
         logger.debug("Order Task not yet planned: " + t);
      }

      return false;
   }

   /**
    * Check the kitchen asset to see if it has the toppings to make the
    * requested type of pizza
    * 
    * @param newTask The order task.
    * @return true If we can make the pizza
    */
   private boolean canMakePizza(Task newTask) {
      PizzaAsset directObject = (PizzaAsset) newTask.getDirectObject();
      // Compare PGs on the pizza to PGs on the kitchen
      if (directObject.hasVeggiePG() && !kitchen.hasVeggiePG()) {
         if (logger.isWarnEnabled()) {
            logger.warn("Can't make the VeggiePizza that was ordered!");
         }
         return false;
      }

      if (directObject.hasMeatPG() && !kitchen.hasMeatPG()) {
         if (logger.isWarnEnabled()) {
            logger.warn("Can't make the MeatPizza that was ordered!");
         }
         return false;
      }
      return true;
   }

   /**
    * A predicate that filters for Verb.Order tasks
    */
   private final static UnaryPredicate ORDER_TASKS_PRED = new UnaryPredicate() {
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
         if (o instanceof Task) {
            return ((Task) o).getVerb().equals(Constants.Verbs.ORDER);
         }
         return false;
      }
   };

   /**
    * A predicate that filters for PlanElements handling Verb.Order tasks
    */
   private final static UnaryPredicate ORDER_TASKS_PES_PRED = new UnaryPredicate() {
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
         if (o instanceof PlanElement) {
            Task t = ((PlanElement) o).getTask();
            return t.getVerb().equals(Constants.Verbs.ORDER);
         }
         return false;
      }
   };

   /**
    * A predicate that filters for KitchenAsset objects
    */
   private final static UnaryPredicate KITCHEN_ASSET_PRED = new UnaryPredicate() {
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
         return (o instanceof KitchenAsset);
      }
   };
} // end of ProcessOrderPlugin

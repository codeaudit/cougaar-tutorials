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
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.asset.KitchenAsset;

import java.util.Collection;
import java.util.Iterator;
import java.util.Enumeration;

/**
 *
 */
public class ProcessOrderPlugin extends ComponentPlugin {
  private LoggingService logger;
  private DomainService domainService;
  private IncrementalSubscription tasksSub;
  private IncrementalSubscription kitchenAssetSub;
  private PlanningFactory pFactory = null;
  private KitchenAsset kitchen = null;

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

  public void load() {
    super.load();
    logger = getLoggingService(this);
  }

  protected void setupSubscriptions() {
    tasksSub = (IncrementalSubscription) blackboard.subscribe(OrderTasksPred);
    kitchenAssetSub = (IncrementalSubscription) blackboard.subscribe(KitchenAssetPred);
    getPlanningFactory();
  }

  private LoggingService getLoggingService(Object requestor) {
    return (LoggingService) getServiceBroker().getService(requestor, LoggingService.class, null);
  }

  protected void execute() {
    // Make sure we have a kitchen asset before we allocate our tasks.
    // We only expect 1 kitchen asset so we'll exit if we don't have one and set it when we do.
    if (kitchen == null) {
      if (!kitchenAssetSub.isEmpty()) {
        kitchen = (KitchenAsset) kitchenAssetSub.first();
        // allocate all of the tasks on our subscription so far in case we missed
        // some on the added list while our kitchen asset was null
        allocateOrderTasks(tasksSub.getCollection());
      }
      //if the kitchen asset is still not there return out of the execute cycle
      if (kitchen == null) {
        return;
      }
    } else {
      //if we have our kitchen asset process our tasks
      //Right now assume we only get new tasks and no changes
      Collection newOrderTasks = tasksSub.getAddedCollection();
      // Try to allocate the new Order tasks
      allocateOrderTasks(newOrderTasks);
    }
  }

  /**
   * Allocate the new order tasks to the pizza kitchen asset
   */
  private void allocateOrderTasks(Collection newOrderTasks)  {
    for (Iterator i = newOrderTasks.iterator(); i.hasNext();) {
      Task newTask = (Task) i.next();
      // See if our kitchen can make the type of pizza requested and then
      // make a successful or unsuccessful allocation result.
      boolean kitchenCanMake = checkWithKitchen(newTask);
      AllocationResult ar = PluginHelper.createEstimatedAllocationResult(newTask, pFactory, 1.0, kitchenCanMake);
      Allocation alloc = pFactory.createAllocation(newTask.getPlan(), newTask, kitchen, ar,
                                                   Role.getRole(Constants.PIZZA_PROVIDER));
      blackboard.publishAdd(alloc);
    }
  }

  /**
   * Check with our kitchen asset to see if it can make the requested type of pizza
   * @param newTask The order task.
   * @return boolean If we can make the pizza - determines if the AllocationResult is
   * successful or not.
   */
  private boolean checkWithKitchen(Task newTask) {
    boolean canMakePizza = false;
    Asset directObject = newTask.getDirectObject();
    Enumeration pgs = (directObject.fetchAllProperties()).elements();
    while (pgs.hasMoreElements()) {
      PropertyGroup pg = (PropertyGroup) pgs.nextElement();
      PropertyGroup match = kitchen.searchForPropertyGroup(pg.getPrimaryClass());
      if (match != null) {
        //TODO: turn down logging to debug
        if (logger.isErrorEnabled()) {
          logger.error("Found a match for the pizza in the kitchen for PG: " + pg);
        }
        canMakePizza = true;
      } else {
        //TODO: turn down logging to debug
        if (logger.isErrorEnabled()) {
          logger.error("Did NOT find a match for the pizza in the kitchen for PG: " + pg + " match: " + match + " calling" +
                       "Kitchen pg directly " + kitchen.getItemIdentificationPG().getItemIdentification());
        }
        canMakePizza = false;
      }
    }
    //TODO: turn down logging to debug
    if (logger.isErrorEnabled()) {
      logger.error("Returning can make pizza answer of: " + canMakePizza + " for task " + newTask);
    }
    return canMakePizza;
  }

   /**
   * Get the planning factory if we already have it, if not set it.
   * @return PlanningFactory The factory that creates planning objects.
   */
  private PlanningFactory getPlanningFactory() {
    if (domainService != null && pFactory == null) {
      pFactory = (PlanningFactory) domainService.getFactory("planning");
    }
    return pFactory;
  }

   /**
   * A predicate that filters for "ORDER" tasks
   */
  private static UnaryPredicate OrderTasksPred = new UnaryPredicate (){
    public boolean execute(Object o) {
      if (o instanceof Task) {
        return ((Task)o).getVerb().equals(Verb.get(Constants.ORDER));
      }
      return false;
    }
  };

   /**
   * A predicate that filters for KitchenAsset objects
   */
  private static UnaryPredicate KitchenAssetPred = new UnaryPredicate (){
    public boolean execute(Object o) {
      if (o instanceof KitchenAsset) {
        return true;
      }
      return false;
    }
  };

}

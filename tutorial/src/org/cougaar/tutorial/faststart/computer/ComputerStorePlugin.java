package org.cougaar.tutorial.faststart.computer;

/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.tutorial.faststart.*;
import org.cougaar.tutorial.faststart.computer.assets.*;
import org.cougaar.util.UnaryPredicate;
import java.util.Enumeration;
import java.util.Vector;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.*;

/**
 * Plugin to simulate the behavior of a computer store
 * Given a request for an estimate, it can provide a no-commitment
 * estimate. Given a request for supply, it can commit a computer and return
 * the costs in all aspects
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ComputerStorePlugin.java,v 1.3 2003-01-23 22:12:55 mbarger Exp $
 */
public class ComputerStorePlugin  extends ComponentPlugin
{

  private DomainService domainService = null;
  private PlanningFactory ldmf = null;

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

  private IncrementalSubscription allComputerAssets;
  private UnaryPredicate allComputerAssetsPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof ComputerAsset;
    }
  };

  // Set up subscription for all plan of tasks with verb 'SUPPLY'
  private IncrementalSubscription allSupplyTasks;
  private UnaryPredicate allSupplyTasksPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      return TutorialUtils.isTaskWithVerb(o, ComputerUtils.SUPPLY_VERB);
    }
  };

  /**
   * Plugin initialization : Subscribe to all assets and 
   * subscribe to all SUPPLY tasks
   **/
  public void setupSubscriptions() 
  {
    //    System.out.println("ComputerStorePlugin::setupSubscriptions");
    ldmf = (PlanningFactory) getDomainService().getFactory("planning");
    if (ldmf == null) {
      throw new RuntimeException(
          "Unable to find \"planning\" domain factory");
    }

    // Set up subscription for all assets
    allComputerAssets = 
      (IncrementalSubscription)getBlackboardService().subscribe(allComputerAssetsPredicate);

    // Subscribe to all SUPPLY tasks
    allSupplyTasks = 
      (IncrementalSubscription)getBlackboardService().subscribe(allSupplyTasksPredicate);
  }

  /**
   * Execute method : Taks supply tasks and allocate computer assets to them
   * as dictated by preferences (if anything fits and is available)
   */
  public void execute() 
  {
    //    System.out.println("ComputerStorePlugin::execute");

    for(Enumeration e = allSupplyTasks.getAddedList();e.hasMoreElements();)
    {
      Task task = (Task)e.nextElement();
      PlanElement plan_element = allocateTask(task);
      getBlackboardService().publishAdd(plan_element);
    }
  }

  // Try to allocate task to an asset
  // Ignore any already allocated assets
  // Return an allocation if success, and a failed allocation if not
  // Failure may be due to no asset available, or score thresholds being
  // outside bounds
  private PlanElement allocateTask(Task task)
  {
    PlanElement plan_element = null;
    ComputerAsset best_asset = null;
    double best_score = -1.0;
    for(Enumeration e = allComputerAssets.elements();e.hasMoreElements();)
    {
      ComputerAsset asset = (ComputerAsset)e.nextElement();

      // Can't promise materials that are already committed!
      if (isAllocated(asset)) {
        //	  System.out.println("Asset already allocated : " + asset);
        continue;
      }

      double score = ComputerUtils.computeScore(asset, task);
      //	System.out.println("Score for asset " + asset + " : " + score);
      if ((best_asset == null) || (best_score > score)) {
        best_asset = asset;
        best_score = score;
      }
    }

    // Is computed score for best_asset acceptable?
    boolean success = ((best_score >= ScoringFunction.LOW_THRESHOLD) && 
      (best_score <= ScoringFunction.HIGH_THRESHOLD));

    // Did we find an asset and was it acceptable by standards set by
    // the preferences?
    if ((best_asset != null) && success) 
    {
      // Found one : Create an allocation
      AllocationResult estAR = ComputerUtils.computeAllocationResult
        (task, true, best_asset, ldmf);
      plan_element = ldmf
        .createAllocation(task.getPlan(), task, best_asset, 
      estAR, Role.AVAILABLE);
      System.out.println("Allocating task to " + best_asset + 
        " Score = " + best_score);
    } 
    else {

      // Nope : No asset fits the order adequately. Report failure
      AllocationResult estAR = 
        ComputerUtils.computeAllocationResult(task, false, best_asset, 
					      ldmf);
      plan_element = ldmf
        .createFailedDisposition(task.getPlan(), task, estAR);
      //	System.out.println("Failed to allocate");
    }
    return plan_element;
  }

  // Is given asset already allocated (and not available to be sold again)
  private boolean isAllocated(ComputerAsset asset) {
    boolean result = false;

    RoleSchedule role_schedule = asset.getRoleSchedule();
    for(Enumeration re = role_schedule.getRoleScheduleElements();	re.hasMoreElements();)
    {
      // If its allocated at all, it must be spoken for
      Allocation alloc = (Allocation)re.nextElement();
      result = true;
      break;
    }
    return result;
  }

}

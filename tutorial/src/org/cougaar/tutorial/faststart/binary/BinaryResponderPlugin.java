package org.cougaar.tutorial.faststart.binary;

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

import java.util.*;
import org.cougaar.tutorial.faststart.*;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
/**
 * Binary search Responder plugin - Picks a number and
 * determines if the preferences (numeric bounds on the guess) contain
 * the number, returning success/failure accordingly
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: BinaryResponderPlugin.java,v 1.4 2003-01-23 22:12:54 mbarger Exp $
 */
public class BinaryResponderPlugin extends ComponentPlugin
{

  // Single asset to which to allocate 'MANAGE' tasks
  private Asset binary_asset;

    private DomainService domainService;
    private PlanningFactory ldmf;

    public void setDomainService(DomainService value) {
	domainService = value;
    }

    public DomainService getDomainService() {
	return domainService;
    }

  // Subscription to 'MANAGE' tasks
  private IncrementalSubscription allManageTasks;
  private UnaryPredicate allManageTasksPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      return TutorialUtils.isTaskWithVerb(o, BinaryUtils.MANAGE_VERB);
    }};
  
  public void setupSubscriptions() {
    //    System.out.println("BinaryResponderPlugin::setupSubscriptions");
    System.out.println("Solution Value = " + solution_value);
    ldmf = (PlanningFactory) getDomainService().getFactory("planning");
    if (ldmf == null) {
      throw new RuntimeException(
          "Unable to find \"planning\" domain factory");
    }

    // Subscribe to 'MANAGE' tasks (CHANGED and NEW)
    allManageTasks = 
      (IncrementalSubscription)getBlackboardService()
	.subscribe(allManageTasksPredicate);

    // Publish dummy asset to LDM to allow for allocating against
    binary_asset = ldmf
	.createPrototype("AbstractAsset", "BinaryAsset");
    getBlackboardService().publishAdd(binary_asset);

  }

  /**
   * Execute method for responder plugin : take new tasks and return
   * an allocation response indicating success/failure, and also
   * how big the gap on the request was, so that the requester
   * can verify that this response was for the proper request
   **/
  public void execute() {
    //    System.out.println("BinaryResponderPlugin::execute");

    // Allocate new tasks
    for(Enumeration e_added = allManageTasks.getAddedList(); 
	e_added.hasMoreElements(); )
      {
	Task task = (Task)e_added.nextElement();
	
	// Create an estimate that reports whether the preferences contain our solution
	AllocationResult estAR = computeAllocationResult(task);

	// Generate an allocation and publish it
	Allocation allocation = 
	  ldmf
	    .createAllocation(task.getPlan(), task, 
				   binary_asset, estAR,
				   Role.AVAILABLE);

	//	System.out.println("Publishing new allocation");

	// Publish new allocation for MANAGE task
	getBlackboardService().publishAdd(allocation);
      }


    // Modify allocation results for changed tasks
    for(Enumeration e_changed = allManageTasks.getChangedList(); 
	e_changed.hasMoreElements();) 
      {
	Task task = (Task)e_changed.nextElement();
	Allocation allocation = null;
	while(allocation == null) {
	  allocation = (Allocation)task.getPlanElement();
	  //	  System.out.println("Still null...");
	}

	// Set a new estimated result on the allocation based on changed preferences
	AllocationResult estAR = computeAllocationResult(task);
	allocation.setEstimatedResult(estAR);

	//	System.out.println("Publishing change to allocation");

	// Publish the change to the allocation
	getBlackboardService().publishChange(allocation);
      }

  }

  /**
   * Compute allocation result indicating success/failure depending on 
   * whether preferences contain solution value
   **/
  private AllocationResult computeAllocationResult(Task task) 
  {
    // Get bounds from preferences
    double []bounds = 
      TutorialUtils.getPreferredValueBounds(task, BinaryUtils.BINARY_BOUNDS_ASPECT,-1.0);
    double low_bound = bounds[0];
    double high_bound = bounds[1];

    // Return success value based on whether bounds contain solution value
    boolean success = ((low_bound <= solution_value) 
		       && (high_bound >= solution_value));

    System.out.println("computingAllocationResult : " + 
		       low_bound + " " + high_bound + " => " + success);

    // Create allocation result with this success value and return it
    // Report the high-low gap so we can tell what request our response is against
    AspectValue avs[] = new AspectValue[1];
    avs[0] = AspectValue.newAspectValue(BinaryUtils.BINARY_BOUNDS_ASPECT, high_bound-low_bound);

    AllocationResult allocation_result = ldmf
	.newAllocationResult(1.0, // rating
				 success, // are we in bounds?
				 avs);

    return allocation_result;
  }

  // At creation time, pick a private random integer 
  // for the other plugin to guess
  private static int solution_value = 
  BinaryUtils.MIN_VALUE + 
  (int)(java.lang.Math.random() * (BinaryUtils.MAX_VALUE - BinaryUtils.MIN_VALUE));
}



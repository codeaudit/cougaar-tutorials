package org.cougaar.tutorial.faststart.binary;

/**
  * Copyright 1997-1999 Defense Advanced Research Project Agency 
  * and ALPINE (A Raytheon Systems Company and BBN Corporation Consortium). 
  * This software to be used in accordance with the COUGAAR license agreement.
**/

import java.util.*;
import org.cougaar.tutorial.faststart.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;

/**
 * Binary search Responder plugin - Picks a number and
 * determines if the preferences (numeric bounds on the guess) contain
 * the number, returning success/failure accordingly
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: BinaryResponderPlugIn.java,v 1.2 2001-12-27 23:53:14 bdepass Exp $
 */
public class BinaryResponderPlugIn extends org.cougaar.core.plugin.SimplePlugIn
{

  // Single asset to which to allocate 'MANAGE' tasks
  private Asset binary_asset;

  // Subscription to 'MANAGE' tasks
  private IncrementalSubscription allManageTasks;
  private UnaryPredicate allManageTasksPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      return TutorialUtils.isTaskWithVerb(o, BinaryUtils.MANAGE_VERB);
    }};
  
  public void setupSubscriptions() {
    //    System.out.println("BinaryResponderPlugIn::setupSubscriptions");
    System.out.println("Solution Value = " + solution_value);

    // Subscribe to 'MANAGE' tasks (CHANGED and NEW)
    allManageTasks = 
      (IncrementalSubscription)subscribe(allManageTasksPredicate);

    // Publish dummy asset to LDM to allow for allocating against
    binary_asset = theLDMF.createPrototype("AbstractAsset", "BinaryAsset");
    publishAdd(binary_asset);

  }

  /**
   * Execute method for responder plugin : take new tasks and return
   * an allocation response indicating success/failure, and also
   * how big the gap on the request was, so that the requester
   * can verify that this response was for the proper request
   **/
  public void execute() {
    //    System.out.println("BinaryResponderPlugIn::execute");

    // Allocate new tasks
    for(Enumeration e_added = allManageTasks.getAddedList(); 
	e_added.hasMoreElements(); )
      {
	Task task = (Task)e_added.nextElement();
	
	// Create an estimate that reports whether the preferences contain our solution
	AllocationResult estAR = computeAllocationResult(task);

	// Generate an allocation and publish it
	Allocation allocation = 
	  theLDMF.createAllocation(task.getPlan(), task, 
				   binary_asset, estAR,
				   Role.AVAILABLE);

	//	System.out.println("Publishing new allocation");

	// Publish new allocation for MANAGE task
	publishAdd(allocation);
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
	publishChange(allocation);
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
    boolean success = ((low_bound <= solution_value) && (high_bound >= solution_value));

    System.out.println("computingAllocationResult : " + 
		       low_bound + " " + high_bound + " => " + success);

    // Create allocation result with this success value and return it
    int []aspect_types = {BinaryUtils.BINARY_BOUNDS_ASPECT};
    // Report the high-low gap so we can tell what request our response is against
    double []results = {high_bound-low_bound}; 
    AllocationResult allocation_result = 
      theLDMF.newAllocationResult(1.0, // rating
				 success, // are we in bounds?
				 aspect_types,
				 results);

    return allocation_result;
  }

  // At creation time, pick a private random integer 
  // for the other plugin to guess
  private static int solution_value = 
  BinaryUtils.MIN_VALUE + 
  (int)(java.lang.Math.random() * (BinaryUtils.MAX_VALUE - BinaryUtils.MIN_VALUE));
}



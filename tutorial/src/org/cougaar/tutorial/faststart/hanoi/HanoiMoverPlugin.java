package org.cougaar.tutorial.faststart.hanoi;

/**
  * Copyright 1997-1999 Defense Advanced Research Project Agency 
  * and ALPINE (A Raytheon Systems Company and BBN Corporation Consortium). 
  * This software to be used in accordance with the COUGAAR license agreement.
**/

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.tutorial.faststart.*;
import org.cougaar.util.UnaryPredicate;
import java.util.Enumeration;
import java.util.Vector;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.*;

/**
 * NOTE : This entire plugin represents [Level2] functionality unless otherwise
 *    noted.
 *
 * Towers of Hanoi Mover Plugin :
 * Takes a task of verb "TRANSPORT"
 * with Preposition "From" <from pole>
 * and Preposition "To" <to pole>
 * and Preference QUANTITY <count>
 * and Preferences for START_TIME and END_TIME
 * If <count> = 1,
 *   allocates task to dummy asset,
 *   prints "Moving from <from pole> to <to pole> at times..."
 *   generates allocationResponse indicating perfect
 *        satisfaction of preferences [Level4]
 * else error
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: HanoiMoverPlugin.java,v 1.3 2003-01-22 23:09:11 mbarger Exp $
 **/
public class HanoiMoverPlugin extends ComponentPlugin
{

  // Single asset to which to allocate all 'TRANSPORT' tasks
  private Asset mover_asset;

  // Subscription to all 'TRANSPORT' tasks
  private IncrementalSubscription allTransportTasks;

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

  // Predicate for all tasks of verb 'TRANSPORT'
  private UnaryPredicate allTransportTasksPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return TutorialUtils.isTaskWithVerb(o, HanoiUtils.TRANSPORT_VERB);
    }};

  /**
   * Establish subscription for TRANSPORT tasks
   **/
  public void setupSubscriptions() {
    System.out.println("HanoiMoverPlugin::setupSubscriptions");
    ldmf = (PlanningFactory) getDomainService().getFactory("planning");
    if (ldmf == null) {
      throw new RuntimeException(
          "Unable to find \"planning\" domain factory");
    }

    allTransportTasks =
      (IncrementalSubscription)getBlackboardService().subscribe(allTransportTasksPredicate);

    // Publish dummy asset to LDM to allow for allocating against
    mover_asset = ldmf.createPrototype("AbstractAsset", "HanoiAsset");
    getBlackboardService().publishAdd(mover_asset);
  }

  /**
   * Top level plugin execute loop : Take any new TRANSPORT task and
   * allocate to dummy asset
   **/
  public void execute() {
   //     System.out.println("HanoiMoverPlugin::execute");

    // Go through every new task we've subscribed to
    for(Enumeration e = allTransportTasks.getAddedList();e.hasMoreElements();)
      {
	Task task = (Task) e.nextElement();

	// Get the 'Hanoi' data out of the prepositions
	int from_pole = 
	  HanoiUtils.getPoleValue(task, HanoiUtils.FROM_PREPOSITION);
	int to_pole = 
	  HanoiUtils.getPoleValue(task, HanoiUtils.TO_PREPOSITION);
	int count = 
	  (int)TutorialUtils.getPreferredValue(task, AspectType.QUANTITY, -1.0);

	// Get preference times
	// [Level3]
	double start_time = 
	  TutorialUtils.getPreferredValue(task, AspectType.START_TIME, -1.0);
	double end_time = 
	  TutorialUtils.getPreferredValue(task, AspectType.END_TIME, -1.0);

	if (count == 1) {

	  AllocationResult estAR = null;

	  // Create an estimate that reports that we did just what we
	  // were asked to do
	  // [Level4]
          AspectValue avs[] = new AspectValue[2];
          avs[0] = AspectValue.newAspectValue(AspectType.START_TIME, start_time);
          avs[1] = AspectValue.newAspectValue(AspectType.END_TIME, end_time);
	  estAR =  ldmf
	      .newAllocationResult(1.0, // rating
					      true, // success, 
					      avs);
	  // End [Level4]

	  Allocation allocation = ldmf
	    .createAllocation(task.getPlan(), task, mover_asset,
				     estAR, Role.AVAILABLE);
	  System.out.println("Moving from pole " + from_pole + 
			     " to pole " + to_pole +
			     " start " + start_time + 
			     " end " + end_time);
	  getBlackboardService().publishAdd(allocation);
	} else {
	  System.out.println
	    ("Error : Should only be transporting one disk at a time!!!");
	}
      }
  }

}

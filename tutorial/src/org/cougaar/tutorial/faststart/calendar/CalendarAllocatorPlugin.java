package org.cougaar.tutorial.faststart.calendar;

/**
 * Copyright 1997-1999 Defense Advanced Research Project Agency 
 * and ALPINE (A Raytheon Systems Company and BBN Corporation Consortium). 
 * This software to be used in accordance with the COUGAAR license agreement.
 */

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.tutorial.faststart.*;
import org.cougaar.util.UnaryPredicate;
import java.util.*;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;

/**
 * Class to allocate 'Schedule' tasks to the supporting organization
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: CalendarAllocatorPlugin.java,v 1.2 2002-11-19 17:33:04 twright Exp $
 **/
public class CalendarAllocatorPlugin extends ComponentPlugin
{

    private DomainService domainService;
    private PlanningFactory ldmf;
    public void setDomainService(DomainService value) {
	domainService=value;
    }
    public DomainService getDomainService() {
	return domainService;
    }

  // Subscribe to organizations that provide the 'ScheduleManager' role
  private IncrementalSubscription allSupportAssets;
  private UnaryPredicate allSupportAssetsPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return TutorialUtils.isSupplierOrganization
        (o, CalendarUtils.CalendarServiceRole);
    }
  };

  // Establish subscription for all schedule tasks (NEW)
  private IncrementalSubscription allScheduleTasks;
  private UnaryPredicate allScheduleTasksPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return TutorialUtils.isTaskWithVerb(o, CalendarUtils.SCHEDULE_VERB);
    }
  };

  // Establish subscription for all schedule allocations changes
  private IncrementalSubscription allScheduleAllocations;
  private UnaryPredicate allScheduleAllocationsPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return TutorialUtils.isAllocationWithVerb(o, CalendarUtils.SCHEDULE_VERB);
    }
  };

  /**
   * Establish subscriptions for plugin : 
   *   all SCHEDULE tasks, all SCHEDULE allocations and all support assets
   **/
  public void setupSubscriptions() {
    // System.out.println("CalendarAllocatorPlugin::setupSubscriptions");
    ldmf = (PlanningFactory) getDomainService().getFactory("planning");
    if (ldmf == null) {
      throw new RuntimeException(
          "Unable to find \"planning\" domain factory");
    }

    allScheduleTasks = 
      (IncrementalSubscription)getBlackboardService()
	.subscribe(allScheduleTasksPredicate);
    allScheduleAllocations = 
      (IncrementalSubscription)getBlackboardService()
	.subscribe(allScheduleAllocationsPredicate);
    allSupportAssets = 
      (IncrementalSubscription)getBlackboardService()
	.subscribe(allSupportAssetsPredicate);
  }


  /**
   * Execute method for plugin : 
   * Grab organization out of support assets subscription when ready
   * Allocate any new SCHEDULE tasks to support organization
   * Reschedule any tasks that failed to allocate 
   **/
  public void execute() {
    //    System.out.println("CalendarAllocatorPlugin::execute");

    // Grab the first support asset as the organization to whom we'll be
    // allocating the schedule tasks
    Organization mySupportOrganization =
      (Organization)TutorialUtils.getFirstObject(allSupportAssets.elements());

    // System.out.println("mySupprtOrganization = " + mySupportOrganization);

    // Allocate any new SCHEDULE task to the support organization
    if (mySupportOrganization != null) {
      // Take all tasks and allocate it to the asset
      for(Enumeration e = allScheduleTasks.getAddedList(); e.hasMoreElements();)
      {
        // System.out.println("Got a new task");

        Task task = (Task)e.nextElement();

        if (task.getPlanElement() == null) {
          Allocation allocation = ldmf
	      .createAllocation(task.getPlan(), task,
				mySupportOrganization, null,
				Role.AVAILABLE);
          //	  System.out.println("Allocating task " + task + " to  " +
          //			     mySupportOrganization);
          getBlackboardService().publishAdd(allocation);
        }
      }
    }

    // Iterate over all changed allocations and look at results
    for(Enumeration e = allScheduleAllocations.getChangedList();e.hasMoreElements();)
    {
      Allocation alloc = (Allocation)e.nextElement();
      AllocationResult result = alloc.getReportedResult();
      if (result != null) {
        if (result.isSuccess()) {
          // Requester is notified that the task was scheduled
          // within the preference window
          System.out.println(CalendarUtils.getAllocationStatus(alloc));
        }
        else {
            // If there is no remote cluster (mySupportOrganization==null),
            // defer action until the CalendarManager has
            // a chance to try to reschedule.  He copies the reported result to the
            // estimated result.
          if ((mySupportOrganization != null) || (alloc.getReportedResult() == alloc.getEstimatedResult())) {
            // task must be rescheduled, all days were unavailable
            Task task = alloc.getTask();
            System.out.println("All dates taken.  Attempting to reschedule task");
            CalendarUtils.modifyPreferences(task, ldmf);
            getBlackboardService().publishChange(task);  // Change to preferences will propagate
          }
        }
      }
    }
  }
}

package org.cougaar.tutorial.faststart.calendar;

/**
 * Copyright 1997-1999 Defense Advanced Research Project Agency 
 * and ALPINE (A Raytheon Systems Company and BBN Corporation Consortium). 
 * This software to be used in accordance with the COUGAAR license agreement.
 */

import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.domain.planning.ldm.asset.*;
import org.cougaar.tutorial.faststart.*;
import org.cougaar.util.UnaryPredicate;
import java.util.*;

/**
 * Class to allocate 'Schedule' tasks to the supporting organization
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: CalendarAllocatorPlugIn.java,v 1.1 2000-12-15 20:19:04 mthome Exp $
 **/
public class CalendarAllocatorPlugIn extends org.cougaar.core.plugin.SimplePlugIn
{

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
    // System.out.println("CalendarAllocatorPlugIn::setupSubscriptions");

    allScheduleTasks = 
      (IncrementalSubscription)subscribe(allScheduleTasksPredicate);
    allScheduleAllocations = 
      (IncrementalSubscription)subscribe(allScheduleAllocationsPredicate);
    allSupportAssets = 
      (IncrementalSubscription)subscribe(allSupportAssetsPredicate);
  }


  /**
   * Execute method for plugin : 
   * Grab organization out of support assets subscription when ready
   * Allocate any new SCHEDULE tasks to support organization
   * Reschedule any tasks that failed to allocate 
   **/
  public void execute() {
    //    System.out.println("CalendarAllocatorPlugIn::execute");

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
          Allocation allocation =
            theLDMF.createAllocation(task.getPlan(), task,
          mySupportOrganization, null,
          Role.AVAILABLE);
          //	  System.out.println("Allocating task " + task + " to  " +
          //			     mySupportOrganization);
          publishAdd(allocation);
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
            CalendarUtils.modifyPreferences(task, theLDMF);
            publishChange(task);  // Change to preferences will propagate
          }
        }
      }
    }
  }
}

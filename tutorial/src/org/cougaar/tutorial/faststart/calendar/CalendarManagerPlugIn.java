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
 * Plugin to manage the calendar assets for schedule requests, giving
 * an answer of when the scheduling was for, or that it was unfulfilled
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: CalendarManagerPlugIn.java,v 1.2 2001-01-30 21:10:25 wwright Exp $
 **/
public class CalendarManagerPlugIn extends org.cougaar.core.plugin.SimplePlugIn
{

  // Establish subscription for all schedule tasks (NEW and CHANGED)
  private IncrementalSubscription allScheduleTasks;
  private UnaryPredicate allScheduleTasksPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return TutorialUtils.isTaskWithVerb(o, CalendarUtils.SCHEDULE_VERB);
    }
  };

  // Setup subscription for changed allocation results with verb = SCHEDULE
  // And the estimated result is different from reported
  private IncrementalSubscription allScheduleAllocations;
  private UnaryPredicate allScheduleAllocationsPredicate = new UnaryPredicate()
  {
    public boolean execute(Object o) {
      if (o instanceof Allocation) {
        Allocation alloc = (Allocation)o;
        return (alloc.getTask().getVerb().equals(CalendarUtils.SCHEDULE_VERB))
          &&
            (alloc.getReportedResult() != alloc.getEstimatedResult());
      }
      return false;
    }
  };

  /**
   * Set up subscriptions for tasks and for changes to allocations
   * And publish log plan
   */
  public void setupSubscriptions()
  {
    //    System.out.println("CalendarManagerPlugIn::setupSubscriptions");
    allScheduleTasks =
      (IncrementalSubscription)subscribe(allScheduleTasksPredicate);
    allScheduleAllocations =
      (IncrementalSubscription)subscribe(allScheduleAllocationsPredicate);

    // Publish the CalendarAsset  to logplan so others can see it
    theCalendar = (CalendarAsset)theLDMF.createAsset(org.cougaar.tutorial.faststart.calendar.CalendarAsset.class);
    publishAdd(theCalendar);
  }


  public void execute() 
  {
    // System.out.println("CalendarManagerPlugIn::execute");

    // Look at new and changed incoming tasks and schedule them
    for(Enumeration e = allScheduleTasks.getAddedList();e.hasMoreElements();)
    {
      //	System.out.println("Got a new task");

      Task task = (Task)e.nextElement();

      scheduleAndAllocateTask(task);
    }

    // Look at changed incoming tasks and schedule them too
    for(Enumeration e = allScheduleTasks.getChangedList(); e.hasMoreElements();)
    {
      //	System.out.println("Got a changed task");

      Task task = (Task)e.nextElement();

      // If task is already allocated, get rid of the allocation
      if (task.getPlanElement() != null) {
        PlanElement plan_element = task.getPlanElement();
        //	  System.out.println("Deleting PE : " + plan_element);

        // This should also set the PE of the task to null
        publishRemove(plan_element);
      }

      // Now that we know there is no plan element, we can schedule it
      scheduleAndAllocateTask(task);

    }

    // Look at changed allocations and copy failure news into estimate
    for(Enumeration e = allScheduleAllocations.getChangedList();e.hasMoreElements();)
    {
      // System.out.println("Got a changed allocation");
      Allocation alloc = (Allocation)e.nextElement();
      // try to reshcedule using the original dates
      rescheduleTask(alloc);

      alloc.setEstimatedResult(alloc.getReportedResult());
      publishChange(alloc);
    }
  }

  /**
   * Attempt to find a free day within the preferences.  If one is available, the
   * allocation and calendar are both updated.
   * @param alloc The allocation containing the task to reschedule.
   * @return true iff the task has been rescheduled
   */
  private boolean rescheduleTask(Allocation alloc) {
    Task task = alloc.getTask();
    int new_day = scheduleTask(task);
    boolean success = new_day >= 0;

    if (success) {
      // Create allocation Result for success/failure
      AllocationResult allocation_result =
        CalendarUtils.createAllocationResult(new_day, success, theLDMF);

      // Modify the allocation with result indicating success
      ((PlanElementForAssessor)alloc).setReceivedResult(allocation_result);
      publishChange(alloc);

      // we've successfully allocated, register change to calendar as well
      theCalendar.setAssignment(new_day, alloc);
      publishChange(theCalendar);
    }
    return success;

  }

  /**
   * Attempt to find a free day within the task preferences.  If one is available,
   * it is returned. Otherwise -1 is returned.  Neither the task nor the calendar
   * are updated.
   * @param the task to schedule.
   * @return the next available day or (-1) if no days are available.
   */
  private int scheduleTask(Task task) {
    // Get bounds from preferences, and try each day in sequence
    // For a free day. If we find one, great - its a succcess, and
    // book it, otherwise, its a failure, tell him to request again
    // Assume that task is without associated plan element when called
    double []day_bounds = TutorialUtils.getPreferredValueBounds
      (task, AspectType.START_TIME, 0.0);
    int low_day = (int)day_bounds[0];
    int high_day = (int)day_bounds[1];
    boolean success = false;
    int scheduled_day = -1;

    // Find a day in the range that the calendar is free, and book it.
    for(int day = low_day; day <= high_day; day++) {
      if (theCalendar.getAssignment(day) == null) {
        scheduled_day = day;
        System.out.println("Scheduling task for day " + scheduled_day);
        success = true;
        break;
      }
    }
    return scheduled_day;
  }

  /**
   * Schedule a new task.  A new allocation is created reflecting the
   * success/failure of the scheduling operation.  If the scheduling
   * succeeds, the calendar is updated.
   */
  private void scheduleAndAllocateTask(Task task) {
    int scheduled_day = scheduleTask(task);
    boolean success = scheduled_day >= 0;

    // Create allocation Result for success/failure
    AllocationResult allocation_result =
      CalendarUtils.createAllocationResult(scheduled_day, success, theLDMF);

    // Create an allocation with result indicating success/failure
    Allocation allocation =
      theLDMF.createAllocation(task.getPlan(), task, theCalendar,
    allocation_result,
    Role.AVAILABLE);
    publishAdd(allocation);

    // If we've successfully allocated, register change to calendar as well
    if (success) {
      theCalendar.setAssignment(scheduled_day, allocation);
      publishChange(theCalendar);
    }
  }

  // Calendar object to store current state of assignments
  private CalendarAsset theCalendar;

}

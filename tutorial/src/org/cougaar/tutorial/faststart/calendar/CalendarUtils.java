package org.cougaar.tutorial.faststart.calendar;

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
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.tutorial.faststart.*;

/**
 * Set of static methods and constants for supporting
 * Calendar society
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: CalendarUtils.java,v 1.5 2003-01-23 22:12:55 mbarger Exp $
 **/
public class CalendarUtils {

  // Verb for scheduling
  public static final String SCHEDULE_VERB = "SCHEDULE";

  // Name of organization support role provided by manager
  public static String CalendarServiceRole = "ScheduleManager";

  /**
   * Provide a task requesting scheduling.
   * Verb = SCHEDULE
   * Preference = A day in some random range of days
   * @param ClusterObjectFactory from which to generate task
   * @return Task requesting scheduling in some day range
   */
  public static Task createTask(ClusterObjectFactory theCOF) 
  {
    NewTask task = theCOF.newTask();
    
    task.setDirectObject(null);
    task.setVerb(new Verb(SCHEDULE_VERB));
    Vector preps = new Vector();
    task.setPrepositionalPhrases(preps.elements());
    task.setPlan(theCOF.getRealityPlan());

    modifyPreferences(task, theCOF);

    return task;
  }

  /**
   * Create an allocation result for given day and given success
   * @param int scheduled_day for which task is scheduled (if success)
   * @param boolean indicating if scheduling was successful
   * @param ClusterObjectFactory from which to create result
   * @return AllocationResult with proper fields filled in
   **/
  public static AllocationResult createAllocationResult
  (int scheduled_day,
   boolean success,
   ClusterObjectFactory theCOF)
  {
    AspectValue avs[] = new AspectValue[1];
    avs[0] = AspectValue.newAspectValue(AspectType.START_TIME, (double)scheduled_day);
    AllocationResult allocation_result = 
      theCOF.newAllocationResult(1.0, // rating,
				 success, 
				 avs); 
    return allocation_result;
  }

    private static final int CALENDAR_HORIZON_DAYS = 100;
    private static final int REQUEST_WINDOW_MAX = 7;

  /**
   * Modify preferences (requirements for scheduling of task) to try again
   * Pick random days between 0 and CALENDAR_HORIZON_DAYS
   * and a random range between 0 and REQUEST_WINDOW_MAX days
   * @param Task whose preferences are to be modified
   * @param ClusterObjectFactory from which to create preferences
   **/
  public static void modifyPreferences(Task task, ClusterObjectFactory theCOF) 
  {

    int low_day = (int)(Math.random() * CALENDAR_HORIZON_DAYS);
    int range = (int)(Math.random() * REQUEST_WINDOW_MAX);
    System.out.println("Requesting between days " + 
		       low_day + " - " + (low_day + range));
    Vector newPreferences = new Vector();
    ScoringFunction scorefcn = 
      ScoringFunction.createStrictlyBetweenValues
      (AspectValue.newAspectValue(AspectType.START_TIME, low_day), 
       AspectValue.newAspectValue(AspectType.START_TIME, low_day + range));
    Preference pref = theCOF.newPreference(AspectType.START_TIME, scorefcn);
    newPreferences.addElement(pref);
    ((NewTask)task).setPreferences(newPreferences.elements());
  }

  /**
   * Creates a string representation of the requested date range and
   * the currently scheduled date.
   * @param alloc the allocation containing the task to be scheduled.
   * @return a string showing the preference bounds and the currently
   *         scheduled day for the task.
   */
  public static String getAllocationStatus(Allocation alloc) {
    Task task = alloc.getTask();
    double []day_bounds = TutorialUtils.getPreferredValueBounds
      (task, AspectType.START_TIME, 0.0);
    int low_day = (int)day_bounds[0];
    int high_day = (int)day_bounds[1];
    AllocationResult ar = alloc.getReportedResult();
    AspectValue [] scheduled_day = ar.getAspectValueResults();

    return "Task " + task.getUID() + " Bounds: ["+low_day+","+high_day+"] Scheduled for: "+ (int)scheduled_day[0].getValue();

  }
}

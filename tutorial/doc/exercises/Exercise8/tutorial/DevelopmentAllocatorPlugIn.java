/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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
package tutorial;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.util.UnaryPredicate;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Collection;

import tutorial.assets.*;


/**
 * This COUGAAR PlugIn subscribes to tasks in a workflow and allocates
 * the workflow sub-tasks to programmer assets.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: DevelopmentAllocatorPlugIn.java,v 1.4 2002-01-15 20:20:06 cbrundic Exp $
 **/
public class DevelopmentAllocatorPlugIn extends ComponentPlugin
{
  // The domainService acts as a provider of domain factory services
  private DomainService domainService = null;

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

  private IncrementalSubscription allMyTasks;   // Tasks that I'm interested in
  private IncrementalSubscription allProgrammers;  // Programmer assets that I allocate to

  /**
   * Predicate matching all ProgrammerAssets
   */
  private UnaryPredicate allProgrammersPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof ProgrammerAsset;
    }
  };

  /**
   * Predicate that matches all of the tasks I'm interested in
   */
  private UnaryPredicate taskPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task)
      {
// todo: subscribe to DESIGN, DEVELOP, and TEST tasks
      }
      return false;
    }
  };

  /**
   * Establish subscription for tasks, allocations, and assets
   **/
  public void setupSubscriptions() {
    allProgrammers =
      (IncrementalSubscription)getBlackboardService().subscribe(allProgrammersPredicate);
    allMyTasks =
      (IncrementalSubscription)getBlackboardService().subscribe(taskPredicate);
  }

  /**
   * Top level plugin execute loop.  Handle changes to my subscriptions.
   **/
  public void execute() {
    System.out.println("DevelopmentAllocatorPlugIn::execute");

    // todo: Plan any new software development tasks with a start month but no allocation

  }


  /**
   * Extract the start month from a task
   */
  private int startMonth(Task t) {
      int ret = -1;
      Preference start_pref = t.getPreference(AspectType.START_TIME);
      if (start_pref != null)
        ret = (int)start_pref.getScoringFunction().getBest().getValue();
      return ret;
  }

  /**
   * Find an available ProgrammerAsset for this task.  Task must be scheduled
   * after the month "after"
   */
  private int allocateTask(Task task, int after) {
    int end = after;
      // select an available programmer at random
    Vector programmers = new Vector(allProgrammers.getCollection());
    boolean allocated = false;
    while ((!allocated) && (programmers.size() > 0)) {
      int stuckee = (int)Math.floor(Math.random() * programmers.size());
      ProgrammerAsset asset = (ProgrammerAsset)programmers.elementAt(stuckee);
      programmers.remove(asset);

      System.out.println("\nAllocating the following task to "
          +asset.getTypeIdentificationPG().getTypeIdentification()+": "
          +asset.getItemIdentificationPG().getItemIdentification());
      System.out.println("Task: "+task);

      Preference duration_pref = task.getPreference(AspectType.DURATION);
      Preference end_time_pref = task.getPreference(AspectType.END_TIME);
      Schedule sched = asset.getSchedule();
      int duration = (int)duration_pref.getScoringFunction().getBest().getValue();
      int desired_delivery = (int)end_time_pref.getScoringFunction().getBest().getValue();

      // Check the programmer's schedule
      int earliest = findEarliest(sched, after, duration);

      end = earliest + duration -1;

      // Add the task to the programmer's schedule
      for (int i=earliest; i<=end; i++) {
        sched.setWork(i, task);
      }
      getBlackboardService().publishChange(asset);

      AllocationResult estAR = null;

      // Create an estimate that reports that we did just what we
      // were asked to do

      boolean onTime = (end <= desired_delivery);
      String tmpstr =  " start_month: "+earliest;
      tmpstr +=  " duration: "+duration;
      tmpstr +=  " end_month: "+end;
      tmpstr +=  " desired_delivery: "+desired_delivery;
      tmpstr +=  " onTime: "+onTime;
      System.out.println(tmpstr);

      int []aspect_types = {AspectType.START_TIME, AspectType.END_TIME, AspectType.DURATION};
      double []results = {earliest, end, duration};
      estAR =  getDomainService().getFactory().newAllocationResult(1.0, // rating
                  onTime, // success or not
                  aspect_types,
                  results);

      Allocation allocation =
        getDomainService().getFactory().createAllocation(task.getPlan(), task, asset, estAR, Role.ASSIGNED);

      getBlackboardService().publishAdd(allocation);
      allocated = true;
    }
    return end;
  }

  /**
   * find the earliest available time in the schedule.
   * @param sched the programmer's schedule
   * @param earliest the earliest month to look for
   * @param duration the number of months we want to schedule
   */
  private int findEarliest(Schedule sched, int earliest, int duration) {
    boolean found = false;
    int month = earliest;
    while (!found) {
      found = true;
      for (int i=month; i<month+duration; i++) {
        if (sched.getWork(i) != null) {
          found = false;
          month = i+1;
          break;
        }
      }
    }
    return month;
  }

}


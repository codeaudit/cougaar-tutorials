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
package tutorial;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.*;
import java.util.*;
import org.cougaar.planning.ldm.PlanningFactory;

import tutorial.assets.*;


/**
 * This COUGAAR Plugin subscribes to tasks and allocates
 * to programmer assets.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: DevelopmentAllocatorPlugin.java,v 1.1 2003-04-16 22:26:02 dmontana Exp $
 **/
public class DevelopmentAllocatorPlugin extends ComponentPlugin
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

  private IncrementalSubscription allCodeTasks;   // Tasks that I'm interested in
  private IncrementalSubscription allProgrammers;  // Programmer assets that I allocate to
  private IncrementalSubscription allExpansions;  // All expansions
  private IncrementalSubscription myAllocations;  // All expansions

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
        Task task = (Task)o;
        Verb tVerb = task.getVerb();
        if (   Verb.getVerb("DESIGN").equals(tVerb) ||
               Verb.getVerb("DEVELOP").equals(tVerb) ||
               Verb.getVerb("TEST").equals(tVerb))
          return true;
      }
      return false;
    }
  };

  /**
   * Predicate that matches all of allocations that I made
   */
  private UnaryPredicate allocPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Allocation)
      {
        Allocation allo = (Allocation)o;
        Task task = allo.getTask();
        if (task != null)
          return taskPredicate.execute(task);
      }
      return false;
    }
  };

  /**
   * Predicate that matches all of expansions
   */
  private UnaryPredicate expansionPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof Expansion;
    }
  };

  /**
   * Establish subscription for tasks and assets
   **/
  public void setupSubscriptions() {
    allProgrammers =
      (IncrementalSubscription)getBlackboardService().subscribe(allProgrammersPredicate);
    allCodeTasks =
      (IncrementalSubscription)getBlackboardService().subscribe(taskPredicate);
    allExpansions =
      (IncrementalSubscription)getBlackboardService().subscribe(expansionPredicate);
    myAllocations =
      (IncrementalSubscription)getBlackboardService().subscribe(allocPredicate);
  }

  /**
   * Top level plugin execute loop.  Handle changes to my subscriptions.
   **/
  public void execute() {
    System.out.println("DevelopmentAllocatorPlugin::execute");

    // process one task at a time until can't anymore
    boolean anyLeft = true;
    while (anyLeft) {
      anyLeft = false;
      Enumeration task_enum = allCodeTasks.elements();
      while (task_enum.hasMoreElements()) {
        Task task = (Task)task_enum.nextElement();
        if ((task.getPlanElement() == null) &&
            isReady (task)) {
          anyLeft = true;
          Task t = findConstraining (task);
          allocateTask (task, (t == null) ? 0L :
                   ((Allocation) t.getPlanElement()).getEndTime());
          break;
        }
      }
    }
  }

  private boolean isReady (Task task) {
    Task t = findConstraining (task);
    return (t == null) || (t.getPlanElement() != null);
  }

  private Task findConstraining (Task task) {
    Enumeration enum = allExpansions.elements();
    while (enum.hasMoreElements()) {
      Expansion exp = (Expansion) enum.nextElement();
      Enumeration enum2 = exp.getWorkflow().getTaskConstraints (task);
      while (enum2.hasMoreElements()) {
        Constraint c = (Constraint) enum2.nextElement();
        if (task == c.getConstrainedTask())
          return c.getConstrainingTask();
      }
    }
    return null;
  }

  /**
   * Find an available ProgrammerAsset for this task.  Task must be scheduled
   * after the month "after"
   */
  private void allocateTask(Task task, long prevEnd) {
    // extract from preferences
    Preference ePref = task.getPreference(AspectType.START_TIME);
    Preference dPref = task.getPreference(AspectType.DURATION);
    Preference lPref = task.getPreference(AspectType.END_TIME);
    long earliest = (long) ePref.getScoringFunction().getBest().getValue();
    int duration = (int) dPref.getScoringFunction().getBest().getValue();
    long latest = (long) lPref.getScoringFunction().getBest().getValue();
    boolean possiblyAtBeginning = prevEnd <= earliest;
    earliest = Math.max (earliest, prevEnd);

    // select best available programmer
    ProgrammerAsset bestAsset = null;
    long bestTime = 0L;
    Vector programmers = new Vector(allProgrammers.getCollection());
    while (programmers.size() > 0) {
      int stuckee = (int)Math.floor(Math.random() * programmers.size());
      ProgrammerAsset asset = (ProgrammerAsset)programmers.elementAt(stuckee);
      programmers.remove(asset);
      if ((possiblyAtBeginning && asset.getRoleSchedule().isEmpty()) ||
          ((! asset.getRoleSchedule().isEmpty()) &&
           (asset.getRoleSchedule().getEndTime() >= earliest))) {
        long newTime = asset.getRoleSchedule().isEmpty() ? earliest :
                       asset.getRoleSchedule().getEndTime();
        if ((bestAsset == null) || (newTime < bestTime)) {
          bestAsset = asset;
          bestTime = newTime;
        }
      }
    }

    System.out.println("\nAllocating the following task to "
        +bestAsset.getTypeIdentificationPG().getTypeIdentification()+": "
        +bestAsset.getItemIdentificationPG().getItemIdentification());
    System.out.println("Task: "+task);

    // find the times and make the allocation result that
    // assigns these times
    // if can't fit, go on to next programmer
    AspectValue[] inter = findInterval (bestAsset, earliest, latest, duration);
    boolean success = inter != null;
    if (! success)
      inter = new AspectValue[] {
        AspectValue.newAspectValue (AspectType.START_TIME, latest),
        AspectValue.newAspectValue (AspectType.END_TIME, latest) };
    AllocationResult estAR = new AllocationResult (1.0, success, inter);
    Allocation allocation =
      ((PlanningFactory)getDomainService().getFactory("planning")).
        createAllocation (task.getPlan(), task,
                          bestAsset, estAR, Role.ASSIGNED);

    getBlackboardService().publishAdd(allocation);
  }

  /**
   * Find the three-month interval starting either the beginning of
   * next month or the end of the last task on the asset, and
   * return an array of aspect values indicating the time interval
   */
  private AspectValue[] findInterval (Asset asset, long earliest,
                                      long latest, int durationMonths) {
    // figure out time interal, inserting at earliest possible time
    RoleSchedule sched = asset.getRoleSchedule();
    long start = sched.isEmpty() ? earliest :
                 Math.max (earliest, sched.getEndTime());
    GregorianCalendar cal = new GregorianCalendar();
    cal.setTime (new Date (start));
    cal.add (GregorianCalendar.MONTH, durationMonths);
    long end = cal.getTime().getTime();
    String str = " start: " + new Date (start) + " end: " + new Date (end);

    // check that does not violate constraint
    if (end > latest) {
      System.out.println (" Cannot schedule with" + str);
      return null;
    }

    // tell the dates chosen and return the aspect values
    System.out.println (str);
    return new AspectValue[] {
      AspectValue.newAspectValue (AspectType.START_TIME, start),
      AspectValue.newAspectValue (AspectType.END_TIME, end) };
  }

}

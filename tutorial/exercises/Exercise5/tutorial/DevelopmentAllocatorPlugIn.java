/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package tutorial;

import alp.cluster.IncrementalSubscription;
import alp.ldm.plan.*;
import alp.ldm.asset.Asset;
import alp.util.UnaryPredicate;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Collection;

import tutorial.assets.*;


/**
 * This ALP PlugIn subscribes to tasks and allocates
 * to programmer assets.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: DevelopmentAllocatorPlugIn.java,v 1.1 2000-12-15 20:18:58 mthome Exp $
 **/
public class DevelopmentAllocatorPlugIn extends alp.plugin.SimplePlugIn
{
  private IncrementalSubscription allCodeTasks;   // Tasks that I'm interested in
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
   * Predicate that matches all Test tasks
   */
  private UnaryPredicate codeTaskPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task)
      {
        Task task = (Task)o;
        return task.getVerb().equals(Verb.getVerb("CODE"));
      }
      return false;
    }
  };


  /**
   * Establish subscription for tasks and assets
   **/
  public void setupSubscriptions() {
    allProgrammers =
      (IncrementalSubscription)subscribe(allProgrammersPredicate);
    allCodeTasks =
      (IncrementalSubscription)subscribe(codeTaskPredicate);
  }

  /**
   * Top level plugin execute loop.  Handle changes to my subscriptions.
   **/
  public void execute() {
    System.out.println("DevelopmentAllocatorPlugIn::execute");

    // process unallocated tasks
    Enumeration task_enum = allCodeTasks.elements();
    while (task_enum.hasMoreElements()) {
      Task task = (Task)task_enum.nextElement();
      if (task.getPlanElement() == null)
        allocateTask(task, startMonth(task));
    }
  }

  /**
   * Extract the start month from a task
   */
  private int startMonth(Task t) {
      return 0;
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

      Schedule sched = asset.getSchedule();

      // Check the programmer's schedule
      int duration = 3;
      int earliest = findEarliest(sched, after, duration);

      end = earliest + duration;

      // Add the task to the programmer's schedule
      for (int i=earliest; i<end; i++) {
        sched.setWork(i, task);
      }
      publishChange(asset);

      AllocationResult estAR = null;

      // Create an estimate that reports that we did just what we
      // were asked to do
      String tmpstr =  " start_month: "+earliest;
      tmpstr +=  " duration: "+duration;
      tmpstr +=  " end_month: "+end;
      System.out.println(tmpstr);

      Allocation allocation =
        theLDMF.createAllocation(task.getPlan(), task,
                                  asset, estAR, Role.ASSIGNED);

      publishAdd(allocation);
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


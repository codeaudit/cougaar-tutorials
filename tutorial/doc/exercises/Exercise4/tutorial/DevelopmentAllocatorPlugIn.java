/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package tutorial;

import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.util.UnaryPredicate;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Collection;

import tutorial.assets.*;


/**
 * This COUGAAR PlugIn subscribes to tasks in a workflow and allocates
 * the workflow sub-tasks to programmer assets.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: DevelopmentAllocatorPlugIn.java,v 1.1 2001-08-13 15:45:00 wwright Exp $
 **/
 // todo:  add code to make this a subclass
public class DevelopmentAllocatorPlugIn
{
  // todo:  add instance variables to hold subscriptions






  /**
   * todo: Predicate matching all ProgrammerAssets
   */







  /**
   * todo: Predicate that matches all CODE tasks
   */






  /**
   * todo:  Establish subscription for tasks and assets
   *        Store subscriptions in the instance variables above
   **/
  public void setupSubscriptions() {



  }

  /**
   * Top level plugin execute loop.  Handle changes to my subscriptions.
   **/
  public void execute() {
    System.out.println("DevelopmentAllocatorPlugIn::execute");

    // Plan any new software development tasks
    // todo:  Allocate new tasks using the allocateTask fn later in this file



  }

  /**
   * Extract the start month from a task (zero for now is OK)
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
    // todo:  get a vector containing the programmers
    Vector programmers = // get all of the programmers from the subscription


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
      int duration = 3;   // let's say it takes three months
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

      // todo: Create an allocation and put it on the LogPlan






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


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

import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.util.UnaryPredicate;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Collection;

import tutorial.assets.*;


/**
 * This ALP PlugIn monitors ProgrammerAssets for conflicts between their
 * internal schedule and the tasks allocated to them.  When a conflict is
 * detected, the task allocation results are updated to reflect the conflict.
 *
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: DevelopmentAssessorPlugIn.java,v 1.2 2000-12-18 15:41:04 wwright Exp $
 **/
public class DevelopmentAssessorPlugIn extends org.cougaar.core.plugin.SimplePlugIn
{
  // The set of programmer assets
  private IncrementalSubscription allProgrammers;

  /**
   * Create predicate that matches all programmer assets
   */
  private UnaryPredicate allProgrammersPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof ProgrammerAsset;
    }
  };

  /**
   * Establish subscription for programmer assets
   **/
  public void setupSubscriptions() {
    allProgrammers = (IncrementalSubscription)subscribe(allProgrammersPredicate);
  }

  /**
   * Top level plugin execute loop.  Look at all changed programmer
   * assets and check their schedules.
   **/
  public void execute() {
    System.out.println("DevelopmentAssessorPlugIn::execute");

    // todo:  process all programmer assets that have changed on the PLAN
    // Enumeration e = ...
    while(e.hasMoreElements())
    {
      ProgrammerAsset pa = (ProgrammerAsset)e.nextElement();
      validateSchedule(pa);
    }
  }

  /**
   * Check the programmer's schedule against his assigned tasks.
   * If there are conflicts, update the task's allocation results.
   */
  private void validateSchedule(ProgrammerAsset asset) {
      // todo:  Iterate over allocations in the role schedule and make
      //        sure they're all on the calendar.



      Enumeration re;
      // re = ...
      for(; re.hasMoreElements();)
      {
        Allocation alloc = (Allocation)re.nextElement();
        Task t = alloc.getTask();

        // todo: get estimated start_time and end_time
        int scheduled_start_month ;

        // scheduled_start_month =


        int scheduled_end_month ;

        // scheduled_end_month =



        // look through the months in this role schedule element.  Make sure
        // all of the months are scheduled for the allocated task.
        for (int scheduled_month = scheduled_start_month;
                 scheduled_month < scheduled_end_month;
                 scheduled_month++) {
          if (asset.getSchedule().getWork(scheduled_month) != t) {

            // Conflict : The role_schedule thinks it is allocated
            // for a specific time, but the schedule doesn't think it is.
            // We need to set the reported allocation result to be failure
            System.out.println("Conflict detected : month = " +
              scheduled_month + " " +
              alloc + " " +
              asset.getSchedule().getWork(scheduled_month));

            // todo: publish ReportedResult indicating a failed AllocationResult
            //       (use the next two lines)
            int []aspects = {AspectType.START_TIME, AspectType.END_TIME, AspectType.DURATION};
            double []results = {(double)scheduled_start_month, (double)scheduled_end_month,
                                alloc.getEstimatedResult().getValue(AspectType.DURATION)};

            // add code here ....



          }
        }
      }
  }

}



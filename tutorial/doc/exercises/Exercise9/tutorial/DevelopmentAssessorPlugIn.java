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
 * This COUGAAR PlugIn monitors ProgrammerAssets for conflicts between their
 * internal schedule and the tasks allocated to them.  When a conflict is
 * detected, the task allocation results are updated to reflect the conflict.
 *
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: DevelopmentAssessorPlugIn.java,v 1.4 2002-01-15 20:20:19 cbrundic Exp $
 **/
public class DevelopmentAssessorPlugIn extends ComponentPlugin
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
    allProgrammers = (IncrementalSubscription)getBlackboardService().subscribe(allProgrammersPredicate);
  }

  /**
   * Top level plugin execute loop.  Look at all changed programmer
   * assets and check their schedules.
   **/
  public void execute() {
    System.out.println("DevelopmentAssessorPlugIn::execute");

    Enumeration e = null;
    // todo:  process all programmer assets that have changed on the PLAN
    // e = ...
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



      Enumeration re = null;
      // re = ...
      for(; re.hasMoreElements();)
      {
        Allocation alloc = (Allocation)re.nextElement();
        Task t = alloc.getTask();

        // todo: get estimated start_time and end_time
        int scheduled_start_month = 0;

        // scheduled_start_month =


        int scheduled_end_month  = 0;

        // scheduled_end_month =



        // look through the months in this role schedule element.  Make sure
        // all of the months are scheduled for the allocated task.
        for (int scheduled_month = scheduled_start_month;
                 scheduled_month <= scheduled_end_month;
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


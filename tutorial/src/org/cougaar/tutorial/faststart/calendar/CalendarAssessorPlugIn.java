package org.cougaar.tutorial.faststart.calendar;

/**
 * Copyright 1997-1999 Defense Advanced Research Project Agency 
 * and ALPINE (A Raytheon Systems Company and BBN Corporation Consortium). 
 * This software to be used in accordance with the COUGAAR license agreement.
 */

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.tutorial.faststart.*;
import org.cougaar.util.UnaryPredicate;
import java.util.*;
import org.cougaar.core.service.*;

import org.cougaar.core.plugin.ComponentPlugin;

/**
 * Plugin to Assess the calendar asset for inconsistencies and
 * force replanning when the occur. In this case, a calendar holds
 * its information in two ways which must correspond: 
 * It has a list of allocations in its RoleSchedule, and 
 * it has a list of appointments.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: CalendarAssessorPlugIn.java,v 1.4 2002-02-01 15:43:36 krotherm Exp $
 **/
public class CalendarAssessorPlugIn extends ComponentPlugin
{

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


  // Establish subscription for all changes to calendar assets
  private IncrementalSubscription allCalendarAssets;
  private UnaryPredicate allCalendarAssetsPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof CalendarAsset);
    }
  };

  /**
   * Init method for plugin - subscribe to calendar assets
   **/
  public void setupSubscriptions() 
  {
    //    System.out.println("CalendarAssessorPlugIn::setupSubscriptions");

    // Subscribe to changes to Calendar asset
    allCalendarAssets = 
      (IncrementalSubscription)getBlackboardService()
	.subscribe(allCalendarAssetsPredicate);
  }

  /**
   * Execute method for plugin : 
   *   Grab CalendarAsset when it changes and
   *   change the reported allocation result on when a conflict is found
   **/
  public void execute() 
  {
    //     System.out.println("CalendarAssessorPlugIn::execute");

    // Look at changed calendar asset and see if we need to invalidate
    // reported allocation response
    CalendarAsset asset = (CalendarAsset)TutorialUtils.getFirstObject
      (allCalendarAssets.getChangedList());

    if (asset != null) {

      // Iterate over allocations in the role schedule and make
      // sure they're all on the calendar.
      RoleSchedule role_schedule = asset.getRoleSchedule();
      for(Enumeration re = role_schedule.getRoleScheduleElements();
	  re.hasMoreElements();)
      {
        Allocation alloc = (Allocation)re.nextElement();
        Task t = alloc.getTask();
        int scheduled_day = 
          (int)alloc.getEstimatedResult().getValue(AspectType.START_TIME);

        if (asset.getAssignment(scheduled_day) != alloc) {

          // Conflict : The role_schedule thinks it is allocated
          // for a specific time, but the calendar doesn't think
          // it is.
          // We need to set the reported allocation result to be failure
          System.out.println("Conflict detected : day = " + 
            scheduled_day + " " +
            alloc + " " + 
            asset.getAssignment(scheduled_day));
          AllocationResult failed_result = 
            CalendarUtils.createAllocationResult
            (scheduled_day, false, getDomainService().getFactory());

          // Set reported result on allocation
          // Note : This is an operation intended only for assessors
          // so we require a cast to make coder acknowledge 
          // that it is being done
          ((PlanElementForAssessor)alloc).setReceivedResult(failed_result);
          //	    System.out.println("Publishing alloc");
          getBlackboardService().publishChange(alloc);
        }
      }
    }
  }
}

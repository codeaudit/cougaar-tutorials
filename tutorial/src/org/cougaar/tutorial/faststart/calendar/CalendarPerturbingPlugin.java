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

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.*;
import java.util.*;
import java.awt.event.*;
import org.cougaar.tutorial.faststart.*;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.plugin.ComponentPlugin;

/**
 * Plugin that occassionally declares vacation days in a schedule, 
 * wreaking havoc and forcing replanning
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: CalendarPerturbingPlugin.java,v 1.3 2003-01-23 22:12:54 mbarger Exp $
 */
public class CalendarPerturbingPlugin extends ComponentPlugin
{
  // Establish subscription for all changes to calendar assets
  private IncrementalSubscription allCalendarAssets;
  private UnaryPredicate allCalendarAssetsPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof CalendarAsset);
    }};


  public void setupSubscriptions()
  {
    //    System.out.println("CalendarPerturbingPlugin::setupSubscriptions");

    allCalendarAssets = 
      (IncrementalSubscription)getBlackboardService()
	.subscribe(allCalendarAssetsPredicate);

    // Set up UI button that will grab a vacation day when pressed
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	grabVacation();
      }
    };
    TutorialUtils.createGUI("Grab Vacation Day", "Calendar", listener);
  }

  // Grab a day that is currently scheduled for vacation
  // and grab it for a vacation day. This should cause an assessor
  // to note a discrepancy between the asset and the plan, and 
  // force replanning
  private void grabVacation()
  {
    //    System.out.println("Grabbing vacation...");
    Integer scheduled_day = null;

    // Grab first day that isn't already a vacation
    for(Enumeration scheduled_days = theCalendar.getAssignedDays();
	scheduled_days.hasMoreElements();) {
      scheduled_day = (Integer)scheduled_days.nextElement();
      if (theCalendar.getAssignment(scheduled_day.intValue()) 
	  != VACATION_TEXT) {
	break;
      }
    }

    if (scheduled_day != null) {
      
      // Set the assignment to be null : the day is spoken for, but
      // not with an allocation, but with the String "VACATION" instead
      System.out.println("Grabbing vacation on day " + scheduled_day);
      getBlackboardService().openTransaction();
      theCalendar.setAssignment(scheduled_day.intValue(), VACATION_TEXT);
      getBlackboardService().publishChange(theCalendar);
      getBlackboardService().closeTransactionDontReset();
    }
  }


  /**
   * Execute method for plugin : Grab the calendar asset and hold on
   **/
  public void execute() 
  {
    // System.out.println("CalendarPerturbingPlugin::execute");

    // Get the calendar when it is published, for perturbing later
    if (theCalendar == null) {
      theCalendar = (CalendarAsset)TutorialUtils.getFirstObject
	(allCalendarAssets.getAddedList());
    }
  }

  // String object to hold the place of vacation in date book
  private static String VACATION_TEXT = "Vacation";

  // Hold onto the calendar object
  private CalendarAsset theCalendar;

}

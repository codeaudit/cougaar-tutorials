package org.cougaar.tutorial.faststart;

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
import org.cougaar.planning.ldm.asset.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;


/**
 * Utility class for writing all tutorial plugin classes
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: TutorialUtils.java,v 1.6 2003-01-23 22:12:53 mbarger Exp $
 **/
public class TutorialUtils {

  /**
   * Return the preference for the given aspect
   * @param task for which to return given preference
   * @param int aspect type
   * @return Preference (or null) from task for given aspect
   **/
  public static Preference getPreference(Task task, int aspect_type)
  {
    Preference aspect_pref = null;
    for(Enumeration e = task.getPreferences(); e.hasMoreElements();)
    {
      Preference pref = (Preference)e.nextElement();
      if (pref.getAspectType() == aspect_type) {
        aspect_pref = pref;
        break;
      }
    }
    return aspect_pref;
  }

  /**
   * Return the preferred aspect type value from the preference of
   *  that aspect type. Return provided default if no such aspect type found.
   * @param task from which to get preferred value for aspect type
   * @param aspect type to lookup preferred value
   * @param double default value if not found
   * @return preferred value (or default_value if no such preference listed)
   **/
  public static double getPreferredValue(Task task, 
  int aspect_type, 
  double default_value) {
    double value = default_value;
    Preference pref = getPreference(task, aspect_type);
    if (pref != null)
      value = pref.getScoringFunction().getBest().getAspectValue().getValue();
    return value;
  }

  /**
   * Return 2-tuple of the bounds of given aspect type
   * Return 2-tuple of provided default if no such aspect type found.
   * @param task from which to get preferred value bounds for aspect type
   * @param aspect type to lookup preferred value
   * @param double default value if not found
   * @return array of doubles, lower and upper bounds 
   *      (or default_value if no such preference listed)
   **/
  public static double []getPreferredValueBounds(Task task, 
  int aspect_type, 
  double default_value) 
  {
    double []bounds = {
      default_value, default_value    };
    Enumeration prefs = task.getPreferences();
    while(prefs.hasMoreElements()) {
      Preference pref = (Preference)prefs.nextElement();
      if (pref.getAspectType() == aspect_type) {

        // Get the scoring function and pull out the range values, filling
        // in bounds array

        ScoringFunction func = pref.getScoringFunction();
        for(Enumeration e = 
	      func.getValidRanges(AspectValue.newAspectValue(aspect_type, default_value), 
				  AspectValue.newAspectValue(aspect_type, default_value));
	    e.hasMoreElements();) 
        {
          AspectScoreRange range = (AspectScoreRange)e.nextElement();
          bounds[0] = range.getRangeStartPoint().getValue();
          bounds[1] = range.getRangeEndPoint().getValue();
          break;
        }
        break;
      }
    }

    return bounds;
  }

  /**
   * Return an integer parameter from the head of a list of plugin parameters
   * @param Enumeration of Plugin command line parameters
   * @param integer default value if no numeric value found
   * @return int value parsed from first numeric argument
   */
  public static int getNumericParameter(Enumeration parameters, 
  int default_value) 
  {
    int value = default_value;
    while(parameters.hasMoreElements()) {
      String param = (String)parameters.nextElement();
      try {
        value = Integer.parseInt(param);
        break;
      } 
      catch (NumberFormatException nfe) {
        System.out.println("Error formatting numeric argument : " + param);
      }
    }
    return value;
  }

  /**
   * Return an integer parameter from the head of a list of plugin parameters
   * @param Iterator of Plugin command line parameters
   * @param integer default value if no numeric value found
   * @return int value parsed from first numeric argument
   */
  public static int getNumericParameter(Iterator parameters, 
  int default_value) 
  {
    int value = default_value;
    while(parameters.hasNext()) {
      String param = (String)parameters.next();
      try {
        value = Integer.parseInt(param);
        break;
      } 
      catch (NumberFormatException nfe) {
        System.out.println("Error formatting numeric argument : " + param);
      }
    }
    return value;
  }

  /**
   * Create a simple free-floating GUI button with a label
   */
  public static void createGUI(String button_label, String frame_label, 
  ActionListener listener) 
  {
    JFrame frame = new JFrame(frame_label);
    frame.getContentPane().setLayout(new FlowLayout());
    JPanel panel = new JPanel();
    // Create the button
    JButton button = new JButton(button_label);

    // Register a listener for the button
    button.addActionListener(listener);
    panel.add(button);
    frame.getContentPane().add("Center", panel);
    frame.pack();
    frame.setVisible(true);
  }

  /**
   * Simple main to test 'createGUI'
   **/
  public static void main(String []args) {
    System.out.println("TutorialUtils...");
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("Event = " + e);
      }
    };
    createGUI("Hello", "There", listener);
  }

  /**
   * Grab first asset from a list of assets and return it
   * @param Enumeration of assets from which to return first 
   *    (or null if there are none)
   * @return Object from list
   **/
  public static Object getFirstObject(Enumeration objects) 
  {
    Object object = null;
    if(objects.hasMoreElements()) {
      object = objects.nextElement();
    }
    return object;
  }

  /**
   * Is given object an Organization that has given role
   * among given capable roles?
   * @param Object to test for role
   * @param String role to check for on object
   * @return boolean indicating whether object is Organization with given role
   **/
  public static boolean isSupplierOrganization(Object o, String role)
  {
    if (o instanceof Organization) {
      Organization organization = (Organization)o;
      if (organization.getRelationship().equals(Organization.SUPPLIER_RELATIONSHIP)) {
        for(int i=0; i<organization.getRoles().length; i++) {
           if (organization.getRoles()[i].getName().equals(role)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Is given object a task with given verb?
   * @param Object to test as task with verb
   * @param String verb for object
   * @return boolean indicating if object is a task with given verb
   **/
  public static boolean isTaskWithVerb(Object o, String verb) 
  {
    if (o instanceof Task) {
      Task task = (Task) o;
      return task.getVerb().equals(verb);
    } 
    return false;
  }

  /**
   * Is given object an allocation whose task with given verb?
   * @param Object to test as allocation with task with verb
   * @param String verb for object
   * @return boolean indicating if object is a allocation with given verb/task
   **/
  public static boolean isAllocationWithVerb(Object o, String verb) 
  {
    if (o instanceof Allocation) {
      Allocation alloc = (Allocation) o;
      return alloc.getTask().getVerb().equals(verb);
    } 
    return false;
  }

  /**
   * Is given object a plan_element whose task with given verb?
   * @param Object to test as plan_element with task with verb
   * @param String verb for object
   * @return boolean indicating if object is a PE with given verb/task
   **/
  public static boolean isPlanElementWithVerb(Object o, String verb) 
  {
    if (o instanceof PlanElement) {
      PlanElement plan_element = (PlanElement) o;
      return plan_element.getTask().getVerb().equals(verb);
    } 
    return false;
  }

  // Create a date at a given year/month/day
  public static Date createDate(int year, int month, int day) {
    Calendar my_calendar = Calendar.getInstance();
    my_calendar.set(year, month-1, day, 0, 0, 0);
    return my_calendar.getTime();
  }
}


package org.cougaar.tutorial.faststart.binary;

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

import org.cougaar.planning.ldm.plan.*;
import java.util.*;

/**
 * Class containing static methods and constants useful in constructing 
 * the Binary search tutorial plugins
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: BinaryUtils.java,v 1.4 2003-01-23 22:12:54 mbarger Exp $
 **/
public class BinaryUtils {

  public static final String MANAGE_VERB      = "MANAGE"; 

  // statics defining the potential search space
  public static final int MIN_VALUE = 1;
  public static final int MAX_VALUE = 100;

  // Define aspect for preference on lower and upper bounds on search
  public static final int BINARY_BOUNDS_ASPECT = 1;

  // Name of Binary service provided by responder
  public static final String BinaryServiceRole = "BinaryProvider";

  /**
   * Update preferences on given task to reflect new lower and upper bounds
   * @param Task on which to set preferences
   * @param double low bounds on binary search
   * @param double high bounds on binary search
   * @param ClusterObjectFactory from which to generate preferences
   **/
  public static void UpdatePreferences(Task task, double low_bounds,
				       double high_bounds,
				       ClusterObjectFactory theCOF)
  {
    //    System.out.println("BinaryUtils::UpdatePreferences : new bounds = " + 
    //		       low_bounds +  " => " + high_bounds);

    Vector newPreferences = new Vector();

    ScoringFunction scorefcn = 
      ScoringFunction.createStrictlyBetweenValues
      (AspectValue.newAspectValue(BINARY_BOUNDS_ASPECT, low_bounds), 
       AspectValue.newAspectValue(BINARY_BOUNDS_ASPECT, high_bounds));

    Preference pref = theCOF.newPreference(BINARY_BOUNDS_ASPECT, scorefcn);
    newPreferences.addElement(pref);

    ((NewTask)task).setPreferences(newPreferences.elements());
  }

  /**
   * Create a task to manage a binary search for an externally held value
   * @param ClusterObjectFactory from which to construct task
   * @return new Task to represent a binary search for an externally held value
   **/
  public static Task createNewTask(ClusterObjectFactory theCOF) 
  {

    // Create task and null out unused fields
    NewTask new_task = theCOF.newTask();

    new_task.setDirectObject(null);
    new_task.setPrepositionalPhrases((new Vector()).elements());

    // Set verb of task to 'MANAGE'
    new_task.setVerb(new Verb(BinaryUtils.MANAGE_VERB));

    // Set the reality plan for the task
    new_task.setPlan(theCOF.getRealityPlan());

    // Set initial search bounds (indicated by preferences) to full search space
    UpdatePreferences(new_task, (double)MIN_VALUE, (double)MAX_VALUE, theCOF);

    return new_task;
  }
}

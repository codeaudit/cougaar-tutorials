package org.cougaar.tutorial.faststart.binary;

/**
  * Copyright 1997-1999 Defense Advanced Research Project Agency 
  * and ALPINE (A Raytheon Systems Company and BBN Corporation Consortium). 
  * This software to be used in accordance with the COUGAAR license agreement.
**/

import org.cougaar.planning.ldm.plan.*;
import java.util.*;

/**
 * Class containing static methods and constants useful in constructing 
 * the Binary search tutorial plugins
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: BinaryUtils.java,v 1.3 2002-10-17 19:48:59 mthome Exp $
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

package org.cougaar.tutorial.faststart.computer;

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
import org.cougaar.planning.ldm.asset.*;
import java.util.*;
import org.cougaar.tutorial.faststart.*;
import org.cougaar.tutorial.faststart.computer.assets.ComputerAsset;


/**
 * Package of static utility methods and constants in support
 * of the Computer tutorial lesson.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ComputerUtils.java,v 1.6 2003-04-08 17:43:27 dmontana Exp $
 **/
public class ComputerUtils {

  public static final String SUPPLY_VERB      = "SUPPLY";

  // define aspects over which we will be negotiating for computers
  public static final int PRICE_ASPECT = 1; // Price of computer
  public static final int SHIP_ASPECT = AspectType.START_TIME; // Days to Ship for computer
  public static final int CPU_ASPECT = 3; // Clock Speed of CPU
  public static final int RAM_ASPECT = 4; // Amount of RAM Memory

  /**
   * Create a task to request a computer with particular mix of aspects
   * @param LdmFactory from which to construct task
   * @return new Task to represent request to supply a computer relative
   * to particular preferences
   **/
  public static Task createNewTask( double price_weight,
                                    double ship_weight,
                                    double cpu_weight,
                                    double ram_weight,
                                    ClusterObjectFactory theLDMF)
  {

    // Create task and null out unused fields
    NewTask new_task = theLDMF.newTask();
    new_task.setDirectObject(null);
    new_task.setPrepositionalPhrases((new Vector()).elements());

    // Set verb of task to 'SUPPLY'
    new_task.setVerb(Verb.get(SUPPLY_VERB));

    // Set the reality plan for the task
    new_task.setPlan(theLDMF.getRealityPlan());

    setPreferences(new_task, price_weight, ship_weight, 
    cpu_weight, ram_weight, theLDMF);

    return new_task;
  }

  // Set preferences on tasks to indicate specifically what
  // we mean by 'as early as possible for as cheap as possible
  //   with as much RAM and CPU speed as possible' with weights set on
  // these variables, and limits of what is acceptable, and at what
  // point additional is not of value
  private static void setPreferences( Task task,
                                      double ship_weight,
                                      double price_weight,
                                      double cpu_weight,
                                      double ram_weight,
                                      ClusterObjectFactory theLDMF)
  {
    Vector newPreferences = new Vector();

    // Add a preference for 'as early as possible'
    // Set the 'preferred value' to be zero days to ship, and
    // assess a penaly of .05 days per day later
    // (so that later than 20 days is unacceptable).
    ScoringFunction scorefcn =
      ScoringFunction.createNearOrAbove(AspectValue.newAspectValue(SHIP_ASPECT, 0.0), .05);
    Preference pref =
      theLDMF.newPreference(SHIP_ASPECT, scorefcn, ship_weight);
    newPreferences.addElement(pref);

    // Add a preference for 'as cheap as possible'
    // Set the 'preferred value' to be $1000, and
    // assess a penalty of .0005 for each dollar over
    // (so that $3000 is unacceptable)
    scorefcn =
      ScoringFunction.createNearOrAbove
      (AspectValue.newAspectValue(PRICE_ASPECT, 1000.0), 0.0005);
    pref = theLDMF.newPreference(PRICE_ASPECT, scorefcn, price_weight);
    newPreferences.addElement(pref);

    // Add a preference for 'as much RAM as possible'
    // Set the best to be 512 Mb, with anything less
    // penalized by .0025 so that < 112Mb will be unacceptable
    scorefcn =
      ScoringFunction.createNearOrBelow
      (AspectValue.newAspectValue(RAM_ASPECT, 512), .0025);
    pref = theLDMF.newPreference(RAM_ASPECT, scorefcn, ram_weight);
    newPreferences.addElement(pref);

    // Add a preference for 'as fast a CPU as possible'
    // Set the best to be 400Mhz
    // With anything less penalized by .005 per Mhz so that 200Mhz would
    // be unacceptable
    scorefcn =
      ScoringFunction.createNearOrBelow
      (AspectValue.newAspectValue(CPU_ASPECT, 400), .005);
    pref = theLDMF.newPreference(CPU_ASPECT, scorefcn, cpu_weight);
    newPreferences.addElement(pref);

    ((NewTask)task).setPreferences(newPreferences.elements());
  }


  // Compute the score of hypothetically assigning given
  // asset to given task according to preferences for given task
  public static double computeScore(ComputerAsset asset, Task task)
  {
    int cpu = asset.getCPUPG().getClockSpeed();
    int ram = asset.getMemoryPG().getRAM();
    int price = asset.getMarketPG().getPrice();
    int ship = asset.getMarketPG().getDaysToShip();

    Preference cpu_pref = TutorialUtils.getPreference(task, CPU_ASPECT);
    double cpu_score = cpu_pref.getScoringFunction().getScore
      (AspectValue.newAspectValue(CPU_ASPECT, (double)cpu));
    double cpu_weight = cpu_pref.getWeight();

    Preference ram_pref = TutorialUtils.getPreference(task, RAM_ASPECT);
    double ram_score = ram_pref.getScoringFunction().getScore
      (AspectValue.newAspectValue(RAM_ASPECT, (double)ram));
    double ram_weight = ram_pref.getWeight();

    Preference price_pref = TutorialUtils.getPreference(task, PRICE_ASPECT);
    double price_score = price_pref.getScoringFunction().getScore
      (AspectValue.newAspectValue(PRICE_ASPECT, (double)price));
    double price_weight = price_pref.getWeight();

    Preference ship_pref = TutorialUtils.getPreference(task, SHIP_ASPECT);
    double ship_score = ship_pref.getScoringFunction().getScore
      (AspectValue.newAspectValue(SHIP_ASPECT, (double)ship));
    double ship_weight = ship_pref.getWeight();

    /*
     *     System.out.println("Price : " + price +
     * 		       "(" + price_score + " , " + price_weight + ") " +
     * 		       "Ship : " + ship +
     * 		       "(" + ship_score + " , " + ship_weight + ") " +
     * 		       "CPU : " + cpu +
     * 		       "(" + cpu_score + " , " + cpu_weight + ") " +
     * 		       "RAM : " + ram +
     * 		       "(" + ram_score + " , " + ram_weight + ")");
     * 		       */

    // Return aggregate score over all aspects
    return (price_score*price_weight) +
      (ship_score*ship_weight) +
      (cpu_score*cpu_weight) +
      (ram_score*ram_weight);
  }

  /**
   * Compute an allocation result from given task, success, asset, COF
   * @param Task for which to generate result
   * @param is allocationResult a success?
   * @param Asset from which to pull values of result (if provided)
   * @param ClusterObjectFactory from which to generate result
   * @return AllocationResult
   **/
  public static AllocationResult computeAllocationResult
    (Task task,
  boolean success,
  ComputerAsset asset,
  ClusterObjectFactory theLDMF)
  {
    // Set aspect-by-aspect results (0.0 if no asset provided)
    double price = 0.0;
    double ship = 0.0;
    double cpu = 0.0;
    double ram =0.0;
    if (asset != null) {
      price = asset.getMarketPG().getPrice();
      ship = asset.getMarketPG().getDaysToShip();
      cpu = asset.getCPUPG().getClockSpeed();
      ram = asset.getMemoryPG().getRAM();
    }

    // Set up aspect values
    AspectValue avs[] = new AspectValue[4];
    avs[0] = AspectValue.newAspectValue(PRICE_ASPECT, price);
    avs[1] = AspectValue.newAspectValue(SHIP_ASPECT, ship);
    avs[2] = AspectValue.newAspectValue(CPU_ASPECT, cpu);
    avs[3] = AspectValue.newAspectValue(RAM_ASPECT, ram);

    AllocationResult allocation_result =
    theLDMF.newAllocationResult(1.0, // rating,
    success, // successful or failed allocation
      avs);
    return allocation_result;
  }

}

package org.cougaar.tutorial.faststart.hanoi;

/**
 * Copyright 1997-1999 Defense Advanced Research Project Agency 
 * and ALPINE (A Raytheon Systems Company and BBN Corporation Consortium). 
 * This software to be used in accordance with the COUGAAR license agreement.
 */

import java.util.*;
import org.cougaar.domain.planning.ldm.*;
import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.tutorial.faststart.*;


/**
 * Series of static methods supporting Towers of Hanoi tutorial plugins
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: HanoiUtils.java,v 1.1 2000-12-15 20:19:04 mthome Exp $
 **/
public class HanoiUtils 
{
  public static final String FROM_PREPOSITION = "FROM";
  public static final String TO_PREPOSITION   = "TO";

  public static final String MANAGE_VERB      = "MANAGE"; 
  public static final String TRANSPORT_VERB   = "TRANSPORT"; 

  /**
   * [Level1]
   * Given two poles, which is the third?
   * @param int 'from' pole (1, 2 or 3)
   * @param int 'to' pole (1, 2 or 3)
   * @return int the 'other' pole of the trio
   **/
  public static int computeRemainingPole(int from_pole, int to_pole) 
  {
    if (from_pole == 1) 
      return to_pole == 2 ? 3 : 2;
    else if (from_pole == 2)
      return to_pole == 1 ? 3 : 1;
    else 
      return to_pole == 1 ? 2 : 1;
  }

  /**
   * Create and return a new task reflecting a 
   * need to transport count disks from from_pole to to_pole 
   * @param int 'from' pole (1, 2 or 3)
   * @param int 'to' pole (1, 2 or 3)
   * @param int count : # of disks to move
   * @param double start_time prefered for task [Level3]
   * @param double end_time prefered for task [Level3]
   * @param String verb (Verb.TRANSPORT for many disks 
   *     or Verb.MANAGE for 1 disk)
   * @param Task the parent task of the newly created task
   * @param ClusterObjectFactory from which to generate task
   * @return Task created
   **/
  public static Task createNewTask( int from_pole, int to_pole, int count,
                                    double start_time, double end_time,
                                    String verb,
                                    Task parent,
                                    ClusterObjectFactory theCOF
    )
  {

    // Create task, set parent as given and no direct object
    NewTask new_task = theCOF.newTask();
    if (parent != null)
      new_task.setParentTask(parent);
    new_task.setDirectObject(null);

    // Set up prepositions for task : FROM (pole), TO (pole)
    Vector prepositions = new Vector();

    NewPrepositionalPhrase npp = theCOF.newPrepositionalPhrase();
    npp.setPreposition(FROM_PREPOSITION);
    PolePosition from_location = new PolePosition(from_pole);
    npp.setIndirectObject(from_location);
    prepositions.add(npp);

    npp = theCOF.newPrepositionalPhrase();
    npp.setPreposition(TO_PREPOSITION);
    PolePosition to_location = new PolePosition(to_pole);
    npp.setIndirectObject(to_location);
    prepositions.add(npp);
    new_task.setPrepositionalPhrases(prepositions.elements());

    // Set the verb as given
    new_task.setVerb(new Verb(verb));

    // Set the reality plan for the task
    new_task.setPlan(theCOF.getRealityPlan());


    // Establish preferences for task
    Vector preferences = new Vector();

    // Add quantity preference
    ScoringFunction scorefcn =
      ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.QUANTITY, (double)count));
    Preference pref =
      theCOF.newPreference(AspectType.QUANTITY, scorefcn);
    preferences.add(pref);

    // [Level3]
    // Add a start_time and end_time strict preference
    scorefcn =
      ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.START_TIME, start_time));
    pref =
      theCOF.newPreference(AspectType.START_TIME, scorefcn);
    preferences.add(pref);

    scorefcn =
      ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.END_TIME, end_time));
    pref = theCOF.newPreference(AspectType.END_TIME, scorefcn);
    preferences.add(pref);

    new_task.setPreferences(preferences.elements());
    // End [Level3]

    // And return the task
    return new_task;
  }

  /**
   * Return an integer for the Value of the pole NamedPosition
   * that is the indirect object of the preposition on the
   * given list of prepositional phrases. Return -1 if not found.
   * @param Task containing  prepositional phrases
   * @param String preposition to look for
   * @param int found in NamedPosition value for corresponding preposition
   *    or -1 if not found
   **/
  public static int getPoleValue(Task task, String prep)
  {
    int value = -1;
    PrepositionalPhrase pp = task.getPrepositionalPhrase(prep);
    if (pp != null) {
      value = ((PolePosition)pp.getIndirectObject()).getPosition();
    }
    return value;
  }

}
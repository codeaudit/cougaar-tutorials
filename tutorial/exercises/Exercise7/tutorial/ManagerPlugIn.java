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

import org.cougaar.core.plugin.SimplePlugIn;
import org.cougaar.core.cluster.IncrementalSubscription;
import java.util.*;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.domain.planning.ldm.asset.*;

/**
 * This ALP PlugIn creates and publishes "CODE" tasks
 */
public class ManagerPlugIn extends SimplePlugIn {

  // Two assets to use as direct objects for the CODE tasks
  private Asset what_to_code, what_else_to_code;

  /**
   * Using setupSubscriptions to create the initial CODE tasks
   */
protected void setupSubscriptions() {
  // Create a task to code the next killer app
  what_to_code = theLDMF.createPrototype("AbstractAsset", "The Next Killer App");
  NewItemIdentificationPG iipg = (NewItemIdentificationPG)theLDMF.createPropertyGroup("ItemIdentificationPG");
  iipg.setItemIdentification("e-somthing");
  what_to_code.setItemIdentificationPG(iipg);
  publishAdd(what_to_code);
  publishAdd(makeTask(what_to_code));

  // Create a task to code something java
  what_else_to_code = theLDMF.createInstance(what_to_code);
  iipg = (NewItemIdentificationPG)theLDMF.createPropertyGroup("ItemIdentificationPG");
  iipg.setItemIdentification("something java");
  what_else_to_code.setItemIdentificationPG(iipg);
  publishAdd(what_else_to_code);
  publishAdd(makeTask(what_else_to_code));

  // Create a task to code something java
  what_else_to_code = theLDMF.createInstance(what_to_code);
  iipg = (NewItemIdentificationPG)theLDMF.createPropertyGroup("ItemIdentificationPG");
  iipg.setItemIdentification("something big java");
  what_else_to_code.setItemIdentificationPG(iipg);
  publishAdd(what_else_to_code);
  publishAdd(makeTask(what_else_to_code));

  // Create a task to code something java
  what_else_to_code = theLDMF.createInstance(what_to_code);
  iipg = (NewItemIdentificationPG)theLDMF.createPropertyGroup("ItemIdentificationPG");
  iipg.setItemIdentification("distributed intelligent java agent");
  what_else_to_code.setItemIdentificationPG(iipg);
  publishAdd(what_else_to_code);
  publishAdd(makeTask(what_else_to_code));


}


/**
 * This PlugIn has no subscriptions so this method does nothing
 */
protected void execute () {
}

/**
 * Create a CODE task.
 * @param what the direct object of the task
 */
protected Task makeTask(Asset what) {
    NewTask new_task = theLDMF.newTask();

    // Set the verb as given
    new_task.setVerb(new Verb("CODE"));

    // Set the reality plan for the task
    new_task.setPlan(theLDMF.getRealityPlan());

    new_task.setDirectObject(what);

    // Set up prepositions for task : "USING_LANGUAGE" "C++"
    // Prepositions can add information to how the task should be accomplished
    Vector prepositions = new Vector();

    NewPrepositionalPhrase npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition("USING_LANGUAGE");
    npp.setIndirectObject("C++");
    prepositions.add(npp);
    new_task.setPrepositionalPhrases(prepositions.elements());

    // Establish preferences for task
    // Preferences specify when the task needs to be done
    Vector preferences = new Vector();

    // Add a start_time and end_time strict preference
    double start_month = 0;
    ScoringFunction scorefcn = ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.START_TIME, start_month));
    Preference pref =
      theLDMF.newPreference(AspectType.START_TIME, scorefcn);
    preferences.add(pref);

    double end_month = 12;  // give them one year to do it
    scorefcn = ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.END_TIME, end_month));
    pref = theLDMF.newPreference(AspectType.END_TIME, scorefcn);
    preferences.add(pref);

    double duration = 6;  // it will take them 6 months effort
    scorefcn = ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.DURATION, duration));
    pref = theLDMF.newPreference(AspectType.DURATION, scorefcn);
    preferences.add(pref);

    new_task.setPreferences(preferences.elements());          

    return new_task;
}

}

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
import org.cougaar.domain.planning.ldm.asset.*;
import org.cougaar.domain.planning.ldm.plan.*;

/**
 * This COUGAAR PlugIn creates and publishes "CODE" tasks
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
  iipg.setItemIdentification("intelligent java agent");
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

    return new_task;
}

}

/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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
 * This COUGAAR PlugIn creates and publishes "CODE" tasks
 */
 // todo:  add code to make this a subclass
public class ManagerPlugIn {

  // Two assets to use as direct objects for the CODE tasks
  private Asset what_to_code, what_else_to_code;

  /**
   * todo: Use setupSubscriptions to create the initial CODE tasks
   */
protected void setupSubscriptions() {
  // todo: Create a task to code "the next killer app"
  // todo: Part 1: create the AbstractAsset representing the project.
  //       Store it in instance variable 'what_to_code'

  // todo: Part 2: put the 'what_to_code' asset on the LogPlan

  // todo: Part 3: create and publish a task to CODE the 'what_to_code' asset
  //       (complete and use the makeTask function which is later in this file)

  // todo: Create an task to code "something java"
  //       (store it in instance variable 'what_else_to_code')
}


/**
 * This PlugIn has no subscriptions so this method does nothing
 */
protected void execute () {
}

/**
 * This is a convenience funciont that should create a CODE task.
 * @param what the direct object of the task
 */
protected Task makeTask(Asset what) {
    NewTask new_task;

    // todo: create the task with the verb "CODE"
    // new_task = // todo Part 1: put code here...
    // todo Part 2: ...and put code here



    return new_task;
}

}

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
import tutorial.assets.*;

/**
 * todo: Create a predicate class that matches all ProgrammerAssets
 */

/**
 * todo: Create a predicate class that matches "CODE" tasks
 */

/**
 * This COUGAAR PlugIn allocates tasks of verb "CODE"
 * to ProgrammerAssets
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ManagerAllocatorPlugIn.java,v 1.5 2001-03-29 21:51:39 mthome Exp $
 **/
// todo:  Make ManagerAllocatorPlugIn a subclass
public class ManagerAllocatorPlugIn  {

  private IncrementalSubscription tasks;         // "CODE" tasks
  private IncrementalSubscription programmers;   // Programmers

  /**
   * Subscribe to tasks and programming assets
   */
protected void setupSubscriptions() {
  // todo:  subscribe to CODE tasks
  // tasks =

  // todo:  subscribe to ProgrammerAssets
  // programmers =
}


  /**
   * Top level plugin execute loop.
   * todo:  Allocate CODE tasks to programmers
   */

  /*
  Pseudo Code for one possible approach to the following execute() function:
  for each unallocated task
    pull an available programmer off of the programmers list
    if no programmer is available,
      do nothing.  I will execute() again when a new programmer is published
    else
      allocate the task to the programmer
  */

protected void execute () {

  // process new tasks
  // todo Part 1: get unallocated tasks and allocate them to programmers
  //              Note the allocateTo function later in this file
  // hint:  see IncrementalSubscription.elements()
  // hint:  see IncrementalSubscription.first()
  // hint:  see Task.getPlanElement()
  // hint:  remember Enumeration functions: hasMoreElements(), nextElement()

  }

}

/**
 * This is a convenience function that should allocate the task to the asset
 */
private void allocateTo(Asset asset, Task task) {

	  AllocationResult estAR = null;

    // todo:  Allocate the task to the asset (Note: estAR can remain null)

    // todo:  Put the allocation on the LogPlan

}

}

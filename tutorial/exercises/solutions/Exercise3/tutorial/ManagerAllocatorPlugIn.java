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

import alp.plugin.SimplePlugIn;
import alp.cluster.IncrementalSubscription;
import alp.util.UnaryPredicate;
import java.util.Enumeration;
import alp.ldm.plan.*;
import alp.ldm.asset.*;
import tutorial.assets.*;

/**
 * A predicate that matches all unallocated "CODE" tasks
 */
class myTaskPredicate implements UnaryPredicate{
  public boolean execute(Object o) {
    boolean ret = false;
    if (o instanceof Task) {
      Task t = (Task)o;
      ret = (t.getVerb().equals(Verb.getVerb("CODE"))) &&
            (t.getPlanElement() == null);
    }
    return ret;
  }
}

/**
 * A predicate that matches all ProgrammerAssets
 */
class myProgrammersPredicate implements UnaryPredicate{
  public boolean execute(Object o) {
    return o instanceof ProgrammerAsset;
  }
}

/**
 * This ALP PlugIn allocates tasks of verb "CODE"
 * to ProgrammerAssets
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ManagerAllocatorPlugIn.java,v 1.1 2000-12-15 20:19:01 mthome Exp $
 **/
public class ManagerAllocatorPlugIn extends SimplePlugIn {

  private IncrementalSubscription tasks;         // "CODE" tasks
  private IncrementalSubscription programmers;   // Programmers

  /**
   * subscribe to tasks and programming assets
   */
protected void setupSubscriptions() {
  tasks = (IncrementalSubscription)subscribe(new myTaskPredicate());
  programmers = (IncrementalSubscription)subscribe(new myProgrammersPredicate());
}


  /**
   * Top level plugin execute loop.  Allocate CODE tasks to organizations
   */
protected void execute () {

  // process unallocated tasks
  Enumeration task_enum = tasks.elements();
  while (task_enum.hasMoreElements()) {
    Task t = (Task)task_enum.nextElement();
    Asset programmer = (Asset)programmers.first();
    if (programmer != null)  // if no programmer yet, give up for now
      allocateTo(programmer, t);
  }

}

/**
 * Allocate the task to the asset
 */
private void allocateTo(Asset asset, Task task) {
	  AllocationResult estAR = null;

	  Allocation allocation =
	    theLDMF.createAllocation(task.getPlan(), task, asset,
				     estAR, Role.ASSIGNED);

    System.out.println("\nAllocating the following task to "
          +asset.getTypeIdentificationPG().getTypeIdentification()+": "
          +asset.getItemIdentificationPG().getItemIdentification());
    System.out.println("Task: "+task);

	  publishAdd(allocation);

}

}

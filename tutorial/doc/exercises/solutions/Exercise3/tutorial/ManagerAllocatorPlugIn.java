/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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
package tutorial;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import java.util.Enumeration;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.core.service.*;
import tutorial.assets.*;

/**
 * A predicate that matches all "CODE" tasks
 */
class myTaskPredicate implements UnaryPredicate{
  public boolean execute(Object o) {
    boolean ret = false;
    if (o instanceof Task) {
      Task t = (Task)o;
      ret = t.getVerb().equals(Verb.getVerb("CODE"));
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
 * This COUGAAR PlugIn allocates tasks of verb "CODE"
 * to ProgrammerAssets
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ManagerAllocatorPlugIn.java,v 1.4 2002-01-15 20:21:15 cbrundic Exp $
 **/
public class ManagerAllocatorPlugIn extends ComponentPlugin {

  // The domainService acts as a provider of domain factory services
  private DomainService domainService = null;

  private IncrementalSubscription tasks;         // "CODE" tasks
  private IncrementalSubscription programmers;   // Programmers

  /**
   * Used by the binding utility through reflection to set my DomainService
   */
  public void setDomainService(DomainService aDomainService) {
    domainService = aDomainService;
  }

  /**
   * Used by the binding utility through reflection to get my DomainService
   */
  public DomainService getDomainService() {
    return domainService;
  }

  /**
   * subscribe to tasks and programming assets
   */
protected void setupSubscriptions() {
  tasks = (IncrementalSubscription)getBlackboardService().subscribe(
                                        new myTaskPredicate());
  programmers = (IncrementalSubscription)getBlackboardService().subscribe(
                                        new myProgrammersPredicate());
}


  /**
   * Top level plugin execute loop.  Allocate CODE tasks to organizations
   */
protected void execute () {

  // process unallocated tasks
  Enumeration task_enum = tasks.elements();
  while (task_enum.hasMoreElements()) {
    Task t = (Task)task_enum.nextElement();
    if (t.getPlanElement() != null)
      continue; // already allocated.
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
      getDomainService().getFactory().createAllocation(task.getPlan(), task,
				     asset, estAR, Role.ASSIGNED);

    System.out.println("\nAllocating the following task to "
          +asset.getTypeIdentificationPG().getTypeIdentification()+": "
          +asset.getItemIdentificationPG().getItemIdentification());
    System.out.println("Task: "+task);

	  getBlackboardService().publishAdd(allocation);

}

}

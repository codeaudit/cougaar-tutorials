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
package tutorial;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.blackboard.IncrementalSubscription;
import java.util.*;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.service.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.OrganizationPG;
import tutorial.assets.*;
import org.cougaar.planning.ldm.PlanningFactory;

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
 * A predicate that matches allocations of "CODE" tasks
 */
class myAllocationPredicate implements UnaryPredicate{
  public boolean execute(Object o) {
    boolean ret = false;
    if (o instanceof Allocation) {
      Task t = ((Allocation)o).getTask();
      ret = (t != null) && (t.getVerb().equals(Verb.getVerb("CODE")));
    }
    return ret;
  }
}

/**
 * A predicate that matches all organizations that can
 * fulfill the SoftwareDevelopment role
 */
class myProgrammersPredicate implements UnaryPredicate{
  public boolean execute(Object o) {
    boolean ret = false;
    if (o instanceof Organization) {
      Organization org = (Organization)o;
      OrganizationPG orgPG = org.getOrganizationPG();
      ret = orgPG.inRoles(Role.getRole("SoftwareDevelopment"));
    }
    return ret;
  }
}

/**
 * This COUGAAR Plugin allocates tasks of verb "CODE"
 * to Organizations that have the "SoftwareDevelopment" role.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ManagerAllocatorPlugin.java,v 1.1 2003-04-16 22:18:27 dmontana Exp $
 **/
public class ManagerAllocatorPlugin extends ComponentPlugin {

  // The domainService acts as a provider of domain factory services
  private DomainService domainService = null;

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

  private IncrementalSubscription tasks;         // "CODE" tasks
  private IncrementalSubscription programmers;   // SoftwareDevelopment orgs
  private IncrementalSubscription allocations;   // My allocations

  /**
   * subscribe to tasks and programming organizations
   */
protected void setupSubscriptions() {
  tasks = (IncrementalSubscription)getBlackboardService().subscribe(new myTaskPredicate());
  programmers = (IncrementalSubscription)getBlackboardService().subscribe(new myProgrammersPredicate());
  allocations = (IncrementalSubscription)getBlackboardService().subscribe(new myAllocationPredicate());
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
      continue;
    Asset organization = (Asset)programmers.first();
    if (organization != null)  // if no organization yet, give up for now
      allocateTo(organization, t);
  }

  // Process changed allocations
  AllocationResult est, rep;
  Enumeration allo_enum = allocations.getChangedList();
  while (allo_enum.hasMoreElements()) {
    Allocation alloc = (Allocation)allo_enum.nextElement() ;
    est=null; rep=null;
    System.out.println("MANAGER ALLOCATOR: Allocation changed: "+alloc);
    est = alloc.getEstimatedResult();
    rep = alloc.getReportedResult();
    System.out.println("MANAGER ALLOCATOR: Estimated Allocation Result: "+est);
    if (est!=null)
      System.out.println("MANAGER ALLOCATOR: Estimated Allocation Result success? "+est.isSuccess());
    System.out.println("MANAGER ALLOCATOR: Reported Allocation Result: "+rep);
    if (rep!=null)
      System.out.println("MANAGER ALLOCATOR: Reported Allocation Result success? "+rep.isSuccess());
  }
}

/**
 * Allocate the task to the asset
 */
private void allocateTo(Asset asset, Task task) {

	  AllocationResult estAR = null;

	  Allocation allocation =
      ((PlanningFactory)getDomainService().getFactory("planning")).createAllocation(task.getPlan(), task,
				     asset, estAR, Role.ASSIGNED);

    System.out.println("Allocating to programmer: "+asset.getItemIdentificationPG().getItemIdentification());
	  getBlackboardService().publishAdd(allocation);

}
}

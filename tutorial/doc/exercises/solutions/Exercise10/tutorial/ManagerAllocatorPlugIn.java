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

import org.cougaar.core.plugin.SimplePlugIn;
import org.cougaar.core.cluster.IncrementalSubscription;
import java.util.*;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.domain.planning.ldm.asset.*;
import org.cougaar.domain.glm.ldm.asset.Organization;
import org.cougaar.domain.glm.ldm.asset.OrganizationPG;
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
 * This COUGAAR PlugIn allocates tasks of verb "CODE"
 * to Organizations that have the "SoftwareDevelopment" role.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ManagerAllocatorPlugIn.java,v 1.2 2001-08-22 20:30:48 mthome Exp $
 **/
public class ManagerAllocatorPlugIn extends SimplePlugIn {

  private IncrementalSubscription tasks;         // "CODE" tasks
  private IncrementalSubscription programmers;   // SoftwareDevelopment orgs
  private IncrementalSubscription allocations;   // My allocations

  /**
   * subscribe to tasks and programming organizations
   */
protected void setupSubscriptions() {
  tasks = (IncrementalSubscription)subscribe(new myTaskPredicate());
  programmers = (IncrementalSubscription)subscribe(new myProgrammersPredicate());
  allocations = (IncrementalSubscription)subscribe(new myAllocationPredicate());
}


  /**
   * Top level plugin execute loop.  Allocate CODE tasks to organizations
   */
protected void execute () {

  // process unallocated tasks
  Enumeration task_enum = tasks.elements();
  while (task_enum.hasMoreElements()) {
    Task t = (Task)task_enum.nextElement();
    if (t.getPlanElement() != null) // already allocated
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

	  // Create an estimate that reports that we did just what we
	  // were asked to do
	  int []aspect_types = {AspectType.START_TIME, AspectType.END_TIME};
	  double []results = {getStartTime(task), getEndTime(task)};
	  estAR =  theLDMF.newAllocationResult(1.0, // rating
					      true, // success,
					      aspect_types,
					      results);

	  Allocation allocation =
	    theLDMF.createAllocation(task.getPlan(), task, asset,
				     estAR, Role.ASSIGNED);

    System.out.println("Allocating to programmer: "+asset.getItemIdentificationPG().getItemIdentification());
	  publishAdd(allocation);

}
  /**
   * Get the END_TIME preference for the task
   */
  private double getEndTime(Task t) {
    double end = 0.0;
    Preference pref = getPreference(t, AspectType.END_TIME);
    if (pref != null)
      end = pref.getScoringFunction().getBest().getAspectValue().getValue();
    return end;
  }

  /**
   * Get the START_TIME preference for the task
   */
  private double getStartTime(Task t) {
    double start = 0.0;
    Preference pref = getPreference(t, AspectType.START_TIME);
    if (pref != null)
      start = pref.getScoringFunction().getBest().getAspectValue().getValue();
    return start;
  }

  /**
   * Return the preference for the given aspect
   * @param task for which to return given preference
   * @paran int aspect type
   * @return Preference (or null) from task for given aspect
   **/
  private Preference getPreference(Task task, int aspect_type)
  {
    Preference aspect_pref = null;
    for(Enumeration e = task.getPreferences(); e.hasMoreElements();)
    {
      Preference pref = (Preference)e.nextElement();
      if (pref.getAspectType() == aspect_type) {
        aspect_pref = pref;
        break;
      }
    }
    return aspect_pref;
  }

}

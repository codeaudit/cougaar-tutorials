package org.cougaar.tutorial.faststart.hanoi;

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

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.tutorial.faststart.*;
import org.cougaar.util.UnaryPredicate;
import java.util.Enumeration;
import java.util.Vector;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.*;


/**
 * Towers of Hanoi Plugin :
 * Expands tasks of verb "MANAGE"
 * with Preposition "From" <from pole>
 * and Preposition "To" <to pole>
 * and Preference QUANTITY <count>
 * and Preferences for START_TIME and END_TIME
 * into workflows of subtasks:
 * MANAGE <from pole> <remaining pole> <count - 1>
 * TRANSPORT <from pole> <to pole> <1>
 * MANAGE <remaining pole> <to pole> <count - 1>
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: HanoiPlugin.java,v 1.3 2003-01-23 22:12:55 mbarger Exp $
 **/
public class HanoiPlugin  extends ComponentPlugin
{
  private DomainService domainService = null;
  private PlanningFactory ldmf = null;

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

  // Subscription for all 'MANAGE' tasks
  private IncrementalSubscription allManageTasks;
  private UnaryPredicate allManageTasksPredicate = new UnaryPredicate() {
    public boolean execute(Object o) { 
      return TutorialUtils.isTaskWithVerb(o, HanoiUtils.MANAGE_VERB);
    }};

  // Subscriptionfor all 'MANAGE' Expansions [Level4]
  private IncrementalSubscription allManageExpansions;
  private UnaryPredicate allManageExpansionsPredicate = new UnaryPredicate() {
    public boolean execute(Object o) { 
      if (o instanceof Expansion) {
	Expansion expansion = (Expansion)o;
	return expansion.getWorkflow().getParentTask().getVerb().equals
	  (HanoiUtils.MANAGE_VERB);
      }
      return false;
    }};

  /**
   * Establish subscription for TRANSPORT tasks
   **/
  public void setupSubscriptions() { 
    System.out.println("HanoiPlugin::setupSubscriptions ");
    ldmf = (PlanningFactory) getDomainService().getFactory("planning");
    if (ldmf == null) {
      throw new RuntimeException(
          "Unable to find \"planning\" domain factory");
    }

    allManageTasks =
      (IncrementalSubscription)getBlackboardService().subscribe(allManageTasksPredicate);

    // [Level4]
    allManageExpansions = 
      (IncrementalSubscription)getBlackboardService().subscribe(allManageExpansionsPredicate);
  }

  /**
   * Top level plugin execute loop : Take any new task and generate
   * new tasks for it as above, or print top level message
   **/
  public void execute() { 
      // System.out.println("HanoiPlugin::execute ");

    // [Level4]
    // First, look at changed expansions : need to take 
    // reported allocationResult and copy to estimated allocationResult
    for(Enumeration e = allManageExpansions.getChangedList();
	e.hasMoreElements();) 
      {

	Expansion expansion = (Expansion)e.nextElement();

	// NOTE : At this point, one could compare the reported and
	// estimated allocationResults to see if the results were
	// satisfactory, and replan if necessary.
	// Similarly, one could check the integrity of workflow constraints
	// at this point, and replan if necessary.

	if (expansion.getEstimatedResult() == null) {
	  expansion.setEstimatedResult(expansion.getReportedResult());
	  getBlackboardService().publishChange(expansion);
	}
      }
    // End [Level4]

    // Now look through all new 'MANAGE' tasks
    // and expand as above
    for(Enumeration e = allManageTasks.getAddedList();e.hasMoreElements();) 
      {
	Task task = (Task) e.nextElement();
	//	System.out.println("Element in allManageTasks : " + task);

	// Pull 'Hanoi' data from prepositions
	int from_pole =
	  HanoiUtils.getPoleValue(task, HanoiUtils.FROM_PREPOSITION);
	int to_pole =
	  HanoiUtils.getPoleValue(task, HanoiUtils.TO_PREPOSITION);
	int count =
	  (int)TutorialUtils.getPreferredValue(task, AspectType.QUANTITY, -1.0);
	// [Level3]
	// Pull start/end preferences from preferences
	// Divide up the time allotted into thirds between the three subtasks
	double start_time = 
	  TutorialUtils.getPreferredValue(task, AspectType.START_TIME, -1.0);
	double end_time = 
	  TutorialUtils.getPreferredValue(task, AspectType.END_TIME, -1.0);
	double third_of_delta = (end_time - start_time)/3.0;
	// End [Level3]

	// Create expansion and workflow to represent the expansion 
	// of this task
	NewWorkflow new_wf = ldmf.newWorkflow();
	new_wf.setParentTask(task);

	// Base recursion case : If only one disk to move, generate
	// TRANSPORT task
	if (count == 1) {
	  Task t1 = 
	    HanoiUtils.createNewTask
	    (from_pole, to_pole, 1, 
	     start_time, end_time,
	     HanoiUtils.TRANSPORT_VERB, task, ldmf);
	  new_wf.addTask(t1);
	  getBlackboardService().publishAdd(t1);
	} else {
	  // Otherwise, generate three subtasks as above, 
	  // with preferences sharing the allotted time in thirds
	  Task t1 = 
	    HanoiUtils.createNewTask
	    (from_pole, 
	     HanoiUtils.computeRemainingPole(from_pole, to_pole), 
	     count-1, 
	     start_time, start_time + third_of_delta,
	     HanoiUtils.MANAGE_VERB,
	     task, ldmf);
	  new_wf.addTask(t1);
	  getBlackboardService().publishAdd(t1);

	  Task t2 = 
	    HanoiUtils.createNewTask(from_pole,
				     to_pole, 
				     1, 
				     start_time + third_of_delta,
				     end_time - third_of_delta,
				     HanoiUtils.TRANSPORT_VERB,
				     task, ldmf);
	  new_wf.addTask(t2);
	  getBlackboardService().publishAdd(t2);

	  Task t3 = 
	    HanoiUtils.createNewTask
	    (HanoiUtils.computeRemainingPole(from_pole, to_pole),
	     to_pole, 
	     count-1,
	     end_time - third_of_delta,
	     end_time,
	     HanoiUtils.MANAGE_VERB,
	     task, ldmf);
	  new_wf.addTask(t3);
	  getBlackboardService().publishAdd(t3);

	  // [Level5]
	  // Add constraints onto the workflow that t1 < t2 < t3
	  Vector constraints = new Vector();

	  // End(t1) must be before Start(t2)
	  NewConstraint c1 = ldmf.newConstraint();
	  c1.setConstrainingTask(t1);
	  c1.setConstrainingAspect(AspectType.END_TIME);
	  c1.setConstrainedTask(t2);
	  c1.setConstrainedAspect(AspectType.START_TIME);
	  c1.setConstraintOrder(Constraint.BEFORE);
	  constraints.addElement(c1);

	  // End(t2) must be before Start(t3)
	  NewConstraint c2 = ldmf.newConstraint();
	  c2.setConstrainingTask(t2);
	  c2.setConstrainingAspect(AspectType.END_TIME);
	  c2.setConstrainedTask(t3);
	  c2.setConstrainedAspect(AspectType.START_TIME);
	  c2.setConstraintOrder(Constraint.BEFORE);
	  constraints.addElement(c2);
	  new_wf.setConstraints(constraints.elements());
	  // End [Level5]

	}

	AllocationResult estAR = null;
	Expansion new_exp = 
	  ldmf.createExpansion(task.getPlan(), task, new_wf, estAR);
	getBlackboardService().publishAdd(new_wf);
	getBlackboardService().publishAdd(new_exp);

      }
  }


}

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

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.UnaryPredicate;
import java.util.Enumeration;
import java.util.Vector;
import org.cougaar.planning.ldm.PlanningFactory;


/**
 * This COUGAAR Plugin expands tasks of verb "CODE"
 * into workflows of subtasks:
 * DESIGN
 * DEVELOP
 * TEST
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: DevelopmentExpanderPlugin.java,v 1.8 2003-04-10 16:00:58 dmontana Exp $
 **/
public class DevelopmentExpanderPlugin extends ComponentPlugin
{
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

  // Subscription for all 'CODE' tasks
  private IncrementalSubscription allCodeTasks;

  // Subscription for all of my expansions
  private IncrementalSubscription allMyExpansions;

  // Subscription for allocations of all subtasks that I make
  private IncrementalSubscription allSubTaskAllocations;

  // This predicate matches all tasks with verb "CODE"
  private UnaryPredicate allCodeTasksPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      boolean ret = false;
      if (o instanceof Task) {
        Task t = (Task)o;
        ret = t.getVerb().equals("CODE");
      }
      return ret;
    }
  };

  // This predicate matches all of my subtask plan elements
  private UnaryPredicate allSubTasksPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      boolean ret = false;
      if (o instanceof PlanElement) {
        Task t = ((PlanElement)o).getTask();
        Verb v = t.getVerb();
        ret = v.equals("DESIGN") || v.equals("DEVELOP") || v.equals("TEST");
      }
      return ret;
    }
  };

  /**
   * Predicate that matches all Expansion of CODE tasks
   */
  private UnaryPredicate expansionPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Expansion)
      {
        Expansion exp = (Expansion)o;
        return (exp.getTask().getVerb().equals(Verb.getVerb("CODE")));
      }
      return false;
    }
  };

  /**
   * Establish subscription for CODE tasks
   **/
  public void setupSubscriptions() {
    allCodeTasks =
      (IncrementalSubscription)getBlackboardService().subscribe(allCodeTasksPredicate);
    allMyExpansions = (IncrementalSubscription)getBlackboardService().subscribe(expansionPredicate);
    allSubTaskAllocations =(IncrementalSubscription)getBlackboardService().subscribe(allSubTasksPredicate);
  }

  /**
   * Top level plugin execute loop.
   **/
  public void execute() {
    System.out.println("DevelopmentExpanderPlugin::execute");

    // Now look through all new 'CODE' tasks
    // and expand
    for(Enumeration e = allCodeTasks.getAddedList();e.hasMoreElements();)
    {
      Task task = (Task) e.nextElement();

      // Create expansion and workflow to represent the expansion
      // of this task
      NewWorkflow new_wf = ((PlanningFactory)domainService.getFactory("planning")).newWorkflow();
      new_wf.setParentTask(task);

      plan(new_wf);

      AllocationResult estAR = null;
      Expansion new_exp =
        ((PlanningFactory)domainService.getFactory("planning")).createExpansion(task.getPlan(), task, new_wf, estAR);
      getBlackboardService().publishAdd(new_exp);
    }

    // Now look through all changed expansions and update the allocation result
    Enumeration exps = allMyExpansions.elements();
    while (exps.hasMoreElements()) {
      checkForReplan((Expansion)exps.nextElement());
    }
    PluginHelper.updateAllocationResult(allMyExpansions);
  }

  /**
   * Create a task.
   * @param verb The string for the verb for the task.
   * @param parent_task The task being expanded
   * @param start the start month for the task
   * @param deadline the end month for the task
   * @param duration the length (in months) of the task
   * @param workflow the workflow being filled out
   * @return A new sub-task member of the workflow
   */
  private NewTask makeTask(String verb, Task parent_task, Workflow wf) {
    NewTask new_task = ((PlanningFactory)domainService.getFactory("planning")).newTask();

    new_task.setParentTask(parent_task);
    new_task.setWorkflow(wf);

    // Set the verb as given
    new_task.setVerb(Verb.getVerb(verb));

    // Copy important fields from the parent task
    new_task.setPlan(parent_task.getPlan());
    new_task.setDirectObject(parent_task.getDirectObject());
    new_task.setPrepositionalPhrases(parent_task.getPrepositionalPhrases());

    return new_task;
  }

  private void setPreferences(NewTask new_task, long start, int duration, long deadline) {
    // Establish preferences for task
    Vector preferences = new Vector();

    // Add a start_time, end_time, and duration strict preference
    ScoringFunction scorefcn = ScoringFunction.createStrictlyAtValue
      (AspectValue.newAspectValue(AspectType.START_TIME, start));
    Preference pref =
      ((PlanningFactory)domainService.getFactory("planning")).newPreference(AspectType.START_TIME, scorefcn);
    preferences.add(pref);

    scorefcn = ScoringFunction.createStrictlyAtValue
      (AspectValue.newAspectValue(AspectType.END_TIME, deadline));
    pref = ((PlanningFactory)domainService.getFactory("planning")).newPreference(AspectType.END_TIME, scorefcn);
    preferences.add(pref);

    scorefcn = ScoringFunction.createStrictlyAtValue
      (AspectValue.newAspectValue(AspectType.DURATION, duration));
    pref = ((PlanningFactory)domainService.getFactory("planning")).newPreference(AspectType.DURATION, scorefcn);
    preferences.add(pref);

    new_task.setPreferences(preferences.elements());
  }

  private void setPreferences(NewTask new_task, int duration, long deadline) {
    // Establish preferences for task (just duration and deadline, not start)
    Vector preferences = new Vector();

    ScoringFunction scorefcn = ScoringFunction.createStrictlyAtValue
      (AspectValue.newAspectValue(AspectType.DURATION, duration));
    Preference pref = ((PlanningFactory)domainService.getFactory("planning")).newPreference(AspectType.DURATION, scorefcn);
    preferences.add(pref);

    scorefcn = ScoringFunction.createStrictlyAtValue
      (AspectValue.newAspectValue(AspectType.END_TIME, deadline));
    pref = ((PlanningFactory)domainService.getFactory("planning")).newPreference(AspectType.END_TIME, scorefcn);
    preferences.add(pref);

    new_task.setPreferences(preferences.elements());
  }

  /**
   * Determine if we should replan this expansion because something changed
   */
  private void checkForReplan(Expansion exp) {
    Workflow wf = exp.getWorkflow();
    Constraint c = wf.getNextPendingConstraint();
    if (c != null) {
      ConstraintEvent ced = c.getConstrainedEventObject();
      if (ced instanceof SettableConstraintEvent)
      {
        ((SettableConstraintEvent)ced).setValue(c.computeValidConstrainedValue());
//System.out.println("START_TIME on "+c.getConstrainedTask().getVerb()+" set to "+getStartTime(c.getConstrainedTask()));
        getBlackboardService().publishAdd(c.getConstrainedTask());
      }
    }
  }

  /**
   * This VERY primitive scheduler just keeps moving the whole workflow later until it can be scheduled
   */
  private void plan(NewWorkflow new_wf) {
    Task task = new_wf.getParentTask();
    long latest_end = getEndTime(task);

    long start_month    = getStartTime(task);
    long deadline_month = latest_end;

    // assign one month for design
    int this_task_duration = 1;
    NewTask t1 = makeTask("DESIGN", task, new_wf);
    setPreferences(t1, start_month, this_task_duration, deadline_month);
    getBlackboardService().publishAdd(t1);      // Add the task to the Blackboard
    new_wf.addTask(t1);  // Add the task to the Workflow

    // assign three months for development
    this_task_duration = 3;
    NewTask t2 = makeTask("DEVELOP", task, new_wf);
    setPreferences(t2, this_task_duration, deadline_month);
    // publishAdd(t2);      // Don't add the task to the Blackboard yet
    new_wf.addTask(t2);  // Add the task to the Workflow

    // testing takes two month
    this_task_duration = 2;
    NewTask t3 = makeTask("TEST", task, new_wf);
    setPreferences(t3, this_task_duration, deadline_month);

    // publishAdd(t2);      // Don't add the task to the Blackboard yet
    new_wf.addTask(t3);  // Add the task to the Workflow

    // Add constraints onto the workflow that t1 < t2 < t3
    Vector constraints = new Vector();

    // End(t1) must be before Start(t2)
    NewConstraint c1 = ((PlanningFactory)domainService.getFactory("planning")).newConstraint();
    c1.setConstrainingTask(t1);
    c1.setConstrainingAspect(AspectType.END_TIME);
    c1.setConstrainedTask(t2);
    c1.setConstrainedAspect(AspectType.START_TIME);
    c1.setConstraintOrder(Constraint.BEFORE);
    constraints.addElement(c1);

    // End(t2) must be before Start(t3)
    NewConstraint c2 = ((PlanningFactory)domainService.getFactory("planning")).newConstraint();
    c2.setConstrainingTask(t2);
    c2.setConstrainingAspect(AspectType.END_TIME);
    c2.setConstrainedTask(t3);
    c2.setConstrainedAspect(AspectType.START_TIME);
    c2.setConstraintOrder(Constraint.BEFORE);
    constraints.addElement(c2);

    // set the constraints on the workflow
    new_wf.setConstraints(constraints.elements());
  }

  /**
   * Get the END_TIME preference for the task
   */
  private long getEndTime(Task t) {
    double end = 0.0;
    Preference pref = getPreference(t, AspectType.END_TIME);
    if (pref != null)
      end = pref.getScoringFunction().getBest().getAspectValue().getValue();
    return (long) end;
  }

  /**
   * Get the START_TIME preference for the task
   */
  private long getStartTime(Task t) {
    double start = 0.0;
    Preference pref = getPreference(t, AspectType.START_TIME);
    if (pref != null)
      start = pref.getScoringFunction().getBest().getAspectValue().getValue();
    return (long) start;
  }
  /**
   * Get the DURATION preference for the task
   */
  private int getDuration(Task t) {
    double start = 0.0;
    Preference pref = getPreference(t, AspectType.DURATION);
    if (pref != null)
      start = pref.getScoringFunction().getBest().getAspectValue().getValue();
    return (int)start;
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

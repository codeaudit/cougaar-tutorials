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

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.core.plugin.util.PluginHelper;
import org.cougaar.util.UnaryPredicate;
import java.util.Enumeration;
import java.util.Vector;


/**
 * This COUGAAR Plugin expands tasks of verb "CODE"
 * into workflows of subtasks:
 * DESIGN
 * DEVELOP
 * TEST
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: DevelopmentExpanderPlugin.java,v 1.1 2002-02-12 19:30:10 jwinston Exp $
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

  // Subscription for all subtasks that I make
  private IncrementalSubscription allSubTasks;

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

  // This predicate matches all of my subtsks
  private UnaryPredicate allSubTasksPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      boolean ret = false;
      if (o instanceof Task) {
        Task t = (Task)o;
        Verb v = t.getVerb();
        ret = v.equals("DESIGN") || v.equals("DEVELOP") || v.equals("TEST");
      }
      return ret;
    }
  };

  /**
   * Predicate that matches all Expansion of CODE tasks
   * todo: subscribe to expansions of CODE tasks
   */
  private UnaryPredicate expansionPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {


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
    allSubTasks =(IncrementalSubscription)getBlackboardService().subscribe(allSubTasksPredicate);
  }

  /**
   * Top level plugin execute loop.
   **/
  public void execute() {
    System.out.println("DevelopmentExpanderPlugin::execute");

    // todo: Now look through all new 'CODE' tasks
    // and expand
    for(Enumeration e = allCodeTasks.getAddedList();e.hasMoreElements();)
    {
      // todo: Create expansion and workflow to represent the expansion
      // of this task.  Publish the expansion, workflow and subtasks.







    }

    // Now look through all changed expansions and update the allocation result
    Enumeration exps = allMyExpansions.elements();
    while (exps.hasMoreElements()) {
       checkForReplan((Expansion)exps.nextElement());
    }
// todo: report allocation results
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
    NewTask new_task = domainService.getFactory().newTask();

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

  /**
   * This convenience function is complete as-is
   */
  private void setPreferences(NewTask new_task,int start, int duration, int deadline) {
    // Establish preferences for task
    Vector preferences = new Vector();

    // Add a start_time, end_time, and duration strict preference
    ScoringFunction scorefcn = ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.START_TIME, start));
    Preference pref =
      domainService.getFactory().newPreference(AspectType.START_TIME, scorefcn);
    preferences.add(pref);

    scorefcn = ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.END_TIME, deadline));
    pref = domainService.getFactory().newPreference(AspectType.END_TIME, scorefcn);
    preferences.add(pref);

    scorefcn = ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.DURATION, duration));
    pref = domainService.getFactory().newPreference(AspectType.DURATION, scorefcn);
    preferences.add(pref);

    new_task.setPreferences(preferences.elements());
  }

  /**
   * This convenience function is complete as-is
   */
  private void setPreferences(NewTask new_task, int duration, int deadline) {
    // Establish preferences for task (just duration and deadline, not start)
    Vector preferences = new Vector();

    ScoringFunction scorefcn = ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.DURATION, duration));
    Preference pref = domainService.getFactory().newPreference(AspectType.DURATION, scorefcn);
    preferences.add(pref);

    scorefcn = ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.END_TIME, deadline));
    pref = domainService.getFactory().newPreference(AspectType.END_TIME, scorefcn);
    preferences.add(pref);

    new_task.setPreferences(preferences.elements());
  }

  /**
   * Determine if we should replan this expansion because something changed
   */
  private void checkForReplan(Expansion exp) {
  // todo: get the next pending constraint and update the constrained task's preferences

  }

  /**
   * This VERY primitive scheduler just keeps moving the whole workflow later until it can be scheduled
   */
  private void plan(NewWorkflow new_wf) {
    Task task = new_wf.getParentTask();
    int latest_end = getEndTime(task);

    int start_month    = getStartTime(task);
    int deadline_month = latest_end;

    Vector tasks = new Vector();  // Vector in which to hold subtasks

    // todo : use makeTask and setPreferences convenience functions to create a "DESIGN" task
    // assign one month for design
    int this_task_duration = 1;

    // assign three months for development
    this_task_duration = 3;

    // testing takes two months
    this_task_duration = 2;

    // todo: Add constraints onto the workflow that t1 < t2 < t3

    // End(t1) must be before Start(t2)

    // End(t2) must be before Start(t3)

    // set the constraints on the workflow
  }

  /**
   * Get the END_TIME preference for the task
   */
  private int getEndTime(Task t) {
    double end = 0.0;
    Preference pref = getPreference(t, AspectType.END_TIME);
    if (pref != null)
      end = pref.getScoringFunction().getBest().getAspectValue().getValue();
    return (int)end;
  }

  /**
   * Get the START_TIME preference for the task
   */
  private int getStartTime(Task t) {
    double start = 0.0;
    Preference pref = getPreference(t, AspectType.START_TIME);
    if (pref != null)
      start = pref.getScoringFunction().getBest().getAspectValue().getValue();
    return (int)start;
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

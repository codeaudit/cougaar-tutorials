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

import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.core.plugin.util.PlugInHelper;
import org.cougaar.util.UnaryPredicate;
import java.util.Enumeration;
import java.util.Vector;


/**
 * This ALP PlugIn expands tasks of verb "CODE"
 * into workflows of subtasks:
 * DESIGN
 * DEVELOP
 * TEST
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: DevelopmentExpanderPlugIn.java,v 1.2 2000-12-18 15:41:13 wwright Exp $
 **/
public class DevelopmentExpanderPlugIn extends org.cougaar.core.plugin.SimplePlugIn
{
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
      (IncrementalSubscription)subscribe(allCodeTasksPredicate);
    allMyExpansions = (IncrementalSubscription)subscribe(expansionPredicate);
    allSubTasks =(IncrementalSubscription)subscribe(allSubTasksPredicate);
  }

  /**
   * Top level plugin execute loop.
   **/
  public void execute() {
    System.out.println("DevelopmentExpanderPlugIn::execute");

    // Now look through all new 'CODE' tasks
    // and expand
    for(Enumeration e = allCodeTasks.getAddedList();e.hasMoreElements();)
    {
      Task task = (Task) e.nextElement();

      // Create expansion and workflow to represent the expansion
      // of this task
      NewWorkflow new_wf = theLDMF.newWorkflow();
      new_wf.setParentTask(task);

      plan(new_wf);

      AllocationResult estAR = null;
      Expansion new_exp =
        theLDMF.createExpansion(task.getPlan(), task, new_wf, estAR);
      publishAdd(new_wf);
      publishAdd(new_exp);
    }

    // Now look through all changed expansions and update the allocation result
//    if (allSubTasks.hasChanged()) {
      Enumeration exps = allMyExpansions.elements();
      while (exps.hasMoreElements()) {
        checkForReplan((Expansion)exps.nextElement());
      }
      PlugInHelper.updateAllocationResult(allMyExpansions);
//    }
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
    NewTask new_task = theLDMF.newTask();

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

  private void setPreferences(NewTask new_task,int start, int duration, int deadline) {
    // Establish preferences for task
    Vector preferences = new Vector();

    // Add a start_time, end_time, and duration strict preference
    ScoringFunction scorefcn = ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.START_TIME, start));
    Preference pref =
      theLDMF.newPreference(AspectType.START_TIME, scorefcn);
    preferences.add(pref);

    scorefcn = ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.END_TIME, deadline));
    pref = theLDMF.newPreference(AspectType.END_TIME, scorefcn);
    preferences.add(pref);

    scorefcn = ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.DURATION, duration));
    pref = theLDMF.newPreference(AspectType.DURATION, scorefcn);
    preferences.add(pref);

    new_task.setPreferences(preferences.elements());
  }

  private void setPreferences(NewTask new_task, int duration, int deadline) {
    // Establish preferences for task (just duration and deadline, not start)
    Vector preferences = new Vector();

    ScoringFunction scorefcn = ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.DURATION, duration));
    Preference pref = theLDMF.newPreference(AspectType.DURATION, scorefcn);
    preferences.add(pref);

    scorefcn = ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.END_TIME, deadline));
    pref = theLDMF.newPreference(AspectType.END_TIME, scorefcn);
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
        ((SettableConstraintEvent)ced).setValue(c.computeValidConstrainedValue(),
	                                        Constraint.COINCIDENT, 0.0);
//System.out.println("START_TIME on "+c.getConstrainedTask().getVerb()+" set to "+getStartTime(c.getConstrainedTask()));
        publishAdd(c.getConstrainedTask());
      }
    }

    //
    // if any of the contstraints are violated, replan the whole thing
    //
    if (wf.constraintViolation())
    {
      System.out.println("Constraints violated on "+wf);
      plan((NewWorkflow)wf);
    }

  }

  /**
   * This VERY primitive scheduler just keeps moving the whole workflow later until it can be scheduled
   */
  private void plan(NewWorkflow new_wf) {
    Task task = new_wf.getParentTask();
    int latest_end = getEndTime(task);

    // rescind all the old tasks (if any)
    if (new_wf.getTasks().hasMoreElements()) {
      Enumeration subtasks = sortTasks(new_wf.getTasks());
      while (subtasks.hasMoreElements()) {
        Task t = (Task) subtasks.nextElement();
        publishRemove(t);
        new_wf.removeTask(t);
        if (getEndTime(t) > latest_end)
          latest_end = getEndTime(t);
      }
    }

    int start_month    = getStartTime(task);
    int deadline_month = latest_end;

    // assign one month for design
    int this_task_duration = 1;
    NewTask t1 = makeTask("DESIGN", task, new_wf);
    setPreferences(t1, start_month, this_task_duration, deadline_month);
    publishAdd(t1);      // Add the task to the PLAN
    new_wf.addTask(t1);  // Add the task to the Workflow

    // assign three months for development
    this_task_duration = 3;
    NewTask t2 = makeTask("DEVELOP", task, new_wf);
    setPreferences(t2, this_task_duration, deadline_month);
    // publishAdd(t2);      // Don't add the task to the PLAN yet
    new_wf.addTask(t2);  // Add the task to the Workflow

    // testing takes two month
    this_task_duration = 2;
    NewTask t3 = makeTask("TEST", task, new_wf);
    setPreferences(t3, this_task_duration, deadline_month);

    // publishAdd(t3);      // Don't add the task to the PLAN yet
    new_wf.addTask(t3);  // Add the task to the Workflow

    // Add constraints onto the workflow that t1 < t2 < t3
    Vector constraints = new Vector();

    // End(t1) must be before Start(t2)
    NewConstraint c1 = theLDMF.newConstraint();
    c1.setConstrainingTask(t1);
    c1.setConstrainingAspect(AspectType.END_TIME);
    c1.setConstrainedTask(t2);
    c1.setConstrainedAspect(AspectType.START_TIME);
    c1.setConstraintOrder(Constraint.BEFORE);
    constraints.addElement(c1);

    // End(t2) must be before Start(t3)
    NewConstraint c2 = theLDMF.newConstraint();
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
   * Sort the subtasks into chronological order
   */
  private Enumeration sortTasks(Enumeration inTasks) {
    // I know there are three tasks.
    Task[] ret = new Task[3];
    while (inTasks.hasMoreElements()) {
      Task t = (Task)inTasks.nextElement();
      if (t.getVerb().equals(Verb.getVerb("DESIGN")))
        ret[0] = t;
      if (t.getVerb().equals(Verb.getVerb("DEVELOP")))
        ret[1] = t;
      if (t.getVerb().equals(Verb.getVerb("TEST")))
        ret[2] = t;
    }
    Vector v = new Vector (3);
    v.add(ret[0]);
    v.add(ret[1]);
    v.add(ret[2]);
    return v.elements();
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

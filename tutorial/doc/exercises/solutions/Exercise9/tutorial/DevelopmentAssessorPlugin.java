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
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.util.UnaryPredicate;
import java.util.*;
import org.cougaar.planning.ldm.PlanningFactory;

import tutorial.assets.*;


/**
 * This COUGAAR Plugin monitors ProgrammerAssets for conflicts between their
 * internal schedule and the tasks allocated to them.  When a conflict is
 * detected, the task allocation results are updated to reflect the conflict.
 *
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: DevelopmentAssessorPlugin.java,v 1.4 2003-04-11 18:16:47 dmontana Exp $
 **/
public class DevelopmentAssessorPlugin extends ComponentPlugin
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

  // The set of programmer assets
  private IncrementalSubscription vacationAllocs;

  /**
   * This predicate matches all programmer assets
   */
  private UnaryPredicate vacationsPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof Allocation) &&
             (((Allocation) o).getTask().getVerb().equals
               (Verb.getVerb ("VACATION")));
    }
  };

  /**
   * Establish subscription for assets
   **/
  public void setupSubscriptions() {
    vacationAllocs =
      (IncrementalSubscription)getBlackboardService().subscribe(vacationsPredicate);
  }

  /**
   * Top level plugin execute loop.  Look at all changed programmer
   * assets and check their schedules.
   **/
  public void execute() {
    System.out.println("DevelopmentAssessorPlugin::execute");

    for(Enumeration e = vacationAllocs.getAddedList();e.hasMoreElements();)
    {
      Allocation alloc = (Allocation) e.nextElement();
      ProgrammerAsset pa = (ProgrammerAsset) alloc.getAsset();
      validateSchedule(pa);
    }
  }

  /**
   * Check the programmer's schedule against his assigned tasks.
   * If there are conflicts, update the task's allocation results.
   */
  private void validateSchedule(ProgrammerAsset asset) {
      System.out.println ("Validating schedule of " +
        asset.getItemIdentificationPG().getItemIdentification());

      // first, find vacation task on schedule
      RoleSchedule sched = asset.getRoleSchedule();
      Enumeration enum = sched.getRoleScheduleElements();
      Allocation vacAlloc = null;
      while (enum.hasMoreElements()) {
        Allocation alloc = (Allocation) enum.nextElement();
        if (alloc.getTask().getVerb().equals (Verb.getVerb ("VACATION"))) {
          vacAlloc = alloc;
          break;
        }
      }

      // if does not exist, return
      if (vacAlloc == null)
        return;

      // next, find other task at same time as vacation
      Collection c = sched.getOverlappingRoleSchedule
        ((long) vacAlloc.getEstimatedResult().getValue(AspectType.START_TIME),
         (long) vacAlloc.getEstimatedResult().getValue(AspectType.END_TIME) - 1);
      Iterator iter = c.iterator();
      Allocation badAlloc = null;
      while (iter.hasNext()) {
        Allocation alloc = (Allocation) iter.next();
        if (! alloc.getTask().getVerb().equals (Verb.getVerb ("VACATION")))
          badAlloc = alloc;
      }

      // if it exists, remove it
      if (badAlloc != null) {
        getBlackboardService().publishRemove (badAlloc);
        System.out.println ("Removing allocation for " +
          asset.getItemIdentificationPG().getItemIdentification());
      }
  }

}



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
 * @version $Id: DevelopmentAssessorPlugin.java,v 1.5 2003-04-16 22:44:41 dmontana Exp $
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
   * This predicate matches all vacation allocations
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
   * Top level plugin execute loop.  Look at all programmers
   * with new vacations and mark their schedules as needing replanning
   **/
  public void execute() {
    System.out.println("DevelopmentAssessorPlugin::execute");

    for(Enumeration e = vacationAllocs.getAddedList(); e.hasMoreElements();)
    {
      Allocation alloc = (Allocation) e.nextElement();
      ProgrammerAsset pa = (ProgrammerAsset) alloc.getAsset();
      validateSchedule(pa);
    }
  }

  /**
   * Remove from schedule all allocations that are not the vacation
   * Allocator will put things back on
   * It is easiest just to redo everything
   */
  private void validateSchedule(ProgrammerAsset asset) {
      System.out.println ("Validating schedule of " +
        asset.getItemIdentificationPG().getItemIdentification());

      // if not a vacation, then remove it
      RoleSchedule sched = asset.getRoleSchedule();
      Enumeration enum = sched.getRoleScheduleElements();
      while (enum.hasMoreElements()) {
        Allocation alloc = (Allocation) enum.nextElement();
        if (! alloc.getTask().getVerb().equals (Verb.getVerb ("VACATION")))
          blackboard.publishRemove (alloc);
      }
  }
}

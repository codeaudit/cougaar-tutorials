/*
 * <copyright>
 *  Copyright 2000-2003 BBNT Solutions, LLC
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
package org.cougaar.tutorial.exercise10;
import org.cougaar.tutorial.assets.ProgrammerAsset;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.planning.ldm.plan.*;

import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.service.BlackboardService; 
import org.cougaar.core.service.ServletService;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.core.service.*;

/**
 * This Servlet assigns a vacation month to each ProgrammerAsset.
 * It always looks for the earliest scheduled (to a task) month
 * for the vacation month.  It responds with text describing what it did.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: TakeVacationServlet.java,v 1.1 2003-12-15 16:07:02 twright Exp $
 */

public class TakeVacationServlet extends BaseServletComponent 
implements BlackboardClient 
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

  private BlackboardService blackboard;

  protected String getPath() {
    return "/takeVacationServlet";
  }

  protected Servlet createServlet() {
      // get the blackboard service
      blackboard = (BlackboardService) serviceBroker.getService(
            this, 
            BlackboardService.class,
            null);
      if (blackboard == null) {
        throw new RuntimeException(
            "Unable to obtain blackboard service");
      }


    // We could inline "MyServlet" here as an anonymous
    // inner-class (like HelloBaseServletComponent does). Instead, 
    // we'll move it to a simple inner-class, which will make the 
    // code a little easier to read.
    return new MyServlet();
  }


  public void setBlackboardService(BlackboardService blackboard) {
    this.blackboard = blackboard;
  }

  //
  // These are required when implementing a BlackboardClient:
  // A Component must implement BlackboardClient in order 
  // to obtain BlackboardService.
  //

  // BlackboardClient method:
  public String getBlackboardClientName() {
    return toString();
  }

  // unused BlackboardClient method:
  public long currentTimeMillis() {
      return new Date().getTime();
  }

  // unused BlackboardClient method:
  public boolean triggerEvent(Object event) {
      return false;
  }

  public void unload() {
    super.unload();
    // release the blackboard service
    if (blackboard != null) {
      serviceBroker.releaseService(
        this, BlackboardService.class, servletService);
      blackboard = null;
    }
  }

  private class MyServlet extends HttpServlet {
      UnaryPredicate pred=new UnaryPredicate() {
	      public boolean execute(Object o) {
		  return o instanceof ProgrammerAsset;
	      }
	  };
    public void doGet(
        HttpServletRequest req,
        HttpServletResponse res) throws IOException {
	execute(req, res);
    }
    public void doPost(
        HttpServletRequest req,
        HttpServletResponse res) throws IOException {
	execute(req, res);
    }

  /**
   * Iterate over the list of programmers and have each take some vacation.  
   * Print the programmers name and vacation month to the Servlet response.
   */
      public void execute (
			   HttpServletRequest req,
			   HttpServletResponse res) throws IOException {
	  PrintWriter out = res.getWriter();
	  out.println("<html><head></head><body>");
	  
	  Collection col;
	  try {
	      ProgrammerAsset pa;
	      blackboard.openTransaction();
	      col = blackboard.query(pred);
	      for (Iterator it=col.iterator(); it.hasNext(); ) {
		  pa=(ProgrammerAsset)it.next();
                  makeVacation (pa, out);
	      }
	  } finally {
	      blackboard.closeTransactionDontReset();
	      out.println("<BR>Done.</body></html>");
	      out.flush();
	  }
	  
      }

  /**
   * Find and take a vacation month for this programmer.  Print the vacation
   * month to the PrintStream
   */
  private void makeVacation(ProgrammerAsset pa, PrintWriter out) {

    // make a VACATION task
    PlanningFactory factory =
      (PlanningFactory) getDomainService().getFactory("planning");
    NewTask task = factory.newTask();
    task.setVerb(Verb.getVerb("VACATION"));
    task.setPlan(factory.getRealityPlan());
    task.setDirectObject (pa);
    blackboard.publishAdd (task);

    // allocate it to current first month of schedule
    Enumeration e = pa.getRoleSchedule().getRoleScheduleElements();
    while (e.hasMoreElements()) {
      Allocation alloc = (Allocation) e.nextElement();
      if (! alloc.getEstimatedResult().isSuccess())
        continue;
      long start = alloc.getStartTime();
      GregorianCalendar cal = new GregorianCalendar();
      cal.setTime (new Date (start));
      cal.add (GregorianCalendar.MONTH, 1);
      long end = cal.getTime().getTime();
      AllocationResult ar = new AllocationResult (1.0, true,
        new AspectValue[] {
          AspectValue.newAspectValue (AspectType.START_TIME, start),
          AspectValue.newAspectValue (AspectType.END_TIME, end) });
      Allocation alloc2 = factory.createAllocation
        (task.getPlan(), task, pa, ar, Role.ASSIGNED);
      blackboard.publishAdd (alloc2);
      System.out.println ("Adding VACATION time for " +
               pa.getItemIdentificationPG().getItemIdentification() +
               " at time " + new Date (start));
      break;
    }
  }

  }
}

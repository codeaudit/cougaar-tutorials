/*
 * <copyright>
 *  Copyright 2000-2001 BBNT Solutions, LLC
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
import tutorial.assets.ProgrammerAsset;

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
 * @version $Id: TakeVacationServlet.java,v 1.4 2002-11-19 17:33:01 twright Exp $
 */

public class TakeVacationServlet extends BaseServletComponent 
implements BlackboardClient 
{

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
		  if (makeVacation(pa, out)) {
		      blackboard.publishChange(pa);
		      System.out.println("Adding VACATION time for "
					 +pa.getItemIdentificationPG()
					 .getItemIdentification());

		  }
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
  private boolean makeVacation(ProgrammerAsset pa, PrintWriter out) {
    boolean vacation = false;
    int min_month = Integer.MAX_VALUE;
    Schedule s = pa.getSchedule();
    Enumeration months = s.keys();
    // find the earliest month to force the most replanning
    while (months.hasMoreElements()) {
      int month = ((Integer)months.nextElement()).intValue();
      if ((month < min_month) && (s.getWork(month) instanceof Task))
        min_month = month;
    }
    if (min_month != Integer.MAX_VALUE) {
      s.setWork(min_month, "Vacation");
      out.println("<p>Vacation time for "
		  +pa.getItemIdentificationPG()
		  .getItemIdentification() 
		  + " month: "+min_month+"</p>");
      vacation = true;
    }
    return vacation;
  }

  }
}

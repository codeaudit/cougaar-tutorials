/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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
package org.cougaar.tutorial.exercise9;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.core.servlet.SimpleServletSupport;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.servlet.SimpleServletComponent;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.RoleSchedule;

import org.cougaar.tutorial.assets.*;

public class ScheduleServlet extends HttpServlet
{
  private SimpleServletSupport support;

  public void setSimpleServletSupport(SimpleServletSupport support)
  {
    this.support = support;
  }

  public void doGet(
		    HttpServletRequest request,
		    HttpServletResponse response) throws IOException, ServletException
  {
    execute(request, response);
  }

  public void doPost(
		     HttpServletRequest request,
		     HttpServletResponse response) throws IOException, ServletException
  {
    execute(request, response);
  }

  private void execute(
		       HttpServletRequest request,
		       HttpServletResponse response) throws IOException, ServletException
  {

    PrintWriter out = response.getWriter();
    ArrayList failedAllocs = new ArrayList();

    try
      {
	System.out.println("Servlet called." );

	Collection programmers =  support.queryBlackboard(new ProgrammersPredicate());
	Iterator iter = programmers.iterator();
	while (iter.hasNext()) {
	  ProgrammerAsset pa = (ProgrammerAsset)iter.next();
	  dumpProgrammerSchedule(pa, out, failedAllocs);
	}
      }
    catch (Exception ex)
      {
	out.println("Error processing servlet:"+ex.getMessage());
	ex.printStackTrace(out);
	System.out.println(ex);
	out.flush();
      }
    out.println ("<br><b>Failed Allocations</b><br>");
    out.println("<table border=1>");
    for (int i = 0; i < failedAllocs.size(); i++) {
      Allocation alloc = (Allocation) failedAllocs.get(i);
      out.println ("<tr><td>" + alloc.getTask().getVerb() + " " +
		   alloc.getTask().getDirectObject().
		   getItemIdentificationPG().getItemIdentification() +
		   "</td></tr>");
    }
    out.println("</table>");
    out.flush();
  }


  /**
   * Print an HTML table of this programmer's schedule to the PrintStream
   */
  private void dumpProgrammerSchedule(ProgrammerAsset pa, PrintWriter out,
                                      ArrayList failedAllocs) {
    // dump classnames and count to output stream
    out.println("<br><b>Programmer: "+pa.getItemIdentificationPG().getItemIdentification()+"<b><br>");
    out.println("<table border=1>");
    RoleSchedule s = pa.getRoleSchedule();
    Enumeration iter = s.getAllScheduleElements();

    out.println("<tr><td><b>Month</b></td><td><b>Task</b></td></tr>");
    while (iter.hasMoreElements()) {
      Object o = iter.nextElement();
      if (o instanceof Allocation) {
	Allocation alloc = (Allocation) o;
	SimpleDateFormat sdf = new SimpleDateFormat ("MMM");
	String startStr = sdf.format (alloc.getStartDate());
	String endStr = sdf.format (new Date (alloc.getEndTime() - 1));
	String monthStr = startStr.equals (endStr) ? startStr :
	  (startStr + "-" + endStr);
	if (alloc.getEstimatedResult().isSuccess())
	  out.print ("<tr><td>" + monthStr + "</td><td>" +
		     alloc.getTask().getVerb() + " " +
		     alloc.getTask().getDirectObject().
		     getItemIdentificationPG().getItemIdentification() +
		     "</td></tr>");
	else
	  failedAllocs.add (alloc);
      }
    }
    out.println("</table>");
    out.flush();
  }

}

/**
 * This predicate matches all Programmer asset objects
 */
class ProgrammersPredicate implements UnaryPredicate {
  public boolean execute(Object o) {
    return o instanceof ProgrammerAsset;
  }
}



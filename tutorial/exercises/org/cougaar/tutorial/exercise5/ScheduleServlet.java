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
package org.cougaar.tutorial.exercise5;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import javax.servlet.*;
import javax.servlet.http.*;
import org.cougaar.core.servlet.*;

import org.cougaar.tutorial.assets.*;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.mts.*;
import org.cougaar.core.agent.*;
import org.cougaar.glm.ldm.asset.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.servlet.SimpleServletComponent;



  /**
   * todo:  Create Predicate matching all ProgrammerAssets
   */


 // todo:  add code to make this a servlet subclass
public class ScheduleServlet
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

		// todo:  get the PrintWriter which sends data to HTTP
		try
		{
  		  System.out.println("Servlet called." );

		// todo: query the Blackboard for a Collection of ProgrammerAssets
           }
		catch (Exception ex)
		{
			out.println("Error processing servlet:"+ex.getMessage());
			ex.printStackTrace(out);
			System.out.println(ex);
			out.flush();
		}

	}

  /**
   * Print an HTML table of this programmer's schedule to the PrintStream
   */
  private void dumpProgrammerSchedule(ProgrammerAsset pa, PrintWriter out) {

      // todo: print programmer's name and a line break
      out.println("<table border=1>");
      RoleSchedule s = pa.getRoleSchedule();
      Enumeration iter = s.getAllScheduleElements();

      out.println("<tr><td><b>Month</b></td><td><b>Task</b></td></tr>");
      while (iter.hasMoreElements()) {
        Object o = iter.nextElement();
        if (o instanceof Allocation) {
          Allocation alloc = (Allocation) o;
          SimpleDateFormat sdf = new SimpleDateFormat ("MMM");
          out.print ("<tr><td>");
          // todo: print start month
          out.print ("-");
          // todo: print end month
          out.print ("</td><td>");
          // todo: print verb
          out.print (" ");
          // todo: print name of CODE task
          out.print ("</td></tr>");
        }
      }
      out.println("</table>");
      out.flush();
  }

}


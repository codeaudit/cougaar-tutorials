/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.pizza.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;

import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.text.SimpleDateFormat;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.community.CommunityDescriptor;
//import org.cougaar.community.CommunityImpl;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ServletService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.blackboard.ChangeReport;
import org.cougaar.core.blackboard.Claimable;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.core.util.UID;
import org.cougaar.planning.servlet.PlanViewServlet;

import org.cougaar.pizza.plugin.PizzaPreferences;

/**
 * Shows RSVP from each invited guest.
 */
public class PizzaPreferenceServlet extends BaseServletComponent {
  protected BlackboardQueryService blackboardQueryService;
  protected LoggingService logger;
  protected String agentID;

  public void load() {
    super.load();

    // get services
    blackboardQueryService= (BlackboardQueryService)
      serviceBroker.getService(this, BlackboardQueryService.class, null);
    AgentIdentificationService agentIDService = 
      (AgentIdentificationService) serviceBroker.getService(this, 
							    AgentIdentificationService.class, 
							    null);
    agentID = agentIDService.getMessageAddress().toString();
  }

  protected String getPath() {
    return "/pizza";
  }

  protected Servlet createServlet() {
    return new PizzaWorker();
  }

  /**
   * Inner-class that's registered as the servlet.
   */
  protected class PizzaWorker extends HttpServlet {
    public void doGet(
		      HttpServletRequest request,
		      HttpServletResponse response) throws IOException, ServletException {
      doPost(request, response);
    }

    public void doPost(
		       HttpServletRequest request,
		       HttpServletResponse response) throws IOException, ServletException {
      new PizzaFormatter(request, response);
    }
  }

  protected class PizzaFormatter {
    public static final int FORMAT_DATA = 0;
    public static final int FORMAT_XML = 1;
    public static final int FORMAT_HTML = 2;

    private int format;

    public PizzaFormatter(
			  HttpServletRequest request, 
			  HttpServletResponse response) throws IOException, ServletException
    {
      format = getFormat (request);
      execute (response);
    }         

    protected int getFormat (HttpServletRequest request) {
      String formatParam = request.getParameter("format");
      int format;
      if (formatParam == null) {
        format = FORMAT_HTML; // default
      } else if ("data".equals(formatParam)) {
        format = FORMAT_DATA;
      } else if ("xml".equals(formatParam)) {
        format = FORMAT_XML;
      } else if ("html".equals(formatParam)) {
        format = FORMAT_HTML;
      } else {
        format = FORMAT_HTML; // other
      }
      return format;
    }

    public void execute(HttpServletResponse response) throws IOException, ServletException {
      if (format == FORMAT_HTML) {
        response.setContentType("text/html");
	PrintWriter out = response.getWriter();
        out.print(
		  "<html><head><title>"+
		  "The Pizza Party"+
		  "</title></head>"+
		  "<body>" +
		  "<p/>" +
		  "<p/><center><h1>Pizza Preferences</h1><p/>"+
		  "<b>RSVP from each invited guest, invited by host" +
		  agentID +
		  "</b></center><p/>"+
		  getHtmlForPreferences () +
		  "</body>" +
		  "</html>\n");
        out.flush();
      }
      // FIXME: Add support for FORMAT_XML and FORMAT_DATA
    }

    protected String getHtmlForPreferences () {
      Collection pizzaPreferences = blackboardQueryService.query (new UnaryPredicate() {
	  public boolean execute(Object o) {
	    return (o instanceof PizzaPreferences);
	  }
	}
								  );

      if (pizzaPreferences.isEmpty()) {
	return "<center><b>Waiting for invitiation RSVP from friends.</b></center>";
      }
      else {
	PizzaPreferences prefs = (PizzaPreferences) pizzaPreferences.iterator().next();
	StringBuffer buf = new StringBuffer();
	buf.append("<table border=1 align=center>");
	buf.append("<tr>");
	buf.append("<th>");
	buf.append("Friend");
	buf.append("</th>");
	buf.append("<th>");
	buf.append("Preference");
	buf.append("</th>");
	buf.append("</tr>");
	for (Iterator iter = new TreeSet(prefs.getFriends()).iterator(); iter.hasNext(); ) {
	  buf.append("<tr>");

	  buf.append("<td>");
	  String friend = (String) iter.next();
	  buf.append(friend);
	  buf.append("</td>");

	  buf.append("<td>");
	  String preference = prefs.getPreferenceForFriend(friend);
	  buf.append(preference);
	  buf.append("</td>");

	  buf.append("</tr>");
	}
	buf.append("</table>");
	return buf.toString();
      }
    }
  }
}

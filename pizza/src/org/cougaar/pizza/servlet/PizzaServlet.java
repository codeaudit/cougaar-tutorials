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


//import org.apache.log4j.Category;
//import org.apache.log4j.Priority;
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
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
//import org.cougaar.util.log.log4j.DetailPriority;
//import org.cougaar.util.log.log4j.ShoutPriority;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.plugin.PizzaPreferences;

/**
 * 
 * 
 */
public class PizzaServlet extends ComponentPlugin {
  protected MessageAddress localAgent;
  protected AgentIdentificationService agentIdService;
  //  protected BlackboardQueryService blackboardQueryService;
  protected LoggingService logger;
  protected boolean stopRefresh = false;
  private ServletService servletService;
  private IncrementalSubscription relaysSubscription;
  private IncrementalSubscription tasksSubscription; 
  private IncrementalSubscription planElementsSubscription;
  protected static SimpleDateFormat format = new SimpleDateFormat ("hh:mm:ss:SSS");
  List events = new ArrayList();

  /** Sets the servlet service. Called by introspection on start
   **/
  //  public void setServletService(ServletService ss) {
  //    servletService = ss;
  //  }

  private UnaryPredicate relayPredicate =  new UnaryPredicate() {
      public boolean execute(Object o) {
	return (o instanceof Relay);
      }
    };

  private UnaryPredicate taskPredicate =  new UnaryPredicate() {
      public boolean execute(Object o) {
	return (o instanceof Task);
      }
    };

  private UnaryPredicate allocationPredicate =  new UnaryPredicate() {
      public boolean execute(Object o) {
	return (o instanceof PlanElement);
      }
    };

  protected String getPath() {
    return "/pizza";
  }

  /*
  public void setAgentIdentificationService(
      AgentIdentificationService agentIdService) {
    this.agentIdService = agentIdService;
    if (agentIdService == null) {
      // Revocation
    } else {
      this.localAgent = agentIdService.getMessageAddress();
      //  encLocalAgent = formURLEncode(localAgent.getAddress());
    }
  }
  */

  //  public void setBlackboardQueryService(
  //      BlackboardQueryService blackboardQueryService) {
  //    this.blackboardQueryService = blackboardQueryService;
  //  }

  public void load() {
    super.load();
    ServiceBroker sb = getServiceBroker();
    logger = (LoggingService)
      sb.getService(this, LoggingService.class, null);
    servletService = (ServletService)
      sb.getService(this, ServletService.class, null);
    servletService = (ServletService)
      sb.getService(this, ServletService.class, null);

    // prefix all logging calls with our agent name
    logger = LoggingServiceWithPrefix.add(logger, localAgent+": ");
  }

  /*
   * Creates a subscription.
   */
  protected void setupSubscriptions() { 
    relaysSubscription       = (IncrementalSubscription) blackboard.subscribe(relayPredicate);
    tasksSubscription       = (IncrementalSubscription) blackboard.subscribe(taskPredicate);
    planElementsSubscription = (IncrementalSubscription) blackboard.subscribe(allocationPredicate);

    // register with servlet service
    try {
      servletService.register(getPath(), createServlet());
    } catch (Exception e) {
      logger.warn ("could not register servlet?");
      e.printStackTrace();
    }

  }

  protected Servlet createServlet() {
    return new PizzaWorker();
  }

  /**
   * Executes Plugin functionality.
   */
  public void execute() {
    long now = System.currentTimeMillis();

    if (tasksSubscription.hasChanged()) {
      Collection added = tasksSubscription.getAddedCollection();
      for (Iterator iter = added.iterator(); iter.hasNext(); ) {
	Task task = (Task) iter.next();
	events.add (new EventInfo ("New task " + task.getUID ()+ " - " + task.getVerb() + " was published.",
				   now,
				   ""));
      }

      Collection changed = tasksSubscription.getChangedCollection();
      for (Iterator iter = changed.iterator(); iter.hasNext(); ) {
	Task task = (Task) iter.next();
	events.add (new EventInfo ("Task " + task.getUID() + " changed.",
				   now,
				   "",
				   tasksSubscription.getChangeReports(task)));
      }

      Collection removed = tasksSubscription.getRemovedCollection();
      for (Iterator iter = removed.iterator(); iter.hasNext(); ) {
	Task task = (Task) iter.next();
	events.add (new EventInfo ("Task " + task.getUID() + " was removed.",
				   now,
				   ""));
      }
    }

    if (planElementsSubscription.hasChanged()) {
      Collection added = planElementsSubscription.getAddedCollection();
      for (Iterator iter = added.iterator(); iter.hasNext(); ) {
	PlanElement planElement = (PlanElement) iter.next();
	events.add (new EventInfo ("New " + getClassName(planElement) + " " + 
				   planElement.getUID ()+ " - of task " + 
				   planElement.getTask().getUID() + " was published.",
				   now,
				   getAddedComment(planElement)));
      }

      Collection changed = planElementsSubscription.getChangedCollection();
      for (Iterator iter = changed.iterator(); iter.hasNext(); ) {
	PlanElement planElement = (PlanElement) iter.next();
	events.add (new EventInfo (getClassName(planElement) + " " + planElement.getUID() + " changed.",
				   now,
				   getChangedComment(planElement),
				   planElementsSubscription.getChangeReports(planElement)));
      }

      Collection removed = planElementsSubscription.getRemovedCollection();
      for (Iterator iter = removed.iterator(); iter.hasNext(); ) {
	PlanElement planElement = (PlanElement) iter.next();
	events.add (new EventInfo (getClassName(planElement) + " " + 
				   planElement.getUID() + " was removed.",
				   now,
				   ""));
      }
    }

    if (relaysSubscription.hasChanged()) {
      Collection added = relaysSubscription.getAddedCollection();
      for (Iterator iter = added.iterator(); iter.hasNext(); ) {
	Relay relay = (Relay) iter.next();
	events.add (new EventInfo ("New relay " + relay.getUID ()+ " was published.",
				   now,
				   getAddedRelayComment(relay)));
      }

      Collection changed = relaysSubscription.getChangedCollection();
      for (Iterator iter = changed.iterator(); iter.hasNext(); ) {
	Relay relay = (Relay) iter.next();
	events.add (new EventInfo ("Relay " + relay.getUID() + " changed.",
				   now,
				   getChangedRelayComment(relay),
				   relaysSubscription.getChangeReports(relay)));
      }

      Collection removed = relaysSubscription.getRemovedCollection();
      for (Iterator iter = removed.iterator(); iter.hasNext(); ) {
	Relay relay = (Relay) iter.next();
	events.add (new EventInfo ("Relay " + relay.getUID() + " was removed.",
				   now,
				   ""));
      }
    }

    StringBuffer buf = new StringBuffer();
    for (Iterator iter = events.iterator (); iter.hasNext(); ) {
      buf.append(iter.next() + "\n");
    }

    logger.info ("buf\n" + buf);
  }

  protected String getClassName (Object obj) {
    String classname = obj.getClass().getName ();
    int index = classname.lastIndexOf (".");
    classname = classname.substring (index+1, classname.length ());
    return classname;
  }

  protected String getAddedRelayComment (Relay relay) {
    return (relay instanceof Relay.Source) ? 
      "Source Targets : " + ((Relay.Source)relay).getTargets().iterator().next().toString() : 
      "Target Source  : " + ((Relay.Target)relay).getSource().toString();
  }

  protected String getChangedRelayComment (Relay relay) {
    return (relay instanceof Relay.Source) ? 
      "Source Query    : " + ((Relay.Source)relay).getContent().toString(): 
      "Target Response : " + ((Relay.Target)relay).getResponse().toString();
  }

  protected String getChangedComment (PlanElement planElement) {
    StringBuffer buf = new StringBuffer();

    if (planElement.getEstimatedResult() != null) {
      AspectValue [] values = planElement.getEstimatedResult().getAspectValueResults();

      boolean success = planElement.getEstimatedResult().isSuccess();
      String prefix = "Estimated - " + (success ? "success" : "failure");
      buf.append (getAspectValues (values, prefix));
    }

    if (planElement.getReportedResult () != null) {
      boolean success = planElement.getReportedResult().isSuccess();
      String prefix = "Reported - " + (success ? "success" : "failure");
      AspectValue [] values = planElement.getReportedResult().getAspectValueResults();
      buf.append (getAspectValues (values, prefix));
    }

    return buf.toString();
  }

  protected String getAspectValues(AspectValue [] values, String prefix) {
    StringBuffer buf = new StringBuffer();
    buf.append ("<table>");
    buf.append ("<tr><td>");
    buf.append (prefix + " Aspect Values" );
    buf.append ("</td></tr>");
    for (int i = 0; i < values.length; i++) {
      buf.append ("<tr><td>");
      buf.append(values[i]);
      buf.append ("</td></tr>");
    }
    buf.append ("</table>");
    return buf.toString();
  }

  protected String getAddedComment (PlanElement planElement) {
    if (planElement instanceof Allocation) {
      Allocation allocation = (Allocation) planElement;
      if (allocation.getAsset() == null)
	return "";
      else
	return "Allocation to " + allocation.getAsset();
    }
    else {
      Expansion expansion = (Expansion) planElement;
      StringBuffer buf = new StringBuffer();
      buf.append ("<table>");
      buf.append ("<tr><td>");
      buf.append ("Child tasks are :" );
      buf.append ("</td></tr>");

      for (Enumeration e = expansion.getWorkflow().getTasks(); e.hasMoreElements();) {
	Task task = (Task) e.nextElement();
	buf.append ("<tr><td>");
	
	buf.append ("Task " + task.getUID() + " " + task.getVerb());

	if (task.getDirectObject() != null) {
	  buf.append (" d.o. " + task.getDirectObject().getUID() + " ");
	  if (task.getDirectObject().getTypeIdentificationPG() != null) {
	    buf.append (task.getDirectObject().getTypeIdentificationPG().getTypeIdentification() + " - ");
	  }
	  if (task.getDirectObject().getItemIdentificationPG() != null) {
	    buf.append (task.getDirectObject().getItemIdentificationPG().getItemIdentification()); 
	  }
	}

	buf.append ("</td></tr>");
      }

      buf.append ("</table>");
      return buf.toString();
    }
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
	//	if (!stopRefresh) {
	//	  response.setHeader("Refresh", ""+Constants.SERVLET_REFRESH_INTERVAL);
	//	}
	PrintWriter out = response.getWriter();
        out.print(
            "<html><head><title>"+
            "The Pizza Party"+
	    "</title></head>"+
	    "<body>" +
	    "<p><center>Pizza Preferences</center>"+
	    getHtmlForPreferences () +
	    "<p><center>State History</center>"+
	    getHtmlForState() +
	    "</body>" +
            "</html>\n");
        out.flush();
      }
      // FIXME: Add support for FORMAT_XML and FORMAT_DATA
    }

    protected String getHtmlForPreferences () {
      blackboard.openTransaction();
      Collection pizzaPreferences = blackboard.query (new UnaryPredicate() {
	  public boolean execute(Object o) {
	    return (o instanceof PizzaPreferences);
	  }
	}
					      );
      blackboard.closeTransaction();

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
	stopRefresh = true;
	return buf.toString();
      }
    }

    protected String getHtmlForState () {
      StringBuffer buf = new StringBuffer();
      buf.append("<table border=1 align=center>");
      buf.append("<tr>");
      buf.append("<th>");
      buf.append("When");
      buf.append("</th>");
      buf.append("<th>");
      buf.append("Event");
      buf.append("</th>");
      buf.append("<th>");
      buf.append("Change Report");
      buf.append("</th>");
      buf.append("<th>");
      buf.append("Comment");
      buf.append("</th>");
      buf.append("</tr>");
      for (Iterator iter = events.iterator (); iter.hasNext(); ) {
	buf.append(iter.next());
	buf.append("\n");
      }
      buf.append("</table>");
      return buf.toString();
    }
  }

  private static class EventInfo {
    public String event;
    public long timeStamp;
    public String meaning;
    public Set changeReports = null;

    public EventInfo (String event, long timeStamp, String meaning) {
      this.event = event;
      this.timeStamp = timeStamp;
      this.meaning = meaning;
    }

    public EventInfo (String event, long timeStamp, String meaning, Set changeReports) {
      this.event = event;
      this.timeStamp = timeStamp;
      this.meaning = meaning;
      this.changeReports = changeReports;
    }

    public String toString () {
      return "<tr>" + 
	"<td>" + format.format(new Date(timeStamp)) + "</td>" +
	"<td>" + event + "</td>" +
	((changeReports != null) ? "<td>" + htmlForChangeReport() + "</td>" : "<td></td>") +
	((meaning       != null) ? "<td>" + meaning + "</td>" : "") +
	"</tr>";
    }

    public String htmlForChangeReport () {
      StringBuffer buf = new StringBuffer();
      buf.append ("<table>");

      for (Iterator iter = changeReports.iterator(); iter.hasNext(); ) {
	buf.append ("<tr><td>" + iter.next() + "</td></tr>");
      }

      buf.append ("</table>");

      return buf.toString();
    }
  }
}

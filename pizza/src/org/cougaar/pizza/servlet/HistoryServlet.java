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
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.core.util.UID;
import org.cougaar.planning.servlet.PlanViewServlet;

import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.plugin.PizzaPreferences;

/**
 * 
 * 
 */
public class HistoryServlet extends ComponentPlugin {
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
  protected String encAgentName;
  List events = new ArrayList();
  boolean showChangeReport = false;

  private SimpleDateFormat myDateFormat = new SimpleDateFormat("MM_dd_yyyy_h:mma");
  private Date myDateInstance = new Date();
  private java.text.FieldPosition myFieldPos = new java.text.FieldPosition(SimpleDateFormat.YEAR_FIELD);

  // what are we looking at?
  public static final int TASK = 0;
  public static final int PLAN_ELEMENT = 1;
  public static final int ASSET = 2;
  public static final int UNIQUE_OBJECT = 3;
  public static final int DIRECT_OBJECT = 4;

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
    return "/history";
  }

  public void load() {
    super.load();
    ServiceBroker sb = getServiceBroker();
    logger = (LoggingService)
      sb.getService(this, LoggingService.class, null);
    servletService = (ServletService)
      sb.getService(this, ServletService.class, null);

    // prefix all logging calls with our agent name
    logger = LoggingServiceWithPrefix.add(logger, localAgent+": ");
    encAgentName = encodeAgentName(agentId.getAddress());
  }

  public String encodeAgentName(String name) {
    try {
      return URLEncoder.encode(name, "UTF-8");
    } catch (java.io.UnsupportedEncodingException e) {
      // should never happen
      throw new RuntimeException("Unable to encode to UTF-8?");
    }
  }

  protected String getURL (UID uid, int which) {
    int mode=0;//PlanViewServlet.MODE_FRAME;
    switch (which) {
    case TASK:
      mode = 3;//PlanViewServlet.MODE_TASK_DETAILS;
      break;
    case PLAN_ELEMENT:
      mode = 5;//PlanViewServlet.MODE_PLAN_ELEMENT_DETAILS;
      break;
    case ASSET:
      mode = 7;//PlanViewServlet.MODE_PLAN_ELEMENT_DETAILS;
      break;
    case UNIQUE_OBJECT:
      mode = 10;//PlanViewServlet.MODE_XML_HTML_DETAILS;
      break;
    case DIRECT_OBJECT:
      mode = 15;//PlanViewServlet.MODE_XML_HTML_DETAILS;
      break;
    }

    StringBuffer buf = new StringBuffer();
    buf.append("<a href=\"/$");
    buf.append(encAgentName);
    buf.append("/tasks");
    buf.append(
	      "?"+
	      "mode" +//PlanViewServlet.MODE+
	      "="+
	      mode+
	      "&"+
	      "uid"+//PlanViewServlet.ITEM_UID+
	      "=");
    buf.append(encode(uid.toString()));
    //        buf.append("\" target=\"itemFrame\">");
    buf.append("\" target=\"_blank\">");
    //    buf.append("\"");
    buf.append(uid);
    buf.append("</a>");

    return buf.toString();
  }

  protected String encode(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (Exception e) {
      throw new IllegalArgumentException(
					 "Unable to encode URL ("+s+")");
    }
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
	String event = 
	  "New task " + getURL(task.getUID (), TASK)+ 
	  " - " + task.getVerb() + " was published by " + ((Claimable)task).getClaim();
				   
	events.add (new EventInfo (event,
				   now,
				   getTaskPreferences(task),
				   encodeHTML(task.toString())));
      }

      Collection changed = tasksSubscription.getChangedCollection();
      for (Iterator iter = changed.iterator(); iter.hasNext(); ) {
	Task task = (Task) iter.next();
	events.add (new EventInfo ("Task " + getURL(task.getUID(), TASK) + " changed.",
				   now,
				   "",
				   tasksSubscription.getChangeReports(task),
				   encodeHTML(task.toString())));
      }

      Collection removed = tasksSubscription.getRemovedCollection();
      for (Iterator iter = removed.iterator(); iter.hasNext(); ) {
	Task task = (Task) iter.next();
	events.add (new EventInfo ("Task " + getURL(task.getUID(), TASK) + " was removed.",
				   now,
				   ""));
      }
    }

    if (planElementsSubscription.hasChanged()) {
      Collection added = planElementsSubscription.getAddedCollection();
      for (Iterator iter = added.iterator(); iter.hasNext(); ) {
	PlanElement planElement = (PlanElement) iter.next();
	String event = 
	  "New " + getClassName(planElement) + 
	  " " + getURL(planElement.getUID (), PLAN_ELEMENT) + 
	  " - of task " + getURL(planElement.getTask().getUID(), TASK) + 
	  " was published by " + planElement.getClaimable().getClaim();
				   
	events.add (new EventInfo (event,
				   now,
				   getAddedComment(planElement),
				   encodeHTML(planElement.toString())));
      }

      Collection changed = planElementsSubscription.getChangedCollection();
      for (Iterator iter = changed.iterator(); iter.hasNext(); ) {
	PlanElement planElement = (PlanElement) iter.next();
	String event = 
	  getClassName(planElement) + " " + 
	  getURL(planElement.getUID(), PLAN_ELEMENT) + " changed.";

	events.add (new EventInfo (event,
				   now,
				   getChangedComment(planElement),
				   planElementsSubscription.getChangeReports(planElement),
				   encodeHTML(planElement.toString())));
      }

      Collection removed = planElementsSubscription.getRemovedCollection();
      for (Iterator iter = removed.iterator(); iter.hasNext(); ) {
	PlanElement planElement = (PlanElement) iter.next();
	String event = 
	  getClassName(planElement) + " " + 
	  getURL(planElement.getUID(), PLAN_ELEMENT) + " was removed.";

	events.add (new EventInfo (event,
				   now,
				   ""));
      }
    }

    if (relaysSubscription.hasChanged()) {
      Collection added = relaysSubscription.getAddedCollection();
      for (Iterator iter = added.iterator(); iter.hasNext(); ) {
	Relay relay = (Relay) iter.next();
	events.add (new EventInfo ("New relay " + getURL(relay.getUID(), UNIQUE_OBJECT)+ " was published.",
				   now,
				   getAddedRelayComment(relay), 
				   encodeHTML(relay.toString())));
      }

      Collection changed = relaysSubscription.getChangedCollection();
      for (Iterator iter = changed.iterator(); iter.hasNext(); ) {
	Relay relay = (Relay) iter.next();
	events.add (new EventInfo ("Relay " + getURL(relay.getUID(), UNIQUE_OBJECT) + " changed.",
				   now,
				   getChangedRelayComment(relay),
				   relaysSubscription.getChangeReports(relay),
				   encodeHTML(relay.toString())));
      }

      Collection removed = relaysSubscription.getRemovedCollection();
      for (Iterator iter = removed.iterator(); iter.hasNext(); ) {
	Relay relay = (Relay) iter.next();
	events.add (new EventInfo ("Relay " + getURL(relay.getUID(), UNIQUE_OBJECT) + " was removed.",
				   now,
				   "Response returned, so request removed from blackboard."));
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
    StringBuffer buf = new StringBuffer();

    if (relay instanceof Relay.Source) {
      Relay.Source sourceRelay = (Relay.Source) relay;
      if (!sourceRelay.getTargets().isEmpty()) {
	buf.append("Source Targets : " + encodeHTML(sourceRelay.getTargets().iterator().next().toString()) + "<br/>");
	if (sourceRelay.getContent() instanceof CommunityDescriptor) {
	  CommunityDescriptor response = (CommunityDescriptor) sourceRelay.getContent();
	  Community community = (Community)response.getCommunity();
	  buf.append(getCommunityText("Source Response : ", community));
	}
      }
    }
    if (relay instanceof Relay.Target) {
      Relay.Target targetRelay = (Relay.Target)relay;
      //    buf.append("instanceof " + getClassName(targetRelay.getResponse()) + " ");
      if (targetRelay.getResponse() instanceof CommunityResponse) {
	CommunityResponse response = (CommunityResponse) targetRelay.getResponse();
	Community community = (Community)response.getContent();
	buf.append("Entity " + targetRelay.getSource() + " registers with community " + community.getName());
      }
      else {
	buf.append("Target Source  : " + encodeHTML(((Relay.Target)relay).getSource().toString()));
      }
    }

    return buf.toString();
  }

  protected String getChangedRelayComment (Relay relay) {
    StringBuffer buf = new StringBuffer();
    if (relay instanceof Relay.Source) { 
      buf.append("Response Returned</br>");

      Relay.Source sourceRelay = (Relay.Source)relay;
      if (!(sourceRelay.getContent() instanceof Relay))
	buf.append("Source Query    : " + encodeHTML(sourceRelay.getContent().toString()) + "<br/>");
      
      if (sourceRelay.getContent() instanceof CommunityDescriptor) {
	CommunityDescriptor response = (CommunityDescriptor) sourceRelay.getContent();
	Community community = (Community)response.getCommunity();
	buf.append(getCommunityText("Source Response : ", community));
      }
    }
    if (relay instanceof Relay.Target) {
      Relay.Target targetRelay = (Relay.Target)relay;
      //  buf.append("instanceof " + getClassName(targetRelay.getResponse()) + " ");
      if (targetRelay.getResponse() instanceof CommunityResponse) {
	CommunityResponse response = (CommunityResponse) targetRelay.getResponse();
	Community community = (Community)response.getContent();
	buf.append(getCommunityText("Target Response : ", community));
      }
      else if (targetRelay.getResponse() instanceof CommunityDescriptor) {
	CommunityDescriptor response = (CommunityDescriptor) targetRelay.getResponse();
	Community community = (Community)response.getCommunity();
	buf.append(getCommunityText("Target Response : ", community));
      }
      else {
	buf.append("Target Response : " + encodeHTML(targetRelay.getResponse().toString()));
      }
    }

    return buf.toString();
  }

  protected String getCommunityText(String prefix, Community community) {
    StringBuffer buf = new StringBuffer();
    buf.append (prefix);
    buf.append(" Community <b>" + community.getName() + "</b> with members : "); 
    //    buf.append("<table>");
    for (Iterator iter = community.getEntities().iterator(); iter.hasNext(); ) {
      buf.append(
		 "<font size=small color=mediumblue>"+
		 "<li>");
      //      buf.append("<tr><td>");
      buf.append(iter.next());
      //      buf.append("</td></tr>");
      buf.append(
		 "</li>"+
		 "</font>\n");
    }
    //    buf.append("</table>");
    return buf.toString();
  }

  protected String getChangedComment (PlanElement planElement) {
    StringBuffer buf = new StringBuffer();

    if (planElement.getEstimatedResult() != null) {
      AspectValue [] values = planElement.getEstimatedResult().getAspectValueResults();
      boolean success = planElement.getEstimatedResult().isSuccess();

      String prefix = "Estimated Allocation Result - " + 
	(success ? "<font color=\"green\">success</font>" : "<font color=\"red\">failure</font>");
      buf.append (prefix + "<br/>");
      buf.append (getAspectValues2 (values));
    }

    if (planElement.getReportedResult () != null) {
      AspectValue [] values = planElement.getReportedResult().getAspectValueResults();
      boolean success = planElement.getReportedResult().isSuccess();

      String prefix = "<br/>Reported Allocation Result - " + 
	(success ? "<font color=\"green\">success</font>" : "<font color=\"red\">failure</font>");
      buf.append (prefix + "<br/>");
      buf.append (getAspectValues2 (values));
      buf.append ("<br/>");
      if (!success) {
	if (planElement.getTask().getPreferences ().hasMoreElements()) {
	  buf.append ("<br>Failed because reported aspect values did not satisfy task preferences:");
	  buf.append (getTaskPreferences(planElement.getTask()));
	}
	else {
	  buf.append ("<br>Failed because one or more child tasks failed.");
	}
      }
    }

    return buf.toString();
  }

  protected String getAspectValues(AspectValue [] values, String prefix) {
    StringBuffer buf = new StringBuffer();
    buf.append ("<table>");
    buf.append ("<tr><td>");
    buf.append (prefix + " Aspect Values:" );
    buf.append ("</td></tr>");
    for (int i = 0; i < values.length; i++) {
      buf.append ("<tr><td>");
      buf.append ("Type ");
      buf.append (AspectValue.aspectTypeToString(values[i].getType()));
      buf.append (" - Value ");
      buf.append (values[i].getValue());
      buf.append ("</td></tr>");
    }
    buf.append ("</table>");
    return buf.toString();
  }

  protected String getAspectValues2 (AspectValue [] values) {
    StringBuffer buf = new StringBuffer();
    // for all (type, result) pairs
    for (int i = 0; i < values.length; i++) {
      AspectValue avi = values[i];
      buf.append(getAspectValue(avi));
    }
    return buf.toString();
  }

  protected String getAspectValue(AspectValue avi) {
    StringBuffer buf = new StringBuffer();
    buf.append(
	       "<font size=small color=mediumblue>"+
	       "<li>");
    // show type
    buf.append(
	       AspectValue.aspectTypeToString(
					      avi.getType()));
    buf.append("= ");
    // show value
    if (avi instanceof TimeAspectValue) {
      // print the date in our format
      long time = ((TimeAspectValue) avi).timeValue();
      buf.append(getTimeString(time));
    } else {
      buf.append(avi.getValue());
    }
    buf.append(
	       "</li>"+
	       "</font>\n");
    return buf.toString();
  }

  /**
   * getTimeString.
   * <p>
   * Formats time to String.
   */
  protected String getTimeString(long time) {
    synchronized (myDateFormat) {
      myDateInstance.setTime(time);
      return 
	myDateFormat.format(
			    myDateInstance,
			    new StringBuffer(20), 
			    myFieldPos
			    ).toString();
    }
  }


  protected String getTaskPreferences(Task task) {
    Enumeration prefs = task.getPreferences ();
    StringBuffer buf = new StringBuffer();

    buf.append ("<table>");
    buf.append ("<tr><td>");
    buf.append (" Preferences : " );
    buf.append ("</td></tr>");
    for (; prefs.hasMoreElements (); ) {
      Preference pref = (Preference) prefs.nextElement ();
      AspectValue prefav = 
        pref.getScoringFunction().getBest().getAspectValue();
      buf.append ("<tr><td>");
      buf.append(getAspectValue(prefav));
      /*
      buf.append ("Type ");
      buf.append (AspectValue.aspectTypeToString(prefav.getType()));
      buf.append (" - Value");
      buf.append (prefav.getValue());
      */
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
	return "Place " + allocation.getTask().getVerb() + 
	  " with " + getTypeAndItemInfo(allocation.getAsset());
    }
    else if (planElement instanceof Expansion) {
      Expansion expansion = (Expansion) planElement;
      StringBuffer buf = new StringBuffer();
      buf.append ("<table>");
      buf.append ("<tr><td>");
      buf.append ("Child tasks are :" );
      buf.append ("</td></tr>");

      for (Enumeration e = expansion.getWorkflow().getTasks(); e.hasMoreElements();) {
	Task task = (Task) e.nextElement();
	buf.append ("<tr><td>");
	
	buf.append ("Task " + getURL(task.getUID(), TASK) + " " + task.getVerb());

	if (task.getDirectObject() != null) {
	  buf.append (" direct object ");// + getURL(task.getDirectObject().getUID(), DIRECT_OBJECT) + " ");
	  buf.append (getTypeAndItemInfo(task.getDirectObject()));
	}

	buf.append ("</td></tr>");
      }

      buf.append ("</table>");
      return buf.toString();
    }
    return "";
  }

  protected String getTypeAndItemInfo (Asset asset) {
    StringBuffer buf = new StringBuffer();

    if (asset.getTypeIdentificationPG() != null) {
      buf.append (asset.getTypeIdentificationPG().getTypeIdentification() + " - ");
    }
    if (asset.getItemIdentificationPG() != null) {
      buf.append (asset.getItemIdentificationPG().getItemIdentification()); 
    }

    return buf.toString();
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
	    //	    "<p><center>Pizza Preferences</center>"+
	    //	    getHtmlForPreferences () +
	    "<p><center><h1>State History</h1></center>"+
	    getHtmlForState() +
	    "</body>" +
            "</html>\n");
        out.flush();
      }
      else if (format == FORMAT_XML) {
      }
      else if (format == FORMAT_DATA) {
      }
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
      buf.append("Object");
      buf.append("</th>");
      if (showChangeReport) {
	buf.append("<th>");
	buf.append("Change Report");
	buf.append("</th>");
      }
      buf.append("<th>");
      buf.append("Comment");
      buf.append("</th>");
      buf.append("</tr>");
      long lastTime = -1;

      boolean colorRowGrey = false;
      for (Iterator iter = events.iterator (); iter.hasNext(); ) {
	EventInfo event = (EventInfo)iter.next();

	if (event.timeStamp != lastTime && event.timeStamp != -1) {
	  colorRowGrey = !colorRowGrey;
	  lastTime = event.timeStamp;
	}

	buf.append(event.toString(colorRowGrey, showChangeReport));
	buf.append("\n");
      }
      buf.append("</table>");
      return buf.toString();
    }
  }

    /**
     * Encodes a string that may contain HTML syntax-significant
     * characters by replacing with a character entity.
     **/
    protected String encodeHTML(String s) {
      if (s == null)
	return "";

      boolean noBreakSpaces = true;
      boolean sawEquals = false;
      StringBuffer buf = null;  // In case we need to edit the string
      int ix = 0;               // Beginning of uncopied part of s
      for (int i = 0, n = s.length(); i < n; i++) {
	String replacement = null;
	switch (s.charAt(i)) {
	case '"': replacement = "&quot;"; break;
	case '[': replacement = "<br/>["; break;
	case '<': replacement = "&lt;"; break;
	case '>': replacement = "&gt;"; break;
	case '&': replacement = "&amp;"; break;
	case '=': sawEquals = true; break;
	case ' ': 
	  if (sawEquals && (i+1 < n) && s.charAt(i+1) != '>') 
	    replacement = "<br/>";
	  else if (noBreakSpaces) 
	    replacement = "&nbsp;"; 
	  sawEquals=false;
	  break;
	}
	if (replacement != null) {
	  if (buf == null) buf = new StringBuffer();
	  buf.append(s.substring(ix, i));
	  buf.append(replacement);
	  ix = i + 1;
	}
      }
      if (buf != null) {
	buf.append(s.substring(ix));
	return buf.toString();
      } else {
	return s;
      }
    }

  private static class EventInfo {
    public String event;
    public long timeStamp;
    public String meaning;
    public Set changeReports = null;
    //    public String uid;
    public String toStringResult;

    public EventInfo (String event, long timeStamp, String meaning) {
      //      this.uid = uid;
      this.event = event;
      this.timeStamp = timeStamp;
      this.meaning = meaning;
    }

    public EventInfo (String event, long timeStamp, String meaning, Set changeReports) {
      this (event, timeStamp, meaning);
      this.changeReports = changeReports;
    }

    public EventInfo (String event, long timeStamp, String meaning, String toStringResult) {
      this (event, timeStamp, meaning);
      this.toStringResult = toStringResult;
    }

    public EventInfo (String event, long timeStamp, String meaning, Set changeReports, String toStringResult) {
      this (event, timeStamp, meaning, changeReports);
      this.toStringResult = toStringResult;
    }

    public String toString () {
      return toString (true, false);
    }

    public String toString (boolean odd, boolean showChangeReport) {
      String color = odd ? "#FFFFFF" : "#c0c0c0";
      StringBuffer buf = new StringBuffer();
      buf.append("<tr BGCOLOR=" + color + ">");
      buf.append("<td>" + format.format(new Date(timeStamp)) + "</td>");
      buf.append("<td>" + event + "</td>");
      buf.append("<td>" + toStringResult + "</td>");

      if (showChangeReport) {
	buf.append ("<td>");

	if (changeReports == null) {
	  buf.append(htmlForChangeReport());
	}

	buf.append("</td>");
      }

      if (meaning != null) {
	//	if (meaning.startsWith ("<table>")) { // don't encode html we produce
	  buf.append ("<td>" + meaning + "</td>");
	  //	}
	  //	else {
	  //	  buf.append ("<td>" + encodeHTML(meaning) + "</td>");
	  //	}
      }

      buf.append("</tr>");
      return buf.toString();
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

    /**
     * Encodes a string that may contain HTML syntax-significant
     * characters by replacing with a character entity.
     **/
    protected String encodeHTML(String s) {
      if (s == null)
	return "";

      boolean noBreakSpaces = true;
      boolean sawEquals = false;
      StringBuffer buf = null;  // In case we need to edit the string
      int ix = 0;               // Beginning of uncopied part of s
      for (int i = 0, n = s.length(); i < n; i++) {
	String replacement = null;
	switch (s.charAt(i)) {
	case '"': replacement = "&quot;"; break;
	case '<': replacement = "&lt;"; break;
	case '>': replacement = "&gt;"; break;
	case '&': replacement = "&amp;"; break;
	case '=': sawEquals = true; break;
	case ' ': 
	  if (sawEquals) 
	    replacement = "<br/>";
	  else if (noBreakSpaces) 
	    replacement = "&nbsp;"; 
	  sawEquals=false;
	  break;
	}
	if (replacement != null) {
	  if (buf == null) buf = new StringBuffer();
	  buf.append(s.substring(ix, i));
	  buf.append(replacement);
	  ix = i + 1;
	}
      }
      if (buf != null) {
	buf.append(s.substring(ix));
	return buf.toString();
      } else {
	return s;
      }
    }

  }
}

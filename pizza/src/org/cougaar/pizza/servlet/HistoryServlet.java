/*
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
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.community.CommunityDescriptor;
import org.cougaar.community.manager.Request;

import org.cougaar.core.blackboard.Claimable;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ServletService;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

import org.cougaar.multicast.AttributeBasedAddress;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.Entity;
import org.cougaar.planning.ldm.plan.Aggregation;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.AssetTransfer;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TimeAspectValue;

import org.cougaar.util.Arguments;
import org.cougaar.util.UnaryPredicate;

/**
 * Generic debugging servlet that displays all Adds/Changes/Removes on Blackboard objects. 
 * <p>
 * Specifically tracks changes on Relays, Tasks, PlanElements, Assets,
 * UniqueObjects, and implementations of {@link HistoryServletFriendly}.  
 * For every event, attempts to explain the event.
 * For instance, when a Relay is first published, it shows which Agent
 * the Relay is being sent to.
 * <p>
 * The Servlet lets the user sort events by time, then by uid, or by uid
 * only.  Sorting by uid shows the complete lifecycle of a Blackboard
 * object; especially useful for transient objects that live on the
 * Blackboard only a short time.  When sorting by time, each group of 
 * changes that happens in the same execute cycle is drawn with 
 * the same background color.  When sorting by uid, each distinct 
 * object is drawn with the same background color.
 * <p>
 * The servlet has a "show details" link which will show the toString for 
 * the blackboard object at that time.
 * <p>
 * Also shows which plugin initially published an object to the Blackboard
 * if that information is available (for Claimables).
 * <p>
 *
 * Has a default limit of 1000 events, but this can be set by the 
 * MAX_EVENTS_REMEMBERED component argument to the servlet.
 * E.g. :
 *   &lt;argument&gt;MAX_EVENTS_REMEMBERED=5000&lt;/argument&gt;
 * FIXME: MAX_ROLE_SCHEDULE_ELEMENTS and MAX_CHILD_TASKS
 */
public class HistoryServlet extends ComponentPlugin {
  // Some defaults
  protected final int INITIAL_MAX_ENTRIES=1000; // Default mas # events to track
  protected final int INITIAL_MAX_ROLE_SCHEDULE_ELEMENTS=5;
  protected final int INITIAL_MAX_CHILD_TASKS=5;

  // Actual value of parametrized preferences
  private int maxEvents;
  private int maxRoleScheduleElements;
  private int maxChildTasks;

  /** initialize args to the empty instance */
  private Arguments args = Arguments.EMPTY_INSTANCE;
  protected MessageAddress localAgent;

  protected LoggingService logger;
  private ServletService servletService;

  // Subscribe to various kinds of objects
  private IncrementalSubscription relaysSubscription;
  private IncrementalSubscription tasksSubscription; 
  private IncrementalSubscription planElementsSubscription;
  private IncrementalSubscription assetsSubscription;
  // Note that this is an everything-but-the-above subscription
  private IncrementalSubscription uniqueObjectsSubscription;

  protected static SimpleDateFormat format = new SimpleDateFormat ("MM-dd hh:mm:ss:SSS");
  protected String encAgentName;

  // The actual Blackboard history we collect
  protected SortedSet events = new TreeSet();

  // Has the events list been trimmed?
  private boolean didDropOldEntries = false;

  // User preferences
  boolean showChangeReport = false;
  boolean sortByUID = false;
  boolean showDetails = false;

  private SimpleDateFormat myDateFormat = new SimpleDateFormat("MM_dd_yyyy_h:mma");
  private Date myDateInstance = new Date();
  private FieldPosition myFieldPos = new FieldPosition(SimpleDateFormat.YEAR_FIELD);

  // what are we looking at?
  public static final int TASK = 0;
  public static final int PLAN_ELEMENT = 1;
  public static final int ASSET = 2;
  public static final int UNIQUE_OBJECT = 3;
  public static final int DIRECT_OBJECT = 4;

  // What kind of event was this?
  public static final int ADDED   = 0;
  public static final int CHANGED = 1;
  public static final int REMOVED = 2;

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

  private UnaryPredicate assetPredicate =  new UnaryPredicate() {
      public boolean execute(Object o) {
	return (o instanceof Asset);
      }
    };

  private UnaryPredicate uniqueObjectPredicate =  new UnaryPredicate() {
      public boolean execute(Object o) {
	return (!(o instanceof Relay) &&
		!(o instanceof Task) &&
		!(o instanceof PlanElement) &&
		!(o instanceof Asset) &&
		 (o instanceof UniqueObject));
      }
    };

  /** "setParameter" is only called if a plugin has parameters */
  public void setParameter(Object o) {
    args = new Arguments(o);
  }  

  // Load this servlet at /history
  protected String getPath() {
    return "/history";
  }

  public void setServletService(ServletService servletService) {
    this.servletService = servletService;
  }

  public void load() {
    super.load();
    ServiceBroker sb = getServiceBroker();
    logger = (LoggingService)
      sb.getService(this, LoggingService.class, null);
    if (logger == null)
      logger = LoggingService.NULL;

    encAgentName = encodeAgentName(agentId.getAddress());

    // when plugin is added to agent you can set this to a smaller or
    // larger value depending on how much of the heap you want
    // to fill up with events
    maxEvents = args.getInt("MAX_EVENTS_REMEMBERED", INITIAL_MAX_ENTRIES);
    maxRoleScheduleElements = 
      args.getInt("MAX_ROLE_SCHEDULE_ELEMENTS", INITIAL_MAX_ROLE_SCHEDULE_ELEMENTS);
    maxChildTasks = args.getInt("MAX_CHILD_TASKS", INITIAL_MAX_CHILD_TASKS);
    if (logger.isInfoEnabled()) {
      logger.info ("max events " + args.getInt("MAX_EVENTS_REMEMBERED", 55));
      logger.info ("args is " + args);
    }
  }

  // Every load() should be matched with an unload() 
  public void unload() {
    if (servletService != null) {
      getServiceBroker().releaseService(this, ServletService.class, servletService);
      servletService = null;
    }
    if (logger != LoggingService.NULL) {
      getServiceBroker().releaseService(this, LoggingService.class, logger);
      logger = null;
    }

    super.unload();
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
    tasksSubscription        = (IncrementalSubscription) blackboard.subscribe(taskPredicate);
    planElementsSubscription = (IncrementalSubscription) blackboard.subscribe(allocationPredicate);
    assetsSubscription       = (IncrementalSubscription) blackboard.subscribe(assetPredicate);
    uniqueObjectsSubscription = (IncrementalSubscription) blackboard.subscribe(uniqueObjectPredicate);

    // register with servlet service
    try {
      servletService.register(getPath(), createServlet());
    } catch (Exception e) {
      if (logger.isWarnEnabled())
	logger.warn ("could not register servlet?");
      e.printStackTrace();
    }

  }

  protected Servlet createServlet() {
    return new HistoryWorker();
  }

  /**
   * Executes Plugin functionality.
   */
  public void execute() {
    long now = currentTimeMillis(); // this is scenario time.

    try {
      // synchronized prevents servlet from iterating over events
      // and adding to list of known events at same time
      synchronized (events) {
	checkTasks(now);
	checkPlanElements(now);
	checkRelays(now);
	checkAssets(now);
	checkUniqueObjects(now);
      }
    } catch (Exception e) {
      if (logger.isWarnEnabled())
	logger.warn ("got exception", e);
    }

    if (logger.isInfoEnabled()) {
      StringBuffer buf = new StringBuffer();
      synchronized (events) {
	for (Iterator iter = events.iterator (); iter.hasNext(); ) {
	  buf.append(iter.next() + "\n");
	}
      }

      logger.info ("buf\n" + buf);
    }
  }

  protected void checkTasks (long now) {
    if (tasksSubscription.hasChanged()) {
      Collection added = tasksSubscription.getAddedCollection();
      for (Iterator iter = added.iterator(); iter.hasNext(); ) {
	Task task = (Task) iter.next();
	String event = 
	  "Task " + getURL(task.getUID (), TASK)+ 
	  " - " + task.getVerb() + 
	  "<br/>was published by " + ((Claimable)task).getClaim();
				   
	addEvent (new EventInfo (ADDED, 
				 task.getUID().toString(),
				 event,
				 now,
				 getAddedTaskComment(task),
				 encodeHTML(task.toString())));
      }

      Collection changed = tasksSubscription.getChangedCollection();
      for (Iterator iter = changed.iterator(); iter.hasNext(); ) {
	Task task = (Task) iter.next();
	addEvent (new EventInfo (CHANGED,
				 task.getUID().toString(),
				 "Task " + getURL(task.getUID(), TASK),
				 now,
				 "",
				 tasksSubscription.getChangeReports(task),
				 encodeHTML(task.toString())));
      }

      Collection removed = tasksSubscription.getRemovedCollection();
      for (Iterator iter = removed.iterator(); iter.hasNext(); ) {
	Task task = (Task) iter.next();
	addEvent (new EventInfo (REMOVED,
				 task.getUID().toString(),
				 "Task " + getURL(task.getUID(), TASK),
				 now,
				 ""));
      }
    }
  }

  protected String getAddedTaskComment (Task task) {
    StringBuffer buf = new StringBuffer();

    buf.append(task.getVerb());
    buf.append(", ");
    if (task.getDirectObject() != null) {
      buf.append (" direct object ");
      buf.append (getTypeAndItemInfo(task.getDirectObject()));
    }
    buf.append (getTaskPreferences(task));

    return buf.toString();
  }

  protected void checkPlanElements(long now) {
    if (planElementsSubscription.hasChanged()) {
      Collection added = planElementsSubscription.getAddedCollection();
      for (Iterator iter = added.iterator(); iter.hasNext(); ) {
	PlanElement planElement = (PlanElement) iter.next();
	String event = 
	  "" + getClassName(planElement) + 
	  " " + getURL(planElement.getUID (), PLAN_ELEMENT) + 
	  "<br/>of task " + getURL(planElement.getTask().getUID(), TASK) + 
	  "<br/>was published by " + planElement.getClaimable().getClaim();
				   
	addEvent (new EventInfo (ADDED,
				 planElement.getUID().toString(),
				   event,
				   now,
				   getAddedComment(planElement),
				   encodeHTML(planElement.toString())));
      }

      Collection changed = planElementsSubscription.getChangedCollection();
      for (Iterator iter = changed.iterator(); iter.hasNext(); ) {
	PlanElement planElement = (PlanElement) iter.next();
	String event = 
	  getClassName(planElement) + " " + 
	  getURL(planElement.getUID(), PLAN_ELEMENT);

	addEvent (new EventInfo (CHANGED,
				 planElement.getUID().toString(),
				   event,
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
	  getURL(planElement.getUID(), PLAN_ELEMENT);

	addEvent (new EventInfo (REMOVED,
				 planElement.getUID().toString(),
				   event,
				   now,
				   ""));
      }
    }
  }

  protected void checkRelays (long now) {
    if (relaysSubscription.hasChanged()) {
      Collection added = relaysSubscription.getAddedCollection();
      for (Iterator iter = added.iterator(); iter.hasNext(); ) {
	Relay relay = (Relay) iter.next();
	addEvent (new EventInfo (ADDED,
				 relay.getUID().toString(),
				 "Relay " + getURL(relay.getUID(), UNIQUE_OBJECT),
				 now,
				 getAddedRelayComment(relay), 
				 encodeHTML(relay.toString())));
      }

      Collection changed = relaysSubscription.getChangedCollection();
      for (Iterator iter = changed.iterator(); iter.hasNext(); ) {
	Relay relay = (Relay) iter.next();
	addEvent (new EventInfo (CHANGED,
				 relay.getUID().toString(),
				 "Relay " + getURL(relay.getUID(), UNIQUE_OBJECT),
				 now,
				 getChangedRelayComment(relay),
				 relaysSubscription.getChangeReports(relay),
				 encodeHTML(relay.toString())));
      }

      Collection removed = relaysSubscription.getRemovedCollection();
      for (Iterator iter = removed.iterator(); iter.hasNext(); ) {
	Relay relay = (Relay) iter.next();
	addEvent (new EventInfo (REMOVED,
				 relay.getUID().toString(),
				 "Relay " + getURL(relay.getUID(), UNIQUE_OBJECT),
				 now,
				 "Request removed from blackboard."));
      }
    }
  }

  protected void checkAssets (long now) {
    if (assetsSubscription.hasChanged()) {
      Collection added = assetsSubscription.getAddedCollection();
      for (Iterator iter = added.iterator(); iter.hasNext(); ) {
	Asset asset = (Asset) iter.next();
	addEvent (new EventInfo (ADDED,
				 asset.getUID().toString(),
				 "Asset " + getURL(asset.getUID(), ASSET),
				 now,
				 getAddedAssetComment(asset),
				 encodeHTML(asset.toString())));
      }

      Collection changed = assetsSubscription.getChangedCollection();
      for (Iterator iter = changed.iterator(); iter.hasNext(); ) {
	Asset asset = (Asset) iter.next();
	addEvent (new EventInfo (CHANGED,
				 asset.getUID().toString(),
				 "Asset " + getURL(asset.getUID(), ASSET),
				 now,
				 getChangedAssetComment(asset),
				 encodeHTML(asset.toString())));
      }

      Collection removed = assetsSubscription.getRemovedCollection();
      for (Iterator iter = removed.iterator(); iter.hasNext(); ) {
	Asset asset = (Asset) iter.next();
	addEvent (new EventInfo (REMOVED,
				 asset.getUID().toString(),
				 "Asset " + getURL(asset.getUID(), ASSET),
				 now,
				 ""));
      }
    }
  }

  protected String getAddedAssetComment (Asset asset) {
    return getChangedAssetComment(asset);
  }

  protected String getChangedAssetComment (Asset asset) {
    StringBuffer buf = new StringBuffer();

    buf.append (getClassName(asset));
    buf.append (" : <b>");
    buf.append (getTypeAndItemInfo(asset));
    buf.append ("</b><br/>"); 

    if (asset instanceof Entity) {
      Entity entity = (Entity) asset;

      if (!entity.getEntityPG ().getRoles().isEmpty()) {
	buf.append (" with roles : "); 
      }

      for (Iterator iter = entity.getEntityPG ().getRoles().iterator(); iter.hasNext(); ) {
	buf.append(
		   "<font size=small color=mediumblue>"+
		   "<li>");
	Object obj = iter.next();
	buf.append (getClassName(obj) + " - " + obj);
	buf.append(
		   "</li>"+
		   "</font>\n");
      }

      Collection relationships = 
	entity.getRelationshipSchedule().getMatchingRelationships(new UnaryPredicate() {
	    public boolean execute(Object o) { return true; }
	  });

      if (!relationships.isEmpty()) {
	buf.append("<br/>with relationships : "); 
      }

      for (Iterator iter = relationships.iterator(); iter.hasNext(); ) {
	buf.append(
		   "<font size=small color=mediumblue>"+
		   "<li>");

	Object obj = iter.next();
	// buf.append (getClassName(obj) + " - " + obj);
	if (obj instanceof Relationship) {
	  Relationship relation = (Relationship) obj;
	  buf.append (relation.getRoleA().toString());
	  buf.append ("=");

	  if (relation.getA() instanceof Asset) {
	    buf.append (getTypeAndItemInfo ((Asset) relation.getA()));
	  }

	  buf.append ("<br/>" + relation.getRoleB().toString());
	  buf.append ("=");

	  if (relation.getB() instanceof Asset) {
	    buf.append (getTypeAndItemInfo ((Asset) relation.getB()));
	  }
	}

	buf.append(
		   "</li>"+
		   "</font>\n");
      }

    }

    buf.append(getRoleSchedule(asset)); 

    if (asset instanceof HistoryServletFriendly) {
      buf.append (((HistoryServletFriendly)asset).toHTML(CHANGED));
    }

    return buf.toString();
  }

  protected String getRoleSchedule (Asset asset) {
    StringBuffer buf = new StringBuffer();

    if (asset.getRoleSchedule().getRoleScheduleElements().hasMoreElements()) {
      buf.append("<br/>with role schedule : "); 
    }

    int numShown = 0;
    for (Enumeration enum = asset.getRoleSchedule().getRoleScheduleElements(); 
	 enum.hasMoreElements(); numShown++) {
      buf.append(
		 "<font size=small color=mediumblue>"+
		 "<li>");
      Object elem = enum.nextElement();
      if (numShown >= maxRoleScheduleElements) {
	buf.append("... (more than ");
	buf.append(maxRoleScheduleElements);
	buf.append(")");
	buf.append("</li>"+
		   "</font>\n");
	break;
      }
      else {
	if (elem instanceof Allocation) {
	  buf.append("Allocation to " + getTypeAndItemInfo (((Allocation) elem).getAsset()));
	}
	else if (elem instanceof PlanElement) {
	  buf.append (getAddedComment ((PlanElement) elem));
	}
	else {
	  buf.append (enum.nextElement());
	}
      }

      buf.append("</li>"+
		 "</font>\n");
    }

    return buf.toString();
  }

  protected void checkUniqueObjects (long now) {
    if (uniqueObjectsSubscription.hasChanged()) {
      Collection added = uniqueObjectsSubscription.getAddedCollection();
      for (Iterator iter = added.iterator(); iter.hasNext(); ) {
	UniqueObject uniqueObject = (UniqueObject) iter.next();
	addEvent (new EventInfo (ADDED,
				 uniqueObject.getUID().toString(),
				 "UniqueObject " + getURL(uniqueObject.getUID(), UNIQUE_OBJECT), 
				 now,
				 getAddedUniqueObjectComment (uniqueObject),
				 encodeHTML(uniqueObject.toString())));
      }

      Collection changed = uniqueObjectsSubscription.getChangedCollection();
      for (Iterator iter = changed.iterator(); iter.hasNext(); ) {
	UniqueObject uniqueObject = (UniqueObject) iter.next();
	addEvent (new EventInfo (CHANGED,
				 uniqueObject.getUID().toString(),
				   "UniqueObject " + getURL(uniqueObject.getUID(), UNIQUE_OBJECT) + " changed.",
				   now,
				   getChangedUniqueObjectComment(uniqueObject),
				   encodeHTML(uniqueObject.toString())));
      }

      Collection removed = uniqueObjectsSubscription.getRemovedCollection();
      for (Iterator iter = removed.iterator(); iter.hasNext(); ) {
	UniqueObject uniqueObject = (UniqueObject) iter.next();
	addEvent (new EventInfo (REMOVED,
				 uniqueObject.getUID().toString(),
				   "UniqueObject " + getURL(uniqueObject.getUID(), UNIQUE_OBJECT) + " was removed.",
				   now,
				   getRemovedUniqueObjectComment(uniqueObject)));
      }
    }
  }

  protected String getAddedUniqueObjectComment (UniqueObject unique) {
    StringBuffer buf = new StringBuffer();

    buf.append("A ");
    buf.append(getClassName(unique));
    buf.append(" object.<br/>");

    if (unique instanceof HistoryServletFriendly) {
      buf.append (((HistoryServletFriendly)unique).toHTML(ADDED));
    }

    return buf.toString();
  }

  protected String getChangedUniqueObjectComment (UniqueObject unique) {
    return getAddedUniqueObjectComment (unique);
  }

  protected String getRemovedUniqueObjectComment (UniqueObject unique) {
    return getAddedUniqueObjectComment (unique);
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
      buf.append (showTargetAddresses (sourceRelay));

      if (sourceRelay.getContent() instanceof CommunityDescriptor) {
	CommunityDescriptor response = (CommunityDescriptor) sourceRelay.getContent();
	Community community = (Community)response.getCommunity();
	buf.append ("<br/>");
	buf.append(getCommunityText("Source Relay Response : ", community));
      }
      else if (sourceRelay.getContent() instanceof Request) {
	buf.append("<br/>Community Request: ");
	Request request = (Request) sourceRelay.getContent();
	buf.append(request.getRequestTypeAsString(request.getRequestType()));
	buf.append("<br/>Source: ");
	buf.append(request.getSource());
	buf.append("<br/>Entity: ");
	buf.append(request.getEntity());
      }
      else {
	buf.append("<br/>Content : ");
	buf.append(sourceRelay.getContent());
      }
      buf.append("<br/>");
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
	Relay.Target target = (Relay.Target) relay;
	String targetSource = "-NO SOURCE SET-";

	if (target.getSource () != null) {
	  targetSource = target.getSource().toString();
	}

	buf.append("Target Relay Source  : " + encodeHTML(targetSource));
      }
    }

    if (relay instanceof HistoryServletFriendly) {
      buf.append (((HistoryServletFriendly)relay).toHTML(ADDED));
    }

    return buf.toString();
  }

  protected String showTargetAddresses (Relay.Source sourceRelay) {
    StringBuffer buf = new StringBuffer();

    if (!sourceRelay.getTargets().isEmpty()) {
      buf.append("Source Relay Target addresses : ");
      for (Iterator iter = sourceRelay.getTargets().iterator();
	   iter.hasNext(); ) {
	buf.append(
		   "<font size=small color=mediumblue>"+
		   "<li>");
	MessageAddress address = (MessageAddress) iter.next();
	if (address instanceof AttributeBasedAddress) {
	  AttributeBasedAddress aba = (AttributeBasedAddress) address;
	  buf.append ("AttributeBasedAddress : Broadcast to community=");
	  buf.append (aba.getCommunityName());
	  buf.append (" attribute type=");
	  buf.append (aba.getAttributeType());
	  buf.append (" value=");
	  buf.append (aba.getAttributeValue());
	}
	else {
	  buf.append (address);
	}
	// buf.append (encodeHTML(address.toString()));
	buf.append (
		    "</li>"+
		    "</font>\n");
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
	buf.append("Source Relay Query    : " + encodeHTML(sourceRelay.getContent().toString()) + "<br/>");
      
      if (sourceRelay.getContent() instanceof CommunityDescriptor) {
	CommunityDescriptor response = (CommunityDescriptor) sourceRelay.getContent();
	Community community = (Community)response.getCommunity();
	buf.append(getCommunityText("Source Relay Response : ", community));
      }
    }
    if (relay instanceof Relay.Target) {
      Relay.Target targetRelay = (Relay.Target)relay;
      //  buf.append("instanceof " + getClassName(targetRelay.getResponse()) + " ");
      if (targetRelay.getResponse() instanceof CommunityResponse) {
	CommunityResponse response = (CommunityResponse) targetRelay.getResponse();
	Community community = (Community)response.getContent();
	buf.append(getCommunityText("Target Relay Response : ", community));
      }
      else if (targetRelay.getResponse() instanceof CommunityDescriptor) {
	CommunityDescriptor response = (CommunityDescriptor) targetRelay.getResponse();
	Community community = (Community)response.getCommunity();
	buf.append(getCommunityText("Target Relay Response : ", community));
      }
      else {
	buf.append("Target Relay Response : " + encodeHTML(targetRelay.getResponse().toString()));
      }
    }

    if (relay instanceof HistoryServletFriendly) {
      buf.append (((HistoryServletFriendly)relay).toHTML(CHANGED));
    }

    return buf.toString();
  }

  protected String getCommunityText(String prefix, Community community) {
    StringBuffer buf = new StringBuffer();
    buf.append (prefix);
    buf.append(" Community <b>" + community.getName() + "</b> with members : "); 
    for (Iterator iter = community.getEntities().iterator(); iter.hasNext(); ) {
      buf.append(
		 "<font size=small color=mediumblue>"+
		 "<li>");
      buf.append(iter.next());
      buf.append(
		 "</li>"+
		 "</font>\n");
    }
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
    while (prefs.hasMoreElements()) {
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
    StringBuffer buf = new StringBuffer();

    if (planElement instanceof Allocation) {
      Allocation allocation = (Allocation) planElement;

      if (allocation.getAsset() != null) {
	buf.append ("Satisfy ");
	buf.append (allocation.getTask().getVerb());
	buf.append (" task with ");
	if (allocation.getAsset() instanceof Entity) {
	  buf.append (" <i>remote agent</i> ");
	}
	buf.append (getTypeAndItemInfo(allocation.getAsset()));
	buf.append ("<br/>");
	buf.append (getRoleSchedule(allocation.getAsset()));

	if (allocation.getEstimatedResult () != null) {
	  AspectValue [] values = allocation.getEstimatedResult().getAspectValueResults();
	  boolean success = allocation.getEstimatedResult().isSuccess();
	  String prefix = "<br/>Estimated Allocation Result - " + 
	    (success ? "<font color=\"green\">success</font>" : "<font color=\"red\">failure</font>");
	  buf.append (prefix + "<br/>");
	  buf.append (getAspectValues2 (values));
	}
      }
    }
    else if (planElement instanceof Expansion) {
      Expansion expansion = (Expansion) planElement;

      buf.append ("<table>");
      buf.append ("<tr><td>");
      buf.append ("Child tasks are :" );
      buf.append ("</td></tr>");

      int numShown = 0;
      for (Enumeration e = expansion.getWorkflow().getTasks(); e.hasMoreElements(); numShown++) {
	Task task = (Task) e.nextElement();
	buf.append ("<tr><td>");
	
	if (numShown >= maxChildTasks) {
	  buf.append ("... (more than ");
	  buf.append (maxChildTasks);
	  buf.append (")");
	  buf.append ("</td></tr>");
	  break;
	}
	else {
	  buf.append ("Task " + getURL(task.getUID(), TASK) + " " + task.getVerb());

	  if (task.getDirectObject() != null) {
	    buf.append (" direct object ");// + getURL(task.getDirectObject().getUID(), DIRECT_OBJECT) + " ");
	    buf.append (getTypeAndItemInfo(task.getDirectObject()));
	  }
	  
	  buf.append ("</td></tr>");
	}
      }

      buf.append ("</table>");
    }
    else if (planElement instanceof AssetTransfer) {
      AssetTransfer transfer = (AssetTransfer) planElement;
      buf.append("Transfer of <b>");
      buf.append(getTypeAndItemInfo(transfer.getAsset()));
      buf.append("</b> from <b>");
      buf.append(transfer.getAssignor());
      buf.append("</b> to <b>");
      buf.append(getTypeAndItemInfo(transfer.getAssignee()));
      buf.append("</b>");
    }
    else if (planElement instanceof Aggregation) {
      buf.append("Aggregation of ");
      buf.append(((Aggregation)planElement).getComposition().getParentTasks().size());
      buf.append(" parent tasks.");
    }

    if (planElement.getAnnotation() != null) {
      buf.append("<br>Annotation: " + planElement.getAnnotation());
    }

    return buf.toString();
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
  protected class HistoryWorker extends HttpServlet {
    public void doGet(
        HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
      doPost(request, response);
    }

    public void doPost(
        HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
      new HistoryFormatter(request, response);
    }
  }

  protected void addEvent (EventInfo newEvent) {
    events.add (newEvent);

    // if the events are sorted by uid, we want to sort them
    // by time first and then remove the oldest entry

    if (events.size() > maxEvents) {
      if (sortByUID) {
	// switch back to sorting by time, then uid
	sortByUID = false;

	SortedSet set = new TreeSet();
	set.addAll (events);
	setEvents(set);
      }

      if (logger.isInfoEnabled()) {
	logger.info ("removing " + events.first() + 
		     " since more than max " + events.size() + 
		     " elements.");
      }

      events.remove (events.first());
      didDropOldEntries = true;
    }
  }

  protected void setEvents (SortedSet events) {
    this.events = events;
  }

  protected class HistoryFormatter {
    public static final int FORMAT_DATA = 0;
    public static final int FORMAT_XML = 1;
    public static final int FORMAT_HTML = 2;

    private int format;

    public HistoryFormatter(
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

      String sortByUIDParam = request.getParameter("sortByUID");
      boolean oldValue = sortByUID;
      if (sortByUIDParam != null)
	sortByUID = sortByUIDParam.equals ("true");

      String showDetailsParam = request.getParameter("showDetails");
      if (showDetailsParam != null)
	showDetails = showDetailsParam.equals ("true");

      if (sortByUID != oldValue) {
	if (sortByUID) {
	  if (logger.isInfoEnabled()) {
	    logger.info ("sorting by uid, then time.");
	  }
	  sortByUIDThenTime ();
	}
	else {
	  SortedSet set = new TreeSet();
	  set.addAll (events);
	  //logger.warn ("events before " + events.size() + " vs now "+ set.size());
	  setEvents(set);
	}
      }
      
      return format;
    }

    public void execute(HttpServletResponse response) throws IOException, ServletException {
      if (format == FORMAT_HTML) {
        response.setContentType("text/html");

	PrintWriter out = response.getWriter();
	String sortLink = 
	  "<a href=\"/$" +
	  encAgentName+
	  getPath() +
	  "?";

	if (sortByUID) {
	  sortLink = sortLink + 
	    "sortByUID=false" +
	    "\">Sort by time</a>";
	}
	else {
	  sortLink = sortLink + 
	    "sortByUID=true" +
	    "\">Sort by uid</a>";
	}

	String detailsLink = 
	  "<a href=\"/$" +
	  encAgentName+
	  getPath() +
	  "?";

	if (showDetails) {
	  detailsLink = detailsLink + 
	    "showDetails=false" +
	    "\">hide object details</a>";
	}
	else {
	  detailsLink = detailsLink + 
	    "showDetails=true" +
	    "\">show object details</a>";
	}

        out.print(
            "<html><head><title>"+
            "History Servlet for " + agentId.getAddress()+
	    "</title></head>"+
	    "<body>" +
	    "<p><center><h1>Blackboard History</h1></center>"+
	    "<p>" +

	    "<center>" +
	    sortLink + 
	    "&nbsp;" + 
	    detailsLink +
	    "</center>"+

	    getHtmlForState() +
	    "</body>" +
            "</html>\n");
        out.flush();
      }
      // FIXME: Add support for FORMAT_XML and FORMAT_DATA
    }

    protected String getHtmlForState () {
      StringBuffer buf = new StringBuffer();

      if (didDropOldEntries) {
	buf.append("<center>Note : Too many changes have occurred; only newest ");
	buf.append(maxEvents);
	buf.append(" are shown.</center><br/>");
      }

      // tell user what the gray-white highlighting means
      buf.append("<center>Gray-white alternation indicates different ");

      if (sortByUID) {
	buf.append("blackboard objects.");
      }
      else {
	buf.append("plugin execute cycles.");
      }
      buf.append ("<br/>Time shown is scenario time.</center>");

      buf.append("<table border=1 align=center>");
      buf.append("<tr>");
      buf.append("<th>");
      buf.append("When");
      buf.append("</th>");
      buf.append("<th>");
      buf.append("Type");
      buf.append("</th>");
      buf.append("<th>");
      buf.append("Event");
      buf.append("</th>");
      if (showChangeReport) {
	buf.append("<th>");
	buf.append("Change Report");
	buf.append("</th>");
      }
      buf.append("<th>");
      buf.append("Comment");
      buf.append("</th>");

      if (showDetails) {
	buf.append("<th>");
	buf.append("Object Details");
	buf.append("</th>");
      }

      buf.append("</tr>");
      long lastTime = -1;
      String lastUID = "";
      boolean colorRowGrey = false;

      synchronized (events) {
	for (Iterator iter = events.iterator (); iter.hasNext(); ) {
	  EventInfo event = (EventInfo)iter.next();

	  if (sortByUID) {
	    if (!event.uid.equals (lastUID)) {
	      colorRowGrey = !colorRowGrey;
	      lastUID = event.uid;
	    }
	  } else {
	    if (event.timeStamp != lastTime && event.timeStamp != -1) {
	      colorRowGrey = !colorRowGrey;
	      lastTime = event.timeStamp;
	    }
	  }

	  buf.append(event.toString(colorRowGrey, showChangeReport, showDetails));
	  buf.append("\n");
	}
      }
      buf.append("</table>");
      return buf.toString();
    }
  }

  protected void sortByUIDThenTime () {
    SortedSet events2 = new TreeSet(new Comparator () {
	public int compare(Object o1, Object o2) {
	  EventInfo e1 = (EventInfo) o1;
	  EventInfo e2 = (EventInfo) o2;
	  int comp = e1.uid.compareTo(e2.uid);

	  if (comp == 0) {
	    if (e1.timeStamp < e2.timeStamp) {
	      comp = -1;
	    }
	    else if (e1.timeStamp > e2.timeStamp) {
	      comp = 1;
	    }
	  }

	  if (comp == 0) {
	    comp = e1.type-e2.type;
	  }

	  if (comp == 0) {
	    comp = e1.meaning.compareTo (e2.meaning);
	  }

	  if (comp == 0) {
	    if (logger.isInfoEnabled()) {
	      logger.info (e1 + "\nequals\n" + e2);
	    }
	  }
	  
	  return comp;
	}
      });

    events2.addAll (events);
    setEvents(events2);
  }

    /**
     * Encodes a string that may contain HTML syntax-significant
     * characters by replacing with a character entity.
     **/
    protected String encodeHTML(String s) {
      if (s == null)
	return "";

      boolean noBreakSpaces = true;
      //      boolean sawEquals = false;
      StringBuffer buf = null;  // In case we need to edit the string
      int ix = 0;               // Beginning of uncopied part of s
      for (int i = 0, n = s.length(); i < n; i++) {
	String replacement = null;
	switch (s.charAt(i)) {
	case '"': replacement = "&quot;"; break;
	  //case '[': replacement = "["; break;
	case '<': replacement = "&lt;"; break;
	case '>': replacement = "&gt;<br/>"; break;
	case '&': replacement = "&amp;"; break;
	  //	case '=': sawEquals = true; break;
	case ' ': 
	  //	  if (sawEquals && (i+1 < n) && s.charAt(i+1) != '>') {
	  //	    replacement = "<br/>";
	  //	  }
	  //else if (noBreakSpaces) {
	  if (noBreakSpaces) {
	    replacement = "&nbsp;"; 
	  }
	  //sawEquals=false;
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

  private static class EventInfo implements Comparable {
    public String uid;
    public String event;
    public long timeStamp;
    public String meaning;
    public Set changeReports = null;
    public String toStringResult="";
    public int type;

    public EventInfo (int type, String uid, String event, long timeStamp, String meaning) {
      this.type = type;
      this.uid = uid;
      this.event = event;
      this.timeStamp = timeStamp;
      this.meaning = meaning;
    }

    public EventInfo (int type, String uid, String event, long timeStamp, String meaning, Set changeReports) {
      this (type, uid, event, timeStamp, meaning);
      this.changeReports = changeReports;
    }

    public EventInfo (int type, String uid, String event, long timeStamp, String meaning, String toStringResult) {
      this (type, uid, event, timeStamp, meaning);
      this.toStringResult = toStringResult;
    }

    public EventInfo (int type, String uid, String event, long timeStamp, 
		      String meaning, Set changeReports, String toStringResult) {
      this (type, uid, event, timeStamp, meaning, changeReports);
      this.toStringResult = toStringResult;
    }

    public int compareTo (Object other) {
      EventInfo otherEvent = (EventInfo) other;
      
      if (timeStamp < otherEvent.timeStamp)
	return -1;
      if (timeStamp > otherEvent.timeStamp)
	return 1;

      int uidComp = uid.compareTo (otherEvent.uid);
      if (uidComp != 0)
	return uidComp;

      int val = type-otherEvent.type;

      if (val != 0)
	return val;
      
      val = meaning.compareTo (otherEvent.meaning);
      
      return val;
    }

    public String toString () {
      return toString (true, false, false);
    }

    public String toString (boolean odd, boolean showChangeReport, boolean showDetails) {
      String color = odd ? "#FFFFFF" : "#c0c0c0";
      StringBuffer buf = new StringBuffer();
      buf.append("<tr BGCOLOR=" + color + ">");
      buf.append("<td>" + format.format(new Date(timeStamp)) + "</td>");
      String typeString = "Added";
      if (type == CHANGED)
	typeString = "Changed";
      else if (type == REMOVED)
	typeString = "Removed";
	
      buf.append("<td>" + typeString + "</td>");
      buf.append("<td>" + event + "</td>");

      if (showChangeReport) {
	buf.append ("<td>");

	if (changeReports != null) {
	  buf.append(htmlForChangeReport());
	}

	buf.append("</td>");
      }

      if (meaning != null) {
	buf.append ("<td>" + meaning + "</td>");
      }

      if (showDetails) {
	buf.append("<td>" + toStringResult + "</td>");
      }

      buf.append("</tr>");

      return buf.toString();
    }

    public String htmlForChangeReport () {
      StringBuffer buf = new StringBuffer();
      buf.append ("<table>");

      for (Iterator iter = changeReports.iterator(); iter.hasNext(); ) {
	Object obj = iter.next();
	buf.append ("<tr><td>" + obj + "</td></tr>");
      }
      
      buf.append ("</table>");

      return buf.toString();
    }
  }
}

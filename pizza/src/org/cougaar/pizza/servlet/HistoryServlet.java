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
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.util.Arguments;
import org.cougaar.util.UnaryPredicate;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Generic debugging servlet/plugin that displays all Adds/Changes/Removes
 * on Blackboard objects, accessed at "/history".
 * <p/>
 * Specifically tracks changes on Relays, Tasks, PlanElements, Assets,
 * UniqueObjects, and implementations of {@link HistoryServletFriendly}.
 * For every event, attempts to explain the event.
 * For instance, when a Relay is first published, it shows which Agent
 * the Relay is being sent to.
 * <p/>
 * The Servlet lets the user sort events by time, then by uid, or by uid
 * only.  Sorting by uid shows the complete lifecycle of a Blackboard
 * object; especially useful for transient objects that live on the
 * Blackboard only a short time.  When sorting by time, each group of
 * changes that happens in the same execute cycle is drawn with
 * the same background color.  When sorting by uid, each distinct
 * object is drawn with the same background color.
 * <p/>
 * The servlet has a "show details" link which will show the toString for
 * the blackboard object at that time. This is a spot where developers
 * can tune how this servlet displays their objects.
 * <p/>
 * The "Meaning" column uses the HistoryServletFriendly's toHTML() method
 * to fill in content when available.
 * <p/>
 * Also shows which plugin initially published an object to the Blackboard
 * if that information is available (for Claimables).
 * <p/>
 * <p/>
 * Try it in any Cougaar Application! Simply add this as a Plugin
 * in any Agent!
 * <p/>
 * Has a default limit of 1000 events, but this can be set by the
 * MAX_EVENTS_REMEMBERED component argument to the servlet.
 * E.g. :
 * &lt;argument&gt;MAX_EVENTS_REMEMBERED=5000&lt;/argument&gt;
 * <p> Another limit: 5 RoleSchedule elements displayed by default,
 * set with MAX_ROLE_SCHEDULE_ELEMENTS argument.
 * And a default of 5 child Tasks shown per Expansion, changed with the
 * MAX_CHILD_TASKS argument.
 * <p/>
 * Note that this Servlet is actually a Plugin, so that it can subscribe
 * to all the blackboard changes, and keeps a SortedSet of these Events,
 * ready for display. It then provides an inner Servlet to the ServletService,
 * so a user can view the pre-collected Events Set.
 * <p/>
 * Note that this Servlet has heavy Planning dependencies. It has minor
 * Community dependencies, to allow printing details in the Meaning column.
 * By commenting out those items, this dependency could be removed.
 */
public class HistoryServlet extends ComponentPlugin {
  // Some defaults
  protected final int INITIAL_MAX_ENTRIES = 1000; // Default max # events to track
  protected final int INITIAL_MAX_ROLE_SCHEDULE_ELEMENTS = 5;
  protected final int INITIAL_MAX_CHILD_TASKS = 5;

  // Actual value of parametrized preferences
  private int maxEvents;
  private int maxRoleScheduleElements;
  private int maxChildTasks;

  /**
   * initialize args to the empty instance
   */
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

  protected static SimpleDateFormat format = new SimpleDateFormat("MM-dd hh:mm:ss,SSS");
  protected String encAgentName;

  /**
   * The actual Blackboard history we collect
   */
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
  public static final int ADDED = 0;
  public static final int CHANGED = 1;
  public static final int REMOVED = 2;

  private UnaryPredicate relayPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof Relay);
    }
  };

  private UnaryPredicate taskPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof Task);
    }
  };

  private UnaryPredicate allocationPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof PlanElement);
    }
  };

  private UnaryPredicate assetPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof Asset);
    }
  };

  // Subscribe to anything not in the above subscriptions
  private UnaryPredicate uniqueObjectPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (!(o instanceof Relay) &&
          !(o instanceof Task) &&
          !(o instanceof PlanElement) &&
          !(o instanceof Asset) &&
          (o instanceof UniqueObject));
    }
  };

  /**
   * Only called if a plugin has parameters.
   * We over-ride this to use the Arguments utility, since all our
   * arguments are NAME=VALUE format.
   */
  public void setParameter(Object o) {
    args = new Arguments(o);
  }

  /**
   * Get the ServletService via reflection
   */
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
      logger.info("max events " + args.getInt("MAX_EVENTS_REMEMBERED", 55));
      logger.info("args is " + args);
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

  /*
   * Create subscriptions to all the BBoard changes, and register the servlet.
   */
  protected void setupSubscriptions() {
    relaysSubscription = (IncrementalSubscription) blackboard.subscribe(relayPredicate);
    tasksSubscription = (IncrementalSubscription) blackboard.subscribe(taskPredicate);
    planElementsSubscription = (IncrementalSubscription) blackboard.subscribe(allocationPredicate);
    assetsSubscription = (IncrementalSubscription) blackboard.subscribe(assetPredicate);
    uniqueObjectsSubscription = (IncrementalSubscription) blackboard.subscribe(uniqueObjectPredicate);

    // register with servlet service
    try {
      servletService.register(getPath(), createServlet());
    } catch (Exception e) {
      if (logger.isWarnEnabled())
        logger.warn("could not register servlet?", e);
    }

  }

  /**
   * Load this servlet at "/history"
   */
  protected String getPath() {
    return "/history";
  }

  /**
   * Get a new {@link HistoryWorker} to be the Servlet
   */
  protected Servlet createServlet() {
    return new HistoryWorker();
  }

  /**
   * Whenever a BBoard item changes, it adds that event to the list of Events
   * (trimming the set if we reach the MAX size). Then when a user invokes
   * the servlet, the set of events is ready for quick display.
   */
  public void execute() {
    // Grab the time with which to mark the events. Note that this time
    // is not when the event appeneded therefore, but when this plugin saw it.
    // If the agent were busy (persistence?), this could be much later.
    long now = currentTimeMillis(); // this is scenario time.

    try {
      // Look at our subscriptions for changes, adding to our 
      // set of known events if necessary. This actually builds of the EventInfo
      // objects (see definition toward the bottom) which contain some HTML
      // strings -- making the later servlet viewing quicker, but taking
      // up more memory

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
        logger.warn("Got exception adding to events list: ", e);
    }

    // Print the current set of Events...
    if (logger.isDebugEnabled()) {
      StringBuffer buf = new StringBuffer();
      synchronized (events) {
        for (Iterator iter = events.iterator(); iter.hasNext();) {
          buf.append(iter.next() + "\n");
        }
      }

      logger.debug("Current events: \n" + buf);
    }
  }

  /////////////////
  // Below are all the helper methods to collect the Events....

  public String encodeAgentName(String name) {
    try {
      return URLEncoder.encode(name, "UTF-8");
    } catch (java.io.UnsupportedEncodingException e) {
      // should never happen
      throw new RuntimeException("Unable to encode to UTF-8?");
    }
  }

  protected String encode(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to encode URL (" + s + ")");
    }
  }

  /**
   * Generate a link to the PlanView (/tasks) servlet
   * for full details on the objet.
   */
  protected String getURL(UID uid, int which) {
    int mode = 0;//PlanViewServlet.MODE_FRAME;
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
    buf.append("?" +
        "mode" + //PlanViewServlet.MODE+
        "=" +
        mode +
        "&" +
        "uid" + //PlanViewServlet.ITEM_UID+
        "=");
    buf.append(encode(uid.toString()));
    //        buf.append("\" target=\"itemFrame\">");
    buf.append("\" target=\"_blank\">");
    //    buf.append("\"");
    buf.append(uid);
    buf.append("</a>");

    return buf.toString();
  }

  // Keep a running counter of events seen
  // Can later be used to number the rows for readability.
  private int eventNum = 0;

  /**
   * Get the next number for a new Event
   */
  protected int nextEventNum() {
    return eventNum++;
  }

  /**
   * Check the Tasks subscription, adding any new events to the list
   */
  protected void checkTasks(long now) {
    // If there were any Task events
    if (tasksSubscription.hasChanged()) {
      // First look at Added Tasks
      Collection added = tasksSubscription.getAddedCollection();
      // For each
      for (Iterator iter = added.iterator(); iter.hasNext();) {
        Task task = (Task) iter.next();
        // Basic description is the UID, verb, publisher
        String event =
            "Task " + getURL(task.getUID(), TASK) +
            " - " + task.getVerb() +
            "<br/>was published by " + ((Claimable) task).getClaim();
        // Add the event to the set
        addEvent(new EventInfo(ADDED,
            nextEventNum(),
            task.getUID().toString(),
            event,
            now,
            getAddedTaskComment(task),
            encodeHTML(task.toString())));
      }

      // Now the Changed tasks
      Collection changed = tasksSubscription.getChangedCollection();
      for (Iterator iter = changed.iterator(); iter.hasNext();) {
        Task task = (Task) iter.next();
        addEvent(new EventInfo(CHANGED,
            nextEventNum(),
            task.getUID().toString(),
            "Task " + getURL(task.getUID(), TASK),
            now,
            "",
            tasksSubscription.getChangeReports(task),
            encodeHTML(task.toString())));
      }

      // Now the removed tasks
      Collection removed = tasksSubscription.getRemovedCollection();
      for (Iterator iter = removed.iterator(); iter.hasNext();) {
        Task task = (Task) iter.next();
        addEvent(new EventInfo(REMOVED,
            nextEventNum(),
            task.getUID().toString(),
            "Task " + getURL(task.getUID(), TASK),
            now,
            ""));
      }
    }
  }

  /**
   * description of an added Task shows the Verb, DirectObject, and Preferences.
   */
  protected String getAddedTaskComment(Task task) {
    StringBuffer buf = new StringBuffer();

    buf.append(task.getVerb());
    buf.append(" Task, ");
    if (task.getDirectObject() != null) {
      buf.append(" with Direct Object ");
      buf.append(getTypeAndItemInfo(task.getDirectObject()));
      buf.append(".");
    }
    buf.append(getTaskPreferences(task));

    return buf.toString();
  }

  /**
   * Add any PlanElement events to the Set
   */
  protected void checkPlanElements(long now) {
    if (planElementsSubscription.hasChanged()) {
      Collection added = planElementsSubscription.getAddedCollection();
      for (Iterator iter = added.iterator(); iter.hasNext();) {
        PlanElement planElement = (PlanElement) iter.next();
        String event =
            "" + getClassName(planElement) +
            " " + getURL(planElement.getUID(), PLAN_ELEMENT) +
            "<br/>of Task " + getURL(planElement.getTask().getUID(), TASK) +
            "<br/>was published by " + planElement.getClaimable().getClaim();

        addEvent(new EventInfo(ADDED,
            nextEventNum(),
            planElement.getUID().toString(),
            event,
            now,
            getAddedPEComment(planElement),
            encodeHTML(planElement.toString())));
      }

      Collection changed = planElementsSubscription.getChangedCollection();
      for (Iterator iter = changed.iterator(); iter.hasNext();) {
        PlanElement planElement = (PlanElement) iter.next();
        String event =
            getClassName(planElement) + " " +
            getURL(planElement.getUID(), PLAN_ELEMENT);

        addEvent(new EventInfo(CHANGED,
            nextEventNum(),
            planElement.getUID().toString(),
            event,
            now,
            getChangedPEComment(planElement),
            planElementsSubscription.getChangeReports(planElement),
            encodeHTML(planElement.toString())));
      }

      Collection removed = planElementsSubscription.getRemovedCollection();
      for (Iterator iter = removed.iterator(); iter.hasNext();) {
        PlanElement planElement = (PlanElement) iter.next();
        String event =
            getClassName(planElement) + " " +
            getURL(planElement.getUID(), PLAN_ELEMENT);

        addEvent(new EventInfo(REMOVED,
            nextEventNum(),
            planElement.getUID().toString(),
            event,
            now,
            // Alternative to empty comment would be getAddedPEComment(planElement)
            "&nbsp;")); 
      }
    }
  }

  /**
   * Add any Relay events
   */
  protected void checkRelays(long now) {
    if (relaysSubscription.hasChanged()) {
      Collection added = relaysSubscription.getAddedCollection();
      for (Iterator iter = added.iterator(); iter.hasNext();) {
        Relay relay = (Relay) iter.next();
        addEvent(new EventInfo(ADDED,
            nextEventNum(),
            relay.getUID().toString(),
            "Relay " + getURL(relay.getUID(), UNIQUE_OBJECT),
            now,
            getAddedRelayComment(relay),
            encodeHTML(relay.toString())));
      }

      Collection changed = relaysSubscription.getChangedCollection();
      for (Iterator iter = changed.iterator(); iter.hasNext();) {
        Relay relay = (Relay) iter.next();
        addEvent(new EventInfo(CHANGED,
            nextEventNum(),
            relay.getUID().toString(),
            "Relay " + getURL(relay.getUID(), UNIQUE_OBJECT),
            now,
            getChangedRelayComment(relay),
            relaysSubscription.getChangeReports(relay),
            encodeHTML(relay.toString())));
      }

      Collection removed = relaysSubscription.getRemovedCollection();
      for (Iterator iter = removed.iterator(); iter.hasNext();) {
        Relay relay = (Relay) iter.next();
        addEvent(new EventInfo(REMOVED,
            nextEventNum(),
            relay.getUID().toString(),
            "Relay " + getURL(relay.getUID(), UNIQUE_OBJECT),
            now,
            "Relay removed from blackboard."));
      }
    }
  }

  /**
   * Add any Asset events to the Set
   */
  protected void checkAssets(long now) {
    if (assetsSubscription.hasChanged()) {
      Collection added = assetsSubscription.getAddedCollection();
      for (Iterator iter = added.iterator(); iter.hasNext();) {
        Asset asset = (Asset) iter.next();
        addEvent(new EventInfo(ADDED,
            nextEventNum(),
            asset.getUID().toString(),
            "Asset " + getURL(asset.getUID(), ASSET),
            now,
            getAddedAssetComment(asset),
            encodeHTML(asset.toString())));
      }

      Collection changed = assetsSubscription.getChangedCollection();
      for (Iterator iter = changed.iterator(); iter.hasNext();) {
        Asset asset = (Asset) iter.next();
        addEvent(new EventInfo(CHANGED,
            nextEventNum(),
            asset.getUID().toString(),
            "Asset " + getURL(asset.getUID(), ASSET),
            now,
            getChangedAssetComment(asset),
            encodeHTML(asset.toString())));
      }

      Collection removed = assetsSubscription.getRemovedCollection();
      for (Iterator iter = removed.iterator(); iter.hasNext();) {
        Asset asset = (Asset) iter.next();
        addEvent(new EventInfo(REMOVED,
            nextEventNum(),
            asset.getUID().toString(),
            "Asset " + getURL(asset.getUID(), ASSET),
            now,
            ""));
      }
    }
  }

  /**
   * Get the ChangedAssetComment
   */
  protected String getAddedAssetComment(Asset asset) {
    return getChangedAssetComment(asset);
  }

  /**
   * Show the asset class, typeID, ItemID, if an Entity then any Roles, Relationships, RoleSchedule, and the HistoryServletFriendly content
   */
  protected String getChangedAssetComment(Asset asset) {
    StringBuffer buf = new StringBuffer();

    buf.append(getClassName(asset));
    buf.append(": <b>");
    buf.append(getTypeAndItemInfo(asset));
    buf.append("</b><br/>");

    if (asset instanceof Entity) {
      Entity entity = (Entity) asset;

      if (!entity.getEntityPG().getRoles().isEmpty()) {
        buf.append(" with Roles : ");
      }

      for (Iterator iter = entity.getEntityPG().getRoles().iterator(); iter.hasNext();) {
        buf.append("<font size=small color=mediumblue>" +
            "<li>");
        Object obj = iter.next();
        buf.append(getClassName(obj) + " - " + obj);
        buf.append("</li>" +
            "</font>\n");
      } // end of loop over roles
    } // end of block for Entity

    if (asset instanceof HasRelationships) {
      HasRelationships entity = (HasRelationships) asset;

      Collection relationships =
          entity.getRelationshipSchedule().getMatchingRelationships(new UnaryPredicate() {
            public boolean execute(Object o) {
              return true;
            }
          });

      if (!relationships.isEmpty()) {
        buf.append("<br/>with Relationships : ");
      }

      for (Iterator iter = relationships.iterator(); iter.hasNext();) {
        buf.append("<font size=small color=mediumblue>" +
            "<li>");

        Object obj = iter.next();
        // buf.append (getClassName(obj) + " - " + obj);
        if (obj instanceof Relationship) {
          Relationship relation = (Relationship) obj;
          buf.append(relation.getRoleA().toString());
          buf.append("=");

          if (relation.getA() instanceof Asset) {
            buf.append(getTypeAndItemInfo((Asset) relation.getA()));
          }

          buf.append("<br/>" + relation.getRoleB().toString());
          buf.append("=");

          if (relation.getB() instanceof Asset) {
            buf.append(getTypeAndItemInfo((Asset) relation.getB()));
          }
        }

        buf.append("</li>" +
            "</font>\n");
      } // end of loop over relationships

    } // end of HasRelationships

    buf.append(getRoleSchedule(asset));

    if (asset instanceof HistoryServletFriendly) {
      buf.append(((HistoryServletFriendly) asset).toHTML(CHANGED));
    }

    return buf.toString();
  } // end of getChangedAssetComment

  /**
   * Print the RoleSchedule of the Asset if any, up to the
   * maxRoleScheduleElements
   */
  protected String getRoleSchedule(Asset asset) {
    StringBuffer buf = new StringBuffer();

    if (asset.getRoleSchedule().getRoleScheduleElements().hasMoreElements()) {
      buf.append("<br/>which has Role Schedule: ");
    }

    int numShown = 0;
    for (Enumeration enum = asset.getRoleSchedule().getRoleScheduleElements();
         enum.hasMoreElements(); numShown++) {
      buf.append("<font size=small color=mediumblue>" +
          "<li>");
      Object elem = enum.nextElement();
      if (numShown >= maxRoleScheduleElements) {
        buf.append("... (more than ");
        buf.append(maxRoleScheduleElements);
        buf.append(")");
        buf.append("</li>" +
            "</font>\n");
        break;
      } else {
        if (elem instanceof Allocation) {
          buf.append("Allocation to " + getTypeAndItemInfo(((Allocation) elem).getAsset()));
        } else if (elem instanceof PlanElement) {
          buf.append(getAddedPEComment((PlanElement) elem));
        } else {
          buf.append(enum.nextElement());
        }
      }

      buf.append("</li>" +
          "</font>\n");
    }

    return buf.toString();
  }

  /**
   * Add any added/changed/removed UniqueObjects
   */
  protected void checkUniqueObjects(long now) {
    if (uniqueObjectsSubscription.hasChanged()) {
      Collection added = uniqueObjectsSubscription.getAddedCollection();
      for (Iterator iter = added.iterator(); iter.hasNext();) {
        UniqueObject uniqueObject = (UniqueObject) iter.next();
        addEvent(new EventInfo(ADDED,
            nextEventNum(),
            uniqueObject.getUID().toString(),
            "UniqueObject " + getURL(uniqueObject.getUID(), UNIQUE_OBJECT),
            now,
            getAddedUniqueObjectComment(uniqueObject),
            encodeHTML(uniqueObject.toString())));
      }

      Collection changed = uniqueObjectsSubscription.getChangedCollection();
      for (Iterator iter = changed.iterator(); iter.hasNext();) {
        UniqueObject uniqueObject = (UniqueObject) iter.next();
        addEvent(new EventInfo(CHANGED,
            nextEventNum(),
            uniqueObject.getUID().toString(),
            "UniqueObject " + getURL(uniqueObject.getUID(), UNIQUE_OBJECT) + " changed.",
            now,
            getChangedUniqueObjectComment(uniqueObject),
            encodeHTML(uniqueObject.toString())));
      }

      Collection removed = uniqueObjectsSubscription.getRemovedCollection();
      for (Iterator iter = removed.iterator(); iter.hasNext();) {
        UniqueObject uniqueObject = (UniqueObject) iter.next();
        addEvent(new EventInfo(REMOVED,
            nextEventNum(),
            uniqueObject.getUID().toString(),
            "UniqueObject " + getURL(uniqueObject.getUID(), UNIQUE_OBJECT) + " was removed.",
            now,
            getRemovedUniqueObjectComment(uniqueObject)));
      }
    }
  }

  /**
   * For an Added unique object, print it's name, and any
   * HistoryServletFriendly content
   */
  protected String getAddedUniqueObjectComment(UniqueObject unique) {
    StringBuffer buf = new StringBuffer();

    buf.append("A ");
    buf.append(getClassName(unique));
    buf.append(" object.<br/>");

    if (unique instanceof HistoryServletFriendly) {
      buf.append(((HistoryServletFriendly) unique).toHTML(ADDED));
    }

    return buf.toString();
  }

  /**
   * Changed/Removed unique objects use the same comment as for Add
   */
  protected String getChangedUniqueObjectComment(UniqueObject unique) {
    return getAddedUniqueObjectComment(unique);
  }

  /**
   * Changed/Removed unique objects use the same comment as for Add
   */
  protected String getRemovedUniqueObjectComment(UniqueObject unique) {
    return getAddedUniqueObjectComment(unique);
  }

  /**
   * Get the non-Package name of the Class
   */
  protected String getClassName(Object obj) {
    String classname = obj.getClass().getName();
    int index = classname.lastIndexOf(".");
    classname = classname.substring(index + 1, classname.length());
    return classname;
  }

  /**
   * For a Source: show the target addresses, any community content or request, and the
   * relay's Content.
   * <p/>
   * For a Target, show any community info, the relay Source, and any HistoryServletFriendly content.
   */
  protected String getAddedRelayComment(Relay relay) {
    StringBuffer buf = new StringBuffer();

    if (relay instanceof Relay.Source) {
      buf.append("Sent new ");
      Relay.Source sourceRelay = (Relay.Source) relay;
      buf.append(showTargetAddresses(sourceRelay));

      // Special case community relays
      if (sourceRelay.getContent() instanceof CommunityDescriptor) {
        CommunityDescriptor response = (CommunityDescriptor) sourceRelay.getContent();
        Community community = null;
        if (response != null)
          community = (Community) response.getCommunity();
        if (buf.length() != 0)
          buf.append("<br/>");
        buf.append(getCommunityText("Relay Source sending Description: ", community));
      } else if (sourceRelay.getContent() instanceof Request) {
        if (buf.length() != 0)
          buf.append("<br/>");
        buf.append("Community Request: ");
        Request request = (Request) sourceRelay.getContent();
        buf.append(request.getRequestTypeAsString(request.getRequestType()));
        buf.append("<br/>Source: ");
        buf.append(request.getSource());
        buf.append("<br/>Entity: ");
        buf.append(request.getEntity());
      } else {
        if (buf.length() != 0)
          buf.append("<br/>");
        buf.append("Content: ");
        buf.append(encodeHTML(sourceRelay.getContent().toString()));
      }
      buf.append("<br/>");
    }

    if (relay instanceof Relay.Target) {
      Relay.Target targetRelay = (Relay.Target) relay;
      //    buf.append("instanceof " + getClassName(targetRelay.getResponse()) + " ");
      if (targetRelay.getResponse() instanceof CommunityResponse) {
        CommunityResponse response = (CommunityResponse) targetRelay.getResponse();
        Community community = null;
        if (response != null)
          community = (Community) response.getContent();

        buf.append("Entity " + targetRelay.getSource() + " registers with Community " + (community != null ? community.getName() : "[null]"));
      } else {
        // Other than community relays, no good general way to print
        // the relay response
        String targetSource = "-NO SOURCE SET-";

        if (targetRelay.getSource() != null) {
          targetSource = targetRelay.getSource().toString();
        }

        buf.append("Received Relay Target from Source  : " + encodeHTML(targetSource));
      }
    }

    if (relay instanceof HistoryServletFriendly) {
      buf.append(((HistoryServletFriendly) relay).toHTML(ADDED));
    }

    return buf.toString();
  }

  /**
   * Show the target(s) of a relay, including ABAs
   */
  protected String showTargetAddresses(Relay.Source sourceRelay) {
    StringBuffer buf = new StringBuffer();

    if (!sourceRelay.getTargets().isEmpty()) {
      buf.append("Relay Source with Targets: ");
      for (Iterator iter = sourceRelay.getTargets().iterator();
           iter.hasNext();) {
        buf.append("<font size=small color=mediumblue>" +
            "<li>");
        MessageAddress address = (MessageAddress) iter.next();
        if (address instanceof AttributeBasedAddress) {
          AttributeBasedAddress aba = (AttributeBasedAddress) address;
          buf.append("AttributeBasedAddress: Broadcast to Community=");
          buf.append(aba.getCommunityName());
          buf.append(" attribute type=");
          buf.append(aba.getAttributeType());
          buf.append(" value=");
          buf.append(aba.getAttributeValue());
        } else {
          buf.append(address);
        }
        // buf.append (encodeHTML(address.toString()));
        buf.append("</li>" +
            "</font>\n");
      }
    }

    return buf.toString();
  }

  /**
   * For a Source, show any community specific information, the Content.
   * <p/>
   * For a Target, show any community specific information, and the response, and
   * any HistoryServletFriendly content.
   */
  protected String getChangedRelayComment(Relay relay) {
    StringBuffer buf = new StringBuffer();
    if (relay instanceof Relay.Source) {
      buf.append("Response Returned</br>");

      Relay.Source sourceRelay = (Relay.Source) relay;
      if (!(sourceRelay.getContent() instanceof Relay))
        buf.append("Relay Source's Query    : " + encodeHTML(sourceRelay.getContent().toString()) + "<br/>");

      if (sourceRelay.getContent() instanceof CommunityDescriptor) {
        CommunityDescriptor response = (CommunityDescriptor) sourceRelay.getContent();
        Community community = null;
        if (response != null)
          community = (Community) response.getCommunity();
        buf.append(getCommunityText("Relay Source received Response: ", community));
      }
    }

    if (relay instanceof Relay.Target) {
      Relay.Target targetRelay = (Relay.Target) relay;
      //  buf.append("instanceof " + getClassName(targetRelay.getResponse()) + " ");
      if (targetRelay.getResponse() instanceof CommunityResponse) {
        CommunityResponse response = (CommunityResponse) targetRelay.getResponse();
        Community community = null;
        if (response != null)
          community = (Community) response.getContent();
        buf.append(getCommunityText("Relay Target sent Response: ", community));
      } else if (targetRelay.getResponse() instanceof CommunityDescriptor) {
        CommunityDescriptor response = (CommunityDescriptor) targetRelay.getResponse();
        Community community = null;
        if (response != null)
          community = (Community) response.getCommunity();
        buf.append(getCommunityText("Relay Target sent Response: ", community));
      } else {
        buf.append("Relay Target sent Response: " + encodeHTML(targetRelay.getResponse().toString()));
      }
    }

    if (relay instanceof HistoryServletFriendly) {
      buf.append(((HistoryServletFriendly) relay).toHTML(CHANGED));
    }

    return buf.toString();
  }

  /**
   * Describe this Community by name, entities
   */
  protected String getCommunityText(String prefix, Community community) {
    if (community == null)
      return "[empty response]";

    StringBuffer buf = new StringBuffer();
    buf.append(prefix);
    buf.append(" Community <b>" + community.getName() + "</b> with members : ");
    for (Iterator iter = community.getEntities().iterator(); iter.hasNext();) {
      buf.append("<font size=small color=mediumblue>" +
          "<li>");
      buf.append(iter.next());
      buf.append("</li>" +
          "</font>\n");
    }
    return buf.toString();
  }

  /**
   * Show the results on the PE
   */
  protected String getChangedPEComment(PlanElement planElement) {
    StringBuffer buf = new StringBuffer();

    if (planElement.getEstimatedResult() != null) {
      buf.append(getAllocResult("Estimated", planElement.getEstimatedResult()));
    }

    if (planElement.getReportedResult() != null) {
      buf.append(getAllocResult("Reported", planElement.getReportedResult()));
      buf.append("<br/>");
      if (!planElement.getReportedResult().isSuccess()) {
        if (planElement.getTask().getPreferences().hasMoreElements()) {
          buf.append("<br>Failed because reported Aspect Values did not satisfy Task Preferences:");
          buf.append(getTaskPreferences(planElement.getTask()));
        } else {
          buf.append("<br>Failed because one or more Child Tasks failed.");
        }
      }
    }

    return buf.toString();
  }

  /**
   * Get an HTML table of these Aspect Values (id'd by the prefix)
   */
  protected String getAspectValues(AspectValue[] values, String prefix) {
    StringBuffer buf = new StringBuffer();
    buf.append("<table>");
    buf.append("<tr><td>");
    buf.append(prefix + " Aspect Values:");
    buf.append("</td></tr>");
    for (int i = 0; i < values.length; i++) {
      buf.append("<tr><td>");
      buf.append("Type ");
      buf.append(AspectValue.aspectTypeToString(values[i].getType()));
      buf.append(" - Value ");
      buf.append(values[i].getValue());
      buf.append("</td></tr>");
    }
    buf.append("</table>");
    return buf.toString();
  }

  /**
   * Get String representation of these Aspect Values
   */
  protected String getAspectValues2(AspectValue[] values) {
    StringBuffer buf = new StringBuffer();
    // for all (type, result) pairs
    for (int i = 0; i < values.length; i++) {
      AspectValue avi = values[i];
      buf.append(getAspectValue(avi));
    }
    return buf.toString();
  }

  /**
   * Get a String representation of this AspectValue
   */
  protected String getAspectValue(AspectValue avi) {
    StringBuffer buf = new StringBuffer();
    buf.append("<font size=small color=mediumblue>" +
        "<li>");
    // show type
    buf.append(AspectValue.aspectTypeToString(avi.getType()));
    buf.append("= ");
    // show value
    if (avi instanceof TimeAspectValue) {
      // print the date in our format
      long time = ((TimeAspectValue) avi).timeValue();
      buf.append(getTimeString(time));
    } else {
      buf.append(avi.getValue());
    }
    buf.append("</li>" +
        "</font>\n");
    return buf.toString();
  }

  /**
   * Formats long millis time to Date Format String.
   */
  protected String getTimeString(long time) {
    synchronized (myDateFormat) {
      myDateInstance.setTime(time);
      return
          myDateFormat.format(myDateInstance,
              new StringBuffer(20),
              myFieldPos).toString();
    }
  }


  /**
   * Get a String HTML table of the Task's preferences
   */
  protected String getTaskPreferences(Task task) {
    Enumeration prefs = task.getPreferences();
    StringBuffer buf = new StringBuffer();
    boolean hasPrefs = false;
    buf.append("<table>");
    buf.append("<tr><td>");
    buf.append("Preferences: ");
    buf.append("</td></tr>");
    while (prefs.hasMoreElements()) {
      Preference pref = (Preference) prefs.nextElement();
      hasPrefs = true;
      AspectValue prefav =
          pref.getScoringFunction().getBest().getAspectValue();
      buf.append("<tr><td>");
      buf.append(getAspectValue(prefav));
      /*
      buf.append ("Type ");
      buf.append (AspectValue.aspectTypeToString(prefav.getType()));
      buf.append (" - Value");
      buf.append (prefav.getValue());
      */
      buf.append("</td></tr>");
    }
    buf.append("</table>");

    if (!hasPrefs)
      return "";
    else
      return buf.toString();
  }

  /**
   * Return HTML of the success or failure (with Aspect Values) of the given AllocationResult
   */
  private String getAllocResult(String type, AllocationResult ar) {
    if (ar == null)
      return "";

    StringBuffer buf = new StringBuffer();
    AspectValue[] values = ar.getAspectValueResults();
    boolean success = ar.isSuccess();
    String prefix = "<br/>" + type + " Allocation Result - " +
      (success ? "<font color=\"green\">Success</font>" : "<font color=\"red\">Failure</font>");
    buf.append(prefix + "<br/>");
    buf.append(getAspectValues2(values));

    return buf.toString();
  }

  /**
   * If it's an Allocation, show the Task Verb and the allocated Asset's Type
   * and ItemID, RoleSchedule.
   * For an Expansion, show the child tasks (up to the MAX).
   * For an AssetTransfer, show the moving Asset and destination.
   * For an Aggregation, show just the count of parent tasks.
   * For a Disposition, show whether it succeed or failed.
   * Also show any Annotation, Estimated Result, and Reported Result.
   */
  protected String getAddedPEComment(PlanElement planElement) {
    StringBuffer buf = new StringBuffer();

    if (planElement instanceof Allocation) {
      Allocation allocation = (Allocation) planElement;

      if (allocation.getAsset() != null) {
        buf.append("Satisfy ");
        buf.append(allocation.getTask().getVerb());
        buf.append(" Task with ");
        if (allocation.getAsset() instanceof Entity) {
          buf.append(" <i>remote Agent</i> ");
        }
        buf.append(getTypeAndItemInfo(allocation.getAsset()));
        buf.append("<br/>");
        buf.append(getRoleSchedule(allocation.getAsset()));
      }
    } else if (planElement instanceof Expansion) {
      Expansion expansion = (Expansion) planElement;

      buf.append("<table>");
      buf.append("<tr><td>");
      buf.append("Expansion's Child Tasks are:");
      buf.append("</td></tr>");

      int numShown = 0;
      for (Enumeration e = expansion.getWorkflow().getTasks(); e.hasMoreElements(); numShown++) {
        Task task = (Task) e.nextElement();
        buf.append("<tr><td>");

        if (numShown >= maxChildTasks) {
          buf.append("... (more than ");
          buf.append(maxChildTasks);
          buf.append(")");
          buf.append("</td></tr>");
          break;
        } else {
          buf.append("Task " + getURL(task.getUID(), TASK) + " " + task.getVerb());

          if (task.getDirectObject() != null) {
            buf.append(" with Direct Object ");// + getURL(task.getDirectObject().getUID(), DIRECT_OBJECT) + " ");
            buf.append(getTypeAndItemInfo(task.getDirectObject()));
            buf.append(".");
          }

          buf.append("</td></tr>");
        }
      }

      buf.append("</table>");
    } else if (planElement instanceof AssetTransfer) {
      AssetTransfer transfer = (AssetTransfer) planElement;
      buf.append("Transfer of <b>");
      buf.append(getTypeAndItemInfo(transfer.getAsset()));
      buf.append("</b> from <b>");
      buf.append(transfer.getAssignor());
      buf.append("</b> to <b>");
      buf.append(getTypeAndItemInfo(transfer.getAssignee()));
      buf.append("</b>");
    } else if (planElement instanceof Aggregation) {
      buf.append("Aggregation of ");
      buf.append(((Aggregation) planElement).getComposition().getParentTasks().size());
      buf.append(" parent tasks.");
    } else if (planElement instanceof Disposition) {
      boolean suc = ((Disposition)planElement).isSuccess();
      buf.append("Disposition as ");
      if (suc)
	buf.append("<font color=green>Success</font>");
      else
	buf.append("<font color=red>Failure</font>");
    }

    if (planElement.getAnnotation() != null) {
      buf.append("<br>Annotation: " + planElement.getAnnotation());
    }


    if (planElement.getEstimatedResult() != null) {
      buf.append(getAllocResult("Estimated", planElement.getEstimatedResult()));
    }
    if (planElement.getReportedResult() != null) {
      buf.append(getAllocResult("Reported", planElement.getReportedResult()));
    }

    return buf.toString();
  }

  /**
   * Get the type & item ID of an Asset -- to ID it in the display
   */
  protected String getTypeAndItemInfo(Asset asset) {
    StringBuffer buf = new StringBuffer();

    if (asset.getTypeIdentificationPG() != null) {
      buf.append(asset.getTypeIdentificationPG().getTypeIdentification() + " - ");
    }
    if (asset.getItemIdentificationPG() != null) {
      buf.append(asset.getItemIdentificationPG().getItemIdentification());
    }

    return buf.toString();
  }

  // End of the various methods to fill the Events Set during plugin Execution
  /////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////
  // Now, the methods to handle the Servlet:
  
  /**
   * Inner-class that's registered as the servlet.
   */
  protected class HistoryWorker extends HttpServlet {
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {
      doPost(request, response);
    }

    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) throws IOException, ServletException {
      // returns a new instance with each query. Perhaps inefficient?
      new HistoryFormatter(request, response);
    }
  }

  /**
   * Add a new event to the list
   */
  protected void addEvent(EventInfo newEvent) {
    events.add(newEvent);

    // If we've reached the Max, trim the oldest event
    if (events.size() > maxEvents) {
      // if the events are sorted by uid, we want to sort them
      // by time first and then remove the oldest entry
      if (sortByUID) {
        // switch back to sorting by time, then uid
        sortByUID = false;

        SortedSet set = new TreeSet();
        set.addAll(events);
        setEvents(set);
      }

      if (logger.isInfoEnabled()) {
        logger.info("removing " + events.first() +
            " since more than max " + events.size() +
            " elements.");
      }

      // Drop the oldest event
      events.remove(events.first());
      didDropOldEntries = true;
    }
  }

  /**
   * Reset the stored event list -- used when we've resorted the list.
   */
  protected void setEvents(SortedSet events) {
    this.events = events;
  }

  /**
   * The inner class used by the servlet to format the request results.
   */
  protected class HistoryFormatter {
    public static final int FORMAT_DATA = 0;
    public static final int FORMAT_XML = 1;
    public static final int FORMAT_HTML = 2;

    private int format = FORMAT_HTML;

    public HistoryFormatter(HttpServletRequest request,
                            HttpServletResponse response) throws IOException, ServletException {
      // Handle the parameters to the request
      processParams(request);
      // Then print the resulting page
      execute(response);
    }

    /**
     * Interpret the Format requested (only HTML supported),
     * sort the events as requested before we do printing.
     */
    protected void processParams(HttpServletRequest request) {
      // Set page format. Default is HTML
      String formatParam = request.getParameter("format");
      if ("data".equals(formatParam)) {
        format = FORMAT_DATA;
      } else if ("xml".equals(formatParam)) {
        format = FORMAT_XML;
      } else {
        format = FORMAT_HTML;
      }

      // Does the user want the detailed toString column?
      String showDetailsParam = request.getParameter("showDetails");
      // Note the pattern below -- do the .equals on the constant,
      // avoiding the need to check for null in the variable String
      showDetails = "true".equals(showDetailsParam);

      // Re-sort the events if necessary.
      String sortByUIDParam = request.getParameter("sortByUID");
      boolean oldValue = sortByUID;
      sortByUID = "true".equals(sortByUIDParam);

      if (sortByUID != oldValue) {
        if (sortByUID) {
          if (logger.isInfoEnabled()) {
            logger.info("sorting by uid, then time.");
          }
          sortByUIDThenTime();
        } else {
          SortedSet set = new TreeSet();
          set.addAll(events);
          //logger.warn ("events before " + events.size() + " vs now "+ set.size());
          setEvents(set);
        }
      }
    } // done processParams

    /**
     * Execute the request - print the web page
     */
    public void execute(HttpServletResponse response) throws IOException, ServletException {
      if (format == FORMAT_HTML) {
        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        String sortLink =
            "<a href=\"/$" +
            encAgentName +
            getPath() +
            "?";

        if (sortByUID) {
          sortLink = sortLink +
              "sortByUID=false" +
              "\">Sort by time</a>";
        } else {
          sortLink = sortLink +
              "sortByUID=true" +
              "\">Sort by uid</a>";
        }

        String detailsLink =
            "<a href=\"/$" +
            encAgentName +
            getPath() +
            "?";

        if (showDetails) {
          detailsLink = detailsLink +
              "showDetails=false" +
              "\">hide object details</a>";
        } else {
          detailsLink = detailsLink +
              "showDetails=true" +
              "\">show object details</a>";
        }

        // Now print the actual page
        out.print("<html><head><title>" +
            "History Servlet for " + agentId.getAddress() +
            "</title></head>" +
            "<body>" +
            "<p><center><h1>" + agentId.getAddress() + " Blackboard History</h1></center>" +
            "<p>" +

            "<center>" +
            sortLink +
            "&nbsp;" +
            detailsLink +
            "</center>" +
            // Here we print the actual table of events
            getHtmlForState() +
            "</body>" +
            "</html>\n");
        out.flush();
      }
      // FIXME: Add support for FORMAT_XML and FORMAT_DATA
    } // end of execute()

    /**
     * Print the actual table of events
     */
    protected String getHtmlForState() {
      StringBuffer buf = new StringBuffer();

      // First we print the Table header....
      if (didDropOldEntries) {
        buf.append("<center>Note: Too many changes have occurred; only newest ");
        buf.append(maxEvents);
        buf.append(" are shown.</center><br/>");
      }

      // tell user what the gray-white highlighting means
      buf.append("<center>Gray-white alternation indicates different ");

      if (sortByUID) {
        buf.append("blackboard objects.");
      } else {
        buf.append("plugin execute cycles.");
      }
      buf.append("<br/>Time shown is scenario time.</center>");

      buf.append("<table border=1 align=center>");
      buf.append("<tr>");
      buf.append("<th>");
      // Event Number
      // Note that this may never appear in order, since the table is 
      // sorted by the comparator
      buf.append("#");
      buf.append("</th>");
      buf.append("<th>");
      buf.append("When"); // Scenario time
      buf.append("</th>");
      buf.append("<th>");
      buf.append("Type"); // Add/Change/Remove
      buf.append("</th>");
      buf.append("<th>");
      buf.append("Event"); // Basic summary
      buf.append("</th>");
      if (showChangeReport) {
        buf.append("<th>");
        buf.append("Change Report");
        buf.append("</th>");
      }
      buf.append("<th>");
      buf.append("Comment"); // Human readable descriptio in context
      buf.append("</th>");

      if (showDetails) {
        buf.append("<th>");
        buf.append("Object Details"); // Full toString
        buf.append("</th>");
      }

      buf.append("</tr>");
      long lastTime = -1;
      String lastUID = "";
      boolean colorRowGrey = false;

      // Now Loop over the events and print them
      synchronized (events) {
        for (Iterator iter = events.iterator(); iter.hasNext();) {
          EventInfo event = (EventInfo) iter.next();

          // Alternate colors in rows
          if (sortByUID) {
            // Alternate based on changing UID
            if (!event.uid.equals(lastUID)) {
              colorRowGrey = !colorRowGrey;
              lastUID = event.uid;
            }
          } else {
            // Alternate based on changing timestamp
            if (event.timeStamp != lastTime && event.timeStamp != -1) {
              colorRowGrey = !colorRowGrey;
              lastTime = event.timeStamp;
            }
          }

          // Add the next Event
          buf.append(event.toHTML(colorRowGrey, showChangeReport, showDetails));
          buf.append("\n");
        }
      }
      buf.append("</table>");
      return buf.toString();
    } // end of getHTMLForState helper method, called by execute()
  } // end of HistoryFormatter inner class

  /**
   * Sort the events collection by UID and then time. Then type, then meaning.
   * Updates the single events collection Set
   */
  protected void sortByUIDThenTime() {
    SortedSet events2 = new TreeSet(new Comparator() {
      public int compare(Object o1, Object o2) {
        EventInfo e1 = (EventInfo) o1;
        EventInfo e2 = (EventInfo) o2;
        int comp = e1.uid.compareTo(e2.uid);

        if (comp == 0) {
          if (e1.timeStamp < e2.timeStamp) {
            comp = -1;
          } else if (e1.timeStamp > e2.timeStamp) {
            comp = 1;
          }
        }

        if (comp == 0) {
          comp = e1.type - e2.type;
        }

        if (comp == 0) {
          comp = e1.meaning.compareTo(e2.meaning);
        }

        if (comp == 0) {
          if (logger.isInfoEnabled()) {
            logger.info(e1 + "\nequals\n" + e2);
          }
        }

        return comp;
      }
    });

    events2.addAll(events);
    setEvents(events2);
  }

  /**
   * Encodes a string that may contain HTML syntax-significant
   * characters.
   */
  protected String encodeHTML(String s) {
    if (s == null)
      return "";

    boolean noBreakSpaces = true;
    // Commenting out sawEquals for now -- a broken
    // attempt to break up long lines....
    //      boolean sawEquals = false;
    StringBuffer buf = null;  // In case we need to edit the string
    int ix = 0;               // Beginning of uncopied part of s
    for (int i = 0, n = s.length(); i < n; i++) {
      String replacement = null;
      switch (s.charAt(i)) {
        case '"':
          replacement = "&quot;";
          break;
          //case '[': replacement = "["; break;
        case '<':
          if (i != 0 && s.charAt(i - 1) == ' ')
            replacement = "<br/>&nbsp;&lt;";
          else
            replacement = "&lt;";
          break;
        case '>':
	  // put in a line break after '> ' as in the toString of many objects
          if (i + 1 < n && s.charAt(i + 1) == ' ')
            replacement = "&gt;<br/>";
	  else
	    replacement = "&gt;";
          break;
        case ',':
	  // put in a line break after '>,' as in the toString of ServiceContractRelays
	  if (i > 0 && i + 1 < n && s.charAt(i - 1) == '>' && s.charAt(i + 1) == ' ')
	    replacement = ",<br/>";
	  else
	    replacement = ",";
	  break;
        case '&':
          replacement = "&amp;";
          break;
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

  /**
   * This is the class that stores the information about an event
   * Basic information about a particular event in a Transaction
   */
  private static class EventInfo implements Comparable {
    public int num; // Event number
    public String uid; // The UID of the object
    public String event; // Summary descripton of the event
    public long timeStamp; // The scenario time of the event

    // Goes in the Comment column - basic
    // human readable description of the Event
    public String meaning;
    public Set changeReports = null; // Any ChangeReports
    public String toStringResult = ""; // Full toString of the Event
    public int type; // Add, Change, or Remove

    public EventInfo(int type, int num, String uid, String event, long timeStamp, String meaning) {
      this.type = type;
      this.num = num;
      this.uid = uid;
      this.event = event;
      this.timeStamp = timeStamp;
      this.meaning = meaning;
    }

    public EventInfo(int type, int num, String uid, String event, long timeStamp, String meaning, Set changeReports) {
      this(type, num, uid, event, timeStamp, meaning);
      this.changeReports = changeReports;
    }

    public EventInfo(int type, int num, String uid, String event, long timeStamp, String meaning, String toStringResult) {
      this(type, num, uid, event, timeStamp, meaning);
      this.toStringResult = toStringResult;
    }

    public EventInfo(int type, int num, String uid, String event, long timeStamp,
                     String meaning, Set changeReports, String toStringResult) {
      this(type, num, uid, event, timeStamp, meaning, changeReports);
      this.toStringResult = toStringResult;
    }

    /**
     * compare first by timestamp, and withing that, by UID
     * Then by type (A/C/R), and finally by the String meaning.
     */
    public int compareTo(Object other) {
      EventInfo otherEvent = (EventInfo) other;

      if (timeStamp < otherEvent.timeStamp)
        return -1;
      if (timeStamp > otherEvent.timeStamp)
        return 1;

      int uidComp = uid.compareTo(otherEvent.uid);
      if (uidComp != 0)
        return uidComp;

      int val = type - otherEvent.type;

      if (val != 0)
        return val;

      val = meaning.compareTo(otherEvent.meaning);

      return val;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("<EventInfo " + num + " at ");
      buf.append(format.format(new Date(timeStamp)));
      String typeS = " Added ";
      if (type == CHANGED)
        typeS = " Changed ";
      else if (type == REMOVED)
        typeS = " Removed ";
      buf.append(typeS);
      buf.append(meaning);
      buf.append(">");
      return buf.toString();
    }

    /**
     * Produce HTML for the Servlet display. "odd" boolean used to alternate colors for rows
     */
    public String toHTML(boolean odd, boolean showChangeReport, boolean showDetails) {
      String color = odd ? "#FFFFFF" : "#c0c0c0";
      StringBuffer buf = new StringBuffer();
      buf.append("<tr BGCOLOR=" + color + ">");
      buf.append("<td>" + num + "</td>");
      buf.append("<td>" + format.format(new Date(timeStamp)) + "</td>");
      String typeString = "<font color=green>Added</font>";
      if (type == CHANGED)
        typeString = "<font color=blue>Changed</font>";
      else if (type == REMOVED)
        typeString = "<font color=red>Removed</font>";

      buf.append("<td>" + typeString + "</td>");
      buf.append("<td>" + event + "</td>");

      if (showChangeReport) {
        buf.append("<td>");

        if (changeReports != null) {
          buf.append(htmlForChangeReport());
        }

        buf.append("</td>");
      }

      if (meaning != null) {
        buf.append("<td>" + meaning + "</td>");
      }

      if (showDetails) {
        buf.append("<td>" + toStringResult + "</td>");
      }

      buf.append("</tr>");

      return buf.toString();
    }

    /**
     * Produce pretty HTML to show the ChangeReports on a Transaction
     *
     * @return HTML Table of ChangeReport data
     */
    public String htmlForChangeReport() {
      StringBuffer buf = new StringBuffer();
      buf.append("<table>");

      for (Iterator iter = changeReports.iterator(); iter.hasNext();) {
        Object obj = iter.next();
        buf.append("<tr><td>" + obj + "</td></tr>");
      }

      buf.append("</table>");

      return buf.toString();
    }
  } // end of EventInfo class 
} // end of HistoryServlet


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

import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.plugin.PizzaPreferences;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.UnaryPredicate;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Shows collected RSVPs from invited guests at "/pizza".
 * Load into the Agent doing the inviting (has the {@link PizzaPreferences} object).
 * In our case, that is Alice.
 */
public class PizzaPreferenceServlet extends BaseServletComponent {
  protected BlackboardQueryService blackboardQueryService;
  protected LoggingService logger;
  protected String agentID;

  public void load() {
    super.load();

    // get services
    blackboardQueryService = (BlackboardQueryService)
        serviceBroker.getService(this, BlackboardQueryService.class, null);
    AgentIdentificationService agentIDService =
        (AgentIdentificationService) serviceBroker.getService(this,
            AgentIdentificationService.class,
            null);
    if (agentIDService != null) {
      agentID = agentIDService.getMessageAddress().toString();
      
      // Release the agentIDService right away, since we don't need it any more
      serviceBroker.releaseService(this, AgentIdentificationService.class, agentIDService);
      agentIDService = null;
    }
  }

  // Whenever you have a load() method, you should have an unload
  public void unload() {
    if (blackboardQueryService != null) {
      serviceBroker.releaseService(this, BlackboardQueryService.class, blackboardQueryService);
      blackboardQueryService = null;
    }
    super.unload();
  }

  /**
   * This servlet listens at "/pizza"
   */
  protected String getPath() {
    return "/pizza";
  }

  /**
   * Using an inner class to implement the Servlet interface
   * provides a useful design pattern.
   */
  protected Servlet createServlet() {
    return new PizzaWorker();
  }

  /**
   * Inner class that's registered as the servlet.
   */
  protected class PizzaWorker extends HttpServlet {
    // Often we want the servlet to behave identically for Get or Post
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {
      doPost(request, response);
    }

    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) throws IOException, ServletException {
      new PizzaFormatter(request, response);
    }
  }

  /**
   * Worker class that actually produces HTML for the servlet
   */
  protected class PizzaFormatter {
    public static final int FORMAT_DATA = 0; // Not yet supported
    public static final int FORMAT_XML = 1; // Not yet supported
    public static final int FORMAT_HTML = 2;

    private int format = FORMAT_HTML;

    public PizzaFormatter(HttpServletRequest request,
                          HttpServletResponse response) throws IOException, ServletException {
      getFormat(request);
      execute(response);
    }

    /**
     * Parse the requested format. A similar pattern could be used to
     * handle other parameters.
     * Note however that only HTML is currently supported.
     */
    protected void getFormat(HttpServletRequest request) {
      String formatParam = request.getParameter("format");
      if ("data".equals(formatParam)) {
        format = FORMAT_DATA;
      } else if ("xml".equals(formatParam)) {
        format = FORMAT_XML;
      } else {
        format = FORMAT_HTML; // default
      }
    }

    /**
     * Write the servlet response into the given response's stream.
     */
    public void execute(HttpServletResponse response) throws IOException, ServletException {
      if (format == FORMAT_HTML) {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.print("<html><head><title>" +
            "The Pizza Party" +
            "</title></head>" +
            "<body>" +
            "<p/>" +
            "<p/><center><h1>Pizza Preferences</h1><p/>" +
            "<b>RSVP from each invited guest, invited by host " +
            agentID +
            "</b></center><p/>" +
            getHtmlForPreferences() +
            "<br><br>" +
            getHtmlForOrders() +
            "</body>" +
            "</html>\n");
        out.flush();
      }
      // FIXME: Add support for FORMAT_XML and FORMAT_DATA
    }

    protected String getHtmlForOrders() {
      Collection pizzaOrders = blackboardQueryService.query(new UnaryPredicate() {
        public boolean execute(Object o) {
          if (o instanceof Expansion) {
            Task task = ((Expansion)o).getTask();
            return task.getVerb().equals(Constants.Verbs.ORDER);
          }
          return false;
        }
      });

      if(pizzaOrders.isEmpty()) {
        return "<center><b>No Orders placed at the moment.</b></center>";
      } else {
        Iterator iter = pizzaOrders.iterator();
        StringBuffer buf = new StringBuffer();
        while(iter.hasNext()) {
          Expansion exp = (Expansion)iter.next();

          // Completely done, look for Confidence == 1.

          buf.append("<center>");
          buf.append("<table border=\"1\"><tr><td><b>Quantity</b></td><td><b>Type</b></td><td><b>Ordered From</b></td></tr>");
          Enumeration enum = exp.getWorkflow().getTasks();
          while(enum.hasMoreElements()) {
            buf.append("<tr>");
            Task t = (Task)enum.nextElement();
            buf.append("<td>");
            double qty = t.getPreferredValue(AspectType.QUANTITY);
            buf.append(String.valueOf(qty));
            buf.append("</td>");
            buf.append("<td>");
            buf.append(t.getDirectObject().getItemIdentificationPG().getItemIdentification());
            buf.append("</td>");
            buf.append("<td>");
            buf.append(((Allocation)t.getPlanElement()).getAsset().getItemIdentificationPG().getItemIdentification());
            buf.append("</td>");
            buf.append("</tr>");
          }
          buf.append("</table>");
          buf.append("</center>");   // + getTypeAndItemInfo(.) + "<center><br>");
          // no plan element or have an allocation  -- have a pref. and a parent task.
        }
        return "<center><b>Order Placed</b></center>" + buf.toString();
      }
    }

    /**
     * Query the Blackboard for the {@link PizzaPreferences} object,
     * and display the contents in a Table.
     */
    protected String getHtmlForPreferences() {
      Collection pizzaPreferences = blackboardQueryService.query(new UnaryPredicate() {
        public boolean execute(Object o) {
          return (o instanceof PizzaPreferences);
        }
      });

      if (pizzaPreferences.isEmpty()) {
        return "<center><b>Waiting for invitiation RSVP from friends.</b></center>";
      } else {
        // Our subscription is a Collection, but we only expect one
        // PizzaPreferences object -- so we just look at the first.
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
        for (Iterator iter = new TreeSet(prefs.getFriends()).iterator(); iter.hasNext();) {
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

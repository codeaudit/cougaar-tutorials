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
import java.util.TreeSet;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

/**
 * The main UI for the application: shows collected RSVPs from invited guests at
 * "/pizza", and the progress on ordering the pizza.
 * <p>
 * Load into the Agent doing the inviting (has the {@link PizzaPreferences}
 * object). In our case, that is Alice.
 */
public class PizzaPreferenceServlet
      extends BaseServletComponent {
   // This is a servlet, so no subscriptions -- instead,
   // we do one-time queries when needed
   protected BlackboardQueryService blackboardQueryService;
   protected LoggingService logger;
   protected String agentID; // this Agent's name

   /**
    * Load services needed by this servlet: BlackboardQueryService,
    * AgentIDService. Uses the BlackboardQueryService to get a snapshot of the
    * blackboard status when a user asks, and the AgentIDService to get this
    * agent's name.
    */
   @Override
   public void load() {
      super.load();

      // get services
      blackboardQueryService = getService(this, BlackboardQueryService.class, null);
      AgentIdentificationService agentIDService = getService(this, AgentIdentificationService.class, null);
      if (agentIDService != null) {
         agentID = agentIDService.getMessageAddress().toString();

         // Release the agentIDService right away, since we don't need it any
         // more
         releaseService(this, AgentIdentificationService.class, agentIDService);
         agentIDService = null;
      }
   }

   /**
    * Whenever you have a load() method, you should have an unload, to release
    * services.
    */
   @Override
   public void unload() {
      if (blackboardQueryService != null) {
         releaseService(this, BlackboardQueryService.class, blackboardQueryService);
         blackboardQueryService = null;
      }
      super.unload();
   }

   /**
    * This servlet listens at "/pizza".
    */
   @Override
   protected String getPath() {
      return "/pizza";
   }

   /**
    * Using an inner class to implement the Servlet interface provides a useful
    * design pattern.
    */
   @Override
   protected Servlet createServlet() {
      return new PizzaWorker();
   }

   /**
    * Inner class that's registered as the servlet.
    */
   protected class PizzaWorker
         extends HttpServlet {
      private static final long serialVersionUID = 1L;

      // Often we want the servlet to behave identically for Get or Post
      @Override
      public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
         doPost(request, response);
      }

      @Override
      public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
         new PizzaFormatter(request, response);
      }
   }

   /**
    * Worker class that actually produces HTML for the servlet.
    */
   protected class PizzaFormatter {
      public PizzaFormatter(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
         execute(response);
      }

      /**
       * Write the servlet response into the given response's stream.
       */
      public void execute(HttpServletResponse response)
            throws IOException {
         response.setContentType("text/html");

         // This worker only has one call to PrintWriter.print, with the
         // sub-methods
         // calling StringBuffer.append()...
         // An alternative approach would be to keep an inner-class member
         // variable
         // for the PrintWriter, and instead of those buf.append calls, the
         // sub-methods
         // could write directly to the output stream.
         PrintWriter out = response.getWriter();
         out.print("<html><head><title>" + "The " + Constants.PIZZA + " Party" + "</title></head>" + "<body>" + "<p/><center><h1>"
               + Constants.PIZZA + " Party Planner Notes</h1></center><p/>" + "<center><a href=\"/$" + agentID + "/list\">"
               + agentID + "</a> is having a " + Constants.PIZZA
               + " party, and inviting everyone on her \"buddy list\", the people in the <a href=\"/$" + agentID
               + "/communityViewer?community=" + Constants.COMMUNITY + "\">" + Constants.COMMUNITY
               + "</a> community. She sends them a Relay, with the invitation: `" + Constants.INVITATION_QUERY
               + "'. After waiting a little while for them to reply, she will find a " + Constants.PIZZA
               + " parlor, and order them each the kind of " + Constants.PIZZA + " that they prefer - if she can find "
               + Constants.PIZZA + " parlors to satisfy her guests!"
               + "<br><br>Parlors each have a Kitchen with specific capabilities. Some serve " + Constants.MEAT_PIZZA + ", some "
               + Constants.VEGGIE_PIZZA + ", and some both. " + agentID + " wants to order all her " + Constants.PIZZA
               + " from one parlor. If the parlor can't deliver the " + Constants.PIZZA + " she wants, that order Task fails. "
               + agentID + " will try to find another " + Constants.PIZZA
               + " provider, if she knows how to do Service Discovery...." + "</center><br><br>"
               + "<center><b>RSVP from each invited guest, invited by host " + agentID + "</b>:</center><p/>"
               + getHtmlForPreferences() + "<br>" + getHtmlForOrders() + "<br><hr><center>Status at: "
               + new Date(System.currentTimeMillis()) + "</center></body>" + "</html>\n");
         out.flush();
      }

      /**
       * Get HTML to represent the Orders that have been placed.
       * 
       * @return HTML for output
       */
      protected String getHtmlForOrders() {
         // Get the Expansion of the root Task of verb Order.
         // Note that this is a typical servlet, that has no subscriptions.
         // Instead, it just wants to display a snapshot of the current
         // state, so it uses the BlackboardQueryService.
         Collection pizzaOrders = blackboardQueryService.query(new UnaryPredicate() {
            private static final long serialVersionUID = 1L;

            public boolean execute(Object o) {
               if (o instanceof Expansion) {
                  Task task = ((Expansion) o).getTask();
                  return task.getVerb().equals(Constants.Verbs.ORDER);
               }
               return false;
            }
         });

         if (pizzaOrders.isEmpty()) {
            return "<center><b>No Orders placed yet.</b></center>";
         } else {
            Iterator iter = pizzaOrders.iterator();
            StringBuffer buf = new StringBuffer();

            boolean orderOK = true; // if order sent, did it succeed
            boolean orderSent = true;

            String head = ""; // Fill in header once we have the expansion
            // Loop over the Expansions (expect only one)
            while (iter.hasNext()) {
               Expansion exp = (Expansion) iter.next();

               buf.append("<center>");
               buf.append("<table border=\"1\"><tr><th>Servings</th><th>Type</th><th>Ordered From</th><th>Status</th></tr>");
               Enumeration en = exp.getWorkflow().getTasks();
               // Loop over the sub-tasks
               while (en.hasMoreElements()) {
                  buf.append("<tr>");
                  Task t = (Task) en.nextElement();
                  buf.append("<td>");
                  // Number of servings of pizzas ordered
                  double qty = t.getPreferredValue(AspectType.QUANTITY);
                  buf.append(String.valueOf(qty));
                  buf.append("</td>");
                  buf.append("<td>");
                  // Kind of pizza ordered
                  buf.append(t.getDirectObject().getItemIdentificationPG().getItemIdentification());
                  buf.append("</td>");
                  buf.append("<td>");

                  Allocation pe = (Allocation) t.getPlanElement();
                  if (pe != null) {
                     // Store ordered from
                     String store = pe.getAsset().getItemIdentificationPG().getItemIdentification();
                     // Link store name to that Agent's list of servlets.
                     // FIXME: URLEncode.encode(store, "UTF-8") --- to be safe,
                     // should encode these arbitrary strings...
                     buf.append("<a href=\"/$" + store + "/list\">" + store + "</a>");
                     buf.append("</td>");

                     // Order status
                     boolean subSucc = pe.getReportedResult().isSuccess();
                     buf.append("<td>");

                     // Link the word order to the /tasks servlet for the Order
                     // Task ordering this
                     // kind of pizza. Color code text by success result.
                     String orderlink = "<a href=\"/$" + agentID + "/tasks?mode=3&uid=" + t.getUID() + "\">Order</a>";
                     if (subSucc) {
                        buf.append("<font color=green>" + orderlink + " filled!</font>");
                     } else {
                        orderOK = false;
                        // Provider failed this order task. Say so, with link to
                        // explanation
                        buf.append("<font color=red>" + orderlink + " FAILed!<a href=\"#why\">*</a></font>");
                        // FIXME: Here, we could indicate if there is
                        // an outstanding FindProviders task, including
                        // the Role / exclusions if any
                     }
                  } else {
                     orderOK = false;
                     orderSent = false;
                     buf.append("[Order not sent yet.]</td><td>&nbsp;");
                     // FIXME: Here, we could indicate if there is
                     // an outstanding FindProviders task, including
                     // the Role / exclusions if any
                  }
                  buf.append("</td>");
                  buf.append("</tr>");
               }
               buf.append("</table>");

               // If an order failed, include the footnote explaining why
               if (!orderOK) {
                  buf.append("<br><a name=\"why\"/>* This failure may be because the parlor does not have the topping ordered.");
               }

               buf.append("</center>");

               // Create the table title: Link the word order to the root Pizza
               // order task in the
               // PlanViewerServlet (/tasks)
               // FIXME: encode UID --- to be safe, should encode these
               // arbitrary strings...
               // FIXME: URLEncode.encode(agentID, "UTF-8")
               head = "<center><b><a href=\"/$" + agentID + "/tasks?mode=3&uid=" + exp.getTask().getUID() + "\">Order</a> ";
               if (orderSent) {
                  if (orderOK) {
                     head += "Placed <font color=green>Successfully</font> - Party is on!</b></center><p/>";
                  } else {
                     head += "<font color=red>Failed</font> - guests will not be happy!</b></center><p/>";
                  }
               } else {
                  head += "not yet placed.</b></center>";
               }
            } // loop over expansions

            return head + buf.toString();
         } // had some Pizza Order task expansions
      } // end of getHtmlForOrders

      /**
       * Query the Blackboard for the {@link PizzaPreferences} object, and
       * display the contents in a Table.
       */
      protected String getHtmlForPreferences() {
         Collection pizzaPreferences = blackboardQueryService.query(new UnaryPredicate() {
            private static final long serialVersionUID = 1L;

            public boolean execute(Object o) {
               return (o instanceof PizzaPreferences);
            }
         });

         if (pizzaPreferences.isEmpty()) {
            // Link the word friends to the Community Viewer servlet showing the
            // membership
            // in the community. Remember that this membership will be updated
            // as people show up.
            // FIXME: URLEncode.encode(agentID, "UTF-8") --- to be safe, should
            // encode these arbitrary strings...
            return "<center><b>Waiting for invitiation RSVPs from <a href=\"/$" + agentID + "/communityViewer?community="
                  + Constants.COMMUNITY + "\">friends</a>....</b></center>";
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
            // Add one row per friend
            for (Iterator iter = new TreeSet(prefs.getFriends()).iterator(); iter.hasNext();) {
               buf.append("<tr>");

               buf.append("<td>");
               String friend = (String) iter.next();

               // Link the name of the friend to that Agent's list of servlets
               // FIXME: URLEncode.encode(friend, "UTF-8") --- to be safe,
               // should encode these arbitrary strings...
               buf.append("<a href=\"/$" + friend + "/list\">" + friend + "</a>");
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

/*
 * <copyright>
 *  
 *  Copyright 1997-2007 BBNT Solutions, LLC
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

package org.cougaar.demo.mesh;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.SimpleRelay;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.servlet.ComponentServlet;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.annotations.Cougaar;

/**
 * This servlet shows our mesh relays as an HTML page.
 * <p>
 * Supports an optional Servlet path parameter, which defaults to "/mesh".
 * <p>
 * For simplicity, it's easiest to load a copy of this servlet into every agent.
 */
public class MeshServlet
      extends ComponentServlet {

   private static final long serialVersionUID = 1L;

   private long loadTime;

   @Cougaar.ObtainService()
   public BlackboardQueryService blackboard;

   /** @return a default path if a plugin parameter is not specified */
   @Override
   protected String getPath() {
      String ret = super.getPath();
      return (ret == null ? "/mesh" : ret);
   }

   /** This method is called when the agent is created */
   @Override
   public void load() {
      super.load();

      // Record our load time
      loadTime = System.currentTimeMillis();
      
      LoggingService log = getService(this, LoggingService.class,  null);
      log.warn("Mesh agent " + getEncodedAgentName() + " has been loaded");
   }

   /** This method is called when the agent is created.
    *  It is only here to log a message to let the user know what to browser to
    */
   @Override
   public void start() {
      super.start();
      LoggingService log = getService(this, LoggingService.class,  null);

      log.warn("Mesh agent " + getEncodedAgentName() + " has been started");
      log.warn("Browse to http://localhost:8800/$" + getEncodedAgentName() + "/mesh to interact with Mesh agent " + getEncodedAgentName());
   }

   /** This method is called whenever the browser loads our URL. */
   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response)
         throws IOException {

      // Begin our HTML page response
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      String title = "Agent " + getEncodedAgentName();
      out.println("<html>" + "<head><title>" + title + "</title></head>" + "<body><h1>" + title + "</h1>");

      // Write how long we've been running, to make it easy for the user
      // to calculate the relay throughput
      long runTime = System.currentTimeMillis() - loadTime;
      out.println("Milliseconds since agent load: " + runTime + "<p>");

      // Query the blackboard for relays
      UnaryPredicate<SimpleRelay> pred = new UnaryPredicate<SimpleRelay>() {
         private static final long serialVersionUID = 1L;

         public boolean execute(Object o) {
            return (o instanceof SimpleRelay);
         }
      };
      Collection<SimpleRelay> col = blackboard.query(pred);

      // We expect multiple "incoming" relays from different remote targets,
      // each
      // with possibly different counter values, plus corresponding "outgoing"
      // relays with identical counter values.
      //
      // So, we'll split our relays into pairs based on the targets.
      Map<String, SimpleRelay> sent = new HashMap<String, SimpleRelay>(col.size() >> 1);
      Map<String, SimpleRelay> recv = new HashMap<String, SimpleRelay>(col.size() >> 1);
      for (SimpleRelay relay : col) {
         boolean isOutgoing = getAgentIdentifier().equals(relay.getSource());
         MessageAddress addr = (isOutgoing ? relay.getTarget() : relay.getSource());
         String key = (addr == null ? null : addr.getAddress());
         if (key == null) {
            key = "null";
         }
         if (isOutgoing) {
            sent.put(key, relay);
         } else {
            recv.put(key, relay);
         }
      }

      // Get a sorted set of all unique target names
      Set<String> targetSet = new HashSet<String>(sent.keySet());
      targetSet.addAll(recv.keySet());
      List<String> targets = new ArrayList<String>(targetSet);
      Collections.sort(targets);

      // Write the relays as an HTML table
      out.println("<table border=1>" + "<tr>" + "  <th rowspan=2></th>" + "  <th rowspan=2>Target</th>"
            + "  <th colspan=5>Sent</th>" + "  <th colspan=5>Received</th>" + "</tr>" + "<tr>"
            + "  <th bgcolor=lightgray>&nbsp;</th>" + "  <th>UID</th>" + "  <th>Bloat (byte[])</th>" + "  <th>Count</th>"
            + "  <th>Throughput (relays/second)</th>" + "  <th bgcolor=lightgray>&nbsp;</th>" + "  <th>UID</th>"
            + "  <th>Bloat (byte[])</th>" + "  <th>Count</th>" + "  <th>Throughput (relays/second)</th>" + "</tr>");
      DecimalFormat formatter = new DecimalFormat("0.000");
      for (int i = 0; i < targets.size(); i++) {
         String target = targets.get(i);

         out.print("<tr align=right>" + "  <td>" + i + "</td>" + "  <td>" + target + "</td>");

         for (int j = 0; j < 2; j++) {
            SimpleRelay relay = (j == 0 ? sent.get(target) : recv.get(target));

            out.print("  <td bgcolor=lightgray>&nbsp;</td>");

            if (relay == null) {
               out.print("  <td colspan=4><font color=red>null</font></td>");
               continue;
            }

            int bloat = -1;
            Object o = relay.getQuery();
            if (o instanceof Payload) {
               Payload p = (Payload) o;
               bloat = p.getBloat();
               o = p.getData();
            }
            long count = ((Long) o).longValue();

            double throughput = Double.NaN;
            if (count > 0 && runTime > 0) {
               throughput = 1000.0 * ((double) count / runTime);
            }

            out.print("  <td>" + relay.getUID() + "</td>" + "  <td>" + bloat + "</td>" + "  <td>" + count + "</td>" + "  <td>"
                  + formatter.format(throughput) + "</td>");
         }

         out.println("<tr>");
      }
      out.println("</table>");

      // Create a "reload" button for the user to invoke our servlet again
      out.println("<form method=\"get\" action=\"" + request.getRequestURI() + "\">" + "  <input type=\"submit\" value=\"Reload\">"
            + "</form>");

      // End our HTML page
      out.println("</body></html>");
   }
}

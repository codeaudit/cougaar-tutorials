/*
 * <copyright>
 *  
 *  Copyright 1997-2006 BBNT Solutions, LLC
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

package org.cougaar.demo.ping;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.TodoPlugin;
import org.cougaar.core.relay.SimpleRelay;
import org.cougaar.core.relay.SimpleRelaySource;
import org.cougaar.util.Arguments;
import org.cougaar.util.annotations.Cougaar;
import org.cougaar.util.annotations.Subscribe;

/**
 * This plugin is an example ping source that sends relays to a remote agent.
 * <p>
 * There can be multiple copies of this plugin in a single agent, but every
 * {@link PingSender} must have a unique target. The target is specified as a
 * plugin parameter:
 * <dl>
 * <dt>target=<i>String</i></dt>
 * <dd>Required remote agent name. If the agent doesn't exist then we wait
 * forever -- there's no alarm-based timeout in this plugin implementation.</dd>
 * </p>
 * 
 * <dt>delayMillis=<i>long</i></dt> <dd>Delay milliseconds between relay
 * iterations. Set the delay to zero to run the pings as fast as possible.</dd>
 * <p>
 * 
 * <dt>verbose=<i>boolean</i></dt>
 * <dd>Output SHOUT-level logging messages. This can also be disabled by
 * modifying the Cougaar logging configuration to set:
 * 
 * <pre>
 *         log4j.category.org.cougaar.demo.ping.PingSender=FATAL
 *         log4j.category.org.cougaar.demo.ping.PingReceiver=FATAL
 * </pre>
 * 
 * For simplicity we support this as a plugin parameter, so new users don't need
 * to configure the logging service. If enabled, also consider turning off "+/-"
 * message send/receive logging by setting:
 * 
 * <pre>
 *         -Dorg.cougaar.core.agent.quiet=true
 * </pre>
 * 
 * </dd>
 * <p>
 * </dl>
 * 
 * @property org.cougaar.demo.ping.PingSender.delayMillis=5000 PingSender delay
 *           between ping iterations, if not set as a plugin parameter.
 * 
 * @property org.cougaar.demo.ping.PingSender.verbose=true PingSender should
 *           output SHOUT-level logging messages, if not set as a plugin
 *           parameter.
 * 
 * @see PingReceiver Required plugin for every agent that will receive ping
 *      relays.
 * 
 * @see PingServlet Optional browser-based GUI.
 */
public class PingSender
      extends TodoPlugin {

   @Cougaar.Arg(required = true)
   public MessageAddress target;

   @Cougaar.Arg(defaultValue = "5000")
   public long delayMillis;

   @Cougaar.Arg(defaultValue = "true")
   public boolean verbose;

   @Override
   public void setArguments(Arguments args) {
      super.setArguments(args);
      if (target.equals(agentId)) {
         throw new IllegalArgumentException("Target matches self: " + target);
      }
   }

   @Override
   protected void setupSubscriptions() {
      super.setupSubscriptions();
      // Get our initial counter value, which is zero unless we're restarting
      // from an agent move or persistence snapshot
      int counter = getInitialCounter();

      // Send our first relay to our target
      sendNow(null, new Integer(counter));

      // When our target publishes a response, our "execute()" method will
      // be called.
   }

   /** Create our "isMyRelay" subscription and handle CHANGE callbacks */
   @Cougaar.Execute(on = Subscribe.ModType.CHANGE, when = "isRelayForAgent")
   public void executeRelay(SimpleRelay relay) {
      // Print the target's response
      if (verbose && log.isShoutEnabled()) {
         log.shout("Received response " + relay.getReply() + " from " + target);
      }

      // Figure out our next content value
      //
      // For scalability testing we could make this a large byte array.
      int oldContent = (Integer) relay.getQuery();
      Integer newContent = new Integer(++oldContent);

      if (delayMillis > 0) {
         sendLater(relay, newContent);
      } else {
         sendNow(relay, newContent);
      }
   }

   public boolean isRelayForAgent(SimpleRelay relay) {
      return agentId.equals(relay.getSource()) && target.equals(relay.getTarget());
   }

   /** Get our initial ping iteration counter value */
   private int getInitialCounter() {
      // Check to see if we've already sent a ping, in case we're restarting
      // from an agent move or persistence snapshot.
      int ret = 0;
      if (blackboard.didRehydrate()) {
         // Get the counter from our sent ping, if any, then remove it
         IncrementalSubscription<SimpleRelay> sub = getSubscription("isRelayForAgent", SimpleRelay.class);
         for (SimpleRelay relay : sub) {
            ret = ((Integer) relay.getQuery()).intValue();
            blackboard.publishRemove(relay);
         }
         if (verbose && log.isShoutEnabled()) {
            log.shout("Resuming pings to " + target + " at counter " + ret);
         }
      }
      return ret;
   }

   /** Send our next relay iteration now */
   private void sendNow(SimpleRelay priorRelay, Object content) {
      if (priorRelay != null) {
         // Remove query both locally and at the remote target, to cleanup
         // the blackboard.
         blackboard.publishRemove(priorRelay);
      }

      // Send a new relay to the target
      SimpleRelay relay = new SimpleRelaySource(uids.nextUID(), agentId, target, content);
      if (verbose && log.isShoutEnabled()) {
         log.shout("Sending ping " + content + " to " + target);
      }
      blackboard.publishAdd(relay);
   }

   /** Send our next relay in delayMillis */
   private void sendLater(final SimpleRelay priorRelay, final Object newContent) {
      // Run sendNow later, still in the blackboard execution context.
      if (verbose && log.isShoutEnabled()) {
         log.shout("Will send ping " + newContent + " to " + target + " in " + delayMillis / 1000 + " seconds");
      }
      executeLater(delayMillis, new Runnable() {
         public void run() {
            sendNow(priorRelay, newContent);
         }
      });
   }
}

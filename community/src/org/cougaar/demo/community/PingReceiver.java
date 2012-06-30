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

package org.cougaar.demo.community;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.AnnotatedSubscriptionsPlugin;
import org.cougaar.core.relay.SimpleRelay;
import org.cougaar.core.relay.SimpleRelaySource;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.util.annotations.Cougaar;
import org.cougaar.util.annotations.Cougaar.Arg;
import org.cougaar.util.annotations.Cougaar.ObtainService;
import org.cougaar.util.annotations.Subscribe;


/**
 * This plugin is an example ping target that receives relays and sends back a
 * reply.
 * <p>
 * A "verbose=<i>boolean</i>" plugin parameter and System property is supported,
 * exactly as documented in {@link PingSender}.
 * <p>
 * There must be one instance of this plugin in every agent that will receive
 * {@link PingSender} relays. For simplicity, it's easiest to load a copy of
 * this plugin into every agent.
 * 
 * @property org.cougaar.demo.community.PingReceiver.verbose=true PingReceiver
 *           should output SHOUT-level logging messages, if not set as a plugin
 *           parameter.
 * 
 * @see PingSender Remote plugin that sends the ping relays to this plugin
 * 
 * @see PingServlet Optional browser-based GUI.
 */
public class PingReceiver
      extends AnnotatedSubscriptionsPlugin {


   @ObtainService
   public LoggingService log;
   
   @ObtainService
   public UIDService uids;

   @Arg(defaultValue="false")
   public boolean verbose;

   private SimpleRelay reply_relay;

   @Cougaar.Execute(on = {Subscribe.ModType.CHANGE, Subscribe.ModType.ADD}, when = "isABA")
   public void replyTo(SimpleRelay relay) {
      log.debug("well, let's reply, I'm polite!");
      // Send back the same content as our response
      Object content = relay.getQuery();

      if (reply_relay != null) {
         if (!relay.getSource().equals(reply_relay.getTarget())) {
            getBlackboardService().publishRemove(reply_relay);
            reply_relay = new SimpleRelaySource(uids.nextUID(), agentId, relay.getSource(), content);
         }
      } else {
         reply_relay = new SimpleRelaySource(uids.nextUID(), agentId, relay.getSource(), content);
      }

      if (verbose && log.isShoutEnabled()) {
         log.shout("Responding to ping " + content + " from " + relay.getSource());
      }
      // updating content
      reply_relay.setQuery(content);
      blackboard.publishChange(reply_relay);
   }

   public boolean isABA(SimpleRelay relay) {
      MessageAddress target = relay.getTarget();
      return target instanceof AttributeBasedAddress;
   }
}

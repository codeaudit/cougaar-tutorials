/** 
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

package org.cougaar.pizza.plugin;

import java.util.Collection;
import java.util.Iterator;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.plugin.util.PizzaPreferenceHelper;
import org.cougaar.pizza.relay.RSVPRelayTarget;
import org.cougaar.pizza.relay.RSVPReply;
import org.cougaar.planning.ldm.asset.Entity;
import org.cougaar.util.UnaryPredicate;

/**
 * Reply to the RSVP relay invitation with the kind of pizza we like.
 * <p/>
 * Makes a new RSVPReply and sets the name of the guest (this agent) and their
 * pizza preference, based on the PGs on the self Entity. Put the RSVPReply on
 * the relay and sends the response back.
 */
public class RSVPPlugin
      extends ComponentPlugin {

   private LoggingService log;

   private IncrementalSubscription relaySubscription;

   /**
    * Set up the services we need - logging service
    */
   @Override
   public void load() {
      super.load();

      // get services
      log = getServiceBroker().getService(this, LoggingService.class, null);

      if (log.isDebugEnabled()) {
         log.debug("plugin loaded, services found");
      }
   }

   /**
    * We have one subscription, to the relays (the invitation) we expect to get.
    */
   @Override
   protected void setupSubscriptions() {
      if (log.isDebugEnabled()) {
         log.debug("setupSubscriptions");
      }

      // create relay subscription
      // Note that blackboard is inherited from the BlackboardClientComponent
      // base class
      // (ComponentPlugin extends BlackboardClientComponent.)
      relaySubscription = (IncrementalSubscription) blackboard.subscribe(new InvitePred());
   }

   /**
    * Watch for incoming RSVPRelays. Then search our local Entity's topping PGs,
    * and set that preference on a response to the relay.
    */
   @Override
   protected void execute() {
      if (log.isDebugEnabled()) {
         log.debug("execute");
      }

      // observe added relays
      for (Iterator iter = relaySubscription.getAddedCollection().iterator(); iter.hasNext();) {
         RSVPRelayTarget relay = (RSVPRelayTarget) iter.next();

         if (log.isInfoEnabled()) {
            log.info("Saw added " + relay);
         }

         // check for expected invitation relay
         if (Constants.INVITATION_QUERY.equals(relay.getQuery())) {
            // get the self entity
            Entity entity = getSelfEntity();
            if (entity == null) {
               // This is problematic.
               if (log.isWarnEnabled()) {
                  log.warn("Couldn't find self Entity!");
               }
            }

            // determine if I like meat or veggie pizza using the
            // PizzaPreferenceHelper, which looks at the Entity object's role
            String preference = PizzaPreferenceHelper.getPizzaPreference(log, entity);

            // send back reply
            RSVPReply reply = new RSVPReply(getAgentIdentifier().toString(), preference);
            relay.setResponse(reply);

            if (log.isInfoEnabled()) {
               log.info("Replying: " + relay);
            }

            blackboard.publishChange(relay);
         } else {
            if (log.isInfoEnabled()) {
               log.info("Ignoring non-invite relay " + relay);
            }
         }
      }

      // Print the removed relays for debugging
      if (log.isDebugEnabled()) {
         // removed relays
         for (Iterator iter = relaySubscription.getRemovedCollection().iterator(); iter.hasNext();) {
            Object sr = iter.next();
            log.debug("observed removed " + sr);
         }
      }
   }

   /**
    * Do a blackboard query to get the self Entity. Looks for entities that have
    * the same item id as my agent id.
    * 
    * @return self Entity, null if not found
    */
   protected Entity getSelfEntity() {
      // get the self entity
      Collection entities = blackboard.query(new UnaryPredicate() {
         private static final long serialVersionUID = 1L;

         public boolean execute(Object o) {
            if (o instanceof Entity) {
               return ((Entity) o).isLocal();
            } else {
               return false;
            }
         }
      });

      // there should be only one self entity
      if (!entities.isEmpty()) {
         return (Entity) entities.iterator().next();
      } else {
         return null;
      }
   }

   /**
    * My subscription predicate, which matches RSVPRelayTargets
    */
   private class InvitePred
         implements UnaryPredicate {
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
         return (o instanceof RSVPRelayTarget);
      }
   }
} // end of RSVPPlugin

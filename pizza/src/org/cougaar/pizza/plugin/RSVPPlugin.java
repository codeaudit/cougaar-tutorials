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

package org.cougaar.pizza.plugin;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.service.LoggingService;
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.relay.RSVPRelayTarget;
import org.cougaar.pizza.relay.RSVPReply;
import org.cougaar.planning.ldm.asset.Entity;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Iterator;

/**
 * Simple reply to the invitation.
 * <p/>
 * Makes a new RSVPReply (defined in InvitePlugin) and sets the name of
 * guest and their preference. Set the relay reply to the RSVPReply.
 */
public class RSVPPlugin extends ComponentPlugin {

  private LoggingService log;

  private IncrementalSubscription relaySubscription;

  /**
   * Set up the services we need - logging service
   */
  public void load() {
    super.load();

    // get services
    ServiceBroker sb = getServiceBroker();
    log = (LoggingService)
        sb.getService(this, LoggingService.class, null);

    // prefix all logging calls with our agent name
    log = LoggingServiceWithPrefix.add(log, getAgentIdentifier() + ": ");

    if (log.isDebugEnabled()) {
      log.debug("plugin loaded, services found");
    }
  }

  /**
   * We have one subscription, to the relays (the invitation) we expect to
   * get.
   */
  protected void setupSubscriptions() {
    if (log.isDebugEnabled()) {
      log.debug("setupSubscriptions");
    }

    // create relay subscription
    // Note that blackboard is inherited from the BlackboardClientComponent base class
    // (ComponentPlugin extends BlackboardClientComponent.)
    relaySubscription =
        (IncrementalSubscription) blackboard.subscribe(new InvitePred());
  }

  protected void execute() {
    if (log.isInfoEnabled()) {
      log.info("execute");
    }

    // observe added relays
    for (Iterator iter = relaySubscription.getAddedCollection().iterator(); iter.hasNext();) {
      RSVPRelayTarget relay = (RSVPRelayTarget) iter.next();

      if (log.isInfoEnabled()) {
	log.info(" - observe added " + relay);
      }

      // check for expected invitation relay
      if (Constants.INVITATION_QUERY.equals(relay.getQuery())) {
	// get the self entity
	Entity entity = getSelfEntity();

        // determine if I like meat or veggie pizza using the
        // PizzaPreferenceHelper, which looks at the Entity object's role
        PizzaPreferenceHelper prefHelper = new PizzaPreferenceHelper();

	String preference = prefHelper.getPizzaPreference(log, entity);

        // send back reply
        RSVPReply reply = 
	  new RSVPReply(getAgentIdentifier().toString(), preference);
        relay.setResponse(reply);

	if (log.isInfoEnabled()) {
	  log.info(" - Reply : " + relay);
	}

        blackboard.publishChange(relay);
      } else {
        log.info("ignoring relay " + relay);
      }
    }

    if (log.isDebugEnabled()) {
      // removed relays
      for (Iterator iter = relaySubscription.getRemovedCollection().iterator(); 
	   iter.hasNext();) {
        Object sr = iter.next();
        log.debug(" - observe removed " + sr);
      }
    }
  }

  /**
   * Does a blackboard query to get the self entity.
   *
   * Looks for entities that have the same item id as my agent id.
   */
  protected Entity getSelfEntity () {
    // get the self entity
    Collection entities = blackboard.query (new UnaryPredicate() {
	public boolean execute(Object o) {
	  if (o instanceof Entity) {
	    Entity entity = (Entity) o;
	    String entityItemId = 
	      entity.getItemIdentificationPG().getItemIdentification();
	    return (entityItemId.equals(getAgentIdentifier().toString()));
	  }
	  else {
	    return false;
	  }
	}
      });

    // there should be only one self entity
    if (!entities.isEmpty()) {
      return (Entity) entities.iterator().next();
    }
    else {
      return null;
    }
  }

  /**
   * My subscription predicate, which matches RSVPRelayTargets
   */
  private class InvitePred implements UnaryPredicate {
    public boolean execute(Object o) {
      return (o instanceof RSVPRelayTarget); 
    }
  }
}

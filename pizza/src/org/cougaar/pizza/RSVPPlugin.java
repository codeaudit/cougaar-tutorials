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

package org.cougaar.pizza;

import java.util.Enumeration;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;
import org.cougaar.util.UnaryPredicate;

/**
 * Simple reply to the invitation.
 * 
 * Makes a new RSVPReply (defined in InvitePlugin) and sets the name of guest and their preference.
 * Set the relay reply to the RSVPReply.
 */
public class RSVPPlugin extends ComponentPlugin {

  private LoggingService log;
  private UIDService uids;

  private IncrementalSubscription sub;

  public void load() {
    super.load();

    // get services
    ServiceBroker sb = getServiceBroker();
    log = (LoggingService)
      sb.getService(this, LoggingService.class, null);

    // prefix all logging calls with our agent name
    log = LoggingServiceWithPrefix.add(log, agentId+": ");

    if (log.isDebugEnabled()) {
      log.debug("loaded");
    }
  }

  protected void setupSubscriptions() {
    if (log.isDebugEnabled()) {
      log.debug("setupSubscriptions");
    }

    // create relay subscription
    sub = (IncrementalSubscription) blackboard.subscribe(new InvitePred());
  }

  protected void execute() {
    log.info("execute");

    if (!sub.hasChanged()) {
      // usually never happens, since the only reason to execute
      // is a subscription change
      return;
    }

    // observe added relays
    for (Enumeration en = sub.getAddedList(); en.hasMoreElements();) {
      RSVPRelayTarget relay = (RSVPRelayTarget) en.nextElement();

      log.info (" - observe added "+relay);

      if (Constants.INVITATION_QUERY.equals(relay.getQuery())) {
        // send back reply
	PizzaPreferenceHelper prefHelper = new PizzaPreferenceHelper();
	String preference = (prefHelper.iLikeMeat(log, blackboard)) ? "meat" : "veg";

	RSVPReply reply = new RSVPReply (agentId.toString(), preference); 

	((RSVPRelayTarget) relay).setResponse(reply);

	log.info(" - Reply : " + relay);

        blackboard.publishChange(relay);
      } else {
        log.info ("ignoring relay " + relay);
      }
    }

    if (log.isDebugEnabled()) {
      // removed relays
      for (Enumeration en = sub.getRemovedList(); en.hasMoreElements();) {
        Object sr = en.nextElement();
        log.debug(" - observe removed "+sr);
      }
    }
  }

  /**
   * My subscription predicate, which matches SimpleRelays
   */ 
  private class InvitePred implements UnaryPredicate {
    public boolean execute(Object o) {
      if (o instanceof RSVPRelayTarget) {
	return true;
      }
      else if (o instanceof Relay) {
	log.info ("\nIgnoring : " + o + 
		  "\nclass    : " + o.getClass());
      }
      return false;
    }
  }
}

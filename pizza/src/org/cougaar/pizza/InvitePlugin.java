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

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.relay.SimpleRelay;
import org.cougaar.core.relay.SimpleRelayImpl;
import org.cougaar.core.util.UID;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.util.UnaryPredicate;

/**
 * Sends a simple relay invitation to all "FriendsOfMark".
 *
 * As replies come back, updates the PizzaPreferences object on the blackboard.
 */
public class InvitePlugin extends ComponentPlugin {
  private LoggingService log;

  /** the uid service gives me the uid for the relay with a unique id */
  private UIDService uids;

  /** my subscription to relays */
  private IncrementalSubscription sub;

  /** my list of pizza preferences generated from RSVPs */
  protected PizzaPreferences pizzaPreferences;

  /** the query to my friends */
  public static final String INVITATION_QUERY = "invitation-meat_or_veg";

  /**
   * Set up the services we need - logging and the uid service
   */
  public void load() {
    super.load();

    // get services
    ServiceBroker sb = getServiceBroker();
    log = (LoggingService)
      sb.getService(this, LoggingService.class, null);
    uids = (UIDService)
      sb.getService(this, UIDService.class, null);

    // prefix all logging calls with our agent name
    log = LoggingServiceWithPrefix.add(log, agentId+": ");

    if (log.isDebugEnabled()) {
      log.debug("loaded");
    }
  }

  /**
   * We have one subscription, to the relays (the invitation) we produce.
   *
   * Here we also publish the pizza preferences, although they are empty initially.
   */
  protected void setupSubscriptions() {
    if (log.isDebugEnabled()) {
      log.debug("setupSubscriptions");
    }

    // create relay subscription
    sub = (IncrementalSubscription) blackboard.subscribe(new MyPred());

    // Create recipient addresses 
    MessageAddress source = getAgentIdentifier(); 
    MessageAddress target = new MyABA("FriendsOfMark-COMM", "Role", "Member"); 

    // give bob a chance to show up

    // BOZO - perhaps we should reconsider retries here?

    log.shout ("about to sleep");
    try { Thread.sleep (10000); } catch (Exception e) {}
    log.shout ("woke up...");

    UID uid = uids.nextUID();
    SimpleRelay simpleRelay = 
      new SimpleRelayImpl(uid, agentId, target, INVITATION_QUERY);
    if (log.isShoutEnabled()) {
      log.shout(getAgentIdentifier () + " - Sending "+simpleRelay);
    }
    blackboard.publishAdd(simpleRelay);

    // publish pizza preferences - change as RSVPs arrive

    pizzaPreferences = new PizzaPreferences ();
    pizzaPreferences.setUID (uids.nextUID());
    blackboard.publishAdd(pizzaPreferences);
  }

  /**
   * When a relay changes, this method gets called
   */
  protected void execute() {
    if (log.isDebugEnabled()) {
      log.debug("execute");
    }

    if (!sub.hasChanged()) {
      // usually never happens, since the only reason to execute
      // is a subscription change
      return;
    }

    // observe changed relays
    for (Enumeration en = sub.getChangedList(); en.hasMoreElements();) {
      SimpleRelay sr = (SimpleRelay) en.nextElement();
      if (log.isDebugEnabled()) {
        log.debug("observe changed "+sr);
      }
      if (agentId.equals(sr.getSource())) {
        // got back answer
        if (log.isShoutEnabled()) {
          log.shout("Received "+sr);
        } 

	RSVPReply rsvpReply = (RSVPReply) sr.getReply();
	pizzaPreferences.addFriendToPizza (rsvpReply.friend, rsvpReply.pizzaPreference);

	log.warn ("pizza prefs now : " + pizzaPreferences);

        // remove query both locally and at the remote target.
        //
        // this is optional, but it's a good idea to clean up and
        // free some memory.
        blackboard.publishRemove(sr); 

	// update pizza preferences now that a new RSVP arrived
	blackboard.publishChange (pizzaPreferences);
      } else {
        // ignore relays from other sources...
	log.warn ("********************* ignoring " + sr);
	log.warn ("********************* ignoring " + sr);
	log.warn ("********************* ignoring " + sr);
      }
    }

    if (log.isDebugEnabled()) {
      // removed relays
      for (Enumeration en = sub.getRemovedList(); en.hasMoreElements();) {
        Relay.Source sr = (Relay.Source) en.nextElement();
        log.debug("observe removed "+sr);
      }
    }
  }

  /**
   * My subscription predicate, which matches SimpleRelays
   */ 
  private class MyPred implements UnaryPredicate {
    public boolean execute(Object o) {
      if (o instanceof SimpleRelay) {
	return true;
      }
      else {
	if (log.isDebugEnabled()) {
	  log.debug ("\nignored : " + o +
		     "\nclass   : " + o.getClass());
	}
      }
      return false;
    }
  }

  /** required since ABA doesn't have public setter methods */
  private static class MyABA extends AttributeBasedAddress {
    public MyABA(String commName, String attrType, String attrValue) {
      this.myCommunityName = commName;
      this.myAttributeType = attrType;
      this.myAttributeValue = attrValue;
    }
  }

  /** the RSVP reply - just the friend and their pizza preference */
  public static class RSVPReply {
    public String friend;
    public String pizzaPreference;

    public RSVPReply (String friend, String pizzaPreference) {
      this.friend = friend;
      this.pizzaPreference = pizzaPreference;
    }

    public String toString () { return "RSVPReply : " + friend + " - " + pizzaPreference; }
  }
}

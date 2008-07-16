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

import java.util.Iterator;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.relay.SimpleRelay;
import org.cougaar.core.relay.SimpleRelaySource;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.Arguments;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.core.service.UIDService;
/**
 * This plugin is an example ping target that receives relays and sends
 * back a reply.
 * <p>
 * A "verbose=<i>boolean</i>" plugin parameter and System property is
 * supported, exactly as documented in {@link PingSender}.
 * <p>
 * There must be one instance of this plugin in every agent that will
 * receive {@link PingSender} relays.  For simplicity, it's easiest to load
 * a copy of this plugin into every agent.
 *
 * @property org.cougaar.demo.community.PingReceiver.verbose=true
 *   PingReceiver should output SHOUT-level logging messages, if not set as
 *   a plugin parameter.
 *
 * @see PingSender Remote plugin that sends the ping relays to this plugin
 *
 * @see PingServlet Optional browser-based GUI.
 */
public class PingReceiver extends ComponentPlugin {

  private LoggingService log;
  private UIDService 	uids;
  
  private boolean verbose;

  private IncrementalSubscription sub;
  private SimpleRelay reply_relay;

  private static final boolean DEFAULT_VERBOSE =
    SystemProperties.getBoolean(
        "org.cougaar.demo.community.PingReceiver.verbose", true);
		
  /** This method is called when the agent is constructed. */
  public void setArguments(Arguments args) {
    verbose = args.getBoolean("verbose", DEFAULT_VERBOSE);
  }

  /** This method is called when the agent loads. */
  public void load() {
    super.load();

    // Get our required Cougaar services
    log 	= getServiceBroker().getService(this, LoggingService.class, null);
	uids 	= getServiceBroker().getService(this, UIDService.class, null);
  }

  /** This method is called when the agent starts. */
  protected void setupSubscriptions() {
    // Subscribe to all relays sent to our agent
    sub = (IncrementalSubscription) blackboard.subscribe(createPredicate());

    // When a relay arrives on our blackboard, our "execute()" method
    // will be called.
  }

  /** This method is called whenever a subscription changes. */
  protected void execute() {
	log.debug("well, let's execute");
    // Observe added relays by looking at our subscription's add list
    for (Iterator iter = sub.getAddedCollection().iterator(); iter.hasNext(); ) {
	  log.debug("looping through addedCollection");
      SimpleRelay relay = (SimpleRelay) iter.next();
      replyTo(relay);
    }
    for (Iterator iter = sub.getChangedCollection().iterator(); iter.hasNext(); ) {
	  log.debug("looping through changedCollection");
      SimpleRelay relay = (SimpleRelay) iter.next();
      replyTo(relay);
    }
  }

  /** Create our subscription filter */
  private UnaryPredicate createPredicate() {
    // Matches any relay sent to our agent
    return new UnaryPredicate() {
      public boolean execute(Object o) {
		if (o instanceof SimpleRelay) {
			MessageAddress target = ((SimpleRelay)o).getTarget();
			if (target instanceof AttributeBasedAddress ) return true;
		}
		return false;
      }
    };
  }

  private void replyTo(SimpleRelay relay) {
	log.debug("well, let's reply, I'm polite!");
    // Send back the same content as our response
    Object content = relay.getQuery();

	if (reply_relay != null) {
	  if (!relay.getSource().equals(reply_relay.getTarget())) {
		getBlackboardService().publishRemove(reply_relay);
		reply_relay = new SimpleRelaySource(
			uids.nextUID(), agentId, relay.getSource(), content);
	  }
    } else {
	  reply_relay = new SimpleRelaySource(
			uids.nextUID(), agentId, relay.getSource(), content);	
	}
	
    if (verbose && log.isShoutEnabled()) {
      log.shout("Responding to ping "+content+" from "+relay.getSource());
    }
    // updating content 
    reply_relay.setQuery(content);
    blackboard.publishChange(reply_relay);
  }
}

/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

package org.cougaar.pizza.relay;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.util.SimpleUniqueObject;

import org.cougaar.pizza.Constants;
import org.cougaar.pizza.plugin.PizzaPreferences;

/**
 * A source-side  {@link Relay}.
 * <p>
 */
public class RSVPRelaySource
  extends SimpleUniqueObject
  implements Relay.Source, NotPersistable
{
  private LoggingService log;
  Set targets = new HashSet();
  MessageAddress target;
  Object query; 
  PizzaPreferences pizzaPreferences;

  long then = System.currentTimeMillis ();

  public RSVPRelaySource (LoggingService log, 
			  MessageAddress target, 
			  Object query, 
			  PizzaPreferences pizzaPreferences) {
    this.log    = log;
    this.target = target;
    this.query  = query;
    this.pizzaPreferences = pizzaPreferences;
    targets.add (target);
  }

  /** 
   * Relay.Source implementation 
   *
   * @return set of target (just one member - the AttributeBasedAddress)
   */
  public Set getTargets() {
    return targets;
  }

  /** 
   * Relay.Source implementation 
   *
   * @return the query in the RSVP - meat or veg?
   */
  public Object getContent() {
    return query;
  }

  /** 
   * Relay.Source implementation 
   *
   * Make sure on the target side we create a target relay. 
   * This ensures that the target agent gets a target relay.  The
   * target relay has just a source, and no target address, so the 
   * the relay won't be propagated at the target.
   * 
   * @return target factory that makes a target that has no target address
   */
  public TargetFactory getTargetFactory() {
    return RSVPRelayFactory.getTargetFactory();
  }

  /**
   * Relay.Source implementation 
   *
   * Record responses from remote agents as they come in.
   *
   * Note that we assume the response will be a RSVPReply.
   *
   * Synchronized because - why???
   *
   * @return Relay.RESPONSE_CHANGE - since every time we get a response, we want to examine it
   */
  public int updateResponse(MessageAddress target, Object response) {
    //    synchronized (pizzaPreferences) {
      RSVPReply rsvpReply = (RSVPReply) response;
      pizzaPreferences.addFriendToPizza (rsvpReply.friend, 
					 rsvpReply.pizzaPreference);

      log.info ("RSVPRelaySource - pizza prefs now : " + pizzaPreferences);

      if (pizzaPreferences.getNumMeat () == Constants.EXPECTED_NUM_FRIENDS) {
	log.info ("Waited " + ((System.currentTimeMillis ()-then)/1000) + 
		  " seconds to get responses back from " + Constants.EXPECTED_NUM_FRIENDS + 
		  " friends (including party planner).");
      }

      return Relay.RESPONSE_CHANGE;
      //    }
  }

  public String toString () { 
    return "RSVPRelaySource : " + 
      "\nquery       : " + getContent() + 
      "\ntarget      : " + target +
      "\npreferences : " + pizzaPreferences;
  }
}

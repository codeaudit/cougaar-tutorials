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

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;

/**
 * A target-side {@link Relay}. It has a slot for the query (from the sender)
 * and the local response.
 * <p/>
 * The target relay has just a source address, and no target address, so that
 * the relay won't be (re)propagated at the target agent.
 * <p/>
 * An alternative would be to have the relay implement both source and target
 * interfaces, but this could lead to endless pinging in this case where the
 * target address is an ABA broadcast to all members of the community (as in
 * this pizza app).
 * <p/>
 * In contrast, {@link org.cougaar.core.relay.SimpleRelayImpl} which implements
 * both source and target.
 */
public class RSVPRelayTarget
      implements Relay.Target {
   private static final long serialVersionUID = 1L;
   private final MessageAddress source; // Who do we respond to?
   private final Object query; // What info did they send to us?
   private Object response; // Our response, an RSVPReply for the pizza app
   private final UID uid;

   public RSVPRelayTarget(UID uid, MessageAddress source, Object query) {
      this.uid = uid;
      this.source = source;
      this.query = query;
   }

   /**
    * Get Relay sender.
    * 
    * Relay.Target implementation.
    * 
    * @return source (sender or inviter) address
    */
   public MessageAddress getSource() {
      return source;
   }

   /**
    * Specify the reply to the invite.
    * 
    * @param response - reply to relay (should be an RSVPReply object)
    */
   public void setResponse(Object response) {
      this.response = response;
   }

   /**
    * Used by the RelayLP to get the response to send back the the Relay sender.
    * Relay.Target implementation
    * 
    * @return reply to relay
    */
   public Object getResponse() {
      return response;
   }

   /**
    * This is important so that if there are multiple relays, the RSVP plugin
    * can know which to examine.
    * 
    * @return query - the type of invitation
    */
   public Object getQuery() {
      return query;
   }

   /**
    * Used by the RelayLP to update the query from the Relay sender. Not used in
    * our application. Relay.Target implementation
    * 
    * @return Relay.NO_CHANGE - content doesn't need to be updated, only the
    *         response
    */
   public int updateContent(Object newContent, Token token) {
      // Content is never updated in the pizza app
      // Another application could have Alice change details about the party,
      // so send new relay content out. This method would be
      // used by the RelayLP to propogate that change out to her invitees.
      return Relay.NO_CHANGE;
   }

   /**
    * Implemented for UniqueObject interface.
    * 
    * @return the UID of this UniqueObject.
    **/
   public UID getUID() {
      return uid;
   }

   /**
    * Implemented for UniqueObject interface. Does nothing - not allowed to
    * reset UID.
    **/
   public void setUID(UID uid) {
   }

   @Override
   public String toString() {
      return "RSVPRelayTarget : " + " source=" + getSource() + " query=" + getQuery() + " response=" + getResponse();
   }
}

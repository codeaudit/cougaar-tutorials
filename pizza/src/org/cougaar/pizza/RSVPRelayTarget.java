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

package org.cougaar.pizza;

import java.util.Set;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.SimpleUniqueObject;

/**
 * A target-side relay {@link Relay}.
 * <p>
 */
public class RSVPRelayTarget
  extends SimpleUniqueObject
  implements Relay.Target, NotPersistable
{
  transient MessageAddress sourceAddress;
  transient Object response;
  transient Object query; 
  static int numMade = 0;
  int num = 0;
  static Object lock = new Object();

  public RSVPRelayTarget(MessageAddress source, Object query) {
    this.sourceAddress = source;
    this.query = query;
    synchronized (lock) {
      numMade++;
      num = numMade;
    }
  }

  /** 
   * Relay.Target implementation 
   *
   * @return source (sender or inviter) address
   */
  public MessageAddress getSource() {
    return sourceAddress;
  }

  /** 
   * @param response - reply to relay
   */
  public void setResponse (Object response) { 
    this.response = response; 
  }
  
  /** 
   * Relay.Target implementation 
   *
   * @return reply to relay
   */
  public Object getResponse() {
    return response;
  }

  /** 
   * This is important so that if there are multiple relays, the
   * RSVP plugin can know which to examine.
   *
   * @return query - the type of invitation
   */
  public Object getQuery () {
    return query;
  }

  /** 
   * Relay.Target implementation 
   *
   * @return Relay.NO_CHANGE - content doesn't need to be updated, only the response
   */
  public int updateContent(Object newContent, Token token) {
    return Relay.NO_CHANGE;     // Content is never updated
  }

  public String toString () { 
    return 
      "RSVPRelayTarget : " + 
      "\nsource   : " + getSource() + 
      "\nquery    : " + getQuery() + 
      "\nresponse : " + getResponse();
  }
}


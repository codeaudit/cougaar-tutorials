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
import org.cougaar.pizza.Constants;

import java.io.Serializable;

/**
 * A {@link org.cougaar.core.relay.Relay.TargetFactory} for {@link RSVPRelayTarget}s.
 * <p/>
 * This factory is required in order to produce the correct Relay.Target
 * when the Relay has been received by the target Agent.
 */
public class RSVPTargetRelayFactory
    implements Relay.TargetFactory, Serializable {

  // Typically you only need one instance of your TargteFactory.
  private final static Relay.TargetFactory SINGLETON_FACTORY = new RSVPTargetRelayFactory();

  /** Get s singleton instance of the TargetFactory */
  public static synchronized Relay.TargetFactory getTargetFactory() {
    return SINGLETON_FACTORY;
  }

  /**
   * Whenever you have a factory you should implement the readResolve method on
   * an object that implements Serializable.
   *
   * The reason is to prevent proliferation of new copies of 
   * the factory every time the relay gets sent. (The relay has a reference
   * to this factory.) 
   *
   * GC would eventually remove those extra copies, but having a readResolve method wastes less time.
   */
  private Object readResolve () {
    return SINGLETON_FACTORY;
  }

  /**
   * TargetFactory implementation - create an RSVPRelayTarget.
   * Note that the RSVPRelay doesn't use the token slot, though some implementations might.
   */
  public Relay.Target create(UID uid, MessageAddress source, Object content,
                             Relay.Token token) {
    // Note that in our case the content is always Constants.INVITATION_QUERY
    RSVPRelayTarget result = new RSVPRelayTarget(uid,
						 source,
                                                 content);
    return result;
  }

}

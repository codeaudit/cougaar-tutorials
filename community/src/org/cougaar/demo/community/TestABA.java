package org.cougaar.demo.community;

/*
 * <copyright>
 *  
 *  Copyright 1997-2007 BBNT Solutions, LLC
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

/**
 * This class is copied from package org.cougaar.core.agent.service.community
 * with just a change to its package. 
 * It has been copied in this demo to include all examples in one place.
 */

import org.cougaar.core.plugin.AnnotatedSubscriptionsPlugin;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.relay.SimpleRelay;
import org.cougaar.core.relay.SimpleRelaySource;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.util.annotations.Cougaar.ObtainService;
import org.cougaar.util.annotations.Cougaar.Arg;
import org.cougaar.util.annotations.Cougaar.Execute;
import org.cougaar.util.annotations.Subscribe;

/**
 * This plugin sends a simple ABA Relay to a target community.
 */
public class TestABA
      extends AnnotatedSubscriptionsPlugin {


   @ObtainService
   public LoggingService log;
   
   @ObtainService
   public UIDService uids;
   
   @Arg(name="target")
   public String community;

   @Execute(on=Subscribe.ModType.ADD)
   public void handleAdd(Relay relay) {
      log.info("Observed add");
   }
   
   @Execute(on=Subscribe.ModType.CHANGE)
   public void handleChaneg(Relay relay) {
      log.info("Observed add");
   }

   @Execute(on=Subscribe.ModType.ADD)
   public void handlRemove(Relay relay) {
      log.info("Observed remove");
   }


   @Override
   protected void setupSubscriptions() {
      super.setupSubscriptions();
      if (community != null) {
         if (log.isInfoEnabled()) {
            log.info("Sending ABA Relay to " + community);
         }
         SimpleRelay sr =
               new SimpleRelaySource(uids.nextUID(),
                                     agentId,
                                     AttributeBasedAddress.getAttributeBasedAddress(community, "Role", "Member"),
                                     "(Test from \"" + agentId + "\" to \"" + community + "\")");
         blackboard.publishAdd(sr);
      }
   }
}

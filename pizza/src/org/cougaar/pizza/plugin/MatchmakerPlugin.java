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

import org.cougaar.pizza.Constants;
import org.cougaar.servicediscovery.plugin.SimpleMatchmakerPlugin;

/**
 * The Matchmaker is responsible for taking service discovery requests
 * (MMQueryRequests) from the {@link SDClientPlugin}, and issuing asynchronous
 * queries to the YP to find matching providers. When one (or more) is found,
 * send the scored results back the SDClient on the MMQueryRequest.
 * <p>
 * This version extends the SimpleMatchmakerPlugin, specifying that the Role
 * requested will be in the pizza constants
 * {@link org.cougaar.pizza.Constants.UDDIConstants#COMMERCIAL_SERVICE_SCHEME}.
 * As noted in the base class, it allows the YP to handle walking up YP
 * communities as necessary. It does not handle quiescence, is not guaranteed to
 * work with kills/restarts (persistence), only works with a distributed YP
 * (using communities, not a single static instance), etc.
 * 
 * @property 
 *           org.cougaar.servicediscovery.plugin.SimpleMatchmakerQueryGracePeriod
 *           (in minutes, default is 2) specifies how long to wait before YP
 *           query errors should be logged at ERROR instead of DEBUG.
 */
public class MatchmakerPlugin
      extends SimpleMatchmakerPlugin {
   /**
    * Return the UDDI Service Scheme that contains the Roles we will look for.
    * In this case, that is the
    * {@link org.cougaar.pizza.Constants.UDDIConstants#COMMERCIAL_SERVICE_SCHEME}
    * .
    * <p>
    * This method is the only one we need to over-ride.
    * 
    * @return UDDI Service Scheme to find Roles in
    */
   @Override
   protected String getServiceSchemeForRoles() {
      return Constants.UDDIConstants.COMMERCIAL_SERVICE_SCHEME;
   }
}

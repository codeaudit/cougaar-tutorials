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

import java.io.File;
import java.net.URL;

import org.cougaar.pizza.Constants;
import org.cougaar.servicediscovery.plugin.SimpleSDRegistrationPlugin;

/**
 * Extension of the SimpleSDRegistrationPlugin to use the pizza serviceprofiles.
 * Registers this agent's services as specified in
 * the <agent name>-profile.owl file if any in the plugin parameter-named YP agent.
 * <p>
 * Remember that this version of the SDRegistration plugin is somewhat simplified 
 * -- fewer error checks for example.
 *<p>
 * Note that the only change this extension has to make, is to point the plugin
 * at the correct directory for the service profiles for this application.
 * <p>
 * First plugin argument is the name of the agent hosting the YP that we will register with. 
 *
 * @property org.cougaar.servicediscovery.plugin.SimpleRegistrationGracePeriod is the 
 * number of minutes after startup, during which we ignore SD registration Warnings, 
 * to allow the YP to start up. After this we complain more loudly. Default is 5 minutes.
 **/
public class SDRegistrationPlugin extends SimpleSDRegistrationPlugin {
  /** 
   * Get the URL for the service profiles directory for this application. 
   * This is the only method we need to over-ride to customize the base class for the
   * pizza application. 
   *<p>
   * Note that we use the method in {@link Constants} to find the directory contains the service profiles.
   */
  protected URL getServiceProfileURL() {
    try {
      return new URL(Constants.getDataPath() + File.separator + "serviceprofiles" + File.separator);
    } catch (java.net.MalformedURLException mue) {
      log.error("Exception constructing service profile URL" , mue);
      return null;
    }
  }
}


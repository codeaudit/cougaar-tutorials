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
package org.cougaar.demo.hello;

import org.cougaar.core.plugin.AnnotatedSubscriptionsPlugin;
import org.cougaar.util.annotations.Cougaar;

/** HelloPublisher add a Hello object to the blackboard */
public class HelloPublishAddPlugin
      extends AnnotatedSubscriptionsPlugin {

   /**
    * Arguments can be passed into a plugin from the Society XML definition
    * file. The parameter value is converted from a String to the local field
    * type and can have a default value and description text
    */
   @Cougaar.Arg(defaultValue = "Hello", description = "Message to be published on blackboard")
   public String message;

   /** A local field to hold onto the blackboard object that we are publishing */
   private HelloObject hello;

   /**
    * Execute can be used to publish initial blackboard items. Blackboard object
    * can only be manipulated inside a blackboard transaction. Execute is
    * wrapped in a blackboard transaction. Execute runs once during plugin
    * startup and every time the plugin's subscription fire. So care must be
    * taken to dispatch the desired Execute code must check conditions for when
    * to run its code.
    */
   @Override
   public void execute() {
      super.execute();
      // Test for initial run of execute
      if (hello == null) {
         hello = new HelloObject(uids.nextUID(), message);
         blackboard.publishAdd(hello);
      } else {
         log.shout("This should not happen, because this plugin does not subscribe");
      }
   }
}

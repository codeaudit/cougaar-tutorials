/*
 * <copyright>
 *  
 *  Copyright 1997-2012 BBNT Solutions, LLC
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

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.plugin.TodoPlugin;
import org.cougaar.util.annotations.Cougaar;

/** This plugin call the HelloService periodically, forever */
public class HelloServiceClientPlugin
      extends TodoPlugin {

   @Cougaar.Arg(defaultValue = "1000", description = "The millisecond period to publish a new hello message")
   public int periodMillis;

   @Cougaar.Arg(defaultValue = "Hello", description = "Message to be published on blackboard")
   public String message;

   @Cougaar.ObtainService
   public HelloService helloService;

   @SuppressWarnings("unused")
   private Alarm alarm;
   private int count = 0;

   /** log Logging service initialized by parent ParameterizedPlugin */
   /** uids UID service initialized by parent ParameterizedPlugin */

   @Override
   public void start() {
      super.start();
      log.shout("start");
      if (helloService == null) {
         log.shout("no luck getting hello service");
      }
   }

   /*
    * Alarms should be created only after setupSubscriptions have been
    * initialized
    */
   @Override
   public void setupSubscriptions() {
      super.setupSubscriptions();
      log.shout("setupSubsciptions");
      alarm = executeLater(periodMillis, new CallServiceTask());
   }

   /*
    * Timer task calls the hello service , then restarts the timer.
    */
   private final class CallServiceTask
         implements Runnable {
      public void run() {
         log.shout("alarm fired");
         if (helloService != null) {
            helloService.changeMessage(message + "_" + count++);
         }
         alarm = executeLater(periodMillis, new CallServiceTask());
      }
   }
}

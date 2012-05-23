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
package org.cougaar.demo.node;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.plugin.TodoPlugin;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObjectBase;
import org.cougaar.util.annotations.Cougaar;

/**
 * This NodeAgent Plugin will shutdown the Node after a timeout period. The
 * shutdown can be delayed, if calls are made to the stillActive method of the
 * RegisterActivityService
 * */
public class InactiveShutdownPlugin
      extends TodoPlugin
      implements InactiveShutdownService {

   @Cougaar.Arg(name = "MaxInactiveTime", defaultValue = "1000", description = "The millisecond period in which activity must"
         + " be detected before node will be shut down")
   public int maxInactiveTime;
   private static final long TIME_GRANULARITY = 500;

   @Cougaar.ObtainService
   public NodeControlService nodeControlService;

   private Alarm terminateAlarm;
   private ActivityCounter countBbObject;
   private long lastTimeAlarmWasReset;

   /** log Logging service initialized by parent ParameterizedPlugin */
   /** uids UID service initialized by parent ParameterizedPlugin */

   /**
    * Blackboard Object that will be incremented each every time activity is
    * detected Activity count can be monitored by external plugins and the task
    * view servlet
    */
   @SuppressWarnings("serial")
   public final class ActivityCounter
         extends UniqueObjectBase {
      private long count;
      private long lastActivityTime;
      private String lastAgentName;
      private String lastPlugName;

      public ActivityCounter(UID uid) {
         super(uid);
         this.count = 0;
      }

      public long getInactiveTime() {
         return System.currentTimeMillis() - lastActivityTime;
      }

      public long increment(String agentName, String pluginName) {
         lastActivityTime = System.currentTimeMillis();
         lastAgentName = agentName;
         lastPlugName = pluginName;
         count++;
         return count;
      }

      public long getCount(String agentName, String pluginName) {
         return count;
      }

      public String getLastAgentName() {
         return lastAgentName;
      }

      public String getLastPlugName() {
         return lastPlugName;
      }

   }

   @Override
   public void load() {
      super.load();
      countBbObject = new ActivityCounter(uids.nextUID());
      publishAddLater(countBbObject);
      terminateAlarm = executeLater(maxInactiveTime, new TerminatorTask());
      provider = new MyServiceProvider();
      ServiceBroker rootsb = nodeControlService.getRootServiceBroker();
      rootsb.addService(InactiveShutdownService.class, provider);
   }

   @Override
   public void unload() {
      if (provider != null) {
         ServiceBroker rootsb = nodeControlService.getRootServiceBroker();
         rootsb.revokeService(InactiveShutdownService.class, provider);
      }
      super.unload();
   }

   /**
    * InactiveShutdownService implementation This is run by external threads, so
    * it should not touch the black board directly
    */
   public synchronized void stillActive(String agentName, String pluginName) {
      countBbObject.increment(agentName, pluginName);
      publishChangeLater(countBbObject);
      // Check if it is worth resetting the timer
      if (System.currentTimeMillis() - lastTimeAlarmWasReset > TIME_GRANULARITY) {
         lastTimeAlarmWasReset = System.currentTimeMillis();
         if (terminateAlarm != null) {
            terminateAlarm.cancel();
         }
         terminateAlarm = executeLater(maxInactiveTime, new TerminatorTask());
      }
   }

   /**
    * InactiveShutdownService Service Provider implementation
    */
   private ServiceProvider provider;

   private class MyServiceProvider
         implements ServiceProvider {
      public MyServiceProvider() {
      }

      public Object getService(ServiceBroker sb, Object req, Class<?> cl) {
         return InactiveShutdownPlugin.this;
      }

      public void releaseService(ServiceBroker arg0, Object arg1, Class<?> arg2, Object arg3) {
      }
   }

   /**
    * Shut down the node in the cleanest way possible. But make sure it is
    * shutdown.
    */
   private final class TerminatorTask
         implements Runnable {
      public void run() {
         try {
            if (nodeControlService != null) {
               log.shout("Shutting Down Node Cleanly");
               nodeControlService.shutdown();
            } else {
               log.shout("Crashing Node Now");
               try {
                  Thread.sleep(500);
               } catch (InterruptedException e) {
               }
               System.exit(0);
            }
         } catch (Exception e1) {
            log.shout("Error while attempting to shutdown node cleanly" + e1.getMessage());
            try {
               Thread.sleep(500);
            } catch (InterruptedException e2) {
            }
            System.exit(1);
         }
      }
   }

}

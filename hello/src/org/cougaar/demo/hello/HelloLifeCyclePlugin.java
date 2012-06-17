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

import org.cougaar.core.plugin.AnnotatedSubscriptionsPlugin;
import org.cougaar.demo.node.InactiveShutdownService;
import org.cougaar.util.annotations.Cougaar;

/** 
 * This plugin logs "Hello" at various agent life-cycle times 
 */
public class HelloLifeCyclePlugin
      extends AnnotatedSubscriptionsPlugin {

   @Cougaar.Arg(defaultValue = "Hello", description = "Message to be logged")
   public String message;

   // Watchdog service that detects inactivity in Agents, then shuts down the Node.
   @Cougaar.ObtainService(releaseOnUnload=false)
   public InactiveShutdownService inactiveShutdownService;
   
   private String agentName;
   private String pluginName;

   /*******
    * Plugin Life-Cycle Events
    */

   /**
    * Load method is called when the agent is created. 
    * Node-layer services are bound after super.load is run.
    * Agent services should be provided at load time.
    */
   @Override
   public void load() {
      super.load();
      agentName = getAgentIdentifier().getAddress();
      pluginName = getBlackboardClientName();
      log.shout(message + ": Load!" );
   }

   /**
    * Unload method is called whenever a plugin is about to be unloaded as part
    * of an agent move or clean shutdown. 
    * Agents plugins should release services.
    */
   @Override
   public void unload() {
      inactiveShutdownService = null;
      log.shout(message + ": Unload!" );
      super.unload();
   }

   /*******
    * Agent Service-hookup Life-Cycle Events
    */

   /** 
    * Start method allows binding to Agent-layer services (none in this example).
    */
   @Override
   public void start() {
      super.start();
      log.shout(message + ": Start!" );
   }

   /**
    * Stop method is called for a clean shutdown of the plugin 
    * All Agent-Layer services should be released (none in this example).
    */
   @Override
   public void stop() {
      super.stop();
      log.shout(message + ": Stop!" );
   }

   /** 
    * Halt method is called for an emergency shutdown of the plugin.
    */
   @Override
   public void halt() {
      super.halt();
      log.shout(message + ": Halt!" );
   }

   /********
    * Mobility Life-Cycle Events
    */

   /**
    * Suspend method is called before an agent moves. The plugin local state
    * should be moved to the blackboard and services and timers shutdown.
    */
   @Override
   public void suspend() {
      super.suspend();
      log.shout(message + ": Suspend!"  );
   }

   /**
    * Resume method is call after an agent has moved. The plugin local state
    * should be restored from the blackboard.
    */
   @Override
   public void resume() {
      super.resume();
      // retrieve plugin state from blackboard, agent restarted after move
      log.shout(message + ": Resume!" );
   }

   /******
    * Blackboard Life-Cycle Events
    */

   /**
    * SetupSubscriptions method is called when the agent starts. 
    * Subscribe to changes in blackboard objects
    */
   @Override
   protected void setupSubscriptions() {
      inactiveShutdownService.stillActive(agentName, pluginName);
      log.shout(message + ": Setup Subscriptions!" );
   }

   /**
    * Execute method is called whenever a subscription changes. Check
    * subscriptions for add-delete-modified blackboard objects. Note execute()
    * is run once at startup, even if nothing has changed on the blackboard.
    * This initial execution can be used to inject the initial blackboard objects
    * that trigger the agent processing.
    */
   @Override
   protected void execute() {
      log.shout(message + ": Execute!" );
      inactiveShutdownService.stillActive(agentName, pluginName);
   }
}

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

package org.cougaar.demo.community;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.agent.service.alarm.AlarmBase;
import org.cougaar.core.blackboard.TodoSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.AnnotatedSubscriptionsPlugin;
import org.cougaar.core.relay.SimpleRelay;
import org.cougaar.core.relay.SimpleRelaySource;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.util.annotations.Cougaar.ObtainService;
import org.cougaar.util.annotations.Cougaar.Arg;
import org.cougaar.util.annotations.Subscribe;
import org.cougaar.util.annotations.Cougaar.Execute;
import org.cougaar.util.annotations.Cougaar.Query;

/**
 * This plugin is an example ping source that sends relays to remote agents
 * based on a community name.
 * <p>
 * There can be multiple copies of this plugin in a single agent, but every
 * {@link PingSender} must have a unique community target name. The target is
 * specified as a plugin parameter:
 * <dl>
 * <dt>community-name=<i>String</i></dt>
 * <dd>Required community name. If the community doesn't exist then there is no
 * problem, the message is sent to no agent.</dd>
 * </p>
 * 
 * <dt>delayMillis=<i>long</i></dt> <dd>Delay milliseconds between relay
 * iterations. Set the delay to zero to run the pings as fast as possible.</dd>
 * <p>
 * 
 * <dt>verbose=<i>boolean</i></dt>
 * <dd>Output SHOUT-level logging messages. This can also be disabled by
 * modifying the Cougaar logging configuration to set:
 * 
 * <pre>
 *         log4j.category.org.cougaar.demo.community.PingSender=FATAL
 *         log4j.category.org.cougaar.demo.community.PingReceiver=FATAL
 * </pre>
 * 
 * For simplicity we support this as a plugin parameter, so new users don't need
 * to configure the logging service. If enabled, also consider turning off "+/-"
 * message send/receive logging by setting:
 * 
 * <pre>
 *         -Dorg.cougaar.core.agent.quiet=true
 * </pre>
 * 
 * </dd>
 * <p>
 * </dl>
 * 
 * @property org.cougaar.demo.community.delayMillis=5000 PingSender delay
 *           between ping iterations, if not set as a plugin parameter.
 * 
 * @property org.cougaar.demo.community.verbose=true PingSender should output
 *           SHOUT-level logging messages, if not set as a plugin parameter.
 * 
 * @see PingReceiver Required plugin for every agent that will receive ping
 *      relays.
 * 
 * @see PingServlet Optional browser-based GUI.
 */
public class PingSender
      extends AnnotatedSubscriptionsPlugin {


   @ObtainService
   public LoggingService log;
   
   @ObtainService
   public UIDService uids;
   
   @Arg(name="community-name")
   public String community_name;

   private MessageAddress target;
   
   @Arg(defaultValue="5000")
   public long delayMillis;
   
   @Arg(defaultValue="false")
   public boolean verbose;
   
   // sending relay change subscription
   // just used to rehydrate and get the counter
   private TodoSubscription<MyAlarm> expiredAlarms;

   private SimpleRelay sending_relay;

   /** This method is called when the agent is created */
   @Override
   public void start() {
      super.start();

      // using the attribute role to specify the target
      target = AttributeBasedAddress.getAttributeBasedAddress(community_name, "Role", "Member");
      if (target == null) {
         throw new IllegalArgumentException("Must specify a target");
      } else if (target.equals(agentId)) {
         throw new IllegalArgumentException("Target matches self: " + target);
      }
   }
   
   @Execute(on={Subscribe.ModType.ADD, Subscribe.ModType.CHANGE}, when="isMine")
   public void handleAdd(SimpleRelay relay) {
      log.debug("seems like I received something!");
      handleResponse(relay);
   }

   public boolean isMine(SimpleRelay relay) {
      return agentId.equals(relay.getTarget());
   }

   public boolean isTarget(SimpleRelay relay) {
      return agentId.equals(relay.getSource()) && target.equals(relay.getTarget());
   }

   /** This method is called when the agent starts. */
   @Override
   protected void setupSubscriptions() {
      super.setupSubscriptions();

      // Create a holder for alarms that have come due
      //
      // The "myAlarms" string is any arbitrary identifier, and would only be
      // significant if we made more than one TodoSubscription instance.
      if (delayMillis > 0) {
         expiredAlarms = blackboard.subscribe(new TodoSubscription("myAlarms"));
      }

      // Get our initial counter value, which is zero unless we're restarting
      // from an agent move or persistence snapshot
      int counter = getInitialCounter();

      // Send our first relay to our target
      sendNow(new Integer(counter));

      // When our target publishes a response, our "execute()" method will
      // be called.
   }

   /** This method is called whenever a subscription changes. */
   @Override
   protected void execute() {
      super.execute();
      // If we're using a delay, check to see if it is time to send the next
      // ping iteration
      if (delayMillis > 0 && expiredAlarms.hasChanged()) {
         for (MyAlarm alarm : expiredAlarms.getAddedCollection()) {
            handleAlarm(alarm);
         }
      }
   }
   
   @Query(where="isTarget")
   public void countQuery(SimpleRelay relay, QueryContext context) {
      context.count = ((Integer) relay.getQuery()).intValue();
      blackboard.publishRemove(relay);
   }
   
   /** Get our initial ping iteration counter value */
   private int getInitialCounter() {
      // Check to see if we've already sent a ping, in case we're restarting
      // from an agent move or persistence snapshot.
      if (blackboard.didRehydrate()) {
         QueryContext queryContext = new QueryContext();
         runQuery("countQuery", SimpleRelay.class, queryContext);
         if (verbose && log.isShoutEnabled()) {
            log.shout("Resuming pings to " + target + " at counter " + queryContext.count);
            return queryContext.count;
         }
      }
      return 0;
   }

   /** Handle a response to a ping relay that we sent */
   private void handleResponse(SimpleRelay relay) {
      // Print the target's response
      if (verbose && log.isShoutEnabled()) {
         log.shout("Received response " + relay.getQuery() + " from " + relay.getSource());
      }

      /*
       * // Figure out our next content value // // For scalability testing we
       * could make this a large byte array. Integer old_content = (Integer)
       * relay.getQuery(); Integer new_content = new
       * Integer(old_content.intValue() + 1);
       * 
       * if (delayMillis > 0) { // Set an alarm to call our "execute()" method
       * in the future sendLater(new_content); } else { // Send our relay now
       * sendNow(new_content); }
       */
   }

   /**
    * Wake up from an alarm to send our next relay iteration (only use if
    * delayMillis is greater than zero),
    */
   private void handleAlarm(MyAlarm alarm) {
      // Send our next relay iteration to the target
      Object content = alarm.getContent();
      sendNow(content);
   }

   /** Send our next relay iteration now */
   private void sendNow(Object content) {
      if (sending_relay == null) {
         // Remove query both locally and at the remote target, to cleanup
         // the blackboard.
         sending_relay = new SimpleRelaySource(uids.nextUID(), agentId, target, content);

         // Reuse our relay to send counter to the target
         if (verbose && log.isShoutEnabled()) {
            log.shout("Sending ping " + content + " to " + target);
            sending_relay.setQuery(content);
         }
         blackboard.publishAdd(sending_relay);
      } else {
         // Reuse our relay to send counter to the target
         if (verbose && log.isShoutEnabled()) {
            log.shout("Sending ping " + content + " to " + target);
            sending_relay.setQuery(content);
         }
         blackboard.publishChange(sending_relay);
      }

      sendLater((Integer) content + 1);
   }

   /** Send our next relay iteration after the non-zero delayMillis */
   private void sendLater(Object content) {
      // Set an alarm to call our "execute()" method in the future.
      //
      // An asynchronous alarm is more efficient and scalable than calling
      // a blocking "Thread.sleep(delayMillis)", since it doesn't tie up a
      // pooled Cougaar thread. By default, a Cougaar Node (JVM) is
      // configured to have a limit of 30 pooled threads.
      //
      // Instead of removing the relay now, we hold onto it until the alarm
      // is due. This allows the PingServlet to see the old relay on
      // blackboard during our delay time.
      if (verbose && log.isShoutEnabled()) {
         log.shout("Will send ping " + content + " to " + target + " in " + (delayMillis / 1000) + " seconds");
      }
      long futureTime = System.currentTimeMillis() + delayMillis;
      Alarm alarm = new MyAlarm(sending_relay, content, futureTime);
      getAlarmService().addRealTimeAlarm(alarm);
   }

   private static final class QueryContext {
      int count;
   }

   /** An alarm that we use to wake us up after the delayMillis */
   private class MyAlarm
         extends AlarmBase {
      private SimpleRelay priorRelay;
      private Object content;

      public MyAlarm(SimpleRelay priorRelay, Object content, long futureTime) {
         super(futureTime);
         this.priorRelay = priorRelay;
         this.content = content;
      }

      @SuppressWarnings("unused")
      // reflective access?
      public SimpleRelay getPriorRelay() {
         return priorRelay;
      }

      public Object getContent() {
         return content;
      }

      // Put this alarm on the "expiredAlarms" queue and request an "execute()"
      @Override
      public void onExpire() {
         expiredAlarms.add(this);
      }
   }
}

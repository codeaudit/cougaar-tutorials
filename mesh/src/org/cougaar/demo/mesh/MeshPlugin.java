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

package org.cougaar.demo.mesh;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.agent.service.alarm.AlarmBase;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.TodoSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.AnnotatedSubscriptionsPlugin;
import org.cougaar.core.relay.SimpleRelay;
import org.cougaar.core.relay.SimpleRelaySource;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.util.Arguments;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.annotations.Cougaar;

/**
 * This plugin is a relay scalability test that creates an arbitrarily large
 * "mesh" of relays.
 * <p>
 * For example, this plugin can be configured to create a fully-connected "star"
 * network formation:
 * 
 * <pre>
 *   agent "Peer0" sends to "Peer1" and "Peer2"
 *   agent "Peer1" sends to "Peer0" and "Peer2"
 *   agent "Peer2" sends to "Peer0" and "Peer1"
 * </pre>
 * <p>
 * Other topologies can be created, e.g. chains, rings, trees, etc. The only
 * requirement is that, if agent "A" lists agent "B" as a <i>target</i>, then
 * "B" must also list "A" as a <i>target</i>.
 * <p>
 * Each relay iteration waits until the prior iteration has completed, making it
 * easy to identify bottlenecks and dropped/duplicate relays. Each agent logs<br>
 * &nbsp;&nbsp;<code>Completed all <i>N</i> iterations</code><br>
 * once all <b>maxIterations</b> have succeeded.
 * <p>
 * Plugin parameters:
 * <dl>
 * <dt><b>targets</b>=</dt>
 * <dd>
 * Required comma-separated targets list, which supports range expressions for
 * easy scalability testing. For example, "Peer[0..3]" is expanded to:
 * 
 * <pre>
 *     Peer0, Peer1, Peer2
 * </pre>
 * 
 * Note that this agent must be listed as one of the targets, otherwise it will
 * not send any relays. This "self" requirement makes it easy to enable/disable
 * many agents using a global-replace in the configuration file.</dd>
 * <dt><b>verbose</b>=true</dt>
 * <dd>
 * Enable verbose SHOUT logging.</dd>
 * <dt><b>bloatSize</b>=-1</dt>
 * <dd>
 * Number of extra bytes to bloat each message, or -1 for no added size.</dd>
 * <dt><b>maxIterations</b>=-1</dt>
 * <dd>
 * Maximum number of relay iterations, or -1 for no limit.</dd>
 * <dt><b>delayMillis</b>=5000</dt>
 * <dd>
 * Added delay between iterations, in milliseconds, or -1 for for no extra
 * delay.</dd>
 * <dt><b>timeoutMillis</b>=5000</dt>
 * <dd>
 * How long to wait into an iteration before logging a warning that the
 * iteration is taking a long time, or -1 for no warnings.</dd>
 * <dt><b>exitWhenDone</b>=false</dt>
 * <dd>
 * Call {@link System#exit} when all <b>maxIterations</b> have been completed.</dd>
 * </dl>
 * <p>
 */
public class MeshPlugin
      extends AnnotatedSubscriptionsPlugin {

   @Cougaar.ObtainService()
   public LoggingService log;
   
   @Cougaar.ObtainService()
   public UIDService uids;


   /** Can't use annotation here. See {@link #parseTargets(String, String)} */
   private List<String> targets;
   
   @Cougaar.Arg(defaultValue="true")
   public boolean verbose;
   
   @Cougaar.Arg(defaultValue="-1")
   public int bloatSize;
   
   @Cougaar.Arg(defaultValue="-1")
   public long maxIterations;
   
   @Cougaar.Arg(defaultValue="5000")
   public long delayMillis;
   
   @Cougaar.Arg(defaultValue="30000")
   public long timeoutMillis;
   
   @Cougaar.Arg(defaultValue="false")
   public boolean exitWhenDone;

   private long counter = -1;
   private final Map<String, SimpleRelay> sent = new HashMap<String, SimpleRelay>();
   private final Map<String, Long> received = new HashMap<String, Long>();
   private int num_pending = 0;

   // time at end of our "load()" method
   private long loadTime;
   // time when we completed our second iteration
   private long activeTime = -2;
   // time when our delay-alarm will expire
   private long delayTime = -1;
   // time when we called "setTimeout()", or -1 if cancelled
   private long setTime = -1;
   // time when our timeout-alarm will expire
   private long timeoutTime = -1;

   private IncrementalSubscription<SimpleRelay> sub;
   private TodoSubscription<MyAlarm> expiredAlarms;

   /** This method is called when the agent is created */
   @Override
   public void load() {
      super.load();
      // parse our plugin parameters
      Arguments args = new Arguments(getParameters(), getClass());
      String targets_string = args.getString("targets");
      targets = parseTargets(targets_string, agentId.getAddress());

      if (verbose && log.isShoutEnabled()) {
         log.shout("Parsed " + targets.size() + " target" + (targets.size() == 1 ? "" : "s") + ": " + targets);
      }

      loadTime = System.currentTimeMillis();
   }

   /**
    * Configure alarm and relay subscriptions. Can't use annotations since both
    * are non-standard.  The relay sub requires the added list to be in order
    * and the alarm sub has to be a {@link #TodoSubscription}.
    */
   @Override
   protected void setupSubscriptions() {
      super.setupSubscriptions();
      if (targets.isEmpty()) {
         // we're not in the targets set, so don't send anything
         return;
      }

      // initialize our received table
      for (String target : targets) {
         received.put(target, new Long(0));
      }

      // subscribe to all relays sent to our agent
      UnaryPredicate<SimpleRelay> pred = new UnaryPredicate<SimpleRelay>() {
         private static final long serialVersionUID = 1L;

         public boolean execute(Object o) {
            return ((o instanceof SimpleRelay) && (agentId.equals(((SimpleRelay) o).getTarget())));
         }
      };
      sub = blackboard.subscribe(
            new IncrementalSubscription<SimpleRelay>(pred, new HashSet<SimpleRelay>()) {
         // we need our added list to be in order!
         @Override
         protected Set<SimpleRelay> createAddedSet() {
            return new LinkedHashSet<SimpleRelay>(5);
         }
      });

      // create a queue for expired alarms
      expiredAlarms = blackboard.subscribe(new TodoSubscription<MyAlarm>("myAlarms"));

      if (blackboard.didRehydrate()) {
         // restarting from an agent move or persistence snapshot
         restoreState();
         return;
      }

      // send our first round of relays to our targets
      sendNow();

      // when any target publishes a relay or one of our alarms fires,
      // our "execute()" method will be called.
   }

   /** This method is called whenever a subscription changes or alarm fires. */
   @Override
   protected void execute() {
      super.execute();
      if (sub == null) {
         // Our "execute()" is always called at least once, even if we
         // don't have any subscriptions.
         assert targets.isEmpty();
         return;
      }

      // check for expired alarms (delays & timeouts)
      if (expiredAlarms.hasChanged()) {
         for (MyAlarm oi : expiredAlarms.getAddedCollection()) {
            handleAlarm(oi);
         }
      }

      // check for incoming relays
      if (sub.hasChanged()) {
         for (SimpleRelay oi : sub.getAddedCollection()) {
            handleRelay(oi);
         }
      }
   }

   // handle an expired alarm
   private void handleAlarm(MyAlarm alarm) {
      assert (delayMillis > 0 || timeoutMillis > 0);

      long now = System.currentTimeMillis();

      if (delayTime > 0 && delayTime <= now) {
         // send our next round of relays to our targets
         delayTime = -1;
         sendNow();
      }

      // check for an input timeout
      if (timeoutTime > 0 && timeoutTime <= now) {
         checkTimeout();
      }
   }

   // handle an incoming relay
   private void handleRelay(SimpleRelay relay) {
      String target = relay.getSource().getAddress();
      Object new_obj = relay.getQuery();
      if (new_obj instanceof Payload) {
         new_obj = ((Payload) new_obj).getData();
      }
      Long new_value = (Long) new_obj;
      long value = new_value.longValue();

      if (verbose && log.isShoutEnabled()) {
         log.shout("Received " + value + " from " + target);
      }

      if (value != (counter + 1) && value != (counter + 2)) {
         log.error("Expecting " + (counter + 1) + " or " + (counter + 2) + " from " + target + ", not " + value + ", relay is "
               + relay);
         return;
      }

      Long old_obj = received.get(target);
      if (old_obj == null) {
         log.error("Unexpected relay from " + target + "  " + relay);
         return;
      }
      long old_value = old_obj.longValue();
      if (value == (old_value + 1)) {
         received.put(target, new_value);
      } else {
         // this is an error, unless we're restarting from an agent move or
         // persistence snapshot.
         log.warn("Unexpected value " + value + " from " + target + ", expecting " + (old_value + 1) + ", relay is " + relay
               + ".  Was this agent moved or restarted?");
         if (value > old_value) {
            received.put(target, new_value);
         }
      }

      if (value != (counter + 1)) {
         return;
      }
      num_pending--;
      if (num_pending > 0) {
         // keep waiting for more relays
         return;
      }

      cancelTimeout();

      // send next relay
      if (delayMillis > 0) {
         // set an alarm to call our "execute()" method in the future
         sendLater();
      } else {
         // send our relay now
         sendNow();
      }
   }

   /** Send our next relay iteration now */
   private void sendNow() {
      // record the timestamp of our second iteration
      //
      // we ignore the first iteration because it includes the naming and
      // messaging startup costs.
      if (activeTime < 0) {
         if (activeTime < -1) {
            activeTime = -1;
         } else {
            activeTime = System.currentTimeMillis();
         }
      }

      // increment counter
      counter++;
      if (maxIterations >= 0 && counter >= maxIterations) {
         long now = System.currentTimeMillis();
         log.shout("Completed all " + counter + " iterations in an initial " + (activeTime - loadTime) + " plus subsequent "
               + (now - activeTime) + " milliseconds");
         if (exitWhenDone) {
            try {
               System.exit(0);
            } catch (Exception e) {
               log.error("Unable to exit", e);
            }
         }
         return;
      }

      // delete old sent relays
      for (SimpleRelay priorRelay : sent.values()) {
         blackboard.publishRemove(priorRelay);
      }
      sent.clear();

      // send new relays
      Object content = new Long(counter + 1);
      if (bloatSize > 0) {
         content = new Payload(content, bloatSize);
      }
      if (verbose && log.isShoutEnabled()) {
         log.shout("Sending counter " + content + " to " + targets.size() + " target" + (targets.size() == 1 ? "" : "s"));
      }
      for (String target : targets) {
         SimpleRelay relay = new SimpleRelaySource(uids.nextUID(), agentId, MessageAddress.getMessageAddress(target), content);

         sent.put(target, relay);

         blackboard.publishAdd(relay);
      }

      updateNumPending();
      if (num_pending <= 0) {
         // if our delay is relatively large compared to the comms time, then
         // all our inputs may have already arrived.
         if (delayMillis > 0) {
            sendLater();
         } else {
            log.error("None pending?");
         }
         return;
      }

      // set timeout alarm
      setTimeout();
   }

   /** Send our next relay iteration after the non-zero delayMillis */
   private void sendLater() {
      if (verbose && log.isShoutEnabled()) {
         log.shout("Will send counter " + (counter + 1) + " in " + (delayMillis / 1000) + " seconds");
      }
      delayTime = System.currentTimeMillis() + delayMillis;
      getAlarmService().addRealTimeAlarm(new MyAlarm(delayTime));
   }

   private void updateNumPending() {
      // update num_pending
      num_pending = targets.size();
      for (Long vi : received.values()) {
         long value = vi.longValue();
         if (value > counter) {
            num_pending--;
         }
      }
   }

   // The following use of alarms has been optimized!
   //
   // The naive solution is to create an alarm in "setTimeout()", cancel it in
   // "cancelTimeout()", and "checkTimeout()" simply calls "handleTimeout()".
   // The downside to this solution is that we expect our mesh iteration period
   // to be *much* smaller than the timeout period, which will result in lots of
   // created-then-cancelled alarms. This is wasteful.
   //
   // Instead, it's more efficient to keep an alarm around and not cancel it.
   // This makes the "setTimeout()" and "cancelTimeout()" operations very fast.
   // The minor downside is that the "checkTimeout()" method is a bit more
   // complicated.
   private void setTimeout() {
      if (timeoutMillis <= 0) {
         return;
      }
      setTime = System.currentTimeMillis();
      if (timeoutTime < 0) {
         timeoutTime = setTime + timeoutMillis;
         getAlarmService().addRealTimeAlarm(new MyAlarm(timeoutTime));
      }
   }

   private void cancelTimeout() {
      if (timeoutMillis <= 0) {
         return;
      }
      setTime = -1;
   }

   private void checkTimeout() {
      assert timeoutMillis > 0;
      long expirationTime = timeoutTime;
      timeoutTime = -1;
      if (setTime >= 0) {
         long t = setTime + timeoutMillis;
         if (expirationTime < t) {
            timeoutTime = t;
            getAlarmService().addRealTimeAlarm(new MyAlarm(timeoutTime));
         } else {
            handleTimeout();
         }
      }
   }

   private void handleTimeout() {
      reportTimeout();
      setTimeout();
   }

   private void reportTimeout() {
      if (!log.isWarnEnabled()) {
         return;
      }

      StringBuffer buf = new StringBuffer("Waiting for " + num_pending + " of " + sent.size() + " relays {");
      for (String target : targets) {
         buf.append("\n  ").append(target).append(" ");
         long value = received.get(target).longValue();
         buf.append(value);
         if (value <= counter) {
            buf.append("  PENDING");
         }
      }
      buf.append("\n}");

      log.warn(buf.toString());
   }

   private void restoreState() {
      // figure out our counter by looking at our sent relays
      UnaryPredicate<SimpleRelay> pred = new UnaryPredicate<SimpleRelay>() {
         private static final long serialVersionUID = 1L;

         public boolean execute(Object o) {
            return ((o instanceof SimpleRelay) && (agentId.equals(((SimpleRelay) o).getSource())));
         }
      };
      long sent_value = -1;
      Collection<SimpleRelay> sent_col = blackboard.query(pred);
      for (SimpleRelay relay : sent_col) {
         String target = relay.getTarget().getAddress();
         Object obj = relay.getQuery();
         if (obj instanceof Payload) {
            obj = ((Payload) obj).getData();
         }
         long value = ((Long) obj).longValue();
         if (verbose && log.isShoutEnabled()) {
            log.shout("Sent " + value + " to " + target + "  " + relay);
         }
         if (value >= sent_value) {
            sent_value = value;
         }
      }
      counter = sent_value - 1;
      if (log.isShoutEnabled()) {
         log.shout("Restored counter at " + counter);
      }

      // remove any old sent relays
      for (SimpleRelay relay : sent_col) {
         String target = relay.getTarget().getAddress();
         Object obj = relay.getQuery();
         if (obj instanceof Payload) {
            obj = ((Payload) obj).getData();
         }
         long value = ((Long) obj).longValue();
         if (value == (counter + 1)) {
            sent.put(target, relay);
         } else {
            log.warn("Found stale relay to " + target + " with value " + value + " instead of expected " + (counter + 1)
                  + ", removing " + relay);
            blackboard.publishRemove(relay);
         }
      }

      // figure out what we've received
      for (Object oi : sub) {
         SimpleRelay relay = (SimpleRelay) oi;
         String target = relay.getSource().getAddress();
         Object obj = relay.getQuery();
         if (obj instanceof Payload) {
            obj = ((Payload) obj).getData();
         }
         Long longV = (Long) obj;
         long value = longV.longValue();
         Long old_value = received.get(target);
         if (verbose && log.isShoutEnabled()) {
            log.shout("Received " + value + " from " + target + "  " + relay);
         }
         if (old_value == null || old_value.longValue() < value) {
            received.put(target, longV);
         }
      }

      updateNumPending();
      if (num_pending <= 0) {
         // we must have been delaying between iterations.
         if (delayMillis > 0) {
            sendLater();
         } else {
            log.error("Restore with zero delay found no pending relays?");
         }
         return;
      }

      // we're still waiting for input, set timeout alarm
      if (verbose && log.isShoutEnabled()) {
         reportTimeout();
      }
      setTimeout();
   }

   // parse our target list, e.g. "Peer[0..20]"
   private static List<String> parseTargets(String s, String thisAgent) {
      if (s == null) {
         throw new IllegalArgumentException("Must specify targets");
      }
      boolean containsThisAgent = false;
      List<String> ret = new ArrayList<String>();
      s = s.replace('\n', ' ');
      String[] sa = s.split(",");
      ret = new ArrayList<String>();
      for (String si : sa) {
         si = si.trim();
         if (si.length() <= 0) {
            continue;
         }
         try {
            int j = si.indexOf('[');
            int k = (j >= 0 ? si.indexOf(']', j) : -1);
            if (j <= 0 || k <= 0) {
               // specific target name, e.g. "Foo"
               String target = si;
               if (target.equals(thisAgent)) {
                  containsThisAgent = true;
               } else {
                  ret.add(target);
               }
               continue;
            }
            // expand pattern, e.g. "X[0..3]Y" becomes "X0Y, X1Y, X2Y"
            //
            // could use a regex here
            int q = si.indexOf("..", j);
            if (q >= k) {
               q = -1;
            }
            int seq_begin;
            int seq_end;
            if (q < 0) {
               String x = si.substring(j + 1, k).trim();
               seq_begin = Integer.parseInt(x);
               seq_end = seq_begin + 1;
            } else {
               String x = si.substring(j + 1, q).trim();
               String y = si.substring(q + 2, k).trim();
               seq_begin = Integer.parseInt(x);
               seq_end = Integer.parseInt(y);
            }
            for (int index = seq_begin; index < seq_end; index++) {
               String target = si.substring(0, j).trim() + index + si.substring(k + 1).trim();
               if (target.equals(thisAgent)) {
                  containsThisAgent = true;
               } else {
                  ret.add(target);
               }
            }
         } catch (Exception e) {
            throw new RuntimeException("Invalid target: " + si, e);
         }
      }
      if (!containsThisAgent) {
         ret = Collections.emptyList();
      }
      return ret;
   }

   private class MyAlarm
         extends AlarmBase {
      public MyAlarm(long futureTime) {
         super(futureTime);
      }

      // Put this alarm on the "expiredAlarms" queue and request an "execute()"
      @Override
      public void onExpire() {
         expiredAlarms.add(this);
      }
   }
}

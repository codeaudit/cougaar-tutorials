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

package org.cougaar.demo.ping;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.agent.service.alarm.AlarmBase;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.blackboard.TodoSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.AnnotatedPlugin;
import org.cougaar.core.relay.SimpleRelay;
import org.cougaar.core.relay.SimpleRelaySource;
import org.cougaar.core.service.UIDService;
import org.cougaar.util.Arguments;
import org.cougaar.util.annotations.Cougaar;

/**
 * This plugin is an example ping source that sends relays to a remote agent.
 * <p>
 * There can be multiple copies of this plugin in a single agent, but every
 * {@link PingSender} must have a unique target. The target is specified as a
 * plugin parameter:
 * <dl>
 * <dt>target=<i>String</i></dt>
 * <dd>Required remote agent name. If the agent doesn't exist then we wait
 * forever -- there's no alarm-based timeout in this plugin implementation.</dd>
 * </p>
 * 
 * <dt>delayMillis=<i>long</i></dt>
 * <dd>Delay milliseconds between relay iterations. Set the delay to zero to
 * run the pings as fast as possible.</dd>
 * <p>
 * 
 * <dt>verbose=<i>boolean</i></dt>
 * <dd>Output SHOUT-level logging messages. This can also be disabled by
 * modifying the Cougaar logging configuration to set:
 * 
 * <pre>
 *         log4j.category.org.cougaar.demo.ping.PingSender=FATAL
 *         log4j.category.org.cougaar.demo.ping.PingReceiver=FATAL
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
 * @property org.cougaar.demo.ping.PingSender.delayMillis=5000 PingSender delay
 *           between ping iterations, if not set as a plugin parameter.
 * 
 * @property org.cougaar.demo.ping.PingSender.verbose=true PingSender should
 *           output SHOUT-level logging messages, if not set as a plugin
 *           parameter.
 * 
 * @see PingReceiver Required plugin for every agent that will receive ping
 *      relays.
 * 
 * @see PingServlet Optional browser-based GUI.
 */
public class PingSender extends AnnotatedPlugin {

    private UIDService uids;

    private MessageAddress target;
    
    @Cougaar.Param(name="target", required=true)
    public String targetName;
    
    @Cougaar.Param(name="delayMillis", defaultValue="5000")
    public long delayMillis;
    
    @Cougaar.Param(name="verbose", defaultValue="true")
    public boolean verbose;

    /** This method is called when the agent is constructed. */
    public void setArguments(Arguments args) {
        super.setArguments(args);
        // Parse our plugin parameters
        target = MessageAddress.getMessageAddress(targetName);
        if (target == null) {
            throw new IllegalArgumentException("Must specify a target");
        } else if (target.equals(agentId)) {
            throw new IllegalArgumentException("Target matches self: " + target);
        }
    }

    public void setUIDService(UIDService uids) {
        this.uids = uids;
    }
    
    /** This method is called when the agent starts. */
    protected void setupSubscriptions() {
        super.setupSubscriptions();
        // Get our initial counter value, which is zero unless we're restarting
        // from an agent move or persistence snapshot
        int counter = getInitialCounter();

        // Send our first relay to our target
        sendNow(null, new Integer(counter));

        // When our target publishes a response, our "execute()" method will
        // be called.
    }

    /** Create our "myAlarm" queue and handle ADD callbacks. */
    @Cougaar.Execute(on=Cougaar.BlackboardOp.ADD, todo="myAlarm")
    public void executeAlarm(MyAlarm alarm) {
        handleAlarm(alarm);
    }
    
    /** Create our "isMyRelay" subscription and handle CHANGE callbacks */
    @Cougaar.Execute(on=Cougaar.BlackboardOp.CHANGE, when="isMyRelay")
    public void executeRelay(SimpleRelay relay) {
        if (delayMillis <= 0) {
            return;
        }
        handleResponse(relay);
    }
    
    /** Blackboard predicate */
    public boolean isMyRelay(SimpleRelay relay) {
        return agentId.equals(relay.getSource()) && target.equals(relay.getTarget());
    }
    

    /** Get our initial ping iteration counter value */
    private int getInitialCounter() {
        // Check to see if we've already sent a ping, in case we're restarting
        // from an agent move or persistence snapshot.
        int ret = 0;
        if (blackboard.didRehydrate()) {
            // Get the counter from our sent ping, if any, then remove it
            Subscription sub = getSubscription("isMyRelay");
            for (Object o: (IncrementalSubscription) sub) {
                SimpleRelay relay = (SimpleRelay) o;
                ret = ((Integer) relay.getQuery()).intValue();
                blackboard.publishRemove(relay);
            }
            if (verbose && log.isShoutEnabled()) {
                log.shout("Resuming pings to " + target + " at counter " + ret);
            }
        }
        return ret;
    }

    /** Handle a response to a ping relay that we sent */
    private void handleResponse(SimpleRelay relay) {
        // Print the target's response
        if (verbose && log.isShoutEnabled()) {
            log.shout("Received response " + relay.getReply() + " from " + target);
        }

        // Figure out our next content value
        //
        // For scalability testing we could make this a large byte array.
        Integer old_content = (Integer) relay.getQuery();
        Integer new_content = new Integer(old_content.intValue() + 1);

        if (delayMillis > 0) {
            // Set an alarm to call our "execute()" method in the future
            sendLater(relay, new_content);
        } else {
            // Send our relay now
            sendNow(relay, new_content);
        }
    }

    /**
     * Wake up from an alarm to send our next relay iteration (only use if
     * delayMillis is greater than zero),
     */
    private void handleAlarm(MyAlarm alarm) {
        // Send our next relay iteration to the target
        SimpleRelay priorRelay = alarm.getPriorRelay();
        Object content = alarm.getContent();
        sendNow(priorRelay, content);
    }

    /** Send our next relay iteration now */
    private void sendNow(SimpleRelay priorRelay, Object content) {
        if (priorRelay != null) {
            // Remove query both locally and at the remote target, to cleanup
            // the blackboard.
            blackboard.publishRemove(priorRelay);
        }

        // Send a new relay to the target
        SimpleRelay relay = new SimpleRelaySource(uids.nextUID(), agentId, target, content);
        if (verbose && log.isShoutEnabled()) {
            log.shout("Sending ping " + content + " to " + target);
        }
        blackboard.publishAdd(relay);
    }

    /** Send our next relay iteration after the non-zero delayMillis */
    private void sendLater(SimpleRelay priorRelay, Object content) {
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
            log.shout("Will send ping " + content + " to " + target + " in " + delayMillis / 1000
                    + " seconds");
        }
        long futureTime = System.currentTimeMillis() + delayMillis;
        Alarm alarm = new MyAlarm(priorRelay, content, futureTime);
        getAlarmService().addRealTimeAlarm(alarm);
    }

    /** An alarm that we use to wake us up after the delayMillis */
    private class MyAlarm extends AlarmBase {
        private final SimpleRelay priorRelay;
        private final Object content;

        public MyAlarm(SimpleRelay priorRelay, Object content, long futureTime) {
            super(futureTime);
            this.priorRelay = priorRelay;
            this.content = content;
        }

        public SimpleRelay getPriorRelay() {
            return priorRelay;
        }

        public Object getContent() {
            return content;
        }

        // Put this alarm on the "myAlarms" queue and request an "execute()"
        public void onExpire() {
            TodoSubscription expiredAlarms = (TodoSubscription) getSubscription("myAlarm");
            expiredAlarms.add(this);
        }
    }
}

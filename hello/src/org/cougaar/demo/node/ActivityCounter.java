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
package org.cougaar.demo.node;

import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObjectBase;

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
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

import java.util.Collection;

import org.cougaar.core.plugin.AnnotatedSubscriptionsPlugin;
import org.cougaar.util.annotations.Cougaar;

/**
 * Illustrate query annotation
 */
public class HelloQueryPlugin
      extends AnnotatedSubscriptionsPlugin {
   
   /**
    * Named blackboard query. 
    * When the query is invoked via {@link #runQuery}, 
    * this method will be called once per match.
    * The where clause defines the match predicate.
    * Any number of context variables are passed to the processing method.
    * The same context is passed in each iteration.
    * 
    * @param match The next matching blackboard object.
    * 
    * @param context for the query. 
    */
   @Cougaar.Query(name="queryMoreThanThree", where="isMoreThanThree")
   public void helloQuery(HelloObject match, QueryStats context) {
      ++context.count;
   }
   
   /**
    * Predicate to match against all HelloObject on the blackboard
    * @param hello
    * @return true if more than three, else false
   */
   public boolean isMoreThanThree(HelloObject hello) {
      return  hello.getValue() > 3;
   }
   
   @Override
   protected void execute() {
      super.execute();
      QueryStats stats = new QueryStats();
      Collection<HelloObject> result = runQuery("queryMoreThanThree", HelloObject.class, stats);
      log.shout(stats.count + " Hello messages processed and  " + result.size() + " results returned");
   }

   /**
    * A context state that can change by each match invocation.
    */
   private static final class QueryStats {
      private int count;
   }

}

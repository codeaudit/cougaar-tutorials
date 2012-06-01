/* 
 * <copyright>
 *  Copyright 2012 BBN Technologies
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

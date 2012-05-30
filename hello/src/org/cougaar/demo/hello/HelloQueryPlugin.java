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
   
   @Cougaar.Query(name="more-than-three-hellos", where="gtr3")
   public void helloQuery(HelloObject hello, QueryStats stats) {
      ++stats.count;
   }
   
   public boolean gtr3(HelloObject hello) {
      String [] split = hello.getMessage().split("\\.");
      if (split.length == 2) {
         String countString = split[1];
         try {
            int count = Integer.parseInt(countString);
            return count > 3;
         } catch (NumberFormatException e) {
            return false;
         }
      }
      return false;
   }
   
   @Override
   protected void execute() {
      super.execute();
      QueryStats stats = new QueryStats();
      Collection<HelloObject> result = runQuery("more-than-three-hellos", HelloObject.class, stats);
      log.shout(stats.count + " Hello messages from " + result.size() + " results");
   }

   private static final class QueryStats {
      private int count;
   }

}

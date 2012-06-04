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

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.AnnotatedSubscriptionsPlugin;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.annotations.Cougaar;
import org.cougaar.util.annotations.Subscribe;

/**
 * Example of classic subscriptions that do not use annotations.
 * Classic subscriptions are useful if the changed blackboard objects 
 * need to be treated as a group. For example, if one group of blackboard
 * changes must be coorelated with another group.
 * Classic subscriptions can be used in conjunction with subscriptions added
 * using annotations. 
 * 
 */
public class HelloClassicSubscribePlugin
      extends AnnotatedSubscriptionsPlugin {
   
   //Generic Subscriptions added for Cougaar 12.6
   private IncrementalSubscription<HelloObject> subscription;

   /**
    *  Classic way to add subscriptions 
    */
   @Override
   protected void setupSubscriptions() {
      super.setupSubscriptions();
      
      subscription = blackboard.subscribe(new UnaryPredicate<HelloObject>() {
         private static final long serialVersionUID = -8980291132303080172L;

         public boolean execute(Object o) {
            return o instanceof HelloObject;
         }
      });
   }

   /**
    *  Classic way to process subscriptions 
    */
   @Override
   protected void execute() {
      //super call is necessary to allow annotated subscriptions to execute.
      super.execute();
      // Test if classic subscription has changed
      if (subscription.hasChanged()) {
         for (HelloObject hello : subscription.getAddedCollection()) {
            log.shout( "Classic Added " + hello);
         }
         for (HelloObject hello : subscription.getChangedCollection()) {
            log.shout( "Classic Changed " + hello);
         }
      }
   }



   /** 
    * The same subscription using annotations
    */
   @Cougaar.Execute(on = {
      Subscribe.ModType.CHANGE,
      Subscribe.ModType.ADD
   })
   public void executeLogHello(HelloObject hello) {
      log.shout("Annotated " + hello);
   }

}

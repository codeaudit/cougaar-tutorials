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
import org.cougaar.util.annotations.Cougaar;
import org.cougaar.util.annotations.Subscribe;

/** Hello Plugin logs "Hello" */
public class HelloSubscribePlugin
      extends AnnotatedSubscriptionsPlugin {

   /**
    * The execute annotation sets up a subscription for:
    *  1) A blackboard object of a specific type, here HelloObject. 
    *  2) A blackboard object's content has been modified, here changed or added. 
    *  3) A blackboard object's content matches a predicate, here blank.
    * 
    * The body of the method will be run on the matching object.
    * The name of the method is arbitrary, but convention uses a "execute" prefix because the
    * method will be run inside the execute event.
    * 
    * Note, If multiple changes were made to the object before this plugin was
    * called, only the last value of the matching object will be used.
    * 
    */
   @Cougaar.Execute(on = {
      Subscribe.ModType.CHANGE,
      Subscribe.ModType.ADD
   })
   public void executeLogHello(HelloObject hello) {
      log.shout(hello.toString());
   }

}

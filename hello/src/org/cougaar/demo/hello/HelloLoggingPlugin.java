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

/**
 * Hello Logger Plugin
 * The Logger service has multiple log levels that are control
 * by a logging.props file.
 * The file name is defined by the vm property 
 * -Dorg.cougaar.core.logging.config.filename=logging.props
 * <p>
 * Logger users should check "isEnabledFor(..)" before requesting 
 * a log message, to prevent unnecessary string creation.

 */
public class HelloLoggingPlugin
      extends AnnotatedSubscriptionsPlugin {

   /**
    * Execute method is called once after agent has started.
    * The Cougaar Node runs forever, until terminated
    */
   @Override
   public void execute() {
      super.execute();
      
       if ( log.isFatalEnabled()) {
         log.fatal("Hello, Fatal");
      }
      
      if ( log.isShoutEnabled()) {
         log.shout("Hello, Shout");
      }
      
      if ( log.isErrorEnabled()) {
         log.error("Hello, Error");
      }
      
      if(log.isWarnEnabled()) {
         log.warn("Hello, Wait");
      }
      
      if (log.isInfoEnabled()) {
         log.info("Hello, Info");
      }

      if (log.isDebugEnabled()) {
         log.debug("Hello, Debug");
       }
      
      if (log.isDetailEnabled()) {
         log.detail("Hello,Detail");
      }

   }
}

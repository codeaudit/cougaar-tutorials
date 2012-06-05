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

import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObjectBase;

/**
 * Hello Object for publishing and subscribing on blackboard extending Unique Object Base.
 * Setters and Getters allow access to attributes via the task servlet. 
 * Use of public fields is discouraged for blackboard objects.
 * Notice the Blackboard Object MUST have a unique the UUID field or they will test as equal.
 * Task Servlet.
 */
@SuppressWarnings("serial")
public class HelloObject
      extends UniqueObjectBase {

   private String message;
   private long changeCount;
   private long value;

 
   /**
    * Hello Object Constructor
    * @param uid is set in the creator's context with uids.nextUID() service
    * @param message is a user friendly id.
    * @param value is dynamic value
    */
   public HelloObject(UID uid, String message, long value) {
      super(uid);
      changeCount = 0;
      this.message = message;
      this.value = value;
   }

   /**
    * Hello Object Constructor
    * Value defaults to 0
    * @param uid is set in the creator's context with uids.nextUID() service
    * @param message is a user friendly id.
    */
   public HelloObject(UID uid, String message) {
      this(uid, message, 0);
   }
   
   /**
    * Hello Object Constructor
    * Value defaults to 0 and message defaults to "hello"
    * @param uid is set in the creator's context with uids.nextUID() service
    */
   public HelloObject(UID uid) {
      this(uid, "hello");
   }

    /**
    * Hello message
    * @return the message
    */
   public String getMessage() {
      return message;
   }

   /**
    * Set Hello message and increment change count, even if message is the same.
    */
   public void setMessage(String message) {
      this.message = message;
      this.changeCount++;
   }

   /**
    * Number of times the message or value has been set.
    */
   public long getChangeCount() {
      return changeCount;
   }

   /**
    * Dynamic Value 
    * @return the value
    */
   public long getValue() {
      return value;
   }

   /**
    * Set Dynamic value and increment change count
    * @param value the value to set
    */
   public void setValue(long value) {
      this.value = value;
      this.changeCount++;
   }
   
 @Override
  public String toString() {
      return "Hello Object:"
            + " Value=" + value 
            + " Changes=" +changeCount
            + " Message=" + message;
   }

}

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

import java.io.Serializable;
import java.util.Random;

/**
 * A {@link org.cougaar.core.relay.SimpleRelay#getQuery} content wrapper, used
 * to add "bloat" byte[] data to every message.
 */
public class Payload
      implements Serializable {
   private static final long serialVersionUID = 1L;

   private static final Random RANDOM = new Random();

   private final Object data;
   private final byte[] bloat;

   public Payload(Object data) {
      this(data, 0);
   }

   public Payload(Object data, int bloatSize) {
      this.data = data;
      byte[] b = null;
      if (bloatSize > 0) {
         b = new byte[bloatSize];
         RANDOM.nextBytes(b);
      }
      this.bloat = b;
   }

   /** @return the query data, typically an Integer */
   public Object getData() {
      return data;
   }

   /** @return the extra byte[] "bloat" length, or -1 if none */
   public int getBloat() {
      return (bloat == null ? -1 : bloat.length);
   }

   @Override
   public String toString() {
      return "(payload data=" + data + (bloat == null ? "" : " bloat[" + bloat.length + "]") + ")";
   }
}

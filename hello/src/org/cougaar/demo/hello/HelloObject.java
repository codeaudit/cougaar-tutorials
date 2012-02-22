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
package org.cougaar.demo.hello;

import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObjectBase;

/** Hello Object for publishing and subscribing on blackboard 
 * Unique Object Base gives blackboard support and allows attributes
 * to be set and viewed using the Task Servlet.
 */
@SuppressWarnings("serial")
public class HelloObject extends UniqueObjectBase {

	private String message;
	private long changeCount;

	public HelloObject(UID uid) {
		this(uid,"hello");
	}

	public HelloObject(UID uid, String message) {
		super(uid);
		changeCount=0;
		this.message=message;
	}

	/**
	 * Setter and Getter allow access to attributes via the task servlet
	 * Use of public fields is discouraged for blackboard objects
	 * @return
	 */

	/**
	 * Hello message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set Hello message
	 * increment Change Count, even if message is the same.
	 */
	public void setMessage(String message) {
		this.message = message;
		changeCount++;
	}

	/**
	 * Number of times the message has been set.
	 */
	public long getChangeCount() {
		return changeCount;
	}

}

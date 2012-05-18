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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.plugin.TodoPlugin;
import org.cougaar.util.annotations.Cougaar;
import org.cougaar.util.annotations.Subscribe;

/**
 * Offer an Agent-layer service that can change the HelloObject on the
 * blackboard This plugin show how to decouple calls from external threads from
 * blackboard transactions. Notice that the service is not offered at the normal
 * start() event, because the prerequisite HelloObject has not been created by
 * the time of the start event. The HelloService clients must wait for the
 * service to be offered.
 * */
public class HelloServicePlugin extends TodoPlugin implements HelloService {

	private HelloObject hello;

	/** log Logging service initialized by parent ParameterizedPlugin */
	/** uids UID service initialized by parent ParameterizedPlugin */

	/**
	 * Get the HelloObject from the blackboard
	 */
	@Cougaar.Execute(on = Subscribe.ModType.ADD)
	public void executeGetHello(HelloObject hello) {
		log.shout("got hello from blackboard");
		// Remember HelloObject
		this.hello = hello;
	}

	@Override
   public void load() {
		log.shout("loaded plugin");
		// Offer HelloService service
		serviceProvider = new MyServiceProvider();
		getServiceBroker().addService(HelloService.class, serviceProvider);
		super.load();
	}
	@Override
   public void stop() {
		log.shout("stop???");
		if (serviceProvider != null) {
			getServiceBroker().revokeService(HelloService.class,
					serviceProvider);
		}
		super.stop();
	}

	/**
	 * HelloService implementation. This interface is called by external threads,
	 * so it should not touch the black board directly
	 */
	public synchronized void changeMessage(String message) {
		// delay registering change with blackboard,
		// until later using the plugin's thread and not the callers thread
		// executeLater actually creates a todo task which runs in
		// this plugin's execute transaction
		executeLater(new ChangeTask(message));
	}

	/**
	 * Runable task to change the HelloObject. 
	 * This class remembers the new message value,
	 * delaying the actual change until its run() method is executed. 
	 * Run can only be executed in the plugin's thread inside an execute transaction
	 */
	private final class ChangeTask implements Runnable {
		private String newMessage;

		public ChangeTask(String message) {
			newMessage = message;
		}

		public void run() {
			if (hello != null) {
				hello.setMessage(newMessage);
				blackboard.publishChange(hello);
			}
		}
	}
	
	/**
	 * HelloService Service Provider implementation.
	 *  Use this (the plugin itself) as the service implementation.
	 */
	private ServiceProvider serviceProvider;

	private class MyServiceProvider implements ServiceProvider {
		public MyServiceProvider() {
			log.shout("new service provider");
		}

		public Object getService(ServiceBroker sb, Object req, Class<?> cl) {
			log.shout("getService");
			return HelloServicePlugin.this;
		}

		public void releaseService(ServiceBroker arg0, Object arg1,
				Class<?> arg2, Object arg3) {
			log.shout("releaseService");
		}
	}

}

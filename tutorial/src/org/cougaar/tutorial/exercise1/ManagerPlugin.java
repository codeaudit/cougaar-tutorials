/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.tutorial.exercise1;

/**
 * This COUGAAR Plugin publishes a Job object.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ManagerPlugin.java,v 1.1 2003-12-15 16:07:02 twright Exp $
 **/
import org.cougaar.core.plugin.*;
public class ManagerPlugin extends ComponentPlugin {

/**
 * setupSubscriptions is called when the Plugin is loaded.  We use
 * it here to create and publish a Job object.
 */
protected void setupSubscriptions() {
  getBlackboardService().publishAdd(new Job("Work"));
  System.out.println("ManagerPlugin::setupSubscriptions");
}

/**
 * This plugin has no subscriptions so execute does nothing
 */
protected void execute () {}
}

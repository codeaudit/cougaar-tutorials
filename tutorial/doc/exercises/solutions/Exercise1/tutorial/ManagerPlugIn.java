/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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
package tutorial;

/**
 * This COUGAAR PlugIn publishes a Job object.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ManagerPlugIn.java,v 1.3 2002-01-15 20:20:42 cbrundic Exp $
 **/
import org.cougaar.core.plugin.*;
public class ManagerPlugIn extends ComponentPlugin {

/**
 * setupSubscriptions is called when the PlugIn is loaded.  We use
 * it here to create and publish a Job object.
 */
protected void setupSubscriptions() {
  getBlackboardService().publishAdd(new Job("Work"));
  System.out.println("ManagerPlugIn::setupSubscriptions");
}

/**
 * This plugin has no subscriptions so execute does nothing
 */
protected void execute () {}
}

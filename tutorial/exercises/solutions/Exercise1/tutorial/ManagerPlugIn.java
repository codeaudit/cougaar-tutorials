/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package tutorial;

/**
 * This ALP PlugIn publishes a Job object.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ManagerPlugIn.java,v 1.2 2000-12-18 15:41:05 wwright Exp $
 **/
import org.cougaar.core.plugin.*;
public class ManagerPlugIn extends SimplePlugIn {

/**
 * setupSubscriptions is called when the PlugIn is loaded.  We use
 * it here to create and publish a Job object.
 */
protected void setupSubscriptions() {
  publishAdd( new Job("Work"));
  System.out.println("ManagerPlugIn::setupSubscriptions");
}

/**
 * This plugin has no subscriptions so execute does nothing
 */
protected void execute () {}
} 

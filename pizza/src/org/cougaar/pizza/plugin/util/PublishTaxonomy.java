/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.pizza.plugin.util;

import java.io.File;
import java.util.ArrayList;

import org.cougaar.pizza.Constants;

/**
 * Simple extension of the main ServiceDiscovery PublishTaxonomy support class, 
 * to point to the tModelNames needed by the pizza application, and point to the 
 * pizza app Taxonomy files. 
 * Note that this class could be used by any simple SD application that needs only
 * the Commercial Service Scheme, by simply using your own Constants.getDataPath().
 */
public class PublishTaxonomy extends org.cougaar.servicediscovery.util.yp.PublishTaxonomy {

  public void initialize() {
    super.initialize();

    // Add taxonomies required for pizza application. 
    // Constants.UDDIConstants.COMMERCIAL_SERVICE_SCHEME lists all
    // recognized roles.
    // Constants.UDDIConstants.ORGANIZATION_TYPES lists the recognized
    // organization types e.g. Military or Commercial (which we want).
    ArrayList tModelNames = new ArrayList();
    tModelNames.add(Constants.UDDIConstants.COMMERCIAL_SERVICE_SCHEME);
    tModelNames.add(Constants.UDDIConstants.ORGANIZATION_TYPES);

    setTModelNames(tModelNames);

    // Define file path for above taxonomy files - for pizza app, located in 
    // $CIP/pizza/data/taxonomies.
    setBasePath(Constants.getDataPath() + File.separator + 
		"taxonomies" + File.separator);
  }
}






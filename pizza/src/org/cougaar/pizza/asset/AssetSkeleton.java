/*
 * <copyright>
 *  
 *  Copyright 1997-2012 Raytheon BBN Technologies
 *  under partial sponsorship of the Defense Advanced Research Projects
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

/* @generated Wed Jun 06 07:13:47 EDT 2012 from /Users/djw/unbackedup/cougaar-tmp/repo/tutorials/pizza/src/org/cougaar/pizza/asset/properties.def - DO NOT HAND EDIT */
/** Abstract Asset Skeleton implementation
 * Implements default property getters, and additional property
 * lists.
 * Intended to be extended by org.cougaar.planning.ldm.asset.Asset
 **/

package org.cougaar.pizza.asset;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;


import java.io.Serializable;
import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;

public abstract class AssetSkeleton extends org.cougaar.planning.ldm.asset.Asset {

  protected AssetSkeleton() {}

  protected AssetSkeleton(AssetSkeleton prototype) {
    super(prototype);
  }

  /**                 Default PG accessors               **/

  /** Search additional properties for a VeggiePG instance.
   * @return instance of VeggiePG or null.
   **/
  public VeggiePG getVeggiePG()
  {
    VeggiePG _tmp = (VeggiePG) resolvePG(VeggiePG.class);
    return (_tmp==VeggiePG.nullPG)?null:_tmp;
  }

  /** Test for existence of a VeggiePG
   **/
  public boolean hasVeggiePG() {
    return (getVeggiePG() != null);
  }

  /** Set the VeggiePG property.
   * The default implementation will create a new VeggiePG
   * property and add it to the otherPropertyGroup list.
   * Many subclasses override with local slots.
   **/
  public void setVeggiePG(PropertyGroup aVeggiePG) {
    if (aVeggiePG == null) {
      removeOtherPropertyGroup(VeggiePG.class);
    } else {
      addOtherPropertyGroup(aVeggiePG);
    }
  }

  /** Search additional properties for a MeatPG instance.
   * @return instance of MeatPG or null.
   **/
  public MeatPG getMeatPG()
  {
    MeatPG _tmp = (MeatPG) resolvePG(MeatPG.class);
    return (_tmp==MeatPG.nullPG)?null:_tmp;
  }

  /** Test for existence of a MeatPG
   **/
  public boolean hasMeatPG() {
    return (getMeatPG() != null);
  }

  /** Set the MeatPG property.
   * The default implementation will create a new MeatPG
   * property and add it to the otherPropertyGroup list.
   * Many subclasses override with local slots.
   **/
  public void setMeatPG(PropertyGroup aMeatPG) {
    if (aMeatPG == null) {
      removeOtherPropertyGroup(MeatPG.class);
    } else {
      addOtherPropertyGroup(aMeatPG);
    }
  }

}

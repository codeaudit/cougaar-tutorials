/*
 * <copyright>
 *
 *  Copyright 1997-2004 BBNT Solutions, LLC
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
package org.cougaar.pizza;

import org.cougaar.planning.ldm.plan.Verb;

import java.io.File;

/**
 * Constants for the pizza party application
 */
public class Constants {
  // Private constructor prevents instantiation
  private Constants() {
  }

  public static final String PIZZA = "Pizza";
  public static final String INVITATION_QUERY = "invitation-meat_or_veg";
  public static final String MEAT_PIZZA = "Meat Pizza";
  public static final String VEGGIE_PIZZA = "Veggie Pizza";
  public static final String PROVIDER_SUFFIX = "Provider";
  public static final String CUSTOMER_SUFFIX = "Customer";

  /* used when showing how long it takes for this number of friends to reply */
  public static final int EXPECTED_NUM_FRIENDS = 4;

  public interface Verbs {
    public static final Verb ORDER = Verb.get("Order");
    public static final Verb FIND_PROVIDERS = Verb.get("FindProviders");
  }

  public interface Prepositions {
    public static final String NOT = "Not";
  }

  public interface RelationshipTypes {
    org.cougaar.planning.ldm.plan.RelationshipType PROVIDER =
        org.cougaar.planning.ldm.plan.RelationshipType.create(PROVIDER_SUFFIX, CUSTOMER_SUFFIX);
  }

  public static class Roles {
    /**
     * Insure that Role constants are initialized. Actually does nothing, but the classloader insures that all static
     * initializers have been run before executing any code in this class.
     */
    public static void init() {
    }

    static {
      org.cougaar.planning.ldm.plan.Role.create("Self", "Self");
      org.cougaar.planning.ldm.plan.Role.create(Constants.PIZZA, RelationshipTypes.PROVIDER);
      org.cougaar.planning.ldm.plan.Role.create("Carnivore", "Carnivore");
      org.cougaar.planning.ldm.plan.Role.create("Vegetarian", "Vegetarian");
    }

    // organization roles
    public static final org.cougaar.planning.ldm.plan.Role SELF = org.cougaar.planning.ldm.plan.Role.getRole("Self");
    public static final org.cougaar.planning.ldm.plan.Role CARNIVORE = org.cougaar.planning.ldm.plan.Role.getRole("Carnivore");
    public static final org.cougaar.planning.ldm.plan.Role VEGETARIAN = org.cougaar.planning.ldm.plan.Role.getRole("Vegetarian");

    public static final org.cougaar.planning.ldm.plan.Role PIZZAPROVIDER =
        org.cougaar.planning.ldm.plan.Role.getRole(Constants.PIZZA + PROVIDER_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role PIZZACUSTOMER =
        org.cougaar.planning.ldm.plan.Role.getRole(Constants.PIZZA + CUSTOMER_SUFFIX);
  }

  public static String getDataPath() {
    return System.getProperty("org.cougaar.install.path") + File.separator + "pizza" + File.separator + "data";
  }

  public interface UDDIConstants {
    public final static String COMMERCIAL_SERVICE_SCHEME = "CommercialServiceScheme";
    public final static String COMMERCIAL_SERVICE_SCHEME_UUID = "uuid:f0b01564-b8f0-b015-dad5-b49598339719";
    public final static String ORGANIZATION_TYPES = "OrganizationTypes";
    public final static String ORGANIZATION_TYPES_UUID = "uuid:c71f3d00-fb35-11d6-8c6a-b8a03c50a862";
  }
}



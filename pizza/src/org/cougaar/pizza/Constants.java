package org.cougaar.pizza;

/**
 *  String constants for the pizza party application
 */
public class Constants {
  // Private constructor prevents instantiation
  private Constants() {}

  public static final String ORDER = "Order";

  /** the query to my friends */
  public static final String INVITATION_QUERY = "invitation-meat_or_veg";

  /* used when showing how long it takes for this number of friends to reply */
  public static final int EXPECTED_NUM_FRIENDS = 4;

  public static final String FIND_PROVIDERS = "FindProviders";
  public static final String PIZZA_PROVIDER = "PizzaProvider";
  public static final String CARNIVORE = "Carnivore";
  public static final String VEGETARIAN = "Vegetarian";
  public static final String PIZZA = "Pizza";


  public interface Preposition {
    String NOT         =  "Not";
  }

  public interface RelationshipType {
    String PROVIDER_SUFFIX = "Provider";
    String CUSTOMER_SUFFIX = "Customer";
    org.cougaar.planning.ldm.plan.RelationshipType PROVIDER = 
      org.cougaar.planning.ldm.plan.RelationshipType.create(PROVIDER_SUFFIX, CUSTOMER_SUFFIX);
  }

  public static class Role {
    /**
     * Insure that Role constants are initialized. Actually does
     * nothing, but the classloader insures that all static
     * initializers have been run before executing any code in this
     * class.
     **/
    public static void init() {
    }

    static {
      org.cougaar.planning.ldm.plan.Role.create("Self", "Self");
      org.cougaar.planning.ldm.plan.Role.create("Pizza", RelationshipType.PROVIDER);
    }

    // organization roles
    public static final org.cougaar.planning.ldm.plan.Role SELF = 
      org.cougaar.planning.ldm.plan.Role.getRole("Self");

    public static final org.cougaar.planning.ldm.plan.Role PIZZAPROVIDER = 
      org.cougaar.planning.ldm.plan.Role.getRole("Pizza" + 
                                RelationshipType.PROVIDER_SUFFIX);
    public static final org.cougaar.planning.ldm.plan.Role PIZZACUSTOMER = 
      org.cougaar.planning.ldm.plan.Role.getRole("PIZZA" + 
                                RelationshipType.CUSTOMER_SUFFIX);
  }

  public interface UDDIConstants {

    public final static String COMMERCIAL_SERVICE_SCHEME = "CommercialServiceScheme";
    public final static String COMMERCIAL_SERVICE_SCHEME_UUID = "uuid:f0b01564-b8f0-b015-dad5-b49598339719";
    public final static String ORGANIZATION_TYPES = "OrganizationTypes";
    public final static String ORGANIZATION_TYPES_UUID = "uuid:c71f3d00-fb35-11d6-8c6a-b8a03c50a862";
  }

}



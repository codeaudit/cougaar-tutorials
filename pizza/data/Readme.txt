pizza/data contains data files used at runtime by Pizza app
components.
In particular, it contains all the data files required for registering
services with the Yellow Pages.

See serviceprofiles/README.txt for details on using the
servidediscovery files.

----
Detailed contents:

servicegroundings/:
	These files are not used by this application, but are here as examples.

	A-Domiinos.wsdl
		WSDL file for Domino's first provided service. Generated.
	A-JoesLocalPizzaShack.wsdl
		WSDL file for Joes' first provided service. Generated.
	B-JoesLocalPizzaShack.wsdl
		WSDL file for Joes' second provided service. Generated.
	PizzaProviderCougaarGrounding.wsdl
		Describes how to talk to a pizza rovider. Generated.
	cougaar.wsdl
		Describes how to talk to a Cougaar agent
	grounding-template.txt
		Template for per-service Agent service grounding files

serviceprofiles/:
	Service profiles are used by ServiceDiscovery agents to
	register their services in the Yellow Pages.

	Dominos.profile.owl
		OWL service profile for Dominos. Generated.
	JoesLocalPizzaShack.profile.owl
		OWL service profile for Joes. Generated.
	README.txt
		Describes how to use these various data files.
	agent-input.txt
		Edit this file to add or change the services or agents
		used. It is the input file to the genrateOWL.pl
		script.
	generateOWL.pl
		Perl script used to process agent-input.txt and
		generate application-specific service profiles and
		groundings.
	profile-template.txt
		Template for the service profiles.

taxonomies/:
	Taxonomies are used to properly file service profiles in the
	yellow pages. These files are application-specific.

	CommercialServiceScheme-yp.xml
		Categorizes commercial entities. Lists entries for
		PizzaProvider and SaladProvider.

	OrganizationTypes-yp.xml
		Lists types of organizations: Military or Commercial.

This directory will contain a skeletal society definition file with
just facets on the agents.
It will also contain a directory of rules that can be run against the
facets file to create both the full agent definitions with components,
and the community definitions.

This rule directory can serve as a "Rule book" for use in the Agent
Work Bench.

Some rules:

1) All agents get the Tasks servlet
2) All agents get the load servlet
3) nameserver node gets the WPServer component, and determines setting
of the -D arg
4) All nodes disable default load of WP stuff
5) all nodes get log config servlet
6) Party guests and pizza parlors get AssetDataParamPlugin
   Details: EatsPizza and HasPizza roles could be used. org_id facet
   Tells us what we need for most of the contents.
7) Party members get a meat/veggie preference pg
   Details: EatsPizza role has preference for kind of pizza
8) pizza parlors get the pizza parlor role
   Details: role=PizzaProvider
9) community members get the community plugin
--- so need a facet that says they're a member of a community, or
maybe that they're a member of a particular community?
   Details: they'll have "community" facet
10) all community members get the community view servlet
   Details: they'll have "community" facet
11) YP users get the YPClientComponent and UDDIRegistryQuery comp
   Details: They'll have the YPUser role
12) SDClients get the MatchmakerStub and helper servlet, and other sd
client stuff
--- note must be added after UDDIRegistryQuery
--- maybe this adds the YP client facet if not already there to force
   Details: They'll have the SDClient role. Note that this could imply
   the YPUser role
the rule to add this on next pass?
13) YPServers get the YPServer comp & PublishTaxonomy comp (or is this
SD related?). Also get the YPClientComp
   Details: They'll have the YPServer role. optional community tag
   indicates the community for which they are the YPServer
14) service providers get the SDReg stuff
--- UDDI4JReg comp
    Details: facet service= indicates they provide a service in general
        role SDProvider indicates they use service discovery to
	provide stuff
15) Guests get the RSVPPI
    Details: role=Invitee
16) Planner gets the InvitePI
--- InvitePI needs count of members of the community being invited
--- maybe this is a CommunityInvitePI and gets params indicating the
ABA to invite as well?
    Details: role=Inviter community=<name of community whose members
    to invite>
17) pizza consumer gets the PlaceOrderPI
    Details: role=PizzaConsumer
18) agents with pizza get the PizzaPrototype loader
    Details: role=HasPizza, extra toppings attribute indicates kind -
    can have multiple
19) pizza sellers get the pizza role and the ProcessOrderPI and the
pizzamenuservlet
     Details: service=PizzaProvider
20) people handling pizza orders get the pizza order servlet
    Details: service=PizzaProvider
21) SD service providers get the SDReg, SDProvider, ?
    Details: role=SDProvider
22) Non-SD rule to put AssetReportPI and Relationship on pizza sellers
- or maybe just one of them


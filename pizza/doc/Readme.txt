Pizza Party Planner Application
------------------------------------------------------
http://tutorials.cougaar.org/

1. INTRODUCTION

The Pizza Party Planner application is a simple 8 agent society that
exercises many key Cougaar modules, while staying relatively
simple. It demonstrates a little of what Cougaar can do, and key
features. It is also a good test to ensure key pieces of Cougaar are
still working, and a good set of sample code for modeling custom
components off of.

2. SETUP

Ensure that you installed cougaar.zip and cougaar-support.zip and that
your Cougaar Install Path environment variable points to the directory
where those were unzipped. It will also help if you put
COUGAAR_INSTALL_PATH/bin on your PATH.

3. RUNNING

To run this application, you run the 2 Cougaar Nodes that make up the
system:
[In a command line window]
cd COUGAAR_INSTALL_PATH/pizza/configs/pizzaparty
cougaar SDPizzaNode1.xml

[Start another window]
cd COUGAAR_INSTALL_PATH/pizza/configs/pizzaparty
cougaar SDPizzaNode2.xml

4. EXPECTED OUTPUT

In the first window, you should see something like:

COUGAAR 11.4 built on Tue Dec 07 07:04:16 GMT 2004
Repository: HEAD on Tue Dec 07 07:00:08 GMT 2004
VM: JDK 1.4.2_06-b03 (mixed mode)
OS: Windows 2000 (5.0)
16:52:11,680 SHOUT - XMLComponentInitializerServiceProvider - Initializing node "SDPizzaNo
de1" from XML file "SDPizzaNode1.xml"
18:05:41,490 SHOUT - Parameters - Warning: Found no source for (Database) Parameters - looked for ~/.cougaarrc or [ConfigPath]/cougaar.rc (see doc/OnlineManual/DataAccess.html)
2004-12-14 06:05:44,805 SHOUT [DOTS] - +-
18:05:45,015 SHOUT - InvitePlugin - Alice: Sending `Come to my party! RSVP: Meat or Veggie Pizza?' to my Buddy list: FriendsOfMark-COMM
2004-12-08 04:52:26,331 SHOUT [DOTS] -+-+-+-.+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-....+----
18:06:29,949 SHOUT - InvitePlugin - Alice: RSVP time is up. Got Party guests pizza preferences: {Bob=Meat Pizza, Mark=Veggie Pizza, Alice=Veggie Pizza, Tony=Meat Pizza}
2004-12-14 06:06:29,949 SHOUT [DOTS] - ++++--+-+-+-+-+-+-+-+-+.-+-
18:06:30,891 SHOUT - SDPlaceOrderPlugin - Alice: Pizza Order Task FAILED
18:06:30,891 SHOUT - SDPlaceOrderPlugin - Alice:      JoesLocalPizzaShack could handle Order for 2.0 servings of Meat Pizza
18:06:30,891 SHOUT - SDPlaceOrderPlugin - Alice:      JoesLocalPizzaShack could NOT handle Order for 2.0 servings of Veggie Pizza
18:06:30,891 SHOUT - SDPlaceOrderPlugin - Alice: Can't get the pizza I need! The party guests will not be happy....
18:06:30,891 SHOUT - SDPlaceOrderPlugin - Alice: Initial Expansion FAILed. Redo Service Discovery.
2004-12-14 06:06:30,891 SHOUT [DOTS] - ++-+-+-+-+-+-+-+-+-+-+-+-
18:06:31,021 SHOUT - SDPlaceOrderPlugin - Alice: Pizza Order Task SUCCEEDED
18:06:31,021 SHOUT - SDPlaceOrderPlugin - Alice:      Dominos could handle Order for 2.0 servings of Meat Pizza
18:06:31,021 SHOUT - SDPlaceOrderPlugin - Alice:      Dominos could handle Order for 2.0 servings of Veggie Pizza
18:06:31,021 SHOUT - SDPlaceOrderPlugin - Alice: The Party is on!

5. OTHER OUTPUT

As with all Cougaar applications, user interfaces for the Pizza Application are available as web pages.

The main interface for the Pizza Party is the "/pizza" servlet,
available at runtime at:
http://localhost:8800/$Alice/pizza
(See the Javadoc at http://cougaar.cougaar.org/software/11.4/javadoc/pizza/doc/api/org/cougaar/pizza/servlet/PizzaPreferenceServlet.html,
and a sample final output at: http://tutorials.cougaar.org/pizza/pizza-servlet-snapshot.htm.

To see all the servlets associated with the party planner, Alice,
navigate at runtime to the "/list" servlet: 
http://localhost:8800/$Alice/list

6. LEARN MORE
There are online documents at http://tutorials.cougaar.org/pizza

There are slides with pictures of the data flow, at http://cougaar.org/docman/view.php/5/169/pizza-overview.ppt

Read more about the story and flow (flow.html) of the
pizza party applcation; What is supposedly going on? What are the
components involved?

These components may be re-used in your application, or modified
slightly for your purposes. Here (extending.html) we
discuss how you might use this code for your own purposes.

Finally, this example includes some interesting design
decisions (see design.html). You may read about these to learn more
about some advanced Cougaar development topics.

################################################################
# Script to generate the profile.owl files, one per provider
# agent. Expects to use a owl template file of specific format.
# Expects to use an agent info input file of specific format.
# Use pizza/data/serviceprofiles/profile-template.txt for
# the template file. Use pizza/data/serviceprofiles/agent-input.txt
# for the input file.
#
# usage is generateOWL <inputfile> <templatefile>
################################################################

# <copyright>
#  
#  Copyright 2003-2004 BBNT Solutions, LLC
#  under sponsorship of the Defense Advanced Research Projects
#  Agency (DARPA).
# 
#  You can redistribute this software and/or modify it under the
#  terms of the Cougaar Open Source License as published on the
#  Cougaar Open Source Website (www.cougaar.org).
# 
#  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
#  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
#  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
#  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
#  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
#  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
#  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
#  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
#  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
#  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
#  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#  
# </copyright>



####################
# helper functions
####################

sub usage {
    &error_out("Usage: generateOWL.pl <inputfile> <templatefile>.");
} # usage

sub error_out {
    foreach (@_) {
	print STDERR $_."\n";
    }
    exit(1);
} # error_out

sub setup_descriptions{
    %serviceDescriptions = (
    "PizzaProvider" => "Make and deliver pizzas. ",
    );		    
}

sub resetServiceVariables{
    $cumulativeDescription="";
    $rememberedRole="none";
}
sub resetServiceCategoryVariables{
    $roleName="none";
}
sub resetAgentVariables{
    $agentName="";
}

sub read_command_args {
    &usage if $#ARGV!=1;

    if($_=shift(@ARGV)) {
	&usage if /-?-h/;
	$INPUTFILE=$_;
    }
    if($_=shift(@ARGV)){
	$TEMPLATEFILE=$_;
    }
    return($INPUTFILE, $TEMPLATEFILE);
} # read_command_args

sub read_globals {
    while($_=shift(@data)) {
	last if /AgentInfo/;
        ($name, $value) = split("=", $_);
        if($name eq "cougaarInstallPath") {
          $cougaarInstallPath = $value;
        }
	elsif($name eq "profileDirectory") {
          $profileDirectory = $value;
        }
	elsif($name eq "cougaarOWLFilename") {
          $cougaarOWLFilename = $value;
        }	
	elsif($name eq "groundingDirectory") {
	    $groundingDirectory = $value;
	}
    }
    return($cougaarInstallPath, $profileDirectory, $groundingDirectory, $cougaarOWLFilename);
}

#################################
# functions for printing out owl
#################################

sub print_through_Ontology {
    print "print_through_Ontology\n" if $dump;
    $tmppath = "file://".$cougaarInstallPath."/".$profileDirectory."/".$cougaarOWLFile;
    open(OUTPUT, ">$OUTPUTFILE") || &error_out("Can't open file $OUTPUTFILE");
    open(TEMPLATE, "$TEMPLATEFILE") || &error_out("Can't open file $TEMPLATEFILE");
    while($_=<TEMPLATE>) {
	s/%FULL_COUGAAR_OWL_FILEPATH%/$tmppath/;
	s/%AGENT_NAME%/$agentName/;

	print OUTPUT $_;
	print $_ if $dump; 
	if(m!/owl:Ontology!) {
	    last;
	}
    }
    close(TEMPLATE);
}

sub print_Service {
    print "print_Service\n" if $dump;
    print OUTPUT "\n";
    open(TEMPLATE, "$TEMPLATEFILE");
    $print=0;
    while($_=<TEMPLATE>) {
	if(/service:Service/) {
	    $print=1;
	}
	if($print) {
	    s/%AGENT_NAME%/$agentName/;
	    s/%INDEX%/$uniqueServiceIndex/;
	    print OUTPUT $_;
	    print $_ if $dump;
	}
	if(m!/service:Service!) {
	    last;
	}
    }
    close(TEMPLATE);
}
sub print_ServiceProfile_top {
    print "print_ServiceProfile_top\n" if $dump;
    print OUTPUT "\n";
    open(TEMPLATE, "$TEMPLATEFILE");
    $print=0;
    while($_=<TEMPLATE>) {
	if(/cougaar:ServiceProfile/) {
	    $print=1;
	}
	if($print) {
	    s/%AGENT_NAME%/$agentName/;
	    s/%INDEX%/$uniqueServiceIndex/;
	    print OUTPUT $_;
	    print $_ if $dump;
	}
	if(m!service:isPresentedBy!) {
	    last;
	}
    }
    close(TEMPLATE);
}

#print out text description and the rest of the ServiceProfile, do not read
#from @data
sub print_ServiceProfile_bottom {
    print "print_ServiceProfile_bottom\n" if $dump;
    open(TEMPLATE, "$TEMPLATEFILE");
    $print=0;
    while($_=<TEMPLATE>) {
	if(/profile:textDescription/) {
	    $print=1;
	    print OUTPUT "\n";
	}
	if($print) {
	    s/%AGENT_NAME%/$agentName/;
	    s/%SERVICE_DESCRIPTION%/$cumulativeDescription/;
	    print OUTPUT $_;
	    print $_ if $dump;
	}
	if(m!/cougaar:ServiceProfile!) {
	    last;
	}
    }
    close(TEMPLATE);
}

sub print_commercialRole_top {
    print "print_commercialRole_top\n" if $dump;
    open(TEMPLATE, "$TEMPLATEFILE");
    $print=0;
    while($_=<TEMPLATE>) {
	if($print) {
	    s/%AGENT_NAME%/$agentName/;
	    s/%ROLE_NAME%/$roleName/;
	    print OUTPUT $_;
	    print $_ if $dump;
	}
	if(/<!--Role-->/) {
	    $print=1;
	}
	if(/CommercialServiceScheme/) {
	    last;
	}
    }
    close(TEMPLATE);
}


sub print_additionalQualifications {
    print OUTPUT "\n";
    print "print_additionalQualifications\n" if $dump;
    while($_=shift(@data)){
	($name, $value) = split("=", $_);
	if(($name eq "roleName") || ($name eq "---")
	    || ($name eq "agentName")) {
	    unshift(@data, $_);
	    last;
	}
	if($name eq "qualificationName"){
	    $qName = $value;
	}
	if($name eq "qualificationValue"){
	    $qValue = $value;
	    open(TEMPLATE, "$TEMPLATEFILE");
	    $print=0;
	    while($_=<TEMPLATE>) {
		if(/cougaar:additionalQualification/) {
		    $print=1;
		}
		if($print) {
		    s/%QNAME%/$qName/;
		    s/%QVALUE%/$qValue/;
		    print OUTPUT $_;
		    print $_ if $dump;
		}
		if(m!/cougaar:additionalQualification!) {
		    last;
		}
	    }
	    close(TEMPLATE);	
	}
    }    
}



sub print_serviceCategory_bottom {
    print "print_serviceCategory_bottom\n" if $dump;
    open(TEMPLATE, "$TEMPLATEFILE");
    $print=0;
    while($_=<TEMPLATE>) {
	if(m!/cougaar:ServiceCategory>!) {
	    $print=1;
	}
	if($print) {
	    print OUTPUT $_;
	    print $_ if $dump;
	}
	if(m!/cougaar:serviceCategory>!) {
	    last;
	}
    }
    close(TEMPLATE);
}

#read from @data and print out a serviceCategory, ServiceCategory
sub print_commercial {
    &print_commercialRole_top;
    &print_additionalQualifications;
    &print_serviceCategory_bottom;
}

#read from @data and print out a serviceCategory, ServiceCategory

#print out the end rdf tag but do not read anything from @data
sub print_end_rdf {
    print "print_end_rdf\n" if $dump;
    print OUTPUT "\n";
    print OUTPUT "</rdf:RDF>";
}

#######################################
# write the profile file for one agent
#######################################

#read from @data and write the file for one agent
sub process_agent {
    while($_=shift(@data)){

	if(/agentName/){
	    $uniqueServiceIndex = "A";
	    ($name, $value) = split("=", $_);
	    $agentName = $value;
	    print "process_agent ".$agentName."\n" if $dump;
	    $OUTPUTFILE = "../../.."."/".$profileDirectory."/".$agentName.".profile.owl"; 
	    &print_through_Ontology;

	    $nextService = 1;
	    #this loop to do the services
	    while($nextService) {
		&resetServiceVariables;
		&print_Service;
		&print_ServiceProfile_top;

		#this loop to do the categories
		while($_=shift(@data)){
		    &resetServiceCategoryVariables;
		    if(/---/){
			$nextService = 1;
			last;
		    }
		    ($name, $value) = split("=", $_);
		    if(/agentName/){
			unshift(@data, $_);
			$nextService=0;
			last;
		    }
		    if($name eq "roleName") {
			$roleName=$value;
			$rememberedRole=$roleName;
			&print_commercial;
			$cumulativeDescription = $cumulativeDescription.$serviceDescriptions{$roleName};
		    }
		    else{
			&error_out("Unexpected line: ".$_);
		    }
		} #while shift data
		&print_ServiceProfile_bottom;
		$uniqueServiceIndex++;		
		if($_=shift(@data)){
		    unshift(@data, $_);
		}
		else{
		    $nextService=0;
		}
	    } #while next service
	    &print_end_rdf;
	} #if agent name
	last;
    } #while shift data
}

##############
# main
##############

sub main {
    
    $dump = 0;

    print "Generating agent owl profile files\n";

    ($INPUTFILE, $TEMPLATEFILE)=&read_command_args;

    open(INPUTINFO, $INPUTFILE) || &error_out("Can't open file $INPUTFILE");

    while(<INPUTINFO>) {
	chomp;   		#remove end newline
	s/\#.*$//;		#remove # comments
	next if /^\s*$/;	#skip whitespace lines
	s/\s*=\s*/=/;	#remove whitespace around the =
	s/\s*$//;		#remove whitespace at end of line
	push @data, $_;	#save this line of input into @data
    }

    close(INPUTINFO);

    $uniqueServiceIndex="A"; #an index for services within a profile

    ($cougaarInstallPath, $profileDirectory, $groundingDirectory, $cougaarOWLFile)=&read_globals;
     
    &setup_descriptions; #text descriptions of services

    while($#data>0) {
	&process_agent;
	&resetAgentVariables;
    }
}


&main;

#!/usr/bin/perl -w
use strict;
use FindBin qw($Bin);
use lib $Bin;
use XtrUtil;
do "xtr_html_util.pl";

# Script that fetches X-Trace reports from the backent and shows a graph with the
# reports connected by the parent/children relationships.
# Works in two modes: default (html) and image. The former produces an html page
# with an image map and a reference to itself in image mode.
# In image mode a png image is returned.
# Uses a stylesheet report.css in the document root of the server.

# parameters: See help below.


use CGI qw/:standard/;           # load standard CGI routines
use DBI;
use File::Temp qw/ tempfile /;
use LWP::Simple qw/get/;

#For different chainIds

################
# Configuration

my $TMP_DIR="/tmp";
my $applet_w = 1000;
my $applet_h = 600;

my $DB_CONFIG_FILE = "db.conf";

my $REPORTS_URL = "http://localhost:4080/reports";
my $DOT_CMD = "dot";

###############
# no need to edit below this point

my @palette = (
 "#000000",
 "#0000FF", "#00FF00", "#FF0000", "#00FFFF", "#FF00FF", "#FFFF00", #saturated
 "#9900FF", "#00FF99", "#99FF00", "#FF9900", "#FF0099", "#00FF99", #intermediate
 "#9999FF", "#99FF99", "#FF9999", "#99FFFF", "#FF99FF", "#FFFF99", #lighter
 "#000099", "#009900", "#990000", "#009999", "#990099", "#999900"  #darker
);
my (%color_index, $ci);
my @set312palette = (
"#8dd3c7", "#ffffb3", "#bebada", "#fb8072", "#80b1d3", "#fdb462",
"#b3de69", "#fccde5", "#d9d9d9", "#bc80bd", "#ccebc5", "#ffed6f"
);
my (%rpc_color_index, $rpc_ci);
my (%host_index, $host_index);

my @delays =    ( 1, 10, 100, 1000 ); #in ms
my @thickness = ( 1,  3,   5, 10   ); #in pt


# parameters to set:

my $graph_file_header = "xtrace"; 
my $graph_file_dir = "/tmp";

# parameters from URL:

#Parameter's help
#parameter, required, default, description
my @help = (
['db'         , 0 , 'default', 'The database to fetch reports from'],
['id'         , 1 , '', 'The X-Trace Task Id to lookup'],
['h'          , 0 , $applet_h, 'The height of the applet, in pixels'],
['w'          , 0 , $applet_w, 'The width of the applet, in pixels'],
['collapse'   , 0 , 'no', 'Whether recursive subgraphs should be collapsed'],
['colorby'    , 0 , 'rpc', 'Color nodes by rpc|host|level'],
['useChainId' , 0 , 'no', 'Whether to color nodes using the report ChainIds'],
['bp'         , 0 , 'no', 'Whether to break periodic events from the main graph'],
['m'          , 0 , '-', 'Different return types: i: svg, p: png, d: dot, other: regular html page with applet'],
['help'       , 0 , 'no', 'Print this help message']
);



my $xtr_id = param('id');
my $mode   = param('m');    #not useful to public
my $cache  = param('fn');   #not useful to public
$applet_h = param('h') if (defined param('h'));
$applet_w = param('w') if (defined param('w'));
my $debug = param('debug');
my $db    = param('db');
my $collapse = param('collapse');
my $explicit_chains = param('useChainId'); #use the chains defined in the reports, as opposed to
my $break_periodic = param('bp'); #disconnect periodic nodes from the graph
my $color_mode = param('colorby');
&output_help if (defined param('help'));

if (!defined $color_mode || 
    !($color_mode eq 'rpc' || $color_mode eq 'host' || $color_mode eq 'level')) {
    $color_mode = 'rpc';
}

srand(1);
###################
my ($min_time);

###################
my ($fh,$dotname);
my ($result);
my ($reverse_fn);

if ( !defined $mode || !($mode eq 'i') || ($mode eq 'i' && !defined $cache)) {
  if (!defined $xtr_id) {
          &output_error("No Task Id Id to report on");
  };
  $xtr_id =~ tr/a-f/A-F/;
  my $valid = $xtr_id =~ /^[A-F0-9]+$/;
  if ((length($xtr_id) < 8) || !$valid) {
          &output_error("Invalid X-Trace Task Id");
  }
  
  #xtr_id should now be sanitized and valid, we try to look it up
  $result = &get_results_ws($xtr_id);

  
  if (!defined $result || scalar(@$result) == 0) {
     output_error("No info for TaskId $xtr_id\n");
  }
  
  # Now that we have results we can do the real output
  # result is a reference to an array of hashes
  #   each report is one such hash, mapping report keys to an array of values
  #   Most keys will only map to one value
  
  # Create the graphviz file
  ($fh, $dotname) = tempfile($graph_file_header . "XXXXXX",DIR=>$graph_file_dir);
  my $g = &process_graph($result);
  &toposort($g);
  &find_nodes_coral_lvl($g);
  &find_edge_subgraphs($g);
  &find_periodic($g);
  &create_graphviz($fh,  $g);
} else {
   $reverse_fn = reverse $cache;
   $dotname = $graph_file_dir . "/" . $graph_file_header . $reverse_fn;
}
if (defined $mode && $mode eq 'i') {
    print header('image/svg+xml');
    print `$DOT_CMD $dotname -Tsvg`;
} elsif (defined $mode && $mode eq 'p') {
    print header('image/png');
    print `$DOT_CMD $dotname -Tpng`;
} elsif (defined $mode && $mode eq 'pdf') {
    print header('application/pdf');
    print `$DOT_CMD -Gratio=compress -Gsize="100,20" -Nfontsize=4 -Efontsize=4 $dotname -Tpdf`;
} elsif (defined $mode && $mode eq 'd') {
    print header('text/plain');
    print `cat $dotname`;
} else {
     my $url = url(-relative=>0);
     my $script=<<EOT ;
     function reportObject(ids, report) {
        this.ids = ids;
        this.report = report;
     }
     
     function showInfo(idx) {
        var tRow = document.getElementById("displayRow");
        tRow.cells[0].innerHTML = reports[idx].ids;
        tRow.cells[1].innerHTML = reports[idx].report;
     }

     var reports = new Array;
EOT
     my ($report,$opId);
     for (my $node_idx = 0; $node_idx < scalar (@$result); $node_idx++) {
        $report = ${$result}[$node_idx]->{'_Full'};
        $opId   = ${$result}[$node_idx]->{'_OpId'}->[0];
        next unless defined $opId;
        $report =~ s/\r?\n/<br>/g;
        $script .= qq|   reports[$node_idx] = new reportObject("$opId", "$report");\n|;
     }
  
     my $count_reports = @$result;
  
     #### HTML Output begins here
     print header();
     print start_html(-title=>"X-Trace reports for id $xtr_id",
                      -style=>{-src=>'/report.css'},
                      -script=>$script,
                      -head=>Link({-rel=>'search', 
                                   -type=>'application/opensearchdescription+xml', 
                                   -title=>'X-Trace', 
                                   -href=>'http://viz.x-trace.net/xtrace-search.xml'})
                      );
     print <<EOT ;
     <h1> X-Trace reports for task id $xtr_id </h1>
     <p class='description'>
     version 1.3 <br>
     Use url params 'h' and 'w' to set the applet size.<br>
     Node line color shows different 'chains', i.e., explicitly defined concurrent computation.<br>
     Node fill color shows different RPC calls, if any.
     Double click on nodes to show the detailed report.<br>
     Edge thickness is proportional do the delay.<br>
     </span>
EOT
     print "Graph $dotname<br>\n" if ($debug);
     print "<h2>Report Graph</h2>\n";
     print "<div id='divGraph'>\n";
     my $reverse_dotname = reverse $dotname;
     my $filename_to_pass = substr $reverse_dotname, 0, 6;
print <<EOT;
 <table><tr><td>
     <applet code="net.claribole.zgrviewer.ZGRApplet.class"
              archive="/zvtm.jar,/zgrviewer.jar"
              width="$applet_w" height="$applet_h">
        <param name="type" value="application/x-java-applet;version=1.4" />
        <param name="scriptable" value="false" />
        <param name="width" value="$applet_w" />
        <param name="height" value="$applet_h" />
        <param name="svgURL" value="$url?id=$xtr_id&m=i&fn=$filename_to_pass" />
        <param name="title" value="ZGRViewer - Applet" />
        <param name="appletBackgroundColor" value="#DDD" />
        <param name="graphBackgroundColor" value="#DDD" />
        <param name="highlightColor" value="red" />
      </applet> 
</td>
   <td>
     $count_reports reports total. <br>
     <table class="displayTable" border="1px">
      <tr><td class="head">OpId</td>
          <td class="head">Report</td>
      </tr>
      <tr id="displayRow"><td class="content"></td><td class="content"></td></tr>
     </table>
</td></tr></table>
	</div>
EOT
     print "<hr>";
     # Table with all reports fetched 
     print "<h2>All Reports</h2>\n";
     print "<table id='tableAll'>\n";
     print qq( <tr><td class="head">OpId</td><td class="head">Report</td></tr>);
  
     for (my $node_idx = 0; $node_idx < scalar (@$result); $node_idx++) {
        $report = ${$result}[$node_idx]->{'_Full'};
        $opId   = ${$result}[$node_idx]->{'_OpId'}->[0];
        $report =~ s/\r?\n/<br>/g;
        print qq( <tr><a name="$node_idx">);
        print qq( <td class="report">$opId</td><td class="report">$report</td></tr>);
        print qq( </tr>\n);
     }
     print "</table>\n"; 
     print end_html();
     #### End of HTML output
}

sub output_help {
	use strict;
   	print header, start_html(
                      -head=>Link({-rel=>'search', 
                                   -type=>'application/opensearchdescription+xml', 
                                   -title=>'X-Trace', 
                                   -href=>'http://viz.x-trace.net/xtrace-search.xml'}));
	print "<h2>Help on parameters</h2>\n";
	print "<p>* is required</p>\n";
	print "<table>\n";
	print "<tr><td>Parameter</td><td>Description</td><td>Default</td>\n";
	for my $h (@help) {
		print "<tr>";
		print ("<td>" . $h->[0] . (($h->[1])?"*":"") . "</td>");
		print ("<td>" . $h->[3]. "</td><td>".$h->[2]."</td>");
		print "</tr>\n"	;
	}
	print "</table>\n";
	print end_html;
	exit;
}

########################################
#  obtaining reports from backend
########################################  

# get_results (xtr_id)
# Fetch results for this task id from the backend database and returns a reference 
# to the results array.
# @param xtr_id Cannonicalized x-trace task id (all CAPS)
# @return An array reference with the results, and an error string
sub get_results {
    use strict;
    my $xtr_id = shift;
    return unless defined $xtr_id;
    my ($result,$report,$ts);
    my ($dbname, $host, $port, $dbusr, $pwd);
    my $db_config = &load_db_config($DB_CONFIG_FILE);

    if (!defined $db_config) {
    	&output_error("Error with db config\n");
    }
    my $dbkey = (defined $db)?$db:'default'; #this comes from the db parameter
    if (!exists $db_config->{$dbkey}) {
    	&output_error('Error finding database');
    }
    $dbname = $db_config->{$dbkey}->{'dbname'};
    $host = $db_config->{$dbkey}->{'host'};
    $port = $db_config->{$dbkey}->{'port'};
    $dbusr = $db_config->{$dbkey}->{'dbusr'};
    $pwd = $db_config->{$dbkey}->{'pwd'};

    if (!defined $dbname || !defined $host || !defined $port ||
        !defined $dbusr  || !defined $pwd)  {
    	&output_error('Error in database configuration');
    }

    my $dbh = DBI->connect("dbi:Pg:dbname=$dbname;host=$host;port=$port;",
                           $dbusr, $pwd) or
              &output_error("Can't connect to database: ". DBI->errstr);
    my $sth = $dbh->prepare("SELECT contents from reports where taskid = ?");
    $sth->execute($xtr_id) or
              &output_error("Error executing query: ". DBI->errstr);
    my $res_aref = $sth->fetchall_arrayref;
    if (!defined $res_aref) {
        return $result;
    }
    for my $row_ref (@$res_aref) {
        $report = &parse_report($row_ref->[0]);
        $ts = ${$report}{'_ts'}->[0];
        if (defined $ts) {
          $min_time = $ts if (!defined $min_time || $ts < $min_time);
        }
        push @{$result}, $report;
    }
    return ($result);
}

# get_results_ws (xtr_id)
# DEPRECATED (does not support multiple databases in the current web-services form)
# Fetch results for this task id from the backend webservice and returns a reference 
# to the results array.
# @param xtr_id Cannonicalized x-trace task id (all CAPS)
# @return An array reference with the results, and an error string
sub get_results_ws {
   my ($url,$result);
   my (@reports);
   my ($ts, $report,$content);
   my $xtr_id = shift;
   return unless defined $xtr_id;
   $url = "$REPORTS_URL/$xtr_id";
   #$url = "http://localhost/rep";
   $content = get($url);
   @reports = split /\n\n/s, $content;
   #print join "----------------------------", @{$result};
   #print "\n";
   for my $r (@reports) {
      $report = &parse_report($r);
      $ts = ${$report}{'_ts'}->[0];
      if (defined $ts) {
        $min_time = $ts if (!defined $min_time || $ts < $min_time);
      }
      push @{$result}, $report;
   } 
   #&shuffle($result);
   return ($result);
}

sub shuffle {
	my $array = shift;
	my $i;
	for ($i = @$array; --$i; ) {
		my $j = int rand ($i + 1);
		@$array[$i,$j] = @$array[$j,$i];
	}
}


# This function finds the coral levels for given nodes. Levels in Coral correspond to the
# cluster level (2,1, or 0) that the RPCs for finding and storing information on the DHT are
# called.
#  levels start with
#    start_clstr_lvl_[0|1|2]
#  levels end with
#    [SUCCESS|FAILED]_on_lvl_[0|1|2]
# Input: graph $g
# Precondition: $g must have been topo-sorted
# Output: stores in each node of $g a 'coral_level' attribute if the level is defined

sub find_nodes_coral_lvl {
   my $g = shift;

   my @levels;
   my $level;
   my ($curr_d, $curr_f, $curr_level, $nd, $nf, $label,$dummy);
   
   $curr_d = $curr_f = $curr_level = undef; 
   push @levels, [$curr_d, $curr_f, $curr_level];
   
   for my $node (@{$g->{'toposort'}}) {
      ($curr_d, $curr_f, $curr_level) = @{$levels[-1]};
      $nd = $g->{'nodes'}->{$node}->{'d'}; 
      $nf = $g->{'nodes'}->{$node}->{'f'}; 
      $label = $g->{'nodes'}->{$node}->{'report'}->{'Label'}->[0];
      next unless defined $label;
      # change level?
      if (($level) = $label =~ /start clstr lvl (\d)/) {
         $g->{'nodes'}->{$node}->{'coral_level'} = $level;
         #print "start level, setting $node ($label) to level $level\n";
         push @levels, [$nd, $nf, $level];   
         #print " pushing ($nd, $nf, $level)\n";
      } elsif (($dummy, $level) = $label =~ /(SUCCESS|FAILED) on lvl (\d)/) {
         #print "level error on node $node ($level != $curr_level)!\n" if ($level != $curr_level);
         $g->{'nodes'}->{$node}->{'coral_level'} = $level;
         #print "end level $curr_level node $node. setting $node ($label) to level $level\n";
         #print " pushing ($nd, $nf, undef)\n";
         push @levels, [$nd, $nf, undef];
      } elsif (defined $curr_d && $nf < $curr_d) {
         #print "node $node: ($nd, $nf) < ($curr_d) past current stack level\n";
         while($nf < $curr_d) {
            pop @levels;
            ($curr_d, $curr_f, $curr_level) = @{$levels[-1]};
            #print " popping, top of stack now ($curr_d, $curr_f, $curr_level)\n";
         }
         if (defined $curr_level) {
            $g->{'nodes'}->{$node}->{'coral_level'} = $curr_level;
            #print "setting $node ($label) to level $curr_level\n";
         }
      } else {
         if (defined $curr_level) {
            $g->{'nodes'}->{$node}->{'coral_level'} = $curr_level;
            #print "setting $node ($label) to level $curr_level\n";
         }
      }
   }     
} 


########################################
#  graphviz graph generation
########################################  
 
# result is an array reference. Each element in the array is an array
# reference to the query results:
# from the query:
#
sub create_graphviz {
   my ($fh, $g) = @_;
   $ci = 0;
   my ($label);
   # 'printed' is a hash by node that indicates whether 
   #  a node has already been output.
   undef $g->{'printed'} if (defined $g->{'printed'});
   print $fh <<EOT ;
digraph G {
        /* rankdir = LR; */
        node [URL = "javascript:showInfo(\\N)", fontsize="8", fontname="Arial"];
EOT
    #nodes
	for my $node (@{$g->{'toposort'}}) {
		next if (defined $g->{'printed'}->{$node});
		if (! defined $g->{'nodes'}->{$node}->{'in_subgraph'}) {
			$label = &gv_node_to_string($g,$node);
    	    	print $fh "$label\n";
			$g->{'printed'}->{$node} = 1;
		} elsif (! defined $collapse &&  defined $g->{'nodes'}->{$node}->{'subgraph'}) {
			# this only works properly if the nodes are visited
			# in the second topological order (toposort)
			&print_cluster($fh, $g, $node," ");
		}
	}
   
    #edges
    for my $node(@{$g->{'nodelist'}}) {
	next if (!defined $g->{'printed'}->{$node});
	for my $to (@{$g->{'nodes'}->{$node}->{'out'}}) {
		next if (!defined $g->{'printed'}->{$to});
		next if (defined $break_periodic &&
			 defined $g->{'nodes'}->{$to}->{'periodic'});
		$label = &gv_edge_to_string($g,$node,$to);
        	print $fh "$label\n";
	}	
    }
    print $fh "}\n";
}

sub print_cluster {
	use strict;
	my ($fh, $g, $node, $sp) = @_;
	my ($label);
	#print the nodes in this cluster
	print $fh $sp."subgraph cluster_$node {\n";
	print $fh $sp."  color=grey;\n";
	for my $cnode ( @{$g->{'nodes'}->{$node}->{'subgraph'}}) {
		$label = &gv_node_to_string($g,$cnode);
		$g->{'printed'}->{$cnode} = 1;
		print $fh $sp."  $label\n";
	}
	#print the clusters in this cluster
	for my $cnode (@{$g->{'nodes'}->{$node}->{'nest_subgraph'}}) {
		&print_cluster($fh, $g, $cnode, "$sp "); 
	}
	#close this cluster
	print $fh $sp."}\n";
}

sub gv_node_to_string {
	use strict;
	# outside scope: $color_index, $ci, $min_time, @palette
	# collateral effect: $color_index, $ci
	my ($g,$node) = @_;
	my ($_agent, $_rpc,$_http,$_host);
	my ($fillcolor);
	my ($opId, $string, $report, $label, $order, $d, $f);
	my ($agent, $rpc, $chainId, $ts, $color,$periodic,$black, $http, $host);
	my ($style);
    my ($coral_level);

	$report = $g->{'nodes'}->{$node}->{'report'};	
	$opId = ${$report}{'_OpId'}->[0];
	$ts = ${$report}{'_ts'}->[0];
	$_agent = ${$report}{'Agent'}->[0];
	$label = ${$report}{'Label'}->[0];
	$periodic = $g->{'nodes'}->{$node}->{'periodic'};
	if (defined $periodic)  {
		$black = $g->{'periodic'}->{$periodic}->{'black'}->{$node};
	}
	if (defined $_agent) {
		($agent) = $_agent =~ /([^\.]+)/;
	}
	$_http = ${$report}{'HTTP'}->[0];
	if (defined $_http) {
		($http) = $_http =~ /([^\?]+)/;
	}
	$_host = ${$report}{'Host'}->[0];
	if (defined $_host) {
		($host) = $_host; 
        if (!defined $host_index{$host}) {
            $host_index{$host} = $host_index++;
        } 
        if ($color_mode eq 'host') {
		    $fillcolor = "$set312palette[$host_index{$host} % scalar(@set312palette)]";
        }
	}
	$_rpc = ${$report}{'RPC'}->[0];
	if (defined $_rpc) {
		($rpc) = $_rpc =~ /[^:]*:[^:]*:(.*)/;
		if (!defined $rpc_color_index{$rpc}) {
			$rpc_color_index{$rpc} = $rpc_ci++;
		} 
        if ($color_mode eq 'rpc') {
		    #$fillcolor = "/set312/" . ($rpc_color_index{$rpc}+1);
		    $fillcolor = "$set312palette[$rpc_color_index{$rpc} % scalar(@set312palette)]";
        }
	}
	$chainId = ${$report}{'ChainId'}->[0] || 0;
	if (!defined $color_index{$chainId}) {
		$color_index{$chainId} = $ci;
		$ci = ($ci + 1) % scalar(@palette);
	}
	if (defined $explicit_chains) {
		$color = $palette[$color_index{$chainId}];
	} else {
		$color = $palette[$g->{'nodes'}->{$node}->{'graph_chain'}];
	}
	$f = $g->{'nodes'}->{$node}->{'f'};
	$order = $g->{'nodes'}->{$node}->{'order'};
	$d = $g->{'nodes'}->{$node}->{'d'};

    $coral_level =  $g->{'nodes'}->{$node}->{'coral_level'};
    if (defined $coral_level && $color_mode eq 'level') {
       $fillcolor = "$set312palette[$coral_level % scalar(@set312palette)]";
    }

	if (defined $black || defined $fillcolor) {
		$style = "style = \"";
		if (defined $black) {
			$style .= "bold, filled";
			$fillcolor = "red";
			$label = "*$label";
		}
		if (defined $fillcolor) {
			$style .= "filled";
		}
		$style .= "\"";
		if (defined $fillcolor) {
			$style .= ", fillcolor = \"$fillcolor\"";
		}
	}
	
	$string = " \"$node\" [ label = \"$opId ";
	#$string .= "*" if ($debug && defined $g->{'nodes'}->{$node}->{'in_subgraph'});
	$string .= "[$node] ($order: $d,$f)" if ($debug);
	#$string .= "\\nChainId: $chainId" if defined $chainId;
    $string .= " lvl: $coral_level " if ($debug && defined $coral_level);
	$string .= "\\nLabel: $label" if defined $label;
	$string .= "\\n$agent" if defined $agent;
	$string .= "\\n$host" if defined $host;
	$string .= "\\n$rpc"  if defined $rpc;
	$string .= "\\n$http"  if defined $http;
	$string .= sprintf "\\nts: %.3f\"", ($ts - $min_time);
	$string .= ", shape = \"box\"" if defined $periodic;
	$string .= ", $style" if defined $style;
	$string .= ", color = \"$color\"" if defined $color;
    $string .= " ]";
	return $string;
} 

sub gv_edge_to_string {
	use strict;
	my ($g, $from, $to) = @_;
	my ($label);
	my ($fr, $tr, $ts);
	my ($thickness, $order,$cross_host);
	$fr = $g->{'nodes'}->{$from}->{'report'};
	$tr = $g->{'nodes'}->{$to}->{'report'};
	$ts = ${$tr}{'_ts'}->[0] - ${$fr}{'_ts'}->[0];		
	$cross_host = ${$tr}{'Host'}->[0] ne ${$fr}{'Host'}->[0];
	$thickness = &getThickness($ts);
	$order = $g->{'c_edges'}->{"$from.$to"}->{'subgraph'};
	$label = "\"$from\" -> \"$to\"";
	$label .= " [color = \"";
	$label .=  (defined $order)?"red\"":"black\"";
	if ($ts > 0) {
		$label .= sprintf ", label=\"%.3fs\"", $ts;
	}
	$label .= ", style=\"";
	if ($cross_host) {
		$label .= "dashed";
	} else {
		$label .= "solid";
	}
	$label .= ", setlinewidth($thickness)" if defined $thickness;
	$label .= "\"";
	$label .=  "] ";
	return $label;
}

sub getThickness {
	my $ts = shift;
	return unless (defined $ts && $ts >= 0);
	my $i = 0;
	while ($i < scalar(@delays) && $ts > $delays[$i]) {last if ($i+1 == scalar(@delays));$i++;}
        return ($thickness[$i]);	
}




# Finding nested edges:
# The graphs being examined here are DAGs.
# The algorithm for finding nested edges will not work if there are true joins in the graph. In this case,
# there must be proper use of chainIds in the barrier nodes. This algorithm will work in a single chainId with 
# no barriers (joins).
# This program tries to find the nested edges that occur due to encapsulation by doing a topological sort.
# In a topological ordering, a node will only appear after all of its ancestors have appeared.
# We perform two topological sort steps, and use the first one to determine the order in which we examine nodes.
# This guarantees that an edge that abstracts a subgraph will only be examined after the subgraph has been examined.
# 
# 1. Do a Topological sort using the order the reports were stored in the database.
# 2. Do a Topological sort using the topological ordering obtained 1. 
# 3. Mark all edges found 'closed' in 2 as abstract. (again, this will not work properly if there are true barriers)
# 4. Add these edges in order of discovery to a list
# 5. For each abstract edge ij, find the subgraph that it abstracts
#    5.1 Find the right child of i that has the path to j
#    5.2 Mark all nodes k with d_k > d_i and f_k < f_i
#    These nodes have bi < b < bj && fj < f < fi
# Because of the order of edge discovery, we will explore the edges in nesting order. 

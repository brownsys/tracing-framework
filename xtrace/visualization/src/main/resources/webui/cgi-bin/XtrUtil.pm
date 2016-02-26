package XtrUtil;

## Utilities for parsing and processing X-Trace reports and
#  graphs.
#  Before this becomes an installable perl module, you can use it
#  in the same directory as the application by doing something like:
#
#    use FindBin qw($Bin);
#    use lib $Bin;
#    use XtrUtil;
#
#  Rodrigo Fonseca, October 2007
#


use 5.008006;
use strict;
use warnings;

require Exporter;

our @ISA = qw(Exporter);

# Items to export into callers namespace by default. Note: do not export
# names by default without a very good reason. Use EXPORT_OK instead.
# Do not simply export all your public functions/methods/constants.

# This allows declaration	use XtrUtil ':all';
# If you do not need this, moving things directly into @EXPORT or @EXPORT_OK
# will save memory.
our %EXPORT_TAGS = ( 'all' => [ qw(
parse_report 
parse_xtrace_md
process_graph
toposort
find_periodic
find_edge_subgraphs
) ] );

our @EXPORT_OK = ( @{ $EXPORT_TAGS{'all'} } );

our @EXPORT = ( @{ $EXPORT_TAGS{'all'} } );

our $VERSION = '0.01';


# Preloaded methods go here.
## parse_report: Separates a report in its key value pairs
#  @param: report
#  @param: separator (optional, default '\n')
#  @return: reference to a hash of keys
#           For each key there is an array of values, with 1 or more
#           entries
#           A couple of derived keys are created as well:
#           _Full: the raw report
#           _TaskId: the taskid
#           _OpId: the opid
#           _Parents: opids of all incoming edges
#           _ts: timestamps in the report

sub parse_report {
   my $report = shift;
   my $sep = shift;
   if (!defined $sep) {
      $sep = '\n';
   }
   my ($line, $key, $value);
   return unless ($report);
   my $result;
   $result->{'_Full'} = $report;
   for $line (split /$sep/s, $report) {
      #print "\t$line\n";
      ($key, $value) = $line =~ /^([^:]+):\s*(.*)$/;
      next unless defined $key;
      push @{$result->{$key}}, $value;
      #create derived keys
      if ($key eq 'X-Trace') {
          my ($taskId, $opId) = &parse_xtrace_md($value);
          push @{$result->{'_TaskId'}}, $taskId;
          push @{$result->{'_OpId'}}, $opId;
      } elsif ($key eq 'Timestamp') {
          my ($ts,$label) = $value =~ /(.*)\[(.*)\]/;
          if (!defined $ts) {
                ($ts, $label) = $value =~ /(.*),\w+(.*)/;
                if (!defined $ts) {
                    $ts = $value;
                }
          }
          if (!defined $label || $label eq 'event') {
              my $time = &parse_time($ts);
              if (defined $time) {
                  #$min_time = $time if (!defined $min_time|| $time < $min_time );
                  #$max_time = $time if (!defined $max_time|| $time > $max_time );
                  push @{$result->{'_ts'}}, $time;
              }
          }
      } elsif ($key eq 'Edge') {
          my @e = split ",",$value;
          push @{$result->{'_Parents'}},$e[0];
      }
   }
   return $result;
};

## parse_xtrace_md
# @param xtrmetadata
# @return ($taskid, $opid)

sub parse_xtrace_md {
    my ($xtrmetadata) = shift;
    my ($hflags,$flags);
    my ($taskid, $opid);
    ($hflags) = substr($xtrmetadata,0,2);
    $flags = hex($hflags);
    my @len = (4,8,12,20);
    my $len = $len[$flags & 3];
    $taskid = substr($xtrmetadata,2,$len*2);
    
    if ($flags & 8) {
       $opid   = substr($xtrmetadata,2 + $len*2, 16);
    } else {
       $opid   = substr($xtrmetadata,2 + $len*2, 8);
    }
    
    return ($taskid, $opid);
}


## dummy function
sub parse_time {
    my ($timestring) = shift;
    return $timestring;
}

sub i_parse_time {
   my ($timestring) = shift;
   my ($time, $msecs);
   $time = str2time($timestring);
   if (!defined $time) {
        return $time;
   }
   ($msecs) = $timestring =~ /\d\d:\d\d:\d\d\.(\d+)/;
   $time = $time.".$msecs";
   return $time;
}

########################################
# Graph processing: DFS, topological sort, subgraph construction
########################################  

# These functions find the nested structure of the graph, and the 
# subgraphs that correspond to each abstract edge. This is done by
# topologically sorting the nodes of the graph via a depth-first
# traversal (this is actually done twice to guarantee some properties
# of the DFS forest)
# In a second step, for each 'abstract' edge found, we determine the
# subgraph that is abstracts. These subgraphs can be nested.
# Each node only belongs to one subgraph, but the subgraphs themselves
# can encompass others.
# Reference for the $g object:
#   -------------------------------------------------------------------------------------
#   'nodes' - hash reference by 
#     $node_idx - index of node in the report order
#        'report' - reference to the full report hash for this node
#        'out' - array of out edges
#        'in'  - array of incoming edges
#        'outd' - outdegree
#        'ind'  - indegree
#        'd'    - DFS step at which node is first visited
#        'f'    - DFS step at which node is 'closed'
#        'order' - topological order of node
#        'subgraph' - array of nodes in the subgraph that starts at this node
#        'nest_parent' - root of the subgraph in which the subgraph starting at this node 
#                      is nested
#        'nest_subgraph' - array of subgraphs that are nested in this node's subgraph
#        'in_subgraph' - boolean of whether this node is part of a subgraph
#   'nodelist' - array of nodes sorted by report order
#   'toposort' - array of nodes sorted in topological order
#   'c_edges' - hash of reduntant edges
#     "node_start.node_end"
#       'order' - order in which the edge was found
#       'subgraph' - the subgraph that this edge abstracts
#   'c_edges_v' - array of abstract edges in order of insertion
#     [node_start,node_end]
#   ------------------------------------------------------------------------------------
#  

sub process_graph {
	my ($result) = @_;
	my $g; #hash reference for the graph
	my @nodes;
	my $node_idx;
	my %idx;
    my ($report,$opId);

	#get nodes with valid opId. if opIds are not unique,
	#the graph is probably going to be wrong. Right now the 
	#heuristic is to ignore all but the first occurrences.
	for ($node_idx = 0; $node_idx < scalar(@$result); $node_idx++) {
		$report = $result->[$node_idx];
		$opId = ${$report}{'_OpId'}->[0];
		next unless defined $opId;
		#print "process_graph:: $node_idx opId $opId\n";
		if (defined $idx{$opId}) {
			print STDERR "$opId doubly defined in graph!\n";
		} else {
			$idx{$opId} = $node_idx;
			$g->{'nodes'}->{$node_idx}->{'report'} = $report;
			#$g->{'nodes'}->{$idx} = {};
			push @{$g->{'nodelist'}}, $node_idx;
		}
	}
	#get edges
    my ($aref,$parent_opId, $from);
	for my $to (@{$g->{'nodelist'}}) {
		$report = $result->[$to];
		$aref = ${$report}{'_Parents'};
		for $parent_opId (@$aref) {
			$from = $idx{$parent_opId};
			next unless defined $from;
			#$g->{'edges'}->{$from}->{$node} = {};
			push @{$g->{'nodes'}->{$from}->{'out'}}, $to;
			$g->{'nodes'}->{$from}->{'outd'}++;
			push @{$g->{'nodes'}->{$to}->{'in'}}, $from;
			$g->{'nodes'}->{$to}->{'ind'}++;
		}
	}
	return $g;
}

# does DFS and topological sort of the graph
sub toposort {
	use strict;
	my ($g) = @_;
	my @toposort;
	my $dfs_visit;
	my %state;
	my $time;
	my $last_step;
	my $graph_chain;

	#dfs
	# states: undefined, open, closed
        
        # We do two DFS traversals. The difference is the order in which we
        # visit the branches of a given node. The first is simply in order of
        # the reports in the database (reversed), which is roughly by time.
        # The second uses the topological ordering obtained by the first. This
        # makes the nesting relationships properly nested (I conjecture). 
        # v_order is a hash mapping id -> topological order 

	$dfs_visit = sub {
		my ($node,$v_order) = @_;
		my ($next);
		my @children;
		my $opId = $g->{'nodes'}->{$node}->{'report'}->{'_OpId'}->[0];
		$state{$node} = 'open';
		$time++;
		#print "visit $node ($opId) time $time\n";
		$g->{'nodes'}->{$node}->{'d'} = $time;
		if ($last_step eq 'f') {
			$graph_chain++;
		}
		$g->{'nodes'}->{$node}->{'graph_chain'} = $graph_chain;
		$last_step = 'o';
		if (defined @{$g->{'nodes'}->{$node}->{'out'}}) {
			if (defined $v_order) {
				@children = sort {$v_order->{$a} <=> $v_order->{$b}}
                                            @{$g->{'nodes'}->{$node}->{'out'}};	
			} else {
				@children = (@{$g->{'nodes'}->{$node}->{'out'}});	
			}
			for $next (@children) {
				if (!defined $state{$next}) {
					&$dfs_visit($next, $v_order);
				} else {
					$g->{'c_edges'}->{"$node.$next"}->{'order'} = $time;
					push @{$g->{'c_edges_v'}},[$node,$next];
				}
			}
		}
		$state{$node} = 'closed';
		$g->{'nodes'}->{$node}->{'f'} = ++$time;
		$last_step = 'f';
		#print "close $node ($opId) time $time\n";
		unshift @{$g->{'toposort'}},$node;
	};

	my $node;
	my $order;
	my %v_order;

	undef %state;
	$time = 0;
	$order = 0;
	
	$last_step = 'f';
	$graph_chain = 0;
	for $node (@{$g->{'nodelist'}}) {
		next if defined $state{$node};	
		&$dfs_visit($node);
	}	

	$last_step = 'f';
	$graph_chain = 0;
	for $node (@{$g->{'toposort'}}) {
		$v_order{$node} = $order++;
	}
	
	# call dfs_visit again, with v_order as the order
	undef %state;
	$time = 0;
	$order = 0;

	@toposort = @{$g->{'toposort'}};
	undef $g->{'toposort'};
	undef $g->{'c_edges'};   # hash per edge of whether it was found closed

	undef $g->{'c_edges_v'}; # array of such edges, in order of discovery. 
                             # For the second time  around, if an edge A is nested 
                             # in an edge B, A will show up first in the array.

	for $node (@toposort) {
		next if defined $state{$node};
		&$dfs_visit($node,\%v_order);
	}
	for $node (@{$g->{'toposort'}}) {
		$g->{'nodes'}->{$node}->{'order'} = $order++;	
	}
}

## Marks all nodes that have PERIODIC in the label and computes the 
#  distribution. Marks all outliers as well.
sub find_periodic {
	use strict;
	my ($g) = @_;
	my ($label, $periodic,$ts);
	my ($pref, $last, $report, $occ, $delta);
	for my $node (@{$g->{'toposort'}}) {
		$report = $g->{'nodes'}->{$node}->{'report'};	
		$label = ${$report}{'Label'}->[0];
        	$ts = ${$report}{'_ts'}->[0];
		if (defined $label) {
			($periodic) = $label =~ /PERIODIC\s+(.*)/;
			if (defined $periodic) {
				push @{$g->{'periodic'}->{$periodic}->{'occ'}},[$node,$ts];
			}
			$g->{'nodes'}->{$node}->{'periodic'} = $periodic;
		}
	}
	for $periodic (keys %{$g->{'periodic'}}) {
		$pref = $g->{'periodic'}->{$periodic};
		for my $occ ( sort {$a->[1] <=> $b->[1]} 
                               @{$pref->{'occ'}}) {
			if (defined $last) {
				$delta = $occ->[1] - $last->[1];
				push @{$pref->{'deltas'}},
					[$last->[0],$delta];
				if (!defined $pref->{'dist'}) {
					$pref->{'dist'} =
						Statistics::Descriptive::Full->new();
				}
				$pref->{'dist'}->add_data($delta);
			};
			$last = $occ;	
		}
		undef $pref->{'occ'};
		my ($inf, $sup);
		if (defined $pref->{'dist'}) {
			$inf = $pref->{'dist'}->mean() - $pref->{'dist'}->standard_deviation();
			$sup = $pref->{'dist'}->mean() + $pref->{'dist'}->standard_deviation();
			for my $delta ( @{$pref->{'deltas'}} ) {
				if ($delta->[1] > $sup || $delta->[1] < $inf) {
					$pref->{'black'}->{$delta->[0]} = $delta->[1];
				}	
			}
		}
	}
}


# for each abstract edge found in toposort, find the subgraph
# that it abstracts. 
# Results: $g->{'nodes'}->{$n}->{'in_subgraph'} if n is part of a subgraph
#          $g->{'nodes'}->{$n}->{'subgraph'} if n is the root of a subgraph
#          $g->{'c_edges'}->{"$i.$j"}->{'subgraph'}: the root of the subgraph
#                                                    this edge abstracts
#
sub find_edge_subgraphs {
	use strict;
	my $g = shift;
	my ($edge_ref,$i,$j);
	my ($d,$f,$di,$dj,$fi,$fj,$c,$child);
	my ($dc, $c_order, $k, $n, $fn); 
	for $edge_ref (@{$g->{'c_edges_v'}}) {
		($i,$j) = @$edge_ref;
		#find the child of to that nests the edge
		$di = $g->{'nodes'}->{$i}->{'d'};
		$fi = $g->{'nodes'}->{$i}->{'f'};
		$dj = $g->{'nodes'}->{$j}->{'d'};
		$fj = $g->{'nodes'}->{$j}->{'f'};
		undef $child;
		for $c (@{$g->{'nodes'}->{$i}->{'out'}}) {
			$d = $g->{'nodes'}->{$c}->{'d'};
			$f = $g->{'nodes'}->{$c}->{'f'};
			if ($d < $dj && $f > $fj) {
				$child = $c;
				last;
			}
		}
		next unless defined($child); 
		#$child is the child of $i that has the DFS path to $j
		$dc = $g->{'nodes'}->{$child}->{'d'};
		#$fc = $g->{'nodes'}->{$child}->{'f'};
		# find all nodes that have d and f between dc and df,
		#  except j and descendents.
		# nodes are ordered inversely with f in toposort, so the loop
		#  below is going in decreasing order of f
		$c_order = $g->{'nodes'}->{$child}->{'order'}; #child's index in toposort
		for ($k = $c_order; ; $k++) {
			$n = $g->{'toposort'}->[$k];
			last if (!defined $n);
			$fn = $g->{'nodes'}->{$n}->{'f'};
			last if ($fn < $dc);  #end of nodes that can be in the graph
			next if ($fn <= $fj && $fn >= $dj); #skip nodes that are in the j branch
			if (defined $g->{'nodes'}->{$n}->{'subgraph'} &&
                            !defined $g->{'nodes'}->{$n}->{'nest_parent'}) {
				#$g->{'nodes'}->{$n}->{'nest_parent'} = $child;
				push @{$g->{'nodes'}->{$child}->{'nest_subgraph'}}, $n;
			}
			if (! defined $g->{'nodes'}->{$n}->{'in_subgraph'}) {
				$g->{'nodes'}->{$n}->{'in_subgraph'} = 1;
				push @{$g->{'nodes'}->{$child}->{'subgraph'}}, $n;
			}
		}
		$g->{'c_edges'}->{"$i.$j"}->{'subgraph'} = $child;
	}	
}


1;
__END__
# 

=head1 NAME

XtrUtil - Perl utilities to deal with X-Trace reports

=head1 SYNOPSIS

  use XtrUtil;
  blah blah blah

=head1 DESCRIPTION

Stub documentation for XtrUtil, created by h2xs. It looks like the
author of the extension was negligent enough to leave the stub
unedited.

Blah blah blah.

=head2 EXPORT

None by default.



=head1 SEE ALSO

Mention other useful documentation such as the documentation of
related modules or operating system documentation (such as man pages
in UNIX), or any relevant external documentation such as RFCs or
standards.

If you have a mailing list set up for your module, mention it here.

If you have a web site set up for your module, mention it here.

=head1 AUTHOR

Rodrigo Fonseca, E<lt>rfonseca@apple.comE<gt>

=head1 COPYRIGHT AND LICENSE

Copyright (C) 2007 by Rodrigo Fonseca

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself, either Perl version 5.8.6 or,
at your option, any later version of Perl 5 you may have available.


=cut

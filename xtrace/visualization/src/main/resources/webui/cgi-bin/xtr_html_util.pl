###########################
# Utility functions to process X-Trace reports and task graphs
###########################

#####
# Html output
#####
sub output_error {
   my $msg = shift;
   print header, start_html;
   print "<div class='error'>$msg</div>\n";
   print end_html;
   exit;
}


# Load db_config file
# format:
# dbkey <db-internal-name|'default'>
# key value pairs
# ...
# dbkey...
# At least one db SHOULD be named default
sub load_db_config {
	my $db_config_file = shift;
	my $dbkey = undef;
	my $db_config = undef;
	my ($key, $value);
	open DBIN, $db_config_file or return undef;
	while (<DBIN>) {
		chomp;
		next if /^\s*#/ || /^\s*\/\//;
		($key, $value) = $_ =~ /^\s*([^\s]+)\s(.*)$/;
		next unless (defined $key && defined $value);
		if (!defined $dbkey) {
			next unless ($key eq 'dbkey');
		}
		if ($key eq 'dbkey') {
			#print STDERR "dbkey $value\n";
			$dbkey = $value;
		} else {
			$db_config->{$dbkey}->{$key} = $value;
		}
	}	
	close DBIN;
	return $db_config;
}


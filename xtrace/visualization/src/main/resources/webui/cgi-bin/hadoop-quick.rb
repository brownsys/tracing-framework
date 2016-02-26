#!/usr/bin/env ruby

require 'cgi'
require 'erb'
require 'logger'
require 'net/http'
require 'socket'
require 'statistics2.rb'
require 'lib/gnuplot.rb'
require 'lib/htmlutils.rb'
require 'lib/reports.rb'
require 'lib/stats.rb'

DEBUG = true

# Create a CGI object with HTML4 output
cgi = CGI.new('html4')

# Figure out server URL
SERVER_URL = "http://#{cgi.server_name}:#{cgi.server_port}"
REPORTS_URL = "#{SERVER_URL}/reports/"

io = open("hadoop-quick.erb")
template = ERB.new(io.read)
io.close

cgi.out { template.result(binding) }


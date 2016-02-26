#!/usr/bin/ruby

require 'cgi'
require 'erb'
require 'net/http'
require 'socket'

load 'lib/reports.rb'

DOT_PATH = "/usr/bin/dot"         # TODO: Make this configurable
SERVER_URL = "http://localhost:4080"    # TODO: Don't hard-code server info

# Create a CGI object with HTML4 output
cgi = CGI.new('html4')

# Read HTTP parameters
taskid = cgi['taskid']
format = cgi['format']

# Get reports
response = Net::HTTP.get_response(URI::parse("#{SERVER_URL}/reports/#{taskid}"))
if not response.body or response.body.empty?
  cgi.out { <<-EOF
      <html>
      <head><title>X-Trace Graph for Task #{taskid}</title></head>
      <body>
      <p>TaskID not found: '#{taskid}'</p>
      </body>
      </html>
    EOF
  }
  exit
end

if format == ''
  # No format specified - print a HTML page with an applet for viewing the 
  # graph. The applet will invoke graph.rb with format=svg to run GraphViz 
  # and get the SVG image.
  cgi.out { <<-EOF
    <html>
    <head><title>X-Trace Graph for Task #{taskid}</title></head>
    <body>
    <h1>X-Trace Graph for task #{taskid}</h1>
    <applet code="net.claribole.zgrviewer.ZGRApplet.class"
            archive="/zvtm.jar,/zgrviewer.jar"
            width="1000" height="600">
       <param name="type" value="application/x-java-applet;version=1.4" />
       <param name="scriptable" value="false" />
       <param name="width" value="1000" />
       <param name="height" value="600" />
       <param name="svgURL" 
              value="#{SERVER_URL}/graph.rb?taskid=#{taskid}&format=svg" />
       <param name="title" value="ZGRViewer - Applet" />
       <param name="appletBackgroundColor" value="#DDD" />
       <param name="graphBackgroundColor" value="#DDD" />
       <param name="highlightColor" value="red" />
     </applet>
     <p>Download image as: 
       <a href="#{SERVER_URL}/graph.rb?taskid=#{taskid}&format=gif">GIF</a>,
       <a href="#{SERVER_URL}/graph.rb?taskid=#{taskid}&format=png">PNG</a>,
       <a href="#{SERVER_URL}/graph.rb?taskid=#{taskid}&format=pdf">PDF</a>
     </p>.
     </body>
     </html>
    EOF
  }
  exit
end

trace = Trace.new(response.body)
response = nil # Free memory

# Create Graphviz DOT specification for graph, using an ERB template
template = ERB.new '
digraph G {
  rankdir = LR;
  node [fontsize="9"]
  edge [fontsize="9"]
  <% trace.reports.each do |rep| %>
    "<%=rep.opid%>" [label="<%=rep.label%>\\n<%=rep.agent%>\\n<%=rep.host%>"]
    <% rep.children.each do |child| 
         time = "%.03fs" % [child.timestamp - rep.timestamp, 0].max
         color = "black"
         if (child == rep.children[0] and rep.children.size > 1 and
             child.label == rep.label.gsub(/start$/, "end"))
           color = "gray"
         end
    %>
      "<%=rep.opid%>"->"<%=child.opid%>" [label="<%=time%>", color="<%=color%>"]
    <% end %>
  <% end %>
}'
dot = template.result(binding)
trace = nil # Free memory

def run_dot(args, input)
  IO.popen("#{DOT_PATH} #{args}", "w+") do |io|
    io << input
    io.close_write
    return io.read
  end
end

# Send output to the web browser
case format
when 'dot': cgi.out('text/plain') { dot }
when 'svg': cgi.out('image/svg+xml') { run_dot("-Tsvg", dot) }
when 'gif': cgi.out('image/gif') { run_dot("-Tgif", dot) }
when 'png': cgi.out('image/png') { run_dot("-Tpng", dot) }
when 'pdf': cgi.out('application/pdf') { run_dot("-Tpdf", dot) }
else cgi.out('text/html') { "Unknown format: '#{format}'" }
end

class Trace
  attr_reader :reports, :taskid, :roots, :duplicates

  def initialize(text)
    @reports = text.split(/\n\n/).map{|x| Report.new(x)}
    @taskid = @reports[0].taskid

    # Create a list of reports by ID, also counting duplicates in the process
    @duplicates = 0
    @reports_by_id = {}
    @reports.each do |rep| 
      STDERR << rep.opid << "\n" if @reports_by_id[rep.opid]
      @duplicates += 1 if @reports_by_id[rep.opid]
      @reports_by_id[rep.opid] = rep
    end
    
    # Set up child and parent pointers
    @reports.each do |rep|
      valid_edges = rep.edges.select {|id| @reports_by_id.has_key? id}
      rep.parents = valid_edges.map {|id| @reports_by_id[id]}
    end
    @reports.each{|rep| rep.parents.each{|par| par.children << rep}}

    # Define roots as reports without parents
    @roots = @reports.select{|rep| rep.parents.empty?}

    # Perform a topological sort to assign nodes indices (from 0 to N-1)
    last_index = reports.length
    visit_postorder {|node| node.index = last_index -= 1}

    # Sort outgoing and incoming edges in order of topological index
    @reports.each{|rep| rep.children.sort!{|a,b| a.index <=> b.index}}
    @reports.each{|rep| rep.parents.sort!{|a,b| a.index <=> b.index}}
  end

  def text
    reports.map{|x| x.text}.join("\n\n")
  end

  def report(opid)
    @reports_by_id[opid]
  end

  # Visit the nodes in postorder, calling block on each one
  def visit_postorder(&block)
    stack = @roots.clone
    visited = {}
    order = []
    until stack.empty?
      node = stack.pop
      unless visited[node]
        visited[node] = true
        order << node
        node.children.each {|c| stack.push c}
      end
    end
    order.reverse_each {|node| yield node}
  end

  def start_time
    @reports.map{|x| x.timestamp}.min
  end

  def end_time
    @reports.map{|x| x.timestamp}.max
  end

  def get_func_cont_table
    cont_table = FuncContTable.new()
    @reports.each do |r|
      # look for edges coming from a return, going to a call.
      if r.label =~ / return$/
        last = r
        lasts_name = r.label.match(/(.*) return$/)[1]
        lasts_parent = r.parents[0]
        unless lasts_parent.nil?
          lasts_time = last.timestamp - lasts_parent.timestamp
        else
          lasts_time = -1
        end
        all_nxt = r.children.select{|n| n.label =~ / call$/}
        all_nxt.each do |nxt|
          nxts_name = nxt.label.match(/(.*) call$/)[1]
          #is nxt the beginning of a failed event? i.e. call and failed instead of call and return
          nxts_failed = nxt.children.select{|n| n.label =~ / failed$/ and
                                                n.agent == nxt.agent}
          if nxts_failed.empty?
            nxts_child = nxt.children.last
            nxts_time = nxts_child.timestamp - nxt.timestamp
            cont_table.add(lasts_name, lasts_time, nxts_name, nxts_time)
          else
            nxts_failed.each do |nxt_fail|
              nxts_child = nxt_fail
              nxts_time = nxts_child.timestamp - nxt.timestamp
              cont_table.add(lasts_name, lasts_time, 
                             nxts_name + "-FAILED", nxts_time)
            end
          end
        end
      #next handle the case where we have a fail--complete or fail--fail
      elsif r.label =~ / failed$/
        last = r
        lasts_name = r.label.match(/(.*) failed$/)[1]
        lasts_parent = r.parents[0]
        unless lasts_parent.nil?
          lasts_time = last.timestamp - lasts_parent.timestamp
        else
          lasts_time = -1
        end
        all_nxt = r.children.select{|n| n.label =~ / call$/}
        all_nxt.each do |nxt|
          nxts_name = nxt.label.match(/(.*) call$/)[1]
          #is nxt the beginning of a failed event? i.e. call and failed instead of call and return
          nxts_failed = nxt.children.select{|n| n.label =~ / failed$/ and
                                                n.agent == nxt.agent}
          if nxts_failed.empty?
            nxts_child = nxt.children.last
            nxts_time = nxts_child.timestamp - nxt.timestamp
            cont_table.add(lasts_name + "-FAILED", lasts_time, 
                           nxts_name, nxts_time)
          else
            nxts_failed.each do |nxt_fail|
              nxts_child = nxt_fail
              nxts_time = nxts_child.timestamp - nxt.timestamp
              cont_table.add(lasts_name + "-FAILED", lasts_time, 
                             nxts_name + "-FAILED", nxts_time)
            end
          end
        end
      end
    end
    return cont_table
  end

  def get_event_cont_table
    cont_table = EventContTable.new
    cont_table = EventContTable.new
    @reports.each do |report| 
      #record this edge between curr ea. report and its parent
      report.parents.each do |parent|
        cont_table.add(report, parent)
      end
      #also do one if it has no children
      if report.children.empty?
        cont_table.add(nil, report)
      end
    end
    return cont_table 
  end
  
  def get_fanin_cont_table
    cont_table = FaninContTable.new
    @reports.each do |report| 
    if report.parents.length > 1
      parents = []
        report.parents.sort.each do |parent|
          parents << (parent.label+"-"+parent.agent).gsub(/(: |tip_|task_)[^ ]*/, '')
        end
        cont_table.add((report.label+"-"+report.agent).gsub(/(: |tip_|task_)[^ ]*/, ''), parents)
      end
    end
    return cont_table
  end
end

class Report
  attr_reader :text, :fields, :taskid, :opid, :edges, :timestamp
  attr_accessor :children, :parents, :index

  def initialize(text)
    @text = text
    @fields = {}
    @edges = []
    @children = []
    @parents = []

    lines = text.split(/\n/)
    lines.shift # Skip first line (X-Trace header)
    lines.delete_if{|x| x =~ /^[ \t]*at/} # Skip "at" lines from stack traces
    lines.each do |line|
      if line =~ /([^:]*): (.*)/
        key, value = $1.downcase, $2
        
        # Handle special keys
        if key == "x-trace"
          flags = value[0..1].to_i(16)
          opid_len = (flags & 8) > 0 ? 16 : 8
          @taskid = value[2..-(opid_len+1)]
          @opid = value[-(opid_len)..-1]
        elsif key == "edge"
          @edges << value.split(",")[0]
        elsif key == "timestamp"
          @timestamp = value.to_f
        end

        if @fields[key]
          @fields[key] << value
        else
          @fields[key] = [value]
        end
      else
        puts "Bad report line: #{line}"
      end
    end
  end

  def method_missing(name)
    name = name.to_s
    if @fields[name]
      if @fields[name].length == 1
        return @fields[name][0]
      else
        return @fields[name]
      end
    else
      return nil
    end
  end

  def has_field(name)
    name = name.to_s
    if @fields[name]
      return true
    else
      return false
    end
  end

  # Find the topologically first descendant of node that matches predicate
  def next(&predicate)
    stack = [self]
    visited = {}
    until stack.empty?
      n = stack.pop
      unless visited[n]
        visited[n] = true
        return n if yield n
        n.children.reverse_each {|c| stack.push c}
      end
    end
    return nil
  end

  def <=> (other)
    self.label <=> other.label 
  end
end

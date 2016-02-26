# A class for keeping statistics of a given variable and computing its mean
# and standard deviation. Create a new StatCounter and add values to it
# using <<, then obtain the mean, standard deviation, number of points, etc
# using mean, std, variance, and n.
class StatCounter
  attr_reader :values

  def initialize(*args)
    if args.empty? 
      @values = []
    else
     if args[0].kind_of? Array then @values = args[0] 
     else raise "argument of type Array expected in StatCounter constructor"
     end
    end
  end
 
  def <<(value)
    if value.respond_to? :each
      value.each{|x| self << x}
    else
      @values << value.to_f
    end
    return self
  end

  def +(stat_counter)
    if stat_counter.kind_of? StatCounter then
       retval = StatCounter.new
       retval << @values + stat_counter.values 
       return retval 
    else 
      raise "bad parameter to + function of StatCounter, arg 2 is of type " +
          stat_counter.class.to_s
    end
  end

  def n; @values.length; end
  def sum; @values.inject(0) {|sum, x| sum + x} end
  def max; @values.max; end
  def min; @values.min; end
  def med; @values.sort[@values.length/2] end
  def mean; sum / n unless @values.empty?  end

  def variance
    return nil if @values.empty?
    sum_sq = @values.inject(0) {|sum, x| sum + x*x}
    mu = mean
    return sum_sq / n - mu * mu
  end

  def std
    Math.sqrt variance unless @values.empty?
  end

  def percentile(pct)
    @values.sort!
    return @values[[(n*pct/100.0).floor, n-1].min] unless @values.empty?
  end
end

# A hash containing named StatCounters. Simply use table[key] << value to
# add a value to the counter with the given key.
class StatTable < Hash
  def initialize()
    super {|hash, key| hash[key] = StatCounter.new}
  end

  def to_html(key_label = "Key")
    headers = [key_label, "Count", "Mean", "Med", "Dev", "Min", "Max", "95%"]
    values = self.select{|k,v| v.n > 0}.map do |k,v|
      [k, v.n] + [v.mean, v.percentile(50), v.std, v.min, v.max, v.percentile(95)].map{|x| "%.2f" % x}
    end
    html_table headers, values.sort{|a,b| a[0] <=> b[0]}
  end
end

# A hash containing functions on both axis with a count in each cell
class FuncContTable < Hash
  attr_reader :x_axis, :y_axis
  attr_writer :x_axis, :y_axis

  def initialize()
    super {|hash, key| hash[key] = StatCounter.new}
    @x_axis = Hash.new {|hash, key| hash[key] = 0} 
    @y_axis = Hash.new {|hash, key| hash[key] = 0} 
  end

  def add(last_name, last_time, next_name, next_time)
    self[[last_name, next_name, 1]] << last_time
    self[[last_name, next_name, 2]] << next_time
    @x_axis[next_name] += 1
    @y_axis[last_name] += 1
  end

  def to_html
    unless self.empty?
      table ="<script>function showContCellStats(cellname){
                currStyle = document.getElementById(cellname).style.display;
                if (currStyle == \"block\")
                  document.getElementById(cellname).style.display = \"none\"; 
                else
                  document.getElementById(cellname).style.display = \"block\";
              }</script>
              <table<tr><th><span style=\"color:green\">1st func</span>/
              <span style=\"color:blue\">following func</span></th>"
      x_axis.each_pair do |colname, colval|
        table+="<th><span style=\"color:blue\">#{colname}</span></th>"
      end
      table+="<th>TOTALS</th></tr>"
      col_count = {}
      row_count = {}
      y_axis.each_pair do |rowname, rowval|
        table+="<tr><th><span style=\"color:green\">#{rowname}</span></th>"
        x_axis.each do |colname, colval|
          if row_count[rowname].nil?
            row_count[rowname] = self[[rowname,colname,1]].n
          else 
            row_count[rowname] += self[[rowname,colname,1]].n
          end 
          if col_count[colname].nil? 
            col_count[colname] = self[[rowname,colname,1]].n
          else
            col_count[colname] += self[[rowname,colname,1]].n
          end
          if self[[rowname,colname,1]].n>0
            stats, name = "<table><tr>", ""
            [1,2].each do |i|
              if i==1 
                color = "green"; name = rowname
              else
                color = "blue"; name = colname
              end
              stats += "<td class=\"contTableCell\"><span style=\"color:" + color + "\">
                          <u>#{name}</u><br/>
                          mean:#{"%.03f"%self[[rowname,colname,i]].mean}<br/>
                          min:#{"%.03f"%self[[rowname,colname,i]].min}<br/>
                          max:#{"%.03f"%self[[rowname,colname,i]].max}<br/>
                        </span></td>"
            end
            stats += "</tr></table>"
            table+="<td onclick=\"showContCellStats(\'#{rowname+colname}\')\" style=\"cursor:pointer\">
                      <span style=\"font-weight:bold; text-decoration:underline;\"> 
                      #{self[[rowname,colname,1]].n}</span><br/>
                      <div id=\"#{rowname+colname}\" style=\"display:none\">
                        <span style=\"color:blue\">"+
                          stats +
                        "</span>
                      </div>
                    </td>"
          else
            table+="<td>&nbsp;</td>"
          end
        end
        table+="<td>#{row_count[rowname]}</td></tr>"
      end
      table += "<tr><th>TOTALS</th>"
      x_axis.each_pair do |colname, colval|
        table += "<td>" + col_count[colname].to_s + "</td>"
      end
      table += "<td>&nbsp;</td></tr></table>"
    else
      "<div>No data for contingency table</div>"
    end
  end

  # Returns a hash containing counts for each call of b from a,
  # where the key is represented as a string "a,b" instead of arrays
  # as we represent it internally.
  def to_json
    table = {}
    @y_axis.each_key do |rowname|
      @x_axis.each_key do |colname|
        counter = self[[rowname, colname, 1]]
        table["#{rowname},#{colname}"] = counter.values if counter.n > 0
      end
    end
    return table.to_json
  end
end

class EventContTable < Hash
  attr_reader :x_axis, :y_axis
  attr_writer :x_axis, :y_axis

  def initialize
    super {|hash, key| hash[key] = StatCounter.new}
    @x_axis = Hash.new {|hash, key| hash[key] = 0} 
    @y_axis = Hash.new {|hash, key| hash[key] = 0} 
  end

  def add(curr, parent)
    unless curr == nil
      curr_label = (curr.label + "-" + curr.agent).gsub(/(: |tip_|task_)[^ ]*/, '') 
      time_diff = curr.timestamp - parent.timestamp
    else
      curr_label = "NULL"
      time_diff = 0
    end   
    parent_label = (parent.label + "-" + parent.agent).gsub(/(: |tip_|task_)[^ ]*/, '')
    self[[curr_label, parent_label]] << time_diff
    @x_axis[parent_label] += 1 
    @y_axis[curr_label] += 1 
  end

  def to_html
    table = "<table><tr><th>&nbsp;</th>"
    @x_axis.each_key do |colname|
      table += "<th>#{colname}</th>"
    end
    table += "</tr>"
    @y_axis.each_key do |rowname|
      table += "<tr><th>#{rowname}</th>"
      @x_axis.each_key do |colname|
        val = self[[rowname, colname]]
        table += "<td>#{val.n}</td>"
      end
      table += "</tr>"
    end
    table += "</table>"
  end

  def call_counts
    table = {}
    @y_axis.each_key do |rowname|
      @x_axis.each_key do |colname|
        val = self[[rowname, colname]]
        table["#{rowname},#{colname}"] = val.n if val.n > 0
      end
    end
    return table
  end
end

class FaninContTable < Hash
  attr_reader :x_axis, :y_axis
  attr_writer :x_axis, :y_axis

  #this is a hash table which holds a copy of the edges of the graph, and outputs them
  def initialize
    super {|hash, key| hash[key] = 0}
  end

  def add(report_name, parents)
    key = report_name+parents.map{|parent| ","+parent}.to_s 
    self[key] += 1 
  end

  def to_html
    table = "<table>"
    self.each do |id, count|
      table += "<tr><th>#{id}</th><td>#{count}</td></tr>"
    end
    table += "</table>"
 end

  def fanin_counts
    return self 
  end
end

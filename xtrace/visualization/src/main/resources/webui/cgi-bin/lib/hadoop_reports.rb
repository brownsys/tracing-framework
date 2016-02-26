class HadoopReport
  #a list of reports
  attr_reader :json_tasks, :json_maps, :json_reduces, :suspects, :report_times,
              :maps, :reduces, :bad_tasks, :map_stats, :reduce_stats

  # accept an array of json formatted tasks
  def initialize (task_array)
    @maps, @reduces, @bad_tasks, @suspects, @report_times = [], [], [], [], []
    @map_stats, @reduce_stats = StatCounter.new, StatCounter.new
    @json_maps = task_array["maps"]
    @json_reduces = task_array["reduces"]
    @json_tasks = @json_maps + @json_reduces
    set_tasks
    set_suspects
    set_report_times
    set_stats
  end

  def length;     @json_tasks.length      end
  def empty?;     tasks.empty?            end
  def start_time; report_times.min/1000.0 end
  def end_time;   report_times.max/1000.0 end
  def duration;   end_time - start_time   end
 
  def tasks(*arg)
    if arg.empty?
      if not @maps.nil? and not @reduces.nil? then return @maps + @reduces
      else return []
      end
    else return tasks.select{|x| x.spec_state == arg[0]} 
    end
  end

  def maps(*arg)
    if arg.empty? then return @maps
    else return @maps.select{|x| x.spec_state == arg[0]} 
    end
  end

  def reduces(*arg)
    if arg.empty? then return @reduces
    else return @reduces.select{|x| x.spec_state == arg[0]} 
    end
  end

  def task_stats(*spec_type)
    tmp_map_stats, tmp_reduce_stats = @map_stats, @reduce_stats
    if not spec_type.empty?
      tmp_map_stats = map_stats(spec_type[0])
      tmp_reduce_stats = reduce_stats(spec_type[0])
    end
    
    if not tmp_map_stats.nil? and not tmp_reduce_stats.nil?
      return tmp_map_stats + tmp_reduce_stats
    else
      return []
    end
  end

  def reduce_stats(*spec_type)
    if spec_type.empty?
      return @reduce_stats
    else
      retval = StatCounter.new()
      @reduces.select{|x| x.spec_state == spec_type[0]}.each do |task|
        retval << task.duration
      end
      return retval
    end
  end
  
  def map_stats(*spec_type)
    if spec_type.empty?
      return @map_stats
    else
      retval = StatCounter.new()
      @maps.select{|x| x.spec_state == spec_type[0]}.each do |task|
        retval << task.duration
      end
      return retval
    end
  end

  def to_s
    out = "HadoopReport details:"
    if not @maps.empty? : out += " #{@maps.length} maps" end
    if not @reduces.empty? : out += ", #{@reduces.length} reduces" end
    return out
  end

  def set_report_times
    @json_tasks.each do |tip|
      tip["tasks"].each do |task|
        start_time = task["startTime"]
        end_time = task["finishTime"]
        @report_times << start_time if start_time != 0
        @report_times << end_time if end_time != 0
      end
    end
  end

  def set_tasks
   [@json_maps, @json_reduces].each_with_index do |task_type,i| 
     is_reduce = (i == 1)
     task_type.each do |tip|
       tip["tasks"].each do |task| 
         
         #create new task [tid, start_time, end_time, state, task_tracker]   
         curr_task_hash = {
              :id => task["taskId"],
              :start_time => task["startTime"],
              :end_time => task["finishTime"],
              :state => task["state"],
              :tracker => task["taskTracker"]}
         if is_reduce
           curr_task_hash.merge!({
              :shuffle_end_time => task["shuffleFinishTime"],
              :sort_end_time => task["sortFinishTime"]})
           curr_task = HadoopReduceTask.new(curr_task_hash)
         else
           curr_task = HadoopTask.new(curr_task_hash)
         end

         #figure out if curr_task was a suspect
         if curr_task.id =~ /(.*)(_[1-9][0-9]*)$/ then
           curr_task.is_retry = true
           #also mark the 0th attempt of this task as being suspect
           @suspects << $1 + "_0"
         end

         #catch bad timestamps
         if curr_task.start_time != 0 and curr_task.duration > 0 then
           #save the maps and reducers
           if is_reduce : @reduces << curr_task
           else @maps << curr_task
           end
         else
           @bad_tasks << curr_task
         end
       end
     end
   end
  end

  def set_suspects
    tasks.each do |x|
       x.is_suspect = !@suspects.find{|tid| tid == x.id}.nil?
    end
  end

  def set_stats
    [@maps, @reduces].each_with_index do |tasks,i|
      tasks.each do |task|
        if i == 0 : @map_stats << task.duration
        else @reduce_stats << task.duration
        end
      end
    end
  end
end


class HadoopTask
  attr_reader :start_time, :end_time, :state, :state, :id, :tracker, :is_retry, :is_suspect
  attr_writer :is_retry, :is_suspect

  def initialize (args)
    args.each do |key,val|
      instance_variable_set("@"+key.to_s, val) 
    end
  end

#  def initialize (task_id, start_time, end_time, state, tracker, is_retry=false)
#    @id = task_id
#    @start_time = start_time
#    @end_time = end_time
#    @state = state
#    @tracker = tracker
#    @is_retry = is_retry
#    @is_suspect = false
#  end

  def start_time;   @start_time/1000.0   end 
  def end_time;  @end_time/1000.0  end 

  def duration
    (@end_time-@start_time > 0 and @end_time-@start_time < 100000000) ? (@end_time-@start_time)/1000.0 : 0
  end 

  def is_success 
    @state == "SUCCEEDED" or @state == "RUNNING" ? true : false 
  end
  
  # there are 5 types of tasks to distinguish between:
  #   0) not-suspected, i.e. succeeded on first try --- :good
  #   1) suspected, succeeded --- :suspect_good
  #   2) suspected, killed --- :suspect_killed
  #   3) speculative, succeeded --- :spec_good
  #   4) speculative, killed --- :spec_killed
  def spec_state
    if is_success and not @is_retry and not @is_suspect
      return :good
    end
    
    if is_success and not @is_retry and @is_suspect then
      return :suspect_good
    end
   
    if not is_success and not @is_retry then
      return :suspect_killed
    end
  
    if is_success and @is_retry then
      return :spec_good
    end
 
    if not is_success and @is_retry then
      return :spec_killed
    end
  end
 
  def to_s
    out = ""
    instance_variables.each do |x|
      out << x + ": " + eval(x).to_s
    end
    return out
  end
end

class HadoopReduceTask < HadoopTask
  attr_reader :shuffle_end_time, :sort_end_time

  def initialize (args)
    args.each do |key,val|
      instance_variable_set("@"+key.to_s, val) 
    end
  end

  def shuffle_duration
    (@shuffle_end_time-@start_time > 0 and @shuffle_end_time-@start_time < 100000000) ? (@shuffle_end_time-@start_time)/1000.0 : 0
  end

  def sort_duration
    (@sort_end_time-@start_time > 0 and @sort_end_time-@start_time < 100000000) ? (@sort_end_time-@start_time)/1000.0 : 0
  end

end

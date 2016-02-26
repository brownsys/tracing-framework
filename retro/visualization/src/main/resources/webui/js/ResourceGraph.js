function ResourceGraph() {

  // User-modifiable parameters
  var lead_in = 5000; // lag time in milliseconds
  var window = 60000; // visible size in milliseconds
  var interval = 1000; // time in milliseconds between transition redraw
  var width = 800;
  var height = 600;

  // Non-modifiable parameters
  var margin = {top: 25, right: 50, bottom: 20, left: 10};
  var start_time = new Date().getTime();

  var sxAbsolute = d3.scale.linear();
  var sxRelative = d3.scale.linear().domain([start_time-window, start_time]);
  var sy = d3.scale.linear();

  // Draws the axes
  var timeformat = d3.time.format("%H:%M:%S");
  var xAxis = d3.svg.axis().scale(sxAbsolute).orient("bottom").ticks(3).tickFormat(function(d) { return timeformat(new Date(d)); });
  var yAxis = d3.svg.axis().scale(sy).orient("right");

  // Draws the series on screen
  var line = d3.svg.line().interpolate("linear")
  .x(function(d) { return sxRelative(d.time); })
  .y(function(d) { return sy(d.usage); });
  
  /*
   * Main rendering function
   */
  function graph(selection) {
    selection.each(function(dataset) { 
      var svg = d3.select(this).selectAll(".resourceviz").data([dataset]);

      // Create new elements if they don't exist
      var newsvg = svg.enter().append("svg").attr("class", "resourceviz");
      newsvg.append("svg").attr("class", "graph").attr("overflow", "hidden")
            .append("g").attr("class", "graph-series").attr("transform", "translate(0,0)");
      newsvg.append("text").attr("class", "title").attr("text-anchor", "middle");
      newsvg.append("g").attr("class", "x axis")
            .append("text").attr("y", -15).attr("dy", ".71em").text("Time");
      newsvg.append("g").attr("class", "y axis")
            .append("text").attr("transform", "rotate(-90)").attr("dy", ".71em").attr("text-anchor", "middle").attr("class", "title");
      newsvg.append("g").attr("class", "tenant axis");      
      svg.exit().remove();
      
      // Add a label per series
      var tenantlabels = svg.select(".tenant.axis").selectAll(".tenantlabel").data(dataset.series());
      tenantlabels.enter().append("text").attr("class", "tenantlabel");
      tenantlabels.exit().remove();
      tenantlabels.attr("fill", function(series) { return series.color(); }).on("click",function(series) {
        series.color(d3.rgb(Math.random()*255, Math.random()*255, Math.random()*255));
        d3.select(this).attr("fill", series.color());
        svg.select(".graph-series").selectAll("path").attr("stroke", function(series) { return series.color(); });
      });
      
      // Add a line per series
      var path = svg.select(".graph-series").selectAll("path").data(dataset.series());
      path.enter().append("path").attr("class", "series");
      path.attr("stroke", function(series) { return series.color(); });
      path.exit().remove();
      
      // Position the elements
      svg.attr("width", width).attr("height", height);
      svg.select(".graph").attr('x', margin.left).attr('y', margin.top)
                          .attr("height", height - margin.top - margin.bottom)
                          .attr("width", width-margin.left-margin.right);
      svg.select(".title").attr("x", width/2).attr("y", 17).text(dataset.name());
      svg.select(".x.axis").attr("transform", "translate("+margin.left+","+(height - margin.bottom)+")")
      svg.select(".y.axis").attr("transform", "translate("+(width - margin.right)+","+margin.top+")")
         .select("text").attr("y", margin.right-15).attr("x", -(height-margin.top-margin.bottom)/2)
                        .text(function(dataset) { return dataset.yname(); });
      svg.select(".tenant.axis").attr("transform", "translate("+margin.left+","+(margin.top+8)+")");

      // Update the y scale to make sure all of the data fits, and redraw it
      sy.domain([0, Math.max(dataset.miny(), dataset.extent(function(d) { return d.usage; })[1])]);
      svg.select(".y.axis").call(yAxis);
      
      // Update the line
      path.attr("d", function(series) { return line(series.data()); }); 
      
      // Update the label positions
      tenantlabels.attr("y", function(s, i) { return i * 15; })
                  .text(function(series) { return "— "+series.name(); });
    });
  }

  graph.transition = function(selection) {
    selection.each(function(dataset) { 
      var svg = d3.select(this).selectAll(".resourceviz");
      var graph = svg.select(".graph-series");

      // Update the scales
      var visible_range_to = new Date().getTime() - lead_in;
      var visible_range_from = visible_range_to - window;
      sxAbsolute.domain([visible_range_from, visible_range_to]);
      var offset = -sxRelative(visible_range_from);

      // Prune old data
      dataset.prune(visible_range_from - interval);


      // Transition the graph
      d3.select(this).select(".graph-series")
      .transition().ease("linear").duration(interval).attr("transform", "translate("+offset+")");

      // Transition the scale
      svg.select(".x.axis").transition().duration(interval).ease("linear").call(xAxis);
    });
  };


  /*
   * Getters and setters for settable variables and function
   */
  graph.width = function(_) { 
    if (!arguments.length) 
      return width; 
    width = _; 
    var xRange = [0, width-margin.left-margin.right];
    sxAbsolute.range(xRange);
    sxRelative.range(xRange);
    return graph;
  }
  
  graph.height = function(_) { 
    if (!arguments.length) 
      return height; 
    height = _; 
    var yRange = [height-margin.top-margin.bottom, 0];
    sy.range(yRange);
    return graph; 
  }

  graph.refreshinterval = function(_) { if (!arguments.length) return interval; interval = _; return graph; }
  graph.window = function(_) { if (!arguments.length) return window; window = _; sxRelative.domain([start_time-window, start_time]); return graph; }


  return graph;
}
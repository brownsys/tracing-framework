function StaticGraph() {

  // User-modifiable parameters
  var width = 800;
  var height = 600;
  var extent = [0, 1];

  // Non-modifiable parameters
  var margin = {top: 20, right: 100, bottom: 30, left: 20};
  var start_time = new Date().getTime();

  var sx = d3.scale.linear().domain(extent);
  var sy = d3.scale.linear();

  // Draws the axes
  var timeformat = d3.time.format("%H:%M:%S");
  var xAxis = d3.svg.axis().scale(sx).orient("bottom").tickFormat(function(d) { return timeformat(new Date(d)); });
  var yAxis = d3.svg.axis().scale(sy).orient("right");

  // Draws the series on screen
  var line = d3.svg.line().interpolate("linear")
  .x(function(d) { return sx(d.time); })
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
      newsvg.append("g").attr("class", "x axis")
            .append("text").attr("y", -15).attr("dy", ".71em").text("Time");
      newsvg.append("g").attr("class", "y axis")
            .append("text").attr("transform", "rotate(-90)").attr("dy", ".71em").attr("text-anchor", "middle");
      newsvg.append("g").attr("class", "tenant axis");      
      svg.exit().remove();
      
      // Add a label per series
      var tenantlabels = svg.select(".tenant.axis").selectAll(".tenantlabel").data(dataset.series());
      tenantlabels.enter().append("text").attr("class", "tenantlabel").attr("fill", function(series) { return series.color(); })
      tenantlabels.exit().remove();
      
      // Add a line per series
      var path = svg.select(".graph-series").selectAll("path").data(dataset.series());
      path.enter().append("path").attr("class", "series").attr("stroke", function(series) { return series.color(); });
      path.exit().remove();
      
      // Position the elements
      svg.attr("width", width).attr("height", height);
      svg.select(".graph").attr('x', margin.left).attr('y', margin.top)
                          .attr("height", height - margin.top - margin.bottom)
                          .attr("width", width-margin.left-margin.right);
      svg.select(".x.axis").attr("transform", "translate("+margin.left+","+(height - margin.bottom)+")")
      svg.select(".y.axis").attr("transform", "translate("+(width - margin.right)+","+margin.top+")")
         .select("text").attr("y", margin.right-20).attr("x", -(height-margin.top-margin.bottom)/2)
                        .text(function(dataset) { return dataset.yname(); });
      svg.select(".tenant.axis").attr("transform", "translate(0,"+margin.top+")");

      // Update the y scale to make sure all of the data fits, and redraw it
      sy.domain([0, dataset.extent(function(d) { return d.usage; })[1]]);
      svg.select(".y.axis").call(yAxis);
      svg.select(".x.axis").call(xAxis);
      
      // Update the line
      path.attr("d", function(series) { return line(series.data()); }); 
      
      // Update the label positions
      tenantlabels.attr("x", 5).attr("y", function(s, i) { return i * 20; })
                  .text(function(series) { return "— "+series.name(); });
    });
  }

  /*
   * Getters and setters for settable variables and function
   */
  graph.width = function(_) { 
    if (!arguments.length) 
      return width; 
    width = _; 
    var xRange = [0, width-margin.left-margin.right];
    sx.range(xRange);
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
  
  graph.extent = function(_) { if (!arguments.length) return extent; extent = _; sx.domain(extent); return graph; };

  return graph;
}
function SwimLaneControlPanelButton() {
  
  var width = 110;
  var height = 10;
  var border = 4;
  
  function button(selection, layout) {
    selection.each(function(d, i) {
      var rect = d3.select(this).select("rect");
      rect.attr("x", i*width + border).attr("width", width - border * 2).attr("y", border).attr("height", height - border * 2).classed("enabled", d.enabled);
      d3.select(this).select("text").text(d.enabled ? d.lenabled : d.ldisabled).attr("dominant-baseline", "middle")
      .attr("x", (i+0.5)*width).attr("y", height/2).attr("text-anchor", "middle");
    });
  };

  button.width = function(_) { if (!arguments.length) return width; width = _; return button; };
  button.height = function(_) { if (!arguments.length) return height; height = _; return button; };
  
  return button;
}

function SwimLaneControlPanel() {

	/* Default values for placement of the swimlane.  User should pass these */
	var x = 0;
	var y = 0;
	var width = 500;
	var height = 100;

	/* event callbacks */
	var callbacks = {
		"property": function(){},
	};
	
	var button = SwimLaneControlPanelButton();
	var buttons = [
	  {
	    "lenabled": "Edges: On",
	    "ldisabled": "Edges: Off",
	    "property": "showedges",
	    "enabled": true
	  },
    {
      "lenabled": "Events: On",
      "ldisabled": "Events: Off",
      "property": "showevents",
      "enabled": true
    },
    {
      "lenabled": "GC: On",
      "ldisabled": "GC: Off",
      "property": "showgc",
      "enabled": true
    },
    {
      "lenabled": "Spans: On",
      "ldisabled": "Spans: Off",
      "property": "showspans",
      "enabled": true
    },
    {
      "lenabled": "CriticalPath: On",
      "ldisabled": "CriticalPath: Off",
      "property": "showcpath",
      "enabled": false
    }
	];

	/* Main rendering function */
	function controlpanel(selection) {
		selection.each(function(layout) {
		  // Add the main control panel element
		  var cp = d3.select(this).selectAll(".controlpanel").data([layout]);
		  cp.enter().append("g").attr("class", "controlpanel");
		  cp.attr("transform", "translate("+x+","+y+")").attr("width", width).attr("height", height);
		  cp.exit().remove();
		});
	};
	
	controlpanel.refresh = function(selection) {
	  selection.each(function(layout) {
      // Add a container for the buttons, then add the buttons
      var cp = d3.select(this).selectAll(".controlpanel").data([layout]);
      var bs = cp.selectAll("g.button").data(buttons);
      var newbs = bs.enter().append("g").attr("class", "button");
      newbs.append("rect");
      newbs.append("text");
      bs.exit().remove();
      bs.call(button.height(height), layout);
      bs.on("click", function(d) { d.enabled = !d.enabled; callbacks["property"].call(this, d.property, d.enabled); });
	  });
	};

	controlpanel.on = function(evt, cb) {
		if (cb==null)
			return callbacks[evt];
		callbacks[evt] = cb;
		return controlpanel;
	};

	controlpanel.x = function(_) { if (!arguments.length) return x; x = _; return controlpanel; };
	controlpanel.y = function(_) { if (!arguments.length) return y; y = _; return controlpanel; };
	controlpanel.width = function(_) { if (!arguments.length) return width; width = _; return controlpanel; };
	controlpanel.height = function(_) { if (!arguments.length) return height; height = _; return controlpanel; };

	return controlpanel;    
}
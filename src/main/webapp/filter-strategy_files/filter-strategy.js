var map, filter, filterStrategy;
var animationTimer;
var currentDate;
// var startDate = new Date(1272736800000); // lower bound of when values
// var endDate = new Date(1272737100000); // upper value of when values
var startDate = new Date(0); // lower bound of when values
var endDate = new Date(20240000); // upper value of when values
var step = 60; // sencods to advance each interval
var interval = 0.03; // seconds between each step in the animation

function startAnimation() {
    if (animationTimer) {
        stopAnimation(true);
    }
    if (!currentDate) {
        currentDate = startDate;
    }
    var spanEl = document.getElementById("span");
    var next = function() {
    //	alert("working! current date " + currentDate + " end date " + endDate );
    	//var span = parseInt(spanEl.value, 10);
    	var span = 14;
        if (currentDate < endDate) {
            filter.lowerBoundary = currentDate;
            filter.upperBoundary = new Date(currentDate.getTime() + (span * 1000));
            filterStrategy.setFilter(filter);
            currentDate = new Date(currentDate.getTime() + (step * 1000));
        } else {
            stopAnimation(true);
        }
    };
    
    animationTimer = window.setInterval(next, interval * 1000);
}

function stopAnimation(reset) {
    window.clearInterval(animationTimer);
    animationTimer = null;
    if (reset === true) {
        currentDate = null;
    }
}

// add behavior to elements
document.getElementById("start").onclick = startAnimation;
document.getElementById("stop").onclick = stopAnimation;
var spanEl = document.getElementById("span");

var mercator = new OpenLayers.Projection("EPSG:900913");
var geographic = new OpenLayers.Projection("EPSG:4326");
map = new OpenLayers.Map("map");

var osm = new OpenLayers.Layer.OSM();

 filter = new OpenLayers.Filter.Comparison({
 type: OpenLayers.Filter.Comparison.BETWEEN,
 property: "when",
 lowerBoundary: startDate,
 upperBoundary: new Date(startDate.getTime() + (parseInt(spanEl.value, 10) *
 1000))
 });

 filterStrategy = new OpenLayers.Strategy.Filter({filter: filter});

var paths = new OpenLayers.Layer.Vector("Paths", {
projection: geographic,
strategies: [new OpenLayers.Strategy.Fixed(), filterStrategy],
//strategies: [new OpenLayers.Strategy.BBOX()],
protocol: new OpenLayers.Protocol.HTTP({
  //url: "paths_wgs84.geojson",
	url: "/service/agent-paths",
  format: new OpenLayers.Format.GeoJSON()
}),
styleMap: new OpenLayers.StyleMap({
  "default": new OpenLayers.Style({
      graphicName: "circle",
      pointRadius: 3,
      fillOpacity: 0.25,
      fillColor: "#ffcc66",
      strokeColor: "#ff9933",
      strokeWidth: 1
  })
}),
renderers: ["Canvas", "SVG", "VML"]
});


map.addLayers([osm, paths]);
map.setCenter(new OpenLayers.LonLat(144.570412433435773, -37.701804450869475).transform(geographic, mercator), 16);


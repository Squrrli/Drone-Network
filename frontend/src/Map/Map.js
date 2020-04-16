import React, { useEffect, useRef, useState } from "react";
import L, { marker } from "leaflet";

  const style = {
    width: "100%",
    height: "300px"
  };

  const mapbox_url =
   "https://api.mapbox.com/styles/v1/mapbox/dark-v9/tiles/{z}/{x}/{y}?access_token=pk.eyJ1Ijoic3F1cnJsaSIsImEiOiJjamhxY2lkeTcxOG9jMzdudTZkYm9jbmlkIn0.zvZPOE4XHex2E18F0FSdSg"

  const map_bounds = [
    [51.360007, -10.743767],
    [55.391440, -5.497318]
  ]

function Map({ markerPosition }) {
  // create map
  const mapRef = useRef(null);
  const [markers, setMarkers] = useState( [] );

  useEffect(() => {
    mapRef.current = L.map("map", {
      center: [53, -7],
      zoom: 7,
      minZoom: 7,
      bounds: map_bounds,
      maxBounds: map_bounds,
      layers: [
        L.tileLayer(mapbox_url, {
          attribution:
            '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
        })
      ]
    });
  }, []);

  // add marker
  const markerRef = useRef(null);
  useEffect(() => {
    console.log("map stop position: " +  markerPosition)
    markerRef.current = L.marker(markerPosition).addTo(mapRef.current);
    let new_state = markers.push(mapRef);
    // setMarkers(new_state)

    // if(markers.length > 2) {
      // draw line between currentRef and previous Ref
      // L.polyline([markerRef, markers[markers.length-1]]).addTo(mapRef)
    // }
  
  }, [markerPosition]);

  return <div id="map" style={style} />;
}

export default Map;

import React, { useEffect, useRef, useState } from "react";
import ReactDOMServer from "react-dom/server";
import ReactDOM from 'react-dom';
import L, { marker } from "leaflet";
import MissionForm from "../MissionForm";

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

function Map({baseStations}) {
  const mapRef = useRef(null);
  const originRef = useRef(null);
  const destRef = useRef(null);

  const [stationMarkers, setStationMarkers] = useState([])

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
    }).on("click", e => {
      let coords = e.latlng;
      if(e.originalEvent.ctrlKey === true)  addMarker('destinationIcon.svg', coords);
      else                                  addMarker('originIcon.svg', coords);

      if(originRef.current !== null && destRef.current !== null ) 
        ReactDOM.render(<MissionForm origin={originRef.current} dest={destRef.current}/>, document.getElementById('input-form'));
    });

    const addMarker = (iconUrl, coords) => {
      let myIcon = L.icon({
        iconUrl: iconUrl,
        iconSize: [50,50],
        iconAnchor: [25, 50]
      });
      // let m = L.marker(coords, {icon: myIcon}).addTo(mapRef.current);
      let m = L.marker(coords, {icon: myIcon});
      if (iconUrl === 'originIcon.svg') {
        if(originRef.current !== null)
          mapRef.current.removeLayer(originRef.current);
        m.addTo(mapRef.current);
        originRef.current = m;
      } else {
        if(destRef.current !== null)
          mapRef.current.removeLayer(destRef.current);
        m.addTo(mapRef.current);
        destRef.current = m;
      }
    };

    mapRef.current.doubleClickZoom.disable();
  }, []);

  useEffect(() => {
    let baseIcon = L.icon({
      iconUrl: 'wifi.svg',
      iconSize: [50,50],
      iconAnchor: [25, 50]
    });    

    for(let base of baseStations) {
      let [name, lat, lng] = base;
      if(lat != undefined ) {
        let m = L.marker([lat, lng], {icon: baseIcon}).addTo(mapRef.current).on('click', (e) => {
          console.log(e.latlng);
          var popup = L.popup().setContent(ReactDOMServer.renderToString(
            <ul>
              <li>Name: {name}</li>
              <li>Latitude: {lat}</li>
              <li>Longitude: {lng}</li>
            </ul>
          )).setLatLng(e.latlng);
          popup.openOn(mapRef.current);
          
        });
      }
    }
  
  }, [baseStations]);

  return <div id="map" style={style} />;
}

export default Map;

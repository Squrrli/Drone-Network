import React, {useState, useEffect} from "react";
import "./App.css";
import Map from "./Map/Map";

function App() {
  const [stations, setStations] = useState([]);

  // request stop location
  useEffect( () => {
    const fetchStations = () => {
      fetch("http://192.168.43.222:8888/get-stations")
        .then(response => {
          return response.json();
        }).then(json => {
          setStations(json);
        });
    };

    const postMission = (lat, lng, weight) => {
      fetch("http://192.168.43.222:8888/", {
        method: 'POST', // *GET, POST, PUT, DELETE, etc.
        mode: 'cors', // no-cors, *cors, same-origin
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          'lat': lat,
          'lng': lng,
          'weight': weight
        }) 
      })
        .then(response => {
          return response;
        }).then(json => {
          console.log(json);
        });
    };

      // postMission(100.0, 100.0, 2500);
      fetchStations();
  }, [])

  return (
      <div className="App">
        <Map baseStations={ stations } />
        <div id="input-form"></div>
      </div>
    );  

}

export default App;

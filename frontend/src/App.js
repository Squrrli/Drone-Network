import React, {useState, useEffect} from "react";
import "./App.css";
import Map from "./Map/Map";

function App() {
  const [stations, setStations] = useState([]);

  // request stop location
  useEffect( () => {
    const fetchStations = () => {
      fetch("http://192.168.43.100:8888/get-stations")
        .then(response => {
          return response.json();
        }).then(json => {
          setStations(json);
        });
    };
      fetchStations();
      setInterval(() => {
        fetchStations();
      }, 5000);
  }, [])

  return (
      <div className="App">
        <Map baseStations={ stations } />
        <div id="input-form"></div>
      </div>
    );  

}

export default App;

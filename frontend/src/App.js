import React, {useState, useEffect} from "react";
import "./App.css";
import ScoreTracker from "./ScoreTracker";
import logo from "./logo.svg";
import Map from "./Map/Map";

function App() {
  const [stop, setStop] = useState([0, 0])
  let markerPosition = {
    lat: stop[0],
    lng: stop[1]
  }

  // request stop location
  useEffect( () => {
    const fetchProperty = () => {
      fetch("http://127.0.0.1:5000/generateStop")
        .then(response => {
          return response.json();
        })
        .then(stopJSON => {
          markerPosition.lat = stopJSON[0];
          markerPosition.lng = stopJSON[1];
          setStop(stopJSON)
        });
    };
    setInterval(() => {
      fetchProperty()
    }, 500);
    fetchProperty();
  }, [])

    console.log(markerPosition)
    return (
      <div className="App">
        <ScoreTracker />
        <Map markerPosition={ markerPosition } />
      </div>
    );  

}

export default App;

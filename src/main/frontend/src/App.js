import React, {useState, useEffect} from "react";
import "./App.css";
import ScoreTracker from "./ScoreTracker";
import Map from "./Map/Map";
// import TcpSocket from 'react-native-tcp';
// const net = require('net');
import net from 'net';

function App() {
  const [socket, setSocket] = useState(net.createConnection({
    port: 8888,
    host: "127.0.0.1"  
  }));

  socket.on('data', function(data) {
    console.log('message was received', data);
  });
  
  socket.on('error', function(error) {
    console.log(`socket error: ${error}`);
  });
  
  socket.on('close', function(){
    console.log('Connection closed!');
  });

  return (
    <div className="App">
      <ScoreTracker />
      <Map />
    </div>
  );  

}

export default App;

import React from 'react'
import ReactDOM from 'react-dom'

export default class HelpMe extends React.Component {
    constructor(props) {
      super(props);
      this.state = {
        weight: 0,
      };
    }
  
    handleChange = (e) => {
      this.setState({
        weight: e.target.value
      });
    }

    handleSubmit = (e) => {
        e.preventDefault();
        let payloadWeight = parseFloat(this.state.weight);
        let distance = this.props.origin.getLatLng().distanceTo(this.props.dest.getLatLng());
        fetch("http://192.168.43.222:8888/", {
              method: 'POST', // *GET, POST, PUT, DELETE, etc.
              mode: 'cors', // no-cors, *cors, same-origin
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify({
                'origin': [
                  this.props.origin.getLatLng().lat,
                  this.props.origin.getLatLng().lng
                ],
                'dest': [
                  this.props.dest.getLatLng().lat,
                  this.props.dest.getLatLng().lng
                ],
                'distance': distance,
                'weight': payloadWeight
              }) 
            })
        .then(response => {
            console.log(response);
            return response;
        })
        .then(json => {
            console.log(json);
        });

    }
  
    render() {
      const labelStyle = {
        //   display: 'block',
          marginRight: '20px'
      }

      return (
        <div>
          <form onSubmit={this.handleSubmit}>

            <label style={labelStyle} >Origin Latitude:</label>
            <input type="text" name="lat"disabled value={this.props.origin.getLatLng().lat}/>

            <label style={labelStyle} >Origin Longitude:</label>
            <input type="text" name="lng"disabled value={this.props.origin.getLatLng().lng}/>
            
            <label style={labelStyle} >Dest Latitude:</label>
            <input type="text" name="lat"disabled value={this.props.dest.getLatLng().lat}/>

            <label style={labelStyle} >Dest Longitude:</label>
            <input type="text" name="lng"disabled value={this.props.dest.getLatLng().lng}/>
            
            <input
              type="text"
              placeholder="Payload Weight"
              value={this.weight}
              onChange={this.handleChange}
            />
            <button onClick={this.handleSubmit}>Submit</button>
            {/* {showMessage && <div>{message}</div>} */}
          </form>
        </div>
      )
    }
  }
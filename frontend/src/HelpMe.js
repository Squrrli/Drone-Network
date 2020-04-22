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
        fetch("http://192.168.43.222:8888/", {
              method: 'POST', // *GET, POST, PUT, DELETE, etc.
              mode: 'cors', // no-cors, *cors, same-origin
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify({
                'lat': this.props.latlng.lat,
                'lng': this.props.latlng.lng,
                'weight': payloadWeight
              }) 
            })
        .then(response => {
                return response;
            })
        .then(json => {
                console.log(json);
            });

    }
  
    render() {
      const { showMessage, message } = this.state;
      const labelStyle = {
        //   display: 'block',
          marginRight: '20px'
      }

      return (
        <div>
          <form onSubmit={this.handleSubmit}>

            <label style={labelStyle} >Latitude:</label>
            <input type="text" name="lat"disabled value={this.props.latlng.lat}/>

            <label style={labelStyle} >Longitude:</label>
            <input type="text" name="lng"disabled value={this.props.latlng.lng}/>
            
            
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
import React, { useState, useEffect } from "react";

export default function MissionForm() {
  let [weightInput, setWeight] = useState(0);

  useEffect(() => {


  }, []);

  // const  = (event) => {
  //   event.preventDefault();
  //   console.log("Posting Mission details to backend.");
  // }

  return (
    <form onSubmit={postMission1}>
        <label>
          Name:
          <input type="text" onChange={setWeight}/>
          </label>
        <input type="submit" value="Submit" />
      </form>
  );
}

function postMission1(event) {
  event.preventDefault();
  console.log("Posting Mission details to backend.");

}

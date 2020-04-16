import React, { useState, useEffect } from "react";

export default function ScoreTracker() {
  let [timer, setTimer] = useState(0);
  let [score, setScore] = useState(0);

  useEffect(() => {
    setInterval(() => {
      setTimer(timer++);
    }, 1000);
    setInterval(() => {
      setScore(score++);
    }, 500);
  }, []);

  return (
    <header className="score-header">
      <h2 className="timer">Time: {timer}</h2>
      <h2 className="score">Score: {score}</h2>
    </header>
  );
}

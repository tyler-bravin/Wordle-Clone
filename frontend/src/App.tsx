import { useState } from "react";
import type { GameMode } from "./types/game";
import { Game } from "./components/Game";
import "./App.css";

function App() {
  const [mode, setMode] = useState<GameMode>("DAILY");

  return (
    <div className="app">
      {/* Remounting on mode switch keeps each mode's game/stats state fully independent. */}
      <Game key={mode} mode={mode} onModeChange={setMode} />
      <p className="app__disclaimer">
        Unofficial fan-made clone, built as a portfolio project · not affiliated with the NYT or Wordle
      </p>
    </div>
  );
}

export default App;

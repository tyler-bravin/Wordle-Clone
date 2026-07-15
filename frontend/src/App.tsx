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
    </div>
  );
}

export default App;

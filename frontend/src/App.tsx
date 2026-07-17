import { useState } from "react";
import type { GameMode } from "./types/game";
import { Game } from "./components/Game";
import { Header } from "./components/Header";
import { CreateCustomPuzzle } from "./components/CreateCustomPuzzle";
import "./App.css";

type View = { type: "game"; mode: GameMode; puzzleId?: string } | { type: "create" };

/** No router library - a `/custom/{puzzleId}` path is the only route this app has,
 * and nginx already SPA-falls-back any path to index.html (see frontend/nginx.conf). */
function parseInitialView(): View {
  const match = window.location.pathname.match(/^\/custom\/([^/]+)\/?$/);
  if (match) {
    return { type: "game", mode: "CUSTOM", puzzleId: match[1] };
  }
  return { type: "game", mode: "DAILY" };
}

function App() {
  const [view, setView] = useState<View>(parseInitialView);

  const goToMode = (mode: GameMode) => {
    // Only DAILY/ENDLESS are ever navigated to this way - leaving a Custom
    // puzzle or the creation form should drop the /custom/{id} path so a
    // refresh from here lands back on Daily, not the puzzle just left.
    if (window.location.pathname !== "/") {
      window.history.pushState({}, "", "/");
    }
    setView({ type: "game", mode });
  };

  return (
    <div className="app">
      {view.type === "create" ? (
        <div className="terminal">
          <Header
            mode="DAILY"
            onModeChange={goToMode}
            statusLine="fill in a word and guess count, then share the link"
            creating
          />
          <div className="game__keyboard-area">
            <CreateCustomPuzzle onClose={() => goToMode("DAILY")} />
          </div>
        </div>
      ) : (
        // Remounting on mode/puzzle switch keeps each game's state fully
        // independent - see useGame's per-mode/per-puzzle localStorage keys.
        <Game
          key={view.mode === "CUSTOM" ? `custom-${view.puzzleId}` : view.mode}
          mode={view.mode}
          onModeChange={goToMode}
          puzzleId={view.puzzleId}
          onCreateCustom={view.mode === "CUSTOM" ? undefined : () => setView({ type: "create" })}
        />
      )}
      <p className="app__disclaimer">
        Unofficial fan-made clone, built as a portfolio project · not affiliated with the NYT or Wordle
      </p>
    </div>
  );
}

export default App;

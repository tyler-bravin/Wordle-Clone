import { useEffect, useState } from "react";
import { dictionaryApi } from "../api/client";
import type { WordDefinition } from "../types/game";
import "./Definition.css";

interface DefinitionProps {
  word: string;
}

type Status = "loading" | "found" | "unavailable";

export function Definition({ word }: DefinitionProps) {
  const [status, setStatus] = useState<Status>("loading");
  const [definition, setDefinition] = useState<WordDefinition | null>(null);

  useEffect(() => {
    let cancelled = false;
    setStatus("loading");

    dictionaryApi
      .getDefinition(word)
      .then((result) => {
        if (cancelled) return;
        setDefinition(result);
        setStatus(result.found ? "found" : "unavailable");
      })
      .catch(() => {
        if (!cancelled) setStatus("unavailable");
      });

    return () => {
      cancelled = true;
    };
  }, [word]);

  if (status === "loading") {
    return (
      <div className="definition definition--loading">
        <span className="definition__prompt">$</span> looking up {word}...
      </div>
    );
  }

  if (status === "unavailable" || !definition) {
    return (
      <div className="definition definition--unavailable">
        <span className="definition__prompt">$</span> no dictionary entry found for {word}
      </div>
    );
  }

  return (
    <div className="definition">
      <div className="definition__header">
        <span className="definition__prompt">$</span> cat definition.log
        {definition.phonetic && <span className="definition__phonetic">{definition.phonetic}</span>}
      </div>
      <ul className="definition__list">
        {definition.meanings.map((meaning, i) => (
          <li key={i} className="definition__entry">
            <span className="definition__pos">{meaning.partOfSpeech}</span>
            <span className="definition__text">{meaning.definition}</span>
            {meaning.example && <span className="definition__example">"{meaning.example}"</span>}
          </li>
        ))}
      </ul>
    </div>
  );
}

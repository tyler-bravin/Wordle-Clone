# Wordle Clone

[![CI](https://img.shields.io/github/actions/workflow/status/tyler-bravin/Wordle-Clone/ci.yml?style=for-the-badge&label=CI)](https://github.com/tyler-bravin/Wordle-Clone/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](https://github.com/tyler-bravin/Wordle-Clone/blob/master/LICENSE)

A Wordle clone with a **Java/Spring Boot** backend and a **React/TypeScript** frontend, self-hosted via **Docker** on Coolify. Built around the same "engineering workstation" aesthetic as the rest of my projects — graphite surfaces, an amber terminal accent, IBM Plex Mono — styled as a fake shell session complete with a blinking prompt and `cat`-style output for stats and word definitions.

> **Disclaimer:** this is an unofficial portfolio/learning project. "Wordle" is a trademark of The New York Times Company. Not affiliated with, endorsed by, or sponsored by the NYT or the creators of Wordle — it recreates the *mechanics* of the game to demonstrate full-stack development, not for public or commercial use. See [`LICENSE`](./LICENSE) for what license terms actually apply (and don't apply) here.

<br>

<img src="./assets/daily-mode.png" alt="Daily mode, solved in 3/6 with definition lookup, stats, and the live countdown to the next word" width="49%" style="vertical-align: top;"> <img src="./assets/endless-mode.png" alt="Endless mode, round 19 with a 21-game win streak, definition lookup, and guess distribution" width="49%" style="vertical-align: top;">

---

### ✨ Key Features
* **Daily Mode**: One shared word per calendar day, the same for every player — derived server-side from a fixed-seed shuffle of the answer list, so it's stable across restarts but not just alphabetical. Once finished, a live countdown to the next UTC-midnight reset replaces the usual "~24h" guess.
* **Endless Mode**: Unlimited rounds via a per-player shuffle bag — every answer word gets dealt exactly once before the bag reshuffles, so nothing repeats until you've genuinely seen the whole pool.
* **Custom Puzzles**: Pick any real English word (3-8 letters), a guess count (3-8), and an expiry (1-48h — links aren't permanent), and get a shareable `/custom/{id}` link anyone can play — the answer word is validated against the same dictionary API used for definitions and checked against a moderation denylist at creation time, and guesses are checked against a bundled ~149k-word general English dictionary rather than the 5-letter-only Daily/Endless list.
* **Hard Mode**: Toggle it in the titlebar for Daily/Endless, or bake it into a Custom link at creation time. Flipping the toggle before your first guess applies it to the game already on screen; after that it's locked in for the rest of that game (a toast explains why) and only takes effect on your next one. Once a letter's revealed green or yellow, every later guess must actually use that information — enforced server-side, with a specific rejection message (e.g. "5th letter must be S") for a guess that doesn't.
* **Server-Authoritative Guessing**: The answer is never sent to the client until a game ends — every guess is scored on the backend, so it can't be read out of the network tab mid-game.
* **Word Definition Lookup**: Once a game ends, the result panel fetches a definition for the answer from a free external dictionary API (falling back to a second source for words the first one's dataset tends to miss), rendered as another terminal-log-style block.
* **Persistent Stats**: Win rate, streak, and guess distribution tracked per mode in `localStorage`, styled as a `cat stats.log` readout.
* **Terminal-Styled UI**: Titlebar, blinking prompt, and `[daily]` / `[endless]` / `[+ custom]` mode tabs — the same shell-session framing device used across my other projects.
* **Skip-Letter Input**: Press Space (or the right arrow) to leave a gap for a letter you're unsure of and keep typing, then use the left/right arrows or click any tile in the row to jump back and fill it in - à la [lessgames.com's Wordless](https://lessgames.com/wordless).
* **Fully Fluid Layout**: Tile and key sizing scale continuously with viewport width (`clamp()`), rather than jumping at a single breakpoint, so it holds up from small phones to ultrawide monitors.
* **Subtle Sound Effects**: Synthesized on the fly with the Web Audio API rather than sample files - soft key ticks, per-tile reveal tones pitched by result, and a short win/lose sting timed to land after the flip cascade finishes. A `[sound]`/`[muted]` toggle in the titlebar turns it all off, persisted in `localStorage`.
* **Dockerized & Coolify-Ready**: Multi-stage Dockerfiles for both services plus a `docker-compose.yaml` that doubles as the Coolify deployment target — see the Deployment section below.
* **Redis-Backed Sessions**: Game sessions and Endless shuffle bags are persisted in Redis rather than backend heap, so an in-progress game survives a backend restart/redeploy — and the backend could scale to multiple instances without losing session state.

---

### 💻 Technologies Used
* **Backend**: Java 21, Spring Boot 3 (Web, Validation, Actuator, Data Redis), Maven
* **Frontend**: React 19, TypeScript, Vite
* **Styling**: Plain CSS with custom properties, `@fontsource` (Archivo + IBM Plex Mono, self-hosted, no external font CDN)
* **Testing**: JUnit 5 + AssertJ (backend), `oxlint` + `tsc` (frontend)
* **Deployment**: Docker (multi-stage builds), nginx (static frontend serving), Redis, Coolify + Traefik

---

### 🧠 How It Works

The backend never sends the answer word to the client until a game ends, so it can't be read out of the network tab mid-game — each guess is scored server-side and only per-letter CORRECT/PRESENT/ABSENT feedback comes back.

**Daily mode** derives a deterministic answer from the calendar date: the 2,315-word answer list is shuffled once at startup with a fixed seed, and the day index (days since a fixed epoch) picks a word from that shuffled order.

**Endless mode** uses a per-player shuffle bag: a randomly-ordered queue of every answer word. Each round pops the next word off the queue; once it's empty, a fresh shuffle refills it — guaranteeing the full answer pool gets seen before anything repeats, without ever showing the same order twice.

**Custom mode** is different from the other two in one important way: Daily/Endless only accept guesses that are in the curated `allowed.txt` dictionary (~14.8k 5-letter words), but a Custom puzzle's word can be any length from 3-8, so guesses are checked against a separate, much broader ~149k-word general English list (`custom_guesses.txt`) instead. The puzzle's *answer* is validated differently - once, at creation time, by looking it up via the same dictionary API used for the post-game definition - and since that live API and the bundled guess list aren't guaranteed to agree on every word, the answer itself is always accepted as a guess regardless of whether it's in the bundled list. A short hand-curated denylist blocks obviously offensive words at creation too. See `CustomPuzzleService`'s Javadoc for the full reasoning.

All three modes share the same guess-scoring logic (`GuessEvaluator`), which handles duplicate letters the way real Wordle does — see `GuessEvaluatorTest` for the specific edge cases.

**Hard Mode** is enforced by a separate `HardModeValidator`, checked before a guess is scored - a rejected guess isn't recorded and doesn't cost an attempt. It's baked into a session (like `maxGuesses`) rather than being a live setting on every request: for Daily/Endless it's a `localStorage` preference, and for Custom it's the creator's choice, stored on the puzzle itself. A Daily/Endless session's setting can still be changed after it's created, but only up until the first guess reveals a hint - since Hard Mode's whole job is enforcing constraints derived from *earlier* guesses, changing it mid-game would mean judging a guess against hints that arrived under a different rule. Flip the titlebar toggle before typing anything and it applies to the game on screen; flip it after and the game keeps whatever setting it already had, with a toast explaining that the change applies to your next game instead (Endless) or isn't available right now (Daily, which has no "next" game to apply it to until tomorrow).

---

### 🛠️ Installation & Setup

Follow these steps to get a local copy of the project up and running.

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/tyler-bravin/Wordle-Clone.git
    cd Wordle-Clone
    ```

2.  **Run the backend:**
    ```bash
    cd backend
    mvn spring-boot:run
    ```
    Runs on `http://localhost:8080`. Config lives in `src/main/resources/application.yml`.

    > **Note:** `wordle.epoch-date` and `wordle.shuffle-seed` fix the Daily word cycle. Don't change either once real players have started playing Daily puzzles, or the day-to-word mapping will shuffle.

3.  **Run the frontend** (in a second terminal):
    ```bash
    cd frontend
    npm install
    npm run dev
    ```
    Runs on `http://localhost:5173` and talks to the backend via `VITE_API_BASE_URL` (defaults to `http://localhost:8080` in the committed `.env.development` — see `frontend/.env.example` for what other env vars exist).

4.  **Or run both together with Docker:**
    ```bash
    docker compose up --build
    ```
    Frontend on `http://localhost:5173`, backend on `http://localhost:8080`.

5.  **Run the tests:**
    ```bash
    cd backend && mvn test
    cd frontend && npm run build && npm run lint
    ```
    Backend tests cover guess-evaluation edge cases and the Endless shuffle bag's no-repeat guarantee. `tsc -b` type-checks as part of the frontend build; `oxlint` covers linting.

    This also runs automatically on every push via GitHub Actions (`.github/workflows/ci.yml`) - see the CI badge at the top of this README.

---

### 🚀 Deployment

This assumes a Coolify instance with a domain you control and DNS already pointed at it.

1.  **Create a new resource in Coolify**, type **Docker Compose**, pointed at this repo's root `docker-compose.yaml`. This also brings up a `redis` service — it needs no domain, only `backend` talks to it, over the compose network.
2.  **Assign domains** per service in the Coolify UI — `backend` (e.g. `wordle-api.yourdomain.dev`) and `frontend` (e.g. `wordle.yourdomain.dev`). Coolify provisions Let's Encrypt certs and Traefik routing automatically.
3.  **Set environment variables** on `backend`:
    * `WORDLE_ALLOWED_ORIGINS` = the frontend's final domain — the API rejects browser requests from anywhere else.
4.  **Set the build argument** on `frontend`:
    * `VITE_API_BASE_URL` = the backend's final domain. This is baked into the static bundle at build time, so it must be set *before* deploying, and redeployed if the backend domain ever changes.
5.  **Deploy.** Coolify builds both Dockerfiles and starts the stack.

---

### ⚠️ Known Simplifications

This is a portfolio project, not a production service, so a few corners were cut deliberately:

* **No accounts**: Endless mode's no-repeat guarantee is scoped to a `playerId` in `localStorage`, not a real user account. Game state lives in Redis keyed by `gameId`/`playerId` with a TTL (`WORDLE_SESSION_TTL` / `WORDLE_BAG_TTL`), rather than anything tied to a real user, since there's no login to tie it to.
* **Third-party dictionary dependency**: The definition lookup depends on two free external APIs (a Wiktionary-backed fallback covers words the primary source's dataset tends to skip, like common function words) with no SLA between them. It's deliberately isolated from actual gameplay, so this only affects that one bonus feature — see `DictionaryService`'s Javadoc.
* **No abuse protection on Custom puzzle creation**: no rate-limiting, no CAPTCHA — anyone can mint `/custom/{id}` links as fast as they like. The denylist (`backend/src/main/resources/words/denylist.txt`) is a short, hand-curated baseline, not a claim of comprehensive moderation.

---

### 📜 License
The original code in this repository is licensed under the MIT License — see [`LICENSE`](./LICENSE) for details. That license doesn't extend to the bundled word lists (own MIT licenses from their original sources — see [`backend/src/main/resources/words/README.md`](./backend/src/main/resources/words/README.md)) or to the word "Wordle" itself, which is a trademark of The New York Times Company.

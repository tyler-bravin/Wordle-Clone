# Wordle Clone

> **This is an unofficial portfolio/learning project.** "Wordle" is a
> trademark of The New York Times Company. This project is not affiliated
> with, endorsed by, or sponsored by the NYT or the creators of Wordle - it
> recreates the *mechanics* of the game to demonstrate full-stack
> development, and isn't intended for public or commercial use. See
> `LICENSE` for what license terms actually apply to (and don't apply to)
> this repo.

A Wordle clone with **Daily** (one shared word per day, like the original) and
**Endless** (unlimited rounds, no repeats until you've seen every word) modes.

- **Backend:** Java 21 / Spring Boot 3, plain REST API, in-memory game state
- **Frontend:** React 19 / TypeScript / Vite, no UI framework dependency
- **Deployment:** Docker + Coolify (Traefik-routed, wildcard SSL)

## How it works

The backend never sends the answer word to the client until a game ends, so
the answer can't be read out of the network tab mid-game. Each guess is
scored server-side and only the per-letter CORRECT/PRESENT/ABSENT feedback is
returned.

**Daily mode** derives a deterministic answer from the calendar date: the
2,315-word answer list is shuffled once at startup with a fixed seed, and the
day index (days since a fixed epoch) picks a word from that shuffled order.
Everyone playing on the same day gets the same word - restarting the backend
doesn't change it.

**Endless mode** uses a per-player "shuffle bag": a randomly-ordered queue of
every answer word. Each round pops the next word off the queue; once it's
empty, a fresh shuffle refills it. This guarantees you see the whole answer
pool before anything repeats, without ever showing the same order twice. The
client persists a `playerId` (see `useGame.ts`) so refreshing the page or
clicking "play again" keeps drawing from the same bag instead of starting a
new one.

Both modes share the same guess-scoring logic (`GuessEvaluator`), which
handles duplicate letters the way real Wordle does - see
`GuessEvaluatorTest` for the specific edge cases.

Once a game ends, the frontend fetches a definition for the answer word from
a free external dictionary API (`DictionaryService`), shown as a small
"what did that word mean" panel. This is the only place the backend calls a
third party, and it's deliberately kept off the actual gameplay path - see
that class's Javadoc for why.

## Project layout

```
backend/    Spring Boot API (Java 21, Maven)
frontend/   React + TypeScript app (Vite)
docker-compose.yml   local full-stack run / Coolify deployment target
```

## Running locally

### Backend

```bash
cd backend
mvn spring-boot:run
```

Runs on `http://localhost:8080`. Config lives in
`src/main/resources/application.yml` - notably `wordle.allowed-origins` for
CORS and `wordle.epoch-date` / `wordle.shuffle-seed`, which control the daily
word cycle (don't change these once real players have started playing daily
puzzles, or the day-to-word mapping will shuffle).

Run tests with `mvn test`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173` and talks to the backend via
`VITE_API_BASE_URL` (defaults to `http://localhost:8080` in `.env.development`).

### Both together, via Docker

```bash
docker compose up --build
```

Frontend on `http://localhost:5173`, backend on `http://localhost:8080`.

## Deploying to Coolify

This assumes a Coolify instance with a domain (or subdomain) you control and
DNS already pointed at it - see the backend's own case study on
`tylerbravin.dev` for the underlying Traefik/Cloudflare DNS-01 setup.

1. **Push this repo to GitHub** (or GitLab) if you haven't already.
2. **Create a new resource in Coolify** and point it at this repository, type
   **Docker Compose**, using the root `docker-compose.yml`.
3. **Assign domains** in the Coolify UI:
   - `backend` service → e.g. `wordle-api.yourdomain.dev`
   - `frontend` service → e.g. `wordle.yourdomain.dev`
   Coolify provisions Let's Encrypt certs and Traefik routing automatically
   once a domain is set per service.
4. **Set environment variables** on the `backend` service:
   - `WORDLE_ALLOWED_ORIGINS` = `https://wordle.yourdomain.dev` (the
     frontend's final domain - the API rejects browser requests from
     anywhere else)
5. **Set the build argument** on the `frontend` service:
   - `VITE_API_BASE_URL` = `https://wordle-api.yourdomain.dev` (the backend's
     final domain). This is baked into the static bundle at build time, so it
     must be set *before* deploying, and redeployed if the backend domain
     ever changes.
6. **Deploy.** Coolify builds both Dockerfiles and starts the stack.

### Updating the daily word cycle safely

`wordle.epoch-date` and `wordle.shuffle-seed` (in `application.yml`, or via
the equivalent environment variables `WORDLE_EPOCH_DATE` /
`WORDLE_SHUFFLE_SEED` if you prefer to set them in Coolify instead of
committing them) fix the daily word sequence. Changing either after players
have started playing daily puzzles will shift which word falls on which day
- fine before launch, disruptive after.

## Known simplifications

This is a portfolio project, not a production service, so a few corners were
cut deliberately:

- **Game state is in-memory**, not persisted to a database or Redis. A
  backend restart loses all in-progress (not-yet-finished) games and Endless
  shuffle bags. Finished-game stats live in the browser's localStorage
  instead, so they survive backend restarts.
- **No accounts.** Endless mode's "no repeat" guarantee is scoped to a
  `playerId` stored in localStorage, not a real user account - clearing
  browser storage effectively starts a new player.
- **Single backend instance.** Because sessions live in a `ConcurrentHashMap`
  on one instance, this wouldn't horizontally scale without moving state to
  something shared like Redis.
- **The definition lookup depends on a free third-party API** (dictionaryapi.dev)
  with no SLA. It's deliberately isolated from actual gameplay so this doesn't
  matter for playing the game - see `DictionaryService`'s Javadoc - but don't
  expect it to be 100% reliable at scale, and its in-memory cache would want
  an eviction policy under real traffic.

## Testing

```bash
cd backend && mvn test
```

Covers the guess-evaluation algorithm (including duplicate-letter edge
cases) and the Endless shuffle bag's no-repeat guarantee, reshuffle behavior,
and per-player isolation.

```bash
cd frontend && npm run build && npm run lint
```

`tsc -b` type-checks as part of the build; `oxlint` covers linting.

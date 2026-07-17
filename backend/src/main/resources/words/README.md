# Word lists

Three files. `answers.txt`/`allowed.txt` are loaded into memory at startup
by `WordService`; `denylist.txt` is loaded by `CustomPuzzleService`. No
database, no external API for any of them. See the root `README.md` for why
that's a deliberate choice rather than a shortcut.

## `answers.txt` — 2,315 words

The curated pool of possible Daily/Endless answers.

**Source:** [`mark-mucchetti/wordle-analysis`](https://github.com/mark-mucchetti/wordle-analysis),
file `wordle-answers.txt` (MIT licensed).

This is the original 2,315-word answer list that shipped hardcoded in
Wordle's client-side source code before the New York Times acquired the
game. Community members extracted it directly from that source in early
2022 (see the repo's README for the extraction story). It's the same list
widely used by Wordle-solver and Wordle-analysis projects as the canonical
"pre-NYT" answer set.

**Caveat:** the NYT has since hand-edited its *live* answer list over time
- removing some words judged too obscure or otherwise unsuitable, and
occasionally reusing old ones. This file reflects the original list as
shipped, not NYT's current, ongoing edits.

## `allowed.txt` — 14,855 words

The full dictionary of words accepted as valid guesses (a strict superset
of `answers.txt` - you can guess far more words than could ever be the
answer, same as real Wordle).

**Source:** [`tabatkins/wordle-list`](https://github.com/tabatkins/wordle-list)
(MIT licensed), also extracted directly from Wordle's client-side source code.

## `denylist.txt` — hand-curated

Checked once, at Custom puzzle *creation* time only (never against guesses)
by `CustomPuzzleService` - a word matching this list (exact, case-insensitive)
is rejected before a shareable link is ever generated.

**Source:** hand-curated, not pulled from any third-party list. Deliberately
short and limited to the most unambiguous slurs/profanity, rather than an
attempt at an exhaustive filter - see the root `README.md`'s "Known
Simplifications" for why a best-effort baseline is the honest scope here,
not a claim of comprehensive moderation.

## How they were combined

Both files were fetched, lowercased, deduplicated, and filtered to exactly
5 letters, then `answers.txt` was merged into `allowed.txt` to guarantee
every possible answer is also an accepted guess (in case the two upstream
lists had ever drifted apart). `WordService` re-verifies this invariant at
startup and refuses to boot if it doesn't hold - see
`WordService`'s constructor.

## License

`answers.txt`/`allowed.txt`'s source repos are MIT licensed, which permits
this kind of reuse. `denylist.txt` is original content (see above), covered
by this repo's own MIT license like the rest of the codebase.

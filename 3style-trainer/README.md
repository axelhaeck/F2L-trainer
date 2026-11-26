# 3Style Trainer (Android 17+)

An offline Android app to learn and drill 3-style commutators for blindfolded Rubik's Cube solving.

**No permissions.** No internet, no storage access. Progress stays in the app's private storage;
backups are saved/loaded through the system file picker (which needs no permission).

Install: `3Style-Trainer.apk` (signed with a debug key; use your own key before publishing it).

## Features

* **All cases for any buffer**: 378 corner and 440 edge 3-cycles, 56 corner twists, 66 edge flips and
  21 parity cases. Buffers can be picked in Settings (all 8 corners, all 12 edges) as well as the parity
  edge. Default UFR / UF, Speffz letters (editable letter scheme), cube colours selectable.
* **Visualisation** of every case: the whole cube as a net, the involved pieces in colour, buffer ring,
  target letters and arrows (buffer → first letter → second letter), and the commutator split into
  setup, insertion and interchange. In landscape the picture is shown next to the content.
* **Learn in small sets, clustered by commutator class**: 4-movers, pure commutators, cancelling
  setups (A9 type), 1-, 2- and 3-move setups; inside a class by the layer of the interchange. At most 8
  cases per set, a case and its inverse always together (their commutators are each other's inverse).
* **Your own algorithms**: up to 3 generated alternatives per case, or type your own (commutator
  notation supported, e.g. `[R' D: [R U R', D2]]`). It is checked by simulation before it is accepted.
* **Practice in small batches** (2–12 cases, each must be answered correctly 1–3× in a row; misses come
  back soon; weakest / longest-unseen first; inverses together). Recall mode or execution mode, times
  are recorded. Status per case *New / Learning / Known*: known cases are left out unless selected for
  review; a case becomes known at full strength (optional) and falls back to learning when forgotten.
* **Letters**: letter scheme reference, an adaptive letter quiz in both directions (sticker → letter,
  tap the sticker of a letter; it remembers which stickers you know and asks the weak ones much more
  often, with a reset), letter-pair words with a drill, and **memo tracing** of random scrambles,
  checked letter by letter (any cycle break accepted).
* Progress statistics, export/import backup.

## Correctness

* The commutators were found by a computer search (`bld/Search.kt`): all pure commutators [A, B] with A,
  B a single move or an insertion X Y X' (corners: face turns; edges: also M E S) are indexed by the
  3-cycle they perform; each case is solved by trying all setups of up to 2 (if needed 3) moves. The
  best 3 by an execution-cost estimate are kept. Twists, flips and parity use verified building blocks
  (twist commutators, 2-flips found by exhaustive search in <M, U>, PLL swaps) with setups.
* The results for all buffers are pre-generated into `app/src/main/assets/algs` and checked three times:
  1. unit tests: every algorithm for every buffer against the case's effect, constructed independently
     from the sticker geometry (`AssetsTest`); parity for all 96 buffer combinations;
  2. an end-to-end test: 300 random scrambles are traced, the memo is executed with the library's
     algorithms (incl. parity handling), and the cube must end solved up to the reported twists/flips;
  3. `tools/verify_assets.py`: a separate Python cube model and parser checks all 25 278 algorithms.
* The app re-checks each loaded algorithm by simulation when it starts.

The algorithms are correct, but they are machine-chosen, not hand-picked by a top solver: if you know
a better commutator for a case, add it in the app.

## Build

```
./gradlew testDebugUnitTest                      # all tests (incl. UI tests via Robolectric)
./gradlew testDebugUnitTest -Pscreenshots        # also writes screenshots to app/build/screenshots
./gradlew testDebugUnitTest --tests '*GenerateAssets*' -PgenerateAssets   # regenerate the algorithm files
python3 tools/verify_assets.py                   # independent check of the algorithm files
./gradlew assembleRelease                        # app/build/outputs/apk/release/app-release.apk
```

Requires Android 17 (API 37). Kotlin + Jetpack Compose (Material 3).

# F2L Trainer (Android 17+)

An offline Android app to learn and drill all 41 F2L cases of the Rubik's Cube CFOP method,
with the goal of fast, automatic F2L for sub-10 averages.

**No permissions.** The app has no internet access and no storage access. Progress is kept in the
app's private storage.

## Features

* **All 41 F2L cases** (front-right slot) in six groups, each with an isometric 3D picture
  (front and back view). Only first-two-layer stickers are coloured; the last layer is grey.
* **Learn mode**: recognition notes, 2–4 algorithms per case, and a step-by-step player that
  shows the cube after every move. The moves are coloured by phase (adjust / pair up / insert),
  computed by simulation.
* **Free back-slot shortcuts**: 17 cases have a shorter algorithm that may be used while the
  back-right slot is still unsolved (shown with that slot "empty").
* **Your own algorithms**: pick which algorithm you use, or type in your own. It is checked
  with the cube simulator before it is accepted (a missing or extra AUF is fixed automatically).
* **Practice in small batches** (2–12 cases): every case must be answered correctly 1–3× in a
  row; missed cases come back soon. The weakest and longest-unseen cases are picked first.
  * Status per case: *New*, *Learning*, *Known*. Known cases are left out of practice unless you
    select them for review.
  * *Flashcards*: recognise the case (optionally from a random U-layer angle), recall the
    algorithm, reveal it, rate yourself. Recognition time is recorded.
  * *Timed with cube*: apply a setup to a solved cube, time your solve of the pair.
* **Progress**: status counts per group, strength (Leitner box 0–5), average recognition and
  solve times, weakest cases first.

Colours: white cross on the bottom, yellow top, green front, orange right.

## Correctness

Every algorithm is checked by a geometric cube simulator. The move permutations are derived
from 3D rotations, not hand-written tables:

* `tools/` (Python, development only) holds an independent cube model and an IDA* solver. It
  enumerates all 42 possible FR-pair situations, checks the curated algorithms in
  `tools/cases_src.py`, normalises their pre-AUF, finds the free-slot shortcuts, and generates
  `app/src/main/java/app/f2ltrainer/data/CaseData.kt`.
* The Kotlin unit tests (`app/src/test`) check it all again with the app's own engine. Every
  algorithm must solve its case, with every AUF; the 41 cases must be distinct and cover every
  possible situation; the pictures must match the Python model; setup scrambles must create the
  right case; and the practice logic and storage are tested.

```
./gradlew testDebugUnitTest            # all tests (incl. a UI smoke test via Robolectric)
./gradlew testDebugUnitTest -Pscreenshots   # also writes screenshots to app/build/screenshots
./gradlew assembleRelease              # app/build/outputs/apk/release/app-release.apk
```

To change cases or algorithms: edit `tools/cases_src.py`, run `python3 tools/gen_kotlin.py`
(it refuses to generate anything if an algorithm is wrong), then run the tests.

The release build is signed with the debug key so it can be installed directly. Use your own key
before publishing it anywhere.

## Requirements

Android 17 (API 37) or newer. Built with Kotlin and Jetpack Compose (Material 3, dynamic colours).

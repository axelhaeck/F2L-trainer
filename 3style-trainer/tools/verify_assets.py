"""Independent check of the shipped algorithm files (development tool).

Uses its own cube model (cube.py, derived from 3D rotations) and its own notation parser, i.e.
nothing from the Kotlin app. For every line of every file in app/src/main/assets/algs it applies
the inverse of each algorithm to a solved cube (= the case state) and checks what the case means:

  corners/edges "X Y": the buffer holds the sticker that belongs at X, X holds Y's sticker,
                       Y holds the buffer's sticker; every other piece is solved.
  twists "P Q":        P twisted clockwise, Q counter-clockwise, rest solved.
  flips "P Q":         P and Q flipped, rest solved.

Run:  python3 tools/verify_assets.py
"""
import os
import sys
from cube import (STICKERS, PIECES, SOLVED, NORMAL, FACES, apply, invert, dot)

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "algs")


def sticker_name(i):
    pos, n = STICKERS[i]
    face = [f for f in FACES if NORMAL[f] == n][0]
    others = "".join(f for f in "UDFBRL" if f != face and dot(NORMAL[f], pos) == 1)
    return face + others


NAME = {sticker_name(i): i for i in range(54) if sum(1 for c in STICKERS[i][0] if c) >= 2}


def piece(i):
    return STICKERS[i][0]


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def clockwise_next(i):
    """Next sticker clockwise around a corner seen from outside (URF: U -> R -> F)."""
    p, n = STICKERS[i]
    for j in PIECES[p]:
        if j != i and dot(cross(n, STICKERS[j][1]), p) < 0:
            return j
    raise ValueError


# -- notation ---------------------------------------------------------------------------------
def tokens(text):
    out, i = [], 0
    while i < len(text):
        c = text[i]
        if c.isspace():
            i += 1
        elif c in "[],:":
            out.append(c); i += 1
        else:
            j = i + 1
            while j < len(text) and text[j] in "2'":
                j += 1
            out.append(text[i:j]); i = j
    return out


def parse_seq(toks, k):
    """Returns (moves, k). Grammar: seq := (move | '[' seq (','|':') seq ']')*"""
    moves = []
    while k < len(toks) and toks[k] not in "],:":
        if toks[k] == "[":
            a, k = parse_seq(toks, k + 1)
            sep = toks[k]
            b, k = parse_seq(toks, k + 1)
            assert toks[k] == "]"
            k += 1
            moves += a + b + invert(a) + (invert(b) if sep == "," else [])
        else:
            moves.append(toks[k]); k += 1
    return moves, k


def parse(text):
    m, k = parse_seq(tokens(text), 0)
    assert k == len(tokens(text)), text
    return m


def decode(enc):
    f = enc.split("|")
    if len(f) == 3:
        return "[%s: [%s, %s]]" % tuple(f)
    return "[%s: %s]" % tuple(f)


# -- checks -----------------------------------------------------------------------------------
def case_state(alg):
    return apply(SOLVED, invert(parse(alg)))


def rest_solved(s, pieces):
    return all(s[i] == i for i in range(54) if piece(i) not in pieces)


def check_cycle(s, b, x, y):
    return s[b] == x and s[x] == y and s[y] == b and rest_solved(s, {piece(b), piece(x), piece(y)})


def check_twist(s, p, q):
    # p clockwise: the sticker of p moved one step clockwise, so p's position shows the sticker
    # that is one step counter-clockwise from it; q the other way round.
    ok = s[clockwise_next(p)] == p and s[q] == clockwise_next(q)
    return ok and rest_solved(s, {piece(p), piece(q)})


def check_flip(s, p, q):
    other = lambda i: [j for j in PIECES[piece(i)] if j != i][0]
    return s[p] == other(p) and s[q] == other(q) and rest_solved(s, {piece(p), piece(q)})


def main():
    files = sorted(os.listdir(ROOT))
    total = bad = 0
    for fn in files:
        kind = fn.split("-")[0].split(".")[0]
        buf = None
        if "-" in fn:
            buf = NAME[fn.split("-")[1][:-4]]
        lines = [l for l in open(os.path.join(ROOT, fn)).read().split("\n") if l and not l.startswith("#")]
        for line in lines:
            f = line.split("\t")
            a, b = (NAME[n] for n in f[0].split())
            if len(f) < 2:
                print("NO ALGORITHM", fn, f[0]); bad += 1
            for enc in f[1:]:
                total += 1
                s = case_state(decode(enc))
                ok = {"corners": lambda: check_cycle(s, buf, a, b), "edges": lambda: check_cycle(s, buf, a, b),
                      "twists": lambda: check_twist(s, a, b), "flips": lambda: check_flip(s, a, b)}[kind]()
                if not ok:
                    bad += 1
                    print("WRONG", fn, f[0], decode(enc))
        expected = {"corners": 378, "edges": 440, "twists": 56, "flips": 66}[kind]
        if len(lines) != expected:
            bad += 1
            print("WRONG LINE COUNT", fn, len(lines))
    print("%d files, %d algorithms checked, %d problems" % (len(files), total, bad))
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()

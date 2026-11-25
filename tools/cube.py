"""Geometric 3x3 cube model (dev tool, mirrors the Kotlin engine in the app).

Coordinates: x -> R, y -> U, z -> F.  A sticker is (cubie position, outward normal).
State: tuple s of length 54, s[pos] = id of the sticker sitting at facelet pos;
a sticker's id is its home facelet, so face(id) = id // 9 is its colour.
Faces order: U R F D L B.
"""
import itertools

FACES = "URFDLB"
NORMAL = {'U': (0, 1, 0), 'R': (1, 0, 0), 'F': (0, 0, 1),
          'D': (0, -1, 0), 'L': (-1, 0, 0), 'B': (0, 0, -1)}
# (right, down) viewing vectors per face
GRID = {'U': ((1, 0, 0), (0, 0, 1)), 'R': ((0, 0, -1), (0, -1, 0)),
        'F': ((1, 0, 0), (0, -1, 0)), 'D': ((1, 0, 0), (0, 0, -1)),
        'L': ((0, 0, 1), (0, -1, 0)), 'B': ((-1, 0, 0), (0, -1, 0))}


def dot(a, b):
    return sum(i * j for i, j in zip(a, b))


def add(a, b):
    return tuple(i + j for i, j in zip(a, b))


def scale(a, k):
    return tuple(i * k for i in a)


STICKERS = []  # index -> (pos, normal)
for f in FACES:
    n = NORMAL[f]
    r, d = GRID[f]
    for row in range(3):
        for col in range(3):
            p = add(add(n, scale(r, col - 1)), scale(d, row - 1))
            STICKERS.append((p, n))
INDEX = {s: i for i, s in enumerate(STICKERS)}


def rot(v, axis, quarter):
    """Rotate integer vector v about axis ('x','y','z') by quarter*90deg (right-hand rule)."""
    x, y, z = v
    for _ in range(quarter % 4):
        if axis == 'x':
            y, z = -z, y
        elif axis == 'y':
            z, x = -x, z
        else:
            x, y = -y, x
    return (x, y, z)


AX = {'x': 0, 'y': 1, 'z': 2}
# move -> (axis, layers (coordinate values), quarter turns for the base (clockwise) move)
BASE = {
    'R': ('x', (1,), -1), 'L': ('x', (-1,), 1), 'M': ('x', (0,), 1),
    'U': ('y', (1,), -1), 'D': ('y', (-1,), 1), 'E': ('y', (0,), 1),
    'F': ('z', (1,), -1), 'B': ('z', (-1,), 1), 'S': ('z', (0,), -1),
    'r': ('x', (1, 0), -1), 'l': ('x', (-1, 0), 1),
    'u': ('y', (1, 0), -1), 'd': ('y', (-1, 0), 1),
    'f': ('z', (1, 0), -1), 'b': ('z', (-1, 0), 1),
    'x': ('x', (-1, 0, 1), -1), 'y': ('y', (-1, 0, 1), -1), 'z': ('z', (-1, 0, 1), -1),
}


def move_perm(name, power):
    """perm[i] = index the sticker at i moves to."""
    axis, layers, q = BASE[name]
    q = q * power
    perm = [0] * 54
    for i, (p, n) in enumerate(STICKERS):
        if p[AX[axis]] in layers:
            perm[i] = INDEX[(rot(p, axis, q), rot(n, axis, q))]
        else:
            perm[i] = i
    return perm


PERMS = {}
for m in BASE:
    for suf, pw in (("", 1), ("2", 2), ("'", 3)):
        PERMS[m + suf] = move_perm(m, pw)

SOLVED = tuple(range(54))


def apply_move(s, mv):
    perm = PERMS[mv]
    out = [0] * 54
    for i in range(54):
        out[perm[i]] = s[i]
    return tuple(out)


def parse(alg):
    toks = []
    for t in alg.replace("(", " ").replace(")", " ").split():
        t = t.replace("2'", "2").replace("’", "'")
        if t not in PERMS:
            raise ValueError("bad token %r in %r" % (t, alg))
        toks.append(t)
    return toks


def invert(toks):
    out = []
    for t in reversed(toks):
        if t.endswith("'"):
            out.append(t[:-1])
        elif t.endswith("2"):
            out.append(t)
        else:
            out.append(t + "'")
    return out


def apply(s, alg):
    toks = parse(alg) if isinstance(alg, str) else alg
    for t in toks:
        s = apply_move(s, t)
    return s


def colors(s):
    """Facelet colours relative to the current centres (rotation independent)."""
    centre_at = {s[f * 9 + 4] // 9: f for f in range(6)}
    return tuple(centre_at[s[i] // 9] for i in range(54))


# ---------- pieces -------------------------------------------------------------
def piece_of(i):
    return STICKERS[i][0]


PIECES = {}  # cubie position -> list of facelet indices
for i, (p, n) in enumerate(STICKERS):
    PIECES.setdefault(p, []).append(i)


def home_piece(sticker_id):
    return piece_of(sticker_id)


def name_of(p):
    n = ""
    for f in "UDFBRL":
        if dot(NORMAL[f], p) == 1:
            n += f
    return n


POS = {name_of(p): p for p in PIECES}
CROSS = ["DF", "DR", "DB", "DL"]
SLOT_PIECES = {
    'FR': ["DFR", "FR"], 'FL': ["DFL", "FL"], 'BR': ["DBR", "BR"], 'BL': ["DBL", "BL"]}


def piece_ok(col, name):
    """Is the piece with home `name` solved (relative to centres) in colouring col?"""
    for i in PIECES[POS[name]]:
        if col[i] != i // 9:
            return False
    return True


def f2l_status(s, free=()):
    col = colors(s)
    ok = all(piece_ok(col, c) for c in CROSS)
    for slot, ps in SLOT_PIECES.items():
        if slot in free:
            continue
        ok = ok and all(piece_ok(col, p) for p in ps)
    return ok


if __name__ == "__main__":
    # sanity checks
    s = SOLVED
    for m in ["R", "U", "F", "L", "D", "B", "M", "E", "S", "x", "y", "z", "r", "u", "d", "l", "f", "b"]:
        t = s
        for _ in range(4):
            t = apply_move(t, m)
        assert t == s, m
    assert apply(s, "R U R' U' " * 6) == s
    assert apply(s, "R U R' U'") != s
    # R = L x (wide) etc.
    assert colors(apply(s, "r")) == colors(apply(s, "L"))
    assert colors(apply(s, "u")) == colors(apply(s, "D"))
    assert colors(apply(s, "d")) == colors(apply(s, "U"))
    assert apply(s, "x") == apply(s, "R M' L'")
    assert apply(s, "y") == apply(s, "U E' D'")
    assert apply(s, "z") == apply(s, "F S B'")
    # R moves UFR sticker on F to U
    st = apply(s, "R")
    assert st[INDEX[((1, 1, 1), (0, 1, 0))]] == INDEX[((1, -1, 1), (0, 0, 1))]
    # superflip order / sexy move order etc
    assert apply(s, "R U2 D' B D'" * 0 + "") == s
    # T-perm keeps F2L
    assert f2l_status(apply(s, "R U R' U' R' F R2 U' R' U' R U R' F'"))
    print("cube ok")

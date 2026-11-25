"""F2L case classification helpers (dev tool)."""
from cube import *

U_, R_, F_, D_, L_, B_ = range(6)


def find_piece(col, colorset):
    for p, idx in PIECES.items():
        if len(idx) == len(colorset) and set(col[i] for i in idx) == set(colorset):
            return idx
    raise KeyError(colorset)


def pair_key(s):
    col = colors(s)
    c = find_piece(col, {D_, F_, R_})
    e = find_piece(col, {F_, R_})
    ck = [i for i in c if col[i] == D_][0]
    ek = [i for i in e if col[i] == F_][0]
    return (ck, ek)


def class_key(s):
    keys = []
    t = s
    for k in range(4):
        keys.append((pair_key(t), k))
        t = apply_move(t, "U")
    return min(keys)[0]


def case_state(alg):
    return apply(SOLVED, invert(parse(alg)))


def is_f2l_case(alg, free=()):
    s = case_state(alg)
    col = colors(s)
    ok = all(piece_ok(col, c) for c in CROSS)
    for slot, ps in SLOT_PIECES.items():
        if slot == 'FR' or slot in free:
            continue
        ok = ok and all(piece_ok(col, p) for p in ps)
    return ok


def fname(i):
    return "%s%d" % (FACES[i // 9], i % 9)


def all_classes():
    """Enumerate every placement of the FR pair with everything else solved."""
    corner_targets = [n for n in POS if len(n) == 3 and ('U' in n or n == 'DFR')]
    edge_targets = [n for n in POS if len(n) == 2 and ('U' in n or n == 'FR')]
    # build states by search over sequences is complex; instead use BFS over <U,R,F> from solved,
    # restricted to states where only the FR pair is out of place.
    from collections import deque
    seen = {SOLVED}
    q = deque([SOLVED])
    classes = set()
    gens = ["U", "U'", "U2", "R", "R'", "R2", "F", "F'", "F2"]
    depth = {SOLVED: 0}
    while q:
        s = q.popleft()
        if depth[s] >= 7:
            continue
        for m in gens:
            t = apply_move(s, m)
            if t in depth:
                continue
            depth[t] = depth[s] + 1
            q.append(t)
    for s in depth:
        col = colors(s)
        ok = all(piece_ok(col, c) for c in CROSS) and all(
            piece_ok(col, p) for sl in ('FL', 'BL', 'BR') for p in SLOT_PIECES[sl])
        if ok:
            classes.add(class_key(s))
    return classes


def canon(ck, ek):
    P = PERMS["U"]
    best = None
    for k in range(4):
        cand = (ck, ek)
        best = cand if best is None or cand < best else best
        ck, ek = P[ck], P[ek]
    return best


def all_classes2():
    corner_locs = [n for n in POS if len(n) == 3 and ('U' in n or n == 'DFR')]
    edge_locs = [n for n in POS if len(n) == 2 and ('U' in n or n == 'FR')]
    cks = [i for n in corner_locs for i in PIECES[POS[n]]]
    eks = [i for n in edge_locs for i in PIECES[POS[n]]]
    return {canon(c, e) for c in cks for e in eks}


def describe(key):
    ck, ek = key
    cpiece = name_of(piece_of(ck)); epiece = name_of(piece_of(ek))
    return "corner %s (D-colour on %s)  edge %s (F-colour on %s)" % (
        cpiece, fname(ck), epiece, fname(ek))

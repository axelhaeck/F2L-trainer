"""IDA* search for F2L algorithms (dev tool)."""
import sys
from collections import deque
from f2l import *

HOME_C = INDEX[((1, -1, 1), (0, -1, 0))]  # D sticker of DFR
HOME_E = INDEX[((1, 0, 1), (0, 0, 1))]    # F sticker of FR


def tracked_homes(free=()):
    homes = [HOME_C, HOME_E]
    for c in CROSS:
        homes.append(PIECES[POS[c]][0])
    for slot in ('FL', 'BL', 'BR'):
        if slot in free:
            continue
        for p in SLOT_PIECES[slot]:
            homes.append(PIECES[POS[p]][0])
    return homes


def gens_for(faces):
    return [f + s for f in faces for s in ("", "2", "'")]


def pair_table(gens):
    start = (HOME_C, HOME_E)
    dist = {start: 0}
    q = deque([start])
    while q:
        st = q.popleft()
        for g in gens:
            P = PERMS[g]
            nt = (P[st[0]], P[st[1]])
            if nt not in dist:
                dist[nt] = dist[st] + 1
                q.append(nt)
    return dist


def solve(start_c, start_e, faces, maxlen, free=(), extra=0, limit=200):
    gens = gens_for(faces)
    table = pair_table(gens)
    homes = tracked_homes(free)
    goal = tuple(homes)
    start = tuple([start_c, start_e] + homes[2:])
    sols = []
    path = []

    def dfs(st, depth, lastface):
        if len(sols) >= limit:
            return
        if depth == 0:
            if st == goal:
                sols.append(" ".join(path))
            return
        h = table.get((st[0], st[1]), 99)
        if h > depth:
            return
        for g in gens:
            if g[0] == lastface:
                continue
            P = PERMS[g]
            path.append(g)
            dfs(tuple(P[i] for i in st), depth - 1, g[0])
            path.pop()

    found_len = None
    for L in range(0, maxlen + 1):
        before = len(sols)
        dfs(start, L, None)
        if len(sols) > before and found_len is None:
            found_len = L
        if found_len is not None and L >= found_len + extra:
            break
    return sols


if __name__ == "__main__":
    pass

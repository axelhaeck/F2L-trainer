from build_cases import *
from search import solve, HOME_C, HOME_E
from score import score
import re

def core_len(a):
    t = [x for x in a.split() if x[0] not in 'xyz']
    if t and t[0][0] == 'U':
        t = t[1:]
    return len(t)

def parse_ranked(fn):
    res = {}
    cur = None
    for line in open(fn):
        if line.startswith("corner"):
            cur = line.split("  optimalHTM")[0].strip()
            res[cur] = []
        elif line.strip():
            parts = line.split()
            res[cur].append(" ".join(parts[2:]))
    return res

free = parse_ranked("ranked_free.txt")
out, errs = build()
picks = {}
for c in out:
    d = describe(c['key']).strip()
    best_std = min(core_len(a) for a in c['algs'])
    cands = free[d]
    # best: fewest core moves, then score
    cands = sorted(cands, key=lambda a: (core_len(a), score(a)))
    b = cands[0]
    flag = core_len(b) < best_std
    print("%2d std=%2d free=%2d %s  %s   | %s" % (c['id'], best_std, core_len(b), "YES" if flag else "   ", b, " / ".join(cands[1:4])))

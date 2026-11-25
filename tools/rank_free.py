import sys
from search import *
from score import score
keys = sorted(all_classes2())
for k in keys:
    if k == (HOME_C, HOME_E):
        continue
    allsols = set()
    for faces, ml, ex in (("RU", 13, 1), ("RUF", 11, 1), ("RUD", 10, 0)):
        allsols |= set(solve(k[0], k[1], faces, ml, ('BR',), ex, limit=3000))
    allsols = {a for a in allsols if not a.split()[-1].startswith('U')}
    ranked = sorted(allsols, key=lambda a: (score(a), len(a.split())))
    print(describe(k), " optimalHTM=", min(len(a.split()) for a in allsols))
    for a in ranked[:8]:
        print("   %5.2f %2d  %s" % (score(a), len(a.split()), a))
    sys.stdout.flush()

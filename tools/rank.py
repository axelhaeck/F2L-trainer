import sys
from search import *
from score import score
free = tuple(sys.argv[1].split(",")) if len(sys.argv) > 1 and sys.argv[1] else ()
keys = sorted(all_classes2())
for k in keys:
    if k == (HOME_C, HOME_E):
        continue
    allsols = set()
    for faces, ml, ex in (("RU", 13, 2), ("RUF", 11, 2), ("RUD", 11, 1), ("RUFL", 10, 1)):
        allsols |= set(solve(k[0], k[1], faces, ml, free, ex, limit=3000))
    ranked = sorted(allsols, key=lambda a: (score(a), len(a.split())))
    print(describe(k), " optimalHTM=", min(len(a.split()) for a in allsols))
    for a in ranked[:8]:
        print("   %5.2f %2d  %s" % (score(a), len(a.split()), a))
    sys.stdout.flush()

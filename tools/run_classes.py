import sys
from search import *
faces = sys.argv[1]
maxlen = int(sys.argv[2])
extra = int(sys.argv[3]) if len(sys.argv) > 3 else 0
free = tuple(sys.argv[4].split(",")) if len(sys.argv) > 4 and sys.argv[4] else ()
for k in sorted(all_classes2()):
    if k == (HOME_C, HOME_E):
        continue
    sols = solve(k[0], k[1], faces, maxlen, free, extra, limit=40)
    print(describe(k))
    for s in sols[:12]:
        print("    ", len(s.split()), s)
    sys.stdout.flush()

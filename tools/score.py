COST = {'U': 1.0, 'R': 1.0, 'F': 1.7, 'D': 1.6, 'L': 1.8, 'B': 2.5}
def score(alg):
    t = alg.split()
    s = 0
    for m in t:
        c = COST[m[0]]
        if m.endswith('2'):
            c *= 1.35
        s += c
    # leading AUF costs less (often absorbed)
    if t and t[0][0] == 'U':
        s -= 0.5
    return round(s, 2)

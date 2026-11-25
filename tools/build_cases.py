import json, sys
from f2l import *
from score import score
from cases_src import GROUPS, CASES, FREE_BR

U_TURN = {1: "U", 2: "U2", 3: "U'"}
POW = {"": 1, "2": 2, "'": 3}


def tok_pow(t):
    return POW[t[1:]]


def merge_prefix(j, alg):
    """Return an alg string equal to U^j followed by alg, merging AUFs."""
    toks = parse(alg)
    if j % 4 == 0:
        return " ".join(toks)
    rots = []
    i = 0
    while i < len(toks) and toks[i][0] == 'y':
        rots.append(toks[i]); i += 1
    rest = toks[i:]
    if rest and rest[0][0] == 'd' and not rots:
        p = tok_pow(rest[0])  # d^p == y'^p U^p
        rots = [{1: "y'", 2: "y2", 3: "y"}[p]]
        rest = [U_TURN[p]] + rest[1:]
    if rest and rest[0][0] == 'U':
        k = (tok_pow(rest[0]) + j) % 4
        rest = ([U_TURN[k]] if k else []) + rest[1:]
    else:
        rest = [U_TURN[j % 4]] + rest
    return " ".join(rots + rest)


def corner_on_top(s):
    ck, ek = pair_key(s)
    return ck < 9 or name_of(piece_of(ck)).startswith('U')


def corner_at_ufr(s):
    ck, ek = pair_key(s)
    return name_of(piece_of(ck)) == 'UFR'


def strip_auf_count(alg):
    t = alg.split()
    while t and t[0][0] in 'y':
        t = t[1:]
    n = len(alg.split())
    if t and t[0][0] == 'U':
        n -= 1
    return n


def htm(alg):
    return len([t for t in alg.split() if t[0] not in 'xyz'])


def build():
    errors = []
    out = []
    seen = {}
    solved_key = canon(*pair_key(SOLVED))
    for idx, (group, algs) in enumerate(CASES, 1):
        prim = algs[0]
        if not is_f2l_case(prim):
            errors.append("case %d primary not an F2L case" % idx); continue
        S = case_state(prim)
        key = class_key(S)
        if key == solved_key:
            errors.append("case %d is solved state" % idx)
        if key in seen:
            errors.append("case %d duplicates case %d" % (idx, seen[key]))
        seen[key] = idx
        # display state
        D = S
        pre = 0
        if corner_on_top(S):
            for k in range(4):
                if corner_at_ufr(D):
                    break
                D = apply_move(D, "U"); pre += 1
            assert corner_at_ufr(D)
        dk = pair_key(D)
        fixed = []
        for a in algs:
            if not is_f2l_case(a):
                errors.append("case %d alg %r is not an F2L case" % (idx, a)); continue
            sa = case_state(a)
            if class_key(sa) != key:
                errors.append("case %d alg %r solves a different case" % (idx, a)); continue
            j = None
            for k in range(4):
                t = D
                for _ in range(k):
                    t = apply_move(t, "U")
                if pair_key(t) == pair_key(sa):
                    j = k; break
            assert j is not None
            na = merge_prefix(j, a)
            if not f2l_status(apply(D, na)):
                errors.append("case %d alg %r failed after AUF merge -> %r" % (idx, a, na)); continue
            if na in fixed:
                continue
            fixed.append(na)
        free_alg = None
        if idx in FREE_BR:
            a = FREE_BR[idx]
            if not is_f2l_case(a, free=('BR',)):
                errors.append("case %d free alg %r invalid" % (idx, a))
            elif class_key(case_state(a)) != key:
                errors.append("case %d free alg %r solves a different case" % (idx, a))
            elif is_f2l_case(a):
                errors.append("case %d free alg %r does not need the free slot (add as normal alg)" % (idx, a))
            else:
                sa = case_state(a)
                j = [k for k in range(4) if pair_key(apply(D, ["U"] * k)) == pair_key(sa)][0]
                na = merge_prefix(j, a)
                if not f2l_status(apply(D, na), free=('BR',)):
                    errors.append("case %d free alg after merge failed" % idx)
                elif f2l_status(apply(D, na)):
                    errors.append("case %d free alg keeps BR?" % idx)
                else:
                    free_alg = na
        out.append(dict(id=idx, group=group, key=key, algs=fixed, display=D, free=free_alg))
    missing = all_classes2() - set(seen) - {solved_key}
    for m in missing:
        errors.append("missing class: " + describe(m))
    return out, errors


if __name__ == "__main__":
    out, errors = build()
    for c in out:
        print("%2d %-10s %s" % (c['id'], c['group'], describe(c['key'])))
        for a in c['algs']:
            print("        %2d  %s" % (htm(a), a))
        if c['free']:
            print("   free %2d  %s" % (htm(c['free']), c['free']))
    print("ERRORS:" if errors else "no errors")
    for e in errors:
        print("  ", e)

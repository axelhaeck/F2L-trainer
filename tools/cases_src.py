# Curated F2L cases.  (group, name, [main alg, alternatives...])
# Pre-AUF of every algorithm is normalised automatically by build_cases.py, so the
# leading U-turn written here does not matter; algorithms that do not solve the case
# are rejected by the builder (and later again by the Kotlin unit tests).
GROUPS = [
    ("basic", "Basic inserts"),
    ("split", "Corner on top, cross colour to the side"),
    ("crossup", "Corner on top, cross colour facing up"),
    ("edgeslot", "Edge in slot, corner on top"),
    ("cornerslot", "Corner in slot, edge on top"),
    ("bothslot", "Corner and edge in slot"),
]

CASES = [
    # ---- basic inserts
    ("basic", ["R U R'"]),
    ("basic", ["U R U' R'", "U' R U2 R'"]),
    ("basic", ["F' U' F", "y' R' U' R"]),
    ("basic", ["U' F' U F", "y' U' R' U R", "F R' F' R"]),
    # ---- corner on top (cross colour sideways), edge on top
    ("split", ["U' R U R' U2 R U' R'", "U' R U R' U' R U2 R'"]),
    ("split", ["U' R U R' U R U R'", "U2 R U' R' U' R U R'"]),
    ("split", ["U' R U' R' U R U R'", "U2 R2 U R' U R U2 R2"]),
    ("split", ["R' U2 R2 U R2 U R", "U F' U2 F U' R U R'", "R U' R' U R U' R' U2 R U' R'"]),
    ("split", ["R U' R' U2 F' U' F", "R U' R' U2 y' R' U' R"]),
    ("split", ["y' U R' U' R U2 R' U R", "U F' U' F U2 F' U F", "d R' U' R U2 R' U R", "U2 F' L' U' L U2 F"]),
    ("split", ["y' U R' U2 R U2 R' U R", "U F' U2 F U2 F' U F", "d R' U2 R U2 R' U R"]),
    ("split", ["U F' U F U2 R U R'", "y' U R' U R U2 y R U R'"]),
    ("split", ["U' R U2 R' U2 R U' R'", "U' R U2 R' U' R U2 R'"]),
    ("split", ["y' U R' U R U' R' U' R", "U F' U F U' F' U' F"]),
    ("split", ["U' R U' R' U F' U' F", "U' R U' R' d R' U' R", "U' R U' R' U y' R' U' R"]),
    ("split", ["U' R U2 R' U F' U' F", "U' R U2 R' d R' U' R", "U' R U2 R' U y' R' U' R"]),
    # ---- corner on top, cross colour facing up, edge on top
    ("crossup", ["R U R' U2 R U R' U' R U R'", "U2 R2 U2 R' U' R U' R2", "U R U' R' U' R U' R' U R U' R'"]),
    ("crossup", ["R U2 R' U' R U R'", "R2 U2 F R F' U2 R2"]),
    ("crossup", ["U2 R U R' U R U' R'", "R U' R' U2 R U R'", "F' U' F R U R'"]),
    ("crossup", ["U R U2 R' U R U' R'", "U' R U2 R' U2 R U2 R'", "U' R U2 R2 F R F'"]),
    ("crossup", ["y' U' R' U2 R U' R' U R", "U' F' U2 F U' F' U F", "U F' U2 F2 R' F' R"]),
    ("crossup", ["y' U2 R' U' R U' R' U R", "U2 F' U' F U' F' U F", "R U R' F' U' F"]),
    ("crossup", ["y' R' U2 R U R' U' R", "F' U2 F U F' U' F"]),
    ("crossup", ["y' R' U' R U2 R' U' R U R' U' R", "F' U' F U2 F' U' F U F' U' F", "y' U2 R2 U2 R U R' U R2"]),
    # ---- edge in slot, corner on top
    ("edgeslot", ["R U R' U' R U R' U' R U R'", "U R U' R' U R U' R' U R U' R'", "U2 R2 U2 R2 U' R2 U' R2"]),
    ("edgeslot", ["U' R' F R F' R U' R'", "U' R U2 R' F' U' F", "U F' U2 F R U R'"]),
    ("edgeslot", ["U R U R' U2 R U R'", "U' R U2 R' U R U R'", "U R' D' R U' R' D R"]),
    ("edgeslot", ["U F' U' F U' R U R'", "y' U R' U' R U' y R U R'"]),
    ("edgeslot", ["U' R U' R' U2 R U' R'", "U' R U' R' U' R U2 R'"]),
    ("edgeslot", ["U' R U R' U F' U' F", "U' R U R' d R' U' R", "U' R U R' U y' R' U' R"]),
    # ---- corner in slot, edge on top
    ("cornerslot", ["R' F R F' R U R'", "F' U' F U R U R'", "U' R' F R F' R U R'"]),
    ("cornerslot", ["U R U' R' U' F' U F", "R U2 R' U2 F' U' F", "U R U' R' F R' F' R"]),
    ("cornerslot", ["R U R' U' R U R'", "R U2 R' U2 R U R'", "F R' F' R2 U R'"]),
    ("cornerslot", ["y' R' U R U' R' U R", "F' U F U' F' U F", "U R U2 R' F' U2 F"]),
    ("cornerslot", ["R U' R' U R U' R'", "U F' U2 F R U2 R'"]),
    ("cornerslot", ["y' R' U' R U R' U' R", "F' U' F U F' U' F", "R U' R' F' U' F"]),
    # ---- corner and edge in slot
    ("bothslot", ["R2 U2 F R2 F' U2 R' U R'", "R U' R' d R' U2 R U2 R' U R", "R U2 R' U R U2 R' U F' U' F"]),
    ("bothslot", ["R U' R' U R U2 R' U R U' R'", "R U2 R U R' U R U2 R2"]),
    ("bothslot", ["R U' R' U' R U R' U2 R U' R'", "R U R' U' R U2 R' U' R U R'", "R2 U2 R' U' R U' R' U2 R'"]),
    ("bothslot", ["R U F R U R' U' F' R'", "R U' R' F' L' U2 L F", "R U R' U' R U' R' U2 F' U' F", "R U R' U' R U' R' U2 y' R' U' R"]),
    ("bothslot", ["R F U R U' R' F' U' R'", "F' L' U2 L F R U R'", "F' U' F U F' U F U2 R U R'", "R U' R' F R U R' U' F' R U' R'"]),
]

# Shortcuts when the back-right (BR) slot is still unsolved (found by search, see rank_free.py)
FREE_BR = {
    5: "U2 R F R' F' R'",
    6: "R' U R2 U R'",
    7: "U' R2 U R U' R2",
    8: "R' U2 R2 U R'",
    9: "F' R' U' R U F",
    10: "U' R F R F' R'",
    11: "U' R U' F R F' R'",
    13: "U' R U' F R' F' R'",
    14: "U R' U R F' U' F",
    15: "U R' U' R F' U' F",
    16: "U F' U' R' U2 R F",
    25: "U' R2 F' U2 F U2 R2",
    27: "U D R' U2 R D'",
    28: "U' R U F R F' R'",
    29: "D R' U' R D'",
    31: "D' R U R' D",
    32: "U2 F' R' U' R F",
}

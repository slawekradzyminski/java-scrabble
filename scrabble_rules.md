Below is a *game-implementation-friendly* description of **Polish Scrabble (Scrabble PL)**: full rules, tile set, scoring, challenges, and an exact specification of the **board shown in `scrabble.jpg`**.

---

## 1) Core components and data you need in code

### Players

* 2–4 players.
* Turn order is clockwise.

### Tiles (Polish set)

A Polish Scrabble set contains **100 tiles**:

* **98 letter tiles** (each has a printed letter and a point value),
* **2 blanks** (no letter printed, worth **0** points).

The **distribution (counts)** on your photo (bottom-right “ROZKŁAD PŁYTEK”) is:

* A×9, Ą×1, B×2, C×3, Ć×1, D×3, E×7, Ę×1, F×1, G×2, H×2, I×8, J×2, K×3, L×3, Ł×2, M×3, N×5, Ń×1, O×6, Ó×1, P×3, R×4, S×4, Ś×1, T×3, U×2, W×4, Y×4, Z×5, Ź×1, Ż×1, BLANK×2.
  (That’s exactly what’s printed on the board in your image.)

The standard **Polish letter point values** used in Scrabble PL are: ([Wikipedia][1])

* **0 pts**: blank ×2
* **1 pt**: A×9, I×8, E×7, O×6, N×5, Z×5, R×4, S×4, W×4
* **2 pts**: Y×4, C×3, D×3, K×3, L×3, M×3, P×3, T×3
* **3 pts**: B×2, G×2, H×2, J×2, Ł×2, U×2
* **5 pts**: Ą, Ę, F, Ó, Ś, Ż (each ×1)
* **6 pts**: Ć×1
* **7 pts**: Ń×1
* **9 pts**: Ź×1

### Tile bag + racks

* All tiles start in the bag.
* Each player has a rack capacity of **7 tiles**.

---

## 2) Setup and start-of-game procedure

1. Put all tiles in the bag.
2. Determine first player: each draws 1 tile; the player with the letter closest to the beginning of the alphabet starts (blank is treated as preceding “A” in many official rulesets). ([service.mattel.com][2])
3. Return those tiles to the bag and reshuffle.
4. Each player draws **7 tiles**.

---

## 3) The board in `scrabble.png` (exact spec)

### Board size and coordinate system

* It is the standard **15×15** Scrabble board. ([Wikipedia][3])
* Your board uses:

  * **Columns numbered 1–15** along the top edge.
  * **Rows lettered A–O** along the left edge.
* The **centre square is H8** (marked with a star on your photo). In Scrabble it is also a **Double Word** square. ([Wikipedia][3])

### Premium square types (as labelled on the Polish board)

Your photo uses Polish labels and a distinct colour scheme:

* **Green**: `POTRÓJNA PREMIA SŁOWNA` = **Triple Word (TW)**
* **Yellow**: `PODWÓJNA PREMIA SŁOWNA` = **Double Word (DW)**
* **Red/pink**: `POTRÓJNA PREMIA LITEROWA` = **Triple Letter (TL)**
* **Dark blue**: `PODWÓJNA PREMIA LITEROWA` = **Double Letter (DL)**

Rule: **premium squares apply only when you place a new tile on them**; later turns do not re-trigger that premium. ([Wikipedia][3])

### Premium square coordinates (standard layout; matches your photo)

I’m listing them in your board’s coordinate notation: **RowLetter + ColumnNumber** (e.g., `A1`).

#### Triple Word (TW) — 8 squares

`A1, A8, A15, H1, H15, O1, O8, O15` ([Wikipedia][3])

#### Double Word (DW) — 17 squares (includes centre star)

`B2, B14, C3, C13, D4, D12, E5, E11, H8, K5, K11, L4, L12, M3, M13, N2, N14` ([Wikipedia][3])

#### Triple Letter (TL) — 12 squares

`B6, B10, F2, F6, F10, F14, J2, J6, J10, J14, N6, N10` ([Wikipedia][3])

#### Double Letter (DL) — 24 squares

`A4, A12, C7, C9, D1, D8, D15, G3, G7, G9, G13, H4, H12, I3, I7, I9, I13, L1, L8, L15, M7, M9, O4, O12` ([Wikipedia][3])

---

## 4) What a player can do on their turn

On your turn you choose exactly one:

1. **Play tiles** to form a legal move (score points).
2. **Exchange tiles**: swap any number of rack tiles for the same number from the bag (score 0). Polish rule summaries often just say “any number”; many official rulesets additionally require **at least 7 tiles remain in the bag** to exchange, so this is best implemented as a configurable rule toggle. ([pfs.org.pl][4])
3. **Pass** (score 0).

Game can end if everyone passes twice in a row (as described in Polish rules summaries). 

---

## 5) Legal move rules (placement + dictionary)

### Placement geometry (must be strict for implementation)

A “play” consists of placing **one or more tiles from the rack** onto empty board squares, then committing the move.

Rules:

* Tiles placed in a single turn must lie in **one straight line**: horizontal or vertical only (no diagonals). 
* The placed tiles must form a **contiguous run** along that line (no gaps between newly placed tiles).
* **First move** must:

  * place at least **2 tiles**, and
  * cover the centre square `H8`. ([pfs.org.pl][4])
* **Subsequent moves** must connect to the existing position:

  * at least one newly placed tile must be adjacent (N/E/S/W) to an existing tile, *or* the placed run must cross an existing tile (by extending through it). ([Wikipedia][3])
* All words created must be valid, and (per Polish rules) newly formed words are expected to have length **≥ 2**. 

### Word validity (Polish-specific)

Polish Scrabble uses **OSPS (Oficjalny Słownik Polskiego Scrabblisty)** as the authoritative list of acceptable words. ([pfs.org.pl][4])

Allowed:

* Common words and their grammatically correct forms present in OSPS.

Disallowed (typical Polish Scrabble rules summaries):

* Words starting with a capital letter (proper names),
* Abbreviations,
* Prefixes/suffixes used alone,
* Forms requiring apostrophes or hyphens. 

Also important operational rule:

* You generally **do not consult the dictionary while constructing your move**; dictionary use is for *challenges/checking* after a move is laid.

---

## 6) Blanks (wildcards)

* A blank has **0 points**.
* When played, the player must declare what letter it represents; that assignment remains for the rest of the game unless the whole move is taken back due to an invalid play. ([service.mattel.com][2])
* A blank on a **DL/TL** still scores **0** (so letter multipliers don’t help it), but if it lies on a **DW/TW**, the word multiplier still applies to the total word score. 

---

## 7) Scoring (the exact algorithm you want in code)

A single move can create:

* one “main word” along the placement line, and
* several perpendicular “cross words” (one for each placed tile that touches existing tiles sideways).

You score **all** words formed/modified by the move. ([pfs.org.pl][4])

### How to score one word produced in the move

1. Compute the sum of letter values for every tile in that word.
2. Apply **letter premiums** (**only** for squares you newly covered this turn):

   * DL: letter value ×2
   * TL: letter value ×3
3. Apply **word premiums** (**only** for squares you newly covered this turn):

   * DW: word total ×2
   * TW: word total ×3
4. If a word premium is hit multiple times in the same word, multiply cumulatively (e.g., DW+DW ⇒ ×4, TW+TW ⇒ ×9). ([Wikipedia][3])

### Key nuance that trips implementations

If a newly placed tile participates in both:

* the main word, and
* a cross word,
  then that tile’s **letter premium (DL/TL)** is applied **separately** in each word’s calculation (because you score each word independently). ([Wikipedia][3])

### 50-point bonus (“bingo”)

If a player uses **all 7 rack tiles** in one move, add **+50** after computing all word scores for that move. ([pfs.org.pl][4])

---

## 8) Challenging words (kwestionowanie)

Polish Scrabble descriptions commonly use this rule:

* After a player lays tiles, but **before the score is recorded and the next player proceeds**, the opponent may challenge the legality of the word(s).
* If the challenged word is not acceptable in OSPS, the move is undone (tiles removed), and the player **loses the turn**. 
* If the word is acceptable, the move stands and scoring proceeds.

Implementation tip: treat a challenge as a state where the just-played move is still “pending” until accepted/recorded.

---

## 9) Drawing tiles after a move

After a successful play (or after exchanging tiles), the player draws from the bag so they again have **7 tiles**, unless the bag is empty. ([pfs.org.pl][4])

---

## 10) End of game and final scoring

Common end conditions:

1. The bag is empty and a player plays their last rack tile (“goes out”). 
2. No-one can/will play: e.g., everyone passes twice in a row (as described in Polish rules summaries). 

Final score adjustment:

* Each player **subtracts** the sum of point values left on their rack.
* If someone went out, that player **adds** the sum of all opponents’ leftover rack values. 

---

## 11) Practical implementation checklist (what to encode explicitly)

### Data structures

* `Board[15][15]` with:

  * placed tile (letter + isBlank + blankAssignedLetter),
  * premium type (`NONE | DL | TL | DW | TW`).
* `Bag` (multiset with counts).
* `Rack` per player (size ≤ 7).

### Move validation steps (suggested order)

1. All placed squares empty + in bounds.
2. All placed tiles are collinear and contiguous.
3. If first move: covers `H8` and uses ≥2 tiles.
4. Else: move touches existing tiles (adjacent/cross).
5. Build the set of words formed (main + crosses); ensure each word length ≥2.
6. Dictionary validation (OSPS) for each word (either immediately or via challenge flow).

### Scoring steps

1. For each formed word:

   * compute letter sum with DL/TL applied only for new tiles,
   * apply DW/TW from new tiles only.
2. Sum word scores.
3. If 7 tiles used: +50.

---

If you want, I can also provide a **precise move-generation/scoring pseudocode** (including how to extract the main word and cross words from a placement), but the rules above should already be enough to implement the engine cleanly.

[1]: https://en.wikipedia.org/wiki/Scrabble_letter_distributions?utm_source=chatgpt.com "Scrabble letter distributions"
[2]: https://service.mattel.com/instruction_sheets/53639-ENG.pdf?utm_source=chatgpt.com "HOW TO PLAY EVERY WORD COUNTS! SCRABBLE® is ..."
[3]: https://en.wikipedia.org/wiki/Scrabble?utm_source=chatgpt.com "Scrabble"
[4]: https://www.pfs.org.pl/reguly.php "Polska Federacja Scrabble :: Scrabble : Reguły gry w SCRABBLE"

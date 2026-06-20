# Snakes and Ladders — LLD Interview Reference

## System Overview

The Snakes and Ladders system models a turn-based board game. A `Game` is constructed via a Builder. The board stores snake/ladder positions in a `HashMap<start, end>`. Players start at position 0 and win by reaching the board size exactly. A single `Dice` rolls random values in a configurable range.

---

## Core Entities

| Entity | Key Fields | Responsibilities |
|--------|-----------|-----------------|
| `Game` | `board`, `players`, `dice`, `status` | Orchestrates turns; checks for win |
| `Board` | `size`, `boardEntities` (Map<int, BoardEntity>) | Maps positions to snakes/ladders |
| `Player` | `name`, `position` | Tracks current board position |
| `Dice` | `minValue`, `maxValue` | Rolls a random number in range |
| `BoardEntity` (abstract) | `start`, `end` | Base for Snake and Ladder |
| `SnakeEntity` | `start > end` | Moves player backward |
| `LadderEntity` | `start < end` | Moves player forward |

### Enums

| Enum | Values |
|------|--------|
| `GameStatus` | `NOT_STARTED`, `RUNNING`, `COMPLETED` |

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Builder** | `Game.Builder` | Complex setup — board size, snakes, ladders, players, dice — with validation at `build()` |
| **Template Method** (implicit) | `Game.playTurn()` | Fixed sequence: roll → move → check snake/ladder → check win |
| **Inheritance** | `BoardEntity` → `SnakeEntity`, `LadderEntity` | Polymorphic handling of board elements |

---

## Database Schema

```sql
-- Games
CREATE TABLE games (
    id              VARCHAR(36)   PRIMARY KEY,
    board_size      INT           NOT NULL,
    status          ENUM('NOT_STARTED','RUNNING','COMPLETED') NOT NULL DEFAULT 'NOT_STARTED',
    winner_name     VARCHAR(200),
    total_turns     INT           NOT NULL DEFAULT 0,
    started_at      TIMESTAMP,
    ended_at        TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Players
CREATE TABLE players (
    id              VARCHAR(36)   PRIMARY KEY,
    name            VARCHAR(200)  NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Game participants
CREATE TABLE game_players (
    game_id         VARCHAR(36)   NOT NULL,
    player_id       VARCHAR(36)   NOT NULL,
    play_order      INT           NOT NULL,
    final_position  INT,
    PRIMARY KEY (game_id, player_id),
    FOREIGN KEY (game_id)   REFERENCES games(id),
    FOREIGN KEY (player_id) REFERENCES players(id)
);

-- Board configuration (snakes and ladders)
CREATE TABLE board_entities (
    game_id         VARCHAR(36)   NOT NULL,
    entity_type     ENUM('SNAKE','LADDER') NOT NULL,
    start_position  INT           NOT NULL,
    end_position    INT           NOT NULL,
    PRIMARY KEY (game_id, start_position),
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    CONSTRAINT chk_snake   CHECK (entity_type = 'LADDER' OR start_position > end_position),
    CONSTRAINT chk_ladder  CHECK (entity_type = 'SNAKE'  OR start_position < end_position)
);

-- Turn log
CREATE TABLE game_turns (
    id              BIGINT        PRIMARY KEY AUTO_INCREMENT,
    game_id         VARCHAR(36)   NOT NULL,
    player_id       VARCHAR(36)   NOT NULL,
    turn_number     INT           NOT NULL,
    dice_value      INT           NOT NULL,
    position_before INT           NOT NULL,
    position_after  INT           NOT NULL,
    entity_type     ENUM('SNAKE','LADDER'),  -- NULL if no entity hit
    turned_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (game_id)   REFERENCES games(id),
    FOREIGN KEY (player_id) REFERENCES players(id)
);
```

---

## API Modelling

### POST /api/games
Create a new game.

**Request Body:**
```json
{
  "boardSize": 100,
  "players": ["Alice", "Bob"],
  "snakes": [{ "start": 99, "end": 5 }, { "start": 70, "end": 30 }],
  "ladders": [{ "start": 4, "end": 56 }, { "start": 22, "end": 58 }],
  "diceMin": 1,
  "diceMax": 6
}
```

**Failure Cases:**
- Snake/ladder start or end outside board range → 400
- Snake `end >= start` → 400
- Ladder `end <= start` → 400
- Two entities at the same start position → 400
- Snake/ladder at position 0 or at `boardSize` → 400

---

### POST /api/games/{gameId}/roll
Roll dice for the current player's turn.

**Response 200:**
```json
{
  "player": "Alice",
  "diceValue": 6,
  "positionBefore": 22,
  "entityHit": "LADDER",
  "positionAfter": 58,
  "gameStatus": "RUNNING",
  "nextPlayer": "Bob"
}
```

**Failure Cases:**
- Game not started or already completed → 409
- Extra roll after winning → 409

---

### GET /api/games/{gameId}
Get current game state.

---

## Code Review Findings

**Design:**
- `Game.Builder` validates `boardSize > 0` — good. But it doesn't validate that snakes/ladders don't overlap with each other or that no entity's start/end is at position 0 (invalid) or at `boardSize` (winning position).
- `Player.position` is mutable with no bounds checking — a player could theoretically move past the board size without a win check.
- `Dice` generates values using `Math.random()` — not seeded, so games can't be replayed. Use `Random` with a seed for testability.

**Minor:**
- `GameStatus.NOT_STARTED` is never set to anything before `RUNNING` in the current demo — the Builder sets it to `RUNNING` immediately.
- No "exact roll to win" rule — many variants require an exact roll to land on the last square. The current code just checks `position >= boardSize`.

---

## Extension Points

- **Multiple dice:** Allow configuring N dice and summing rolls.
- **Online multiplayer:** Add WebSocket push so each player sees the board update in real time.
- **Custom win condition:** Add an `IWinCondition` strategy (exact landing vs. reach-or-pass).

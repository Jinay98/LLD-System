# Tic-Tac-Toe — LLD Interview Reference

## System Overview

The Tic-Tac-Toe system supports configurable board sizes (N×N), multiple players, pluggable winning strategies, and an Observer-based scoreboard. `TicTacToeSystem` manages multiple concurrent games. Winning is checked after every move via a list of `IWinningStrategy` implementations (Row, Column, Diagonal). An Observer notifies a `Scoreboard` at game end.

---

## Core Entities

| Entity | Key Fields | Responsibilities |
|--------|-----------|-----------------|
| `TicTacToeSystem` | `games` (Map) | Singleton; creates and manages game instances |
| `Game` | `board`, `players`, `currentPlayerIndex`, `status`, `winner`, `winningStrategies`, `observers` | Orchestrates turns and delegates win-checking |
| `Board` | `grid` (Cell[][]) | N×N grid |
| `Cell` | `row`, `col`, `symbol` | One board position |
| `Player` | `name`, `symbol` | One participant |
| `Scoreboard` | `scores` (Map<symbol, wins>) | Tracks wins across games |

### Enums

| Enum | Values |
|------|--------|
| `Symbol` | `X`, `O`, `EMPTY` |
| `GameStatus` | `IN_PROGRESS`, `WINNER_X`, `WINNER_O`, `DRAW` |

---

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** | `TicTacToeSystem` | Single registry of active games |
| **Strategy** | `IWinningStrategy` (Row, Column, Diagonal) | Each strategy checks one win condition; all three checked after every move |
| **Observer** | `GameObservers`, `Scoreboard` | Scoreboard updated without coupling Game to Scoreboard |

---

## Database Schema

```sql
-- Players
CREATE TABLE players (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Games
CREATE TABLE games (
    id          VARCHAR(36)  PRIMARY KEY,
    board_size  INT          NOT NULL DEFAULT 3,
    status      ENUM('IN_PROGRESS','WINNER_X','WINNER_O','DRAW') NOT NULL DEFAULT 'IN_PROGRESS',
    winner_id   VARCHAR(36),
    started_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at    TIMESTAMP,
    FOREIGN KEY (winner_id) REFERENCES players(id)
);

-- Game participants
CREATE TABLE game_players (
    game_id     VARCHAR(36)  NOT NULL,
    player_id   VARCHAR(36)  NOT NULL,
    symbol      CHAR(1)      NOT NULL,  -- 'X' or 'O'
    play_order  INT          NOT NULL,
    PRIMARY KEY (game_id, player_id),
    FOREIGN KEY (game_id)   REFERENCES games(id)   ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES players(id)
);

-- Move history
CREATE TABLE moves (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    game_id     VARCHAR(36)  NOT NULL,
    player_id   VARCHAR(36)  NOT NULL,
    row_number  INT          NOT NULL,
    col_number  INT          NOT NULL,
    move_number INT          NOT NULL,
    moved_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (game_id, row_number, col_number),
    UNIQUE (game_id, move_number),
    FOREIGN KEY (game_id)   REFERENCES games(id),
    FOREIGN KEY (player_id) REFERENCES players(id)
);

-- Scores / leaderboard
CREATE TABLE player_scores (
    player_id   VARCHAR(36)   PRIMARY KEY,
    wins        INT           NOT NULL DEFAULT 0,
    losses      INT           NOT NULL DEFAULT 0,
    draws       INT           NOT NULL DEFAULT 0,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (player_id) REFERENCES players(id)
);
```

---

## API Modelling

### POST /api/games
Create a new game.

**Request Body:**
```json
{ "playerIds": ["p1", "p2"], "boardSize": 3 }
```

**Response 201:**
```json
{ "gameId": "g1", "status": "IN_PROGRESS", "nextPlayer": "p1" }
```

**Failure Cases:**
- Only 1 player → 400
- `boardSize < 3` → 400
- Player not found → 404

---

### POST /api/games/{gameId}/moves
Make a move.

**Request Body:**
```json
{ "playerId": "p1", "row": 1, "col": 2 }
```

**Response 200:**
```json
{
  "status": "IN_PROGRESS",
  "board": [["X","_","_"],["_","O","_"],["_","_","_"]],
  "nextPlayer": "p2"
}
```

**Responses:**
| Code | Meaning |
|------|---------|
| 200 | Move accepted |
| 400 | Cell already occupied; row/col out of bounds |
| 403 | Not this player's turn |
| 409 | Game already ended |

**Failure Cases:**
- Cell `(0,0)` already has `X`, player tries again → 400 "Cell already occupied"
- Wrong player's turn → 403
- Move on a completed game → 409

---

### GET /api/games/{gameId}
Get game state.

**Response 200:**
```json
{
  "gameId": "g1",
  "status": "WINNER_X",
  "board": [["X","X","X"],["O","O","_"],["_","_","_"]],
  "winner": "p1",
  "moves": 5
}
```

---

## Concurrency & Thread-Safety Notes

- `TicTacToeSystem` uses a HashMap for games — not thread-safe for concurrent game creation. Use `ConcurrentHashMap`.
- `Game.makeMove()` should be synchronized to prevent two players from submitting moves simultaneously (especially in multiplayer online mode).
- `Scoreboard.scores` should use `ConcurrentHashMap` or `AtomicInteger` for concurrent score updates.

---

## Code Review Findings

**Design:**
- `WinningStrategy` implementations check the entire board on every move — O(N²). Instead, pass the last move `(row, col)` to the strategy and check only the row, column, and diagonal through that cell — O(N).
- `GameStatus` enum encodes the winner (`WINNER_X`, `WINNER_O`) — not extensible to 3+ players. Use `WINNER` + a `winner` field on `Game`.
- `Game` holds both `winningStrategies` and `observers` — too many concerns. Extract a `GameEventPublisher`.

**Minor:**
- `Symbol.EMPTY` is used for empty cells — consider using `Optional<Symbol>` instead to avoid treating EMPTY as a valid player symbol.
- `Board` constructor takes size but creates `Cell[][]` without initializing each cell — NPE if a cell is accessed before being set. Initialize all cells with `Symbol.EMPTY` in the constructor.

---

## Extension Points

- **N-player support:** Change `Symbol` to a generic `PlayerSymbol`; `GameStatus` to `WINNER(player)` vs `DRAW`
- **AI opponent:** Add `AIPlayer extends Player` with a `MinimaxStrategy` for move selection
- **Replay:** The moves table enables full game replay; add `GET /api/games/{gameId}/replay`

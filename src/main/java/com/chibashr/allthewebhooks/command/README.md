# Commands

## fire — Manually fire an event

The **fire** subcommand lets you trigger an event by key and optional context data. It runs the same rule resolution and matching as normal in-game events (including dynamically added events from `events.yaml`), and reports to the console (or the commanding player) what happened.

### Usage

```
/allthewebhooks fire <eventKey> [key=value ...] [--dry-run]
```

- **eventKey**: Dot-notation event key (e.g. `player.join`, `server.enable`, or any key you defined in `events.yaml`).
- **key=value**: Optional. If omitted, the plugin builds a **synthetic context** as the server would fire: it uses the commanding player (if run by a player) and server state to fill the same fields a real event would produce. Use this to test emissions without typing extra data.
- **key=value** (when provided): Override or add context entries (e.g. `player.name=Test`, `world.name=world_nether`).
- **--dry-run**: Report which rule matched and what would be sent, without dispatching to the webhook.

### Permission

- `allthewebhooks.fire` (default: op). Configurable in `config.yaml` under `commands.fire.permission`.

### Console reporting

For each fire, the command reports:

1. Event key and world (if set).
2. Whether a rule matched, and which rule (matched key, webhook, message).
3. If the rule is disabled.
4. Permission check result (if the rule requires a permission).
5. Condition evaluation result.
6. Rate limit result.
7. Message template resolution.
8. Webhook resolution.
9. Outcome: either **Dry run: would dispatch...** (with `--dry-run`), or **Dispatched to webhook &lt;name&gt;.**

If no rule matches, or a check fails (permission, conditions, rate limit, missing message/webhook), the report states why the event was not fired.

### Examples

- Fire an event **without extra info** (uses server/commanding player data, as the server would fire):
  ```
  /allthewebhooks fire player.join
  /allthewebhooks fire server.enable
  ```
  Run as a player to use that player’s name, world, etc.; run from console for server-only events or placeholder player data.

- Fire with custom data (e.g. to test specific message content):
  ```
  /allthewebhooks fire player.join player.name=TestPlayer world.name=world
  ```

- See what would happen without sending:
  ```
  /allthewebhooks fire player.chat --dry-run
  ```

When no `key=value` pairs are given, the plugin builds a synthetic context from the commanding player and server so you can test emissions without typing extra information.

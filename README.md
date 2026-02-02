# All the Webhooks

A **Paper-** and **Folia-** compatible Minecraft plugin that forwards in-game events to webhooks (e.g. Discord) using YAML-driven rules. Configure exactly which events to send, to which endpoints, and how they look—all without touching code.

---

## Why All the Webhooks?

- **All-encompassing** — Covers the full range of in-game events: player (join, chat, death, advancement), block (break, place), entity (damage, spawn), world, inventory, combat, and more. Events use dot-notation keys (e.g. `player.break.block.diamond_ore`, `entity.damage.player.by.zombie`) so you can target as broadly or as narrowly as you need.
- **Extensible** — Define multiple webhooks, use wildcards at any depth (`player.break.block.*`, `entity.damage.*`), and combine rules with predicates and conditions. Add new events or endpoints by editing YAML; resolution is dynamic and best-match.
- **Regex in messages** — Message templates support placeholder transforms. Use `{key|regex:pattern:replacement}` to reshape values (e.g. extract a path segment, normalize text) with Java regex; invalid or timed-out regex falls back to the raw value safely.

Events are opt-in, dispatched asynchronously, and rate-limited. Safe by default with redaction, validation, and Folia-friendly execution.

---

## Quick Start

1. **Build:** `./gradlew build` (output: `build/libs/`)
2. **Install:** Drop the JAR into your server `plugins/` folder and start once to generate:
   - `config.yaml` — webhooks, rate limits, redaction, safety
   - `events.yaml` — which events go to which webhooks
   - `messages.yaml` — payload templates
3. **Configure** your webhook URLs and rules, then run `/allthewebhooks reload`.
4. **Docs:** Open `plugins/AllTheWebhooks/docs/docs.html` for the full event list and placeholder/regex reference.

---

## Commands

| Command | Description |
|--------|-------------|
| `/allthewebhooks reload` | Reload config, events, and messages |
| `/allthewebhooks stats`  | View dispatch statistics |
| `/allthewebhooks docs generate` | Regenerate HTML docs (if needed) |

---

## Project

- **Author:** chibashr  
- **License:** See repository  
- **Tests:** `./gradlew test` — see `planning/testing.md` for layout and coverage.

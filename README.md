# All the Webhooks

A **Paper-** and **Folia-** compatible Minecraft plugin that forwards **in-game events** to webhooks (e.g. Discord) using **YAML-driven rules**. Configure exactly which events to send, to which endpoints, and how they look—all without touching code.

---

## What It Does

**All the Webhooks** lets server owners send a wide range of in-game events to one or more webhooks (primarily Discord). Events use **dot-notation keys** (e.g. `player.join`, `player.break.block.diamond_ore`, `entity.damage.player.by.zombie`) so you can target as broadly or as narrowly as you need. The plugin is:

- **Opt-in** — No events are sent unless you configure them in `events.yaml`.
- **Expressive** — Wildcards at any depth (`player.break.block.*`, `entity.damage.*`), conditions (e.g. gamemode, damage amount), and optional permission checks.
- **Safe by default** — Rate-limited, async dispatch, redaction of sensitive fields, and Folia-friendly execution.

---

## How It Works (High Level)

1. **Event discovery** — The plugin discovers Bukkit/Paper event classes and builds dot-notation keys. When an event fires, it builds an **event context** (key + key/value map from the event). Context enrichment adds fields from entities in scope (World, Player, Block)—e.g. `world.environment` is available for `player.death`.
2. **Rule resolution** — For each event, the plugin finds the best-matching rule in `events.yaml` (exact match, then more specific wildcards). Optional world overrides apply.
3. **Checks** — Rule enabled, optional permission, conditions (e.g. `equals`, `greater-than`), and rate limiting are applied.
4. **Message** — The chosen template from `messages.yaml` is filled with placeholders (e.g. `{player.name}`) and optional regex transforms (`{key|regex:pattern:replacement}`). Redaction is applied to configured fields.
5. **Dispatch** — A JSON payload is sent to the webhook URL via HTTP POST, asynchronously and with configurable rate limits.

---

## Quick Start

Just go to the [releases page](https://github.com/chibashr/all-the-webhooks/releases) and download the latest JAR. Drop it into your server’s `plugins/` folder, start the server once to generate configs, then follow the generated `README` and in-plugin docs for setup and usage.

---

## Commands

| Command | Description |
|--------|-------------|
| `/allthewebhooks reload` | Reload config, events, and messages |
| `/allthewebhooks stats` | View dispatch statistics (dispatched, dropped, rate-limited) |
| `/allthewebhooks validate` | Validate config and event keys |
| `/allthewebhooks docs generate` | Regenerate HTML and JSON docs |
| `/allthewebhooks fire <eventKey> [key=value ...] [--dry-run]` | Manually fire an event for testing |

---

## Self-generating documentation

The plugin generates documentation from the **actual runtime event registry**, so it always matches the events your server exposes (including discovered Bukkit/Paper events).

**Output location:** `plugins/AllTheWebhooks/docs/`

| File | Purpose |
|------|---------|
| `docs.html` | Main user-facing docs: overview, event list by category, placeholders, entity field reference, and regex reference |
| `events.json` | Machine-readable list of event keys and metadata |

**When it runs:**

- **On startup** — If `config.yaml` has `documentation.generate-on-startup: true` (default).
- **On reload** — If `documentation.generate-on-reload: true`.
- **On demand** — `/allthewebhooks docs generate` (runs async so it does not block the server).

Docs are **offline-first**: no hosted site or internet required. Open `docs.html` in a browser to browse events by category, see placeholders per event (including context-enriched fields from World, Player, Block), use the entity field reference, and the regex transform reference.

---

## Configuration at a glance

- **config.yaml** — Webhook URLs and timeouts, global rate limit, async/Folia, redaction fields, logging and doc generation options, command permissions.
- **events.yaml** — Defaults (enabled, webhook, message, permission), optional world overrides, and event rules: event key (or wildcard) → message template, conditions, optional permission.
- **messages.yaml** — Named message templates with `{placeholder}` (including context-enriched fields like `{world.environment}`) and optional `{key|regex:pattern:replacement}`.

---

## Project
Vibe-coded with love <3

- **Author:** chibashr  
- **License:** MIT — see [LICENSE](LICENSE)  
- **Tests:** `./gradlew test` — see `planning/testing.md` for layout and coverage.

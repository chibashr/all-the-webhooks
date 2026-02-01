# All the Webhooks

All the Webhooks is a Paper- and Folia-compatible plugin that forwards explicitly
configured in-game events to one or more webhooks using YAML-driven rules.

## Building and testing

- Build (includes tests): `./gradlew build`
- Run tests only: `./gradlew test`
- Output JAR: `build/libs/`

See `planning/testing.md` for test layout and coverage.

## Quick Start

1. Build the plugin.
2. Drop the jar into your server `plugins/` directory.
3. Start the server once to generate:
   - `config.yaml`
   - `events.yaml`
   - `messages.yaml`
4. Edit the files and run `/allthewebhooks reload`.
5. Open `plugins/AllTheWebhooks/docs/events.html` to browse supported events.

## Commands

- `/allthewebhooks reload`
- `/allthewebhooks stats`
- `/allthewebhooks docs generate`

## Notes

- Documentation is generated offline in `plugins/AllTheWebhooks/docs/`.
- All webhooks are dispatched asynchronously.

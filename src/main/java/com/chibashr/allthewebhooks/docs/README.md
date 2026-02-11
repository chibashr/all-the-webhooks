# Documentation Generator

Generates `docs.html` and `events.json` for the All the Webhooks plugin.

## Output Layout (Wiki-style)

The generated `docs.html` uses a two-panel layout:

- **Sidebar (left):**
  - Header with version and server info
  - **Documentation** section: Overview, Quick start, Event key hierarchy, Message structure, Conditions, Webhook display name, Regex
  - **Events** section: Searchable, hierarchical (category → subcategory → event), Expand/Collapse All
  - **Sub-events** section: Same structure as Events, for discovered sub-events

- **Main content (right):**
  - Welcome screen with stats (default)
  - Doc sections when a doc link is clicked
  - Event detail (predicates table, wildcards, badges) when an event is clicked

## Event Hierarchy

- **Category** = first segment of event key (e.g. `player`, `entity`)
- **Subcategory** = second segment (e.g. `death`, `damage`)

## Files Generated

- `docs/docs.html` – Single-page wiki-style documentation
- `docs/events.json` – Event data including `event`, `category`, `subcategory`, `predicates`, `wildcards`

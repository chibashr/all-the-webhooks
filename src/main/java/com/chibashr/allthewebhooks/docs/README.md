# Documentation Generator

Generates `docs.html` and `events.json` for the All the Webhooks plugin.

## Output Layout (Wiki-style)

The generated `docs.html` uses a two-panel layout:

- **Sidebar (left, resizable via drag handle):**
  - Header with version and server info
  - **Documentation** section (blue-green header): Overview, Quick start, Event key hierarchy, Message structure, Conditions, Webhook display name, Regex
  - **Events** section (green header): Tabbed [Events | Sub-events], each with search, Expand/Collapse All, and hierarchical category → subcategory → event list
  - Each section scrolls independently within its area

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

package com.chibashr.allthewebhooks.docs;

import com.chibashr.allthewebhooks.events.EventDefinition;
import com.chibashr.allthewebhooks.events.EventRegistry;
import com.chibashr.allthewebhooks.util.AsyncExecutor;
import com.chibashr.allthewebhooks.util.HtmlEscaper;
import com.chibashr.allthewebhooks.webhook.JsonEscaper;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;

public class DocumentationGenerator {
    private final JavaPlugin plugin;
    private final EventRegistry registry;
    private final AsyncExecutor asyncExecutor;

    public DocumentationGenerator(JavaPlugin plugin, EventRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.asyncExecutor = new AsyncExecutor(plugin);
    }

    public void generateAsync() {
        asyncExecutor.runAsync(this::generate);
    }

    public void generate() {
        File docsDir = new File(plugin.getDataFolder(), "docs");
        if (!docsDir.exists() && !docsDir.mkdirs()) {
            plugin.getLogger().warning("Failed to create docs directory.");
            return;
        }

        List<EventDefinition> events = new ArrayList<>(registry.getDefinitions());
        events.sort(Comparator.comparing(EventDefinition::getKey));

        writeFile(new File(docsDir, "docs.html"), buildDocsHtml(events));
        writeFile(new File(docsDir, "events.json"), buildEventsJson(events));
    }

    private void writeFile(File file, String content) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to write " + file.getName() + ": " + ex.getMessage());
        }
    }

    private String buildDocsHtml(List<EventDefinition> events) {
        String sidebar = buildDocsSidebar(events, true);
        String content = buildDocsContent(events);
        return buildDocsShell("All the Webhooks - Documentation", sidebar, content);
    }

    private String buildDocsShell(String title, String sidebar, String content) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html>\n");
        builder.append("<html>\n");
        builder.append("<head>\n");
        builder.append("<meta charset=\"utf-8\">\n");
        builder.append("<title>").append(HtmlEscaper.escape(title)).append("</title>\n");
        builder.append("<style>\n");
        builder.append(buildStyles());
        builder.append("</style>\n");
        builder.append("</head>\n");
        builder.append("<body>\n");
        builder.append("<div class=\"layout\">\n");
        builder.append("<div class=\"sidebar\">\n");
        builder.append(sidebar);
        builder.append("</div>\n");
        builder.append("<div class=\"content\">\n");
        builder.append("<div class=\"content-inner\">\n");
        builder.append(content);
        builder.append("</div>\n");
        builder.append("</div>\n");
        builder.append("</div>\n");
        builder.append("<script>\n");
        builder.append(buildScripts());
        builder.append("</script>\n");
        builder.append("</body>\n");
        builder.append("</html>\n");
        return builder.toString();
    }

    private String buildDocsSidebar(List<EventDefinition> events, boolean includeSections) {
        StringBuilder builder = new StringBuilder();
        builder.append("<div class=\"sidebar-header\">\n");
        builder.append("<div class=\"sidebar-title\">All the Webhooks</div>\n");
        builder.append("<div class=\"meta\">Version: ").append(HtmlEscaper.escape(plugin.getDescription().getVersion()))
                .append("</div>\n");
        builder.append("<div class=\"meta\">Server: ").append(HtmlEscaper.escape(plugin.getServer().getVersion()))
                .append("</div>\n");
        builder.append("<div class=\"meta\">Generated: ").append(Instant.now().toString()).append("</div>\n");
        builder.append("</div>\n");
        builder.append("<input class=\"search\" id=\"search\" placeholder=\"Search events or predicates\" />\n");
        builder.append("<div class=\"nav\">\n");
        if (includeSections) {
            builder.append("<div class=\"nav-section\">\n");
            builder.append("<div class=\"nav-heading\">Docs</div>\n");
            builder.append("<a class=\"nav-anchor nav-link\" href=\"#overview\">Overview</a>\n");
            builder.append("<a class=\"nav-anchor nav-link\" href=\"#quick-start\">Quick start</a>\n");
            builder.append("<a class=\"nav-anchor nav-link\" href=\"#event-keys\">Event key hierarchy</a>\n");
            builder.append("<a class=\"nav-anchor nav-link\" href=\"#message-structure\">Message structure</a>\n");
            builder.append("<a class=\"nav-anchor nav-link\" href=\"#conditions\">Conditions</a>\n");
            builder.append("<a class=\"nav-anchor nav-link\" href=\"#webhook-display-name\">Webhook display name</a>\n");
            builder.append("<a class=\"nav-anchor nav-link\" href=\"#regex\">Regex</a>\n");
            builder.append("<a class=\"nav-anchor nav-link\" href=\"#events\">Events</a>\n");
            builder.append("</div>\n");
        }
        builder.append("<div class=\"nav-section\">\n");
        builder.append("<div class=\"nav-heading\">Event index</div>\n");
        builder.append(buildNavigation(events));
        builder.append("</div>\n");
        builder.append("</div>\n");
        return builder.toString();
    }

    private String buildDocsContent(List<EventDefinition> events) {
        StringBuilder builder = new StringBuilder();
        builder.append(buildOverviewSection());
        builder.append(buildQuickStartSection());
        builder.append(buildEventKeysSection(events));
        builder.append(buildMessageStructureSection());
        builder.append(buildConditionsSection());
        builder.append(buildWebhookDisplayNameSection());
        builder.append(buildPlaceholderRegexSpecSection());
        builder.append(buildEventsContent(events, true));
        return builder.toString();
    }

    private String buildOverviewSection() {
        StringBuilder builder = new StringBuilder();
        builder.append("<section id=\"overview\" class=\"doc-section\" data-anchor=\"overview\">\n");
        builder.append("<h1>All the Webhooks</h1>\n");
        builder.append("<div class=\"meta\">Version: ").append(HtmlEscaper.escape(plugin.getDescription().getVersion()))
                .append("</div>\n");
        builder.append("<div class=\"meta\">Server: ").append(HtmlEscaper.escape(plugin.getServer().getVersion()))
                .append("</div>\n");
        builder.append("<div class=\"meta\">Generated: ").append(Instant.now().toString()).append("</div>\n");
        builder.append("<p>Configure events in <code>events.yaml</code>, messages in <code>messages.yaml</code>, ");
        builder.append("and webhooks in <code>config.yaml</code>.</p>\n");
        builder.append("<p>Generate docs with <code>/allthewebhooks docs generate</code>.</p>\n");
        builder.append("</section>\n");
        return builder.toString();
    }

    private String buildQuickStartSection() {
        StringBuilder builder = new StringBuilder();
        builder.append("<section id=\"quick-start\" class=\"doc-section\" data-anchor=\"quick-start\">\n");
        builder.append("<h1>Quick start</h1>\n");
        builder.append("<ol>\n");
        builder.append("<li><strong>Configure a webhook</strong> in <code>config.yaml</code> under <code>webhooks</code>.</li>\n");
        builder.append("<li><strong>Choose an event</strong> from the <a class=\"inline-link\" href=\"#events\">Events</a> section (e.g. <code>player.death</code>, <code>player.join</code>).</li>\n");
        builder.append("<li><strong>Add a rule</strong> in <code>events.yaml</code> under <code>events</code> with <code>message</code> and optional <code>conditions</code>.</li>\n");
        builder.append("<li><strong>Define the message</strong> in <code>messages.yaml</code> using placeholders from the event's predicates.</li>\n");
        builder.append("</ol>\n");
        builder.append("<div class=\"example-block\">\n");
        builder.append("<div class=\"example-title\">Example: notify on player death</div>\n");
        builder.append("<pre># events.yaml\nevents:\n  player.death:\n    message: death_alert\n\n# messages.yaml\nmessages:\n  death_alert:\n    content: \"**{player.name}** died in {world.name}\"</pre>\n");
        builder.append("</div>\n");
        builder.append("<div class=\"example-block\">\n");
        builder.append("<div class=\"example-title\">Example: only lava deaths</div>\n");
        builder.append("<pre>events:\n  player.death.attack.lava:\n    message: lava_death\n\nmessages:\n  lava_death:\n    content: \"**{player.name}** tried to swim in lava in {world.name}\"</pre>\n");
        builder.append("</div>\n");
        builder.append("</section>\n");
        return builder.toString();
    }

    private String buildEventKeysSection(List<EventDefinition> events) {
        StringBuilder builder = new StringBuilder();
        builder.append("<section id=\"event-keys\" class=\"doc-section\" data-anchor=\"event-keys\">\n");
        builder.append("<h1>Event key hierarchy</h1>\n");
        builder.append("<p>Event keys use dot notation and support <strong>wildcard matching</strong>. More specific keys match before less specific ones.</p>\n");
        builder.append("<h3>How matching works</h3>\n");
        builder.append("<ul>\n");
        builder.append("<li><code>player.death.attack.dryout.player</code> — matches only that exact death type.</li>\n");
        builder.append("<li><code>player.death.attack.*</code> — matches all attack-type deaths (cactus, lava, etc.).</li>\n");
        builder.append("<li><code>player.death</code> — matches any player death.</li>\n");
        builder.append("</ul>\n");
        builder.append("<h3>Event categories</h3>\n");
        builder.append("<p>Events are grouped by category. Use the search box or sidebar to find events.</p>\n");
        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        for (EventDefinition e : events) {
            if (!e.isSubEvent()) {
                categoryCounts.merge(e.getCategory(), 1L, Long::sum);
            }
        }
        builder.append("<ul>\n");
        for (Map.Entry<String, Long> entry : categoryCounts.entrySet()) {
            builder.append("<li><strong>").append(HtmlEscaper.escape(entry.getKey())).append("</strong> — ")
                    .append(entry.getValue()).append(" event(s)</li>\n");
        }
        builder.append("</ul>\n");
        builder.append("<h3>Sub-events</h3>\n");
        builder.append("<p>Some events (e.g. <code>player.death</code>, <code>entity.damage.player</code>) have <strong>sub-events</strong> ");
        builder.append("discovered from registries and enums. Sub-events inherit predicates from their base, are shown in compact form under the base, ");
        builder.append("and can be referenced under multiple logical groups (e.g. <code>player.death.attack.lava</code> under both <code>player.death</code> and <code>player.death.attack.*</code>).</p>\n");
        builder.append("<p class=\"meta\">Event keys are discovered at startup from Bukkit/Paper registries and enums. ");
        builder.append("Death types, damage causes, and other sub-events are scanned dynamically.</p>\n");
        builder.append("</section>\n");
        return builder.toString();
    }

    private String buildConditionsSection() {
        StringBuilder builder = new StringBuilder();
        builder.append("<section id=\"conditions\" class=\"doc-section\" data-anchor=\"conditions\">\n");
        builder.append("<h1>Conditions</h1>\n");
        builder.append("<p>Add <code>conditions</code> to event rules to filter when the webhook fires. Each condition key must match a predicate from the event.</p>\n");
        builder.append("<h3>Available operators</h3>\n");
        builder.append("<ul>\n");
        builder.append("<li><code>equals</code> — exact match (string or number)</li>\n");
        builder.append("<li><code>not</code> — value not in list</li>\n");
        builder.append("<li><code>greater-than</code> / <code>less-than</code> — numeric comparison</li>\n");
        builder.append("<li><code>regex</code> — pattern match</li>\n");
        builder.append("</ul>\n");
        builder.append("<div class=\"example-block\">\n");
        builder.append("<div class=\"example-title\">Example: only drownings</div>\n");
        builder.append("<pre>events:\n  player.death:\n    message: death_alert\n    conditions:\n      death.message.key:\n        equals: death.attack.drown</pre>\n");
        builder.append("</div>\n");
        builder.append("<div class=\"example-block\">\n");
        builder.append("<div class=\"example-title\">Example: exclude fall damage</div>\n");
        builder.append("<pre>events:\n  entity.damage.player:\n    message: player_damaged\n    conditions:\n      damage.cause:\n        not:\n          - FALL\n          - FALLING_BLOCK</pre>\n");
        builder.append("</div>\n");
        builder.append("</section>\n");
        return builder.toString();
    }

    private String buildMessageStructureSection() {
        StringBuilder builder = new StringBuilder();
        builder.append("<section id=\"message-structure\" class=\"doc-section\" data-anchor=\"message-structure\">\n");
        builder.append("<h1>Message structure</h1>\n");
        builder.append("<p>Event rules set <code>message: &lt;key&gt;</code>; that key is looked up in <code>messages.yaml</code>. ");
        builder.append("The <code>content</code> template uses placeholders like <code>{player.name}</code> — each event's ");
        builder.append("<a class=\"inline-link\" href=\"#events\">predicates</a> can be used as placeholders. ");
        builder.append("Optional <code>username</code> per message sets the webhook display name; see <a class=\"inline-link\" href=\"#webhook-display-name\">Webhook display name</a>.</p>\n");
        builder.append("<div class=\"example-block\">\n");
        builder.append("<div class=\"example-title\">Example</div>\n");
        builder.append("<pre>events.yaml\n");
        builder.append("entity.damage.player:\n");
        builder.append("  message: player_damaged\n\n");
        builder.append("messages.yaml\n");
        builder.append("messages:\n");
        builder.append("  player_damaged:\n");
        builder.append("    content: \"{player.name} took {damage.amount} damage in {world.name}\"");
        builder.append("</pre>\n");
        builder.append("</div>\n");
        builder.append("</section>\n");
        return builder.toString();
    }

    private String buildWebhookDisplayNameSection() {
        StringBuilder builder = new StringBuilder();
        builder.append("<section id=\"webhook-display-name\" class=\"doc-section\" data-anchor=\"webhook-display-name\">\n");
        builder.append("<h1>Webhook display name</h1>\n");
        builder.append("<p>For Discord (and compatible webhooks), the name shown as the message author can be set in three places. ");
        builder.append("The first value found in the order below is used.</p>\n");
        builder.append("<ol>\n");
        builder.append("<li><strong>Per message</strong> — In <code>messages.yaml</code>, add optional <code>username</code> to a message entry. "
                + "Example: <code>messages.player_join.username: \"Join/Quit\"</code>.</li>\n");
        builder.append("<li><strong>Per event or defaults</strong> — In <code>events.yaml</code>, set <code>webhook-username</code> under "
                + "<code>defaults</code> or on any event rule. Example: <code>webhook-username: \"Diamond Alerts\"</code>.</li>\n");
        builder.append("<li><strong>Per webhook</strong> — In <code>config.yaml</code>, add optional <code>username</code> under each webhook. "
                + "Example: <code>webhooks.default.username: \"Server Alerts\"</code>.</li>\n");
        builder.append("</ol>\n");
        builder.append("<p>If none are set, the webhook uses whatever name is configured in the Discord (or provider) webhook settings.</p>\n");
        builder.append("<div class=\"example-block\">\n");
        builder.append("<div class=\"example-title\">Example</div>\n");
        builder.append("<pre>config.yaml\n");
        builder.append("webhooks:\n");
        builder.append("  default:\n");
        builder.append("    url: \"https://discord.com/api/webhooks/...\"\n");
        builder.append("    username: \"Server Alerts\"\n\n");
        builder.append("events.yaml\n");
        builder.append("events:\n");
        builder.append("  player.break.block.diamond_ore:\n");
        builder.append("    webhook: moderation\n");
        builder.append("    webhook-username: \"Diamond Alerts\"\n");
        builder.append("    message: diamond_mined\n\n");
        builder.append("messages.yaml\n");
        builder.append("  player_join:\n");
        builder.append("    content: \"**{player.name}** joined\"\n");
        builder.append("    username: \"Join/Quit\"</pre>\n");
        builder.append("</div>\n");
        builder.append("</section>\n");
        return builder.toString();
    }

    private String buildPlaceholderRegexSpecSection() {
        StringBuilder builder = new StringBuilder();
        builder.append("<section id=\"regex\" class=\"doc-section\" data-anchor=\"regex\">\n");
        builder.append("<h1>Regex</h1>\n");

        builder.append("<h3>Scope</h3>\n");
        builder.append("<p>Message placeholder resolution can transform a placeholder value before substitution using a regex replace. ");
        builder.append("Only the resolution step in <code>MessageResolver</code> is affected; event context building is unchanged.</p>\n");

        builder.append("<h3>Syntax</h3>\n");
        builder.append("<p><strong>Simple placeholder:</strong> <code>{key}</code> uses the context value for <code>key</code> with no transform.</p>\n");
        builder.append("<p><strong>Placeholder with regex replace:</strong> <code>{key|regex:pattern:replacement}</code></p>\n");
        builder.append("<ul>\n");
        builder.append("<li><code>key</code>: the part before <code>|</code> (trimmed); context lookup and redaction use this key.</li>\n");
        builder.append("<li><code>pattern</code>: Java <code>Pattern</code> regex (no flags). First unescaped colon after <code>regex:</code> separates pattern from replacement.</li>\n");
        builder.append("<li><code>replacement</code>: Java <code>Matcher.replaceAll</code> replacement string; <code>$1</code>, <code>$2</code>, … for capture groups, <code>$$</code> for literal <code>$</code>.</li>\n");
        builder.append("<li>Semantics: get context value for <code>key</code>, then <code>Pattern.compile(pattern).matcher(value).replaceAll(replacement)</code>; substitute the result. If the key is redacted, <code>[REDACTED]</code> is substituted and no transform runs.</li>\n");
        builder.append("</ul>\n");
        builder.append("<p><strong>Escaping colons:</strong> To include a literal colon in <code>pattern</code> or <code>replacement</code>, escape it as <code>\\:</code>. ");
        builder.append("Example: <code>{key|regex:foo\\:bar:baz\\:qux}</code> uses pattern <code>foo:bar</code> and replacement <code>baz:qux</code>.</p>\n");
        builder.append("<p><strong>Invalid or missing transform:</strong> If there is no <code>|</code>, or the part after <code>|</code> does not start with <code>regex:</code>, or there is no unescaped colon to split pattern and replacement, the value is used unchanged. Reserved for later: other transform names (e.g. <code>{key|last-path-segment}</code>).</p>\n");

        builder.append("<h3>Example</h3>\n");
        builder.append("<pre>**{world.name|regex:.*/([^/]+)/[^/]+$:$1}** has been loaded</pre>\n");
        builder.append("<p>For <code>world.name</code> = <code>2026/02/hardcore-26/world</code>, the pattern captures the second-to-last path segment; <code>$1</code> yields <code>hardcore-26</code>.</p>\n");
        builder.append("<pre>**hardcore-26** has been loaded</pre>\n");

        builder.append("<h3>Configuration</h3>\n");
        builder.append("<p>No YAML config for regex in v1; the syntax is only in message template strings.</p>\n");

        builder.append("<h3>Security</h3>\n");
        builder.append("<p>Regex runs with a 300 ms timeout in a single-thread executor. On timeout, <code>PatternSyntaxException</code>, or any other exception, the resolver uses the untransformed value and logs a one-time warning per placeholder key.</p>\n");

        builder.append("</section>\n");
        return builder.toString();
    }

    private String buildEventsContent(List<EventDefinition> events, boolean includeSectionWrapper) {
        Map<String, EventDefinition> byKey = new LinkedHashMap<>();
        for (EventDefinition e : events) {
            byKey.put(e.getKey(), e);
        }
        List<EventDefinition> baseEvents = events.stream().filter(e -> !e.isSubEvent()).toList();
        Map<String, List<EventDefinition>> subEventsByParent = new LinkedHashMap<>();
        for (EventDefinition e : events) {
            if (e.isSubEvent() && e.getParentBaseKey() != null) {
                subEventsByParent.computeIfAbsent(e.getParentBaseKey(), k -> new ArrayList<>()).add(e);
            }
        }
        Node root = buildEventTree(baseEvents);
        StringBuilder builder = new StringBuilder();
        if (includeSectionWrapper) {
            builder.append("<section id=\"events\" class=\"doc-section\" data-anchor=\"events\">\n");
            builder.append("<h1>Events</h1>\n");
        } else {
            builder.append("<h1 data-anchor=\"events\" id=\"events\">Event Reference</h1>\n");
        }
        builder.append("<p class=\"meta\">Events are resolved dynamically. Unresolvable keys are warned and ignored. ");
        builder.append("Wildcard precedence favors the most specific match, then less specific keys.</p>\n");
        builder.append("<div id=\"no-results\" class=\"no-results\" aria-live=\"polite\"></div>\n");
        builder.append(buildNestedEventContent(root, "", byKey, subEventsByParent));
        if (includeSectionWrapper) {
            builder.append("</section>\n");
        }
        return builder.toString();
    }

    private Node buildEventTree(List<EventDefinition> events) {
        Node root = new Node("");
        for (EventDefinition event : events) {
            String[] parts = event.getKey().split("\\.");
            Node current = root;
            for (String part : parts) {
                current = current.children.computeIfAbsent(part, Node::new);
            }
            current.eventKey = event.getKey();
        }
        return root;
    }

    private String buildNestedEventContent(Node node, String pathPrefix, Map<String, EventDefinition> byKey,
            Map<String, List<EventDefinition>> subEventsByParent) {
        StringBuilder builder = new StringBuilder();
        String segmentPath = pathPrefix.isEmpty() ? node.name : (pathPrefix + "." + node.name);
        if (node.eventKey != null) {
            EventDefinition event = byKey.get(node.eventKey);
            if (event != null) {
                builder.append(buildEventEntry(event, byKey, subEventsByParent));
            }
        }
        if (!node.children.isEmpty()) {
            for (Node child : node.children.values()) {
                builder.append("<div class=\"event-nest\">");
                builder.append(buildNestedEventContent(child, segmentPath, byKey, subEventsByParent));
                builder.append("</div>");
            }
        }
        return builder.toString();
    }

    private String buildSubEventsSection(String parentKey, List<EventDefinition> subEvents,
            Map<String, EventDefinition> byKey) {
        if (subEvents == null || subEvents.isEmpty()) {
            return "";
        }
        Map<String, List<EventDefinition>> byGroup = new LinkedHashMap<>();
        int prefixLen = parentKey.length() + 1;
        for (EventDefinition sub : subEvents) {
            String suffix = sub.getKey().substring(prefixLen);
            String group = suffix.contains(".") ? suffix.substring(0, suffix.indexOf('.')) : "";
            byGroup.computeIfAbsent(group, k -> new ArrayList<>()).add(sub);
        }
        for (List<EventDefinition> groupList : byGroup.values()) {
            groupList.sort(Comparator.comparing(EventDefinition::getKey));
        }
        StringBuilder builder = new StringBuilder();
        builder.append("<div class=\"event-section sub-events-section\">");
        builder.append("<h3>Sub-events</h3>");
        builder.append("<p class=\"meta\">Inherit predicates from <code>").append(HtmlEscaper.escape(parentKey)).append("</code>. ");
        builder.append("Discovered from registries/enums; shown in compact form.</p>");
        for (Map.Entry<String, List<EventDefinition>> entry : byGroup.entrySet()) {
            String groupLabel = entry.getKey();
            boolean useDetails = byGroup.size() > 1;
            if (useDetails) {
                builder.append("<details class=\"sub-event-group\"><summary>").append(HtmlEscaper.escape(groupLabel)).append("</summary>");
            } else {
                builder.append("<div class=\"sub-event-group\">");
            }
            builder.append("<ul class=\"sub-event-list\">");
            for (EventDefinition sub : entry.getValue()) {
                builder.append(buildSubEventCompactRef(sub, parentKey));
            }
            builder.append("</ul>");
            builder.append(useDetails ? "</details>" : "</div>");
        }
        builder.append("</div>");
        return builder.toString();
    }

    private String buildSubEventCompactRef(EventDefinition sub, String parentKey) {
        String key = sub.getKey();
        StringBuilder builder = new StringBuilder();
        builder.append("<li id=\"").append(HtmlEscaper.escape(key)).append("\" class=\"sub-event-ref\" data-search=\"")
                .append(HtmlEscaper.escape(buildSearchIndex(sub))).append("\">");
        builder.append("<code>").append(HtmlEscaper.escape(key)).append("</code>");
        builder.append(" <span class=\"meta\">— ").append(HtmlEscaper.escape(sub.getDescription())).append("</span>");
        builder.append("</li>");
        return builder.toString();
    }

    private String buildStyles() {
        StringBuilder builder = new StringBuilder();
        builder.append(":root {\n");
        builder.append("  --bg: #141414;\n");
        builder.append("  --sidebar-bg: #1f1f1f;\n");
        builder.append("  --panel-bg: #1b1b1b;\n");
        builder.append("  --text: #e8e8e8;\n");
        builder.append("  --muted: #b0b0b0;\n");
        builder.append("  --accent: #7fbfff;\n");
        builder.append("  --accent-soft: rgba(127, 191, 255, 0.14);\n");
        builder.append("  --border: #2f2f2f;\n");
        builder.append("  --code-bg: #222222;\n");
        builder.append("  --radius: 6px;\n");
        builder.append("  --space-1: 4px;\n");
        builder.append("  --space-2: 8px;\n");
        builder.append("  --space-3: 16px;\n");
        builder.append("  --space-4: 24px;\n");
        builder.append("  --space-5: 32px;\n");
        builder.append("  --max-width: 72ch;\n");
        builder.append("}\n");
        builder.append("* { box-sizing: border-box; }\n");
        builder.append("body {\n");
        builder.append("  font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif;\n");
        builder.append("  margin: 0;\n");
        builder.append("  color: var(--text);\n");
        builder.append("  background: var(--bg);\n");
        builder.append("  line-height: 1.6;\n");
        builder.append("}\n");
        builder.append("a { color: var(--accent); text-decoration: none; }\n");
        builder.append("a:hover { text-decoration: underline; }\n");
        builder.append(".layout { display: flex; min-height: 100vh; }\n");
        builder.append(".sidebar {\n");
        builder.append("  width: 320px;\n");
        builder.append("  max-width: 40vw;\n");
        builder.append("  overflow: auto;\n");
        builder.append("  background: var(--sidebar-bg);\n");
        builder.append("  padding: var(--space-4) var(--space-3);\n");
        builder.append("  border-right: 1px solid var(--border);\n");
        builder.append("  position: sticky;\n");
        builder.append("  top: 0;\n");
        builder.append("  height: 100vh;\n");
        builder.append("}\n");
        builder.append(".sidebar-title { font-size: 1.1rem; font-weight: 600; }\n");
        builder.append(".content { flex: 1; overflow: auto; padding: var(--space-5) var(--space-4); }\n");
        builder.append(".content-inner { max-width: var(--max-width); }\n");
        builder.append("h1 { margin-top: 0; font-size: 1.8rem; }\n");
        builder.append("h2 { margin-top: var(--space-5); font-size: 1.3rem; }\n");
        builder.append("h3 { margin-top: var(--space-4); font-size: 1rem; }\n");
        builder.append(".meta { color: var(--muted); font-size: 0.85rem; }\n");
        builder.append(".doc-section { margin-bottom: var(--space-5); }\n");
        builder.append(".search {\n");
        builder.append("  width: 100%;\n");
        builder.append("  padding: 8px 10px;\n");
        builder.append("  margin: var(--space-3) 0;\n");
        builder.append("  background: var(--panel-bg);\n");
        builder.append("  border: 1px solid var(--border);\n");
        builder.append("  color: var(--text);\n");
        builder.append("  border-radius: var(--radius);\n");
        builder.append("}\n");
        builder.append(".nav { margin-top: var(--space-2); }\n");
        builder.append(".nav-section { margin-bottom: var(--space-3); }\n");
        builder.append(".nav-heading {\n");
        builder.append("  font-size: 0.75rem;\n");
        builder.append("  text-transform: uppercase;\n");
        builder.append("  letter-spacing: 0.08em;\n");
        builder.append("  color: var(--muted);\n");
        builder.append("  margin: var(--space-2) 0;\n");
        builder.append("}\n");
        builder.append(".nav-anchor {\n");
        builder.append("  display: block;\n");
        builder.append("  padding: 4px 8px;\n");
        builder.append("  border-radius: var(--radius);\n");
        builder.append("  color: var(--text);\n");
        builder.append("}\n");
        builder.append(".nav-anchor:hover { background: var(--panel-bg); text-decoration: none; }\n");
        builder.append(".nav-anchor.active {\n");
        builder.append("  background: var(--accent-soft);\n");
        builder.append("  color: var(--accent);\n");
        builder.append("}\n");
        builder.append("details { margin-left: var(--space-2); }\n");
        builder.append("details summary { cursor: pointer; padding: 4px 8px; border-radius: var(--radius); }\n");
        builder.append("details summary:hover { background: var(--panel-bg); }\n");
        builder.append(".nav-item { margin-left: var(--space-2); }\n");
        builder.append(".event-entry {\n");
        builder.append("  margin-bottom: var(--space-4);\n");
        builder.append("  padding-bottom: var(--space-4);\n");
        builder.append("  border-bottom: 1px solid var(--border);\n");
        builder.append("}\n");
        builder.append(".event-nest { margin-left: var(--space-4); border-left: 2px solid var(--border); padding-left: var(--space-3); }\n");
        builder.append(".sub-events-section { border-top: 1px dashed var(--border); margin-top: var(--space-3); padding-top: var(--space-3); }\n");
        builder.append(".sub-event-group { margin-top: var(--space-2); }\n");
        builder.append(".sub-event-group summary { cursor: pointer; color: var(--muted); font-size: 0.9rem; }\n");
        builder.append(".sub-event-list { list-style: none; padding-left: var(--space-3); margin: var(--space-2) 0; }\n");
        builder.append(".sub-event-ref { padding: var(--space-1) 0; font-size: 0.9rem; border-bottom: 1px solid transparent; }\n");
        builder.append(".sub-event-ref code { font-size: 0.85rem; }\n");
        builder.append(".predicate-list li { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-2); align-items: center; }\n");
        builder.append(".predicate-msg, .predicate-cond { font-size: 0.95em; }\n");
        builder.append(".event-section { margin-top: var(--space-3); }\n");
        builder.append("code, pre {\n");
        builder.append("  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, \"Liberation Mono\", monospace;\n");
        builder.append("  background: var(--code-bg);\n");
        builder.append("  border-radius: var(--radius);\n");
        builder.append("  color: var(--text);\n");
        builder.append("}\n");
        builder.append("code { padding: 2px 6px; }\n");
        builder.append("pre {\n");
        builder.append("  padding: var(--space-3);\n");
        builder.append("  overflow: auto;\n");
        builder.append("  border-left: 3px solid var(--accent);\n");
        builder.append("}\n");
        builder.append(".no-results {\n");
        builder.append("  display: none;\n");
        builder.append("  padding: var(--space-3);\n");
        builder.append("  border: 1px dashed var(--border);\n");
        builder.append("  border-radius: var(--radius);\n");
        builder.append("  background: var(--panel-bg);\n");
        builder.append("  margin: var(--space-3) 0;\n");
        builder.append("}\n");
        builder.append(".example-block {\n");
        builder.append("  margin-top: var(--space-3);\n");
        builder.append("  padding: var(--space-3);\n");
        builder.append("  background: var(--panel-bg);\n");
        builder.append("  border-radius: var(--radius);\n");
        builder.append("  border: 1px solid var(--border);\n");
        builder.append("}\n");
        builder.append(".example-title { font-weight: 600; margin-bottom: var(--space-2); }\n");
        builder.append(".inline-link { color: var(--accent); }\n");
        return builder.toString();
    }

    private String buildScripts() {
        StringBuilder builder = new StringBuilder();
        builder.append("(() => {\n");
        builder.append("  const input = document.getElementById('search');\n");
        builder.append("  const noResults = document.getElementById('no-results');\n");
        builder.append("  const eventEntries = Array.from(document.querySelectorAll('.event-entry'));\n");
        builder.append("  const navItems = Array.from(document.querySelectorAll('.nav-item'));\n");
        builder.append("  const navAnchors = Array.from(document.querySelectorAll('.nav-anchor'));\n");
        builder.append("  const detailsNodes = Array.from(document.querySelectorAll('.nav details'));\n");
        builder.append("  const content = document.querySelector('.content');\n");
        builder.append("  const anchorTargets = Array.from(document.querySelectorAll('[data-anchor]'));\n");
        builder.append("  const anchorOffsets = () => anchorTargets.map(el => ({\n");
        builder.append("    id: el.getAttribute('data-anchor'),\n");
        builder.append("    top: el.offsetTop\n");
        builder.append("  })).sort((a, b) => a.top - b.top);\n");
        builder.append("  let cachedOffsets = anchorOffsets();\n");
        builder.append("\n");
        builder.append("  const setActiveAnchor = (id) => {\n");
        builder.append("    if (!id) return;\n");
        builder.append("    navAnchors.forEach(anchor => {\n");
        builder.append("      const target = anchor.getAttribute('href').slice(1);\n");
        builder.append("      anchor.classList.toggle('active', target === id);\n");
        builder.append("    });\n");
        builder.append("  };\n");
        builder.append("\n");
        builder.append("  const updateActiveOnScroll = () => {\n");
        builder.append("    if (!content || cachedOffsets.length === 0) return;\n");
        builder.append("    const scrollTop = content.scrollTop + 8;\n");
        builder.append("    let current = cachedOffsets[0];\n");
        builder.append("    for (const entry of cachedOffsets) {\n");
        builder.append("      if (scrollTop >= entry.top) {\n");
        builder.append("        current = entry;\n");
        builder.append("      } else {\n");
        builder.append("        break;\n");
        builder.append("      }\n");
        builder.append("    }\n");
        builder.append("    setActiveAnchor(current.id);\n");
        builder.append("  };\n");
        builder.append("\n");
        builder.append("  const updateSearch = () => {\n");
        builder.append("    const term = input ? input.value.trim().toLowerCase() : '';\n");
        builder.append("    eventEntries.forEach(entry => {\n");
        builder.append("      const hay = entry.getAttribute('data-search') || '';\n");
        builder.append("      entry.style.display = hay.includes(term) ? '' : 'none';\n");
        builder.append("    });\n");
        builder.append("    navItems.forEach(item => {\n");
        builder.append("      const hay = item.getAttribute('data-search') || '';\n");
        builder.append("      item.style.display = hay.includes(term) ? '' : 'none';\n");
        builder.append("    });\n");
        builder.append("    if (term.length > 0) {\n");
        builder.append("      detailsNodes.forEach(details => {\n");
        builder.append("        const hasVisibleChild = Array.from(details.querySelectorAll('.nav-item'))\n");
        builder.append("          .some(item => item.style.display !== 'none');\n");
        builder.append("        details.open = hasVisibleChild;\n");
        builder.append("      });\n");
        builder.append("    }\n");
        builder.append("    const hasResults = eventEntries.some(entry => entry.style.display !== 'none');\n");
        builder.append("    if (noResults) {\n");
        builder.append("      if (term.length > 0 && !hasResults) {\n");
        builder.append("        noResults.textContent = `No events or predicates match \"${term}\".`;\n");
        builder.append("        noResults.style.display = 'block';\n");
        builder.append("      } else {\n");
        builder.append("        noResults.style.display = 'none';\n");
        builder.append("      }\n");
        builder.append("    }\n");
        builder.append("  };\n");
        builder.append("\n");
        builder.append("  if (input) {\n");
        builder.append("    input.addEventListener('input', updateSearch);\n");
        builder.append("  }\n");
        builder.append("  window.addEventListener('hashchange', () => {\n");
        builder.append("    const hash = window.location.hash.slice(1);\n");
        builder.append("    setActiveAnchor(hash);\n");
        builder.append("  });\n");
        builder.append("  if (content) {\n");
        builder.append("    content.addEventListener('scroll', updateActiveOnScroll);\n");
        builder.append("  }\n");
        builder.append("  window.addEventListener('resize', () => {\n");
        builder.append("    cachedOffsets = anchorOffsets();\n");
        builder.append("    updateActiveOnScroll();\n");
        builder.append("  });\n");
        builder.append("  cachedOffsets = anchorOffsets();\n");
        builder.append("  updateActiveOnScroll();\n");
        builder.append("  updateSearch();\n");
        builder.append("})();\n");
        return builder.toString();
    }

    private String buildNavigation(List<EventDefinition> events) {
        List<EventDefinition> baseEvents = events.stream().filter(e -> !e.isSubEvent()).toList();
        Node root = new Node("");
        for (EventDefinition event : baseEvents) {
            String[] parts = event.getKey().split("\\.");
            Node current = root;
            for (String part : parts) {
                current = current.children.computeIfAbsent(part, Node::new);
            }
            current.eventKey = event.getKey();
        }
        StringBuilder builder = new StringBuilder();
        builder.append(buildNode(root));
        return builder.toString();
    }

    private String buildNode(Node node) {
        StringBuilder builder = new StringBuilder();
        if (!node.children.isEmpty()) {
            for (Node child : node.children.values()) {
                builder.append("<details open><summary>").append(HtmlEscaper.escape(child.name))
                        .append("</summary>");
                builder.append(buildNode(child));
                builder.append("</details>");
            }
        }
        if (node.eventKey != null) {
            builder.append("<div class=\"nav-item\" data-search=\"")
                    .append(HtmlEscaper.escape(node.eventKey.toLowerCase()))
                    .append("\"><a class=\"nav-anchor\" href=\"#")
                    .append(HtmlEscaper.escape(node.eventKey))
                    .append("\">")
                    .append(HtmlEscaper.escape(node.eventKey))
                    .append("</a></div>");
        }
        return builder.toString();
    }

    private String buildEventEntry(EventDefinition event, Map<String, EventDefinition> byKey,
            Map<String, List<EventDefinition>> subEventsByParent) {
        StringBuilder builder = new StringBuilder();
        StringBuilder searchIndex = new StringBuilder(buildSearchIndex(event));
        List<EventDefinition> subEventsForSearch = subEventsByParent.get(event.getKey());
        if (subEventsForSearch != null) {
            for (EventDefinition sub : subEventsForSearch) {
                searchIndex.append(" ").append(buildSearchIndex(sub));
            }
        }
        builder.append("<div class=\"event-entry\" id=\"").append(HtmlEscaper.escape(event.getKey())).append("\"");
        builder.append(" data-anchor=\"").append(HtmlEscaper.escape(event.getKey())).append("\"");
        builder.append(" data-search=\"").append(HtmlEscaper.escape(searchIndex.toString())).append("\">");
        builder.append("<h2>").append(HtmlEscaper.escape(event.getKey())).append("</h2>");
        if (!event.isSubEvent()) {
            String parentKey = findParentEventKey(event.getKey(), byKey);
            if (parentKey != null) {
                builder.append("<div class=\"meta\">(Inherits from ").append(HtmlEscaper.escape(parentKey)).append(")</div>");
            }
        }
        builder.append("<div class=\"meta\">Category: ").append(HtmlEscaper.escape(event.getCategory())).append("</div>");
        builder.append("<p>").append(HtmlEscaper.escape(event.getDescription())).append("</p>");
        if (event.getKey().startsWith("player.death.") && event.getPredicateFields().containsKey("death.message.key")) {
            String minecraftKey = "death." + event.getKey().substring("player.death.".length());
            builder.append("<div class=\"event-section\">");
            builder.append("<h3>Minecraft translation key</h3>");
            builder.append("<p>Use <code>death.message.key</code> in conditions to match this death type. ");
            builder.append("Full key: <code>").append(HtmlEscaper.escape(minecraftKey)).append("</code></p>");
            builder.append("</div>");
        }
        builder.append("<div class=\"event-section\">");
        builder.append("<h3>Predicates</h3>");
        builder.append("<p>Use in messages with <code>{key}</code> or in conditions as <code>key:</code>:</p>");
        builder.append("<ul class=\"predicate-list\">");
        for (Map.Entry<String, String> entry : event.getPredicateFields().entrySet()) {
            builder.append("<li><span class=\"predicate-msg\">In messages: <code>{")
                    .append(HtmlEscaper.escape(entry.getKey())).append("}</code></span>")
                    .append(" <span class=\"predicate-cond\">In conditions: <code>")
                    .append(HtmlEscaper.escape(entry.getKey())).append(":</code></span>")
                    .append(" <span class=\"meta\">").append(HtmlEscaper.escape(entry.getValue())).append("</span></li>");
        }
        builder.append("</ul>");
        builder.append("</div>");
        builder.append("<div class=\"event-section\">");
        builder.append("<h3>Wildcard support</h3>");
        builder.append("<p>These patterns match this event or its children:</p>");
        for (String example : event.getWildcardExamples()) {
            builder.append("<div><code>").append(HtmlEscaper.escape(example)).append("</code></div>");
        }
        builder.append("</div>");
        builder.append("<div class=\"event-section\">");
        builder.append("<h3>events.yaml example</h3>");
        builder.append("<pre>").append(HtmlEscaper.escape(event.getExampleYaml())).append("</pre>");
        builder.append("</div>");
        List<EventDefinition> subEvents = subEventsByParent.get(event.getKey());
        if (subEvents != null && !subEvents.isEmpty()) {
            builder.append(buildSubEventsSection(event.getKey(), subEvents, byKey));
        }
        builder.append("</div>");
        return builder.toString();
    }

    private String findParentEventKey(String eventKey, Map<String, EventDefinition> byKey) {
        int lastDot = eventKey.lastIndexOf('.');
        while (lastDot > 0) {
            String candidate = eventKey.substring(0, lastDot);
            if (byKey.containsKey(candidate)) {
                return candidate;
            }
            lastDot = candidate.lastIndexOf('.');
        }
        return null;
    }

    private String buildSearchIndex(EventDefinition event) {
        StringBuilder builder = new StringBuilder();
        builder.append(event.getKey()).append(" ").append(event.getCategory());
        builder.append(" ").append(event.getDescription());
        for (String field : event.getPredicateFields().keySet()) {
            builder.append(" ").append(field);
        }
        for (String w : event.getWildcardExamples()) {
            builder.append(" ").append(w);
        }
        if (event.getKey().startsWith("player.death.")) {
            builder.append(" death ").append("death.").append(event.getKey().substring("player.death.".length()));
        }
        return builder.toString().toLowerCase();
    }

    private String buildEventsJson(List<EventDefinition> events) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        boolean first = true;
        for (EventDefinition event : events) {
            if (!first) {
                builder.append(",");
            }
            first = false;
            builder.append("{");
            builder.append("\"event\":\"").append(JsonEscaper.escape(event.getKey())).append("\",");
            builder.append("\"category\":\"").append(JsonEscaper.escape(event.getCategory())).append("\",");
            if (event.isSubEvent() && event.getParentBaseKey() != null) {
                builder.append("\"parent_base_key\":\"").append(JsonEscaper.escape(event.getParentBaseKey())).append("\",");
            }
            builder.append("\"description\":\"").append(JsonEscaper.escape(event.getDescription())).append("\",");
            builder.append("\"predicates\":{");
            boolean firstPredicate = true;
            for (Map.Entry<String, String> entry : event.getPredicateFields().entrySet()) {
                if (!firstPredicate) {
                    builder.append(",");
                }
                firstPredicate = false;
                builder.append("\"").append(JsonEscaper.escape(entry.getKey())).append("\":\"")
                        .append(JsonEscaper.escape(entry.getValue())).append("\"");
            }
            builder.append("},");
            builder.append("\"wildcards\":[");
            boolean firstWildcard = true;
            for (String example : event.getWildcardExamples()) {
                if (!firstWildcard) {
                    builder.append(",");
                }
                firstWildcard = false;
                builder.append("\"").append(JsonEscaper.escape(example)).append("\"");
            }
            builder.append("]");
            builder.append("}");
        }
        builder.append("]");
        return builder.toString();
    }

    private static class Node {
        private final String name;
        private final Map<String, Node> children = new LinkedHashMap<>();
        private String eventKey;

        private Node(String name) {
            this.name = name;
        }
    }
}

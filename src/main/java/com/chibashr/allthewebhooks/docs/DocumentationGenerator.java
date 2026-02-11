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
        List<EventDefinition> baseEvents = events.stream().filter(e -> !e.isSubEvent()).toList();
        List<EventDefinition> subEvents = events.stream().filter(EventDefinition::isSubEvent).toList();
        String sidebar = buildWikiSidebar(events, baseEvents, subEvents);
        String content = buildWikiContent(events, baseEvents, subEvents);
        return buildDocsShell("All the Webhooks - Documentation", sidebar, content);
    }

    private String buildDocsShell(String title, String sidebar, String content) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html>\n");
        builder.append("<html lang=\"en\">\n");
        builder.append("<head>\n");
        builder.append("<meta charset=\"utf-8\">\n");
        builder.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        builder.append("<title>").append(HtmlEscaper.escape(title)).append("</title>\n");
        builder.append("<style>\n");
        builder.append(buildStyles());
        builder.append("</style>\n");
        builder.append("</head>\n");
        builder.append("<body>\n");
        builder.append("<div class=\"layout\">\n");
        builder.append(sidebar);
        builder.append("<div class=\"sidebar-resize-handle\" id=\"sidebarResizeHandle\" title=\"Drag to resize\"></div>\n");
        builder.append("<div class=\"main-content\">\n");
        builder.append("<div class=\"content-header\">\n");
        builder.append("<div class=\"breadcrumb\" id=\"breadcrumb\"></div>\n");
        builder.append("<div class=\"content-title\" id=\"headerTitle\">All the Webhooks</div>\n");
        builder.append("<div class=\"content-meta\" id=\"headerMeta\">Select a section from the sidebar</div>\n");
        builder.append("</div>\n");
        builder.append("<div class=\"content-body\" id=\"contentBody\">\n");
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

    /** Derives subcategory from event key (second segment, e.g. player.death → death). */
    private String getSubcategory(EventDefinition e) {
        String[] parts = e.getKey().split("\\.");
        return parts.length >= 2 ? parts[1] : null;
    }

    private String buildWikiSidebar(List<EventDefinition> events, List<EventDefinition> baseEvents,
            List<EventDefinition> subEvents) {
        StringBuilder builder = new StringBuilder();
        builder.append("<div class=\"sidebar\" id=\"sidebar\">\n");
        builder.append("<div class=\"sidebar-header\">\n");
        builder.append("<div class=\"sidebar-title\">All the Webhooks</div>\n");
        builder.append("<div class=\"meta\">Version: ").append(HtmlEscaper.escape(plugin.getDescription().getVersion()))
                .append("</div>\n");
        builder.append("<div class=\"meta\">Server: ").append(HtmlEscaper.escape(plugin.getServer().getVersion()))
                .append("</div>\n");
        builder.append("<input type=\"text\" class=\"search-input\" id=\"globalSearch\" placeholder=\"Search docs...\">\n");
        builder.append("</div>\n");
        builder.append("<div class=\"sidebar-docs\">\n");
        builder.append("<div class=\"nav-section-title nav-section-title-docs\">Documentation</div>\n");
        builder.append("<div class=\"doc-nav\">\n");
        builder.append("<div class=\"nav-link\" data-section=\"welcome\">Overview</div>\n");
        builder.append("<div class=\"nav-link\" data-section=\"quick-start\">Quick start</div>\n");
        builder.append("<div class=\"nav-link\" data-section=\"event-keys\">Event key hierarchy</div>\n");
        builder.append("<div class=\"nav-link\" data-section=\"message-structure\">Message structure</div>\n");
        builder.append("<div class=\"nav-link\" data-section=\"conditions\">Conditions</div>\n");
        builder.append("<div class=\"nav-link\" data-section=\"webhook-display-name\">Webhook display name</div>\n");
        builder.append("<div class=\"nav-link\" data-section=\"regex\">Regex</div>\n");
        builder.append("</div>\n");
        builder.append("</div>\n");
        builder.append("<div class=\"sidebar-events-block\">\n");
        builder.append("<div class=\"nav-section-title nav-section-title-events\">Events</div>\n");
        builder.append("<div class=\"events-tabs\">\n");
        builder.append("<button class=\"events-tab active\" data-tab=\"events\">Events</button>\n");
        builder.append("<button class=\"events-tab\" data-tab=\"sub-events\">Sub-events</button>\n");
        builder.append("</div>\n");
        builder.append("<div class=\"events-tab-panels\">\n");
        builder.append("<div class=\"events-tab-panel active\" id=\"eventsTabPanel\">\n");
        builder.append(buildEventsNavSection(baseEvents, "events", "Search events..."));
        builder.append("</div>\n");
        builder.append("<div class=\"events-tab-panel\" id=\"sub-eventsTabPanel\">\n");
        builder.append("<input type=\"hidden\" id=\"subEventsParentFilter\" value=\"\">\n");
        builder.append("<div class=\"sub-events-filter-indicator\" id=\"subEventsFilterIndicator\"></div>\n");
        builder.append(buildEventsNavSection(subEvents, "sub-events", "Search sub-events..."));
        builder.append("</div>\n");
        builder.append("</div>\n");
        builder.append("</div>\n");
        builder.append("</div>\n");
        return builder.toString();
    }

    private String buildEventsNavSection(List<EventDefinition> events, String sectionId, String searchPlaceholder) {
        Map<String, Map<String, List<EventDefinition>>> hierarchical = new LinkedHashMap<>();
        for (EventDefinition e : events) {
            String category = e.getCategory();
            String subcategory = getSubcategory(e);
            String subcat = subcategory != null ? subcategory : "_root";
            hierarchical.computeIfAbsent(category, k -> new LinkedHashMap<>())
                    .computeIfAbsent(subcat, k -> new ArrayList<>()).add(e);
        }
        for (Map<String, List<EventDefinition>> subcats : hierarchical.values()) {
            for (List<EventDefinition> list : subcats.values()) {
                list.sort(Comparator.comparing(EventDefinition::getKey));
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append("<input type=\"text\" class=\"search-input ").append(sectionId).append("-search\" ");
        builder.append("placeholder=\"").append(HtmlEscaper.escape(searchPlaceholder)).append("\">\n");
        builder.append("<div class=\"sidebar-actions\">\n");
        builder.append("<button class=\"action-btn ").append(sectionId).append("-expand\">Expand All</button>\n");
        builder.append("<button class=\"action-btn ").append(sectionId).append("-collapse\">Collapse All</button>\n");
        builder.append("</div>\n");
        builder.append("<div class=\"category-nav\" id=\"").append(sectionId).append("Nav\">\n");
        List<String> categories = new ArrayList<>(hierarchical.keySet());
        categories.sort(String::compareTo);
        for (String category : categories) {
            Map<String, List<EventDefinition>> subcats = hierarchical.get(category);
            int total = subcats.values().stream().mapToInt(List::size).sum();
            boolean hasSubcats = subcats.size() > 1 || !subcats.containsKey("_root");
            builder.append("<div class=\"category-group\">\n");
            builder.append("<div class=\"category-header\">\n");
            builder.append("<span class=\"category-toggle\">▶</span>\n");
            builder.append("<span class=\"category-name\">").append(HtmlEscaper.escape(category)).append("</span>\n");
            builder.append("<span class=\"event-count\">").append(total).append("</span>\n");
            builder.append("</div>\n");
            builder.append("<div class=\"category-events\">\n");
            if (hasSubcats) {
                List<String> subcatKeys = new ArrayList<>(subcats.keySet());
                subcatKeys.sort((a, b) -> a.equals("_root") ? 1 : b.equals("_root") ? -1 : a.compareTo(b));
                for (String subcat : subcatKeys) {
                    if ("_root".equals(subcat)) {
                        for (EventDefinition e : subcats.get(subcat)) {
                            builder.append(buildEventNavLink(e, sectionId, 40));
                        }
                    } else {
                        List<EventDefinition> subList = subcats.get(subcat);
                        builder.append("<div class=\"subcategory-group\">\n");
                        builder.append("<div class=\"subcategory-header\">\n");
                        builder.append("<span class=\"subcategory-toggle\">▶</span>\n");
                        builder.append("<span class=\"category-name\">").append(HtmlEscaper.escape(subcat)).append("</span>\n");
                        builder.append("<span class=\"event-count\">").append(subList.size()).append("</span>\n");
                        builder.append("</div>\n");
                        builder.append("<div class=\"subcategory-events\">\n");
                        for (EventDefinition e : subList) {
                            builder.append(buildEventNavLink(e, sectionId, 60));
                        }
                        builder.append("</div>\n");
                        builder.append("</div>\n");
                    }
                }
            } else {
                for (EventDefinition e : subcats.get("_root")) {
                    builder.append(buildEventNavLink(e, sectionId, 40));
                }
            }
            builder.append("</div>\n");
            builder.append("</div>\n");
        }
        builder.append("</div>\n");
        return builder.toString();
    }

    private String buildEventNavLink(EventDefinition e, String sectionId, int indentPx) {
        return "<div class=\"event-link\" data-section=\"" + sectionId + "\" data-event-key=\""
                + HtmlEscaper.escape(e.getKey()) + "\" style=\"padding-left:" + indentPx + "px\">"
                + HtmlEscaper.escape(e.getKey()) + "</div>\n";
    }

    private String buildWikiContent(List<EventDefinition> events, List<EventDefinition> baseEvents,
            List<EventDefinition> subEvents) {
        Map<String, EventDefinition> byKey = new LinkedHashMap<>();
        for (EventDefinition e : events) {
            byKey.put(e.getKey(), e);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("<div id=\"welcomePanel\" class=\"panel active\">\n");
        builder.append(buildWelcomeContent(events, baseEvents, subEvents));
        builder.append("</div>\n");
        builder.append("<div id=\"quick-startPanel\" class=\"panel\">\n");
        builder.append(innerContent(buildQuickStartSection()));
        builder.append("</div>\n");
        builder.append("<div id=\"event-keysPanel\" class=\"panel\">\n");
        builder.append(innerContent(buildEventKeysSection(events)));
        builder.append("</div>\n");
        builder.append("<div id=\"message-structurePanel\" class=\"panel\">\n");
        builder.append(innerContent(buildMessageStructureSection()));
        builder.append("</div>\n");
        builder.append("<div id=\"conditionsPanel\" class=\"panel\">\n");
        builder.append(innerContent(buildConditionsSection()));
        builder.append("</div>\n");
        builder.append("<div id=\"webhook-display-namePanel\" class=\"panel\">\n");
        builder.append(innerContent(buildWebhookDisplayNameSection()));
        builder.append("</div>\n");
        builder.append("<div id=\"regexPanel\" class=\"panel\"><div id=\"regex\" data-anchor=\"regex\">\n");
        builder.append(innerContent(buildPlaceholderRegexSpecSection()));
        builder.append("</div></div>\n");
        builder.append("<div id=\"eventDetailPanel\" class=\"panel\"></div>\n");
        builder.append("<script>window.__eventsData=").append(buildEventsJson(events)).append(";</script>\n");
        return builder.toString();
    }

    private String innerContent(String sectionHtml) {
        return sectionHtml.replaceFirst("<section[^>]*>", "").replaceFirst("</section>", "");
    }

    private String buildWelcomeContent(List<EventDefinition> events, List<EventDefinition> baseEvents,
            List<EventDefinition> subEvents) {
        StringBuilder builder = new StringBuilder();
        builder.append("<div class=\"welcome-screen\">\n");
        builder.append("<h1 class=\"welcome-title\">All the Webhooks</h1>\n");
        builder.append("<p class=\"welcome-text\">Configure events in <code>events.yaml</code>, messages in ");
        builder.append("<code>messages.yaml</code>, and webhooks in <code>config.yaml</code>. ");
        builder.append("Generate docs with <code>/allthewebhooks docs generate</code>.</p>\n");
        builder.append("<p class=\"welcome-text\">Use the sidebar to browse documentation, events, and sub-events. ");
        builder.append("Each event includes predicates and wildcard patterns for filtering.</p>\n");
        builder.append("<div class=\"stats-grid\">\n");
        builder.append("<div class=\"stat-card\"><div class=\"stat-value\">").append(events.size()).append("</div>");
        builder.append("<div class=\"stat-label\">Total Events</div></div>\n");
        builder.append("<div class=\"stat-card\"><div class=\"stat-value\">").append(baseEvents.size()).append("</div>");
        builder.append("<div class=\"stat-label\">Base Events</div></div>\n");
        builder.append("<div class=\"stat-card\"><div class=\"stat-value\">").append(subEvents.size()).append("</div>");
        builder.append("<div class=\"stat-label\">Sub-events</div></div>\n");
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
        builder.append("* { margin: 0; padding: 0; box-sizing: border-box; }\n");
        builder.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif; ");
        builder.append("background: #1a1a1a; color: #e0e0e0; display: flex; height: 100vh; overflow: hidden; }\n");
        builder.append(".layout { display: flex; flex: 1; overflow: hidden; min-width: 0; }\n");
        builder.append(".sidebar { width: 300px; min-width: 200px; max-width: 500px; background: #2d2d2d; ");
        builder.append("display: flex; flex-direction: column; overflow: hidden; flex-shrink: 0; }\n");
        builder.append(".sidebar-resize-handle { width: 6px; background: #404040; cursor: col-resize; flex-shrink: 0; }\n");
        builder.append(".sidebar-resize-handle:hover { background: #4a9eff; }\n");
        builder.append(".sidebar-header { padding: 20px; border-bottom: 1px solid #404040; background: #252525; flex-shrink: 0; }\n");
        builder.append(".sidebar-title { font-size: 14px; font-weight: 600; color: #888; text-transform: uppercase; ");
        builder.append("letter-spacing: 1px; margin-bottom: 15px; }\n");
        builder.append(".search-input { width: 100%; padding: 8px 12px; background: #1a1a1a; border: 1px solid #404040; ");
        builder.append("border-radius: 4px; color: #e0e0e0; font-size: 13px; }\n");
        builder.append(".search-input:focus { outline: none; border-color: #4a9eff; }\n");
        builder.append(".sidebar-docs { flex-shrink: 0; overflow-y: auto; padding: 10px 0; border-bottom: 2px solid #1a4a6e; }\n");
        builder.append(".sidebar-events-block { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; border-top: 2px solid #4a6e1a; }\n");
        builder.append(".nav-section-title { padding: 12px 20px; font-size: 12px; font-weight: 600; ");
        builder.append("text-transform: uppercase; letter-spacing: 0.5px; flex-shrink: 0; }\n");
        builder.append(".nav-section-title-docs { background: #1a3a4a; color: #5db8e8; border-bottom: 1px solid #204a5a; }\n");
        builder.append(".nav-section-title-events { background: #2a3a1a; color: #8bc34a; border-bottom: 1px solid #304a20; }\n");
        builder.append(".events-tabs { display: flex; flex-shrink: 0; border-bottom: 1px solid #404040; }\n");
        builder.append(".events-tab { flex: 1; padding: 8px 12px; background: #252525; border: none; color: #888; font-size: 12px; ");
        builder.append("cursor: pointer; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; transition: all 0.15s; }\n");
        builder.append(".events-tab:hover { background: #303030; color: #b0b0b0; }\n");
        builder.append(".events-tab.active { background: #2a3a1a; color: #8bc34a; border-bottom: 2px solid #8bc34a; }\n");
        builder.append(".events-tab-panels { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }\n");
        builder.append(".events-tab-panel { display: none; flex: 1; min-height: 0; flex-direction: column; overflow: hidden; padding: 10px; }\n");
        builder.append(".events-tab-panel.active { display: flex; overflow-y: auto; }\n");
        builder.append(".nav-link { padding: 8px 20px; cursor: pointer; color: #b0b0b0; font-size: 14px; ");
        builder.append("transition: background 0.15s, color 0.15s; }\n");
        builder.append(".nav-link:hover, .nav-link.active { background: #353535; color: #e0e0e0; }\n");
        builder.append(".nav-link.active { color: #4a9eff; }\n");
        builder.append(".doc-nav .nav-link { padding-left: 20px; }\n");
        builder.append(".category-group { margin-bottom: 2px; }\n");
        builder.append(".category-header, .subcategory-header { cursor: pointer; display: flex; align-items: center; gap: 8px; ");
        builder.append("color: #b0b0b0; font-size: 14px; transition: background 0.15s, color 0.15s; user-select: none; }\n");
        builder.append(".category-header { padding: 8px 20px; }\n");
        builder.append(".category-header:hover, .subcategory-header:hover { background: #353535; color: #e0e0e0; }\n");
        builder.append(".category-toggle, .subcategory-toggle { font-size: 9px; transition: transform 0.2s; color: #888; width: 12px; flex-shrink: 0; }\n");
        builder.append(".category-group.expanded > .category-header .category-toggle, ");
        builder.append(".subcategory-group.expanded > .subcategory-header .subcategory-toggle { transform: rotate(90deg); }\n");
        builder.append(".subcategory-header { padding: 6px 20px 6px 40px; font-size: 13px; color: #999; }\n");
        builder.append(".subcategory-header:hover { background: #323232; color: #b0b0b0; }\n");
        builder.append(".category-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }\n");
        builder.append(".event-count { font-size: 11px; color: #666; background: #222; padding: 2px 6px; border-radius: 10px; flex-shrink: 0; }\n");
        builder.append(".category-events, .subcategory-events { max-height: 0; overflow: hidden; transition: max-height 0.3s ease-out; }\n");
        builder.append(".category-group.expanded > .category-events { max-height: 5000px; }\n");
        builder.append(".subcategory-group.expanded > .subcategory-events { max-height: 3000px; }\n");
        builder.append(".event-link { padding: 6px 20px 6px 40px; cursor: pointer; color: #888; font-size: 12px; ");
        builder.append("font-family: 'Courier New', monospace; transition: background 0.15s, color 0.15s; overflow: hidden; text-overflow: ellipsis; }\n");
        builder.append(".event-link:hover { background: #323232; color: #b0b0b0; }\n");
        builder.append(".event-link.active { background: #3d5266; color: #4a9eff; }\n");
        builder.append(".sidebar-actions { padding: 10px 20px; border-bottom: 1px solid #404040; display: flex; gap: 8px; }\n");
        builder.append(".action-btn { flex: 1; padding: 6px 12px; background: #1a1a1a; border: 1px solid #404040; ");
        builder.append("color: #888; font-size: 11px; cursor: pointer; border-radius: 3px; transition: all 0.15s; }\n");
        builder.append(".action-btn:hover { background: #252525; color: #b0b0b0; border-color: #4a9eff; }\n");
        builder.append(".main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }\n");
        builder.append(".content-header { padding: 20px 40px; border-bottom: 1px solid #404040; background: #252525; flex-shrink: 0; }\n");
        builder.append(".content-title { font-size: 32px; font-weight: 600; color: #e0e0e0; margin-bottom: 12px; }\n");
        builder.append(".content-meta { font-size: 13px; color: #888; line-height: 1.6; }\n");
        builder.append(".breadcrumb { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; font-size: 12px; color: #666; }\n");
        builder.append(".breadcrumb-separator { color: #444; }\n");
        builder.append(".breadcrumb-item { color: #888; }\n");
        builder.append(".breadcrumb-item.active { color: #4a9eff; }\n");
        builder.append(".content-body { flex: 1; overflow-y: auto; padding: 40px; }\n");
        builder.append(".panel { display: none; }\n");
        builder.append(".panel.active { display: block; }\n");
        builder.append(".welcome-screen { max-width: 800px; }\n");
        builder.append(".welcome-title { font-size: 36px; font-weight: 600; margin-bottom: 20px; }\n");
        builder.append(".welcome-text { font-size: 16px; line-height: 1.8; color: #b0b0b0; margin-bottom: 20px; }\n");
        builder.append(".stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-top: 40px; }\n");
        builder.append(".stat-card { padding: 20px; background: #252525; border: 1px solid #404040; border-radius: 4px; }\n");
        builder.append(".stat-value { font-size: 32px; font-weight: 700; color: #4a9eff; margin-bottom: 8px; }\n");
        builder.append(".stat-label { font-size: 13px; color: #888; text-transform: uppercase; letter-spacing: 0.5px; }\n");
        builder.append(".event-detail { max-width: 900px; }\n");
        builder.append(".event-name { font-size: 24px; font-weight: 600; color: #e0e0e0; font-family: 'Courier New', monospace; margin-bottom: 12px; }\n");
        builder.append(".event-badges { display: flex; gap: 8px; flex-wrap: wrap; }\n");
        builder.append(".event-category-badge { padding: 4px 10px; background: #3d5266; color: #4a9eff; font-size: 12px; ");
        builder.append("font-weight: 600; border-radius: 3px; text-transform: uppercase; letter-spacing: 0.5px; }\n");
        builder.append(".event-subcategory-badge { padding: 4px 10px; background: #2d3d4a; color: #7fb8e8; font-size: 11px; font-weight: 600; border-radius: 3px; }\n");
        builder.append(".event-relation-links { margin: 12px 0; }\n");
        builder.append(".event-relation-link { color: #4a9eff; }\n");
        builder.append(".event-description { margin-top: 20px; padding: 16px; background: #252525; border-left: 3px solid #4a9eff; ");
        builder.append("color: #b0b0b0; line-height: 1.6; }\n");
        builder.append(".sub-events-filter-indicator { margin-bottom: 8px; font-size: 12px; }\n");
        builder.append(".section { margin-top: 40px; }\n");
        builder.append(".section-heading { font-size: 18px; font-weight: 600; color: #e0e0e0; margin-bottom: 16px; padding-bottom: 8px; border-bottom: 1px solid #404040; }\n");
        builder.append(".predicates-table { width: 100%; border-collapse: collapse; font-family: 'Courier New', monospace; font-size: 13px; }\n");
        builder.append(".predicates-table th { text-align: left; padding: 12px; background: #252525; color: #888; font-weight: 600; ");
        builder.append("text-transform: uppercase; font-size: 11px; letter-spacing: 0.5px; border-bottom: 1px solid #404040; }\n");
        builder.append(".predicates-table td { padding: 12px; border-bottom: 1px solid #2a2a2a; color: #b0b0b0; }\n");
        builder.append(".predicates-table tr:hover { background: #252525; }\n");
        builder.append(".type-badge { padding: 2px 8px; background: #333; color: #888; border-radius: 3px; font-size: 11px; }\n");
        builder.append(".type-badge.string { background: #2d4436; color: #6abf76; }\n");
        builder.append(".type-badge.number { background: #3d3d2d; color: #d4c77f; }\n");
        builder.append(".type-badge.boolean { background: #3d2d36; color: #c67f9f; }\n");
        builder.append(".wildcards-list { list-style: none; padding: 0; }\n");
        builder.append(".wildcard-item { padding: 10px 12px; background: #252525; border-left: 2px solid #4a9eff; ");
        builder.append("margin-bottom: 6px; font-family: 'Courier New', monospace; font-size: 13px; color: #b0b0b0; }\n");
        builder.append("code, pre { font-family: 'Courier New', monospace; background: #252525; padding: 2px 6px; border-radius: 3px; font-size: 13px; color: #6abf76; }\n");
        builder.append("pre { padding: 16px; overflow: auto; border-left: 3px solid #4a9eff; display: block; }\n");
        builder.append("a { color: #4a9eff; text-decoration: none; }\n");
        builder.append("a:hover { text-decoration: underline; }\n");
        builder.append(".meta { color: #888; font-size: 0.85rem; }\n");
        builder.append("h1 { margin-top: 0; font-size: 1.8rem; }\n");
        builder.append("h2, h3 { margin-top: 24px; font-size: 1.2rem; }\n");
        builder.append(".example-block { margin-top: 16px; padding: 16px; background: #252525; border-radius: 4px; border: 1px solid #404040; }\n");
        builder.append(".example-title { font-weight: 600; margin-bottom: 8px; }\n");
        builder.append(".predicate-list li { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; align-items: center; padding: 4px 0; }\n");
        builder.append("::-webkit-scrollbar { width: 8px; height: 8px; }\n");
        builder.append("::-webkit-scrollbar-track { background: #1a1a1a; }\n");
        builder.append("::-webkit-scrollbar-thumb { background: #404040; border-radius: 4px; }\n");
        builder.append("::-webkit-scrollbar-thumb:hover { background: #4a4a4a; }\n");
        return builder.toString();
    }

    private String buildScripts() {
        StringBuilder builder = new StringBuilder();
        builder.append("(() => {\n");
        builder.append("  const eventsData = window.__eventsData || [];\n");
        builder.append("  const eventByKey = Object.fromEntries(eventsData.map(e => [e.event, e]));\n");
        builder.append("  const contentBody = document.getElementById('contentBody');\n");
        builder.append("  const headerTitle = document.getElementById('headerTitle');\n");
        builder.append("  const headerMeta = document.getElementById('headerMeta');\n");
        builder.append("  const breadcrumb = document.getElementById('breadcrumb');\n");
        builder.append("\n");
        builder.append("  function showPanel(panelId) {\n");
        builder.append("    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));\n");
        builder.append("    const p = document.getElementById(panelId + 'Panel');\n");
        builder.append("    if (p) p.classList.add('active');\n");
        builder.append("    document.querySelectorAll('.nav-link, .event-link').forEach(el => el.classList.remove('active'));\n");
        builder.append("  }\n");
        builder.append("\n");
        builder.append("  function showEvent(event) {\n");
        builder.append("    const sub = event.subcategory || getSubcategory(event);\n");
        builder.append("    const bread = ['<span class=\"breadcrumb-item\">' + (event.parent_base_key ? 'Sub-events' : 'Events') + '</span>'];\n");
        builder.append("    bread.push('<span class=\"breadcrumb-separator\">/</span>');\n");
        builder.append("    bread.push('<span class=\"breadcrumb-item\">' + event.category + '</span>');\n");
        builder.append("    if (sub) { bread.push('<span class=\"breadcrumb-separator\">/</span>'); bread.push('<span class=\"breadcrumb-item\">' + sub + '</span>'); }\n");
        builder.append("    bread.push('<span class=\"breadcrumb-separator\">/</span>');\n");
        builder.append("    bread.push('<span class=\"breadcrumb-item active\">' + event.event + '</span>');\n");
        builder.append("    breadcrumb.innerHTML = bread.join('');\n");
        builder.append("    headerTitle.textContent = event.event;\n");
        builder.append("    headerMeta.textContent = event.description || '';\n");
        builder.append("    let predicatesRows = '';\n");
        builder.append("    if (event.predicates) {\n");
        builder.append("      for (const [k, v] of Object.entries(event.predicates)) {\n");
        builder.append("        predicatesRows += '<tr><td>' + k + '</td><td><span class=\"type-badge ' + (v || 'string') + '\">' + (v || 'string') + '</span></td></tr>';\n");
        builder.append("      }\n");
        builder.append("    }\n");
        builder.append("    const wildcardsHtml = (event.wildcards || []).map(w => '<li class=\"wildcard-item\">' + w + '</li>').join('');\n");
        builder.append("    const badges = '<span class=\"event-category-badge\">' + event.category + '</span>' + (sub ? '<span class=\"event-subcategory-badge\">' + sub + '</span>' : '');\n");
        builder.append("    let linksHtml = '';\n");
        builder.append("    if (event.parent_base_key) {\n");
        builder.append("      const parent = eventByKey[event.parent_base_key];\n");
        builder.append("      linksHtml += '<div class=\"event-relation-links\"><span class=\"meta\">Inherits from: </span><a href=\"#\" class=\"event-relation-link\" data-event-key=\"' + event.parent_base_key + '\">' + event.parent_base_key + '</a></div>';\n");
        builder.append("    } else {\n");
        builder.append("      const subCount = eventsData.filter(e => e.parent_base_key === event.event).length;\n");
        builder.append("      if (subCount > 0) linksHtml += '<div class=\"event-relation-links\"><a href=\"#\" class=\"event-relation-link\" data-filter-parent=\"' + event.event + '\">View sub-events (' + subCount + ')</a></div>';\n");
        builder.append("    }\n");
        builder.append("    const html = '<div class=\"event-detail\"><div class=\"event-detail-header\"><div class=\"event-name\">' + event.event + '</div><div class=\"event-badges\">' + badges + '</div>' + linksHtml + '<div class=\"event-description\">' + (event.description || '') + '</div></div>' + (predicatesRows ? '<div class=\"section\"><h2 class=\"section-heading\">Predicates</h2><table class=\"predicates-table\"><thead><tr><th>Predicate Key</th><th>Type</th></tr></thead><tbody>' + predicatesRows + '</tbody></table></div>' : '') + (wildcardsHtml ? '<div class=\"section\"><h2 class=\"section-heading\">Wildcards</h2><ul class=\"wildcards-list\">' + wildcardsHtml + '</ul></div>' : '') + '</div>';\n");
        builder.append("    const detailPanel = document.getElementById('eventDetailPanel');\n");
        builder.append("    if (detailPanel) { detailPanel.innerHTML = html; detailPanel.classList.add('active'); }\n");
        builder.append("    showPanel('eventDetail');\n");
        builder.append("    document.querySelectorAll('.event-link').forEach(link => { link.classList.toggle('active', link.getAttribute('data-event-key') === event.event); });\n");
        builder.append("  }\n");
        builder.append("  function getSubcategory(e) { const p = (e.event || '').split('.'); return p.length >= 2 ? p[1] : null; }\n");
        builder.append("  function showSubEventsForParent(parentKey) {\n");
        builder.append("    document.querySelector('.events-tab[data-tab=\"sub-events\"]').click();\n");
        builder.append("    const filterEl = document.getElementById('subEventsParentFilter');\n");
        builder.append("    if (filterEl) { filterEl.value = parentKey; applySubEventsFilter(); }\n");
        builder.append("    const searchEl = document.querySelector('.sub-events-search');\n");
        builder.append("    if (searchEl) { searchEl.value = ''; }\n");
        builder.append("    const subNav = document.getElementById('sub-eventsNav');\n");
        builder.append("    if (subNav) { subNav.querySelectorAll('.category-group, .subcategory-group').forEach(g => { if (Array.from(g.querySelectorAll('.event-link')).some(l => l.style.display !== 'none')) g.classList.add('expanded'); }); }\n");
        builder.append("  }\n");
        builder.append("\n");
        builder.append("  document.body.addEventListener('click', e => {\n");
        builder.append("    const link = e.target.closest('.event-relation-link');\n");
        builder.append("    if (!link) return;\n");
        builder.append("    e.preventDefault();\n");
        builder.append("    if (link.hasAttribute('data-clear-parent-filter')) {\n");
        builder.append("      const f = document.getElementById('subEventsParentFilter'); if (f) { f.value = ''; applySubEventsFilter(); }\n");
        builder.append("      return;\n");
        builder.append("    }\n");
        builder.append("    const key = link.getAttribute('data-event-key');\n");
        builder.append("    const filterParent = link.getAttribute('data-filter-parent');\n");
        builder.append("    if (key) { const ev = eventByKey[key]; if (ev) showEvent(ev); }\n");
        builder.append("    else if (filterParent) showSubEventsForParent(filterParent);\n");
        builder.append("  });\n");
        builder.append("\n");
        builder.append("  document.querySelectorAll('.nav-link').forEach(link => {\n");
        builder.append("    link.addEventListener('click', () => {\n");
        builder.append("      const section = link.getAttribute('data-section');\n");
        builder.append("      if (!section) return;\n");
        builder.append("      const titles = { 'welcome': 'All the Webhooks', 'quick-start': 'Quick start', 'event-keys': 'Event key hierarchy', 'message-structure': 'Message structure', 'conditions': 'Conditions', 'webhook-display-name': 'Webhook display name', 'regex': 'Regex' };\n");
        builder.append("      breadcrumb.innerHTML = '<span class=\"breadcrumb-item active\">' + (titles[section] || section) + '</span>';\n");
        builder.append("      headerTitle.textContent = titles[section] || section;\n");
        builder.append("      headerMeta.textContent = '';\n");
        builder.append("      showPanel(section);\n");
        builder.append("      link.classList.add('active');\n");
        builder.append("    });\n");
        builder.append("  });\n");
        builder.append("\n");
        builder.append("  document.querySelectorAll('.event-link').forEach(link => {\n");
        builder.append("    link.addEventListener('click', () => {\n");
        builder.append("      const key = link.getAttribute('data-event-key');\n");
        builder.append("      const event = eventByKey[key] || eventsData.find(e => e.event === key);\n");
        builder.append("      if (event) showEvent(event);\n");
        builder.append("    });\n");
        builder.append("  });\n");
        builder.append("\n");
        builder.append("  function applySubEventsFilter() {\n");
        builder.append("    const term = (document.querySelector('.sub-events-search')?.value || '').trim().toLowerCase();\n");
        builder.append("    const parentFilter = (document.getElementById('subEventsParentFilter')?.value || '').trim();\n");
        builder.append("    const ind = document.getElementById('subEventsFilterIndicator');\n");
        builder.append("    if (ind) ind.innerHTML = parentFilter ? '<span class=\"meta\">Filtering by parent: <code>' + parentFilter + '</code> <a href=\"#\" class=\"event-relation-link\" data-clear-parent-filter>clear</a></span>' : '';\n");
        builder.append("    const navEl = document.getElementById('sub-eventsNav');\n");
        builder.append("    if (!navEl) return;\n");
        builder.append("    navEl.querySelectorAll('.event-link').forEach(link => {\n");
        builder.append("      const key = link.getAttribute('data-event-key');\n");
        builder.append("      const ev = eventByKey[key];\n");
        builder.append("      const searchMatch = !term || (key + ' ' + (ev ? ev.description || '' : '')).toLowerCase().includes(term);\n");
        builder.append("      const parentMatch = !parentFilter || (ev && ev.parent_base_key === parentFilter);\n");
        builder.append("      link.style.display = searchMatch && parentMatch ? '' : 'none';\n");
        builder.append("    });\n");
        builder.append("    navEl.querySelectorAll('.subcategory-group').forEach(grp => {\n");
        builder.append("      const visible = Array.from(grp.querySelectorAll('.event-link')).some(l => l.style.display !== 'none');\n");
        builder.append("      grp.style.display = visible ? '' : 'none';\n");
        builder.append("    });\n");
        builder.append("    navEl.querySelectorAll('.category-group').forEach(grp => {\n");
        builder.append("      const visible = Array.from(grp.querySelectorAll('.event-link')).some(l => l.style.display !== 'none');\n");
        builder.append("      grp.style.display = visible ? '' : 'none';\n");
        builder.append("    });\n");
        builder.append("  }\n");
        builder.append("  ['events', 'sub-events'].forEach(sectionId => {\n");
        builder.append("    const searchEl = document.querySelector('.' + sectionId + '-search');\n");
        builder.append("    const navEl = document.getElementById(sectionId + 'Nav');\n");
        builder.append("    const expandBtn = document.querySelector('.' + sectionId + '-expand');\n");
        builder.append("    const collapseBtn = document.querySelector('.' + sectionId + '-collapse');\n");
        builder.append("    if (searchEl && navEl) {\n");
        builder.append("      searchEl.addEventListener('input', e => {\n");
        builder.append("        const term = e.target.value.trim().toLowerCase();\n");
        builder.append("        if (sectionId === 'sub-events') { applySubEventsFilter(); return; }\n");
        builder.append("        navEl.querySelectorAll('.event-link').forEach(link => {\n");
        builder.append("          const key = link.getAttribute('data-event-key');\n");
        builder.append("          const ev = eventByKey[key];\n");
        builder.append("          const hay = (key + ' ' + (ev ? ev.description || '' : '')).toLowerCase();\n");
        builder.append("          const match = !term || hay.includes(term);\n");
        builder.append("          link.style.display = match ? '' : 'none';\n");
        builder.append("        });\n");
        builder.append("        navEl.querySelectorAll('.subcategory-group').forEach(grp => {\n");
        builder.append("          const visible = grp.querySelectorAll('.event-link').length > 0 && Array.from(grp.querySelectorAll('.event-link')).some(l => l.style.display !== 'none');\n");
        builder.append("          grp.style.display = visible ? '' : 'none';\n");
        builder.append("        });\n");
        builder.append("        navEl.querySelectorAll('.category-group').forEach(grp => {\n");
        builder.append("          const visible = grp.querySelectorAll('.event-link').length > 0 && Array.from(grp.querySelectorAll('.event-link')).some(l => l.style.display !== 'none');\n");
        builder.append("          grp.style.display = visible ? '' : 'none';\n");
        builder.append("        });\n");
        builder.append("      });\n");
        builder.append("    }\n");
        builder.append("    if (expandBtn && navEl) expandBtn.addEventListener('click', () => navEl.querySelectorAll('.category-group, .subcategory-group').forEach(el => el.classList.add('expanded')));\n");
        builder.append("    if (collapseBtn && navEl) collapseBtn.addEventListener('click', () => navEl.querySelectorAll('.category-group, .subcategory-group').forEach(el => el.classList.remove('expanded')));\n");
        builder.append("  });\n");
        builder.append("\n");
        builder.append("  document.querySelectorAll('.category-header, .subcategory-header').forEach(header => {\n");
        builder.append("    header.addEventListener('click', () => header.parentElement.classList.toggle('expanded'));\n");
        builder.append("  });\n");
        builder.append("\n");
        builder.append("  document.querySelectorAll('.events-tab').forEach(tab => {\n");
        builder.append("    tab.addEventListener('click', () => {\n");
        builder.append("      const id = tab.getAttribute('data-tab');\n");
        builder.append("      document.querySelectorAll('.events-tab').forEach(t => t.classList.remove('active'));\n");
        builder.append("      document.querySelectorAll('.events-tab-panel').forEach(p => p.classList.remove('active'));\n");
        builder.append("      tab.classList.add('active');\n");
        builder.append("      const panel = document.getElementById(id + 'TabPanel');\n");
        builder.append("      if (panel) panel.classList.add('active');\n");
        builder.append("      if (id === 'sub-events') { applySubEventsFilter(); document.getElementById('sub-eventsNav')?.querySelectorAll('.category-group, .subcategory-group').forEach(el => el.classList.add('expanded')); }\n");
        builder.append("    });\n");
        builder.append("  });\n");
        builder.append("\n");
        builder.append("  const sidebar = document.getElementById('sidebar');\n");
        builder.append("  const handle = document.getElementById('sidebarResizeHandle');\n");
        builder.append("  if (sidebar && handle) {\n");
        builder.append("    let dragging = false;\n");
        builder.append("    handle.addEventListener('mousedown', e => { e.preventDefault(); dragging = true; document.body.style.cursor = 'col-resize'; document.body.style.userSelect = 'none'; });\n");
        builder.append("    const stopDrag = () => { if (dragging) { dragging = false; document.body.style.cursor = ''; document.body.style.userSelect = ''; } };\n");
        builder.append("    document.addEventListener('mouseup', stopDrag);\n");
        builder.append("    document.addEventListener('mouseleave', stopDrag);\n");
        builder.append("    document.addEventListener('mousemove', e => {\n");
        builder.append("      if (!dragging) return;\n");
        builder.append("      const w = Math.max(200, Math.min(500, e.clientX));\n");
        builder.append("      sidebar.style.width = w + 'px';\n");
        builder.append("    });\n");
        builder.append("  }\n");
        builder.append("\n");
        builder.append("  showPanel('welcome');\n");
        builder.append("  document.querySelector('.nav-link[data-section=\"welcome\"]').classList.add('active');\n");
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
            String subcat = getSubcategory(event);
            if (subcat != null) {
                builder.append("\"subcategory\":\"").append(JsonEscaper.escape(subcat)).append("\",");
            }
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

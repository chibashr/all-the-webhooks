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
        writeFile(new File(docsDir, "README.html"), buildReadmeHtml());
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
            builder.append("<a class=\"nav-anchor nav-link\" href=\"#message-structure\">Message structure</a>\n");
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
        builder.append(buildMessageStructureSection());
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

    private String buildMessageStructureSection() {
        StringBuilder builder = new StringBuilder();
        builder.append("<section id=\"message-structure\" class=\"doc-section\" data-anchor=\"message-structure\">\n");
        builder.append("<h1>Message structure</h1>\n");
        builder.append("<p>In <code>events.yaml</code>, each event or rule sets <code>message: &lt;key&gt;</code>, ");
        builder.append("for example <code>message: player_damaged</code>.</p>\n");
        builder.append("<p>That key is looked up under <code>messages</code> in <code>messages.yaml</code>, ");
        builder.append("for example <code>messages.player_damaged.content</code>.</p>\n");
        builder.append("<p>The <code>content</code> value is a template string. Placeholders use ");
        builder.append("<code>{placeholder}</code> syntax, such as <code>{player.name}</code> or ");
        builder.append("<code>{damage.amount}</code>.</p>\n");
        builder.append("<p>Available placeholders correspond to the same predicate fields listed for each event ");
        builder.append("in the Events section. If a placeholder has no value, it is replaced with an empty string.</p>\n");
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
        builder.append("<p class=\"meta\">See <a class=\"inline-link\" href=\"#events\">Events</a> ");
        builder.append("for the full list of supported predicates per event.</p>\n");
        builder.append("</div>\n");
        builder.append("</section>\n");
        return builder.toString();
    }

    private String buildEventsContent(List<EventDefinition> events, boolean includeSectionWrapper) {
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
        for (EventDefinition event : events) {
            builder.append(buildEventEntry(event));
        }
        if (includeSectionWrapper) {
            builder.append("</section>\n");
        }
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
        Node root = new Node("");
        for (EventDefinition event : events) {
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

    private String buildEventEntry(EventDefinition event) {
        StringBuilder builder = new StringBuilder();
        builder.append("<div class=\"event-entry\" id=\"").append(HtmlEscaper.escape(event.getKey())).append("\"");
        builder.append(" data-anchor=\"").append(HtmlEscaper.escape(event.getKey())).append("\"");
        builder.append(" data-search=\"").append(HtmlEscaper.escape(buildSearchIndex(event))).append("\">");
        builder.append("<h2>").append(HtmlEscaper.escape(event.getKey())).append("</h2>");
        builder.append("<div class=\"meta\">Category: ").append(HtmlEscaper.escape(event.getCategory())).append("</div>");
        builder.append("<p>").append(HtmlEscaper.escape(event.getDescription())).append("</p>");
        builder.append("<div class=\"event-section\">");
        builder.append("<h3>Supported predicates</h3>");
        builder.append("<ul>");
        for (Map.Entry<String, String> entry : event.getPredicateFields().entrySet()) {
            builder.append("<li><code>").append(HtmlEscaper.escape(entry.getKey()))
                    .append("</code> <span class=\"meta\">")
                    .append(HtmlEscaper.escape(entry.getValue())).append("</span></li>");
        }
        builder.append("</ul>");
        builder.append("</div>");
        builder.append("<div class=\"event-section\">");
        builder.append("<h3>Wildcard support</h3>");
        for (String example : event.getWildcardExamples()) {
            builder.append("<div><code>").append(HtmlEscaper.escape(example)).append("</code></div>");
        }
        builder.append("</div>");
        builder.append("<div class=\"event-section\">");
        builder.append("<h3>events.yaml example</h3>");
        builder.append("<pre>").append(HtmlEscaper.escape(event.getExampleYaml())).append("</pre>");
        builder.append("</div>");
        builder.append("</div>");
        return builder.toString();
    }

    private String buildSearchIndex(EventDefinition event) {
        StringBuilder builder = new StringBuilder();
        builder.append(event.getKey()).append(" ").append(event.getCategory());
        for (String field : event.getPredicateFields().keySet()) {
            builder.append(" ").append(field);
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

    private String buildReadmeHtml() {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">");
        builder.append("<title>All the Webhooks - README</title>");
        builder.append("<style>body{font-family:Arial, sans-serif;padding:24px;background:#141414;color:#e8e8e8;}");
        builder.append("code{background:#222;padding:2px 4px;border-radius:4px;}</style>");
        builder.append("</head><body>");
        builder.append("<h1>All the Webhooks</h1>");
        builder.append("<p>Configure events in <code>events.yaml</code>, messages in <code>messages.yaml</code>, ");
        builder.append("and webhooks in <code>config.yaml</code>.</p>");
        builder.append("<p>Generate docs with <code>/allthewebhooks docs generate</code>.</p>");
        builder.append("<p>Open <a href=\"docs.html\">docs.html</a> for the full documentation.</p>");
        builder.append("</body></html>");
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

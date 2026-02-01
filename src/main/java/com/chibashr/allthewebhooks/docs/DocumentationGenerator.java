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

        writeFile(new File(docsDir, "events.html"), buildEventsHtml(events));
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

    private String buildEventsHtml(List<EventDefinition> events) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">");
        builder.append("<title>All the Webhooks - Events</title>");
        builder.append("<style>");
        builder.append("body{font-family:Arial, sans-serif;margin:0;color:#e8e8e8;background:#141414;}");
        builder.append(".layout{display:flex;height:100vh;}");
        builder.append(".sidebar{width:300px;overflow:auto;background:#1f1f1f;padding:16px;}");
        builder.append(".content{flex:1;overflow:auto;padding:24px;}");
        builder.append("h1,h2,h3{color:#f0f0f0;}");
        builder.append("a{color:#7fbfff;text-decoration:none;}");
        builder.append("details{margin-bottom:4px;}");
        builder.append(".event-entry{margin-bottom:32px;padding-bottom:16px;border-bottom:1px solid #333;}");
        builder.append(".meta{color:#b0b0b0;font-size:12px;}");
        builder.append(".search{width:100%;padding:8px;margin:12px 0;background:#2a2a2a;border:1px solid #444;color:#fff;}");
        builder.append("code, pre{background:#222;padding:8px;border-radius:4px;color:#dcdcdc;}");
        builder.append("</style></head><body>");
        builder.append("<div class=\"layout\">");
        builder.append("<div class=\"sidebar\">");
        builder.append("<h2>All the Webhooks</h2>");
        builder.append("<div class=\"meta\">Version: ").append(HtmlEscaper.escape(plugin.getDescription().getVersion()))
                .append("</div>");
        builder.append("<div class=\"meta\">Server: ").append(HtmlEscaper.escape(plugin.getServer().getVersion()))
                .append("</div>");
        builder.append("<div class=\"meta\">Generated: ").append(Instant.now().toString()).append("</div>");
        builder.append("<input class=\"search\" id=\"search\" placeholder=\"Search events or predicates\" />");
        builder.append(buildNavigation(events));
        builder.append("</div>");
        builder.append("<div class=\"content\">");
        builder.append("<h1>Event Reference</h1>");
        builder.append("<p class=\"meta\">Events are resolved dynamically. Unresolvable keys are warned and ignored. ");
        builder.append("Wildcard precedence favors the most specific match, then less specific keys.</p>");
        for (EventDefinition event : events) {
            builder.append(buildEventEntry(event));
        }
        builder.append("</div></div>");
        builder.append("<script>");
        builder.append("const input=document.getElementById('search');");
        builder.append("input.addEventListener('input',()=>{");
        builder.append("const term=input.value.toLowerCase();");
        builder.append("document.querySelectorAll('.event-entry').forEach(entry=>{");
        builder.append("const hay=entry.getAttribute('data-search');");
        builder.append("entry.style.display=hay.includes(term)?'block':'none';");
        builder.append("});");
        builder.append("document.querySelectorAll('.nav-item').forEach(item=>{");
        builder.append("const hay=item.getAttribute('data-search');");
        builder.append("item.style.display=hay.includes(term)?'block':'none';");
        builder.append("});");
        builder.append("});");
        builder.append("</script>");
        builder.append("</body></html>");
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
        builder.append("<div class=\"nav\">");
        builder.append(buildNode(root));
        builder.append("</div>");
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
                    .append("\"><a href=\"#")
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
        builder.append(" data-search=\"").append(HtmlEscaper.escape(buildSearchIndex(event))).append("\">");
        builder.append("<h2>").append(HtmlEscaper.escape(event.getKey())).append("</h2>");
        builder.append("<div class=\"meta\">Category: ").append(HtmlEscaper.escape(event.getCategory())).append("</div>");
        builder.append("<p>").append(HtmlEscaper.escape(event.getDescription())).append("</p>");
        builder.append("<h3>Supported predicates</h3><ul>");
        for (Map.Entry<String, String> entry : event.getPredicateFields().entrySet()) {
            builder.append("<li><code>").append(HtmlEscaper.escape(entry.getKey()))
                    .append("</code> <span class=\"meta\">")
                    .append(HtmlEscaper.escape(entry.getValue())).append("</span></li>");
        }
        builder.append("</ul>");
        builder.append("<h3>Wildcard support</h3>");
        for (String example : event.getWildcardExamples()) {
            builder.append("<div><code>").append(HtmlEscaper.escape(example)).append("</code></div>");
        }
        builder.append("<h3>events.yaml example</h3>");
        builder.append("<pre>").append(HtmlEscaper.escape(event.getExampleYaml())).append("</pre>");
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

package com.chibashr.allthewebhooks.docs;

import com.chibashr.allthewebhooks.events.EventRegistry;
import com.chibashr.allthewebhooks.events.SubEventDiscovery;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.logging.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentationGenerator}, including docs HTML generation.
 * Server-like test writes to {@value #SERVER_LIKE_DOCS_HTML_PATH} (relative to project root).
 */
@ExtendWith(MockitoExtension.class)
class DocumentationGeneratorTest {

    /** Relative path from project root to the generated docs.html (server-like test). */
    public static final String SERVER_LIKE_DOCS_HTML_PATH = "build/test-docs/docs/docs.html";

    @TempDir
    Path tempDir;

    @Mock
    private JavaPlugin plugin;

    @Mock
    private PluginDescriptionFile description;

    @Mock
    private org.bukkit.Server server;

    private Path dataFolder;

    @BeforeEach
    void setUp() throws IOException {
        dataFolder = tempDir.resolve("plugin-data");
        Files.createDirectories(dataFolder);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        when(plugin.getDescription()).thenReturn(description);
        when(description.getVersion()).thenReturn("0.1.0-test");
        when(plugin.getServer()).thenReturn(server);
        lenient().when(plugin.getLogger()).thenReturn(Logger.getLogger("DocumentationGeneratorTest"));
        when(server.getVersion()).thenReturn("Paper 1.20.6");
    }

    @Test
    void generate_createsDocsDirectory() {
        EventRegistry registry = EventRegistry.createDefault();
        DocumentationGenerator generator = new DocumentationGenerator(plugin, registry);
        generator.generate();
        assertTrue(Files.isDirectory(dataFolder.resolve("docs")));
    }

    @Test
    void generate_producesDocsHtml() throws IOException {
        EventRegistry registry = EventRegistry.createDefault();
        SubEventDiscovery.discover(server, registry, plugin.getLogger());
        DocumentationGenerator generator = new DocumentationGenerator(plugin, registry);
        generator.generate();
        Path docsHtml = dataFolder.resolve("docs").resolve("docs.html");
        assertTrue(Files.isRegularFile(docsHtml));
        String content = Files.readString(docsHtml);
        assertTrue(content.contains("<!DOCTYPE html>"));
        assertTrue(content.contains("</html>"), "Should contain HTML structure");
        assertTrue(content.contains("All the Webhooks"));
        assertTrue(content.contains("message-structure") || content.contains("events.yaml"));
        assertTrue(content.contains("Overview") || content.contains("overview"));
        assertTrue(content.contains("Events") || content.contains("events"));
    }

    @Test
    void generate_producesEventsJson() throws IOException {
        EventRegistry registry = EventRegistry.createDefault();
        DocumentationGenerator generator = new DocumentationGenerator(plugin, registry);
        generator.generate();
        Path eventsJson = dataFolder.resolve("docs").resolve("events.json");
        assertTrue(Files.isRegularFile(eventsJson));
        String content = Files.readString(eventsJson);
        assertTrue(content.startsWith("["));
        assertTrue(content.contains("\"event\""));
        assertTrue(content.contains("player.join"));
    }

    @Test
    void generate_withDefaultRegistry_includesEventKeysInDocsHtml() throws IOException {
        EventRegistry registry = EventRegistry.createDefault();
        DocumentationGenerator generator = new DocumentationGenerator(plugin, registry);
        generator.generate();
        Path docsHtml = dataFolder.resolve("docs").resolve("docs.html");
        String content = Files.readString(docsHtml);
        assertTrue(content.contains("player.join"));
        assertTrue(content.contains("player.quit"));
    }

    @Test
    void generate_docsHtmlHasValidStructure() throws IOException {
        EventRegistry registry = EventRegistry.createDefault();
        DocumentationGenerator generator = new DocumentationGenerator(plugin, registry);
        generator.generate();
        Path docsHtml = dataFolder.resolve("docs").resolve("docs.html");
        String content = Files.readString(docsHtml);
        assertTrue(content.contains("</head>"));
        assertTrue(content.contains("<body>"));
        assertTrue(content.contains("</html>"));
    }

    @Test
    void generate_eventsJsonEscapesSpecialChars() throws IOException {
        EventRegistry registry = EventRegistry.createDefault();
        DocumentationGenerator generator = new DocumentationGenerator(plugin, registry);
        generator.generate();
        Path eventsJson = dataFolder.resolve("docs").resolve("events.json");
        String content = Files.readString(eventsJson);
        assertTrue(content.contains("\\\"") || !content.contains("\"\n\""));
    }

    /**
     * Generates docs.html (and related docs) under the project build directory in a way that
     * mirrors a real server run: full default event registry, plugin data folder layout, and
     * realistic version strings. The output persists so it can be opened in a browser.
     * <p>
     * Output location (relative to project root): {@value #SERVER_LIKE_DOCS_HTML_PATH}
     */
    @Test
    void generate_serverLike_outputsDocsHtmlUnderBuild() throws IOException {
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        Path pluginDataFolder = projectRoot.resolve("build").resolve("test-docs");
        Files.createDirectories(pluginDataFolder);

        when(plugin.getDataFolder()).thenReturn(pluginDataFolder.toFile());
        when(plugin.getDescription()).thenReturn(description);
        when(description.getVersion()).thenReturn("0.1.11");
        when(plugin.getServer()).thenReturn(server);
        when(server.getVersion()).thenReturn("Paper 1.20.6");

        EventRegistry registry = EventRegistry.createDefault();
        SubEventDiscovery.discover(server, registry, plugin.getLogger());
        DocumentationGenerator generator = new DocumentationGenerator(plugin, registry);
        generator.generate();

        Path docsHtml = pluginDataFolder.resolve("docs").resolve("docs.html");
        assertTrue(Files.isRegularFile(docsHtml), "docs.html should exist at " + docsHtml);
        String content = Files.readString(docsHtml);
        assertTrue(content.contains("<!DOCTYPE html>"));
        assertTrue(content.contains("All the Webhooks"));
        assertTrue(content.contains("0.1.11"));
        assertTrue(content.contains("Paper 1.20.6"));
        assertTrue(content.contains("player.join"));
        assertTrue(content.contains("entity.damage.player"));
        assertTrue(content.contains("inventory.open"));
        assertTrue(content.contains("world.time.change"));
    }

    @Test
    void generate_includesSubEventsWhenDiscoveryRuns() throws IOException {
        EventRegistry registry = EventRegistry.createDefault();
        SubEventDiscovery.discover(server, registry, plugin.getLogger());
        DocumentationGenerator generator = new DocumentationGenerator(plugin, registry);
        generator.generate();
        Path eventsJson = dataFolder.resolve("docs").resolve("events.json");
        String json = Files.readString(eventsJson);
        assertTrue(json.contains("parent_base_key"), "events.json should include parent_base_key for sub-events");
        Path docsHtml = dataFolder.resolve("docs").resolve("docs.html");
        assertTrue(Files.readString(docsHtml).contains("Sub-events"), "docs.html should have Sub-events tab");
    }

    @Test
    void generate_includesRegexSection() throws IOException {
        EventRegistry registry = EventRegistry.createDefault();
        DocumentationGenerator generator = new DocumentationGenerator(plugin, registry);
        generator.generate();
        Path docsHtml = dataFolder.resolve("docs").resolve("docs.html");
        String content = Files.readString(docsHtml);
        assertTrue(content.contains("id=\"regex\"") || content.contains("data-anchor=\"regex\""), "docs.html should have regex section");
        assertTrue(content.contains("<h1>Regex</h1>"));
        assertTrue(content.contains("regex:pattern:replacement"));
        assertTrue(content.contains("hardcore-26"), "Regex example should show hardcore-26");
    }
}

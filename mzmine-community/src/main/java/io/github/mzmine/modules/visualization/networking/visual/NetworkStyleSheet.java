/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.networking.visual;

import com.google.common.io.Resources;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal GraphStream-stylesheet reader for the network legend. Drives {@link NetworkLegend} so its
 * swatches stay in sync with {@code themes/graph_network_style.css} — change a color in the CSS and
 * the legend tracks it on the next reload.
 *
 * <p>Not a real CSS engine: only the rules and properties the legend needs are surfaced
 * (fill-color, shape, stroke-mode). Selectors must be of the form {@code node.X} or {@code edge.X}
 * or the bare {@code node}/{@code edge} default rule. Comments are stripped before parsing because
 * the IDE inserts a license header that GraphStream's own parser also rejects.
 */
final class NetworkStyleSheet {

  private static final Logger LOG = Logger.getLogger(NetworkStyleSheet.class.getName());

  // <selector> { ... } — DOTALL so the body can span multiple lines
  private static final Pattern RULE = Pattern.compile("([a-zA-Z][a-zA-Z0-9_.:]*)\\s*\\{([^}]*)}",
      Pattern.DOTALL);
  // Each declaration is "<property>: <value>;"
  private static final Pattern DECLARATION = Pattern.compile("([a-zA-Z\\-]+)\\s*:\\s*([^;]+);");
  // rgb(...) wraps commas we must NOT split on when reading multi-color fills
  private static final Pattern RGB_FUNC = Pattern.compile("rgb\\(([^)]*)\\)");

  private final String source;

  private NetworkStyleSheet(@NotNull final String source) {
    this.source = source;
  }

  /**
   * Load the default network stylesheet bundled with the application.
   *
   * @return a parser over the loaded source, or an empty one if loading fails (legend will show
   * fallback colors)
   */
  static @NotNull NetworkStyleSheet loadDefault() {
    try {
      String css = Resources.toString(Resources.getResource(NetworkPane.DEFAULT_STYLE_FILE),
          StandardCharsets.UTF_8);
      // strip block comments (license header & inline notes); GraphStream's own parser also can't
      // handle them, so this matches NetworkPane.loadDefaultStyle() behaviour
      css = css.replaceAll("(?s)/\\*.*?\\*/", "");
      return new NetworkStyleSheet(css);
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Could not load network legend stylesheet: " + e.getMessage(), e);
      return new NetworkStyleSheet("");
    }
  }

  /**
   * Parsed list of fill colors for a given CSS class on the {@code node} element type. Returns the
   * default {@code node { fill-color: ... }} colors when the class isn't found, or an empty list
   * when neither rule is present.
   */
  @NotNull List<Color> getNodeFillColors(@NotNull final String cssClass) {
    return getFillColors("node." + cssClass, "node");
  }

  /**
   * Same as {@link #getNodeFillColors(String)} for {@code edge.X} selectors, falling back to the
   * bare {@code edge} rule.
   */
  @NotNull List<Color> getEdgeFillColors(@NotNull final String cssClass) {
    return getFillColors("edge." + cssClass, "edge");
  }

  /**
   * GraphStream's {@code stroke-mode} value for an edge class — e.g. "dashes", "dots", "plain",
   * "none". Used to render the legend line dashed/dotted to match the actual edges. Falls back to
   * the bare {@code edge} rule.
   */
  @Nullable String getEdgeStrokeMode(@NotNull final String cssClass) {
    String v = readDeclaration("edge." + cssClass, "stroke-mode");
    if (v == null) {
      v = readDeclaration("edge", "stroke-mode");
    }
    return v;
  }

  /**
   * GraphStream's {@code shape} value for a node class — e.g. "circle", "diamond". Falls back to
   * the bare {@code node} rule.
   */
  @Nullable String getNodeShape(@NotNull final String cssClass) {
    String v = readDeclaration("node." + cssClass, "shape");
    if (v == null) {
      v = readDeclaration("node", "shape");
    }
    return v;
  }

  private @NotNull List<Color> getFillColors(@NotNull final String selector,
      @NotNull final String fallbackSelector) {
    String raw = readDeclaration(selector, "fill-color");
    if (raw == null) {
      raw = readDeclaration(fallbackSelector, "fill-color");
    }
    if (raw == null) {
      return Collections.emptyList();
    }
    return parseColorList(raw);
  }

  // Scan rules and pick the FIRST declaration of the requested property in the FIRST matching
  // selector. Real CSS would honor cascade order; here the file is small and the selectors are
  // unique per class, so first-match is fine.
  private @Nullable String readDeclaration(@NotNull final String selector,
      @NotNull final String property) {
    final Matcher rules = RULE.matcher(source);
    while (rules.find()) {
      if (!rules.group(1).equals(selector)) {
        continue;
      }
      final Matcher decls = DECLARATION.matcher(rules.group(2));
      while (decls.find()) {
        if (decls.group(1).equalsIgnoreCase(property)) {
          return decls.group(2).trim();
        }
      }
    }
    return null;
  }

  /**
   * Split a comma-separated color list while keeping {@code rgb(a, b, c)} groups intact. Each piece
   * is parsed via {@link #parseSingleColor(String)}.
   */
  static @NotNull List<Color> parseColorList(@NotNull final String raw) {
    // mask rgb(...) commas so a top-level split-on-comma doesn't tear them apart
    final StringBuilder masked = new StringBuilder();
    final Matcher m = RGB_FUNC.matcher(raw);
    int last = 0;
    final List<String> rgbBodies = new ArrayList<>();
    while (m.find()) {
      masked.append(raw, last, m.start()).append("rgb(__").append(rgbBodies.size()).append("__)");
      rgbBodies.add(m.group(1));
      last = m.end();
    }
    masked.append(raw.substring(last));

    final List<Color> result = new ArrayList<>();
    for (final String piece : masked.toString().split(",")) {
      final String trimmed = piece.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      // un-mask any rgb(...) placeholder before parsing
      final Matcher placeholder = Pattern.compile("rgb\\(__(\\d+)__\\)").matcher(trimmed);
      final String resolved;
      if (placeholder.matches()) {
        resolved = "rgb(" + rgbBodies.get(Integer.parseInt(placeholder.group(1))) + ")";
      } else {
        resolved = trimmed;
      }
      final Color color = parseSingleColor(resolved);
      if (color != null) {
        result.add(color);
      }
    }
    return result;
  }

  // Accepts: #RGB / #RRGGBB hex, rgb(r, g, b), or a JavaFX named color ("yellow", "salmon", ...)
  static @Nullable Color parseSingleColor(@NotNull final String spec) {
    final String s = spec.trim();
    try {
      if (s.startsWith("rgb(") && s.endsWith(")")) {
        final String inner = s.substring(4, s.length() - 1);
        final String[] parts = inner.split(",");
        if (parts.length == 3) {
          return Color.rgb(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
              Integer.parseInt(parts[2].trim()));
        }
      }
      // Color.web also accepts "rgb(...)" and named colors, so this is the broad fallback path
      return Color.web(s);
    } catch (Exception e) {
      LOG.fine("Could not parse network legend color '" + spec + "': " + e.getMessage());
      return null;
    }
  }
}

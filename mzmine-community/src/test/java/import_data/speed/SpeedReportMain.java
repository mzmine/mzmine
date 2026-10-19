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

package import_data.speed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reads the csv or jsonlines written by {@link BatchSpeedTestMain} and creates a standalone html page
 * that compares the runs. Each batch step ({@code name} column) becomes one panel and each panel
 * contains one box per series, where the series is the {@code description} the user provided to
 * qualify what was tested. Repeated iterations provide the replicates of a box.
 * <p>
 * On top of the box plots the page shows a comparison table with the median of every step relative to
 * a selectable baseline series - usually the fastest way to see where a branch got faster or slower.
 * {@link SpeedTestPhase#WARMUP} iterations are read but excluded unless the checkbox is enabled, and
 * rows that did not finish are always dropped.
 * <p>
 * Usage: {@code SpeedReportMain [speed.jsonlines|speed.csv] [report.html]}, both arguments are
 * optional. The jsonlines file is preferred because it is self describing.
 */
public class SpeedReportMain {

  private static final Logger logger = Logger.getLogger(SpeedReportMain.class.getName());

  private static final List<String> DEFAULT_DIRECTORIES = List.of(
      "mzmine-community/src/test/java/import_data/speed", "src/test/java/import_data/speed",
      "D:\\git\\mzmine3\\mzmine-community\\src\\test\\java\\import_data\\speed", "D:\\");

  public static void main(String[] args) {
    final Path input = args.length > 0 ? Path.of(args[0]) : resolveDefaultInput();
    final Path html = args.length > 1 ? Path.of(args[1])
        : input.resolveSibling(stripExtension(input.getFileName().toString()) + "_report.html");

    try {
      final List<SpeedReportRow> rows = read(input);
      if (rows.isEmpty()) {
        logger.warning("No measurements found in " + input.toAbsolutePath());
        return;
      }
      writeHtml(rows, input, html);
      logger.info("Wrote speed report of %d measurements to %s".formatted(rows.size(),
          html.toAbsolutePath()));
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to create speed report: " + e.getMessage(), e);
    }
  }

  /**
   * The main is usually started from the repository root but may also run with the module as working
   * directory. The jsonlines export is preferred over the csv.
   */
  @NotNull
  private static Path resolveDefaultInput() {
    for (final String directory : DEFAULT_DIRECTORIES) {
      for (final String name : List.of("speed.jsonlines", "speed.csv")) {
        final Path candidate = Path.of(directory, name);
        if (Files.exists(candidate)) {
          return candidate;
        }
      }
    }
    return Path.of(DEFAULT_DIRECTORIES.getFirst(), "speed.jsonlines");
  }

  /**
   * Reads either the jsonlines or the csv export written by the current
   * {@link SpeedMeasurementWriter}.
   */
  @NotNull
  public static List<SpeedReportRow> read(@NotNull final Path file) throws IOException {
    final List<Map<String, String>> records =
        file.getFileName().toString().endsWith(".jsonlines") ? readJsonLines(file) : readCsv(file);

    final List<SpeedReportRow> rows = new ArrayList<>(records.size());
    for (final Map<String, String> record : records) {
      final SpeedReportRow row = toRow(record);
      if (row != null) {
        rows.add(row);
      }
    }
    return rows;
  }

  @Nullable
  private static SpeedReportRow toRow(@NotNull final Map<String, String> values) {
    final String name = values.getOrDefault("name", "").trim();
    final double seconds = parseDouble(values.get("timeSeconds"), Double.NaN);
    if (name.isBlank() || Double.isNaN(seconds)) {
      return null;
    }

    return new SpeedReportRow((int) parseDouble(values.get("step"), 0), name,
        values.getOrDefault("description", "").trim(), values.getOrDefault("phase", "").trim(),
        (int) parseDouble(values.get("iteration"), 0), values.getOrDefault("runId", "").trim(),
        values.getOrDefault("status", "").trim(), (int) parseDouble(values.get("files"), 0),
        seconds, parseNullableDouble(values.get("gbRamUsed")),
        parseNullableDouble(values.get("tempFilesCreated")),
        parseNullableDouble(values.get("reservedTempFileGB")),
        parseNullableDouble(values.get("usedTempFileGB")),
        parseNullableDouble(values.get("liveTempFiles")),
        parseNullableDouble(values.get("liveTempFileUsedGB")),
        (int) parseDouble(values.get("featureLists"), 0), parseNullableDouble(values.get("rows")),
        parseNullableDouble(values.get("features")),
        parseNullableDouble(values.get("tempDirUsedGB")),
        parseNullableDouble(values.get("peakHeapGB")), parseNullableDouble(values.get("gcCount")),
        parseNullableDouble(values.get("gcTimeSeconds")), describeEnvironment(values));
  }

  /**
   * Compact summary of the documented configuration - the report warns when two compared series
   * were not measured with the same one.
   */
  @NotNull
  private static String describeEnvironment(@NotNull final Map<String, String> values) {
    return String.join(" | ", "mzmine " + values.getOrDefault("mzmineVersion", "?").trim(),
        "inMemory=" + values.getOrDefault("inMemory", "?").trim(),
        "gc=" + values.getOrDefault("runGCafterBatchStep", "?").trim(),
        "threads=" + values.getOrDefault("numOfThreads", "?").trim() + "/" + values.getOrDefault(
            "availableProcessors", "?").trim(),
        "maxHeap=" + values.getOrDefault("maxHeapGB", "?").trim() + " GB",
        values.getOrDefault("memoryVmArgs", "").trim(), values.getOrDefault("osName", "").trim());
  }

  @NotNull
  private static List<Map<String, String>> readJsonLines(@NotNull final Path file)
      throws IOException {
    final ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    final List<Map<String, String>> records = new ArrayList<>();
    for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
      if (line.isBlank()) {
        continue;
      }
      final JsonNode node = mapper.readTree(line);
      final Map<String, String> values = new LinkedHashMap<>();
      for (final Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
        final String field = it.next();
        final JsonNode value = node.get(field);
        values.put(field, value == null || value.isNull() ? "" : value.asText());
      }
      records.add(values);
    }
    return records;
  }

  @NotNull
  private static List<Map<String, String>> readCsv(@NotNull final Path csv) throws IOException {
    final List<Map<String, String>> records = new ArrayList<>();
    List<String> columns = null;
    for (final String line : Files.readAllLines(csv, StandardCharsets.UTF_8)) {
      if (line.isBlank()) {
        continue;
      }
      final List<String> values = splitCsvLine(line).stream().map(String::trim).toList();
      if (columns == null) {
        columns = values;
        continue;
      }
      // headers are appended again when a new file is started - skip repeated header lines
      if (values.equals(columns)) {
        continue;
      }
      final Map<String, String> record = new LinkedHashMap<>();
      for (int i = 0; i < columns.size() && i < values.size(); i++) {
        record.put(columns.get(i), values.get(i));
      }
      records.add(record);
    }
    return records;
  }

  @NotNull
  private static List<String> splitCsvLine(@NotNull final String line) {
    final List<String> values = new ArrayList<>();
    final StringBuilder current = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < line.length(); i++) {
      final char c = line.charAt(i);
      if (c == '"') {
        // escaped quote inside a quoted value
        if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          current.append('"');
          i++;
        } else {
          quoted = !quoted;
        }
      } else if (c == ',' && !quoted) {
        values.add(current.toString());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    values.add(current.toString());
    return values;
  }

  private static double parseDouble(@Nullable final String value, final double defaultValue) {
    try {
      return value == null || value.isBlank() ? defaultValue : Double.parseDouble(value.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  @Nullable
  private static Double parseNullableDouble(@Nullable final String value) {
    final double parsed = parseDouble(value, Double.NaN);
    return Double.isNaN(parsed) ? null : parsed;
  }

  private static void writeHtml(@NotNull final List<SpeedReportRow> rows,
      @NotNull final Path source, @NotNull final Path html) throws IOException {
    final ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    final String data = mapper.writeValueAsString(rows);
    final String meta = mapper.writeValueAsString(
        Map.of("source", source.toAbsolutePath().toString(), "measurements", rows.size()));

    final String page = template().replace("/*__DATA__*/null", data)
        .replace("/*__META__*/null", meta);
    Files.writeString(html, page, StandardCharsets.UTF_8);
  }

  @NotNull
  private static String stripExtension(@NotNull final String fileName) {
    final int dot = fileName.lastIndexOf('.');
    return dot <= 0 ? fileName : fileName.substring(0, dot);
  }

  @NotNull
  private static String template() {
    return """
        <!doctype html>
        <html lang="en">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>mzmine batch speed report</title>
        <style>
          :root { color-scheme: light dark; --fg: #1c1c1e; --muted: #6b7280; --bg: #ffffff;
                  --panel: #f6f7f9; --grid: #d9dce1; --good: #2f9e44; --bad: #e03131;
                  --warn: #e8590c; }
          @media (prefers-color-scheme: dark) {
            :root { --fg: #e8e8ea; --muted: #9aa0a6; --bg: #16181c; --panel: #1e2126;
                    --grid: #33383f; --good: #51cf66; --bad: #ff6b6b; --warn: #ffa94d; }
          }
          body { margin: 0; padding: 20px 24px 40px; background: var(--bg); color: var(--fg);
                 font: 14px/1.45 system-ui, -apple-system, Segoe UI, sans-serif; }
          h1 { font-size: 19px; margin: 0 0 4px; }
          .sub { color: var(--muted); font-size: 12px; margin-bottom: 6px; }
          .warning { color: var(--warn); font-size: 12px; margin-bottom: 10px;
                     white-space: pre-wrap; }
          .controls { display: flex; flex-wrap: wrap; gap: 16px; align-items: center;
                      margin-bottom: 12px; font-size: 13px; }
          .legend { display: flex; flex-wrap: wrap; gap: 6px 18px; margin-bottom: 18px;
                    font-size: 12px; }
          .legend div { display: flex; align-items: center; gap: 6px; }
          .swatch { width: 11px; height: 11px; border-radius: 3px; flex: none; }
          .panels { display: grid; gap: 14px;
                    grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); }
          .panel { background: var(--panel); border-radius: 10px; padding: 10px 10px 4px; }
          #table .panel, #overview .panel { margin-bottom: 14px; }
          #overview svg { min-width: 100%; }
          .scroll { overflow-x: auto; }
          .panel h2 { font-size: 13px; margin: 0 0 2px; }
          .panel .note { color: var(--muted); font-size: 11px; margin: 0 0 4px; }
          svg { width: 100%; height: auto; display: block; }
          text { fill: var(--fg); }
          .axis { stroke: var(--grid); stroke-width: 1; }
          .tick { fill: var(--muted); font-size: 9px; }
          .xlabel { fill: var(--muted); font-size: 10px; }
          table { border-collapse: collapse; font-size: 12px; margin-bottom: 6px; }
          th, td { padding: 3px 12px 3px 0; text-align: right; white-space: nowrap; }
          th { color: var(--muted); font-weight: 600; }
          td.step, th.step { text-align: left; }
          .delta { font-size: 11px; }
          .faster { color: var(--good); }
          .slower { color: var(--bad); }
          .same { color: var(--muted); }
        </style>
        </head>
        <body>
        <h1>mzmine batch speed report</h1>
        <div class="sub" id="sub"></div>
        <div class="warning" id="warnings"></div>
        <div class="controls">
          <label>Metric <select id="metric"></select></label>
          <label>Baseline <select id="baseline"></select></label>
          <label><input type="checkbox" id="warmup"> include warmup iterations</label>
          <label><input type="checkbox" id="log"> log scale</label>
          <label><input type="checkbox" id="zero" checked> start axis at 0</label>
          <label><input type="checkbox" id="total" checked> include WHOLE BATCH in overview</label>
          <label><input type="checkbox" id="share"> per-step share of total</label>
          <label><input type="checkbox" id="drift"> drift per iteration</label>
          <button id="markdown" type="button">copy table as markdown</button>
        </div>
        <div class="legend" id="legend"></div>
        <div id="fingerprint"></div>
        <div id="table"></div>
        <div id="shareBox"></div>
        <div id="overview"></div>
        <div id="driftBox"></div>
        <div class="panels" id="panels"></div>
        <script>
        const DATA = /*__DATA__*/null;
        const META = /*__META__*/null;
        const COLORS = ['#4c78a8', '#f58518', '#54a24b', '#e45756', '#9d755d', '#b279a2',
                        '#72b7b2', '#eeca3b'];
        const SVGNS = 'http://www.w3.org/2000/svg';
        // every measurement that BatchTask collects, see StepMeasurement, plus the per iteration
        // values of SpeedIterationStats. The perIteration ones are the same on every row of an
        // iteration, so they are only meaningful on the WHOLE BATCH panel.
        const METRICS = [
          { key: 'timeSeconds', label: 'time (seconds)' },
          { key: 'gbRamUsed', label: 'used heap (GB)' },
          { key: 'tempFilesCreated', label: 'temp files created' },
          { key: 'reservedTempFileGB', label: 'temp files reserved (GB)' },
          { key: 'usedTempFileGB', label: 'temp files used (GB)' },
          { key: 'liveTempFiles', label: 'live temp files' },
          { key: 'liveTempFileUsedGB', label: 'live temp files used (GB)' },
          { key: 'tempDirUsedGB', label: 'temp dir disk used (GB)', perIteration: true },
          { key: 'peakHeapGB', label: 'peak heap (GB, tracked runs)', perIteration: true },
          { key: 'gcCount', label: 'GC count (tracked runs)', perIteration: true },
          { key: 'gcTimeSeconds', label: 'GC time (seconds, tracked runs)', perIteration: true },
          { key: 'rows', label: 'rows of newest feature list', perIteration: true },
          { key: 'features', label: 'features of newest feature list', perIteration: true }
        ];
        
        // series and step order are global so that colors and panels stay stable while filtering
        const seriesOrder = [];
        // key by step number and name, a batch may apply the same module in several steps
        const stepsByKey = new Map();
        for (const row of DATA) {
          if (!seriesOrder.includes(row.series)) seriesOrder.push(row.series);
          const key = row.step + '|' + row.name;
          if (!stepsByKey.has(key)) {
            stepsByKey.set(key, { key, step: row.step, name: row.name,
                label: (row.step > 0 ? row.step + '. ' : '') + row.name });
          }
        }
        // the whole batch (step 0) is most interesting last in the plots and first in the table
        const stepOrder = [...stepsByKey.values()]
            .sort((a, b) => (rank(a) - rank(b)) || (a.step - b.step));
        function rank(meta) {
          return meta.step === 0 ? 1 : 0;
        }
        const colorOf = series => COLORS[seriesOrder.indexOf(series) % COLORS.length];
        
        const el = (tag, attrs, text) => {
          const node = document.createElementNS(SVGNS, tag);
          for (const key in attrs) node.setAttribute(key, attrs[key]);
          if (text !== undefined) node.textContent = text;
          return node;
        };
        const tag = (name, cls, text) => {
          const node = document.createElement(name);
          if (cls) node.className = cls;
          if (text !== undefined) node.textContent = text;
          return node;
        };
        
        function stats(values) {
          const v = [...values].sort((a, b) => a - b);
          const q = p => {
            const idx = (v.length - 1) * p, lo = Math.floor(idx), hi = Math.ceil(idx);
            return lo === hi ? v[lo] : v[lo] + (v[hi] - v[lo]) * (idx - lo);
          };
          const q1 = q(0.25), med = q(0.5), q3 = q(0.75), iqr = q3 - q1;
          const loFence = q1 - 1.5 * iqr, hiFence = q3 + 1.5 * iqr;
          const inner = v.filter(x => x >= loFence && x <= hiFence);
          const mean = v.reduce((a, b) => a + b, 0) / v.length;
          return {
            n: v.length, q1, med, q3, mean, min: v[0], max: v[v.length - 1],
            whiskerLo: inner.length ? inner[0] : v[0],
            whiskerHi: inner.length ? inner[inner.length - 1] : v[v.length - 1],
            outliers: v.filter(x => x < loFence || x > hiFence), values: v
          };
        }

        const fmt = x => Math.abs(x) >= 100 ? x.toFixed(0)
                       : Math.abs(x) >= 10 ? x.toFixed(1) : x.toFixed(2);

        function ticks(min, max, log) {
          if (log) {
            const out = [];
            for (let e = Math.floor(Math.log10(min)); e <= Math.ceil(Math.log10(max)); e++) {
              for (const m of [1, 2, 5]) {
                const t = m * Math.pow(10, e);
                if (t >= min && t <= max) out.push(t);
              }
            }
            return out.length > 1 ? out : [min, max];
          }
          const raw = (max - min) / 5;
          const mag = Math.pow(10, Math.floor(Math.log10(raw)));
          const step = [1, 2, 2.5, 5, 10].map(m => m * mag).find(s => s >= raw) ?? mag * 10;
          const out = [];
          for (let t = Math.ceil(min / step) * step; t <= max + step * 1e-6; t += step) out.push(t);
          return out;
        }

        /**
         * Rows that pass the current filter, grouped as step -> series -> box. Only finished
         * iterations are used, warmups only when requested.
         */
        function groupRows(metric, withWarmup) {
          const steps = new Map();
          for (const row of DATA) {
            if (row.status !== 'FINISHED') continue;
            if (!withWarmup && row.phase !== 'PRODUCTION') continue;
            const value = row[metric];
            if (typeof value !== 'number' || !isFinite(value)) continue;
            const key = row.step + '|' + row.name;
            if (!steps.has(key)) steps.set(key, new Map());
            const bySeries = steps.get(key);
            if (!bySeries.has(row.series)) bySeries.set(row.series, []);
            bySeries.get(row.series).push(value);
          }
          return stepOrder.filter(meta => steps.has(meta.key)).map(meta => ({
            meta,
            boxes: seriesOrder.filter(s => steps.get(meta.key).has(s))
                .map(series => ({ series, stats: stats(steps.get(meta.key).get(series)) }))
          }));
        }

        function axisRange(allStats, log, fromZero) {
          let max = Math.max(...allStats.map(s => s.max));
          let min = Math.min(...allStats.map(s => s.min));
          if (log) {
            min = Math.max(min * 0.8, 1e-3);
            max = max * 1.2;
          } else {
            const pad = (max - min) * 0.08 || Math.max(max * 0.08, 0.01);
            min = fromZero ? 0 : Math.max(0, min - pad);
            max = max + pad;
          }
          if (max <= min) max = min + 1;
          return { min, max };
        }

        function makeScale(min, max, log, mT, plotH) {
          return v => {
            const t = log
                ? (Math.log10(Math.max(v, 1e-3)) - Math.log10(min))
                  / (Math.log10(max) - Math.log10(min))
                : (v - min) / (max - min);
            return mT + plotH - t * plotH;
          };
        }

        function drawGrid(svg, min, max, log, scale, mL, mR, mT, plotH, W) {
          for (const t of ticks(min, max, log)) {
            const y = scale(t);
            svg.appendChild(el('line', { class: 'axis', x1: mL, x2: W - mR, y1: y, y2: y }));
            svg.appendChild(el('text', { class: 'tick', x: mL - 5, y: y + 3,
                'text-anchor': 'end' }, fmt(t)));
          }
          svg.appendChild(el('line', { class: 'axis', x1: mL, x2: mL, y1: mT, y2: mT + plotH }));
        }

        function drawBox(svg, s, cx, bw, color, scale, tooltip) {
          const g = el('g', {});
          g.appendChild(el('title', {}, tooltip + '\\n' + 'n=' + s.n + ', median ' + fmt(s.med)
              + ', mean ' + fmt(s.mean) + ', min ' + fmt(s.min) + ', max ' + fmt(s.max)));
          // whiskers
          g.appendChild(el('line', { x1: cx, x2: cx, y1: scale(s.whiskerLo),
              y2: scale(s.whiskerHi), stroke: color, 'stroke-width': 1 }));
          for (const v of [s.whiskerLo, s.whiskerHi]) {
            g.appendChild(el('line', { x1: cx - bw / 4, x2: cx + bw / 4, y1: scale(v),
                y2: scale(v), stroke: color, 'stroke-width': 1 }));
          }
          // box and median
          g.appendChild(el('rect', { x: cx - bw / 2, y: scale(s.q3), width: bw,
              height: Math.max(scale(s.q1) - scale(s.q3), 1), fill: color,
              'fill-opacity': 0.25, stroke: color, 'stroke-width': 1, rx: 2 }));
          g.appendChild(el('line', { x1: cx - bw / 2, x2: cx + bw / 2, y1: scale(s.med),
              y2: scale(s.med), stroke: color, 'stroke-width': 2 }));
          // all replicates as jittered points, deterministic offsets
          s.values.forEach((v, j) => {
            const offset = ((j % 5) - 2) * (bw / 12);
            g.appendChild(el('circle', { cx: cx + offset, cy: scale(v), r: 1.9, fill: color,
                'fill-opacity': 0.85 }));
          });
          for (const v of s.outliers) {
            g.appendChild(el('circle', { cx: cx, cy: scale(v), r: 2.6, fill: 'none',
                stroke: color, 'stroke-width': 1 }));
          }
          svg.appendChild(g);
        }
        
        /**
         * Median per step and series relative to the baseline series. Steps are sorted by the
         * baseline median so that the most expensive steps come first, the whole batch stays on top.
         */
        function comparisonTable(groups, baseline, metricLabel) {
          const wrap = tag('div', 'panel');
          wrap.appendChild(tag('h2', null, 'Median per step vs baseline "' + baseline + '"'));
          wrap.appendChild(tag('p', 'note', metricLabel
              + ' - negative means lower than the baseline, steps sorted by baseline cost'));
        
          const statsOf = (group, series) => group.boxes.find(b => b.series === series)?.stats ?? null;
          const sorted = [...groups].sort((a, b) => {
            const byRank = rank(b.meta) - rank(a.meta);
            if (byRank !== 0) return byRank;
            return (statsOf(b, baseline)?.med ?? 0) - (statsOf(a, baseline)?.med ?? 0);
          });
        
          const table = tag('table');
          const head = tag('tr');
          head.appendChild(tag('th', 'step', 'step'));
          for (const series of seriesOrder) {
            head.appendChild(tag('th', null,
                series === baseline ? series + ' (baseline)' : series));
          }
          table.appendChild(head);
        
          for (const group of sorted) {
            const tr = tag('tr');
            tr.appendChild(tag('td', 'step', group.meta.label));
            const base = statsOf(group, baseline);
            for (const series of seriesOrder) {
              const s = statsOf(group, series);
              const td = tag('td');
              if (!s) {
                td.appendChild(tag('span', 'same', '-'));
              } else {
                td.appendChild(tag('span', null, fmt(s.med)));
                td.title = 'n=' + s.n + ', min ' + fmt(s.min) + ', max ' + fmt(s.max);
                if (base && series !== baseline && base.med > 0) {
                  const change = (s.med - base.med) / base.med * 100;
                  const cls = change < -2 ? 'faster' : change > 2 ? 'slower' : 'same';
                  td.appendChild(tag('span', 'delta ' + cls,
                      ' ' + (change >= 0 ? '+' : '') + change.toFixed(1) + '%'));
                }
              }
              tr.appendChild(td);
            }
            table.appendChild(tr);
          }
          const scroller = tag('div', 'scroll');
          scroller.appendChild(table);
          wrap.appendChild(scroller);
          return wrap;
        }

        /**
         * Combined plot: grouped by step on the x axis, one box per series within each step.
         */
        function overviewPanel(groups, metricLabel, log, fromZero) {
          const mL = 54, mR = 12, mT = 10, mB = 104, H = 400;
          const boxSlot = 22;
          const groupPad = 14;
          const groupW = groups.map(g => g.boxes.length * boxSlot + groupPad);
          const W = Math.max(760, mL + mR + groupW.reduce((a, b) => a + b, 0));
          const plotH = H - mT - mB;
        
          const wrap = tag('div', 'panel');
          wrap.appendChild(tag('h2', null, 'All steps - ' + metricLabel));
          wrap.appendChild(tag('p', 'note',
              'grouped by step, one box per series - hover a box for details'));
        
          const allStats = groups.flatMap(g => g.boxes.map(b => b.stats));
          const { min, max } = axisRange(allStats, log, fromZero);
          const scale = makeScale(min, max, log, mT, plotH);

          const svg = el('svg', { viewBox: '0 0 ' + W + ' ' + H, width: W, height: H });
          drawGrid(svg, min, max, log, scale, mL, mR, mT, plotH, W);

          let x = mL;
          groups.forEach((group, gi) => {
            const width = groupW[gi];
            group.boxes.forEach((box, i) => {
              const cx = x + groupPad / 2 + boxSlot * (i + 0.5);
              drawBox(svg, box.stats, cx, boxSlot * 0.62, colorOf(box.series), scale,
                  group.meta.label + ' - ' + box.series);
            });
            // separator between step groups
            if (gi > 0) {
              svg.appendChild(el('line', { class: 'axis', x1: x, x2: x, y1: mT, y2: mT + plotH,
                  'stroke-dasharray': '2 3' }));
            }
            const cx = x + width / 2;
            const y = mT + plotH + 8;
            // long step names are shortened, the full name stays in the tooltip
            const text = group.meta.label;
            const label = el('text', { class: 'xlabel', x: cx, y: y, 'text-anchor': 'end',
                transform: 'rotate(-35 ' + cx + ' ' + y + ')' },
                text.length > 26 ? text.slice(0, 25) + '...' : text);
            label.appendChild(el('title', {}, text));
            svg.appendChild(label);
            x += width;
          });
          const scroller = tag('div', 'scroll');
          scroller.appendChild(svg);
          wrap.appendChild(scroller);
          return wrap;
        }
        
        function panel(step, boxes, log, fromZero) {
          const W = 420, H = 250, mL = 48, mR = 10, mT = 8, mB = 34;
          const wrap = tag('div', 'panel');
          wrap.appendChild(tag('h2', null, step));
          wrap.appendChild(tag('p', 'note', boxes.map(b => b.series + ': n=' + b.stats.n
              + ', median ' + fmt(b.stats.med)).join('  |  ')));
        
          const { min, max } = axisRange(boxes.map(b => b.stats), log, fromZero);
          const plotH = H - mT - mB;
          const scale = makeScale(min, max, log, mT, plotH);
        
          const svg = el('svg', { viewBox: '0 0 ' + W + ' ' + H });
          drawGrid(svg, min, max, log, scale, mL, mR, mT, plotH, W);
        
          const slot = (W - mL - mR) / boxes.length;
          const bw = Math.min(slot * 0.55, 46);
          boxes.forEach((box, i) => {
            const s = box.stats;
            const cx = mL + slot * (i + 0.5);
            drawBox(svg, s, cx, bw, colorOf(box.series), scale, box.series);
            svg.appendChild(el('text', { class: 'xlabel', x: cx, y: H - mB + 14,
                'text-anchor': 'middle' }, String(seriesOrder.indexOf(box.series) + 1)));
            svg.appendChild(el('text', { class: 'xlabel', x: cx, y: H - mB + 26,
                'text-anchor': 'middle' }, fmt(s.med)));
          });
          wrap.appendChild(svg);
          return wrap;
        }
        
        /**
         * Result fingerprint: a series that produced fewer rows or features did not do the same work,
         * so a lower time is a regression and not a win. Uses the whole batch rows of one iteration.
         */
        function fingerprintPanel(withWarmup) {
          const wrap = tag('div', 'panel');
          wrap.appendChild(tag('h2', null, 'Result fingerprint'));
          wrap.appendChild(tag('p', 'note',
              'feature lists in the project and the newest feature list after each iteration -'
              + ' these must match, otherwise the series did not do the same work'));
        
          const bySeries = new Map();
          for (const row of DATA) {
            if (row.status !== 'FINISHED' || row.step !== 0) continue;
            if (!withWarmup && row.phase !== 'PRODUCTION') continue;
            if (!bySeries.has(row.series)) bySeries.set(row.series, new Set());
            bySeries.get(row.series).add([row.featureLists, row.rows, row.features].join(' / '));
          }
        
          const table = tag('table');
          const head = tag('tr');
          for (const column of ['series', 'feature lists / rows / features']) {
            head.appendChild(tag('th', column === 'series' ? 'step' : null, column));
          }
          table.appendChild(head);
          // every series should contribute exactly one distinct fingerprint
          const distinct = new Set();
          for (const series of seriesOrder) {
            const values = [...(bySeries.get(series) ?? [])];
            values.forEach(v => distinct.add(v));
            const tr = tag('tr');
            tr.appendChild(tag('td', 'step', series));
            const td = tag('td');
            td.appendChild(tag('span', values.length > 1 ? 'slower' : null,
                values.length ? values.join(', ') : '-'));
            tr.appendChild(td);
            table.appendChild(tr);
          }
          wrap.appendChild(table);
          if (distinct.size > 1) {
            wrap.appendChild(tag('p', 'note slower',
                'The series produced different results, the timings are not comparable.'));
          }
          return wrap;
        }
        
        /**
         * Share of the whole batch per step, one row per series. Shows whether a win comes from the
         * step that was actually changed.
         */
        function sharePanel(groups, baseline, metricLabel) {
          const wrap = tag('div', 'panel');
          wrap.appendChild(tag('h2', null, 'Share of the whole batch per step'));
          wrap.appendChild(tag('p', 'note', metricLabel + ' - median of each step relative to the'
              + ' summed step medians, hover a segment for details'));
        
          const steps = groups.filter(g => rank(g.meta) === 0);
          const H = 26, W = 800, labelW = 150;
          const svg = el('svg', { viewBox: '0 0 ' + W + ' ' + (seriesOrder.length * (H + 8) + 4) });
          seriesOrder.forEach((series, si) => {
            const values = steps.map(g => ({ meta: g.meta,
                med: g.boxes.find(b => b.series === series)?.stats?.med ?? 0 }));
            const total = values.reduce((a, b) => a + b.med, 0);
            const y = si * (H + 8);
            svg.appendChild(el('text', { class: 'xlabel', x: 0, y: y + H / 2 + 3 }, series));
            if (total <= 0) return;
            let x = labelW;
            values.forEach((value, i) => {
              const width = (W - labelW) * value.med / total;
              const g = el('g', {});
              g.appendChild(el('title', {}, value.meta.label + ': ' + fmt(value.med) + ' ('
                  + (value.med / total * 100).toFixed(1) + '%)'));
              g.appendChild(el('rect', { x: x, y: y, width: Math.max(width, 0.5), height: H,
                  fill: COLORS[i % COLORS.length], 'fill-opacity': 0.75 }));
              svg.appendChild(g);
              x += width;
            });
          });
          wrap.appendChild(svg);
          const legend = tag('p', 'note', steps.map((g, i) => (i + 1) + '. ' + g.meta.label)
              .join('  |  '));
          wrap.appendChild(legend);
          return wrap;
        }
        
        /**
         * Value over the iteration index per series, to spot a machine that got slower during the run
         * (thermal throttling, temp files piling up) instead of a real difference between series.
         */
        function driftPanel(metric, metricLabel, withWarmup) {
          const wrap = tag('div', 'panel');
          wrap.appendChild(tag('h2', null, 'Drift over the iterations - ' + metricLabel));
          wrap.appendChild(tag('p', 'note', 'whole batch per iteration in run order, a trend means'
              + ' the machine changed during the run'));
        
          const bySeries = new Map();
          for (const row of DATA) {
            if (row.status !== 'FINISHED' || row.step !== 0) continue;
            if (!withWarmup && row.phase !== 'PRODUCTION') continue;
            const value = row[metric];
            if (typeof value !== 'number' || !isFinite(value)) continue;
            const key = row.series + ' ' + row.runId;
            if (!bySeries.has(key)) bySeries.set(key, { series: row.series, points: [] });
            bySeries.get(key).points.push({ x: row.iteration, y: value, phase: row.phase });
          }
          const series = [...bySeries.values()];
          if (!series.length) {
            return wrap;
          }
          const all = series.flatMap(s => s.points);
          const W = 800, H = 240, mL = 54, mR = 12, mT = 10, mB = 28;
          const plotH = H - mT - mB;
          const maxX = Math.max(...all.map(p => p.x), 1);
          const { min, max } = axisRange([{ min: Math.min(...all.map(p => p.y)),
              max: Math.max(...all.map(p => p.y)) }], false, false);
          const scaleY = makeScale(min, max, false, mT, plotH);
          const scaleX = x => mL + (W - mL - mR) * (maxX === 1 ? 0.5 : (x - 1) / (maxX - 1));
        
          const svg = el('svg', { viewBox: '0 0 ' + W + ' ' + H });
          drawGrid(svg, min, max, false, scaleY, mL, mR, mT, plotH, W);
          for (const s of series) {
            const color = colorOf(s.series);
            const sorted = [...s.points].sort((a, b) => a.x - b.x);
            svg.appendChild(el('polyline', { fill: 'none', stroke: color, 'stroke-width': 1.4,
                points: sorted.map(p => scaleX(p.x) + ',' + scaleY(p.y)).join(' ') }));
            for (const p of sorted) {
              const dot = el('circle', { cx: scaleX(p.x), cy: scaleY(p.y), r: 2.6, fill: color,
                  'fill-opacity': p.phase === 'PRODUCTION' ? 1 : 0.35 });
              dot.appendChild(el('title', {}, s.series + ' ' + p.phase + ' iteration ' + p.x + ': '
                  + fmt(p.y)));
              svg.appendChild(dot);
            }
          }
          for (let x = 1; x <= maxX; x++) {
            svg.appendChild(el('text', { class: 'xlabel', x: scaleX(x), y: H - mB + 16,
                'text-anchor': 'middle' }, String(x)));
          }
          wrap.appendChild(svg);
          return wrap;
        }
        
        /**
         * The comparison table as markdown, for pasting into a pull request.
         */
        function tableAsMarkdown(groups, baseline, metricLabel) {
          const statsOf = (group, series) => group.boxes.find(b => b.series === series)?.stats ?? null;
          const lines = ['| step | ' + seriesOrder.map(s => s === baseline ? s + ' (baseline)' : s)
              .join(' | ') + ' |',
              '|---|' + seriesOrder.map(() => '---:').join('|') + '|'];
          for (const group of groups) {
            const base = statsOf(group, baseline);
            const cells = seriesOrder.map(series => {
              const s = statsOf(group, series);
              if (!s) return '-';
              if (!base || series === baseline || base.med <= 0) return fmt(s.med);
              const change = (s.med - base.med) / base.med * 100;
              return fmt(s.med) + ' (' + (change >= 0 ? '+' : '') + change.toFixed(1) + '%)';
            });
            lines.push('| ' + group.meta.label + ' | ' + cells.join(' | ') + ' |');
          }
          return metricLabel + ', median of ' + META.measurements + ' measurements\\n\\n'
              + lines.join('\\n') + '\\n';
        }
        
        /**
         * The groups of the last render, so that the markdown button uses what is on screen.
         */
        let lastRender = null;
        
        function render() {
          const metric = document.getElementById('metric').value;
          const metricDef = METRICS.find(m => m.key === metric);
          const metricLabel = metricDef.label;
          const baseline = document.getElementById('baseline').value;
          const withWarmup = document.getElementById('warmup').checked;
          const log = document.getElementById('log').checked;
          const fromZero = document.getElementById('zero').checked && !log;
          const withTotal = document.getElementById('total').checked;
          const withShare = document.getElementById('share').checked;
          const withDrift = document.getElementById('drift').checked;
          for (const id of ['fingerprint', 'table', 'shareBox', 'overview', 'driftBox', 'panels']) {
            document.getElementById(id).textContent = '';
          }
        
          document.getElementById('fingerprint').appendChild(fingerprintPanel(withWarmup));
        
          // per iteration metrics are identical on every step row, only the whole batch is meaningful
          const groups = groupRows(metric, withWarmup)
              .filter(g => !metricDef.perIteration || rank(g.meta) === 1);
          lastRender = { groups, baseline, metricLabel };
          if (!groups.length) return;
        
          document.getElementById('table')
              .appendChild(comparisonTable(groups, baseline, metricLabel));
          if (withShare && !metricDef.perIteration) {
            document.getElementById('shareBox')
                .appendChild(sharePanel(groups, baseline, metricLabel));
          }
          const forOverview = groups.filter(g => withTotal || rank(g.meta) === 0);
          if (forOverview.length) {
            document.getElementById('overview')
                .appendChild(overviewPanel(forOverview, metricLabel, log, fromZero));
          }
          if (withDrift) {
            document.getElementById('driftBox')
                .appendChild(driftPanel(metric, metricLabel, withWarmup));
          }
          for (const group of groups) {
            document.getElementById('panels')
                .appendChild(panel(group.meta.label, group.boxes, log, fromZero));
          }
        }
        
        function fillSelect(id, values, selected) {
          const select = document.getElementById(id);
          for (const value of values) {
            const option = document.createElement('option');
            option.value = value.key;
            option.textContent = value.label;
            if (value.key === selected) option.selected = true;
            select.appendChild(option);
          }
        }
        
        function renderLegend() {
          const legend = document.getElementById('legend');
          seriesOrder.forEach((series, i) => {
            const item = document.createElement('div');
            const swatch = tag('span', 'swatch');
            swatch.style.background = COLORS[i % COLORS.length];
            item.appendChild(swatch);
            item.appendChild(document.createTextNode((i + 1) + ' - ' + series));
            legend.appendChild(item);
          });
        }
        
        /**
         * The harness documents the configuration instead of pinning it, so warn when the compared
         * series were not measured with the same environment, or when iterations did not finish.
         */
        function renderWarnings() {
          const messages = [];
          const environments = [...new Set(DATA.map(r => r.environment))];
          if (environments.length > 1) {
            messages.push('Different environments in this file, the series may not be comparable:\\n'
                + environments.map(e => '  - ' + e).join('\\n'));
          }
          const failed = DATA.filter(r => r.status !== 'FINISHED').length;
          if (failed) messages.push(failed + ' measurements did not finish and are excluded.');
          document.getElementById('warnings').textContent = messages.join('\\n');
        }
        
        const production = DATA.filter(r => r.phase === 'PRODUCTION').length;
        document.getElementById('sub').textContent = META.measurements + ' measurements ('
            + production + ' production, ' + (META.measurements - production) + ' warmup), '
            + stepOrder.length + ' steps, ' + seriesOrder.length + ' series - ' + META.source;
        fillSelect('metric', METRICS.map(m => ({ key: m.key, label: m.label })), 'timeSeconds');
        fillSelect('baseline', seriesOrder.map(s => ({ key: s, label: s })), seriesOrder[0]);
        renderLegend();
        renderWarnings();
        for (const id of ['metric', 'baseline', 'warmup', 'log', 'zero', 'total', 'share',
            'drift']) {
          document.getElementById(id).addEventListener('change', render);
        }
        document.getElementById('markdown').addEventListener('click', () => {
          const button = document.getElementById('markdown');
          if (!lastRender) return;
          const markdown = tableAsMarkdown(lastRender.groups, lastRender.baseline,
              lastRender.metricLabel);
          // the clipboard is not available for local files in every browser, fall back to a prompt
          const done = () => {
            button.textContent = 'copied';
            setTimeout(() => button.textContent = 'copy table as markdown', 1500);
          };
          if (navigator.clipboard) {
            navigator.clipboard.writeText(markdown).then(done, () => window.prompt('markdown',
                markdown));
          } else {
            window.prompt('markdown', markdown);
          }
        });
        render();
        </script>
        </body>
        </html>
        """;
  }

}

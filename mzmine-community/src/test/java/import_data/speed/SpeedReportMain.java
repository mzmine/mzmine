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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reads the csv written by {@link BatchSpeedTestMain} (see speed.csv next to this class) and
 * creates a standalone html page with box plots. Each batch step ({@code name} column) becomes one
 * panel and each panel contains one box per version. The version is taken from the last comma
 * separated part of the description, e.g. {@code "inMemory=none, mzmine4.10.25_master GUI"} becomes
 * {@code "mzmine4.10.25_master GUI"}. Repeated runs of the same batch provide the replicates of a
 * box.
 * <p>
 * Usage: {@code SpeedReportMain [speed.csv] [report.html]}, both arguments are optional and default
 * to the csv next to this class and speed_report.html in the same folder.
 */
public class SpeedReportMain {

  private static final Logger logger = Logger.getLogger(SpeedReportMain.class.getName());

  private static final Path DEFAULT_CSV = Path.of(
      "D:\\git\\mzmine3\\mzmine-community\\src\\test\\java\\import_data\\speed\\speed.csv");

  public static void main(String[] args) {
    final Path csv = args.length > 0 ? Path.of(args[0]) : resolveDefaultCsv();
    final Path html = args.length > 1 ? Path.of(args[1])
        : csv.resolveSibling(stripExtension(csv.getFileName().toString()) + "_report.html");

    try {
      final List<SpeedReportRow> rows = readCsv(csv);
      if (rows.isEmpty()) {
        logger.warning("No measurements found in " + csv.toAbsolutePath());
        return;
      }
      writeHtml(rows, csv, html);
      logger.info("Wrote speed report of %d measurements to %s".formatted(rows.size(),
          html.toAbsolutePath()));
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to create speed report: " + e.getMessage(), e);
    }
  }

  /**
   * The main is usually started from the repository root but may also run with the module as
   * working directory.
   */
  @NotNull
  private static Path resolveDefaultCsv() {
    if (Files.exists(DEFAULT_CSV)) {
      return DEFAULT_CSV;
    }
    // working directory is the module itself
    final Path fromModule = Path.of("src", "test", "java", "import_data", "speed", "speed.csv");
    return Files.exists(fromModule) ? fromModule : DEFAULT_CSV;
  }

  @NotNull
  public static List<SpeedReportRow> readCsv(@NotNull final Path csv) throws IOException {
    final List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
    final List<SpeedReportRow> rows = new ArrayList<>();

    Map<String, Integer> columns = null;
    for (final String line : lines) {
      if (line.isBlank()) {
        continue;
      }
      final List<String> values = splitCsvLine(line);
      if (columns == null) {
        columns = new LinkedHashMap<>();
        for (int i = 0; i < values.size(); i++) {
          columns.put(values.get(i).trim(), i);
        }
        continue;
      }
      // headers are appended again on every new file - skip repeated header lines
      if ("name".equals(values.get(columns.get("name")).trim())) {
        continue;
      }
      final String step = value(values, columns, "name");
      final String description = value(values, columns, "description");
      if (step.isBlank()) {
        continue;
      }
      rows.add(new SpeedReportRow(step, extractVersion(description),
          (int) parseDouble(value(values, columns, "files"), 0),
          parseDouble(value(values, columns, "timeSeconds"), Double.NaN),
          parseNullableDouble(value(values, columns, "gbRamUsed"))));
    }
    rows.removeIf(row -> Double.isNaN(row.timeSeconds()));
    return rows;
  }

  @NotNull
  private static String value(final List<String> values, final Map<String, Integer> columns,
      final String column) {
    final Integer index = columns.get(column);
    return index == null || index >= values.size() ? "" : values.get(index).trim();
  }

  /**
   * Usually the last comma separated part of the description holds the mzmine version.
   */
  @NotNull
  public static String extractVersion(@NotNull final String description) {
    final int comma = description.lastIndexOf(',');
    final String version = comma < 0 ? description : description.substring(comma + 1);
    return version.isBlank() ? description.trim() : version.trim();
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

  private static double parseDouble(final String value, final double defaultValue) {
    try {
      return value.isBlank() ? defaultValue : Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  @Nullable
  private static Double parseNullableDouble(final String value) {
    final double parsed = parseDouble(value, Double.NaN);
    return Double.isNaN(parsed) ? null : parsed;
  }

  private static void writeHtml(@NotNull final List<SpeedReportRow> rows, @NotNull final Path csv,
      @NotNull final Path html) throws IOException {
    final ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    final String data = mapper.writeValueAsString(rows);
    final String meta = mapper.writeValueAsString(
        Map.of("source", csv.toAbsolutePath().toString(), "measurements", rows.size()));

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
                  --panel: #f6f7f9; --grid: #d9dce1; }
          @media (prefers-color-scheme: dark) {
            :root { --fg: #e8e8ea; --muted: #9aa0a6; --bg: #16181c; --panel: #1e2126;
                    --grid: #33383f; }
          }
          body { margin: 0; padding: 20px 24px 40px; background: var(--bg); color: var(--fg);
                 font: 14px/1.45 system-ui, -apple-system, Segoe UI, sans-serif; }
          h1 { font-size: 19px; margin: 0 0 4px; }
          .sub { color: var(--muted); font-size: 12px; margin-bottom: 14px; }
          .controls { display: flex; flex-wrap: wrap; gap: 16px; align-items: center;
                      margin-bottom: 12px; font-size: 13px; }
          .legend { display: flex; flex-wrap: wrap; gap: 6px 18px; margin-bottom: 18px;
                    font-size: 12px; }
          .legend div { display: flex; align-items: center; gap: 6px; }
          .swatch { width: 11px; height: 11px; border-radius: 3px; flex: none; }
          .panels { display: grid; gap: 14px;
                    grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); }
          .panel { background: var(--panel); border-radius: 10px; padding: 10px 10px 4px; }
          #overview .panel { margin-bottom: 14px; }
          #overview svg { min-width: 100%; }
          .scroll { overflow-x: auto; }
          .panel h2 { font-size: 13px; margin: 0 0 2px; }
          .panel .note { color: var(--muted); font-size: 11px; margin: 0 0 4px; }
          svg { width: 100%; height: auto; display: block; }
          text { fill: var(--fg); }
          .axis { stroke: var(--grid); stroke-width: 1; }
          .tick { fill: var(--muted); font-size: 9px; }
          .xlabel { fill: var(--muted); font-size: 10px; }
        </style>
        </head>
        <body>
        <h1>mzmine batch speed report</h1>
        <div class="sub" id="sub"></div>
        <div class="controls">
          <label>Metric
            <select id="metric">
              <option value="timeSeconds">time (seconds)</option>
              <option value="gbRamUsed">RAM (GB)</option>
            </select>
          </label>
          <label><input type="checkbox" id="log"> log scale</label>
          <label><input type="checkbox" id="zero" checked> start axis at 0</label>
          <label><input type="checkbox" id="total" checked> include WHOLE BATCH in overview</label>
        </div>
        <div class="legend" id="legend"></div>
        <div id="overview"></div>
        <div class="panels" id="panels"></div>
        <script>
        const DATA = /*__DATA__*/null;
        const META = /*__META__*/null;
        const COLORS = ['#4c78a8', '#f58518', '#54a24b', '#e45756', '#9d755d', '#b279a2',
                        '#72b7b2', '#eeca3b'];
        const SVGNS = 'http://www.w3.org/2000/svg';
        
        // grouping: panel per step, box per version, replicates from repeated runs
        const seriesOrder = [];
        const steps = new Map();
        for (const row of DATA) {
          if (!seriesOrder.includes(row.series)) seriesOrder.push(row.series);
          if (!steps.has(row.step)) steps.set(row.step, new Map());
          const bySeries = steps.get(row.step);
          if (!bySeries.has(row.series)) bySeries.set(row.series, []);
          bySeries.get(row.series).push(row);
        }
        // the total is most interesting last
        const stepOrder = [...steps.keys()].sort((a, b) => rank(a) - rank(b));
        function rank(step) {
          return step.toUpperCase().includes('WHOLE BATCH') ? 1 : 0;
        }
        
        const el = (tag, attrs, text) => {
          const node = document.createElementNS(SVGNS, tag);
          for (const key in attrs) node.setAttribute(key, attrs[key]);
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
         * All boxes of one step, in the global version order. Steps without values for the current
         * metric return an empty array.
         */
        function boxesOfStep(step, metric) {
          const bySeries = steps.get(step);
          return seriesOrder.map(series => {
            const values = (bySeries.get(series) ?? [])
                .map(r => r[metric]).filter(v => typeof v === 'number' && isFinite(v));
            return { series, stats: values.length ? stats(values) : null };
          }).filter(b => b.stats);
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
        
        const colorOf = series => COLORS[seriesOrder.indexOf(series) % COLORS.length];
        
        function render() {
          const metric = document.getElementById('metric').value;
          const log = document.getElementById('log').checked;
          const fromZero = document.getElementById('zero').checked && !log;
          const withTotal = document.getElementById('total').checked;
          const overview = document.getElementById('overview');
          const panels = document.getElementById('panels');
          overview.textContent = '';
          panels.textContent = '';
        
          const groups = stepOrder.map(step => ({ step, boxes: boxesOfStep(step, metric) }))
              .filter(g => g.boxes.length);
          if (!groups.length) return;
        
          const forOverview = groups.filter(g => withTotal || rank(g.step) === 0);
          if (forOverview.length) {
            overview.appendChild(overviewPanel(forOverview, metric, log, fromZero));
          }
          for (const group of groups) {
            panels.appendChild(panel(group.step, group.boxes, log, fromZero));
          }
        }
        
        /**
         * Combined plot: grouped by step on the x axis, one box per version within each step.
         */
        function overviewPanel(groups, metric, log, fromZero) {
          const mL = 54, mR = 12, mT = 10, mB = 104, H = 400;
          const boxSlot = 22;
          const groupPad = 14;
          const groupW = groups.map(g => g.boxes.length * boxSlot + groupPad);
          const W = Math.max(760, mL + mR + groupW.reduce((a, b) => a + b, 0));
          const plotH = H - mT - mB;
        
          const wrap = document.createElement('div');
          wrap.className = 'panel';
          const title = document.createElement('h2');
          title.textContent = 'All steps'
              + (metric === 'timeSeconds' ? ' - time (seconds)' : ' - RAM (GB)');
          const note = document.createElement('p');
          note.className = 'note';
          note.textContent = 'grouped by step, one box per version - hover a box for details';
          wrap.appendChild(title);
          wrap.appendChild(note);
        
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
                  group.step + ' - ' + box.series);
            });
            // separator between step groups
            if (gi > 0) {
              svg.appendChild(el('line', { class: 'axis', x1: x, x2: x, y1: mT, y2: mT + plotH,
                  'stroke-dasharray': '2 3' }));
            }
            const cx = x + width / 2;
            const y = mT + plotH + 8;
            // long step names are shortened, the full name stays in the tooltip
            const label = el('text', { class: 'xlabel', x: cx, y: y, 'text-anchor': 'end',
                transform: 'rotate(-35 ' + cx + ' ' + y + ')' },
                group.step.length > 26 ? group.step.slice(0, 25) + '...' : group.step);
            label.appendChild(el('title', {}, group.step));
            svg.appendChild(label);
            x += width;
          });
          const scroller = document.createElement('div');
          scroller.className = 'scroll';
          scroller.appendChild(svg);
          wrap.appendChild(scroller);
          return wrap;
        }
        
        function panel(step, boxes, log, fromZero) {
          const W = 420, H = 250, mL = 48, mR = 10, mT = 8, mB = 34;
          const wrap = document.createElement('div');
          wrap.className = 'panel';
          const title = document.createElement('h2');
          title.textContent = step;
          const note = document.createElement('p');
          note.className = 'note';
          note.textContent = boxes.map(b => b.series + ': n=' + b.stats.n + ', median '
              + fmt(b.stats.med)).join('  |  ');
          wrap.appendChild(title);
          wrap.appendChild(note);
        
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
        
        function renderLegend() {
          const legend = document.getElementById('legend');
          seriesOrder.forEach((series, i) => {
            const item = document.createElement('div');
            const swatch = document.createElement('span');
            swatch.className = 'swatch';
            swatch.style.background = COLORS[i % COLORS.length];
            item.appendChild(swatch);
            item.appendChild(document.createTextNode((i + 1) + ' - ' + series));
            legend.appendChild(item);
          });
        }
        
        document.getElementById('sub').textContent = META.measurements + ' measurements, '
            + stepOrder.length + ' steps, ' + seriesOrder.length + ' versions - '
            + META.source;
        renderLegend();
        for (const id of ['metric', 'log', 'zero', 'total']) {
          document.getElementById(id).addEventListener('change', render);
        }
        render();
        </script>
        </body>
        </html>
        """;
  }

}

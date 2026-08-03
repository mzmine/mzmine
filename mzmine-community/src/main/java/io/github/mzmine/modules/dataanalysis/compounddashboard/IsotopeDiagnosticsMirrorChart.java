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

package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.javafx.util.color.ColorsFX;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.ChargeDiagnostics;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.IsotopeSignalAttribution;
import io.github.mzmine.modules.visualization.spectra.spectra_stack.pseudospectra.PseudoSpectraItemLabelGenerator;
import io.github.mzmine.modules.visualization.spectra.spectra_stack.pseudospectra.PseudoSpectraRenderer;
import io.github.mzmine.modules.visualization.spectra.spectra_stack.pseudospectra.PseudoSpectrumDataSet;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleInsets;

/**
 * Builds the developer-only isotope finder diagnostics mirror for the compound dashboard: the
 * detected isotope pattern on top (per-peak element labels, plausibility colouring, ghost sticks
 * for expected-but-absent offsets, a marker at the placed monoisotopic and the M+1 ratio gate band)
 * and, on the bottom, either the representative MS1 scan or the recomputed averagine envelope model
 * used to score. All inputs come from a single {@link ChargeDiagnostics} recomputed on demand.
 */
final class IsotopeDiagnosticsMirrorChart {

  // an expected offset is drawn as a ghost stick / included in the envelope overlay when its
  // predicted relative intensity reaches this fraction of the base peak
  private static final double GHOST_CUTOFF = 0.02;
  // an observed signal counts as "exceeds the plausible upper bound" only beyond this relative slack,
  // so a signal marginally over the bound is not flagged red on rounding noise
  private static final double EXCEEDS_SLACK = 1.05;

  private IsotopeDiagnosticsMirrorChart() {
  }

  /**
   * @param diag            the recomputed diagnostics for the selected charge.
   * @param ms1             the representative MS1 scan (bottom spectrum when not overlaying the
   *                        envelope).
   * @param envelopeOverlay when true, the bottom shows the recomputed averagine envelope model
   *                        instead of the MS1 scan.
   * @return a mirror chart viewer.
   */
  static @NotNull EChartViewer create(@NotNull final ChargeDiagnostics diag,
      @NotNull final Scan ms1, final boolean envelopeOverlay) {
    final SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    final Color withinColor = palette.getPositiveColorAWT();       // observed within the upper bound
    final Color exceedsColor = palette.getNegativeColorAWT();      // observed exceeds the upper bound
    final Color ghostColor = palette.getNeutralColorAWT();         // expected but absent
    final Color gateColor = FxColorUtil.fxColorToAWT(ColorsFX.getModifiedSignalColor());
    final Color envelopeColor = palette.getPositiveColorAWT();

    final double baseIntensity = diag.baseIntensity();
    // The envelope is normalised to its own apex (=1.0), but the observed base may sit at a
    // different offset. Scale the model so its predicted intensity at the placed base offset equals
    // the observed base intensity, so ghost sticks and the overlay align to the observed pattern.
    final int placement = diag.placement();
    final double[] expected = diag.envelopeExpected();
    final double expAtBase =
        placement >= 0 && placement < expected.length ? expected[placement] : 0d;
    final double envScale = expAtBase > 0d ? baseIntensity / expAtBase : baseIntensity;
    // reference intensity for the M+1 ratio gate: the observed monoisotopic if present, else the base
    double monoIntensity = baseIntensity;
    for (final IsotopeSignalAttribution s : diag.signals()) {
      if (s.offsetFromMono() == 0) {
        monoIntensity = s.intensity();
        break;
      }
    }

    // --- top subplot datasets: within / exceeds / ghost / gate ---------------
    final PseudoSpectrumDataSet within = new PseudoSpectrumDataSet(true, "within bound");
    final PseudoSpectrumDataSet exceeds = new PseudoSpectrumDataSet(true, "exceeds bound");
    final PseudoSpectrumDataSet ghost = new PseudoSpectrumDataSet(true, "expected (absent)");
    final PseudoSpectrumDataSet gate = new PseudoSpectrumDataSet(true, "M+1 gate");

    for (final IsotopeSignalAttribution s : diag.signals()) {
      final double predUpper = upperBoundAt(diag, s.offsetFromMono());
      final boolean over = predUpper > 0d && s.relIntensity() > predUpper * EXCEEDS_SLACK;
      (over ? exceeds : within).addDP(s.mz(), s.intensity(), s.label());
    }

    // ghost sticks for expected offsets with no observed signal near them
    for (int o = 0; o <= diag.maxPredictedOffset(); o++) {
      final double exp = diag.envelopeExpected()[o];
      if (exp < GHOST_CUTOFF) {
        continue;
      }
      final int offsetFromMono = o; // predicted offset already relative to the monoisotopic
      final boolean observed = hasSignalAtOffset(diag, offsetFromMono);
      if (!observed) {
        ghost.addDP(diag.mzForPredictedOffset(o), exp * envScale, "exp M+" + o);
      }
    }

    // M+1 ratio gate band: two reference sticks at the M+1 m/z showing the allowed lower and upper
    // M+1 intensity for a real 13C peak.
    final double[] m1 = diag.m1Bounds();
    if (m1.length == 2 && monoIntensity > 0d) {
      final double m1Mz = diag.mzForPredictedOffset(1);
      gate.addDP(m1Mz, m1[1] * monoIntensity, "M+1 gate max");
      gate.addDP(m1Mz, m1[0] * monoIntensity, "M+1 gate min");
    }

    // --- bottom subplot dataset: MS1 or the averagine envelope model ---------
    final PseudoSpectrumDataSet bottom;
    final Color bottomColor;
    if (envelopeOverlay) {
      bottom = new PseudoSpectrumDataSet(true, "averagine envelope");
      bottomColor = envelopeColor;
      for (int o = 0; o <= diag.maxPredictedOffset(); o++) {
        final double exp = diag.envelopeExpected()[o];
        if (exp >= GHOST_CUTOFF) {
          bottom.addDP(diag.mzForPredictedOffset(o), exp * envScale, "M+" + o);
        }
      }
    } else {
      bottom = new PseudoSpectrumDataSet(true, "Representative MS1");
      bottomColor = ghostColor;
      for (final DataPoint dp : ScanUtils.extractDataPoints(ms1)) {
        bottom.addDP(dp.getMZ(), dp.getIntensity(), null);
      }
    }

    final NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    final NumberFormat intForm = MZmineCore.getConfiguration().getIntensityFormat();

    final NumberAxis xAxis = new NumberAxis("m/z");
    xAxis.setNumberFormatOverride(mzForm);
    xAxis.setAutoRangeIncludesZero(false);
    xAxis.setUpperMargin(0.08);
    xAxis.setLowerMargin(0.08);

    // top subplot (detected pattern)
    final NumberAxis topAxis = new NumberAxis("Detected (z=" + diag.charge() + ")");
    topAxis.setNumberFormatOverride(intForm);
    topAxis.setAutoRangeIncludesZero(true);
    final XYPlot topPlot = new XYPlot(null, null, topAxis, null);
    topPlot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    addSeries(topPlot, 0, within, withinColor);
    addSeries(topPlot, 1, exceeds, exceedsColor);
    addSeries(topPlot, 2, ghost, ghostColor);
    addSeries(topPlot, 3, gate, gateColor);
    // marker at the placed monoisotopic m/z
    final ValueMarker monoMarker = new ValueMarker(diag.monoMz());
    monoMarker.setPaint(gateColor);
    monoMarker.setStroke(
        new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f},
            0f));
    monoMarker.setLabel("mono");
    topPlot.addDomainMarker(monoMarker);

    // bottom subplot (MS1 or envelope), range axis inverted
    final NumberAxis bottomAxis = new NumberAxis(
        envelopeOverlay ? "Averagine model" : "Representative MS1");
    bottomAxis.setNumberFormatOverride(intForm);
    bottomAxis.setAutoRangeIncludesZero(true);
    bottomAxis.setInverted(true);
    final XYPlot bottomPlot = new XYPlot(null, null, bottomAxis, null);
    bottomPlot.setRangeAxisLocation(AxisLocation.TOP_OR_LEFT);
    addSeries(bottomPlot, 0, bottom, bottomColor);

    final CombinedDomainXYPlot plot = new CombinedDomainXYPlot(xAxis);
    plot.setGap(0);
    plot.add(topPlot, 1);
    plot.add(bottomPlot, 1);
    plot.setOrientation(PlotOrientation.VERTICAL);
    plot.setBackgroundPaint(Color.white);
    plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);

    final JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    chart.setBackgroundPaint(Color.white);
    chart.getTitle().setVisible(false);
    chart.getXYPlot().setRangeZeroBaselineVisible(true);
    chart.getLegend().setVisible(true);

    final EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(chart);

    final EChartViewer viewer = new EChartViewer(chart);
    // labels reference the viewer, so attach after it exists (mirrors MirrorChartFactory)
    final PseudoSpectraItemLabelGenerator labelGen = new PseudoSpectraItemLabelGenerator(viewer);
    setLabels(topPlot, labelGen);
    if (envelopeOverlay) {
      setLabels(bottomPlot, labelGen);
    }

    zoomToWindow(plot, topPlot, bottomPlot, diag);
    return viewer;
  }

  private static void addSeries(@NotNull final XYPlot plot, final int index,
      @NotNull final PseudoSpectrumDataSet data, @NotNull final Color color) {
    plot.setDataset(index, data);
    plot.setRenderer(index, new PseudoSpectraRenderer(color, false));
  }

  private static void setLabels(@NotNull final XYPlot plot,
      @NotNull final PseudoSpectraItemLabelGenerator labelGen) {
    for (int i = 0; i < plot.getRendererCount(); i++) {
      if (plot.getRenderer(i) instanceof PseudoSpectraRenderer r) {
        r.setSeriesItemLabelGenerator(0, labelGen);
        r.setDefaultItemLabelsVisible(true);
      }
    }
  }

  /**
   * @return the predicted upper-bound relative intensity at a monoisotopic-relative offset.
   */
  private static double upperBoundAt(@NotNull final ChargeDiagnostics diag,
      final int offsetFromMono) {
    final double[] ub = diag.envelopeUpperBound();
    return offsetFromMono >= 0 && offsetFromMono < ub.length ? ub[offsetFromMono] : 0d;
  }

  private static boolean hasSignalAtOffset(@NotNull final ChargeDiagnostics diag,
      final int offsetFromMono) {
    for (final IsotopeSignalAttribution s : diag.signals()) {
      if (s.offsetFromMono() == offsetFromMono) {
        return true;
      }
    }
    return false;
  }

  /**
   * Restrict the shared domain to the pattern m/z window and auto-range each subplot's intensity to
   * the tallest signal within it.
   */
  private static void zoomToWindow(@NotNull final CombinedDomainXYPlot plot,
      @NotNull final XYPlot topPlot, @NotNull final XYPlot bottomPlot,
      @NotNull final ChargeDiagnostics diag) {
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    for (final IsotopeSignalAttribution s : diag.signals()) {
      min = Math.min(min, s.mz());
      max = Math.max(max, s.mz());
    }
    for (int o = 0; o <= diag.maxPredictedOffset(); o++) {
      if (diag.envelopeExpected()[o] >= GHOST_CUTOFF) {
        final double mz = diag.mzForPredictedOffset(o);
        min = Math.min(min, mz);
        max = Math.max(max, mz);
      }
    }
    if (min > max) {
      return;
    }
    final double lo = min - 3;
    final double hi = max + 3;
    plot.getDomainAxis().setRange(lo, hi);
    autoRange(topPlot, lo, hi);
    autoRange(bottomPlot, lo, hi);
  }

  private static void autoRange(@NotNull final XYPlot plot, final double lo, final double hi) {
    double maxIntensity = 0d;
    for (int d = 0; d < plot.getDatasetCount(); d++) {
      final var ds = plot.getDataset(d);
      if (ds == null) {
        continue;
      }
      for (int s = 0; s < ds.getSeriesCount(); s++) {
        for (int i = 0; i < ds.getItemCount(s); i++) {
          final double x = ds.getXValue(s, i);
          if (x >= lo && x <= hi) {
            maxIntensity = Math.max(maxIntensity, ds.getYValue(s, i));
          }
        }
      }
    }
    if (maxIntensity > 0d) {
      plot.getRangeAxis().setRange(0d, maxIntensity * 1.1);
    }
  }
}

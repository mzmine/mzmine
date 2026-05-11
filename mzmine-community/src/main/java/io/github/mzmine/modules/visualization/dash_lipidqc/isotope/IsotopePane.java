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

package io.github.mzmine.modules.visualization.dash_lipidqc.isotope;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.modules.visualization.dash_lipidqc.DashboardComputationPane;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidQcAnnotationSelectionUtils;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.SpectraItemLabelGenerator;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Dashboard panel that displays the measured versus theoretical isotope pattern for the currently
 * selected lipid annotation, helping to assess MS1-level isotope fit quality.
 */
public class IsotopePane extends DashboardComputationPane {

  private static final double APPROX_INSTRUMENT_RESOLUTION = 100_000d;
  private static final double MIN_THEORETICAL_ABUNDANCE = 0.005d;
  private static final double MIN_MERGE_WIDTH = 0.00005d;

  private final @NotNull SpectraPlot plot = new SpectraPlot();
  private @Nullable FeatureListRow row;

  public IsotopePane() {
    super("Select a row with an isotope pattern.");
    plot.getXYPlot().getDomainAxis().setLabel("m/z");
    ((NumberAxis) plot.getXYPlot().getDomainAxis()).setNumberFormatOverride(
        ConfigService.getGuiFormats().mzFormat());
    plot.getXYPlot().getRangeAxis().setLabel("Intensity");
    ((NumberAxis) plot.getXYPlot().getRangeAxis()).setNumberFormatOverride(
        ConfigService.getGuiFormats().intensityFormat());
    plot.setMinSize(250, 200);
  }

  public void setRow(final @Nullable FeatureListRow row) {
    this.row = row;
    requestUpdate();
  }

  private void requestUpdate() {
    scheduleUpdate(new IsotopeComputationTask(this, row));
  }

  void applyComputationResult(final @NotNull IsotopeComputationResult result) {
    plot.applyWithNotifyChanges(false, plot::removeAllDataSets);
    if (result.placeholderText() != null) {
      showPlaceholder(result.placeholderText());
      return;
    }

    final Color measuredColor = ConfigService.getDefaultColorPalette().getNegativeColorAWT();
    final Color theoreticalColor = ConfigService.getDefaultColorPalette().getPositiveColorAWT();

    final ColoredXYBarRenderer measuredRenderer = new ColoredXYBarRenderer(false);
    measuredRenderer.setDefaultItemLabelGenerator(new SpectraItemLabelGenerator(plot));
    plot.addDataSet(new ColoredXYDataset(
            new MassSpectrumProvider(result.measuredPattern(), "Isotope pattern",
                measuredColor)), measuredColor, false,
        measuredRenderer,
        false, false);

    if (result.theoreticalPattern() != null) {
      final XYLineAndShapeRenderer theoreticalRenderer = new XYLineAndShapeRenderer(false, true);
      theoreticalRenderer.setSeriesPaint(0, theoreticalColor);
      theoreticalRenderer.setSeriesShape(0, new Ellipse2D.Double(-4, -4, 8, 8));
      theoreticalRenderer.setSeriesStroke(0, new java.awt.BasicStroke(1.8f));
      theoreticalRenderer.setSeriesVisibleInLegend(0, true);
      plot.addDataSet(new ColoredXYDataset(
          new MassSpectrumProvider(result.theoreticalPattern(), "Theoretical isotope pattern",
              theoreticalColor)), theoreticalColor, false, theoreticalRenderer, false, false);
    }
    setCenter(plot);
  }

  static @NotNull IsotopeComputationResult computeResult(final @Nullable FeatureListRow row) {
    if (row == null) {
      return new IsotopeComputationResult("Select a row with an isotope pattern.", null, null);
    }
    final IsotopePattern measuredPattern = row.getBestIsotopePattern();
    if (measuredPattern == null) {
      return new IsotopeComputationResult("No isotope pattern available for selected row.", null,
          null);
    }

    final @Nullable MatchedLipid selectedMatch = LipidQcAnnotationSelectionUtils.getPreferredOrPotentialLipidMatch(
        row);
    final IsotopePattern theoreticalPattern = resolveTheoreticalPattern(row, selectedMatch);
    return new IsotopeComputationResult(null, measuredPattern, theoreticalPattern);
  }

  private static @Nullable IsotopePattern resolveTheoreticalPattern(final @NotNull FeatureListRow row,
      final @Nullable MatchedLipid selectedMatch) {
    if (selectedMatch == null) {
      return null;
    }
    IonType adductType = selectedMatch.getAdductType();
    if (adductType == null) {
      adductType = IonTypeParser.parse(selectedMatch.getIonizationType().getAdductName());
    }
    IsotopePattern pattern = null;
    final IMolecularFormula neutralFormula = selectedMatch.getLipidAnnotation().getMolecularFormula();
    if (neutralFormula != null && adductType != null) {
      final IMolecularFormula ionFormula = adductType.addToFormula(neutralFormula, true);
      final double referenceMz =
          selectedMatch.getAccurateMz() != null ? selectedMatch.getAccurateMz()
              : row.getAverageMZ();
      final double mergeWidth = Math.max(MIN_MERGE_WIDTH,
          referenceMz > 0d ? referenceMz / APPROX_INSTRUMENT_RESOLUTION : MIN_MERGE_WIDTH);
      pattern = IsotopePatternCalculator.calculateIsotopePattern(ionFormula,
          MIN_THEORETICAL_ABUNDANCE, mergeWidth, adductType.absTotalCharge(),
          adductType.getPolarity(), false);
    }
    final IsotopePattern measured = row.getBestIsotopePattern();
    if (pattern != null && measured != null && measured.getBasePeakIntensity() != null
        && measured.getBasePeakIntensity() > 0d) {
      pattern = IsotopePatternCalculator.normalizeIsotopePattern(pattern,
          measured.getBasePeakIntensity());
    }
    return pattern;
  }

}


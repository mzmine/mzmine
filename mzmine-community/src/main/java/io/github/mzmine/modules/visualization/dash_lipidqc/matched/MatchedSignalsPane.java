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

package io.github.mzmine.modules.visualization.dash_lipidqc.matched;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.visualization.dash_lipidqc.DashboardComputationPane;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidQcAnnotationSelectionUtils;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.LipidSpectrumPlot;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dashboard panel that displays the annotated lipid fragment spectrum for the selected feature
 * list row, showing matched and unmatched signals side-by-side.
 */
public class MatchedSignalsPane extends DashboardComputationPane {

  private @Nullable FeatureListRow row;
  private @Nullable LipidSpectrumPlot spectrumPlot;
  private @Nullable SpectraPlot rawSpectrumPlot;

  public MatchedSignalsPane() {
    super("Select a row with matched lipid signals.");
  }

  public void setRow(final @Nullable FeatureListRow row) {
    this.row = row;
    requestUpdate();
  }

  private void requestUpdate() {
    scheduleUpdate(new MatchedSignalsComputationTask(this, row));
  }

  void applyComputationResult(final @NotNull MatchedSignalsComputationResult result) {
    if (result.placeholderText() != null) {
      showPlaceholder(result.placeholderText());
      return;
    }

    final @Nullable MatchedLipid match = result.match();
    if (match != null) {
      if (spectrumPlot == null) {
        spectrumPlot = new LipidSpectrumPlot(match, true, RunOption.NEW_THREAD);
      } else {
        spectrumPlot.updateLipidSpectrum(match, true, RunOption.NEW_THREAD);
      }
      setCenter(spectrumPlot);
      return;
    }

    showRawMs2Spectrum(result.representativeMs2Scan());
  }

  static @NotNull MatchedSignalsComputationResult computeResult(final @Nullable FeatureListRow row) {
    if (row == null) {
      return new MatchedSignalsComputationResult("Select a row with matched lipid signals.", null,
          null);
    }

    final @Nullable Scan representativeMs2Scan = selectRepresentativeMs2Scan(row);
    if (representativeMs2Scan == null) {
      return new MatchedSignalsComputationResult("No MS2 spectrum available for selected row.",
          null, null);
    }

    final List<MatchedLipid> matches = row.get(LipidMatchListType.class);
    final @Nullable MatchedLipid match = matches == null || matches.isEmpty() ? null
        : LipidQcAnnotationSelectionUtils.getPreferredLipidMatch(row);
    if (match != null && !match.getMatchedFragments().isEmpty()) {
      return new MatchedSignalsComputationResult(null, representativeMs2Scan, match);
    }

    return new MatchedSignalsComputationResult(null, representativeMs2Scan, null);
  }

  private void showRawMs2Spectrum(final @Nullable Scan scan) {
    if (scan == null) {
      showPlaceholder("No MS2 spectrum available for selected row.");
      return;
    }
    if (rawSpectrumPlot == null) {
      rawSpectrumPlot = new SpectraPlot(false, true);
      rawSpectrumPlot.setMinHeight(50d);
    }
    rawSpectrumPlot.removeAllDataSets();
    rawSpectrumPlot.addDataSet(new ScanDataSet("MS2 spectrum", scan),
        ConfigService.getDefaultColorPalette().getNegativeColorAWT(), false, true);
    setCenter(rawSpectrumPlot);
  }

  private static @Nullable Scan selectRepresentativeMs2Scan(final @NotNull FeatureListRow row) {
    final List<Scan> scans = row.getAllFragmentScans();
    if (scans.isEmpty()) {
      return null;
    }
    return scans.stream().max(Comparator.comparingDouble(scan -> scan.getTIC() == null ? 0d
        : scan.getTIC())).orElse(scans.getFirst());
  }

}


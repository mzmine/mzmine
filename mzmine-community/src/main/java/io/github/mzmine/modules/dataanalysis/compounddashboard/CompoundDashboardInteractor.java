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

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.impl.MultiChargeStateIsotopePattern;
import io.github.mzmine.javafx.mvci.FxInteractor;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.scans.FragmentScanSelection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Maintains the derived state of the {@link CompoundDashboardModel} (available raw files, adduct
 * rows, default selections, color palette snapshot) and constructs the heavy
 * {@link CompoundDashboardSpectraTask} / {@link CompoundDashboardEicTask}. All methods here run on
 * the FX thread; the tasks themselves do their work on a background thread.
 */
public class CompoundDashboardInteractor extends FxInteractor<CompoundDashboardModel> {

  public CompoundDashboardInteractor(@NotNull final CompoundDashboardModel model) {
    super(model);
  }

  @Override
  public void updateModel() {
    // No FxUpdateTask drives a single-shot update for this interactor; the dataset tasks each
    // write their own slice of the model directly. This method is required by FxInteractor.
  }

  /**
   * Refresh derived state for the currently selected compound. Cheap; runs on FX thread.
   */
  public void onSelectionChanged() {
    final CompoundRow compound = model.getSelectedCompoundRow();
    if (compound == null) {
      model.getAvailableRawDataFiles().clear();
      model.getAdductRows().clear();
      model.getLegendEntries().clear();
      // Clear selectedAdductRow (the master selection); the controller's derived listener will
      // mirror that into selectedMs2Row.
      model.setSelectedAdductRow(null);
      model.setSelectedMs2Row(null);
      model.getAvailableMs2Scans().clear();
      model.setSelectedMs2Scan(null);
      model.getIsotopeChargeStates().clear();
      model.setSelectedIsotopePattern(null);
      model.setIsotopeRepresentativeScan(null);
      model.setCurrentRawDataFile(null);
      model.getEicDatasets().clear();
      model.getMobilogramDatasets().clear();
      model.getMs1Datasets().clear();
      model.getMs2Datasets().clear();
      return;
    }

    // Available raw files = union over all member rows (and nested sub-members = isotopes).
    final List<FeatureListRow> allRows = CompoundDashboardColoring.flattenAllMemberRows(compound);
    final Set<RawDataFile> union = new LinkedHashSet<>();
    for (final FeatureListRow row : allRows) {
      union.addAll(row.getRawDataFiles());
    }
    model.getAvailableRawDataFiles().setAll(union);

    // Legend entries mirror the per-row colors used by the EIC / mobilogram / MS1 tasks. Use a
    // fresh palette snapshot so the assignment matches the tasks (each task also starts from a
    // reset clone).
    final ColorAssignment legendColors = CompoundDashboardColoring.assign(compound,
        snapshotPalette());
    final List<CompoundDashboardLegendEntry> entries = new ArrayList<>(allRows.size());
    for (final FeatureListRow row : allRows) {
      entries.add(new CompoundDashboardLegendEntry(row, legendColors.colorFor(row)));
    }
    entries.sort(Comparator.comparingDouble(row -> row.row().getAverageMZ()));
    model.getLegendEntries().setAll(entries);

    // decision: always reevaluate the best raw data file for the new compound so the EIC combo
    // tracks the row that has the strongest coverage for this compound.
    model.setCurrentRawDataFile(FeatureListUtils.pickRawDataFileWithMostRowCoverage(allRows));

    // Adduct rows = top-level members that have an MS2 scan. The MS2 selector only lists rows the
    // MS2 chart can actually plot.
    final List<FeatureListRow> ms2Rows = compound.getMemberRows().stream()
        .filter(row -> row.getMostIntenseFragmentScan() != null).toList();
    model.getAdductRows().setAll(ms2Rows);
    // selectedAdductRow is the master selection (any member row); selectedMs2Row is derived from
    // it by the controller. Always reset to the compound's preferred row on a compound change so
    // the dashboard has a sensible default focus.
    final FeatureListRow preferred = compound.getPreferredRow();
    if (model.getSelectedAdductRow() != preferred) {
      model.setSelectedAdductRow(preferred);
    } else {
      // Same row instance as before -> no property change would fire, but adductRows may have
      // changed so selectedMs2Row could be stale. Trigger the derivation explicitly.
      final FeatureListRow desiredMs2 =
          (preferred != null && ms2Rows.contains(preferred)) ? preferred : null;
      if (model.getSelectedMs2Row() != desiredMs2) {
        model.setSelectedMs2Row(desiredMs2);
      }
    }

    // Re-snapshot the palette so the next computation starts from index 0.
    model.setColorPalette(snapshotPalette());
  }

  /**
   * @return a fresh clone of the current default palette with the cycling counter reset so
   * downstream consumers can call {@link SimpleColorPalette#getNextColor()} deterministically.
   */
  public static @NotNull SimpleColorPalette snapshotPalette() {
    return ConfigService.getDefaultColorPalette().clone(true);
  }

  /**
   * @return a new spectra task ready to run, or null when prerequisites are missing.
   */
  public @Nullable CompoundDashboardSpectraTask buildSpectraTask() {
    final CompoundRow compound = model.getSelectedCompoundRow();
    final SimpleColorPalette palette = model.getColorPalette();
    if (compound == null || palette == null) {
      return null;
    }
    return new CompoundDashboardSpectraTask(model, compound, model.getCurrentRawDataFile(),
        model.getSelectedMs2Row(), model.getSelectedMs2Scan(), palette.clone(true));
  }

  /**
   * Recompute {@link CompoundDashboardModel#getAvailableMs2Scans()} for the currently selected MS2
   * row. The first entry is a single merged MS2 (produced by a {@link SpectraMergeSelectParameter}
   * fixed to REPRESENTATIVE_SCANS across samples); the remaining entries are the row's individual
   * fragment scans in their natural order. Also resets
   * {@link CompoundDashboardModel#getSelectedMs2Scan()} to the first available entry so the MS2
   * chart shows the merged scan by default for a freshly selected row.
   */
  public void recomputeAvailableMs2Scans() {
    final FeatureListRow row = model.getSelectedMs2Row();
    if (row == null) {
      model.getAvailableMs2Scans().clear();
      model.setSelectedMs2Scan(null);
      return;
    }
    final List<Scan> sourceScans = row.getAllFragmentScans();
    // LinkedHashSet preserves insertion order (merged first, then source scans) AND deduplicates.
    // Dedup is not required for now as we just use one single merged scan
    // it is required if we switch to representative scans per energy
    final Set<Scan> result = LinkedHashSet.newLinkedHashSet(sourceScans.size() + 1);
    // Skip merging for a single source scan: the result would equal that scan anyway.
    if (sourceScans.size() > 1) {
      final SpectraMergeSelectParameter mergeParam = SpectraMergeSelectParameter.createGnpsSingleScanDefault();
      final FragmentScanSelection selection = mergeParam.createFragmentScanSelection(null);
      // SelectInputScans is NONE for SIMPLE_MERGED, so this returns only merged spectra. Note
      // that a "merged" entry can still equal a source scan when an energy bucket has only one
      // scan (see comment above); the LinkedHashSet handles the resulting overlap.
      result.addAll(selection.getAllFragmentSpectra(sourceScans));
    }
    result.addAll(sourceScans);
    model.getAvailableMs2Scans().setAll(result);
    // Always reset to the first (merged) entry when the row changes — the previous selection
    // refers to a different row and doesn't make sense here anymore.
    model.setSelectedMs2Scan(result.isEmpty() ? null : result.iterator().next());
  }

  /**
   * Recompute the isotope mirror inputs for the currently selected adduct row. The top spectrum of
   * the mirror is the detected isotope pattern of that row; when the row carries a
   * {@link MultiChargeStateIsotopePattern} each charge-state hypothesis becomes a selectable entry
   * in {@link CompoundDashboardModel#getIsotopeChargeStates()} (best score first), otherwise the
   * single detected pattern is the only entry. The bottom spectrum is the row's representative MS1
   * scan (same picking rule as the MS1 background scan).
   * <p>
   * decision: the source is strictly the selected adduct row — no fallback to the compound's
   * preferred row. When the selected row has no detected isotope pattern the charge list stays
   * empty; the view then shows the plain representative MS1 spectrum instead of the mirror.
   * <p>
   * The current charge-state selection is preserved when it still exists in the refreshed list (so
   * a raw-file change does not reset the user's charge pick); otherwise the preferred (first)
   * pattern is selected.
   */
  public void recomputeIsotopePattern() {
    final FeatureListRow source = model.getSelectedAdductRow();
    if (source == null) {
      model.getIsotopeChargeStates().clear();
      model.setSelectedIsotopePattern(null);
      model.setIsotopeRepresentativeScan(null);
      return;
    }
    final IsotopePattern best = source.getBestIsotopePattern();
    final List<IsotopePattern> patterns;
    if (best instanceof MultiChargeStateIsotopePattern multi) {
      patterns = List.copyOf(multi.getPatterns());
    } else if (best != null) {
      patterns = List.of(best);
    } else {
      patterns = List.of();
    }
    model.getIsotopeChargeStates().setAll(patterns);
    final IsotopePattern current = model.getSelectedIsotopePattern();
    model.setSelectedIsotopePattern(current != null && patterns.contains(current) ? current
        : (patterns.isEmpty() ? null : patterns.getFirst()));
    // Representative scan is always resolved for the selected row so the view can show a plain
    // full MS1 spectrum when no isotope pattern is available.
    model.setIsotopeRepresentativeScan(
        pickRepresentativeScan(source, model.getCurrentRawDataFile()));
  }

  /**
   * Pick the representative MS1 scan for {@code row}: the feature in {@code file} if present,
   * otherwise the row's best feature. Mirrors {@code CompoundDashboardSpectraTask} so the mirror's
   * bottom spectrum matches the MS1 background scan.
   */
  private static @Nullable Scan pickRepresentativeScan(@NotNull final FeatureListRow row,
      @Nullable final RawDataFile file) {
    if (file != null) {
      final Feature f = row.getFeature(file);
      if (f != null && f.getRepresentativeScan() != null) {
        return f.getRepresentativeScan();
      }
    }
    final Feature best = row.getBestFeature();
    return best == null ? null : best.getRepresentativeScan();
  }

  /**
   * @return a new EIC task ready to run, or null when prerequisites are missing.
   */
  public @Nullable CompoundDashboardEicTask buildEicTask() {
    final CompoundRow compound = model.getSelectedCompoundRow();
    final SimpleColorPalette palette = model.getColorPalette();
    if (compound == null || palette == null) {
      return null;
    }
    return new CompoundDashboardEicTask(model, compound, model.getCurrentRawDataFile(),
        palette.clone(true));
  }

  /**
   * @return a new mobilogram task ready to run, or null when prerequisites are missing.
   */
  public @Nullable CompoundDashboardMobilogramTask buildMobilogramTask() {
    final CompoundRow compound = model.getSelectedCompoundRow();
    final SimpleColorPalette palette = model.getColorPalette();
    if (compound == null || palette == null) {
      return null;
    }
    return new CompoundDashboardMobilogramTask(model, compound, model.getCurrentRawDataFile(),
        palette.clone(true));
  }
}

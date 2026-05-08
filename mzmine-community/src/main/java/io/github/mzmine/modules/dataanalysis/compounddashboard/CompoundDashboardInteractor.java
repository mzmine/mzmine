package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.javafx.mvci.FxInteractor;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
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
      model.setSelectedAdductRow(null);
      model.setCurrentRawDataFile(null);
      model.getEicDatasets().clear();
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

    // Pick a default current raw file when the previous one no longer applies.
    if (model.getCurrentRawDataFile() == null
        || !union.contains(model.getCurrentRawDataFile())) {
      model.setCurrentRawDataFile(FeatureListUtils.pickRawDataFileWithMostRowCoverage(allRows));
    }

    // Adduct rows = top-level members; these may themselves be CompoundRows for adducts with
    // isotopes. The MS2 selector picks among these.
    model.getAdductRows().setAll(compound.getMemberRows());
    if (model.getSelectedAdductRow() == null
        || !model.getAdductRows().contains(model.getSelectedAdductRow())) {
      model.setSelectedAdductRow(compound.getPreferredRow());
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
        model.getSelectedAdductRow(), palette.clone(true));
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
}

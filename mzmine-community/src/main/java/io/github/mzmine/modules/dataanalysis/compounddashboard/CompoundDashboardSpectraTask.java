package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import io.github.mzmine.util.color.SimpleColorPalette;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Off-FX-thread task that builds the MS1 stick spectrum and the MS2 spectrum for the dashboard.
 * Inputs are captured at construction so the FX thread can mutate the model freely while the task
 * runs.
 */
public class CompoundDashboardSpectraTask extends FxUpdateTask<CompoundDashboardModel> {

  private final @NotNull CompoundRow compound;
  private final @Nullable RawDataFile file;
  private final @Nullable FeatureListRow ms2Row;
  private final @NotNull SimpleColorPalette palette;

  private List<DatasetAndRenderer> ms1Out = List.of();
  private List<DatasetAndRenderer> ms2Out = List.of();

  public CompoundDashboardSpectraTask(@NotNull CompoundDashboardModel model,
      @NotNull CompoundRow compound, @Nullable RawDataFile file, @Nullable FeatureListRow ms2Row,
      @NotNull SimpleColorPalette palette) {
    super("Compound dashboard spectra", model);
    this.compound = compound;
    this.file = file;
    this.ms2Row = ms2Row;
    this.palette = palette;
  }

  @Override
  public String getTaskDescription() {
    return "Building compound dashboard spectra";
  }

  @Override
  public double getFinishedPercentage() {
    return 0d;
  }

  @Override
  protected void process() {
    final ColorAssignment colors = CompoundDashboardColoring.assign(compound, palette);
    ms1Out = buildMs1(colors);
    ms2Out = buildMs2(colors);
  }

  @Override
  protected void updateGuiModel() {
    model.getMs1Datasets().setAll(ms1Out);
    model.getMs2Datasets().setAll(ms2Out);
    model.setComputing(false);
  }

  // --- MS1 -------------------------------------------------------------------

  private @NotNull List<DatasetAndRenderer> buildMs1(@NotNull final ColorAssignment colors) {
    final List<DatasetAndRenderer> out = new ArrayList<>();

    // 1. gray background = preferredRow's representative apex MS1 scan in current raw file (or any)
    final FeatureListRow preferred = compound.getPreferredRow();
    final Scan repScan = pickRepresentativeScan(preferred);
    if (repScan != null) {
      out.add(new DatasetAndRenderer(new MassSpectrumProvider(repScan, "MS1 representative",
          FxColorUtil.fxColorToAWT(colors.representativeBackgroundColor())),
          new ColoredXYBarRenderer(false)));
    }

    // 2. one stick per row at row m/z, intensity = maxheight across files).
    // Walk top-level members + nested isotope sub-members.

    // compound first level is all ions
    for (final FeatureListRow row : compound.getMemberRows()) {
      DoubleArrayList mzs = new DoubleArrayList();
      DoubleArrayList intensities = new DoubleArrayList();

      final String label = stickLabel(row);
      final Color awt = FxColorUtil.fxColorToAWT(colors.colorFor(row));

      if (row instanceof CompoundRow ionRow) {
        // all isotopes
        for (FeatureListRow isotopeRow : ionRow.getMemberRows()) {
          mzs.add(isotopeRow.getAverageMZ());
          intensities.add(stickIntensity(isotopeRow));
        }
      } else {
        // single row no isotopes
        mzs.add(row.getAverageMZ());
        intensities.add(stickIntensity(row));
      }

      out.add(new DatasetAndRenderer(
          new MassSpectrumProvider(mzs.toDoubleArray(), intensities.toDoubleArray(), label, awt),
          new ColoredXYBarRenderer(false)));
    }
    return out;
  }

  private @Nullable Scan pickRepresentativeScan(@NotNull final FeatureListRow row) {
    if (file != null) {
      final Feature f = row.getFeature(file);
      if (f != null && f.getRepresentativeScan() != null) {
        return f.getRepresentativeScan();
      }
    }
    final Feature best = row.getBestFeature();
    return best == null ? null : best.getRepresentativeScan();
  }

  private double stickIntensity(@NotNull final FeatureListRow row) {
    // prefer max height for now.
//    if (file != null) {
//      final Feature f = row.getFeature(file);
//      if (f != null && f.getHeight() != null) {
//        return f.getHeight();
//      }
//    }
    final Float maxH = row.getMaxHeight();
    return maxH == null ? 0d : maxH;
  }

  private static @NotNull String stickLabel(@NotNull final FeatureListRow row) {
    final String ion = CompoundDashboardColoring.ionTypeLabel(row);
    if (ion != null) {
      return ion;
    }
    final Double mz = row.getAverageMZ();
    return mz == null ? ("row " + row.getID()) : ("m/z " + String.format("%.4f", mz));
  }

  // --- MS2 -------------------------------------------------------------------

  private @NotNull List<DatasetAndRenderer> buildMs2(@NotNull final ColorAssignment colors) {
    if (ms2Row == null) {
      return List.of();
    }
    final Scan ms2 = pickMs2Scan(ms2Row);
    if (ms2 == null) {
      return List.of();
    }
    final String label = "MS2 " + (CompoundDashboardColoring.ionTypeLabel(ms2Row) != null
        ? CompoundDashboardColoring.ionTypeLabel(ms2Row) : "");
    final Color awt = FxColorUtil.fxColorToAWT(colors.colorFor(ms2Row));
    return List.of(new DatasetAndRenderer(new MassSpectrumProvider(ms2, label, awt),
        new ColoredXYBarRenderer(false)));
  }

  private @Nullable Scan pickMs2Scan(@NotNull final FeatureListRow row) {
    if (file != null) {
      final Feature f = row.getFeature(file);
      if (f != null && f.getMostIntenseFragmentScan() != null) {
        return f.getMostIntenseFragmentScan();
      }
    }
    return row.getMostIntenseFragmentScan();
  }
}

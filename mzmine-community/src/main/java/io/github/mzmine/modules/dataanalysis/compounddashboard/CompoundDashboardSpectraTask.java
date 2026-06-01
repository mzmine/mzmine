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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.scans.ScanUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.awt.Color;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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
  private final @Nullable Scan ms2Scan;
  private final @NotNull SimpleColorPalette palette;

  private List<DatasetAndRenderer> ms1Out = List.of();
  private List<DatasetAndRenderer> ms2Out = List.of();
  private final Map<FeatureListRow, ColoredXYBarRenderer> ms1RenderersOut = new IdentityHashMap<>();
  private @Nullable String ms1TitleOut = "";
  private @Nullable String ms2TitleOut = "";

  public CompoundDashboardSpectraTask(@NotNull CompoundDashboardModel model,
      @NotNull CompoundRow compound, @Nullable RawDataFile file, @Nullable FeatureListRow ms2Row,
      @Nullable Scan ms2Scan, @NotNull SimpleColorPalette palette) {
    super("Compound dashboard spectra", model);
    this.compound = compound;
    this.file = file;
    this.ms2Row = ms2Row;
    this.ms2Scan = ms2Scan;
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
    // update the reverse map BEFORE the list setAll so list-change subscribers see a consistent
    // row -> renderer mapping for the new datasets
    model.getMs1RenderersByRow().clear();
    model.getMs1RenderersByRow().putAll(ms1RenderersOut);
    model.setMs1Title(ms1TitleOut);
    model.setMs2Title(ms2TitleOut);
    model.getMs1Datasets().setAll(ms1Out);
    model.getMs2Datasets().setAll(ms2Out);
    model.setComputing(false);
  }

  // --- MS1 -------------------------------------------------------------------

  private @NotNull List<DatasetAndRenderer> buildMs1(@NotNull final ColorAssignment colors) {
    final List<DatasetAndRenderer> out = new ArrayList<>();

    // 1. neutral-color background = preferredRow's representative apex MS1 scan in current raw
    // file (or any)
    final FeatureListRow preferred = compound.getPreferredRow();
    final Scan repScan = pickRepresentativeScan(preferred);
    if (repScan != null) {
      final Color representativeAwt = FxColorUtil.fxColorToAWT(
          colors.representativeBackgroundColor());
      final ColoredXYBarRenderer representativeRenderer = new ColoredXYBarRenderer(false);
      // match representative-stick labels to the neutral representative scan color
      representativeRenderer.setDefaultItemLabelPaint(representativeAwt);
      out.add(new DatasetAndRenderer(
          new MassSpectrumProvider(repScan, "MS1 representative", representativeAwt),
          representativeRenderer));
      ms1TitleOut = "MS1 (feature rows & representative MS1: " + repScan.getDataFile().getName()
          + ":" + repScan.getScanNumber() + ")";
    } else {
      ms1TitleOut = "MS1 (feature rows)";
    }

    // 2. one stick per row at row m/z, intensity = maxheight across files).
    // Walk top-level members + nested isotope sub-members.

    // compound first level is all ions
    for (final FeatureListRow row : compound.getMemberRows()) {
      final DoubleArrayList mzs = new DoubleArrayList();
      final DoubleArrayList intensities = new DoubleArrayList();

      // decision: parent (M) row is always at index 0 so IonGroupSpectrumProvider#getLabel(0)
      // labels exactly the parent stick. Isotopes are appended after.
      mzs.add(row.getAverageMZ());
      intensities.add(stickIntensity(row));

      if (row instanceof CompoundRow ionRow) {
        for (FeatureListRow isotopeRow : ionRow.getMemberRows()) {
          // skip isotope row that is the representative row
          if (isotopeRow.equals(((CompoundRow) row).getPreferredRow())) {
            continue;
          }

          mzs.add(isotopeRow.getAverageMZ());
          intensities.add(stickIntensity(isotopeRow));
        }
      }

      final String shortLabel = CompoundDashboardColoring.shortIonLabel(row);
      final String longLabel = CompoundDashboardColoring.longIonLabel(row);
      final Color awt = FxColorUtil.fxColorToAWT(colors.colorFor(row));
      final ColoredXYBarRenderer renderer = new ColoredXYBarRenderer(false);
      // match the on-plot stick label color to the dataset color
      renderer.setDefaultItemLabelPaint(awt);
      // remember the renderer that draws this adduct row's sticks so the controller can widen the
      // bars when this row becomes the selected adduct.
      ms1RenderersOut.put(row, renderer);
      out.add(new DatasetAndRenderer(
          new IonGroupSpectrumProvider(mzs.toDoubleArray(), intensities.toDoubleArray(), shortLabel,
              longLabel, shortLabel, awt), renderer));
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

  // --- MS2 -------------------------------------------------------------------

  private @NotNull List<DatasetAndRenderer> buildMs2(@NotNull final ColorAssignment colors) {
    if (ms2Row == null || ms2Scan == null) {
      ms2TitleOut = "";
      return List.of();
    }
    ms2TitleOut = buildMs2Title(ms2Scan);
    final String label = "MS2 " + (CompoundDashboardColoring.ionTypeLabel(ms2Row) != null
        ? CompoundDashboardColoring.ionTypeLabel(ms2Row) : "");
    final Color awt = FxColorUtil.fxColorToAWT(colors.colorFor(ms2Row));
    final ColoredXYBarRenderer renderer = new ColoredXYBarRenderer(false);
    // match the on-plot stick label color to the dataset color
    renderer.setDefaultItemLabelPaint(awt);
    return List.of(new DatasetAndRenderer(new MassSpectrumProvider(ms2Scan, label, awt), renderer));
  }

  private static @NotNull String buildMs2Title(@NotNull final Scan ms2) {
    final MsMsInfo info = ms2.getMsMsInfo();
    final ActivationMethod method = info != null ? info.getActivationMethod() : null;
    final Float energy = ScanUtils.extractCollisionEnergy(ms2);
    final String methodStr =
        method == null ? ActivationMethod.UNKNOWN.getAbbreviation() : method.getAbbreviation();
    final String energyStr;
    if (energy == null) {
      energyStr = "N.A.";
    } else if (method != null && !method.getUnit().isBlank()) {
      energyStr = energy + " " + method.getUnit();
    } else {
      energyStr = String.valueOf(energy);
    }
    return "MS2 (" + ms2.getDataFile().getName() + ":" + ms2.getScanNumber() + "; " + methodStr
        + "; " + energyStr + ")";
  }
}

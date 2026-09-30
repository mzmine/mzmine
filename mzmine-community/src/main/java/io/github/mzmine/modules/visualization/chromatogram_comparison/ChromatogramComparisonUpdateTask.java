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

package io.github.mzmine.modules.visualization.chromatogram_comparison;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extracts the chromatograms of both lists and groups them.
 */
class ChromatogramComparisonUpdateTask extends FxUpdateTask<ChromatogramComparisonModel> {

  private final @Nullable FeatureList listA;
  private final @Nullable FeatureList listB;
  private final @NotNull MZTolerance mzTolerance;
  private final @Nullable Number minSignalIntensity;
  private @NotNull List<ChromatogramGroup> groups = List.of();
  private @NotNull String status = "";
  private double progress = 0;

  ChromatogramComparisonUpdateTask(@NotNull ChromatogramComparisonModel model) {
    super("Update chromatogram comparison", model);
    listA = model.getFeatureListA();
    listB = model.getFeatureListB();
    mzTolerance = model.getMzTolerance();
    minSignalIntensity = model.getMinSignalIntensity();
  }

  @Override
  public boolean checkPreConditions() {
    return listA != null && listB != null && minSignalIntensity != null;
  }

  @Override
  public void onFailedPreCondition() {
    if (listA == null || listB == null) {
      model.getGroups().clear();
      model.setStatus("Select two feature lists");
    } else {
      model.setStatus("Enter a valid signal intensity");
    }
  }

  @Override
  protected void process() {
    if (listA == null || listB == null || minSignalIntensity == null) {
      return;
    }
    final RawDataFile file = listA.getRawDataFiles().stream()
        .filter(listB.getRawDataFiles()::contains).findFirst().orElse(null);
    if (file == null) {
      status = "The feature lists have no raw data file in common";
      return;
    }

    // all scans that either builder used
    final Set<Scan> usedA = identitySet(usedScans(listA, file));
    final Set<Scan> usedB = identitySet(usedScans(listB, file));
    final List<Scan> scans = file.getScans().stream()
        .filter(scan -> usedA.contains(scan) || usedB.contains(scan)).toList();
    final Map<Scan, Integer> scanIndex = new IdentityHashMap<>(scans.size());
    final float[] rts = new float[scans.size()];
    final boolean[] usedByA = new boolean[scans.size()];
    final boolean[] usedByB = new boolean[scans.size()];
    for (int i = 0; i < scans.size(); i++) {
      final Scan scan = scans.get(i);
      scanIndex.put(scan, i);
      rts[i] = scan.getRetentionTime();
      usedByA[i] = usedA.contains(scan);
      usedByB[i] = usedB.contains(scan);
    }
    progress = 0.1;

    final List<ComparedChromatogram> a = extract(ComparisonSide.A, listA, file, scanIndex);
    progress = 0.4;
    final List<ComparedChromatogram> b = extract(ComparisonSide.B, listB, file, scanIndex);
    progress = 0.7;
    if (isCanceled()) {
      return;
    }

    groups = ChromatogramComparison.compare(a, b, new ComparisonScans(rts, usedByA, usedByB),
        mzTolerance, minSignalIntensity.doubleValue(), MassListProbe.of(scans, mzTolerance),
        ChromatogramComparisonController.extractSegmentFilter(listA),
        ChromatogramComparisonController.extractSegmentFilter(listB));
    final long withIssues = groups.stream().filter(ChromatogramGroup::hasIssues).count();
    status = "%d groups of %d chromatograms (A) and %d (B), %d with issues".formatted(groups.size(),
        a.size(), b.size(), withIssues);
    progress = 1;
  }

  /**
   * @return the scans the chromatogram builder used or all MS1 scans if the list does not define
   * them
   */
  @NotNull
  private static List<? extends Scan> usedScans(@NotNull FeatureList flist,
      @NotNull RawDataFile file) {
    final List<? extends Scan> selected = flist.getSeletedScans(file);
    if (selected != null && !selected.isEmpty()) {
      return selected;
    }
    return file.getScans().stream().filter(scan -> scan.getMSLevel() == 1).toList();
  }

  @NotNull
  private static Set<Scan> identitySet(@NotNull List<? extends Scan> scans) {
    final Set<Scan> set = Collections.newSetFromMap(new IdentityHashMap<>(scans.size()));
    set.addAll(scans);
    return set;
  }

  /**
   * @return the chromatograms of the file without the zero intensities
   */
  @NotNull
  private List<ComparedChromatogram> extract(@NotNull ComparisonSide side,
      @NotNull FeatureList flist, @NotNull RawDataFile file,
      @NotNull Map<Scan, Integer> scanIndex) {
    final List<FeatureListRow> rows = flist.getRowsCopy();
    final List<ComparedChromatogram> chromatograms = new ArrayList<>(rows.size());
    for (final FeatureListRow row : rows) {
      if (isCanceled()) {
        break;
      }
      final Feature feature = row.getFeature(file);
      final IonTimeSeries<? extends Scan> data = feature == null ? null : feature.getFeatureData();
      if (data == null) {
        continue;
      }
      final List<? extends Scan> spectra = data.getSpectra();
      final int numValues = data.getNumberOfValues();
      final int[] indices = new int[numValues];
      final double[] intensities = new double[numValues];
      final double[] mzs = new double[numValues];
      int n = 0;
      for (int i = 0; i < numValues; i++) {
        final double intensity = data.getIntensity(i);
        final Integer index = scanIndex.get(spectra.get(i));
        if (!(intensity > 0) || index == null) {
          continue;
        }
        indices[n] = index;
        intensities[n] = intensity;
        mzs[n++] = data.getMZ(i);
      }
      final Double mz = feature.getMZ() != null ? feature.getMZ() : row.getAverageMZ();
      if (n == 0 || mz == null) {
        continue;
      }
      chromatograms.add(
          ComparedChromatogram.create(side, row.getID(), mz, Arrays.copyOf(indices, n),
              Arrays.copyOf(intensities, n), Arrays.copyOf(mzs, n), feature));
    }
    return chromatograms;
  }

  @Override
  protected void updateGuiModel() {
    model.getGroups().setAll(groups);
    model.setStatus(status);
  }

  @Override
  public String getTaskDescription() {
    return "Comparing the chromatograms of %s and %s".formatted(listA, listB);
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }
}

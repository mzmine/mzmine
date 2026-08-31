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

package io.github.mzmine.modules.dataprocessing.filter_duplicatefilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.dataprocessing.filter_duplicatefilter.DuplicateFilterParameters.FilterMode;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Reproduces the reported slowness of the {@link DuplicateFilterTask} on large aligned feature
 * lists (~1000 samples x ~5000 rows).
 * <p>
 * Sizes default to a scaled down version so the test stays fast in CI, but can be raised to the
 * reported case via system properties, e.g.
 * {@code gradlew :mzmine-community:test --tests "*DuplicateFilterTaskPerformanceTest"
 * -Dmzmine.test.dupfilter.files=1000 -Dmzmine.test.dupfilter.rows=5000}.
 */
@Disabled
class DuplicateFilterTaskPerformanceTest {

  private static final Logger logger = Logger.getLogger(
      DuplicateFilterTaskPerformanceTest.class.getName());

  private static final int N_FILES = Integer.getInteger("mzmine.test.dupfilter.files", 200);
  private static final int N_ROWS = Integer.getInteger("mzmine.test.dupfilter.rows", 1000);
  /**
   * every n-th row gets a near duplicate partner
   */
  private static final int DUPLICATE_EVERY = Integer.getInteger("mzmine.test.dupfilter.dupEvery",
      10);
  /**
   * fraction of samples that actually carry a feature for a row
   */
  private static final double FILL_RATE = 0.7;

  // wizard defaults
  private static final MZTolerance MZ_TOL = new MZTolerance(0.0008, 1.5);
  private static final RTTolerance RT_TOL = new RTTolerance(0.02f, Unit.MINUTES);

  @Test
  void newAverage_onLargeAlignedList() {
    final long tBuild = System.nanoTime();
    final Data data = buildAlignedList();
    logMillis("build synthetic aligned list", tBuild);

    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFeatureList(data.flist());

    final ParameterSet params = createParameters(FilterMode.NEW_AVERAGE,
        OriginalFeatureListOption.PROCESS_IN_PLACE);
    final DuplicateFilterTask task = new DuplicateFilterTask(project, data.flist(), params, null,
        Instant.now());

    final long tRun = System.nanoTime();
    task.run();
    final long runMs = logMillis("DuplicateFilterTask NEW_AVERAGE / PROCESS_IN_PLACE", tRun);

    assertEquals(TaskStatus.FINISHED, task.getStatus());
    assertEquals(data.uniqueRows(), data.flist().getNumberOfRows(),
        "all duplicated rows must be merged away");
    assertTrue(runMs >= 0);
  }

  @Test
  void newAverage_withListCopy() {
    final Data data = buildAlignedList();

    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFeatureList(data.flist());

    final ParameterSet params = createParameters(FilterMode.NEW_AVERAGE,
        OriginalFeatureListOption.KEEP);
    final DuplicateFilterTask task = new DuplicateFilterTask(project, data.flist(), params, null,
        Instant.now());

    final long tRun = System.nanoTime();
    task.run();
    logMillis("DuplicateFilterTask NEW_AVERAGE / KEEP (includes createCopy)", tRun);

    assertEquals(TaskStatus.FINISHED, task.getStatus());
  }

  /**
   * Isolates the cost of the row bindings that are triggered while merging duplicates.
   */
  @Test
  void isolate_addFeatureRowBindingCost() {
    final Data data = buildAlignedList();
    final ModularFeatureList flist = data.flist();
    final ModularFeatureListRow row = (ModularFeatureListRow) flist.getRow(0);
    final RawDataFile raw = flist.getRawDataFile(0);
    final ModularFeature feature = new ModularFeature(flist, raw, FeatureStatus.DETECTED);
    feature.setMZ(row.getAverageMZ());
    feature.setRT(row.getAverageRT());
    feature.setHeight(1e6f);
    feature.setArea(1e6f);

    final int n = 50;
    long t = System.nanoTime();
    for (int i = 0; i < n; i++) {
      flist.applyRowBindings(row);
    }
    final long ms = (System.nanoTime() - t) / 1_000_000;
    logger.info(
        "applyRowBindings on a row with %d features: %d x -> %d ms (%.2f ms each)".formatted(
            row.getNumberOfFeatures(), n, ms, ms / (double) n));
  }

  @Test
  void isolate_setRowsApplySort() {
    final Data data = buildAlignedList();
    final ModularFeatureListRow[] rows = data.flist().getRows()
        .toArray(ModularFeatureListRow[]::new);
    final long t = System.nanoTime();
    data.flist().setRowsApplySort(rows);
    logMillis("setRowsApplySort (all rows)", t);
  }

  @Test
  void isolate_createCopy() {
    final Data data = buildAlignedList();
    final long t = System.nanoTime();
    data.flist().createCopy("copy", null, false);
    logMillis("createCopy", t);
  }

  // ---------------------------------------------------------------- helpers

  private static long logMillis(final @NotNull String what, final long startNanos) {
    final long ms = (System.nanoTime() - startNanos) / 1_000_000;
    logger.info("%s [%d files x %d rows]: %d ms".formatted(what, N_FILES, N_ROWS, ms));
    return ms;
  }

  private static @NotNull ParameterSet createParameters(final @NotNull FilterMode mode,
      final @NotNull OriginalFeatureListOption handleOriginal) {
    final ParameterSet params = new DuplicateFilterParameters().cloneParameterSet();
    params.setParameter(DuplicateFilterParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS));
    params.setParameter(DuplicateFilterParameters.suffix, "dup");
    params.setParameter(DuplicateFilterParameters.filterMode, mode);
    params.setParameter(DuplicateFilterParameters.mzDifferenceMax, MZ_TOL);
    params.setParameter(DuplicateFilterParameters.rtDifferenceMax, RT_TOL);
    params.setParameter(DuplicateFilterParameters.mobilityDifferenceMax, false);
    params.setParameter(DuplicateFilterParameters.requireSameIdentification, false);
    params.setParameter(DuplicateFilterParameters.handleOriginal, handleOriginal);
    return params;
  }

  /**
   * Builds an aligned feature list: {@link #N_ROWS} rows over {@link #N_FILES} samples where every
   * {@link #DUPLICATE_EVERY}-th row has a near duplicate partner within the wizard tolerances.
   */
  private static @NotNull Data buildAlignedList() {
    final List<RawDataFile> files = new ArrayList<>(N_FILES);
    for (int i = 0; i < N_FILES; i++) {
      files.add(new RawDataFileImpl("sample_%04d".formatted(i), null, null));
    }

    final ModularFeatureList flist = new ModularFeatureList("aligned", null, N_ROWS,
        N_ROWS * N_FILES, files);

    final Random random = new Random(42);
    int id = 1;
    int uniqueRows = 0;
    for (int r = 0; r < N_ROWS; r++) {
      // widely spaced so that non duplicates never fall within tolerance
      final double mz = 100d + r * 0.5;
      final float rt = 1f + (r % 100) * 0.1f;
      addRow(flist, id++, files, mz, rt, random);
      uniqueRows++;

      if (r % DUPLICATE_EVERY == 0) {
        // near duplicate: within 0.0008 m/z and 0.02 min
        addRow(flist, id++, files, mz + 0.0003, rt + 0.005f, random);
      }
    }
    return new Data(flist, uniqueRows);
  }

  private static void addRow(final @NotNull ModularFeatureList flist, final int id,
      final @NotNull List<RawDataFile> files, final double mz, final float rt,
      final @NotNull Random random) {
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, id);
    for (final RawDataFile raw : files) {
      if (random.nextDouble() > FILL_RATE) {
        continue;
      }
      final ModularFeature f = new ModularFeature(flist, raw,
          random.nextDouble() < 0.8 ? FeatureStatus.DETECTED : FeatureStatus.ESTIMATED);
      f.setMZ(mz + (random.nextDouble() - 0.5) * 1e-4);
      f.setRT(rt + (float) (random.nextDouble() - 0.5) * 0.002f);
      f.setHeight((float) (1e5 + random.nextDouble() * 1e6));
      f.setArea((float) (1e6 + random.nextDouble() * 1e7));
      // do not trigger row bindings per feature - applied once below
      row.addFeature(raw, f, false);
    }
    row.applyRowBindings();
    flist.addRow(row);
  }

  private record Data(@NotNull ModularFeatureList flist, int uniqueRows) {

  }
}

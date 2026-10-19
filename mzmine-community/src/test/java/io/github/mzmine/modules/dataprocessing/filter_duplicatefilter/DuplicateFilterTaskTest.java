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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
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
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Behaviour of the {@link DuplicateFilterTask} consensus row creation. Guards that features are
 * merged by {@link FeatureStatus} / height and that the row bindings (averages, max height) of the
 * consensus row are up to date - the merge itself adds features without applying row bindings per
 * feature for performance reasons.
 */
class DuplicateFilterTaskTest {

  // wizard defaults
  private static final MZTolerance MZ_TOL = new MZTolerance(0.0008, 1.5);
  private static final RTTolerance RT_TOL = new RTTolerance(0.02f, Unit.MINUTES);

  @Test
  void newAverage_mergesDuplicateAndPrefersDetectedAndHigherFeature() {
    final RawDataFileImpl a = new RawDataFileImpl("sample_a", null, null);
    final RawDataFileImpl b = new RawDataFileImpl("sample_b", null, null);
    final RawDataFileImpl c = new RawDataFileImpl("sample_c", null, null);
    final ModularFeatureList flist = new ModularFeatureList("aligned", null, List.of(a, b, c));

    // row 1: a = DETECTED low, b = ESTIMATED, c = DETECTED high
    final ModularFeatureListRow row1 = addRow(flist, 1, 200d, 5f,
        new FeatureSpec(a, FeatureStatus.DETECTED, 100f),
        new FeatureSpec(b, FeatureStatus.ESTIMATED, 10f),
        new FeatureSpec(c, FeatureStatus.DETECTED, 900f));
    // row 2: duplicate within tolerance. a = DETECTED high (wins), b = DETECTED (wins over
    // ESTIMATED), c = DETECTED low (loses)
    addRow(flist, 2, 200.0003d, 5.005f, new FeatureSpec(a, FeatureStatus.DETECTED, 500f),
        new FeatureSpec(b, FeatureStatus.DETECTED, 20f),
        new FeatureSpec(c, FeatureStatus.DETECTED, 300f));
    // row 3: far away, must survive untouched
    addRow(flist, 3, 300d, 5f, new FeatureSpec(a, FeatureStatus.DETECTED, 42f), null, null);

    runFilter(flist, FilterMode.NEW_AVERAGE);

    assertEquals(2, flist.getNumberOfRows(), "one duplicate row must be removed");

    final FeatureListRow merged = rowById(flist, 1);
    assertNotNull(merged);
    assertEquals(500f, merged.getFeature(a).getHeight(), "higher DETECTED feature must win");
    assertEquals(20f, merged.getFeature(b).getHeight(), "DETECTED must replace ESTIMATED");
    assertEquals(FeatureStatus.DETECTED, merged.getFeature(b).getFeatureStatus());
    assertEquals(900f, merged.getFeature(c).getHeight(), "lower DETECTED feature must not win");

    // row bindings must be up to date after the merge
    assertEquals(900f, merged.getMaxHeight(), "max height binding must reflect merged features");
    final double expectedMz =
        (merged.getFeature(a).getMZ() + merged.getFeature(b).getMZ() + merged.getFeature(c).getMZ())
            / 3d;
    assertEquals(expectedMz, merged.getAverageMZ(), 1e-9,
        "average m/z binding must be recalculated");

    assertNotNull(rowById(flist, 3), "non duplicate row must survive");
    assertNull(rowById(flist, 2), "duplicate row must be gone");
  }

  @Test
  void newAverage_keepsRowsOutsideTolerance() {
    final RawDataFileImpl a = new RawDataFileImpl("sample_a", null, null);
    final ModularFeatureList flist = new ModularFeatureList("aligned", null, List.of(a));

    addRow(flist, 1, 200d, 5f, new FeatureSpec(a, FeatureStatus.DETECTED, 100f), null, null);
    // outside m/z tolerance
    addRow(flist, 2, 200.01d, 5f, new FeatureSpec(a, FeatureStatus.DETECTED, 100f), null, null);
    // inside m/z but outside RT tolerance
    addRow(flist, 3, 200.0002d, 5.5f, new FeatureSpec(a, FeatureStatus.DETECTED, 100f), null, null);

    runFilter(flist, FilterMode.NEW_AVERAGE);

    assertEquals(3, flist.getNumberOfRows());
  }

  @Test
  void oldAverage_keepsHighestAreaRow() {
    final RawDataFileImpl a = new RawDataFileImpl("sample_a", null, null);
    final ModularFeatureList flist = new ModularFeatureList("aligned", null, List.of(a));

    addRow(flist, 1, 200d, 5f, new FeatureSpec(a, FeatureStatus.DETECTED, 100f), null, null);
    addRow(flist, 2, 200.0003d, 5.005f, new FeatureSpec(a, FeatureStatus.DETECTED, 900f), null,
        null);

    runFilter(flist, FilterMode.OLD_AVERAGE);

    assertEquals(1, flist.getNumberOfRows());
    // OLD_AVERAGE sorts by area descending and keeps the first row
    assertEquals(900f, flist.getRow(0).getMaxHeight());
  }

  // ---------------------------------------------------------------- helpers

  private static void runFilter(final @NotNull ModularFeatureList flist,
      final @NotNull FilterMode mode) {
    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFeatureList(flist);

    final ParameterSet params = new DuplicateFilterParameters().cloneParameterSet();
    params.setParameter(DuplicateFilterParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS));
    params.setParameter(DuplicateFilterParameters.suffix, "dup");
    params.setParameter(DuplicateFilterParameters.filterMode, mode);
    params.setParameter(DuplicateFilterParameters.mzDifferenceMax, MZ_TOL);
    params.setParameter(DuplicateFilterParameters.rtDifferenceMax, RT_TOL);
    params.setParameter(DuplicateFilterParameters.mobilityDifferenceMax, false);
    params.setParameter(DuplicateFilterParameters.requireSameIdentification, false);
    params.setParameter(DuplicateFilterParameters.handleOriginal,
        OriginalFeatureListOption.PROCESS_IN_PLACE);

    final DuplicateFilterTask task = new DuplicateFilterTask(project, flist, params, null,
        Instant.now());
    task.run();
    assertEquals(TaskStatus.FINISHED, task.getStatus());
  }

  private static @Nullable FeatureListRow rowById(final @NotNull ModularFeatureList flist,
      final int id) {
    return flist.getRows().stream().filter(r -> r.getID() == id).findFirst().orElse(null);
  }

  private static @NotNull ModularFeatureListRow addRow(final @NotNull ModularFeatureList flist,
      final int id, final double mz, final float rt, final @Nullable FeatureSpec... specs) {
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, id);
    for (final FeatureSpec spec : specs) {
      if (spec == null) {
        continue;
      }
      final ModularFeature f = new ModularFeature(flist, spec.raw(), spec.status());
      f.setMZ(mz);
      f.setRT(rt);
      f.setHeight(spec.height());
      f.setArea(spec.height() * 10f);
      row.addFeature(spec.raw(), f, false);
    }
    row.applyRowBindings();
    flist.addRow(row);
    return row;
  }

  private record FeatureSpec(@NotNull RawDataFile raw, @NotNull FeatureStatus status,
                             float height) {

  }
}

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

package io.github.mzmine.modules.dataprocessing.filter_featurelistpreferences;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.preferences.FeatureListPreferences;
import io.github.mzmine.datamodel.features.types.TagDataType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeRanking;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.parameters.parametertypes.combowithinput.DefaultOffCustomOption;
import io.github.mzmine.parameters.parametertypes.combowithinput.DefaultOffCustomValue;
import java.time.Instant;
import java.util.BitSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FeatureListPreferencesTaskTest {

  private static @NotNull ModularFeatureList createFeatureListWithTags(final int... tagIndexes) {
    final ModularFeatureList featureList = new ModularFeatureList("test", null);
    final ModularFeatureListRow row = new ModularFeatureListRow(featureList, 1);
    row.set(TagDataType.class, tags(tagIndexes));
    featureList.addRow(row);
    return featureList;
  }

  private static void applyTagLabels(@NotNull final ModularFeatureList featureList,
      @NotNull final List<String> labels) {
    final FeatureListPreferencesParameters parameters = FeatureListPreferencesParameters.keepAllAsIs(
        featureList.getPreferences());
    parameters.setParameter(FeatureListPreferencesParameters.tagLabels,
        new DefaultOffCustomValue<>(DefaultOffCustomOption.CUSTOM, labels));

    final FeatureListPreferencesTask task = new FeatureListPreferencesTask(null, Instant.now(),
        parameters, FeatureListPreferencesModule.class, featureList);
    task.process();
  }

  private static @NotNull BitSet tags(final int... indexes) {
    final BitSet tags = new BitSet();
    for (final int index : indexes) {
      tags.set(index);
    }
    return tags;
  }

  @Test
  void testRemovingLabelsTrimsRowTagValues() {
    final ModularFeatureList featureList = createFeatureListWithTags(0, 2, 4, 8);
    applyTagLabels(featureList, List.of("First", "Second", "Third"));

    final BitSet expected = tags(0, 2);
    Assertions.assertEquals(expected, featureList.getRow(0).get(TagDataType.class));
  }

  @Test
  void testAddingLabelsPreservesExistingRowTagValues() {
    final ModularFeatureList featureList = createFeatureListWithTags(0, 4, 8);
    featureList.setPreferences(
        new FeatureListPreferences(SampleTypeFilter.qc(), IonTypeRanking.createDefault(),
            List.of("First", "Second", "Third")));

    applyTagLabels(featureList, List.of("First", "Second", "Third", "Fourth", "Fifth", "Sixth"));

    Assertions.assertEquals(tags(0, 4, 8), featureList.getRow(0).get(TagDataType.class));
  }
}

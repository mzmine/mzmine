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

package io.github.mzmine.modules.dataprocessing.filter_rowsfilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.DoubleBondPositionLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.junit.jupiter.api.Test;

class RowsFilterTaskTest {

  @Test
  void taskKeepsRowsAtOrAboveSelectedLipidLevel() {
    final ModularFeatureList featureList = new ModularFeatureList("lipids", null, List.of());
    addLipidRow(featureList, 1, LipidAnnotationLevel.SPECIES_LEVEL);
    addLipidRow(featureList, 2, LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL);
    addLipidRow(featureList, 3, LipidAnnotationLevel.DOUBLE_BOND_POSITION_LEVEL);

    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFeatureList(featureList);
    final ParameterSet parameters = RowsFilterParameters.createDefaultAllOff();
    parameters.setParameter(RowsFilterParameters.MIN_LIPID_ANNOTATION_LEVEL, true,
        LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL);
    parameters.setParameter(RowsFilterParameters.handleOriginal,
        OriginalFeatureListOption.PROCESS_IN_PLACE);

    final RowsFilterTask task = new RowsFilterTask(project, featureList, parameters, null,
        Instant.now());
    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus());
    assertEquals(List.of(2, 3), featureList.getRows().stream().map(FeatureListRow::getID).toList());
  }

  @Test
  void lipidLevelFilterRejectsRowsWithoutLipidAnnotations() {
    final FeatureListRow row = mock(FeatureListRow.class);
    when(row.getLipidMatches()).thenReturn(List.of());

    assertFalse(RowsFilterTask.hasLipidAnnotationAtLeast(row,
        LipidAnnotationLevel.SPECIES_LEVEL));
  }

  @Test
  void molecularSpeciesMinimumAcceptsMoreSpecificPositionEvidence() {
    final FeatureListRow molecularSpeciesRow = rowWithLipidLevels(
        LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL);
    final FeatureListRow positionRow = rowWithLipidLevels(
        LipidAnnotationLevel.DOUBLE_BOND_POSITION_LEVEL);

    assertTrue(RowsFilterTask.hasLipidAnnotationAtLeast(molecularSpeciesRow,
        LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL));
    assertTrue(RowsFilterTask.hasLipidAnnotationAtLeast(positionRow,
        LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL));
    assertFalse(RowsFilterTask.hasLipidAnnotationAtLeast(molecularSpeciesRow,
        LipidAnnotationLevel.DOUBLE_BOND_POSITION_LEVEL));
  }

  @Test
  void lipidLevelFilterUsesEvidenceLevelInsteadOfPreferredDisplayLevel() {
    final FeatureListRow row = rowWithLipidLevels(
        LipidAnnotationLevel.DOUBLE_BOND_POSITION_LEVEL);
    final MatchedLipid match = row.getLipidMatches().getFirst();
    when(match.getPreferredAnnotationLevel()).thenReturn(
        LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL);

    assertTrue(RowsFilterTask.hasLipidAnnotationAtLeast(row,
        LipidAnnotationLevel.DOUBLE_BOND_POSITION_LEVEL));
  }

  private static @NotNull FeatureListRow rowWithLipidLevels(
      final @NotNull LipidAnnotationLevel... levels) {
    final FeatureListRow row = mock(FeatureListRow.class);
    final List<MatchedLipid> matches = matchedLipids(levels);
    when(row.getLipidMatches()).thenReturn(matches);
    return row;
  }

  private static @NotNull ModularFeatureListRow addLipidRow(
      final @NotNull ModularFeatureList featureList, final int id,
      final @NotNull LipidAnnotationLevel level) {
    final ModularFeatureListRow row = new ModularFeatureListRow(featureList, id);
    row.set(MZType.class, (double) id);
    row.setLipidAnnotations(matchedLipids(level));
    featureList.addRow(row);
    return row;
  }

  private static @NotNull List<MatchedLipid> matchedLipids(
      final @NotNull LipidAnnotationLevel... levels) {
    return Arrays.stream(levels).map(level -> {
      final IMolecularFormula formula = mock(IMolecularFormula.class);
      final ILipidAnnotation annotation = switch (level) {
        case SPECIES_LEVEL -> new SpeciesLevelAnnotation(LipidClasses.FREEFATTYACIDS, "FA 18:1",
            formula, 18, 1, 0);
        case MOLECULAR_SPECIES_LEVEL -> new MolecularSpeciesLevelAnnotation(
            LipidClasses.FREEFATTYACIDS, "FA 18:1", formula, List.of());
        case DOUBLE_BOND_POSITION_LEVEL -> new DoubleBondPositionLevelAnnotation(
            LipidClasses.FREEFATTYACIDS, "FA 18:1(9)", "FA 18:1", formula, List.of());
      };
      final MatchedLipid match = mock(MatchedLipid.class);
      when(match.getLipidAnnotation()).thenReturn(annotation);
      return match;
    }).toList();
  }
}

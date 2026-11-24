/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */

package io.github.mzmine.modules.dataprocessing.filter_lipidpreferredlevel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.PreferredAnnotationType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.DoubleBondPositionLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IMolecularFormula;

class SetLipidAnnotationLevelTaskTest {

  @Test
  void replacesMatchesAndUsesPreferredNameForEveryRow() {
    final ModularFeatureList featureList = new ModularFeatureList("lipids", null, List.of());
    final ModularFeatureListRow annotatedRow = new ModularFeatureListRow(featureList, 1);
    final MatchedLipid positionMatch = positionMatch();
    positionMatch.setComment("reviewed");
    positionMatch.setOverallQualityScore(0.85f);
    annotatedRow.setLipidAnnotations(List.of(positionMatch));
    // Project loading reconstructs the explicitly preferred annotation separately from the list.
    annotatedRow.set(PreferredAnnotationType.class, positionMatch());
    featureList.addRow(annotatedRow);
    featureList.addRow(new ModularFeatureListRow(featureList, 2));

    final ParameterSet parameters = new SetLipidAnnotationLevelParameters();
    parameters.setParameter(SetLipidAnnotationLevelParameters.level,
        LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL);
    final SetLipidAnnotationLevelTask task = new SetLipidAnnotationLevelTask(null, Instant.EPOCH,
        parameters, SetLipidAnnotationLevelModule.class, featureList);

    task.run();

    final MatchedLipid updatedMatch = annotatedRow.getLipidMatches().getFirst();
    assertEquals(TaskStatus.FINISHED, task.getStatus());
    assertEquals(1d, task.getFinishedPercentage());
    assertNotSame(positionMatch, updatedMatch);
    assertEquals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL,
        updatedMatch.getPreferredAnnotationLevel());
    assertEquals("PC 18:0_18:2", updatedMatch.toString());
    assertEquals("PC 18:0_18:2", updatedMatch.getCompoundName());
    assertEquals(updatedMatch, annotatedRow.getPreferredAnnotation());
    assertEquals("PC 18:0_18:2", annotatedRow.getPreferredAnnotationName());
    assertEquals("reviewed", updatedMatch.getComment());
    assertEquals(0.85f, updatedMatch.getOverallQualityScore());
    assertEquals(LipidAnnotationLevel.DOUBLE_BOND_POSITION_LEVEL,
        positionMatch.getPreferredAnnotationLevel());
  }

  private static @NotNull MatchedLipid positionMatch() {
    final DoubleBondPositionLevelAnnotation annotation = new DoubleBondPositionLevelAnnotation(
        LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES, "PC 18:0_18:2(9,12)", "PC 18:0_18:2",
        mock(IMolecularFormula.class), List.of());
    return new MatchedLipid(annotation, 786.6d, IonizationType.POSITIVE_HYDROGEN, Set.of(), 0.9d);
  }
}

/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class LipidMatchListType extends ListWithSubsType<MatchedLipid> implements AnnotationType {

  private static final Map<Class<? extends DataType>, Function<MatchedLipid, Object>> mapper =
      Map.ofEntries(
          createEntry(LipidMatchListType.class, match -> match),
          createEntry(IonAdductType.class, match -> match.getIonizationType().getAdductName()),
          createEntry(FormulaType.class, match -> MolecularFormulaManipulator
              .getString(match.getLipidAnnotation().getMolecularFormula())),
          createEntry(CommentType.class,
              match -> match.getComment() != null ? match.getComment() : ""),
          createEntry(LipidAnnotationMsMsScoreType.class, match -> match.getMsMsScore()),
          createEntry(LipidSpectrumType.class, match -> null), // ???
          createEntry(LipidMsOneErrorType.class, match -> {
            // calc ppm error?
            double exactMass = getExactMass(match);
            return ((exactMass - match.getAccurateMz()) / exactMass) * 1000000;
          })
      );

  private static final List<DataType> subTypes = List.of(//
      new LipidMatchListType(), //
      new IonAdductType(), //
      new FormulaType(), //
      new CommentType(), //
      new LipidMsOneErrorType(), //
      new LipidAnnotationMsMsScoreType(), //
      new LipidSpectrumType());

  private static double getExactMass(MatchedLipid match) {
    return MolecularFormulaManipulator.getMass(match.getLipidAnnotation().getMolecularFormula(),
        AtomContainerManipulator.MonoIsotopic) + match.getIonizationType().getAddedMass();
  }

  @Override
  protected Map<Class<? extends DataType>, Function<MatchedLipid, Object>> getMapper() {
    return mapper;
  }

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "lipid_annotations";
  }

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Lipid Annotation";
  }

}

/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.tools.siriusapi;

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CommentType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ALogPType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireClassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireSubclassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireSuperclassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierClassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierPathwayType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierSuperclassType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.scores.CsiScoreType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.FormulaUtils;
import io.sirius.ms.sdk.SiriusSDK;
import io.sirius.ms.sdk.SiriusSDKUtils;
import io.sirius.ms.sdk.model.AlignedFeature;
import io.sirius.ms.sdk.model.CompoundClass;
import io.sirius.ms.sdk.model.CompoundClasses;
import io.sirius.ms.sdk.model.ProjectInfo;
import io.sirius.ms.sdk.model.StructureCandidateFormula;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class SiriusToMzmine {

  private static final Logger logger = Logger.getLogger(SiriusToMzmine.class.getName());

  public static @Nullable CompoundDBAnnotation structureCandidateToMzmine(
      @Nullable StructureCandidateFormula structure, SiriusSDK sirius, ProjectInfo projectInfo,
      String id) {
    if (structure == null) {
      return null;
    }

    final NumberFormats formats = ConfigService.getGuiFormats();

    final CompoundDBAnnotation annotation = new SimpleCompoundDBAnnotation();

    final IonType ionType = IonTypeParser.parse(structure.getAdduct());
    annotation.put(IonTypeType.class, ionType);
    final IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormula(
        structure.getMolecularFormula());
    final double neutralMass = FormulaUtils.getMonoisotopicMass(formula);
    final double mz = ionType.getMZ(neutralMass);

    annotation.putIfNotNull(CompoundNameType.class, structure.getStructureName());
    annotation.putIfNotNull(FormulaType.class, structure.getMolecularFormula());
    annotation.putIfNotNull(SmilesStructureType.class, structure.getSmiles());
    annotation.putIfNotNull(InChIKeyStructureType.class, structure.getInchiKey());
    annotation.putIfNotNull(PrecursorMZType.class, mz);
    annotation.putIfNotNull(NeutralMassType.class, neutralMass);
    annotation.put(CommentType.class,
        "Imported from Sirius. CSI score: %s".formatted(formats.score(structure.getCsiScore())));
    annotation.putIfNotNull(CsiScoreType.class, structure.getCsiScore().floatValue());
//    annotation.putIfNotNull(DatabaseNameType.class,
//        structure.getDbLinks().isEmpty() ? null : structure.getDbLinks().getFirst().getName());
    annotation.putIfNotNull(ALogPType.class, structure.getXlogP().floatValue());

    transferClassyfireAnnotations(structure, sirius, projectInfo, id, annotation);

    return annotation;
  }

  /**
   * Reads structure classes from classifier for a specific {@link StructureCandidateFormula} to add
   * it to a {@link CompoundDBAnnotation}
   *
   * @param structure the structure to query
   * @param featureId the sirius {@link AlignedFeature#getAlignedFeatureId()}
   */
  private static void transferClassyfireAnnotations(@NotNull StructureCandidateFormula structure,
      @NotNull final SiriusSDK sirius, @NotNull final ProjectInfo projectInfo,
      @NotNull final String featureId, @NotNull CompoundDBAnnotation annotation) {

    final String formulaId = structure.getFormulaId();
    CompoundClasses bestCompClasses = sirius.features()
        .getBestMatchingCompoundClasses(projectInfo.getProjectId(), featureId, formulaId);

    if (bestCompClasses == null) {
      return;
    }

    annotation.putIfNotNull(NPClassifierPathwayType.class,
        bestCompClasses.getNpcPathway().getName());
    annotation.putIfNotNull(NPClassifierSuperclassType.class,
        bestCompClasses.getNpcSuperclass().getName());
    annotation.putIfNotNull(NPClassifierClassType.class, bestCompClasses.getNpcClass().getName());

    final List<CompoundClass> classyFireLineage = bestCompClasses.getClassyFireLineage();
    if (bestCompClasses.getClassyFireLineage() != null) {
      for (CompoundClass compoundClass : classyFireLineage) {
        switch (compoundClass.getLevel()) {
          case "Superclass" ->
              annotation.putIfNotNull(ClassyFireSuperclassType.class, compoundClass.getName());
          case "Class" ->
              annotation.putIfNotNull(ClassyFireClassType.class, compoundClass.getName());
          case "Subclass" ->
              annotation.putIfNotNull(ClassyFireSubclassType.class, compoundClass.getName());
//          case "Level-5" -> annotation.putIfNotNull(ClassyFireSubclassType.class, compoundClass.getName());
          case null, default -> {
          }
        }
      }
    }
  }

  public static String alignedFeatureToString(AlignedFeature alignedFeature) {
    final NumberFormats formats = ConfigService.getGuiFormats();

    return "AlignedFeature{exId=%s, id=%s, mz=%s, rt=%s}".formatted(
        alignedFeature.getExternalFeatureId(), alignedFeature.getAlignedFeatureId(),
        formats.mz(alignedFeature.getIonMass()),
        formats.rt(Objects.requireNonNullElse(alignedFeature.getRtApexSeconds(), 0d) / 60f));
  }
}

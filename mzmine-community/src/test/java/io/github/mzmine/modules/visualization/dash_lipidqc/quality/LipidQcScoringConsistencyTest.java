package io.github.mzmine.modules.visualization.dash_lipidqc.quality;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.PreferredAnnotationType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnalysisType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationModule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidQcWeightParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidFactory;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickReviewMode;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LipidQcScoringConsistencyTest {

  /**
   * Checks the stored/exported score, dashboard card and outlier warning against independent
   * expected values, including missing isotope evidence and disabled MS/MS scoring.
   *
   * @param customWeights whether custom weights are enabled
   * @param includeMs2    whether MS/MS scoring is enabled
   * @param expected      independently calculated weighted quality
   */
  @ParameterizedTest
  @CsvSource({"true, true, 0.2", "true, false, 1.0", "false, true, 0.2571428571428571",
      "false, false, 0.6"})
  void dashboardAndWarningUseLatestAnnotationSettings(final boolean customWeights,
      final boolean includeMs2, final double expected) {
    final var featureList = new ModularFeatureList("Lipid QC", null, List.of());
    final var row = new ModularFeatureListRow(featureList, 1);
    final var annotation = new LipidFactory().buildSpeciesLevelLipid(
        LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES, 34, 1, 0);
    final double mz = FormulaUtils.getMonoisotopicMass(annotation.getMolecularFormula())
        + IonizationType.POSITIVE_HYDROGEN.getAddedMass();
    final var match = new MatchedLipid(annotation, mz, IonizationType.POSITIVE_HYDROGEN, Set.of(),
        0d);
    row.setAverageMZ(mz);
    row.set(LipidMatchListType.class, List.of(match));
    row.set(PreferredAnnotationType.class, match);
    featureList.addRow(row);

    final var parameters = new LipidAnnotationParameters();
    parameters.setParameter(LipidAnnotationParameters.lipidAnalysisType,
        LipidAnalysisType.DIRECT_INFUSION);
    parameters.setParameter(LipidAnnotationParameters.mzTolerance, new MZTolerance(0.005, 10d));
    parameters.setParameter(LipidAnnotationParameters.searchForMSMSFragments, !includeMs2);
    featureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(new LipidAnnotationModule(), parameters, Instant.EPOCH));

    parameters.setParameter(LipidAnnotationParameters.searchForMSMSFragments, includeMs2);
    parameters.setParameter(LipidAnnotationParameters.customQcWeights, customWeights);
    final var weights = parameters.getParameter(LipidAnnotationParameters.customQcWeights)
        .getEmbeddedParameters();
    weights.setParameter(LipidQcWeightParameters.ms1Weight, 20);
    weights.setParameter(LipidQcWeightParameters.ms2Weight, 80);
    weights.setParameter(LipidQcWeightParameters.adductMatchWeight, 0);
    weights.setParameter(LipidQcWeightParameters.adductMismatchWeight, 0);
    weights.setParameter(LipidQcWeightParameters.adductNoIonIdentityWeight, 0);
    weights.setParameter(LipidQcWeightParameters.isotopeWeight, 0);
    weights.setParameter(LipidQcWeightParameters.interferenceWeight, 0);
    featureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(new LipidAnnotationModule(), parameters,
            Instant.EPOCH.plusSeconds(1)));
    LipidQcScoringUtils.rescoreOverallQualityScores(featureList);
    Assertions.assertNotNull(match.getScore());
    Assertions.assertEquals(expected, match.getScore(), 1e-6);

    final var model = new AnnotationQualityModel();
    model.setFeatureList(featureList);
    model.setRow(row);
    model.setRetentionTimeAnalysisEnabled(false);
    model.setKendrickReviewMode(KendrickReviewMode.POTENTIAL_FALSE_POSITIVE);
    final var task = new QualityComputationTask(model);
    task.process();
    task.updateGuiModel();
    final var result = model.getQualityResult();
    Assertions.assertNotNull(result);
    Assertions.assertEquals(expected, result.qualityCards().getFirst().overall(), 1e-9);
    if (expected < 0.5d) {
      Assertions.assertEquals("overall quality %.0f%% is below 50%%".formatted(expected * 100d),
          result.falsePositiveReason());
    } else {
      Assertions.assertNull(result.falsePositiveReason());
    }
  }
}

/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules;

import static io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.clampToUnit;
import static io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.computeMs1Score;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.fragmentation.ILipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.fragmentation.LipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipidStatus;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.molecular_species.GlyceroAndPhosphoMolecularSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.molecular_species.IMolecularSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.molecular_species.SphingoMolecularSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.molecular_species.SterolMolecularSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.FattyAcylSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.GlyceroAndGlycerophosphoSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.ISpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.SphingolipidSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.SterolSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidIon;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.ComponentWeights;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidAnnotationResolver;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidFactory;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.scans.FragmentScanSelection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class LipidAnnotationUtils {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  public static List<LipidIon> buildLipidDatabase(ILipidClass[] selectedLipids, int minChainLength,
      int maxChainLength, int minDoubleBonds, int maxDoubleBonds, boolean onlySearchForEvenChains,
      IonizationType[] ionizationTypesToIgnore, Set<PolarityType> polarityTypes) {

    List<LipidIon> lipidDatabase = new ArrayList<>();

    // add selected lipids
    buildLipidCombinations(lipidDatabase, selectedLipids, minChainLength, maxChainLength,
        minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains, ionizationTypesToIgnore,
        polarityTypes);

    return lipidDatabase;
  }

  private static void buildLipidCombinations(List<LipidIon> lipidDatabase,
      ILipidClass[] lipidClasses, int minChainLength, int maxChainLength, int minDoubleBonds,
      int maxDoubleBonds, boolean onlySearchForEvenChains, IonizationType[] ionizationTypesToIgnore,
      Set<PolarityType> polarityTypes) {
    // Try all combinations of fatty acid lengths and double bonds
    for (ILipidClass lipidClass : lipidClasses) {

      // TODO starting point to extend for better oxidized lipid support
      int numberOfAdditionalOxygens = 0;
      int minTotalChainLength = minChainLength * lipidClass.getChainTypes().length;
      int maxTotalChainLength = maxChainLength * lipidClass.getChainTypes().length;
      int minTotalDoubleBonds = minDoubleBonds * lipidClass.getChainTypes().length;
      int maxTotalDoubleBonds = maxDoubleBonds * lipidClass.getChainTypes().length;
      for (int chainLength = minTotalChainLength; chainLength <= maxTotalChainLength;
          chainLength++) {
        if (onlySearchForEvenChains && chainLength % 2 != 0) {
          continue;
        }
        for (int chainDoubleBonds = minTotalDoubleBonds; chainDoubleBonds <= maxTotalDoubleBonds;
            chainDoubleBonds++) {

          if (chainLength / 2 < chainDoubleBonds || chainLength == 0) {
            continue;
          }

          // Prepare a lipid instance
          ILipidAnnotation lipid = LIPID_FACTORY.buildSpeciesLevelLipid(lipidClass, chainLength,
              chainDoubleBonds, numberOfAdditionalOxygens);
          if (lipid != null) {
            Set<IonizationType> ionizationTypeList = new HashSet<>();
            LipidFragmentationRule[] fragmentationRules = lipid.getLipidClass()
                .getFragmentationRules();
            for (LipidFragmentationRule fragmentationRule : fragmentationRules) {
              if (ionizationTypesToIgnore != null) {
                if (!Arrays.stream(ionizationTypesToIgnore).toList()
                    .contains(fragmentationRule.getIonizationType())) {
                  ionizationTypeList.add(fragmentationRule.getIonizationType());
                }
              } else {
                ionizationTypeList.add(fragmentationRule.getIonizationType());
              }
            }
            for (IonizationType ionization : ionizationTypeList) {
              if (polarityTypes.contains(ionization.getPolarity())) {
                double lipidIonMass = FormulaUtils.getMonoisotopicMass(lipid.getMolecularFormula())
                    + ionization.getAddedMass();
                lipidDatabase.add(new LipidIon(lipid, ionization, lipidIonMass));
              }
            }
          }
        }
      }
    }
  }

  public static void findPossibleLipid(final @NotNull LipidIon lipidIon,
      final @NotNull FeatureListRow row, final @NotNull ParameterSet parameters,
      final @NotNull MZTolerance mzTolerance, final @NotNull MZTolerance mzToleranceMS2,
      final boolean searchForMSMSFragments, final boolean keepUnconfirmedAnnotations,
      final @NotNull LipidCategories lipidCategory,
      final @NotNull FragmentScanSelection scanMergeSelect,
      final @NotNull Set<MatchedLipid> possibleRowAnnotations) {

    if (Objects.equals(row.getRepresentativePolarity(), lipidIon.ionizationType().getPolarity())) {
      Range<Double> mzTolRange12C = mzTolerance.getToleranceRange(row.getAverageMZ());

      // MS1 check
      if (mzTolRange12C.contains(lipidIon.mz())) {

        // If search for MSMS fragments is selected search for fragments
        if (searchForMSMSFragments) {
          possibleRowAnnotations.addAll(
              searchMsmsFragments(row, lipidIon.ionizationType(), lipidIon.lipidAnnotation(),
                  parameters, mzTolerance, mzToleranceMS2, keepUnconfirmedAnnotations, lipidCategory,
                  scanMergeSelect));
        } else {

          // make MS1 annotation
          final MatchedLipid matchedLipid = new MatchedLipid(lipidIon.lipidAnnotation(),
              row.getAverageMZ(), lipidIon.ionizationType(), null, 0.0,
              MatchedLipidStatus.UNCONFIRMED);
          possibleRowAnnotations.add(matchedLipid);
        }
      }
    }
  }

  /**
   * This method searches for MS/MS fragments. A mass list for MS2 scans will be used if present.
   */
  private static @NotNull Set<MatchedLipid> searchMsmsFragments(final @NotNull FeatureListRow row,
      final @NotNull IonizationType ionization, final @NotNull ILipidAnnotation lipid,
      final @NotNull ParameterSet parameters, final @NotNull MZTolerance mzToleranceMS1,
      final @NotNull MZTolerance mzToleranceMS2,
      final boolean keepUnconfirmedAnnotations, final @NotNull LipidCategories lipidCategory,
      final @NotNull FragmentScanSelection scanMergeSelect) {
    final Set<MatchedLipid> matchedLipids = new HashSet<>();
    final LipidFragmentationRule[] rules = lipid.getLipidClass().getFragmentationRules();
    final List<Scan> msmsScans = scanMergeSelect.getAllFragmentSpectra(row);

    if (!msmsScans.isEmpty() || keepUnconfirmedAnnotations) {
      for (final Scan msmsScan : msmsScans) {
        final Set<MatchedLipid> matchedLipidsInScan = new HashSet<>();
        if (msmsScan.getMassList() == null) {
          return new HashSet<>();
        }
        final DataPoint[] dataPoints = msmsScan.getMassList().getDataPoints();
        final Set<LipidFragment> annotatedFragments = new HashSet<>();
        if (rules != null && rules.length > 0) {
          final ILipidFragmentFactory lipidFragmentFactory = new LipidFragmentFactory(
              mzToleranceMS2,
              lipid, ionization, rules, msmsScan,
              parameters.getParameter(LipidAnnotationParameters.lipidChainParameters)
                  .getEmbeddedParameters());
          final List<LipidFragment> annotatedFragmentsForDataPoint = lipidFragmentFactory.findLipidFragments();
          if (annotatedFragmentsForDataPoint != null && !annotatedFragmentsForDataPoint.isEmpty()) {
            annotatedFragments.addAll(annotatedFragmentsForDataPoint);
          }
        }
        if (!annotatedFragments.isEmpty()) {
          final ISpeciesLevelMatchedLipidFactory matchedLipidFactory = getSpeciesLevelMatchedLipidFactory(
              lipidCategory);
          final MatchedLipid matchedSpeciesLevelLipid = matchedLipidFactory.validateSpeciesLevelAnnotation(
              row.getAverageMZ(), lipid, annotatedFragments, dataPoints, 0d,
              mzToleranceMS2, ionization);
          if (matchedSpeciesLevelLipid != null) {
            matchedLipidsInScan.add(matchedSpeciesLevelLipid);
          }

          final IMolecularSpeciesLevelMatchedLipidFactory matchedMolecularSpeciesLipidFactory = getMolecularSpeciesLevelMatchedLipidFactory(
              lipidCategory);
          final Set<MatchedLipid> molecularSpeciesLevelMatchedLipids = matchedMolecularSpeciesLipidFactory.predictMolecularSpeciesLevelMatches(
              annotatedFragments, lipid, row.getAverageMZ(), dataPoints, 0d,
              mzToleranceMS2, ionization);
          if (molecularSpeciesLevelMatchedLipids != null
              && !molecularSpeciesLevelMatchedLipids.isEmpty()) {
            //Add species level fragments to score
            if (matchedSpeciesLevelLipid != null) {
              for (final MatchedLipid molecularSpeciesLevelMatchedLipid : molecularSpeciesLevelMatchedLipids) {
                molecularSpeciesLevelMatchedLipid.getMatchedFragments()
                    .addAll(matchedSpeciesLevelLipid.getMatchedFragments());
              }
            }
            for (MatchedLipid molecularSpeciesLevelMatchedLipid : molecularSpeciesLevelMatchedLipids) {
              //check MSMS score
              molecularSpeciesLevelMatchedLipid = matchedMolecularSpeciesLipidFactory.validateMolecularSpeciesLevelAnnotation(
                  row.getAverageMZ(), molecularSpeciesLevelMatchedLipid.getLipidAnnotation(),
                  molecularSpeciesLevelMatchedLipid.getMatchedFragments(), dataPoints, 0d,
                  mzToleranceMS2, ionization);
              if (molecularSpeciesLevelMatchedLipid != null) {
                matchedLipidsInScan.add(molecularSpeciesLevelMatchedLipid);
              }
            }
          }
        }
        matchedLipids.addAll(matchedLipidsInScan);
      }
      if (keepUnconfirmedAnnotations && matchedLipids.isEmpty()) {
        final MatchedLipid unconfirmedMatchedLipid = new MatchedLipid(lipid, row.getAverageMZ(),
            ionization, null, 0.0, MatchedLipidStatus.UNCONFIRMED);
        unconfirmedMatchedLipid.setComment(
            "Warning, this annotation is based on MS1 mass accuracy only!");
        matchedLipids.add(unconfirmedMatchedLipid);
      }
      if (!matchedLipids.isEmpty() && matchedLipids.size() > 1) {
        onlyKeepBestAnnotations(row, matchedLipids, true, mzToleranceMS1);
      }
    }

    return matchedLipids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
  }

  @NotNull
  private static IMolecularSpeciesLevelMatchedLipidFactory getMolecularSpeciesLevelMatchedLipidFactory(
      LipidCategories lipidCategory) {
    switch (lipidCategory) {
      case FATTYACYLS -> {
        return new GlyceroAndPhosphoMolecularSpeciesLevelMatchedLipidFactory();
      }
      case GLYCEROLIPIDS -> {
        return new GlyceroAndPhosphoMolecularSpeciesLevelMatchedLipidFactory();
      }
      case GLYCEROPHOSPHOLIPIDS -> {
        return new GlyceroAndPhosphoMolecularSpeciesLevelMatchedLipidFactory();
      }
      case SPHINGOLIPIDS -> {
        return new SphingoMolecularSpeciesLevelMatchedLipidFactory();
      }
      case STEROLLIPIDS -> {
        return new SterolMolecularSpeciesLevelMatchedLipidFactory();
      }
      case PRENOLLIPIDS -> {
      }
      case SACCHAROLIPIDS -> {
      }
      case POLYKETIDES -> {
      }
    }
    return new GlyceroAndPhosphoMolecularSpeciesLevelMatchedLipidFactory();

  }

  @NotNull
  private static ISpeciesLevelMatchedLipidFactory getSpeciesLevelMatchedLipidFactory(
      LipidCategories lipidCategory) {
    switch (lipidCategory) {
      case FATTYACYLS -> {
        return new FattyAcylSpeciesLevelMatchedLipidFactory();
      }
      case GLYCEROLIPIDS -> {
        return new GlyceroAndGlycerophosphoSpeciesLevelMatchedLipidFactory();
      }
      case GLYCEROPHOSPHOLIPIDS -> {
        return new GlyceroAndGlycerophosphoSpeciesLevelMatchedLipidFactory();
      }
      case SPHINGOLIPIDS -> {
        return new SphingolipidSpeciesLevelMatchedLipidFactory();
      }
      case STEROLLIPIDS -> {
        return new SterolSpeciesLevelMatchedLipidFactory();
      }
      case PRENOLLIPIDS -> {
      }
      case SACCHAROLIPIDS -> {
      }
      case POLYKETIDES -> {
      }
    }
    return new GlyceroAndGlycerophosphoSpeciesLevelMatchedLipidFactory();

  }

  private static void onlyKeepBestAnnotations(final @NotNull FeatureListRow row,
      final @NotNull Set<MatchedLipid> matchedLipids, final boolean includeMs2Score,
      final @NotNull MZTolerance mzToleranceMS1) {
    final Map<String, List<MatchedLipid>> matchedLipidsByAnnotation = matchedLipids.stream()
        .filter(Objects::nonNull).collect(Collectors.groupingBy(
            matchedLipid -> matchedLipid.getLipidAnnotation().getAnnotation()));

    final List<List<MatchedLipid>> duplicateMatchedLipids = matchedLipidsByAnnotation.values()
        .stream()
        .filter(group -> group.size() > 1).toList();

    for (final List<MatchedLipid> matchedLipidGroup : duplicateMatchedLipids) {
      if (matchedLipidGroup.size() > 1) {
        final MatchedLipid bestMatch = matchedLipidGroup.stream().max(Comparator.comparingDouble(
                matchedLipid -> preResolverRankingScore(row, matchedLipid, includeMs2Score,
                    mzToleranceMS1)))
            .orElse(null);
        matchedLipidGroup.remove(bestMatch);

        for (final MatchedLipid matchedLipidToRemove : matchedLipidGroup) {
          final Iterator<MatchedLipid> iterator = matchedLipids.iterator();
          while (iterator.hasNext()) {
            final MatchedLipid item = iterator.next();
            if (item != null && item.equals(matchedLipidToRemove)) {
              iterator.remove();
            }
          }
        }
      }
    }
  }

  public static void addAnnotationsToFeatureList(final @NotNull FeatureListRow row,
      final @NotNull Set<MatchedLipid> possibleRowAnnotations,
      final @NotNull LipidAnalysisType lipidAnalysisType, final boolean includeMs2Score,
      final double minimumOverallQualityScore,
      final @Nullable ComponentWeights customQcWeights,
      final @Nullable MZTolerance mzToleranceMS1) {
    //consider previous annotations
    final List<MatchedLipid> previousLipidMatches = row.getLipidMatches();
    if (!previousLipidMatches.isEmpty()) {
      possibleRowAnnotations.addAll(previousLipidMatches);
    }
    final LipidAnnotationResolver lipidAnnotationResolver = new LipidAnnotationResolver(true, true,
        includeMs2Score, lipidAnalysisType.hasRetentionTimePattern(),
        minimumOverallQualityScore, lipidAnalysisType, customQcWeights, mzToleranceMS1);
    List<MatchedLipid> finalResults = lipidAnnotationResolver.resolveFeatureListRowMatchedLipids(
        row, possibleRowAnnotations);

    // set all annotations over the old - already merged the old annotations in
    finalResults = finalResults.stream().filter(Objects::nonNull).toList();
    // caching of lipid properties is triggered in row
    row.setLipidAnnotations(finalResults);
  }

  private static double preResolverRankingScore(final @NotNull FeatureListRow row,
      final @NotNull MatchedLipid matchedLipid, final boolean includeMs2Score,
      final @NotNull MZTolerance mzToleranceMS1) {
    final double ms1Score = computeMs1Score(row, matchedLipid, mzToleranceMS1);
    if (includeMs2Score) {
      return (ms1Score + safeMs2Score(matchedLipid)) / 2d;
    }
    return ms1Score;
  }

  private static double safeMs2Score(final @Nullable MatchedLipid matchedLipid) {
    if (matchedLipid == null || matchedLipid.getMsMsScore() == null) {
      return 0d;
    }
    return clampToUnit(matchedLipid.getMsMsScore());
  }

}

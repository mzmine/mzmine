package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.MSMSLipidTools;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation.FattyAcylFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation.GlyceroAndGlyceroPhospholipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation.ILipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation.SphingolipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation.SterollipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.molecularspecieslevelidentities.GlyceroAndPhosphoMolecularSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.molecularspecieslevelidentities.IMolecularSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.molecularspecieslevelidentities.SphingoMolecularSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.molecularspecieslevelidentities.SterolMolecularSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.FattyAcylSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.GlyceroAndGlycerophosphoSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.ISpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.SphingolipidSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.SterolSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils.LipidAnnotationResolver;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils.LipidFactory;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class LipidAnnotationUtils {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  public static Set<ILipidAnnotation> buildLipidDatabase(ILipidClass[] selectedLipids,
      int minChainLength, int maxChainLength, int minDoubleBonds, int maxDoubleBonds,
      boolean onlySearchForEvenChains) {

    Set<ILipidAnnotation> lipidDatabase = new LinkedHashSet<>();

    // add selected lipids
    buildLipidCombinations(lipidDatabase, selectedLipids, minChainLength, maxChainLength,
        minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);

    return lipidDatabase;
  }

  private static void buildLipidCombinations(Set<ILipidAnnotation> lipidDatabase,
      ILipidClass[] lipidClasses, int minChainLength, int maxChainLength, int minDoubleBonds,
      int maxDoubleBonds, boolean onlySearchForEvenChains) {
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
            lipidDatabase.add(lipid);
          }
        }
      }
    }
  }


  public static void findPossibleLipid(ILipidAnnotation lipid, FeatureListRow row,
      ParameterSet parameters, IonizationType[] ionizationTypesToIgnore, MZTolerance mzTolerance,
      MZTolerance mzToleranceMS2, boolean searchForMSMSFragments, double minMsMsScore,
      boolean keepUnconfirmedAnnotations, LipidCategories lipidCategory) {
    Set<MatchedLipid> possibleRowAnnotations = new HashSet<>();
    Set<IonizationType> ionizationTypeList = new HashSet<>();
    LipidFragmentationRule[] fragmentationRules = lipid.getLipidClass().getFragmentationRules();
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
      if (!Objects.requireNonNull(row.getBestFeature().getRepresentativeScan()).getPolarity()
          .equals(ionization.getPolarity())) {
        continue;
      }
      double lipidIonMass = MolecularFormulaManipulator.getMass(lipid.getMolecularFormula(),
          AtomContainerManipulator.MonoIsotopic) + ionization.getAddedMass();
      Range<Double> mzTolRange12C = mzTolerance.getToleranceRange(row.getAverageMZ());

      // MS1 check
      if (mzTolRange12C.contains(lipidIonMass)) {

        // If search for MSMS fragments is selected search for fragments
        if (searchForMSMSFragments) {
          possibleRowAnnotations.addAll(
              searchMsmsFragments(row, ionization, lipid, parameters, mzToleranceMS2, minMsMsScore,
                  keepUnconfirmedAnnotations, lipidCategory));
        } else {

          // make MS1 annotation
          possibleRowAnnotations.add(
              new MatchedLipid(lipid, row.getAverageMZ(), ionization, null, 0.0));
        }
      }

    }
    if (!possibleRowAnnotations.isEmpty()) {
      addAnnotationsToFeatureList(row, possibleRowAnnotations, mzToleranceMS2, minMsMsScore,
          searchForMSMSFragments, keepUnconfirmedAnnotations);
    }
  }

  /**
   * This method searches for MS/MS fragments. A mass list for MS2 scans will be used if present.
   */
  private static Set<MatchedLipid> searchMsmsFragments(FeatureListRow row,
      IonizationType ionization, ILipidAnnotation lipid, ParameterSet parameters,
      MZTolerance mzToleranceMS2, double minMsMsScore, boolean keepUnconfirmedAnnotations,
      LipidCategories lipidCategory) {
    Set<MatchedLipid> matchedLipids = new HashSet<>();
    // Check if selected feature has MSMS spectra and LipidIdentity
    if (!row.getAllFragmentScans().isEmpty()) {
      List<Scan> msmsScans = row.getAllFragmentScans();
      for (Scan msmsScan : msmsScans) {
        Set<MatchedLipid> matchedLipidsInScan = new HashSet<>();
        if (msmsScan.getMassList() == null) {
          return new HashSet<>();
        }
        DataPoint[] massList = null;
        massList = msmsScan.getMassList().getDataPoints();
        massList = MSMSLipidTools.deisotopeMassList(massList, mzToleranceMS2);
        LipidFragmentationRule[] rules = lipid.getLipidClass().getFragmentationRules();
        Set<LipidFragment> annotatedFragments = new HashSet<>();
        if (rules != null && rules.length > 0) {
          for (DataPoint dataPoint : massList) {
            Range<Double> mzTolRangeMSMS = mzToleranceMS2.getToleranceRange(dataPoint.getMZ());
            ILipidFragmentFactory lipidFragmentFactory = getLipidFragmentFactory(ionization, lipid,
                parameters, msmsScan, rules, dataPoint, mzTolRangeMSMS, lipidCategory);
            List<LipidFragment> annotatedFragmentsForDataPoint = lipidFragmentFactory.findLipidFragments();
            if (annotatedFragmentsForDataPoint != null
                && !annotatedFragmentsForDataPoint.isEmpty()) {
              annotatedFragments.addAll(annotatedFragmentsForDataPoint);
            }
          }
        }
        if (!annotatedFragments.isEmpty()) {
          ISpeciesLevelMatchedLipidFactory matchedLipidFactory = getSpeciesLevelMatchedLipidFactory(
              lipidCategory);
          MatchedLipid matchedSpeciesLevelLipid = matchedLipidFactory.validateSpeciesLevelAnnotation(
              row.getAverageMZ(), lipid, annotatedFragments, massList, minMsMsScore, mzToleranceMS2,
              ionization);
          matchedLipidsInScan.add(matchedSpeciesLevelLipid);

          IMolecularSpeciesLevelMatchedLipidFactory matchedMolecularSpeciesLipidFactory = getMolecularSpeciesLevelMatchedLipidFactory(
              lipidCategory);
          Set<MatchedLipid> molecularSpeciesLevelMatchedLipids = matchedMolecularSpeciesLipidFactory.predictMolecularSpeciesLevelMatches(
              annotatedFragments, lipid, row.getAverageMZ(), massList, minMsMsScore, mzToleranceMS2,
              ionization);
          if (molecularSpeciesLevelMatchedLipids != null
              && !molecularSpeciesLevelMatchedLipids.isEmpty()) {
            //Add species level fragments to score
            if (matchedSpeciesLevelLipid != null) {
              for (MatchedLipid molecularSpeciesLevelMatchedLipid : molecularSpeciesLevelMatchedLipids) {
                molecularSpeciesLevelMatchedLipid.getMatchedFragments()
                    .addAll(matchedSpeciesLevelLipid.getMatchedFragments());
              }
            }
            for (MatchedLipid molecularSpeciesLevelMatchedLipid : molecularSpeciesLevelMatchedLipids) {
              //check MSMS score
              molecularSpeciesLevelMatchedLipid = matchedMolecularSpeciesLipidFactory.validateMolecularSpeciesLevelAnnotation(
                  row.getAverageMZ(), molecularSpeciesLevelMatchedLipid.getLipidAnnotation(),
                  molecularSpeciesLevelMatchedLipid.getMatchedFragments(), massList, minMsMsScore,
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
        MatchedLipid unconfirmedMatchedLipid = new MatchedLipid(lipid, row.getAverageMZ(),
            ionization, null, 0.0);
        unconfirmedMatchedLipid.setComment(
            "Warning, this annotation is based on MS1 mass accurracy only!");
        matchedLipids.add(unconfirmedMatchedLipid);
      }
      if (!matchedLipids.isEmpty() && matchedLipids.size() > 1) {
        onlyKeepBestAnnotations(matchedLipids);
      }
    }

    return matchedLipids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
  }

  @NotNull
  private static IMolecularSpeciesLevelMatchedLipidFactory getMolecularSpeciesLevelMatchedLipidFactory(
      LipidCategories lipidCategory) {
    switch (lipidCategory) {
      case FATTYACYLS -> {
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

  @NotNull
  public static ILipidFragmentFactory getLipidFragmentFactory(IonizationType ionization,
      ILipidAnnotation lipid, ParameterSet parameters, Scan msmsScan,
      LipidFragmentationRule[] rules, DataPoint dataPoint, Range<Double> mzTolRangeMSMS,
      LipidCategories lipidCategory) {
    switch (lipidCategory) {

      case FATTYACYLS -> {
        return new FattyAcylFragmentFactory(mzTolRangeMSMS, lipid, ionization, rules,
            new SimpleDataPoint(dataPoint.getMZ(), dataPoint.getIntensity()), msmsScan,
            parameters.getParameter(LipidAnnotationParameters.lipidChainParameters)
                .getEmbeddedParameters());
      }
      case GLYCEROLIPIDS -> {
        return new GlyceroAndGlyceroPhospholipidFragmentFactory(mzTolRangeMSMS, lipid, ionization,
            rules, new SimpleDataPoint(dataPoint.getMZ(), dataPoint.getIntensity()), msmsScan,
            parameters.getParameter(LipidAnnotationParameters.lipidChainParameters)
                .getEmbeddedParameters());
      }
      case GLYCEROPHOSPHOLIPIDS -> {
        return new GlyceroAndGlyceroPhospholipidFragmentFactory(mzTolRangeMSMS, lipid, ionization,
            rules, new SimpleDataPoint(dataPoint.getMZ(), dataPoint.getIntensity()), msmsScan,
            parameters.getParameter(LipidAnnotationParameters.lipidChainParameters)
                .getEmbeddedParameters());
      }
      case SPHINGOLIPIDS -> {
        return new SphingolipidFragmentFactory(mzTolRangeMSMS, lipid, ionization, rules,
            new SimpleDataPoint(dataPoint.getMZ(), dataPoint.getIntensity()), msmsScan,
            parameters.getParameter(LipidAnnotationParameters.lipidChainParameters)
                .getEmbeddedParameters());
      }
      case STEROLLIPIDS -> {
        return new SterollipidFragmentFactory(mzTolRangeMSMS, lipid, ionization, rules,
            new SimpleDataPoint(dataPoint.getMZ(), dataPoint.getIntensity()), msmsScan,
            parameters.getParameter(LipidAnnotationParameters.lipidChainParameters)
                .getEmbeddedParameters());
      }
      case PRENOLLIPIDS -> {
      }
      case SACCHAROLIPIDS -> {
      }
      case POLYKETIDES -> {
      }
    }
    return new GlyceroAndGlyceroPhospholipidFragmentFactory(mzTolRangeMSMS, lipid, ionization,
        rules, new SimpleDataPoint(dataPoint.getMZ(), dataPoint.getIntensity()), msmsScan,
        parameters.getParameter(LipidAnnotationParameters.lipidChainParameters)
            .getEmbeddedParameters());
  }

  private static void onlyKeepBestAnnotations(Set<MatchedLipid> matchedLipids) {
    Map<String, List<MatchedLipid>> matchedLipidsByAnnotation = matchedLipids.stream()
        .filter(Objects::nonNull).collect(Collectors.groupingBy(
            matchedLipid -> matchedLipid.getLipidAnnotation().getAnnotation()));

    List<List<MatchedLipid>> duplicateMatchedLipids = matchedLipidsByAnnotation.values().stream()
        .filter(group -> group.size() > 1).toList();

    for (List<MatchedLipid> matchedLipidGroup : duplicateMatchedLipids) {
      if (matchedLipidGroup.size() > 1) {
        MatchedLipid bestMatch = matchedLipidGroup.stream()
            .max(Comparator.comparingDouble(MatchedLipid::getMsMsScore)).orElse(null);
        matchedLipidGroup.remove(bestMatch);

        for (MatchedLipid matchedLipidToRemove : matchedLipidGroup) {
          Iterator<MatchedLipid> iterator = matchedLipids.iterator();
          while (iterator.hasNext()) {
            MatchedLipid item = iterator.next();
            if (item != null && item.equals(matchedLipidToRemove)) {
              iterator.remove();
            }
          }
        }
      }
    }
  }

  private static void addAnnotationsToFeatureList(FeatureListRow row,
      Set<MatchedLipid> possibleRowAnnotations, MZTolerance mzToleranceMS2, double minMsMsScore,
      boolean searchForMSMSFragments, boolean keepUnconfirmedAnnotations) {
    //consider previous annotations
    List<MatchedLipid> previousLipidMatches = row.getLipidMatches();
    if (!previousLipidMatches.isEmpty()) {
      row.set(LipidMatchListType.class, null);
      possibleRowAnnotations.addAll(previousLipidMatches);
    }
    LipidAnnotationResolver lipidAnnotationResolver = new LipidAnnotationResolver(true, true, true,
        mzToleranceMS2, minMsMsScore, searchForMSMSFragments, keepUnconfirmedAnnotations);
    List<MatchedLipid> finalResults = lipidAnnotationResolver.resolveFeatureListRowMatchedLipids(
        row, possibleRowAnnotations);
    for (MatchedLipid matchedLipid : finalResults) {
      if (matchedLipid != null) {
        row.addLipidAnnotation(matchedLipid);
      }
    }
  }

}



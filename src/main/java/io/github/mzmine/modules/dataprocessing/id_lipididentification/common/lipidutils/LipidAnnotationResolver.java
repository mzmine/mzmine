package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.MSMSLipidTools;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.molecularspecieslevelidentities.FattyAcylMolecularSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.molecularspecieslevelidentities.GlyceroAndPhosphoMolecularSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.molecularspecieslevelidentities.IMolecularSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.molecularspecieslevelidentities.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.molecularspecieslevelidentities.SphingoMolecularSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.molecularspecieslevelidentities.SterolMolecularSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.FattyAcylSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.GlyceroAndGlycerophosphoSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.ISpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.SphingolipidSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.SterolSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.ILipidChain;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class LipidAnnotationResolver {

  private final boolean keepIsobars;
  private final boolean keepIsomers;
  private final boolean addMissingSpeciesLevelAnnotation;
  private final MZTolerance mzToleranceMS2;
  private final double minMsMsScore;
  private int maximumIdNumber;
  private final boolean searchForMSMSFragments;
  private final boolean keepUnconfirmedAnnotations;
  private static final MSMSLipidTools MSMS_LIPID_TOOLS = new MSMSLipidTools();
  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  public LipidAnnotationResolver(boolean keepIsobars, boolean keepIsomers,
      boolean addMissingSpeciesLevelAnnotation, MZTolerance mzToleranceMS2, double minMsMsScore,
      boolean searchForMSMSFragments, boolean keepUnconfirmedAnnotations) {
    this.keepIsobars = keepIsobars;
    this.keepIsomers = keepIsomers;
    this.addMissingSpeciesLevelAnnotation = addMissingSpeciesLevelAnnotation;
    this.maximumIdNumber = -1;
    this.mzToleranceMS2 = mzToleranceMS2;
    this.minMsMsScore = minMsMsScore;
    this.searchForMSMSFragments = searchForMSMSFragments;
    this.keepUnconfirmedAnnotations = keepUnconfirmedAnnotations;
  }

  public LipidAnnotationResolver(boolean keepIsobars, boolean keepIsomers,
      boolean addMissingSpeciesLevelAnnotation, MZTolerance mzToleranceMS2, double minMsMsScore,
      int maximumIdNumber, boolean searchForMSMSFragments, boolean keepUnconfirmedAnnotations) {
    this(keepIsobars, keepIsomers, addMissingSpeciesLevelAnnotation, mzToleranceMS2, minMsMsScore,
        searchForMSMSFragments, keepUnconfirmedAnnotations);
    this.maximumIdNumber = maximumIdNumber;
  }

  public List<MatchedLipid> resolveFeatureListRowMatchedLipids(FeatureListRow featureListRow,
      Set<MatchedLipid> matchedLipids) {
    List<MatchedLipid> resolvedMatchedLipidsList = new ArrayList<>(matchedLipids);
    sortByMsMsScore(resolvedMatchedLipidsList);
    removeMultiplyMatchedDataPoints(resolvedMatchedLipidsList, featureListRow);
    if (addMissingSpeciesLevelAnnotation) {
      estimateMissingSpeciesLevelAnnotations(resolvedMatchedLipidsList);
    }
    //TODO: Add Keep isobars functionality

    //TODO: Add keep isomers functionality

    //add to resolved list
    if (maximumIdNumber != -1 && resolvedMatchedLipidsList.size() > maximumIdNumber) {
      filterMaximumNumberOfId(resolvedMatchedLipidsList);
    }
    return resolvedMatchedLipidsList;
  }

  private void estimateMissingSpeciesLevelAnnotations(
      List<MatchedLipid> resolvedMatchedLipidsList) {
    if (resolvedMatchedLipidsList.stream().anyMatch(
        matchedLipid -> matchedLipid.getLipidAnnotation().getLipidAnnotationLevel()
            .equals(LipidAnnotationLevel.SPECIES_LEVEL))) {
      return;
    } else {
      MatchedLipid estimatedSpeciesLevelMatchedLipid = null;
      for (MatchedLipid lipid : resolvedMatchedLipidsList) {
        ILipidAnnotation estimatedSpeciesLevelAnnotation = convertMolecularSpeciesLevelToSpeciesLevel(
            (MolecularSpeciesLevelAnnotation) lipid.getLipidAnnotation());
        if ((estimatedSpeciesLevelAnnotation != null && estimatedSpeciesLevelMatchedLipid == null)
            || (estimatedSpeciesLevelAnnotation != null
            && estimatedSpeciesLevelMatchedLipid.getLipidAnnotation()
            != estimatedSpeciesLevelAnnotation)) {
          estimatedSpeciesLevelMatchedLipid = new MatchedLipid(estimatedSpeciesLevelAnnotation,
              lipid.getAccurateMz(), lipid.getIonizationType(), lipid.getMatchedFragments(), 0.0);
          estimatedSpeciesLevelMatchedLipid.setComment(
              "Estimated annotation based on molecular species level fragments");
        }
      }
      if (estimatedSpeciesLevelMatchedLipid != null) {
        resolvedMatchedLipidsList.add(estimatedSpeciesLevelMatchedLipid);
      }
    }

  }

  private SpeciesLevelAnnotation convertMolecularSpeciesLevelToSpeciesLevel(
      MolecularSpeciesLevelAnnotation lipidAnnotation) {
    int numberOfCarbons = lipidAnnotation.getLipidChains().stream()
        .mapToInt(ILipidChain::getNumberOfCarbons).sum();
    int numberOfDBEs = lipidAnnotation.getLipidChains().stream()
        .mapToInt(ILipidChain::getNumberOfDBEs).sum();
    return LIPID_FACTORY.buildSpeciesLevelLipid(lipidAnnotation.getLipidClass(), numberOfCarbons,
        numberOfDBEs, 0);
  }

  /*
   * If a Data Point was used for multiple lipid annotations it should be removed from all
   * annotations, except the best one
   */
  private void removeMultiplyMatchedDataPoints(List<MatchedLipid> matchedLipidsList,
      FeatureListRow row) {
    Map<DataPoint, Set<MatchedLipid>> dataPointMap = new HashMap<>();
    for (MatchedLipid matchedLipid : matchedLipidsList) {
      Set<LipidFragment> matchedFragments = matchedLipid.getMatchedFragments();
      if (matchedFragments != null) {
        for (LipidFragment matchedFragment : matchedFragments) {
          DataPoint dataPoint = matchedFragment.getDataPoint();
          dataPointMap.computeIfAbsent(dataPoint, k -> new HashSet<>()).add(matchedLipid);
        }
      }
    }

    List<MatchedLipid> lipidsToRemove = new ArrayList<>();
    Set<LipidFragment> processedFragments = new HashSet<>(); // Track processed fragments
    for (Map.Entry<DataPoint, Set<MatchedLipid>> entry : dataPointMap.entrySet()) {
      Set<MatchedLipid> matchedLipids = entry.getValue();

      // Find the MatchedLipid with the highest MsMsScore in the set
      MatchedLipid highestScoreLipid = null;
      double highestScore = Double.NEGATIVE_INFINITY;
      for (MatchedLipid lipid : matchedLipids) {
        if (lipid.getMsMsScore() > highestScore) {
          highestScore = lipid.getMsMsScore();
          highestScoreLipid = lipid;
        }
      }

      // Remove LipidFragment for all entries except the one with the highest MsMsScore
      // Keep if two different lipids have the same score
      for (MatchedLipid lipid : matchedLipids) {
        if ((lipid != highestScoreLipid && lipid.getMsMsScore() != highestScore) || (
            lipid != highestScoreLipid && lipid.getLipidAnnotation().getAnnotation().equals(
                Objects.requireNonNull(highestScoreLipid).getLipidAnnotation().getAnnotation()))) {
          Set<LipidFragment> fragmentsToRemove = new HashSet<>();
          for (LipidFragment matchedFragment : lipid.getMatchedFragments()) {
            if (matchedFragment.getDataPoint().equals(entry.getKey())
                && !processedFragments.contains(matchedFragment)) {
              fragmentsToRemove.add(matchedFragment);
              processedFragments.add(matchedFragment);
            }
          }
          lipid.getMatchedFragments().removeAll(fragmentsToRemove);
          reanalyzeMsMsScore(lipid, row);
        }
      }
    }
    for (MatchedLipid lipid : matchedLipidsList) {
      if (lipid.getMsMsScore() < minMsMsScore) {
        if (keepUnconfirmedAnnotations || !searchForMSMSFragments) {
          lipid.setComment("Warning, this annotation is based on MS1 mass accuracy only!");
        } else {
          lipidsToRemove.add(lipid);
        }
      }
    }
    lipidsToRemove.forEach(matchedLipidsList::remove);
  }

  private void reanalyzeMsMsScore(MatchedLipid lipid, FeatureListRow row) {
    Set<LipidFragment> matchedFragments = lipid.getMatchedFragments();
    if (matchedFragments != null && !matchedFragments.isEmpty()) {
      Scan msMsScan = matchedFragments.stream().findAny().get().getMsMsScan();
      DataPoint[] dataPoints = msMsScan.getMassList().getDataPoints();
      dataPoints = MSMSLipidTools.deisotopeMassList(dataPoints, mzToleranceMS2);
      Set<MatchedLipid> recalculatedMatches = getRecalculatedMatchedLipids(lipid, row,
          matchedFragments, dataPoints);
      if (recalculatedMatches != null && !recalculatedMatches.isEmpty()
          && recalculatedMatches.stream().anyMatch(
          matchedLipid -> matchedLipid.getLipidAnnotation().equals(lipid.getLipidAnnotation()))) {
        IMolecularFormula lipidFormula = null;
        try {
          lipidFormula = (IMolecularFormula) lipid.getLipidAnnotation().getMolecularFormula()
              .clone();
        } catch (CloneNotSupportedException e) {
          throw new RuntimeException(e);
        }
        lipid.getIonizationType().ionizeFormula(lipidFormula);
        double precursorMz = FormulaUtils.calculateMzRatio(lipidFormula);
        Double msMsScore = MSMS_LIPID_TOOLS.calculateMsMsScore(dataPoints, matchedFragments,
            precursorMz, mzToleranceMS2);
        lipid.setMsMsScore(msMsScore);
      } else {
        lipid.setMsMsScore(0.0);
      }
    } else {
      lipid.setMsMsScore(0.0);
    }
  }

  @Nullable
  private Set<MatchedLipid> getRecalculatedMatchedLipids(MatchedLipid lipid, FeatureListRow row,
      Set<LipidFragment> matchedFragments, DataPoint[] dataPoints) {
    // Try to redo the annotation with fewer fragments
    Set<MatchedLipid> recalculatedMatches = new HashSet<>();
    LipidCategories lipidCategory = lipid.getLipidAnnotation().getLipidClass().getMainClass()
        .getLipidCategory();
    switch (lipidCategory) {
      case FATTYACYLS -> {
        if (lipid.getLipidAnnotation() instanceof MolecularSpeciesLevelAnnotation) {
          IMolecularSpeciesLevelMatchedLipidFactory matchedMolecularSpeciesLipidFactory = new FattyAcylMolecularSpeciesLevelMatchedLipidFactory();
          recalculatedMatches = matchedMolecularSpeciesLipidFactory.predictMolecularSpeciesLevelMatches(
              matchedFragments, lipid.getLipidAnnotation(), row.getAverageMZ(), dataPoints,
              minMsMsScore, mzToleranceMS2, lipid.getIonizationType());
        } else if (lipid.getLipidAnnotation() instanceof SpeciesLevelAnnotation) {
          ISpeciesLevelMatchedLipidFactory matchedSpeciesLipidFactory = new FattyAcylSpeciesLevelMatchedLipidFactory();
          MatchedLipid reMatchedLipid = matchedSpeciesLipidFactory.validateSpeciesLevelAnnotation(
              row.getAverageMZ(), lipid.getLipidAnnotation(), matchedFragments, dataPoints,
              minMsMsScore, mzToleranceMS2, lipid.getIonizationType());
          if (reMatchedLipid != null) {
            recalculatedMatches.add(reMatchedLipid);
          }
        }
      }
      case GLYCEROLIPIDS -> {
        if (lipid.getLipidAnnotation() instanceof MolecularSpeciesLevelAnnotation) {
          IMolecularSpeciesLevelMatchedLipidFactory matchedMolecularSpeciesLipidFactory = new GlyceroAndPhosphoMolecularSpeciesLevelMatchedLipidFactory();
          recalculatedMatches = matchedMolecularSpeciesLipidFactory.predictMolecularSpeciesLevelMatches(
              matchedFragments, lipid.getLipidAnnotation(), row.getAverageMZ(), dataPoints,
              minMsMsScore, mzToleranceMS2, lipid.getIonizationType());
        } else if (lipid.getLipidAnnotation() instanceof SpeciesLevelAnnotation) {
          ISpeciesLevelMatchedLipidFactory matchedSpeciesLipidFactory = new GlyceroAndGlycerophosphoSpeciesLevelMatchedLipidFactory();
          MatchedLipid reMatchedLipid = matchedSpeciesLipidFactory.validateSpeciesLevelAnnotation(
              row.getAverageMZ(), lipid.getLipidAnnotation(), matchedFragments, dataPoints,
              minMsMsScore, mzToleranceMS2, lipid.getIonizationType());
          if (reMatchedLipid != null) {
            recalculatedMatches.add(reMatchedLipid);
          }
        }
      }
      case SPHINGOLIPIDS -> {
        if (lipid.getLipidAnnotation() instanceof MolecularSpeciesLevelAnnotation) {
          IMolecularSpeciesLevelMatchedLipidFactory matchedMolecularSpeciesLipidFactory = new SphingoMolecularSpeciesLevelMatchedLipidFactory();
          recalculatedMatches = matchedMolecularSpeciesLipidFactory.predictMolecularSpeciesLevelMatches(
              matchedFragments, lipid.getLipidAnnotation(), row.getAverageMZ(), dataPoints,
              minMsMsScore, mzToleranceMS2, lipid.getIonizationType());
        } else if (lipid.getLipidAnnotation() instanceof SpeciesLevelAnnotation) {
          ISpeciesLevelMatchedLipidFactory matchedSpeciesLipidFactory = new SphingolipidSpeciesLevelMatchedLipidFactory();
          MatchedLipid reMatchedLipid = matchedSpeciesLipidFactory.validateSpeciesLevelAnnotation(
              row.getAverageMZ(), lipid.getLipidAnnotation(), matchedFragments, dataPoints,
              minMsMsScore, mzToleranceMS2, lipid.getIonizationType());
          if (reMatchedLipid != null) {
            recalculatedMatches.add(reMatchedLipid);
          }
        }
      }
      case STEROLLIPIDS -> {
        if (lipid.getLipidAnnotation() instanceof MolecularSpeciesLevelAnnotation) {
          IMolecularSpeciesLevelMatchedLipidFactory matchedMolecularSpeciesLipidFactory = new SterolMolecularSpeciesLevelMatchedLipidFactory();
          recalculatedMatches = matchedMolecularSpeciesLipidFactory.predictMolecularSpeciesLevelMatches(
              matchedFragments, lipid.getLipidAnnotation(), row.getAverageMZ(), dataPoints,
              minMsMsScore, mzToleranceMS2, lipid.getIonizationType());
        } else if (lipid.getLipidAnnotation() instanceof SpeciesLevelAnnotation) {
          ISpeciesLevelMatchedLipidFactory matchedSpeciesLipidFactory = new SterolSpeciesLevelMatchedLipidFactory();
          MatchedLipid reMatchedLipid = matchedSpeciesLipidFactory.validateSpeciesLevelAnnotation(
              row.getAverageMZ(), lipid.getLipidAnnotation(), matchedFragments, dataPoints,
              minMsMsScore, mzToleranceMS2, lipid.getIonizationType());
          if (reMatchedLipid != null) {
            recalculatedMatches.add(reMatchedLipid);
          }
        }
      }
    }

    return recalculatedMatches;
  }

  private void sortByMsMsScore(List<MatchedLipid> matchedLipids) {
    matchedLipids.sort(Comparator.comparingDouble(MatchedLipid::getMsMsScore).reversed());
  }

  private void filterMaximumNumberOfId(List<MatchedLipid> resolvedMatchedLipids) {
    Iterator<MatchedLipid> iterator = resolvedMatchedLipids.iterator();
    while (iterator.hasNext()) {
      MatchedLipid lipid = iterator.next();
      if (resolvedMatchedLipids.indexOf(lipid) > maximumIdNumber) {
        iterator.remove();
      }
    }
  }
}

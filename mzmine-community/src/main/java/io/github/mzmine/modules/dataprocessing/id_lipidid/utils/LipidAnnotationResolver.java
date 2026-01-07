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

package io.github.mzmine.modules.dataprocessing.id_lipidid.utils;

import com.google.common.collect.ComparisonChain;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipidStatus;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.molecular_species.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.ILipidChain;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The LipidAnnotationResolver class is responsible for resolving and processing matched lipid
 * annotations associated with a feature list row. It provides methods to handle duplicate entries,
 * estimate missing species-level annotations, and limit the maximum number of matched lipids to be
 * retained. This class is designed to enhance the accuracy and usefulness of lipid annotations for
 * a given feature.
 * <p>
 * The class resolves lipids from multiple annotation runs resulting from the same or different
 * annotation parameters.
 */
public class LipidAnnotationResolver {

  private final boolean keepIsobars;
  private final boolean keepIsomers;
  private final boolean addMissingSpeciesLevelAnnotation;

  private int maximumIdNumber;
  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  public LipidAnnotationResolver(boolean keepIsobars, boolean keepIsomers,
      boolean addMissingSpeciesLevelAnnotation) {
    this.keepIsobars = keepIsobars;
    this.keepIsomers = keepIsomers;
    this.addMissingSpeciesLevelAnnotation = addMissingSpeciesLevelAnnotation;
    this.maximumIdNumber = -1;

  }

  public LipidAnnotationResolver(boolean keepIsobars, boolean keepIsomers,
      boolean addMissingSpeciesLevelAnnotation, int maximumIdNumber) {
    this(keepIsobars, keepIsomers, addMissingSpeciesLevelAnnotation);
    this.maximumIdNumber = maximumIdNumber;
  }

  public List<MatchedLipid> resolveFeatureListRowMatchedLipids(FeatureListRow featureListRow,
      Set<MatchedLipid> matchedLipids) {
    List<MatchedLipid> resolvedMatchedLipidsList = removeDuplicates(matchedLipids);
    sortByMsMsScore(resolvedMatchedLipidsList);
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

  private List<MatchedLipid> removeDuplicates(Set<MatchedLipid> resolvedMatchedLipids) {
    return resolvedMatchedLipids.stream().collect(Collectors.collectingAndThen(
        Collectors.toCollection(() -> new TreeSet<>(comparatorMatchedLipids())), ArrayList::new));
  }

  private void estimateMissingSpeciesLevelAnnotations(
      List<MatchedLipid> resolvedMatchedLipidsList) {
    if (resolvedMatchedLipidsList.stream().noneMatch(
        matchedLipid -> matchedLipid.getLipidAnnotation().getLipidAnnotationLevel()
            .equals(LipidAnnotationLevel.SPECIES_LEVEL))) {
      Set<MatchedLipid> estimatedSpeciesLevelMatchedLipids = new HashSet<>();
      for (MatchedLipid lipid : resolvedMatchedLipidsList) {
        ILipidAnnotation estimatedSpeciesLevelAnnotation = convertMolecularSpeciesLevelToSpeciesLevel(
            (MolecularSpeciesLevelAnnotation) lipid.getLipidAnnotation());
        if (resolvedMatchedLipidsList.stream().noneMatch(
            matchedLipid -> matchedLipid.getLipidAnnotation().getAnnotation()
                .equals(estimatedSpeciesLevelAnnotation.getAnnotation()))) {
          if ((estimatedSpeciesLevelAnnotation != null
               && estimatedSpeciesLevelMatchedLipids.isEmpty()) || (
                  estimatedSpeciesLevelAnnotation != null
                  && estimatedSpeciesLevelMatchedLipids.stream().anyMatch(
                      matchedLipid -> !Objects.equals(
                          matchedLipid.getLipidAnnotation().getAnnotation(),
                          estimatedSpeciesLevelAnnotation.getAnnotation())))) {
            MatchedLipid matchedLipidSpeciesLevel = new MatchedLipid(
                estimatedSpeciesLevelAnnotation, lipid.getAccurateMz(), lipid.getIonizationType(),
                new HashSet<>(lipid.getMatchedFragments()), 0.0,
                MatchedLipidStatus.SPECIES_DERIVED_FROM_MOLECULAR_SPECIES);
            estimatedSpeciesLevelMatchedLipids.add(matchedLipidSpeciesLevel);
          }
        }
      }
      if (!estimatedSpeciesLevelMatchedLipids.isEmpty()) {
        resolvedMatchedLipidsList.addAll(estimatedSpeciesLevelMatchedLipids);
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

  private void filterMaximumNumberOfId(List<MatchedLipid> resolvedMatchedLipids) {
    Iterator<MatchedLipid> iterator = resolvedMatchedLipids.iterator();
    while (iterator.hasNext()) {
      MatchedLipid lipid = iterator.next();
      if (resolvedMatchedLipids.indexOf(lipid) > maximumIdNumber) {
        iterator.remove();
      }
    }
  }

  private static Comparator<MatchedLipid> comparatorMatchedLipids() {
    return (lipid1, lipid2) -> ComparisonChain.start()
        .compare(lipid1.getLipidAnnotation().getAnnotation(),
            lipid2.getLipidAnnotation().getAnnotation())
        .compare(lipid1.getMsMsScore(), lipid2.getMsMsScore())
        .compare(lipid1.getAccurateMz(), lipid2.getAccurateMz()).result();
  }

  private void sortByMsMsScore(List<MatchedLipid> matchedLipids) {
    matchedLipids.sort(Comparator.comparingDouble(MatchedLipid::getMsMsScore).reversed());
  }

}

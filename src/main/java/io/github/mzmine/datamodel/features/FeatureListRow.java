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

package io.github.mzmine.datamodel.features;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.CompoundDBIdentity;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface representing feature list row
 */
public interface FeatureListRow extends ModularDataModel {

  /**
   * Return raw data with features on this row
   */
  List<RawDataFile> getRawDataFiles();

  /**
   * Returns ID of this row
   */
  Integer getID();

  /**
   * Returns number of features assigned to this row
   */
  int getNumberOfFeatures();

  // Helper methods
  Range<Double> getMZRange();

  /**
   * Return features assigned to this row
   */
  List<ModularFeature> getFeatures();

  /**
   * Returns feature for given raw data file
   */
  @Nullable
  Feature getFeature(RawDataFile rawData);

  /**
   * Add a feature
   */
  void addFeature(RawDataFile rawData, Feature feature);

  /**
   * Remove a feature
   */
  void removeFeature(RawDataFile file);

  /**
   * Has a feature?
   */
  boolean hasFeature(Feature feature);

  /**
   * Has a feature?
   */
  boolean hasFeature(RawDataFile rawData);

  /**
   * Returns average M/Z for features on this row
   */
  Double getAverageMZ();

  /**
   * Sets average mz for this row
   */
  void setAverageMZ(Double averageMZ);

  /**
   * Returns average RT for features on this row
   */
  Float getAverageRT();

  /**
   * Sets average rt for this row
   */
  void setAverageRT(Float averageRT);

  /**
   * Returns average mobility for features on this row
   */
  @Nullable Float getAverageMobility();

  Float getAverageCCS();

  /**
   * Returns average height for features on this row
   */
  Float getAverageHeight();

  /**
   * Returns the charge for feature on this row. If more charges are found 0 is returned
   */
  Integer getRowCharge();

  /**
   * Returns average area for features on this row
   */
  Float getAverageArea();

  /**
   * Returns comment for this row
   */
  String getComment();

  /**
   * Sets comment for this row
   */
  void setComment(String comment);

  /**
   * Add a new identity candidate (result of identification method)
   *
   * @param identity  New feature identity
   * @param preffered boolean value to define this identity as preferred identity
   */
  void addFeatureIdentity(FeatureIdentity identity, boolean preffered);

  /**
   * Remove identity candidate
   *
   * @param identity Feature identity
   */
  void removeFeatureIdentity(FeatureIdentity identity);

  void addCompoundAnnotation(CompoundDBIdentity id);

  /**
   * Returns all candidates for this feature's identity
   *
   * @return Identity candidates
   */
  List<FeatureIdentity> getPeakIdentities();

  /**
   * Returns preferred feature identity among candidates
   *
   * @return Preferred identity
   */
  FeatureIdentity getPreferredFeatureIdentity();

  /**
   * Sets a preferred feature identity among candidates
   *
   * @param identity Preferred identity
   */
  void setPreferredFeatureIdentity(FeatureIdentity identity);

  /**
   * Returns FeatureInformation
   *
   * @return
   */

  FeatureInformation getFeatureInformation();

  /**
   * Adds a new FeatureInformation object.
   * <p>
   * FeatureInformation is used to keep extra information about features in the form of a map
   * <propertyName, propertyValue>
   *
   * @param featureInformation object
   */

  void setFeatureInformation(FeatureInformation featureInformation);

  /**
   * Returns maximum raw data point intensity among all features in this row
   *
   * @return Maximum intensity
   */
  Float getMaxDataPointIntensity();

  /**
   * Returns the most intense feature in this row
   */
  Feature getBestFeature();

  /**
   * Returns the most intense fragmentation scan in this row
   */
  Scan getMostIntenseFragmentScan();

  /**
   * Returns all fragmentation scans of this row
   */
  @NotNull List<Scan> getAllFragmentScans();

  /**
   * Returns the most intense isotope pattern in this row. If there are no isotope patterns present
   * in the row, returns null.
   */
  IsotopePattern getBestIsotopePattern();

  @Nullable FeatureList getFeatureList();

  void setFeatureList(@NotNull FeatureList flist);

  void addSpectralLibraryMatch(SpectralDBFeatureIdentity id);

  /**
   * Correlated features grouped
   *
   * @return
   */
  RowGroup getGroup();

  /**
   * Correlated features grouped
   *
   * @param group
   */
  void setGroup(RowGroup group);

  /**
   * The list of ion identities
   *
   * @return null or the current list. First element is the "preferred" element
   */
  @Nullable List<IonIdentity> getIonIdentities();

  /**
   * Set the list of ion identities with the first element being the preferred
   *
   * @param ions list of ion identities
   */
  void setIonIdentities(@Nullable List<IonIdentity> ions);

  /**
   * Adds the ion identity as the preferred (first element) of the list
   *
   * @param ion the preferred ion identity
   */
  default void addIonIdentity(IonIdentity ion) {
    this.addIonIdentity(ion, true);
  }

  /**
   * Adds the ion as first or last element of the list.
   *
   * @param ion        the ion identity
   * @param markAsBest true: add as first element; false add as last element
   */
  default void addIonIdentity(IonIdentity ion, boolean markAsBest) {
    // unmodifiable list
    List<IonIdentity> ionIdentities = getIonIdentities();

    if (ionIdentities == null || ionIdentities.isEmpty()) {
      setIonIdentities(List.of(ion));
      return;
    }

    // remove first
    ArrayList<IonIdentity> newList = new ArrayList<>(ionIdentities);
    newList.remove(ion);

    if (markAsBest) {
      newList.add(0, ion);
    } else {
      newList.add(ion);
    }
    setIonIdentities(newList);
  }

  /**
   * Clear all ion identities
   */
  default void clearIonIdentites() {
    setIonIdentities(null);
  }

  /**
   * The first element of {@link #getIonIdentities()}
   *
   * @return the preferred (first) element of all ion identities
   */
  @Nullable
  default IonIdentity getBestIonIdentity() {
    List<IonIdentity> ionIdentities = getIonIdentities();
    return ionIdentities != null && !ionIdentities.isEmpty() ? ionIdentities.get(0) : null;
  }

  /**
   * Set the best ion identity (the first element of the list)
   *
   * @param ion the preferred ion
   */
  default void setBestIonIdentity(@NotNull IonIdentity ion) {
    addIonIdentity(ion, true);
  }

  /**
   * Has at least one ion identity
   */
  default boolean hasIonIdentity() {
    List<IonIdentity> ionIdentities = getIonIdentities();
    return ionIdentities != null && !ionIdentities.isEmpty();
  }

  /**
   * Remove ion identity if available
   *
   * @param ion the ion to remove
   */
  default boolean removeIonIdentity(IonIdentity ion) {
    List<IonIdentity> ionIdentities = getIonIdentities();
    if (ionIdentities != null) {
      ionIdentities = new ArrayList<>(ionIdentities);
      boolean removed = ionIdentities.remove(ion);
      if (removed) {
        setIonIdentities(ionIdentities.isEmpty() ? null : ionIdentities);
      }
      return removed;
    }
    return false;
  }

  /**
   * Returns the group ID
   *
   * @return return the group ID or -1 if not part of a group {@link #getGroup()}
   */
  default int getGroupID() {
    RowGroup g = getGroup();
    return g == null ? -1 : g.groupID;
  }

  List<ResultFormula> getFormulas();

  void setFormulas(List<ResultFormula> formulas);

  /**
   * Checks if MS2 fragmentation data is available
   *
   * @return true if this row has at least 1 MS2 spectrum
   */
  default boolean hasMs2Fragmentation() {
    // should be faster. Best fragmentation loops through all spectra to find best
    return getAllFragmentScans() != null && !getAllFragmentScans().isEmpty();
  }

  /**
   * The intensity summed over all features
   *
   * @return sum of all feature heights
   */
  default double getSumIntensity() {
    return this.getFeatures().stream().filter(Objects::nonNull)
        .filter(f -> f.getFeatureStatus() != FeatureStatus.UNKNOWN).mapToDouble(Feature::getHeight)
        .sum();
  }

  /**
   * Set and override the list of matches
   *
   * @param matches new list of matches
   */
  void setSpectralLibraryMatch(List<SpectralDBFeatureIdentity> matches);

  /**
   * List of library matches sorted from best (index 0) to last match
   *
   * @return list of library matches or an empty list
   */
  @NotNull List<SpectralDBFeatureIdentity> getSpectralLibraryMatches();

  /**
   * Add annotations from lipid search
   *
   * @param matchedLipid the matched lipid
   */
  void addLipidAnnotation(MatchedLipid matchedLipid);

  // -- ModularFeatureListRow additions
  Stream<ModularFeature> streamFeatures();

  void addSpectralLibraryMatches(List<SpectralDBFeatureIdentity> matches);
}

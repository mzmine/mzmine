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

package io.github.mzmine.datamodel.features;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineReactionMatch;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface representing feature list row
 */
public interface FeatureListRow extends ModularDataModel {

  /**
   * Return unmodifiable list of all raw data files in this feature list even those without detection in this row
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
  @Nullable Feature getFeature(RawDataFile rawData);

  /**
   * Add a feature and update all row bindings
   */
  default void addFeature(RawDataFile rawData, Feature feature) {
    addFeature(rawData, feature, true);
  }

  /**
   * add feature and choose to update values by row bindings.
   *
   * @param rawData             associated raw data file
   * @param feature             added feature
   * @param updateByRowBindings updates values by row bindings if true. In case multiple features
   *                            are added, this option may be set to false. Remember to call
   *                            {@link #applyRowBindings()}.
   */
  void addFeature(RawDataFile rawData, Feature feature, boolean updateByRowBindings);


  /**
   * Remove all features from this row
   *
   * @param updateByRowBindings updates values by row bindings if true. In case multiple feature
   *                            add/remove operations are done, this option may be set to false.
   *                            Remember to call {@link #applyRowBindings()}.
   */
  void clearFeatures(boolean updateByRowBindings);

  /**
   * Remove all features from this row
   */
  default void clearFeatures() {
    clearFeatures(true);
  }

  /**
   * apply row bindings of the feature list (if available) to this row
   */
  default void applyRowBindings() {
    final FeatureList featureList = getFeatureList();
    if (featureList != null) {
      featureList.applyRowBindings(this);
    }
  }

  /**
   * Remove a feature
   */
  default void removeFeature(RawDataFile file) {
    removeFeature(file, true);
  }

  /**
   * Remove a features from this row
   *
   * @param updateByRowBindings updates values by row bindings if true. In case multiple feature
   *                            add/remove operations are done, this option may be set to false.
   *                            Remember to call {@link #applyRowBindings()}.
   */
  void removeFeature(RawDataFile file, boolean updateByRowBindings);

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

  @Nullable Float getAverageCCS();

  /**
   * Returns maximum height for features on this row
   */
  Float getMaxHeight();

  /**
   * Returns the charge for feature on this row. If more charges are found 0 is returned
   */
  Integer getRowCharge();

  /**
   * Returns maximum area for features on this row
   */
  Float getMaxArea();

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
   * @deprecated To be replaced by {@link #get(DataType)} and {@link #set(DataType, Object)} and the
   * corresponding data types.
   */
  @Deprecated
  @ScheduledForRemoval
  void addFeatureIdentity(FeatureIdentity identity, boolean preffered);

  @NotNull List<OnlineReactionMatch> getOnlineReactionMatches();

  @NotNull List<MatchedLipid> getLipidMatches();

  /**
   * Remove identity candidate
   *
   * @param identity Feature identity
   * @deprecated To be replaced by {@link #get(DataType)} and {@link #set(DataType, Object)} and the
   * corresponding data types.
   */
  @Deprecated
  @ScheduledForRemoval
  void removeFeatureIdentity(FeatureIdentity identity);

  @Nullable ManualAnnotation getManualAnnotation();

  /**
   * Returns all candidates for this feature's identity
   *
   * @return Identity candidates
   * @deprecated To be replaced by {@link #get(DataType)} and {@link #set(DataType, Object)} and the
   * corresponding data types.
   */
  @Deprecated
  @ScheduledForRemoval
  List<FeatureIdentity> getPeakIdentities();

  /**
   * Returns preferred feature identity among candidates
   *
   * @return Preferred identity
   * @deprecated To be replaced by {@link #get(DataType)} and {@link #set(DataType, Object)} and the
   * corresponding data types.
   */
  @Deprecated
  @ScheduledForRemoval
  FeatureIdentity getPreferredFeatureIdentity();

  /**
   * Sets a preferred feature identity among candidates
   *
   * @param identity Preferred identity
   * @deprecated To be replaced by {@link #get(DataType)} and {@link #set(DataType, Object)} and the
   * corresponding data types.
   */
  @Deprecated
  @ScheduledForRemoval
  void setPreferredFeatureIdentity(FeatureIdentity identity);

  /**
   * Returns FeatureInformation
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
   * Returns all fragmentation scans of this row - a new ArrayList
   */
  @NotNull List<Scan> getAllFragmentScans();

  /**
   * Returns the most intense isotope pattern in this row. If there are no isotope patterns present
   * in the row, returns null.
   */
  IsotopePattern getBestIsotopePattern();

  @NotNull FeatureList getFeatureList();

  /**
   * @return A list of all compound annotations.
   */
  @NotNull List<CompoundDBAnnotation> getCompoundAnnotations();

  /**
   * @param annotations sets all compound annotations.
   */
  void setCompoundAnnotations(List<CompoundDBAnnotation> annotations);

  /**
   * Appends a compound annotation.
   *
   * @param id
   */
  void addCompoundAnnotation(CompoundDBAnnotation id);

  void addSpectralLibraryMatch(SpectralDBAnnotation id);

  boolean isIdentified();

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
    return g == null ? -1 : g.getGroupID();
  }

  List<ResultFormula> getFormulas();

  void setFormulas(List<ResultFormula> formulas);

  void addFormula(ResultFormula formula, boolean preferred);

  /**
   * Checks if MS2 fragmentation data is available
   *
   * @return true if this row has at least 1 MS2 spectrum
   */
  default boolean hasMs2Fragmentation() {
    // should be faster. Best fragmentation loops through all spectra to find best  
    // all fragment scans copies from all features
    return streamFeatures().anyMatch(Feature::hasMs2Fragmentation);
  }

  /**
   * The intensity summed over all features
   *
   * @return sum of all feature heights
   */
  default double getSumIntensity() {
    return this.getFeatures().stream().filter(Objects::nonNull)
        .filter(f -> f.getFeatureStatus() != FeatureStatus.UNKNOWN).map(Feature::getHeight)
        .filter(Objects::nonNull).mapToDouble(Float::doubleValue).sum();
  }

  /**
   * Set and override the list of matches
   *
   * @param matches new list of matches
   */
  void setSpectralLibraryMatch(List<SpectralDBAnnotation> matches);

  /**
   * List of library matches sorted from best (index 0) to last match
   *
   * @return list of library matches or an empty list
   */
  @NotNull List<SpectralDBAnnotation> getSpectralLibraryMatches();

  /**
   * Add annotations from lipid search
   *
   * @param matchedLipid the matched lipid
   */
  void addLipidAnnotation(MatchedLipid matchedLipid);

  // -- ModularFeatureListRow additions
  Stream<ModularFeature> streamFeatures();

  void addSpectralLibraryMatches(List<SpectralDBAnnotation> matches);

  @Nullable Range<Float> getMobilityRange();

  /**
   * Checks for an isotope pattern with >1 data points (main signal plus 1)
   *
   * @return true if isotope pattern available with at least two signals
   */
  boolean hasIsotopePattern();

  /**
   * Uses {@link FeatureAnnotationPriority} to find the best annotation from different methods
   *
   * @return the preferred annotation or null
   */
  @Nullable Object getPreferredAnnotation();


  @NotNull
  default Stream<Object> streamAllFeatureAnnotations() {
    return new FeatureAnnotationIterator(this).stream();
  }

  @NotNull
  default List<Object> getAllFeatureAnnotations() {
    return streamAllFeatureAnnotations().toList();
  }

  /**
   * Uses {@link #getPreferredAnnotation()}
   *
   * @return preferred compound name
   */
  @Nullable String getPreferredAnnotationName();

  /**
   * @return The polarity of this row. Based on {@link Feature#getRepresentativeScan()}.
   */
  @Nullable
  default PolarityType getRepresentativePolarity() {
    final Feature bestFeature = getBestFeature();
    if (bestFeature != null && bestFeature.getRepresentativePolarity() != null) {
      return bestFeature.getRepresentativePolarity();
    }
    return streamFeatures().sorted(Comparator.comparingDouble(Feature::getHeight).reversed())
        .map(Feature::getRepresentativePolarity).filter(Objects::nonNull).findFirst().orElse(null);
  }

}

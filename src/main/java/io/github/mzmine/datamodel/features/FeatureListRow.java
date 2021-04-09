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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.features;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import javafx.collections.ObservableList;

/**
 * Interface representing feature list row
 */
public interface FeatureListRow {

  /**
   * Return raw data with features on this row
   */
  public ObservableList<RawDataFile> getRawDataFiles();

  /**
   * Returns ID of this row
   */
  public int getID();

  /**
   * Returns number of features assigned to this row
   */
  public int getNumberOfFeatures();

  /**
   * Return features assigned to this row
   */
  public ObservableList<Feature> getFeatures();

  /**
   * Returns feature for given raw data file
   */
  public Feature getFeature(RawDataFile rawData);

  /**
   * Add a feature
   */
  public void addFeature(RawDataFile rawData, Feature feature);

  /**
   * Remove a feature
   */
  public void removeFeature(RawDataFile file);

  /**
   * Has a feature?
   */
  public boolean hasFeature(Feature feature);

  /**
   * Has a feature?
   */
  public boolean hasFeature(RawDataFile rawData);

  /**
   * Returns average M/Z for features on this row
   */
  public double getAverageMZ();

  /**
   * Returns average RT for features on this row
   */
  public float getAverageRT();

  /**
   * Returns average mobility for features on this row
   */
  float getAverageMobility();

  /**
   * Returns average height for features on this row
   */
  public double getAverageHeight();

  /**
   * Returns the charge for feature on this row. If more charges are found 0 is returned
   */
  public int getRowCharge();

  /**
   * Returns average area for features on this row
   */
  public double getAverageArea();

  /**
   * Returns comment for this row
   */
  public String getComment();

  /**
   * Sets comment for this row
   */
  public void setComment(String comment);

  /**
   * Sets average mz for this row
   */
  public void setAverageMZ(double averageMZ);

  /**
   * Sets average rt for this row
   */
  public void setAverageRT(float averageRT);

  /**
   * Add a new identity candidate (result of identification method)
   *
   * @param identity New feature identity
   * @param preffered boolean value to define this identity as preferred identity
   */
  public void addFeatureIdentity(FeatureIdentity identity, boolean preffered);

  /**
   * Remove identity candidate
   *
   * @param identity Feature identity
   */
  public void removeFeatureIdentity(FeatureIdentity identity);

  /**
   * Returns all candidates for this feature's identity
   *
   * @return Identity candidates
   */
  public ObservableList<FeatureIdentity> getPeakIdentities();

  /**
   * Returns preferred feature identity among candidates
   *
   * @return Preferred identity
   */
  public FeatureIdentity getPreferredFeatureIdentity();

  /**
   * Sets a preferred feature identity among candidates
   *
   * @param identity Preferred identity
   */
  public void setPreferredFeatureIdentity(FeatureIdentity identity);

  /**
   * Adds a new FeatureInformation object.
   *
   * FeatureInformation is used to keep extra information about features in the form of a map
   * <propertyName, propertyValue>
   *
   * @param featureInformation object
   */

  public void setFeatureInformation(FeatureInformation featureInformation);

  /**
   * Returns FeatureInformation
   *
   * @return
   */

  public FeatureInformation getFeatureInformation();

  /**
   * Returns maximum raw data point intensity among all features in this row
   *
   * @return Maximum intensity
   */
  public double getMaxDataPointIntensity();

  /**
   * Returns the most intense feature in this row
   */
  public Feature getBestFeature();

  /**
   * Returns the most intense fragmentation scan in this row
   */
  public Scan getBestFragmentation();

  /**
   * Returns all fragmentation scans of this row
   */
  @Nonnull
  public ObservableList<Scan> getAllMS2Fragmentations();

  /**
   * Returns the most intense isotope pattern in this row. If there are no isotope patterns present
   * in the row, returns null.
   */
  public IsotopePattern getBestIsotopePattern();

  /**
   * reset the rowID
   */
  public void setID(int id);

  @Nullable
  FeatureList getFeatureList();

  void setFeatureList(@Nonnull FeatureList flist);

  default void addSpectralLibraryMatch(SpectralDBFeatureIdentity id) {
    addFeatureIdentity(id, false);
  }

  void addLipidAnnotation(MatchedLipid matchedLipid);

}

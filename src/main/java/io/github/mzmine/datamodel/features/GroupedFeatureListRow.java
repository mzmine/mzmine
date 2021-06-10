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

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.types.FeatureGroupType.GroupType;
import java.util.List;
import javafx.collections.ObservableList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * GroupedFeatureListRows always have a main (representative) row and sub rows to build a tree
 * structure
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public abstract class GroupedFeatureListRow implements FeatureListRow {

  private GroupType groupType;
  private List<FeatureListRow> subRows;
  private FeatureListRow mainRow;


  public GroupedFeatureListRow(GroupType groupType, List<FeatureListRow> subRows,
      FeatureListRow mainRow) {
    this.groupType = groupType;
    this.subRows = subRows;
    this.mainRow = mainRow;
  }

  public GroupType getGroupType() {
    return groupType;
  }

  public List<FeatureListRow> getSubRows() {
    return subRows;
  }

  public FeatureListRow getMainRow() {
    return mainRow;
  }

  public void setMainRow(FeatureListRow mainRow) {
    this.mainRow = mainRow;
  }

  @Override
  public ObservableList<RawDataFile> getRawDataFiles() {
    return mainRow.getRawDataFiles();
  }

  @Override
  public int getID() {
    return mainRow.getID();
  }

  @Override
  public int getNumberOfFeatures() {
    return mainRow.getNumberOfFeatures();
  }

  @Override
  public ObservableList<Feature> getFeatures() {
    return mainRow.getFeatures();
  }

  @Override
  public Feature getFeature(RawDataFile rawData) {
    return mainRow.getFeature(rawData);
  }

  @Override
  public void addFeature(RawDataFile rawData, Feature feature) {
    mainRow.addFeature(rawData, feature);
  }

  @Override
  public void removeFeature(RawDataFile file) {
    mainRow.removeFeature(file);
  }

  @Override
  public boolean hasFeature(Feature feature) {
    return mainRow.hasFeature(feature);
  }

  @Override
  public boolean hasFeature(RawDataFile rawData) {
    return mainRow.hasFeature(rawData);
  }

  @Override
  public double getAverageMZ() {
    return mainRow.getAverageMZ();
  }

  @Override
  public float getAverageRT() {
    return mainRow.getAverageRT();
  }

  @Override
  public double getAverageHeight() {
    return mainRow.getAverageHeight();
  }

  @Override
  public int getRowCharge() {
    return mainRow.getRowCharge();
  }

  @Override
  public double getAverageArea() {
    return mainRow.getAverageArea();
  }

  @Override
  public String getComment() {
    return mainRow.getComment();
  }

  @Override
  public void setComment(String comment) {
    mainRow.setComment(comment);
  }

  @Override
  public void setAverageMZ(double averageMZ) {
    mainRow.setAverageMZ(averageMZ);
  }

  @Override
  public void setAverageRT(float averageRT) {
    mainRow.setAverageRT(averageRT);
  }

  @Override
  public void addFeatureIdentity(FeatureIdentity identity, boolean preffered) {
    mainRow.addFeatureIdentity(identity, preffered);
  }

  @Override
  public void removeFeatureIdentity(FeatureIdentity identity) {
    mainRow.removeFeatureIdentity(identity);
  }

  @Override
  public ObservableList<FeatureIdentity> getPeakIdentities() {
    return mainRow.getPeakIdentities();
  }

  @Override
  public FeatureIdentity getPreferredFeatureIdentity() {
    return mainRow.getPreferredFeatureIdentity();
  }

  @Override
  public void setPreferredFeatureIdentity(FeatureIdentity identity) {
    mainRow.setPreferredFeatureIdentity(identity);
  }

  @Override
  public void setFeatureInformation(FeatureInformation featureInformation) {
    mainRow.setFeatureInformation(featureInformation);
  }

  @Override
  public FeatureInformation getFeatureInformation() {
    return mainRow.getFeatureInformation();
  }

  @Override
  public double getMaxDataPointIntensity() {
    return mainRow.getMaxDataPointIntensity();
  }

  @Override
  public Feature getBestFeature() {
    return mainRow.getBestFeature();
  }

  @Override
  public Scan getBestFragmentation() {
    return mainRow.getBestFragmentation();
  }

  @Nonnull
  @Override
  public ObservableList<Scan> getAllMS2Fragmentations() {
    return mainRow.getAllMS2Fragmentations();
  }

  @Override
  public IsotopePattern getBestIsotopePattern() {
    return mainRow.getBestIsotopePattern();
  }

  @Nullable
  @Override
  public FeatureList getFeatureList() {
    return mainRow.getFeatureList();
  }

  @Override
  public void setFeatureList(@Nonnull FeatureList flist) {
    mainRow.setFeatureList(flist);
  }

  @Override
  public void setGroup(RowGroup group) {
    mainRow.setGroup(group);
  }

  @Override
  public RowGroup getGroup() {
    return mainRow.getGroup();
  }
}

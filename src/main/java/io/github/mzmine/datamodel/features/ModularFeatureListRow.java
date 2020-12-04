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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.types.AreaBarType;
import io.github.mzmine.datamodel.features.types.AreaShareType;
import io.github.mzmine.datamodel.features.types.FeatureShapeType;
import io.github.mzmine.datamodel.features.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.util.FeatureSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javafx.collections.ObservableList;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DetectionType;
import io.github.mzmine.datamodel.features.types.FeaturesType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import javafx.beans.property.MapProperty;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javax.annotation.Nullable;

/**
 * Map of all feature related data.
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 * <p>
 * TODO: I think the RawFileType should also be in the map and not just accessible via the key set
 *  of {@link ModularFeatureListRow#getFilesFeatures}. -> add during fueature list creation in the
 *  chromatogram builder ~SteffenHeu
 */
@SuppressWarnings("rawtypes")
public class ModularFeatureListRow implements FeatureListRow, ModularDataModel {

  private @Nonnull ModularFeatureList flist;
  /**
   * this final map is used in the FeaturesType - only ModularFeatureListRow is supposed to change
   * this map see {@link #addFeature}
   */
  private final ObservableMap<DataType, Property<?>> map =
      FXCollections.observableMap(new HashMap<>());

  private final Map<RawDataFile, ModularFeature> features;

  // buffert col charts and nodes
  private final Map<String, Node> buffertColCharts = new HashMap<>();

  private ObservableList<FeatureIdentity> identities = FXCollections.observableArrayList();
  private FeatureIdentity preferredIdentity;
  private String comment;
  private double maxDataPointIntensity;

  /**
   * These variables are used for caching the average values, so we don't need to calculate them
   * again and again
   */
  private double averageMZ, averageHeight, averageArea;
  private float averageRT;
  private int rowCharge;

  private FeatureInformation featureInformation;

  public ModularFeatureListRow(@Nonnull ModularFeatureList flist) {
    this.flist = flist;
    // add type property columns to maps
    flist.getRowTypes().values().forEach(type -> {
      this.setProperty(type, type.createProperty());
    });

    List<RawDataFile> raws = flist.getRawDataFiles();
    if (!raws.isEmpty()) {
      // init FeaturesType map (is final)
      HashMap<RawDataFile, ModularFeature> fmap = new HashMap<>(raws.size());
      for (RawDataFile r : raws) {
        fmap.put(r, new ModularFeature(flist));
      }
      features = FXCollections.observableMap(FXCollections.observableMap(fmap));
      // set
      set(FeaturesType.class, features);

      // TODO: MapProperty/Map? change DataTypeCellValueFactory? Bind to features?
      //set(FeatureShapeType.class, getFeatures());
      set(FeatureShapeType.class, getFeaturesProperty());
      set(AreaBarType.class, getFeaturesProperty());
      set(AreaShareType.class, getFeaturesProperty());
    } else {
      features = Collections.emptyMap();
    }
  }

  /**
   * Constructor for row with only one raw data file.
   *
   * @param flist
   * @param id
   * @param raw
   * @param p
   */
  public ModularFeatureListRow(@Nonnull ModularFeatureList flist, int id, RawDataFile raw,
      Feature p) {
    this(flist);
    set(IDType.class, (id));
    addFeature(raw, p);
  }

  /**
   * Constructor for row with only one feature.
   *
   * @param flist
   * @param id
   * @param feature
   */
  public ModularFeatureListRow(@Nonnull ModularFeatureList flist, int id, Feature feature) {
    this(flist);
    set(IDType.class, (id));
    addFeature(feature.getRawDataFile(), feature);
  }

  /**
   * Constructor for row with a specific id.
   *
   * @param flist Feature list
   * @param id ID
   */
  public ModularFeatureListRow(@Nonnull ModularFeatureList flist, int id) {
    this(flist);
    set(IDType.class, (id));
  }

  @Override
  public ObservableMap<Class<? extends DataType>, DataType> getTypes() {
    return flist.getRowTypes();
  }

  @Override
  public ObservableMap<DataType, Property<?>> getMap() {
    return map;
  }

  public Stream<Feature> streamFeatures() {
    return this.getFeatures().stream().filter(Objects::nonNull);
  }

  // Helper methods
  // most common data types
  public FeatureStatus getDetectionType() {
    return get(DetectionType.class).getValue();
  }

  public Range<Double> getMZRange() {
    return get(MZRangeType.class).getValue();
  }

  public float getHeight() {
    return get(HeightType.class).getValue();
  }

  public float getArea() {
    return get(AreaType.class).getValue();
  }

  public ObservableMap<RawDataFile, ModularFeature> getFilesFeatures() {
    return get(FeaturesType.class).getValue();
  }

  public ObservableList<Feature> getFeatures() {
    //return FXCollections.observableArrayList(get(FeaturesType.class).getValue().values());
    return FXCollections.observableArrayList(features.values());
  }

  public MapProperty<RawDataFile, ModularFeature> getFeaturesProperty() {
    return get(FeaturesType.class);
  }

  /**
   * @param raw
   * @param feature
   */
  @Override
  public void addFeature(RawDataFile raw, Feature feature) {
    if(!(feature instanceof ModularFeature)) {
      throw new IllegalArgumentException("Cannot add non-modular feature to modular feature list row.");
    }
    ModularFeature modularFeature = (ModularFeature) feature;

    /*
    if (Objects.equals(modularFeature.getFeatureList(), getFeatureList())) {
      // features are final - replace all values for all data types
      // keep old feature
      ModularFeature old = getFilesFeatures().get(raw);
      for (DataType type : flist.getFeatureTypes().values()) {
        old.set(type, modularFeature.get(type).getValue());
      }
    } else {
      features.put(raw, modularFeature);
    }
    */
    if(hasFeature(raw)) {
      ModularFeature old = getFeature(raw);
      for (DataType<?> type : flist.getFeatureTypes().values()) {
        old.set(type, modularFeature.get(type).getValue());
      }
    } else {
      features.put(raw, modularFeature);
    }

    modularFeature.setFeatureList(flist);

    if (modularFeature.getRawDataPointsIntensityRange().upperEndpoint() > maxDataPointIntensity)
      maxDataPointIntensity = modularFeature.getRawDataPointsIntensityRange().upperEndpoint();

    calculateAverageValues();
  }

  /**
   * Row ID or -1 if not present
   *
   * @return
   */
  public int getID() {
    Property<Integer> idProp = get(IDType.class);
    return idProp == null || idProp.getValue() == null ? -1 : get(IDType.class).getValue();
  }


  @Override
  public int getNumberOfFeatures() {
    return features.size();
  }

  @Override
  public void removeFeature(RawDataFile file) {
    this.features.remove(file);
    calculateAverageValues();
  }

  @Override
  public double getAverageMZ() {
    return averageMZ;
  }

  @Override
  public float getAverageRT() {
    return averageRT;
  }

  @Override
  public double getAverageHeight() {
    return averageHeight;
  }

  @Override
  public int getRowCharge() {
    return rowCharge;
  }

  @Override
  public double getAverageArea() {
    return averageArea;
  }

  @Override
  public ObservableList<RawDataFile> getRawDataFiles() {
    return flist.getRawDataFiles();
  }

  @Override
  public boolean hasFeature(RawDataFile rawData) {
    return features.containsKey(rawData);
  }

  @Override
  public boolean hasFeature(Feature feature) {
    if (!(feature instanceof ModularFeature)) {
      //throw new IllegalArgumentException("Modular feature list row can not contain non-modular feature.");
      return false;
    }
    return features.containsValue((ModularFeature) feature);
  }

  public Node getBufferedColChart(String colname) {
    return buffertColCharts.get(colname);
  }

  public void addBufferedColChart(String colname, Node node) {
    buffertColCharts.put(colname, node);
  }

  /**
   * nonnull if this feature list contains this raw data file. Even if there is no feature in this
   * raw data file
   *
   * @param raw
   * @return
   */
  @Override
  public ModularFeature getFeature(RawDataFile raw) {
    return features.get(raw);
  }

  public void setID(int id) {
    set(IDType.class, id);
  }

  @Nullable
  @Override
  public ModularFeatureList getFeatureList() {
    return flist;
  }

  @Override
  public void setFeatureList(@Nonnull FeatureList flist) {
    if(!(flist instanceof ModularFeatureList)) {
      throw new IllegalArgumentException("Cannot set non-modular feature list to modular feature list row.");
    }
    this.flist = (ModularFeatureList) flist;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @Override
  public void setAverageMZ(double averageMZ) {
    this.averageMZ = averageMZ;
  }

  @Override
  public void setAverageRT(float averageRT) {
    this.averageRT = averageRT;
  }

  @Override
  public ObservableList<FeatureIdentity> getPeakIdentities() {
    return identities;
  }

  public void setPeakIdentities(ObservableList<FeatureIdentity> identities) {
    this.identities = identities;
  }

  @Override
  public void addFeatureIdentity(FeatureIdentity identity, boolean preferred) {
    // Verify if exists already an identity with the same name
    for (FeatureIdentity testId : identities) {
      if (testId.getName().equals(identity.getName())) {
        return;
      }
    }

    identities.add(identity);
    if ((preferredIdentity == null) || (preferred)) {
      setPreferredFeatureIdentity(identity);
    }
  }

  @Override
  public void removeFeatureIdentity(FeatureIdentity identity) {
    identities.remove(identity);
    if (preferredIdentity == identity) {
      if (identities.size() > 0) {
        FeatureIdentity[] identitiesArray = identities.toArray(new FeatureIdentity[0]);
        setPreferredFeatureIdentity(identitiesArray[0]);
      } else
        preferredIdentity = null;
    }
  }

  public FeatureIdentity getPreferredFeatureIdentity() {
    return preferredIdentity;
  }

  public void setPreferredFeatureIdentity(FeatureIdentity preferredIdentity) {
    this.preferredIdentity = preferredIdentity;
  }

  @Override
  public void setFeatureInformation(FeatureInformation featureInformation) {
    this.featureInformation = featureInformation;
  }

  @Override
  public FeatureInformation getFeatureInformation() {
    return featureInformation;
  }

  @Override
  public double getMaxDataPointIntensity() {
    return maxDataPointIntensity;
  }

  @Nullable
  @Override
  public ModularFeature getBestFeature() {
    ModularFeature features[] = getFeatures().toArray(new ModularFeature[0]);
    Arrays.sort(features, new FeatureSorter(SortingProperty.Height, SortingDirection.Descending));
    if (features.length == 0) {
      return null;
    }
    return features[0];
  }

  @Override
  public Scan getBestFragmentation() {
    double bestTIC = 0.0;
    Scan bestScan = null;
    for (Feature feature : getFeatures()) {
      double theTIC = 0.0;
      RawDataFile rawData = feature.getRawDataFile();
      int bestScanNumber = feature.getMostIntenseFragmentScanNumber();
      Scan theScan = rawData.getScan(bestScanNumber);
      if (theScan != null) {
        theTIC = theScan.getTIC();
      }

      if (theTIC > bestTIC) {
        bestTIC = theTIC;
        bestScan = theScan;
      }
    }
    return bestScan;
  }

  @Nonnull
  @Override
  public ObservableList<Scan> getAllMS2Fragmentations() {
    ObservableList<Scan> allMS2ScansList = FXCollections.observableArrayList();
    for (Feature feature : getFeatures()) {
      RawDataFile rawData = feature.getRawDataFile();
      ObservableList<Integer> scanNumbers = feature.getAllMS2FragmentScanNumbers();
      if (scanNumbers != null) {
        for (int scanNumber : scanNumbers) {
          Scan scan = rawData.getScan(scanNumber);
          allMS2ScansList.add(scan);
        }
      }
    }

    return allMS2ScansList;
  }

  @Nullable
  @Override
  public IsotopePattern getBestIsotopePattern() {
    ModularFeature[] features = getFilesFeatures().values().toArray(new ModularFeature[0]);
    Arrays.sort(features, new FeatureSorter(SortingProperty.Height, SortingDirection.Descending));

    for (ModularFeature feature : features) {
      IsotopePattern ip = feature.getIsotopePattern();
      if (ip != null)
        return ip;
    }

    return null;
  }

  // TODO: increase speed(enumeration through features)
  private /*synchronized*/ void calculateAverageValues() {
    double mzSum = 0;
    float rtSum = 0, heightSum = 0, areaSum = 0;
    int charge = 0;
    HashSet<Integer> chargeArr = new HashSet<Integer>();
    for (Feature feature : getFeatures()) {
      // Alignned feature list rows can contain "empty" features
      if(feature.getRawDataFile() == null) {
        continue;
      }
      rtSum += feature.getRT();
      mzSum += feature.getMZ();
      heightSum += feature.getHeight();
      areaSum += feature.getArea();
      if (feature.getCharge() > 0) {
        chargeArr.add(feature.getCharge());
        charge = feature.getCharge();
      }
    }
    averageRT = rtSum / features.size();
    averageMZ = mzSum / features.size();
    averageHeight = heightSum / features.size();
    averageArea = areaSum / features.size();
    if (chargeArr.size() < 2) {
      rowCharge = charge;
    } else {
      rowCharge = 0;
    }
  }
}

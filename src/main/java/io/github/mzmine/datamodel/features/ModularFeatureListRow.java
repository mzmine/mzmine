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
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.types.CommentType;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DetectionType;
import io.github.mzmine.datamodel.features.types.FeatureInformationType;
import io.github.mzmine.datamodel.features.types.FeaturesType;
import io.github.mzmine.datamodel.features.types.IdentityType;
import io.github.mzmine.datamodel.features.types.ManualAnnotationType;
import io.github.mzmine.datamodel.features.types.ModularTypeProperty;
import io.github.mzmine.datamodel.features.types.SpectralLibMatchSummaryType;
import io.github.mzmine.datamodel.features.types.SpectralLibraryMatchType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.util.FeatureSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Map of all feature related data.
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *         <p>
 *         TODO: I think the RawFileType should also be in the map and not just accessible via the
 *         key set of {@link ModularFeatureListRow#getFilesFeatures}. -> add during fueature list
 *         creation in the chromatogram builder ~SteffenHeu
 */
@SuppressWarnings("rawtypes")
public class ModularFeatureListRow implements FeatureListRow, ModularDataModel {

  @Nonnull
  private ModularFeatureList flist;
  /**
   * this final map is used in the FeaturesType - only ModularFeatureListRow is supposed to change
   * this map see {@link #addFeature}
   */
  private final ObservableMap<DataType, Property<?>> map =
      FXCollections.observableMap(new HashMap<>());

  private final Map<RawDataFile, ModularFeature> features;

  // buffert col charts and nodes
  private final Map<String, Node> buffertColCharts = new HashMap<>();

  public ModularFeatureListRow(@Nonnull ModularFeatureList flist) {
    this(flist, null, false);
  }

  public ModularFeatureListRow(@Nonnull ModularFeatureList flist, ModularFeatureListRow row,
      boolean copyFeatures) {
    this.flist = flist;
    // add type property columns to maps
    flist.getRowTypes().values().forEach(type -> {
      this.setProperty(type, type.createProperty());
    });

    // register listener to types map to automatically generate default properties for new DataTypes
    flist.getRowTypes().addListener(
        (MapChangeListener<? super Class<? extends DataType>, ? super DataType>) change -> {
          if (change.wasAdded()) {
            // add type columns to maps
            DataType type = change.getValueAdded();
            this.setProperty(type, type.createProperty());
          } else if (change.wasRemoved()) {
            // remove type columns to maps
            DataType<Property<?>> type = change.getValueRemoved();
            this.removeProperty((Class<DataType<Property<?>>>) type.getClass());
          }
        });

    // copy all but features
    if (row != null) {
      row.stream().filter(e -> !(e.getKey() instanceof FeaturesType))
          .forEach(entry -> this.set(entry.getKey(), entry.getValue()));
    }

    // features
    List<RawDataFile> raws = flist.getRawDataFiles();
    if (!raws.isEmpty()) {
      // init FeaturesType map (is final)
      HashMap<RawDataFile, ModularFeature> fmap = new HashMap<>(raws.size());
      for (RawDataFile r : raws) {
        fmap.put(r, new ModularFeature(flist));
      }
      features = (FXCollections.observableMap(fmap));
      // set
      set(FeaturesType.class, features);
    } else {
      features = Collections.emptyMap();
    }

    if (copyFeatures) {
      // Copy the features.
      for (final Feature feature : row.getFeatures()) {
        this.addFeature(feature.getRawDataFile(), new ModularFeature(flist, feature));
      }
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
   * @param id    ID
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


  @Override
  public <T extends Property<?>> void set(Class<? extends DataType<T>> tclass, Object value) {
    // type in defined columns?
    if (!getTypes().containsKey(tclass)) {
      try {
        DataType newType = tclass.getConstructor().newInstance();
        ModularFeatureList flist = getFeatureList();
        flist.addRowType(newType);
      } catch (NullPointerException | InstantiationException | NoSuchMethodException
          | InvocationTargetException | IllegalAccessException e) {
        e.printStackTrace();
        return;
      }
    }
    // access default method
    ModularDataModel.super.set(tclass, value);

    //
    if (tclass.equals(FeaturesType.class)) {
      get(FeaturesType.class).addListener(
          (MapChangeListener<RawDataFile, ModularFeature>) change -> {
            flist.getRowBindings().forEach(b -> b.apply(this));
          });
    }
  }


  public Stream<ModularFeature> streamFeatures() {
    return this.getFeatures().stream().map(ModularFeature.class::cast).filter(Objects::nonNull);
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

  @Override
  public ObservableList<Feature> getFeatures() {
    // TODO remove features object - not always do we have features
    // FeaturesType creates an empty ListProperty for that
    // return FXCollections.observableArrayList(get(FeaturesType.class).getValue().values());
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
    if (!(feature instanceof ModularFeature)) {
      throw new IllegalArgumentException(
          "Cannot add non-modular feature to modular feature list row.");
    }
    if (!flist.equals(feature.getFeatureList())) {
      throw new IllegalArgumentException("Cannot add feature with different feature list to this "
          + "row. Create feature with the correct feature list as an argument.");
    }
    ModularFeature modularFeature = (ModularFeature) feature;
    /*
     * if (Objects.equals(modularFeature.getFeatureList(), getFeatureList())) { // features are
     * final - replace all values for all data types // keep old feature ModularFeature old =
     * getFilesFeatures().get(raw); for (DataType type : flist.getFeatureTypes().values()) {
     * old.set(type, modularFeature.get(type).getValue()); } } else { features.put(raw,
     * modularFeature); }
     */
    if (hasFeature(raw)) {
      ModularFeature old = getFeature(raw);
      for (DataType<?> type : flist.getFeatureTypes().values()) {
        old.set(type, modularFeature.get(type).getValue());
      }
    } else {
      features.put(raw, modularFeature);
    }
    modularFeature.setFeatureList(flist);
  }

  /**
   * Row ID or -1 if not present
   *
   * @return
   */
  @Override
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
  }

  @Override
  public double getAverageMZ() {
    if (!hasTypeColumn(MZType.class)) {
      return Double.NaN;
    }
    return get(MZType.class).getValue();
  }

  @Override
  public float getAverageRT() {
    if (!hasTypeColumn(RTType.class)) {
      return Float.NaN;
    }
    return get(RTType.class).getValue();
  }

  @Override
  public double getAverageHeight() {
    if (!hasTypeColumn(HeightType.class)) {
      return Double.NaN;
    }
    return get(HeightType.class).getValue();
  }

  @Override
  public int getRowCharge() {
    if (!hasTypeColumn(ChargeType.class)) {
      return 0;
    }
    return get(ChargeType.class).getValue();
  }

  @Override
  public double getAverageArea() {
    if (!hasTypeColumn(AreaType.class)) {
      return Double.NaN;
    }
    return get(AreaType.class).getValue();
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
      // throw new IllegalArgumentException("Modular feature list row can not contain non-modular
      // feature.");
      return false;
    }
    return features.containsValue(feature);
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

  @Override
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
    if (!(flist instanceof ModularFeatureList)) {
      throw new IllegalArgumentException(
          "Cannot set non-modular feature list to modular feature list row.");
    }
    this.flist = (ModularFeatureList) flist;
  }

  @Override
  public String getComment() {
    ModularTypeProperty manual = getManualAnnotation();
    if (manual != null) {
      return manual.get(CommentType.class).getValue();
    } else if (hasTypeColumn(CommentType.class)) {
      return get(CommentType.class).getValue();
    } else {
      return "";
    }
  }

  public ModularTypeProperty getManualAnnotation() {
    return get(ManualAnnotationType.class);
  }

  @Override
  public void setComment(String comment) {
    set(CommentType.class, comment);
  }

  @Override
  public void setAverageMZ(double averageMZ) {
    // binding
  }

  @Override
  public void setAverageRT(float averageRT) {
    // binding
  }

  @Override
  public ObservableList<FeatureIdentity> getPeakIdentities() {
    ModularTypeProperty manual = getManualAnnotation();
    if (manual != null) {
      return manual.get(IdentityType.class).getValue();
    } else if (hasTypeColumn(IdentityType.class)) {
      return get(IdentityType.class).getValue();
    } else {
      return FXCollections.emptyObservableList();
    }
  }

  public void setPeakIdentities(ObservableList<FeatureIdentity> identities) {
    set(IdentityType.class, identities);
  }

  @Override
  public void addFeatureIdentity(FeatureIdentity identity, boolean preferred) {
    if (!hasTypeColumn(IdentityType.class)) {
      getFeatureList().addRowType(new IdentityType());
    }
    // Verify if exists already an identity with the same name
    ObservableList<FeatureIdentity> peakIdentities = getPeakIdentities();
    for (FeatureIdentity testId : peakIdentities) {
      if (testId.getName().equals(identity.getName())) {
        return;
      }
    }

    if (preferred) {
      peakIdentities.add(0, identity);
    } else {
      peakIdentities.add(identity);
    }
  }

  @Override
  public void addSpectralLibraryMatch(SpectralDBFeatureIdentity id) {
    // add column first if needed
    get(SpectralLibraryMatchType.class).get(SpectralLibMatchSummaryType.class).add(id);
  }

  @Override
  public void removeFeatureIdentity(FeatureIdentity identity) {
    ObservableList<FeatureIdentity> identities = getPeakIdentities();
    identities.remove(identity);
  }

  @Override
  public FeatureIdentity getPreferredFeatureIdentity() {
    return getPeakIdentities().stream().findFirst().orElse(null);
  }

  @Override
  public void setPreferredFeatureIdentity(FeatureIdentity preferredIdentity) {
    ObservableList<FeatureIdentity> identities = getPeakIdentities();
    identities.remove(preferredIdentity);
    identities.add(0, preferredIdentity);
  }

  @Override
  public void setFeatureInformation(FeatureInformation featureInformation) {
    set(FeatureInformationType.class, featureInformation);
  }

  @Override
  public FeatureInformation getFeatureInformation() {
    if (!hasTypeColumn(FeatureInformationType.class)) {
      return null;
    }
    return (FeatureInformation) get(FeatureInformationType.class).getValue();
  }

  @Override
  public double getMaxDataPointIntensity() {
    if (!hasTypeColumn(IntensityRangeType.class)) {
      return Double.NaN;
    }
    ObjectProperty<Range<Float>> rangeObjectProperty = get(IntensityRangeType.class);
    return rangeObjectProperty.getValue() != null ?
        rangeObjectProperty.getValue().upperEndpoint() : 0;
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
      if (ip != null) {
        return ip;
      }
    }

    return null;
  }

  // TODO: increase speed(enumeration through features)
  // private synchronized void calculateAverageValues() {
  // double mzSum = 0;
  // float rtSum = 0, heightSum = 0, areaSum = 0;
  // int charge = 0;
  // HashSet<Integer> chargeArr = new HashSet<Integer>();
  // for (Feature feature : getFeatures()) {
  // // Alignned feature list rows can contain "empty" features
  // if (feature.getRawDataFile() == null) {
  // continue;
  // }
  // rtSum += feature.getRT();
  // mzSum += feature.getMZ();
  // heightSum += feature.getHeight();
  // areaSum += feature.getArea();
  // if (feature.getCharge() > 0) {
  // chargeArr.add(feature.getCharge());
  // charge = feature.getCharge();
  // }
  // }
  // averageRT = rtSum / features.size();
  // averageMZ = mzSum / features.size();
  // averageHeight = heightSum / features.size();
  // averageArea = areaSum / features.size();
  // if (chargeArr.size() < 2) {
  // rowCharge = charge;
  // } else {
  // rowCharge = 0;
  // }
  // }
}

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

package io.github.mzmine.datamodel.data;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PeakIdentity;
import io.github.mzmine.datamodel.PeakInformation;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data.types.AreaBarType;
import io.github.mzmine.datamodel.data.types.AreaShareType;
import io.github.mzmine.datamodel.data.types.FeatureShapeType;
import io.github.mzmine.datamodel.data.types.numbers.MZExpandingType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javafx.util.Pair;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.FeaturesType;
import io.github.mzmine.datamodel.data.types.exceptions.WrongFeatureListException;
import io.github.mzmine.datamodel.data.types.numbers.AreaType;
import io.github.mzmine.datamodel.data.types.numbers.HeightType;
import io.github.mzmine.datamodel.data.types.numbers.IDType;
import io.github.mzmine.datamodel.data.types.numbers.RTType;
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
 *  of {@link ModularFeatureListRow#getFeatures}. -> add during fueature list creation in the
 *  chromatogram builder ~SteffenHeu
 */
@SuppressWarnings("rawtypes")
public class ModularFeatureListRow implements FeatureListRow, ModularDataModel {

  private final @Nonnull
  ModularFeatureList flist;
  private final ObservableMap<DataType, Property<?>> map =
      FXCollections.observableMap(new HashMap<>());

  private List<PeakIdentity> identities;
  private PeakIdentity preferredIdentity;
  private String comment;

  /**
   * this final map is used in the FeaturesType - only ModularFeatureListRow is supposed to change
   * this map see {@link #addPeak(RawDataFile, ModularFeature)}
   */
  private final Map<RawDataFile, ModularFeature> features;

  // buffert col charts and nodes
  private Map<String, Node> buffertColCharts = new HashMap<>();

  public ModularFeatureListRow(@Nonnull ModularFeatureList flist) {
    this.flist = flist;
    // add type property columns to maps
    flist.getRowTypes().values().forEach(type -> {
      this.setProperty(type, type.createProperty());
    });

    set(MZExpandingType.class, new Pair<>(30.0, Range.closed(2, 3)));

    List<RawDataFile> raws = flist.getRawDataFiles();
    if (!raws.isEmpty()) {
      // init FeaturesType map (is final)
      HashMap<RawDataFile, ModularFeature> fmap = new HashMap<>(raws.size());
      for (RawDataFile r : raws) {
        fmap.put(r, new ModularFeature(flist));
      }
      features = FXCollections.unmodifiableObservableMap(FXCollections.observableMap(fmap));
      // set
      set(FeaturesType.class, features);

      // TODO: MapProperty/Map? change DataTypeCellValueFactory?
      //set(FeatureShapeType.class, getFeatures());
      set(FeatureShapeType.class, getFeaturesProperty());
      set(AreaBarType.class,getFeaturesProperty());
      set(AreaShareType.class,getFeaturesProperty());
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
      ModularFeature p) {
    this(flist);
    set(IDType.class, (id));
    addPeak(raw, p);
  }

  public ModularFeatureList getFeatureList() {
    return flist;
  }

  @Override
  public ObservableMap<Class<? extends DataType>, DataType> getTypes() {
    return flist.getRowTypes();
  }

  @Override
  public ObservableMap<DataType, Property<?>> getMap() {
    return map;
  }

  public Stream<ModularFeature> streamFeatures() {
    return this.getFeatures().values().stream().filter(Objects::nonNull);
  }

  // Helper methods
  // most common data types
  public FeatureStatus getDetectionType() {
    return get(DetectionType.class).getValue();
  }

  public double getMZ() {
    return get(MZExpandingType.class).getValue().getKey();
  }

  public Range<Double> getMZRange() {
    return get(MZExpandingType.class).getValue().getValue();
  }

  public float getRT() {
    return get(RTType.class).getValue();
  }

  public float getHeight() {
    return get(HeightType.class).getValue();
  }

  public float getArea() {
    return get(AreaType.class).getValue();
  }

  public ObservableMap<RawDataFile, ModularFeature> getFeatures() {
    return get(FeaturesType.class).getValue();
  }

  public MapProperty<RawDataFile, ModularFeature> getFeaturesProperty() {
    return get(FeaturesType.class);
  }

  /**
   * @param raw
   * @param f
   */
  public void addPeak(RawDataFile raw, ModularFeature f) {
    if (!f.getFeatureList().equals(getFeatureList())) {
      throw new WrongFeatureListException();
    }
    // features are final - replace all values for all data types
    // keep old feature
    ModularFeature old = getFeatures().get(raw);
    for (DataType type : flist.getFeatureTypes().values()) {
      old.set(type, f.get(type).getValue());
    }
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

  // TODO: implement getters

  @Override
  public int getNumberOfPeaks() {
    return 0;
  }

  @Override
  public Feature[] getPeaks() {
    return new Feature[0];
  }

  @Override
  public Feature getPeak(RawDataFile rawData) {
    return null;
  }

  @Override
  public void addPeak(RawDataFile rawData, Feature peak) {

  }

  @Override
  public void removePeak(RawDataFile file) {

  }

  @Override
  public boolean hasPeak(Feature peak) {
    return false;
  }

  @Override
  public boolean hasPeak(RawDataFile rawData) {
    return false;
  }

  @Override
  public double getAverageMZ() {
    return 0;
  }

  @Override
  public double getAverageRT() {
    return 0;
  }

  @Override
  public double getAverageHeight() {
    return 0;
  }

  @Override
  public int getRowCharge() {
    return 0;
  }

  @Override
  public double getAverageArea() {
    return 0;
  }

  public List<RawDataFile> getRawDataFiles() {
    return flist.getRawDataFiles();
  }

  public boolean hasFeature(ModularFeature feature) {
    return getFeatures().values().contains(feature);
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
  public ModularFeature getFeature(RawDataFile raw) {
    return features.get(raw);
  }

  public void setID(int id) {
    set(IDType.class, id);
  }

  @Nullable
  @Override
  public PeakList getPeakList() {
    return null;
  }

  @Override
  public void setPeakList(@Nonnull PeakList peakList) {

  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @Override
  public void setAverageMZ(double mz) {

  }

  @Override
  public void setAverageRT(double rt) {

  }

  public PeakIdentity[] getPeakIdentities() {
    return identities.toArray(new PeakIdentity[0]);
  }

  public void setPeakIdentities(PeakIdentity[] identities) {
    this.identities = Arrays.asList(identities);
  }

  public void addPeakIdentity(PeakIdentity identity, boolean preferred) {
    // Verify if exists already an identity with the same name
    for (PeakIdentity testId : identities) {
      if (testId.getName().equals(identity.getName())) {
        return;
      }
    }

    identities.add(identity);
    if ((preferredIdentity == null) || (preferred)) {
      setPreferredPeakIdentity(identity);
    }
  }

  @Override
  public void removePeakIdentity(PeakIdentity identity) {

  }

  public PeakIdentity getPreferredPeakIdentity() {
    return preferredIdentity;
  }

  public void setPreferredPeakIdentity(PeakIdentity preferredIdentity) {
    this.preferredIdentity = preferredIdentity;
  }

  @Override
  public void setPeakInformation(PeakInformation information) {

  }

  @Override
  public PeakInformation getPeakInformation() {
    return null;
  }

  @Override
  public double getDataPointMaxIntensity() {
    return 0;
  }

  @Override
  public Feature getBestPeak() {
    return null;
  }

  @Override
  public Scan getBestFragmentation() {
    return null;
  }

  @Nonnull
  @Override
  public Scan[] getAllMS2Fragmentations() {
    return new Scan[0];
  }

  @Override
  public IsotopePattern getBestIsotopePattern() {
    return null;
  }
}

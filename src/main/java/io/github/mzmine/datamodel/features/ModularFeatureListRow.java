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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import io.github.mzmine.datamodel.features.types.FeatureGroupType;
import io.github.mzmine.datamodel.features.types.FeatureInformationType;
import io.github.mzmine.datamodel.features.types.FeaturesType;
import io.github.mzmine.datamodel.features.types.FormulaAnnotationType;
import io.github.mzmine.datamodel.features.types.FormulaSummaryType;
import io.github.mzmine.datamodel.features.types.IdentityType;
import io.github.mzmine.datamodel.features.types.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.IonIdentityModularType;
import io.github.mzmine.datamodel.features.types.LipidAnnotationSummaryType;
import io.github.mzmine.datamodel.features.types.LipidAnnotationType;
import io.github.mzmine.datamodel.features.types.ManualAnnotationType;
import io.github.mzmine.datamodel.features.types.ModularType;
import io.github.mzmine.datamodel.features.types.ModularTypeProperty;
import io.github.mzmine.datamodel.features.types.SpectralLibMatchSummaryType;
import io.github.mzmine.datamodel.features.types.SpectralLibraryMatchType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.util.FeatureSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Node;

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

  /**
   * this final map is used in the FeaturesType - only ModularFeatureListRow is supposed to change
   * this map see {@link #addFeature}
   */
  private final ObservableMap<DataType, Property<?>> map =
      FXCollections.observableMap(new HashMap<>());
  private final Map<RawDataFile, ModularFeature> features;
  // buffert col charts and nodes
  private final Map<String, Node> buffertColCharts = new HashMap<>();
  @NotNull
  private ModularFeatureList flist;

  /**
   * Creates an empty row
   *
   * @param flist the feature list
   * @param id the row id
   */
  public ModularFeatureListRow(@NotNull ModularFeatureList flist, int id) {
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

    // set ID
    this.set(IDType.class, id);
  }

  /**
   * Constructor for row with only one feature.
   *
   * @param flist the feature list
   * @param id the row id
   * @param feature a feature to add to the row
   */
  public ModularFeatureListRow(@NotNull ModularFeatureList flist, int id, Feature feature) {
    this(flist, id);
    addFeature(feature.getRawDataFile(), feature);
  }

  /**
   * Create a row based on another row. Uses the old row ID
   *
   * @param flist the new feature list
   * @param row a row to copy (uses the row.getID() as the new ID)
   * @param copyFeatures true also copy features, false leave features empty
   */
  public ModularFeatureListRow(@NotNull ModularFeatureList flist, ModularFeatureListRow row,
      boolean copyFeatures) {
    this(flist, row.getID(), row, copyFeatures);
  }

  /**
   * Create a row based on another row
   *
   * @param flist the new feature list
   * @param id the row id
   * @param row a row to copy
   * @param copyFeatures true also copy features, false leave features empty
   */
  public ModularFeatureListRow(@NotNull ModularFeatureList flist, int id, ModularFeatureListRow row,
      boolean copyFeatures) {
    this(flist, id);

    // copy all but features
    if (row != null) {
      row.stream().filter(e -> !(e.getKey() instanceof FeaturesType))
          .forEach(entry -> this.set(entry.getKey(), entry.getValue()));
    }

    if (copyFeatures) {
      // Copy the features.
      for (final Entry<RawDataFile, ModularFeature> feature : row.getFilesFeatures().entrySet()) {
        this.addFeature(feature.getKey(), new ModularFeature(flist, feature.getValue()));
      }
    }
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
      get(FeaturesType.class)
          .addListener((MapChangeListener<RawDataFile, ModularFeature>) change -> {
            flist.getRowBindings().forEach(b -> b.apply(this));
          });
    }
  }


  public Stream<ModularFeature> streamFeatures() {
    return this.getFeatures().stream().map(ModularFeature.class::cast).filter(Objects::nonNull);
  }

  // Helper methods
  public Range<Double> getMZRange() {
    ObjectProperty<Range<Double>> v = get(MZRangeType.class);
    return v == null || v.getValue() == null ? Range.singleton(0d) : v.getValue();
  }

  public float getHeight() {
    Property<Float> v = get(HeightType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }

  public float getArea() {
    Property<Float> v = get(AreaType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }

  public ObservableMap<RawDataFile, ModularFeature> getFilesFeatures() {
    MapProperty<RawDataFile, ModularFeature> v = get(FeaturesType.class);
    return v == null || v.getValue() == null ? null : v.getValue();
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
  public synchronized void addFeature(RawDataFile raw, Feature feature) {
    if (!(feature instanceof ModularFeature)) {
      throw new IllegalArgumentException(
          "Cannot add non-modular feature to modular feature list row.");
    }
    if (!flist.equals(feature.getFeatureList())) {
      throw new IllegalArgumentException("Cannot add feature with different feature list to this "
          + "row. Create feature with the correct feature list as an argument.");
    }
    if (raw == null) {
      throw new IllegalArgumentException("Raw file cannot be null");
    }
    ModularFeature modularFeature = (ModularFeature) feature;

    features.put(raw, modularFeature);
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
    return idProp == null || idProp.getValue() == null ? -1 : idProp.getValue();
  }


  @Override
  public int getNumberOfFeatures() {
    return (int) features.values().stream()
        .filter(f -> f.getFeatureStatus() != FeatureStatus.UNKNOWN).count();
  }

  @Override
  public void removeFeature(RawDataFile file) {
    this.features.remove(file);
  }

  @Override
  public double getAverageMZ() {
    Property<Double> v = get(MZType.class);
    return v == null || v.getValue() == null ? Double.NaN : v.getValue();
  }

  @Override
  public void setAverageMZ(double averageMZ) {
    // binding
  }

  @Override
  public float getAverageRT() {
    Property<Float> v = get(RTType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }

  @Override
  public void setAverageRT(float averageRT) {
    // binding
  }

  @Override
  public Float getAverageMobility() {
    Property<Float> v = get(MobilityType.class);
    return v == null || v.getValue() == null ? null : v.getValue();
  }

  @Override
  public Float getAverageCCS() {
    Property<Float> v = get(CCSType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }

  @Override
  public double getAverageHeight() {
    Property<Float> v = get(HeightType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }

  @Override
  public int getRowCharge() {
    Property<Integer> v = get(ChargeType.class);
    return v == null || v.getValue() == null ? 0 : v.getValue();
  }

  @Override
  public double getAverageArea() {
    Property<Float> v = get(AreaType.class);
    return v == null || v.getValue() == null ? Float.NaN : v.getValue();
  }

  @Override
  public ObservableList<RawDataFile> getRawDataFiles() {
    return flist.getRawDataFiles();
  }

  @Override
  public boolean hasFeature(RawDataFile rawData) {
    ModularFeature feature = features.get(rawData);
    return feature != null && !feature.getFeatureStatus().equals(FeatureStatus.UNKNOWN);
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
  @Nullable
  @Override
  public ModularFeature getFeature(RawDataFile raw) {
    ModularFeature f = features.get(raw);
    return f != null && f.getFeatureStatus().equals(FeatureStatus.UNKNOWN) ? null : f;
  }

  @Nullable
  @Override
  public ModularFeatureList getFeatureList() {
    return flist;
  }

  @Override
  public void setFeatureList(@NotNull FeatureList flist) {
    if (!(flist instanceof ModularFeatureList)) {
      throw new IllegalArgumentException(
          "Cannot set non-modular feature list to modular feature list row.");
    }
    this.flist = (ModularFeatureList) flist;
  }

  @Override
  public RowGroup getGroup() {
    ObjectProperty<RowGroup> groupProperty = get(FeatureGroupType.class);
    return groupProperty == null? null : groupProperty.getValue();
  }

  @Override
  public void setGroup(RowGroup group) {
    set(FeatureGroupType.class, group);
  }

  /**
   * The list of ion identities
   *
   * @return null or the current list. First element is the "preferred" element
   */
  @Override
  @Nullable
  public List<IonIdentity> getIonIdentities() {
    if (get(IonIdentityModularType.class) == null) {
      return null;
    } else {
      return get(IonIdentityModularType.class).get(IonIdentityListType.class).getValue();
    }
  }

  /**
   * Set the list of ion identities with the first element being the preferred
   *
   * @param ions list of ion identities
   */
  @Override
  public void setIonIdentities(@Nullable List<IonIdentity> ions) {
    if (get(IonIdentityModularType.class) == null) {
      // add row type if not available
      flist.addRowType(new IonIdentityModularType());
    }
    get(IonIdentityModularType.class).set(IonIdentityListType.class, ions);
  }

  /**
   * Checks if typeClass was added as a FeatureType - does not check nested types in a
   * {@link ModularType}
   *
   * @param typeClass class of a DataType
   * @return true if feature type is available
   */
  public boolean hasFeatureType(Class typeClass) {
    return getFeatureList().hasFeatureType(typeClass);
  }

  /**
   * Checks if typeClass was added as a row type - does not check nested types in a
   * {@link ModularType}
   *
   * @param typeClass class of a DataType
   * @return true if row type is available
   */
  public boolean hasRowType(Class typeClass) {
    return getFeatureList().hasRowType(typeClass);
  }

  @Override
  public String getComment() {
    ModularTypeProperty manual = getManualAnnotation();
    if (manual != null) {
      return manual.get(CommentType.class).getValue();
    } else {
      StringProperty v = get(CommentType.class);
      return v == null || v.getValue() == null ? "" : v.getValue();
    }
  }

  @Override
  public void setComment(String comment) {
    ModularTypeProperty manual = getManualAnnotation();
    if (manual == null) {
      // add type
      flist.addRowType(new ManualAnnotationType());
      setComment(comment);
      return;
    }
    manual.set(CommentType.class, comment);
  }

  public ModularTypeProperty getManualAnnotation() {
    return get(ManualAnnotationType.class);
  }

  @Override
  public ObservableList<FeatureIdentity> getPeakIdentities() {
    ModularTypeProperty manual = getManualAnnotation();
    if (manual != null) {
      return manual.get(IdentityType.class).getValue();
    } else {
      ListProperty<FeatureIdentity> prop = get(IdentityType.class);
      return prop == null || prop.getValue() == null ? null
          : FXCollections.unmodifiableObservableList(FXCollections.emptyObservableList());
    }
  }

  public void setPeakIdentities(ObservableList<FeatureIdentity> identities) {
    ModularTypeProperty manual = getManualAnnotation();
    if (manual == null) {
      // add type
      flist.addRowType(new ManualAnnotationType());
      setPeakIdentities(identities);
      return;
    }
    manual.set(IdentityType.class, identities);
  }

  @Override
  public void addFeatureIdentity(FeatureIdentity identity, boolean preferred) {
    ModularTypeProperty manual = getManualAnnotation();
    if (manual == null) {
      // add type
      flist.addRowType(new ManualAnnotationType());
      addFeatureIdentity(identity, preferred);
      return;
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
  @NotNull
  public List<SpectralDBFeatureIdentity> getSpectralLibraryMatches() {
    ModularTypeProperty matchProperty = get(SpectralLibraryMatchType.class);
    if (matchProperty != null) {
      return matchProperty.get(SpectralLibMatchSummaryType.class).getValue();
    } else {
      return FXCollections.unmodifiableObservableList(FXCollections.emptyObservableList());
    }
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
  public FeatureInformation getFeatureInformation() {
    ObjectProperty<SimpleFeatureInformation> v = get(FeatureInformationType.class);
    return v == null ? null : v.getValue();
  }

  @Override
  public void setFeatureInformation(FeatureInformation featureInformation) {
    set(FeatureInformationType.class, featureInformation);
  }

  @Override
  public double getMaxDataPointIntensity() {
    ObjectProperty<Range<Float>> rangeObjectProperty = get(IntensityRangeType.class);
    return rangeObjectProperty != null && rangeObjectProperty.getValue() != null
        ? rangeObjectProperty.getValue().upperEndpoint()
        : Double.NaN;
  }

  @Nullable
  @Override
  public ModularFeature getBestFeature() {
    return streamFeatures().filter(f -> f.get(DetectionType.class).get() != FeatureStatus.UNKNOWN)
        .sorted(new FeatureSorter(SortingProperty.Height, SortingDirection.Descending)).findFirst()
        .orElse(null);
  }

  @Override
  public Scan getBestFragmentation() {
    double bestTIC = 0.0;
    Scan bestScan = null;
    for (Feature feature : getFeatures()) {
      RawDataFile rawData = feature.getRawDataFile();
      if (rawData == null || feature.getFeatureStatus().equals(FeatureStatus.UNKNOWN)) {
        continue;
      }

      Scan theScan = feature.getMostIntenseFragmentScan();
      double theTIC = 0.0;
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

  @NotNull
  @Override
  public ObservableList<Scan> getAllMS2Fragmentations() {
    ObservableList<Scan> allMS2ScansList = FXCollections.observableArrayList();
    for (Feature feature : getFeatures()) {
      RawDataFile rawData = feature.getRawDataFile();
      ObservableList<Scan> scans = feature.getAllMS2FragmentScans();
      if (scans != null) {
        for (Scan scan : scans) {
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

  public List<ResultFormula> getFormulas() {
    ModularTypeProperty formulaType = get(FormulaAnnotationType.class);
    return formulaType == null ? null : formulaType.get(FormulaSummaryType.class).getValue();
  }

  public void setFormulas(List<ResultFormula> formulas) {
    if (get(FormulaAnnotationType.class) == null) {
      flist.addRowType(new FormulaAnnotationType());
    }
    get(FormulaAnnotationType.class).set(FormulaSummaryType.class, formulas);
  }

  @Override
  public void addLipidAnnotation(MatchedLipid matchedLipid) {
    // add column first if needed
    get(LipidAnnotationType.class).get(LipidAnnotationSummaryType.class).add(matchedLipid);
  }

}

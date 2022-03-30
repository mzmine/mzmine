/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.features;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DetectionType;
import io.github.mzmine.datamodel.features.types.FeatureGroupType;
import io.github.mzmine.datamodel.features.types.FeatureInformationType;
import io.github.mzmine.datamodel.features.types.FeaturesType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotationType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaListType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.util.FeatureSorter;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.FragmentScanSorter;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Map of all feature related data.
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 * <p>
 * TODO: I think the RawFileType should also be in the map and not just accessible via the key set
 * of {@link ModularFeatureListRow#getFilesFeatures}. -> add during fueature list creation in the
 * chromatogram builder ~SteffenHeu
 */
@SuppressWarnings("rawtypes")
public class ModularFeatureListRow implements FeatureListRow {

  private static final Logger logger = Logger.getLogger(ModularFeatureListRow.class.getName());
  /**
   * this final map is used in the FeaturesType - only ModularFeatureListRow is supposed to change
   * this map see {@link #addFeature}
   */
  private final ObservableMap<DataType, Object> map = FXCollections.observableMap(new HashMap<>());
  private final Map<RawDataFile, ModularFeature> features;
  // buffert col charts and nodes
  private final Map<String, Node> buffertColCharts = new HashMap<>();
  @NotNull
  private ModularFeatureList flist;

  /**
   * Creates an empty row
   *
   * @param flist the feature list
   * @param id    the row id
   */
  public ModularFeatureListRow(@NotNull ModularFeatureList flist, int id) {
    this.flist = flist;

    // register listener to types map to automatically generate default properties for new DataTypes
    flist.getRowTypes().addListener(
        (MapChangeListener<? super Class<? extends DataType>, ? super DataType>) change -> {
          if (change.wasAdded()) {
            // do nothing for now
          } else if (change.wasRemoved()) {
            // remove type columns to maps
            DataType type = change.getValueRemoved();
            this.remove((Class) type.getClass());
          }
        });

    // features
    List<RawDataFile> raws = flist.getRawDataFiles();
    if (!raws.isEmpty()) {
      // init FeaturesType map (is final)
      HashMap<RawDataFile, ModularFeature> fmap = new HashMap<>(raws.size());
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
   * @param flist   the feature list
   * @param id      the row id
   * @param feature a feature to add to the row
   */
  public ModularFeatureListRow(@NotNull ModularFeatureList flist, int id, Feature feature) {
    this(flist, id);
    addFeature(feature.getRawDataFile(), feature);
  }

  /**
   * Create a row based on another row. Uses the old row ID
   *
   * @param flist        the new feature list
   * @param row          a row to copy (uses the row.getID() as the new ID)
   * @param copyFeatures true also copy features, false leave features empty
   */
  public ModularFeatureListRow(@NotNull ModularFeatureList flist, ModularFeatureListRow row,
      boolean copyFeatures) {
    this(flist, row.getID(), row, copyFeatures);
  }

  /**
   * Create a row based on another row
   *
   * @param flist        the new feature list
   * @param id           the row id
   * @param row          a row to copy
   * @param copyFeatures true also copy features, false leave features empty
   */
  public ModularFeatureListRow(@NotNull ModularFeatureList flist, int id, ModularFeatureListRow row,
      boolean copyFeatures) {
    this(flist, id);

    // copy all but features and id
    if (row != null) {
      row.stream()
          .filter(e -> !(e.getKey() instanceof FeaturesType) && !(e.getKey() instanceof IDType))
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

  // todo make private?
  @Override
  public ObservableMap<DataType, Object> getMap() {
    return map;
  }

  @Override
  public @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getValueChangeListeners() {
    return getFeatureList().getRowTypeChangeListeners();
  }

  @Override
  public <T> boolean set(Class<? extends DataType<T>> tclass, T value) {
    // type in defined columns?
    if (!getTypes().containsKey(tclass)) {
      try {
        DataType newType = tclass.getConstructor().newInstance();
        ModularFeatureList flist = getFeatureList();
        flist.addRowType(newType);
      } catch (NullPointerException | InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
        e.printStackTrace();
        return false;
      }
    }
    // access default method
    boolean changed = FeatureListRow.super.set(tclass, value);

    //
    if (changed && tclass.equals(FeaturesType.class)) {
      // TODO new features set -> use bindings?
    }
    return changed;
  }

  @Override
  public Stream<ModularFeature> streamFeatures() {
    return this.getFeatures().stream().map(ModularFeature.class::cast).filter(Objects::nonNull);
  }


  // Helper methods
  @Override
  public Range<Double> getMZRange() {
    Range<Double> v = get(MZRangeType.class);
    return v == null ? Range.singleton(0d) : v;
  }

  public Map<RawDataFile, ModularFeature> getFilesFeatures() {
    return get(FeaturesType.class);
  }

  @Override
  public List<ModularFeature> getFeatures() {
    // TODO remove features object - not always do we have features
    // FeaturesType creates an empty ListProperty for that
    // return FXCollections.observableArrayList(get(FeaturesType.class).values());
    return new ArrayList<>(features.values());
  }

  @Override
  public synchronized void addFeature(RawDataFile raw, Feature feature,
      boolean updateByRowBindings) {
    if (!(feature instanceof ModularFeature modularFeature)) {
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

//    logger.log(Level.FINEST, "ADDING FEATURE");
    ModularFeature oldFeature = features.put(raw, modularFeature);
    modularFeature.setFeatureList(flist);
    modularFeature.setRow(this);

    if (!Objects.equals(oldFeature, modularFeature)) {
      // reflect changes by updating all row bindings
      getFeatureList().fireFeatureChangedEvent(this, modularFeature, raw, updateByRowBindings);
    }
  }

  /**
   * Row ID or -1 if not present
   *
   * @return
   */
  @Override
  public Integer getID() {
    Integer idProp = get(IDType.class);
    return idProp == null ? -1 : idProp;
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
  public Double getAverageMZ() {
    return get(MZType.class);
  }

  @Override
  public void setAverageMZ(Double averageMZ) {
    // binding
  }

  @Override
  public Float getAverageRT() {
    return get(RTType.class);
  }

  @Override
  public void setAverageRT(Float averageRT) {
    // binding
  }

  @Override
  @Nullable
  public Float getAverageMobility() {
    return get(MobilityType.class);
  }

  @Override
  @Nullable
  public Float getAverageCCS() {
    return get(CCSType.class);
  }

  @Override
  public Float getAverageHeight() {
    return get(HeightType.class);
  }

  @Override
  public Integer getRowCharge() {
    Integer v = get(ChargeType.class);
    return v == null ? 0 : v;
  }

  @Override
  public Float getAverageArea() {
    return get(AreaType.class);
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

  public void clearBufferedColCharts() {
    buffertColCharts.forEach((k, v) -> {
      if (v instanceof Pane p && p.getParent() instanceof Pane pane) {
        // remove the node from the parent so there is no more reference and it can be GC'ed
        pane.getChildren().remove(v);
      }
    });
    buffertColCharts.clear();
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
    return get(FeatureGroupType.class);
  }

  @Override
  public void setGroup(RowGroup group) {
    set(FeatureGroupType.class, group);
  }

  /**
   * The immutable list of ion identities.
   *
   * @return null or the current list. First element is the "preferred" element
   */
  @Override
  @Nullable
  public List<IonIdentity> getIonIdentities() {
    List<IonIdentity> ions = get(IonIdentityListType.class);
    return ions == null ? List.of() : ions;
  }

  /**
   * Set the list of ion identities with the first element being the preferred
   *
   * @param ions list of ion identities
   */
  @Override
  public void setIonIdentities(@Nullable List<IonIdentity> ions) {
    set(IonIdentityListType.class, ions);
  }

  /**
   * Checks if typeClass was added as a FeatureType
   *
   * @param typeClass class of a DataType
   * @return true if feature type is available
   */
  public boolean hasFeatureType(Class typeClass) {
    ModularFeatureList flist = getFeatureList();
    return flist != null && flist.hasFeatureType(typeClass);
  }

  /**
   * Checks if typeClass was added as a row type
   *
   * @param typeClass class of a DataType
   * @return true if row type is available
   */
  public boolean hasRowType(Class typeClass) {
    ModularFeatureList flist = getFeatureList();
    return flist != null && flist.hasRowType(typeClass);
  }

  @Override
  public String getComment() {
    ManualAnnotation manual = getManualAnnotation();
    return manual == null ? null : manual.getComment();
  }

  @Override
  public void setComment(String comment) {
    ManualAnnotation manual = getManualAnnotation();
    if (manual == null) {
      manual = new ManualAnnotation();
    }
    manual.setComment(comment);
    set(ManualAnnotationType.class, manual);
  }

  @Nullable
  public ManualAnnotation getManualAnnotation() {
    return get(ManualAnnotationType.class);
  }

  @Override
  public List<FeatureIdentity> getPeakIdentities() {
    ManualAnnotation manual = getManualAnnotation();
    return manual == null ? List.of() : Objects.requireNonNullElse(manual.getIdentities(), List.of());
  }

  public void setPeakIdentities(List<FeatureIdentity> identities) {
    ManualAnnotation manual = getManualAnnotation();
    if (manual == null) {
      manual = new ManualAnnotation();
    }
    manual.setIdentities(identities);
    set(ManualAnnotationType.class, manual);
  }

  @Override
  public void addFeatureIdentity(FeatureIdentity identity, boolean preferred) {
    ManualAnnotation manual = Objects.requireNonNullElse(getManualAnnotation(),
        new ManualAnnotation());

    List<FeatureIdentity> peakIdentities;
    // getPeakIdentities initializes the returned list as an immutable list if manual is null
    // if we add a new identity for the first time here, this will lead to an UnsupportedOperationException
    if (getManualAnnotation() == null) {
      peakIdentities = new ArrayList<>();
    } else {
      peakIdentities = getPeakIdentities();
    }
    peakIdentities.remove(identity);
    if (preferred) {
      peakIdentities.add(0, identity);
    } else {
      peakIdentities.add(identity);
    }
    manual.setIdentities(peakIdentities);
    set(ManualAnnotationType.class, manual);
  }

  @Override
  public void addCompoundAnnotation(CompoundDBAnnotation id) {
    synchronized (getMap()) {
      List<CompoundDBAnnotation> matches = get(CompoundDatabaseMatchesType.class);
      if (matches == null) {
        matches = new ArrayList<>();
      }
      matches.add(id);
      set(CompoundDatabaseMatchesType.class, matches);
    }
  }

  @NotNull
  @Override
  public List<CompoundDBAnnotation> getCompoundAnnotations() {
    var list = get(CompoundDatabaseMatchesType.class);
    return list != null ? list : List.of();
  }

  @Override
  public void setCompoundAnnotations(List<CompoundDBAnnotation> annotations) {
    synchronized (getMap()) {
      set(CompoundDatabaseMatchesType.class, annotations);
    }
  }

  @Override
  public void addSpectralLibraryMatch(SpectralDBFeatureIdentity id) {
    synchronized (getMap()) {
      List<SpectralDBFeatureIdentity> matches = get(SpectralLibraryMatchesType.class);
      if (matches == null) {
        matches = new ArrayList<>();
      }
      matches.add(id);
      set(SpectralLibraryMatchesType.class, matches);
    }
  }

  @Override
  public void addSpectralLibraryMatches(List<SpectralDBFeatureIdentity> matches) {
    synchronized (getMap()) {
      List<SpectralDBFeatureIdentity> old = get(SpectralLibraryMatchesType.class);
      if (old == null) {
        old = new ArrayList<>();
      }
      old.addAll(matches);
      set(SpectralLibraryMatchesType.class, old);
    }
  }

  @Override
  @Nullable
  public Range<Float> getMobilityRange() {
    return get(MobilityRangeType.class);
  }

  @Override
  public void setSpectralLibraryMatch(List<SpectralDBFeatureIdentity> matches) {
    synchronized (getMap()) {
      set(SpectralLibraryMatchesType.class, matches);
    }
  }

  @Override
  @NotNull
  public List<SpectralDBFeatureIdentity> getSpectralLibraryMatches() {
    List<SpectralDBFeatureIdentity> matches = get(SpectralLibraryMatchesType.class);
    return matches == null ? List.of() : matches;
  }

  @Override
  public void removeFeatureIdentity(FeatureIdentity identity) {
    ManualAnnotation manual = getManualAnnotation();
    List<FeatureIdentity> identities = manual.getIdentities();
    if (identities != null && !identities.isEmpty()) {
      identities = new ArrayList<>(identities);
      identities.remove(identity);
      setPeakIdentities(identities.isEmpty() ? null : identities);
    }
  }

  @Override
  public FeatureIdentity getPreferredFeatureIdentity() {
    return getPeakIdentities().stream().findFirst().orElse(null);
  }

  @Override
  public void setPreferredFeatureIdentity(FeatureIdentity preferredIdentity) {
    // unmodifiable list
    List<FeatureIdentity> identities = getPeakIdentities();
    if (identities == null || identities.isEmpty()) {
      setPeakIdentities(List.of(preferredIdentity));
    }
    identities = new ArrayList<>(identities);
    identities.remove(preferredIdentity);
    identities.add(0, preferredIdentity);
    setPeakIdentities(identities);
  }

  @Override
  public FeatureInformation getFeatureInformation() {
    return get(FeatureInformationType.class);
  }

  @Override
  public void setFeatureInformation(FeatureInformation featureInformation) {
    set(FeatureInformationType.class, featureInformation);
  }

  @Override
  public Float getMaxDataPointIntensity() {
    Range<Float> intensityRange = get(IntensityRangeType.class);
    return intensityRange != null ? intensityRange.upperEndpoint() : null;
  }

  @Nullable
  @Override
  public ModularFeature getBestFeature() {
    return streamFeatures().filter(Objects::nonNull)
        .filter(f -> f.get(DetectionType.class) != FeatureStatus.UNKNOWN)
        .sorted(new FeatureSorter(SortingProperty.Height, SortingDirection.Descending)).findFirst()
        .orElse(null);
  }

  @Override
  public Scan getMostIntenseFragmentScan() {
    // best scan is always the first in the sorted stream
    return streamFeatures().map(Feature::getMostIntenseFragmentScan).filter(Objects::nonNull)
        .min(FragmentScanSorter.DEFAULT_TIC).orElse(null);
  }

  @NotNull
  @Override
  public List<Scan> getAllFragmentScans() {
    List<Scan> allMS2ScansList = new ArrayList<>();
    final ModularFeature[] features = getFeatures().toArray(ModularFeature[]::new);
    for (Feature feature : features) {
      List<Scan> scans = feature.getAllMS2FragmentScans();
      if (scans != null) {
        allMS2ScansList.addAll(scans);
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


  @Override
  public boolean hasIsotopePattern() {
    ModularFeature[] features = getFilesFeatures().values().toArray(new ModularFeature[0]);

    for (ModularFeature feature : features) {
      IsotopePattern ip = feature.getIsotopePattern();
      if (ip != null) {
        return true;
      }
    }

    return false;
  }


  public List<ResultFormula> getFormulas() {
    return get(FormulaListType.class);
  }

  public void setFormulas(List<ResultFormula> formulas) {
    set(FormulaListType.class, formulas);
  }

  @Override
  public void addLipidAnnotation(MatchedLipid matchedLipid) {
    // add column first if needed
    List<MatchedLipid> matches = get(LipidMatchListType.class);
    if (matches == null) {
      matches = List.of(matchedLipid);
    } else {
      matches = new ArrayList<>(matches);
      matches.add(matchedLipid);
    }
    set(LipidMatchListType.class, matches);
  }

  @Override
  public String toString() {
    return FeatureUtils.rowToString(this);
  }
}

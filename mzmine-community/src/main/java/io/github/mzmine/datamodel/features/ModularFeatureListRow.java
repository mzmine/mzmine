/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.columnar_data.ColumnarModularDataModelRow;
import io.github.mzmine.datamodel.features.columnar_data.ColumnarModularFeatureListRowsSchema;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DetectionType;
import io.github.mzmine.datamodel.features.types.FeatureGroupType;
import io.github.mzmine.datamodel.features.types.FeatureInformationType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotationType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaListType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.annotations.online_reaction.OnlineLcReactionMatchType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
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
import io.github.mzmine.datamodel.features.types.numbers.RIType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineReactionMatch;
import io.github.mzmine.util.FeatureSorter;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.FragmentScanSorter;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Map of all feature related data. Uses the {@link ModularDataModel} and the
 * {@link ColumnarModularFeatureListRowsSchema} in a {@link FeatureList}.
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
@SuppressWarnings("rawtypes")
public class ModularFeatureListRow extends ColumnarModularDataModelRow implements FeatureListRow {

  private static final Logger logger = Logger.getLogger(ModularFeatureListRow.class.getName());
  @NotNull
  private final ModularFeatureList flist;

  /**
   * Simple write lock for more complex objects to be changed like list types. Only write is
   * locked.
   */
  private final Object writeLock = new Object();

  /**
   * Creates an empty row
   *
   * @param flist the feature list
   * @param id    the row id
   */
  public ModularFeatureListRow(@NotNull ModularFeatureList flist, int id) {
    super(flist.getRowsSchema());
    this.flist = flist;
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
      row.stream().filter(e -> !(e.getKey() instanceof IDType))
          .forEach(entry -> this.set(entry.getKey(), entry.getValue()));

      if (copyFeatures) {
        // Copy the features.
        row.streamFeatures().forEach(feature -> this.addFeature(feature.getRawDataFile(),
            new ModularFeature(flist, feature)));
      }
    }
  }

  @Override
  public Set<DataType> getTypes() {
    return flist.getRowTypes();
  }

  @Override
  public @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getValueChangeListeners() {
    return getFeatureList().getRowTypeChangeListeners();
  }

  @Override
  public Stream<ModularFeature> streamFeatures() {
    return flist.getRowsSchema().streamFeatures(modelRowIndex)
        .filter(f -> f.get(DetectionType.class) != FeatureStatus.UNKNOWN);
  }

  // Helper methods
  @Override
  public Range<Double> getMZRange() {
    Range<Double> v = get(MZRangeType.class);
    return v == null ? Range.singleton(0d) : v;
  }

  public Map<RawDataFile, ModularFeature> getFilesFeatures() {
    return streamFeatures().collect(Collectors.toMap(ModularFeature::getRawDataFile, f -> f));
  }

  @Override
  public List<ModularFeature> getFeatures() {
    return streamFeatures().toList();
  }

  @Override
  public synchronized void addFeature(RawDataFile raw, Feature feature,
      boolean updateByRowBindings) {
    final ModularFeature oldFeature;
    if (feature == null) {
      oldFeature = flist.getRowsSchema().setFeature(modelRowIndex, raw, null);
    } else {
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

      oldFeature = flist.getRowsSchema().setFeature(modelRowIndex, raw, modularFeature);
      modularFeature.setRow(this);
    }

    if (!Objects.equals(oldFeature, feature)) {
      // reflect changes by updating all row bindings
      getFeatureList().fireFeatureChangedEvent(this, feature, raw, updateByRowBindings);
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
    return (int) streamFeatures().count();
  }

  @Override
  public void removeFeature(RawDataFile file, boolean updateByRowBindings) {
    addFeature(file, null, updateByRowBindings);
  }

  @Override
  public void clearFeatures(final boolean updateByRowBindings) {
    final boolean changed = flist.getRowsSchema().clearFeatures(modelRowIndex);
    if (changed) {
      // reflect changes by updating all row bindings
      getFeatureList().fireFeatureChangedEvent(this, null, null, updateByRowBindings);
    }
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

  public Float getAverageRI() {
    return get(RIType.class);
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
  public Float getMaxHeight() {
    return get(HeightType.class);
  }

  @Override
  public Integer getRowCharge() {
    Integer v = get(ChargeType.class);
    return v == null ? 0 : v;
  }

  @Override
  public Float getMaxArea() {
    return get(AreaType.class);
  }

  /**
   * @return unmodifiable list of all raw data files - even if there is no feature
   */
  @Override
  public List<RawDataFile> getRawDataFiles() {
    return flist.getRawDataFiles();
  }

  @Override
  public boolean hasFeature(RawDataFile rawData) {
    return getFeature(rawData) != null;
  }

  @Override
  public boolean hasFeature(Feature feature) {
    if (!(feature instanceof ModularFeature)) {
      // throw new IllegalArgumentException("Modular feature list row can not contain non-modular
      // feature.");
      return false;
    }
    return streamFeatures().anyMatch(f -> Objects.equals(f, feature));
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
    ModularFeature f = flist.getRowsSchema().getFeature(modelRowIndex, raw);
    return f != null && f.getFeatureStatus().equals(FeatureStatus.UNKNOWN) ? null : f;
  }

  @Override
  public @NotNull ModularFeatureList getFeatureList() {
    return flist;
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

  @Override
  @Nullable
  public ManualAnnotation getManualAnnotation() {
    return get(ManualAnnotationType.class);
  }

  @Override
  public List<FeatureIdentity> getPeakIdentities() {
    ManualAnnotation manual = getManualAnnotation();
    return manual == null ? List.of() : requireNonNullElse(manual.getIdentities(), List.of());
  }

  public void setPeakIdentities(List<FeatureIdentity> identities) {
    synchronized (writeLock) {
      ManualAnnotation manual = getManualAnnotation();
      if (manual == null) {
        manual = new ManualAnnotation();
      }
      manual.setIdentities(identities);
      set(ManualAnnotationType.class, manual);
    }
  }

  @Override
  public void addFeatureIdentity(FeatureIdentity identity, boolean preferred) {
    synchronized (writeLock) {
      ManualAnnotation manual = requireNonNullElse(getManualAnnotation(), new ManualAnnotation());

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
  }

  @Override
  public void addCompoundAnnotation(CompoundDBAnnotation id) {
    // should usually not be called from multiple threads
    synchronized (writeLock) {
      List<CompoundDBAnnotation> matches = get(CompoundDatabaseMatchesType.class);
      List<CompoundDBAnnotation> newList = new ArrayList<>();
      if (matches != null) {
        newList.addAll(matches);
      }
      newList.add(id);
      set(CompoundDatabaseMatchesType.class, newList);
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
    set(CompoundDatabaseMatchesType.class, annotations);
  }

  /**
   * Checks if this row contains an annotation based on the {@link ListWithSubsType} and the
   * {@link AnnotationType} and if the corresponding entry is not null or empty.
   *
   * @return True if a value that is not null or empty for a {@link ListWithSubsType} and a
   * {@link AnnotationType} is contained in this feature.
   */
  @Override
  public boolean isIdentified() {
    for (DataType dt : getTypes()) {
      if (dt instanceof ListWithSubsType<?> listType && dt instanceof AnnotationType
          && !(dt instanceof IonIdentityListType)) {
        final List<?> list = get(listType);
        if (list != null && !list.isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void addSpectralLibraryMatch(SpectralDBAnnotation id) {
    synchronized (writeLock) {
      List<SpectralDBAnnotation> old = requireNonNullElseGet(get(SpectralLibraryMatchesType.class),
          ArrayList::new);
      old.add(id);
      set(SpectralLibraryMatchesType.class, old);
    }
  }

  @Override
  public void addSpectralLibraryMatches(List<SpectralDBAnnotation> matches) {
    synchronized (writeLock) {
      List<SpectralDBAnnotation> old = requireNonNullElseGet(get(SpectralLibraryMatchesType.class),
          ArrayList::new);
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
  public void setSpectralLibraryMatch(List<SpectralDBAnnotation> matches) {
    set(SpectralLibraryMatchesType.class, matches);
  }

  @Override
  public @NotNull List<SpectralDBAnnotation> getSpectralLibraryMatches() {
    List<SpectralDBAnnotation> matches = get(SpectralLibraryMatchesType.class);
    return matches == null ? List.of() : matches;
  }

  @Override
  public @NotNull List<OnlineReactionMatch> getOnlineReactionMatches() {
    List<OnlineReactionMatch> matches = get(OnlineLcReactionMatchType.class);
    return matches == null ? List.of() : matches;
  }

  @Override
  public @NotNull List<MatchedLipid> getLipidMatches() {
    var matches = get(LipidMatchListType.class);
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
    return streamFeatures().sorted(
            new FeatureSorter(SortingProperty.Height, SortingDirection.Descending)).findFirst()
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
    return streamFeatures().filter(f -> f != null && f.getIsotopePattern() != null
            && f.getFeatureStatus() != FeatureStatus.UNKNOWN)
        .max(Comparator.comparingDouble(ModularFeature::getHeight))
        .map(ModularFeature::getIsotopePattern).orElse(null);
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

  @Override
  public Object getPreferredAnnotation() {
    return streamAllFeatureAnnotations().findFirst().orElse(null);
  }

  @Override
  public String getPreferredAnnotationName() {
    Object annotation = getPreferredAnnotation();
    return switch (annotation) {
      case FeatureAnnotation ann -> ann.getCompoundName();
      case ManualAnnotation ann -> ann.getCompoundName();
      case MolecularFormulaIdentity ann -> ann.getFormulaAsString();
      case null -> null;
      default -> throw new IllegalStateException("Unexpected value: " + annotation);
    };
  }

  @Override
  public List<ResultFormula> getFormulas() {
    return requireNonNullElse(get(FormulaListType.class), List.of());
  }

  @Override
  public void setFormulas(List<ResultFormula> formulas) {
    set(FormulaListType.class, formulas);
  }

  @Override
  public void addFormula(ResultFormula formula, boolean preferred) {
    synchronized (writeLock) {
      final List<ResultFormula> resultFormulas = new ArrayList<>(getFormulas());
      if (preferred) {
        resultFormulas.addFirst(formula);
      } else {
        resultFormulas.add(formula);
      }
      setFormulas(resultFormulas);
    }
  }

  @Override
  public void addLipidAnnotation(MatchedLipid matchedLipid) {
    synchronized (writeLock) {
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
  }

  @Override
  public String toString() {
    return FeatureUtils.rowToString(this);
  }
}

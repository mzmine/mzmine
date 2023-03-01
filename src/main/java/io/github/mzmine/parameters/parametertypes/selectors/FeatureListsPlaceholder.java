package io.github.mzmine.parameters.parametertypes.selectors;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.DataTypeValueChangeListener;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.RowGroup;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.main.MZmineCore;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureListsPlaceholder implements FeatureList {

  private final String name;

  WeakReference<FeatureList> featureList;
  @Nullable
  private final Integer fileHashCode;

  public FeatureListsPlaceholder(@NotNull final FeatureList featureList) {
    name = featureList.getName();
    this.featureList = new WeakReference<>(featureList);
    if (featureList instanceof FeatureListsPlaceholder flp) {
      if (flp.fileHashCode == null) {
        fileHashCode = null;
      } else {
        fileHashCode = flp.fileHashCode;
      }
    } else {
      fileHashCode = featureList.hashCode();
    }
  }

  public FeatureListsPlaceholder(@NotNull String name) {
    this(name, null);
  }

  public FeatureListsPlaceholder(@NotNull String name, @Nullable Integer fileHashCode) {
    this.name = name;
    this.fileHashCode = fileHashCode;
  }

  @Nullable
  public FeatureList getMatchingFeatureList() {
    final MZmineProject proj = MZmineCore.getProjectManager().getCurrentProject();
    if (proj == null) {
      return null;
    }

    return proj.getCurrentFeatureLists().stream().filter(this::matches).findFirst().orElse(null);
  }

  public boolean matches(@Nullable final FeatureList featureList) {
    return featureList != null && featureList.getName().equals(name) && Objects.equals(fileHashCode,
        featureList.hashCode());
  }

  @Override
  public @NotNull String getName() {
    return name;
  }

  @Override
  public String setName(@NotNull String name) {
    throw new UnsupportedOperationException(
        "This class is only to be used in the FeatureListsSelection and does not support the required operation.");
  }

  @Override
  public void addRowBinding(@NotNull List<RowBinding> bindings) {

  }

  @Override
  public void addFeatureTypeListener(DataType featureType, DataTypeValueChangeListener listener) {

  }

  @Override
  public void addRowTypeListener(DataType rowType, DataTypeValueChangeListener listener) {

  }

  @Override
  public void removeRowTypeListener(DataType rowType, DataTypeValueChangeListener listener) {

  }

  @Override
  public void removeFeatureTypeListener(DataType featureType,
      DataTypeValueChangeListener listener) {

  }

  @Override
  public void applyRowBindings() {

  }

  @Override
  public void applyRowBindings(FeatureListRow row) {

  }

  @Override
  public ObservableMap<Class<? extends DataType>, DataType> getFeatureTypes() {
    return null;
  }

  @Override
  public void addFeatureType(Collection<DataType> types) {

  }

  @Override
  public void addFeatureType(@NotNull DataType<?>... types) {

  }

  @Override
  public void addRowType(Collection<DataType> types) {

  }

  @Override
  public void addRowType(@NotNull DataType<?>... types) {

  }

  @Override
  public ObservableMap<Class<? extends DataType>, DataType> getRowTypes() {
    return null;
  }

  @Override
  public boolean hasFeatureType(Class typeClass) {
    return false;
  }

  @Override
  public boolean hasRowType(Class typeClass) {
    return false;
  }

  @Override
  public int getNumberOfRawDataFiles() {
    return 0;
  }

  @Override
  public ObservableList<RawDataFile> getRawDataFiles() {
    return null;
  }

  @Override
  public boolean hasRawDataFile(RawDataFile file) {
    return false;
  }

  @Override
  public RawDataFile getRawDataFile(int position) {
    return null;
  }

  @Override
  public int getNumberOfRows() {
    return 0;
  }

  @Override
  public ModularFeature getFeature(int row, RawDataFile rawDataFile) {
    return null;
  }

  @Override
  public List<ModularFeature> getFeatures(RawDataFile rawDataFile) {
    return null;
  }

  @Override
  public FeatureListRow getRow(int row) {
    return null;
  }

  @Override
  public ObservableList<FeatureListRow> getRows() {
    return null;
  }

  @Override
  public void setRows(FeatureListRow... rows) {

  }

  @Override
  public void removeRow(int rowNum, FeatureListRow row) {

  }

  @Override
  public Stream<FeatureListRow> stream() {
    return null;
  }

  @Override
  public Stream<FeatureListRow> parallelStream() {
    return null;
  }

  @Override
  public Stream<ModularFeature> streamFeatures() {
    return null;
  }

  @Override
  public Stream<ModularFeature> parallelStreamFeatures() {
    return null;
  }

  @Override
  public String setNameNoChecks(@NotNull String name) {
    return null;
  }

  @Override
  public void setSelectedScans(@NotNull RawDataFile file, @Nullable List<? extends Scan> scans) {

  }

  @Override
  public @Nullable List<? extends Scan> getSeletedScans(@NotNull RawDataFile file) {
    return null;
  }

  @Override
  public List<FeatureListRow> getRowsInsideScanRange(Range<Float> rtRange) {
    return null;
  }

  @Override
  public List<FeatureListRow> getRowsInsideMZRange(Range<Double> mzRange) {
    return null;
  }

  @Override
  public List<FeatureListRow> getRowsInsideScanAndMZRange(Range<Float> rtRange,
      Range<Double> mzRange) {
    return null;
  }

  @Override
  public List<Feature> getFeaturesInsideScanRange(RawDataFile file, Range<Float> rtRange) {
    return null;
  }

  @Override
  public List<Feature> getFeaturesInsideMZRange(RawDataFile file, Range<Double> mzRange) {
    return null;
  }

  @Override
  public List<Feature> getFeaturesInsideScanAndMZRange(RawDataFile file, Range<Float> rtRange,
      Range<Double> mzRange) {
    return null;
  }

  @Override
  public double getDataPointMaxIntensity() {
    return 0;
  }

  @Override
  public void addRow(FeatureListRow row) {

  }

  @Override
  public void removeRow(int row) {

  }

  @Override
  public void removeRow(FeatureListRow row) {

  }

  @Override
  public int getFeatureListRowNum(Feature feature) {
    return 0;
  }

  @Override
  public FeatureListRow getFeatureRow(Feature feature) {
    return null;
  }

  @Override
  public void addDescriptionOfAppliedTask(FeatureListAppliedMethod appliedMethod) {

  }

  @Override
  public ObservableList<FeatureListAppliedMethod> getAppliedMethods() {
    return null;
  }

  @Override
  public Range<Double> getRowsMZRange() {
    return null;
  }

  @Override
  public Range<Float> getRowsRTRange() {
    return null;
  }

  @Override
  public FeatureListRow findRowByID(int id) {
    return null;
  }

  @Override
  public String getDateCreated() {
    return null;
  }

  @Override
  public void setDateCreated(String date) {

  }

  @Override
  public List<RowGroup> getGroups() {
    return null;
  }

  @Override
  public void setGroups(List<RowGroup> groups) {

  }

  @Override
  public void addRowsRelationship(FeatureListRow a, FeatureListRow b,
      RowsRelationship relationship) {

  }

  @Override
  public void addRowsRelationships(R2RMap<? extends RowsRelationship> map, Type relationship) {

  }

  @Override
  public @NotNull Map<Type, R2RMap<RowsRelationship>> getRowMaps() {
    return null;
  }

  @Override
  public @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getFeatureTypeChangeListeners() {
    return null;
  }

  @Override
  public @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getRowTypeChangeListeners() {
    return null;
  }
}

package util.lipidvalidationtest;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.*;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.correlation.R2RNetworkingMaps;
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.util.MemoryMapStorage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

/**
 * Dummy implementation of ModularFeatureList for testing purposes.
 */
@SuppressWarnings("rawtypes")
public class DummyModularFeatureList implements FeatureList {

    public static final DateFormat DATA_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private final ObservableList<RawDataFile> dataFiles;
    private final ObservableList<FeatureListRow> featureListRows;
    private final ObservableList<FeatureListAppliedMethod> appliedMethods;
    private final ObservableMap<RawDataFile, List<? extends io.github.mzmine.datamodel.Scan>> selectedScans;
    private final ObservableSet<DataType> rowTypes;
    private final ObservableSet<DataType> featureTypes;
    private final String name;
    private final String dateCreated;

    public DummyModularFeatureList(String name, @Nullable MemoryMapStorage storage,
                                   @NotNull List<RawDataFile> dataFiles) {
        this.name = name;
        this.dateCreated = DATA_FORMAT.format(new Date());
        this.dataFiles = FXCollections.observableList(dataFiles);
        this.featureListRows = FXCollections.observableArrayList();
        this.appliedMethods = FXCollections.observableArrayList();
        this.selectedScans = FXCollections.observableMap(new HashMap<>());
        this.rowTypes = FXCollections.observableSet(new LinkedHashSet<>());
        this.featureTypes = FXCollections.observableSet(new LinkedHashSet<>());
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    /**
     * @param name
     * @return
     */
    @Override
    public String setName(@NotNull String name) {
        return null;
    }

    /**
     * @param bindings
     */
    @Override
    public void addRowBinding(@NotNull List<RowBinding> bindings) {

    }

    /**
     * @param featureType
     * @param listener
     */
    @Override
    public void addFeatureTypeListener(DataType featureType, DataTypeValueChangeListener listener) {

    }

    /**
     * @param rowType
     * @param listener
     */
    @Override
    public void addRowTypeListener(DataType rowType, DataTypeValueChangeListener listener) {

    }

    /**
     * @param rowType
     * @param listener
     */
    @Override
    public void removeRowTypeListener(DataType rowType, DataTypeValueChangeListener listener) {

    }

    /**
     * @param featureType
     * @param listener
     */
    @Override
    public void removeFeatureTypeListener(DataType featureType, DataTypeValueChangeListener listener) {

    }

    /**
     *
     */
    @Override
    public void applyRowBindings() {

    }

    /**
     * @param row
     */
    @Override
    public void applyRowBindings(FeatureListRow row) {

    }

    /**
     * @return
     */
    @Override
    public ObservableSet<DataType> getFeatureTypes() {
        return null;
    }

    /**
     * @param types
     */
    @Override
    public void addFeatureType(Collection<DataType> types) {

    }

    /**
     * @param types
     */
    @Override
    public void addFeatureType(@NotNull DataType<?>... types) {

    }

    /**
     * @param types
     */
    @Override
    public void addRowType(Collection<DataType> types) {

    }

    /**
     * @param types
     */
    @Override
    public void addRowType(@NotNull DataType<?>... types) {

    }

    /**
     * @return
     */
    @Override
    public ObservableSet<DataType> getRowTypes() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public int getNumberOfRawDataFiles() {
        return 0;
    }

    @Override
    public @NotNull ObservableList<RawDataFile> getRawDataFiles() {
        return dataFiles;
    }

    /**
     * @param file
     * @return
     */
    @Override
    public boolean hasRawDataFile(RawDataFile file) {
        return false;
    }

    /**
     * @param position Position of the raw data file in the matrix (running numbering from left
     *                 0,1,2,...)
     * @return
     */
    @Override
    public RawDataFile getRawDataFile(int position) {
        return null;
    }

    /**
     * @return
     */
    @Override
    public int getNumberOfRows() {
        return 0;
    }

    /**
     * @param row         Row of the feature list
     * @param rawDataFile Raw data file where the feature is detected/estimated
     * @return
     */
    @Override
    public ModularFeature getFeature(int row, RawDataFile rawDataFile) {
        return null;
    }

    /**
     * @param rawDataFile
     * @return
     */
    @Override
    public List<ModularFeature> getFeatures(RawDataFile rawDataFile) {
        return null;
    }

    /**
     * @param row
     * @return
     */
    @Override
    public FeatureListRow getRow(int row) {
        return null;
    }

    @Override
    public @NotNull ObservableList<FeatureListRow> getRows() {
        return featureListRows;
    }

    /**
     * @param rows new rows to set
     */
    @Override
    public void setRows(FeatureListRow... rows) {

    }

    /**
     * @param rowNum
     * @param row
     */
    @Override
    public void removeRow(int rowNum, FeatureListRow row) {

    }

    /**
     * @return
     */
    @Override
    public Stream<FeatureListRow> stream() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public Stream<FeatureListRow> parallelStream() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public Stream<ModularFeature> streamFeatures() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public Stream<ModularFeature> parallelStreamFeatures() {
        return null;
    }

    /**
     * @param name force set this name
     * @return
     */
    @Override
    public String setNameNoChecks(@NotNull String name) {
        return null;
    }

    /**
     * @param file  the data file of the scans
     * @param scans all filtered scans that were used to build the chromatogram in the first place.
     *              For ion mobility data, the Frames are returned
     */
    @Override
    public void setSelectedScans(@NotNull RawDataFile file, @Nullable List<? extends Scan> scans) {

    }

    /**
     * @param file the data file
     * @return
     */
    @Override
    public @Nullable List<? extends Scan> getSeletedScans(@NotNull RawDataFile file) {
        return null;
    }

    /**
     * @param rtRange Retention time range
     * @return
     */
    @Override
    public List<FeatureListRow> getRowsInsideScanRange(Range<Float> rtRange) {
        return null;
    }

    /**
     * @param mzRange m/z range
     * @return
     */
    @Override
    public List<FeatureListRow> getRowsInsideMZRange(Range<Double> mzRange) {
        return null;
    }

    /**
     * @param rtRange Retention time range
     * @param mzRange m/z range
     * @return
     */
    @Override
    public List<FeatureListRow> getRowsInsideScanAndMZRange(Range<Float> rtRange, Range<Double> mzRange) {
        return null;
    }

    /**
     * @param file    Raw data file
     * @param rtRange Retention time range
     * @return
     */
    @Override
    public List<Feature> getFeaturesInsideScanRange(RawDataFile file, Range<Float> rtRange) {
        return null;
    }

    /**
     * @param file    Raw data file
     * @param mzRange m/z range
     * @return
     */
    @Override
    public List<Feature> getFeaturesInsideMZRange(RawDataFile file, Range<Double> mzRange) {
        return null;
    }

    /**
     * @param file    Raw data file
     * @param rtRange Retention time range
     * @param mzRange m/z range
     * @return
     */
    @Override
    public List<Feature> getFeaturesInsideScanAndMZRange(RawDataFile file, Range<Float> rtRange, Range<Double> mzRange) {
        return null;
    }

    /**
     * @return
     */
    @Override
    public double getDataPointMaxIntensity() {
        return 0;
    }

    /**
     * @param row
     */
    @Override
    public void addRow(FeatureListRow row) {

    }

    /**
     * @param row
     */
    @Override
    public void removeRow(int row) {

    }

    /**
     * @param row
     */
    @Override
    public void removeRow(FeatureListRow row) {

    }

    /**
     * @param feature
     * @return
     */
    @Override
    public int getFeatureListRowNum(Feature feature) {
        return 0;
    }

    /**
     * @param feature
     * @return
     */
    @Override
    public FeatureListRow getFeatureRow(Feature feature) {
        return null;
    }

    /**
     * @param appliedMethod
     */
    @Override
    public void addDescriptionOfAppliedTask(FeatureListAppliedMethod appliedMethod) {

    }

    @Override
    public @NotNull ObservableList<FeatureListAppliedMethod> getAppliedMethods() {
        return appliedMethods;
    }

    /**
     * @return
     */
    @Override
    public Range<Double> getRowsMZRange() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public Range<Float> getRowsRTRange() {
        return null;
    }

    /**
     * @param id id
     * @return
     */
    @Override
    public FeatureListRow findRowByID(int id) {
        return null;
    }

    @Override
    public @NotNull String getDateCreated() {
        return dateCreated;
    }

    /**
     * @param date
     */
    @Override
    public void setDateCreated(String date) {

    }

    /**
     * @return
     */
    @Override
    public List<RowGroup> getGroups() {
        return null;
    }

    /**
     * @param groups
     */
    @Override
    public void setGroups(List<RowGroup> groups) {

    }

    /**
     * @return
     */
    @Override
    public @NotNull R2RNetworkingMaps getRowMaps() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getFeatureTypeChangeListeners() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getRowTypeChangeListeners() {
        return null;
    }

    // Implement more methods as needed for your test

}


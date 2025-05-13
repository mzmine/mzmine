package util.lipidvalidationtest;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.*;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.FeaturesType;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotation;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids.FoundLipid;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineReactionMatch;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Dummy implementation of ModularFeatureListRow for testing purposes.
 */
@SuppressWarnings("rawtypes")
public class DummyFeatureListRow implements FeatureListRow {

    private final ObservableMap<DataType, Object> map = FXCollections.observableMap(new HashMap<>());
    private final Map<RawDataFile, ModularFeature> features;
    private final DummyModularFeatureList flist;

    private List<MatchedLipid> matchedLipids;

    private Double mz;
    private Float rt;
    private Float height;

    public DummyFeatureListRow(@NotNull DummyModularFeatureList flist, int id) {
        this.flist = flist;
        map.put(new IDType(), id);

        if (!flist.getRawDataFiles().isEmpty()) {
            features = new HashMap<>();
            map.put(new FeaturesType(), features);
        } else {
            features = Collections.emptyMap();
        }
    }


    public void setMatchedLipid(List<MatchedLipid> matchedLipid) {
        this.matchedLipids = matchedLipid;
    }

    /**
     * @return
     */
    @Override
    public List<RawDataFile> getRawDataFiles() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public Integer getID() {
        return null;
    }


    /**
     * @return
     */
    @Override
    public int getNumberOfFeatures() {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public Range<Double> getMZRange() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public List<ModularFeature> getFeatures() {

        return features.values().stream().filter(f -> f.getFeatureStatus() != FeatureStatus.UNKNOWN)
                .toList();
    }

    /**
     * @param rawData
     * @return
     */
    @Override
    public @Nullable Feature getFeature(RawDataFile rawData) {
        return null;
    }

    /**
     * @param rawData             associated raw data file
     * @param feature             added feature
     * @param updateByRowBindings updates values by row bindings if true. In case multiple features
     *                            are added, this option may be set to false. Remember to call
     *                            {@link #applyRowBindings()}.
     */
    @Override
    public void addFeature(RawDataFile rawData, Feature feature, boolean updateByRowBindings) {

    }

    /**
     * @param file
     */
    @Override
    public void removeFeature(RawDataFile file) {

    }

    /**
     * @param feature
     * @return
     */
    @Override
    public boolean hasFeature(Feature feature) {
        return false;
    }

    /**
     * @param rawData
     * @return
     */
    @Override
    public boolean hasFeature(RawDataFile rawData) {
        return false;
    }

    /**
     * @return
     */
    @Override
    public Double getAverageMZ() {
        return mz;
    }

    /**
     * @param averageMZ
     */
    @Override
    public void setAverageMZ(Double averageMZ) {
        mz = averageMZ;
    }

    /**
     * @return
     */
    @Override
    public Float getAverageRT() {
        return rt;
    }

    /**
     * @param averageRT
     */
    @Override
    public void setAverageRT(Float averageRT) {
        rt = averageRT;
    }

    /**
     * @return
     */
    @Override
    public @Nullable Float getAverageMobility() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public @Nullable Float getAverageCCS() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public Float getMaxHeight() {
        return height;
    }

    /**
     * @return
     */
    @Override
    public Integer getRowCharge() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public Float getMaxArea() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public String getComment() {
        return null;
    }

    /**
     * @param comment
     */
    @Override
    public void setComment(String comment) {

    }

    /**
     * @param identity  New feature identity
     * @param preffered boolean value to define this identity as preferred identity
     */
    @Override
    public void addFeatureIdentity(FeatureIdentity identity, boolean preffered) {

    }

    /**
     * @return
     */
    @Override
    public @NotNull List<OnlineReactionMatch> getOnlineReactionMatches() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public @NotNull List<MatchedLipid> getLipidMatches() {
        return matchedLipids;
    }

    /**
     * @param identity Feature identity
     */
    @Override
    public void removeFeatureIdentity(FeatureIdentity identity) {

    }

    /**
     * @return
     */
    @Override
    public @Nullable ManualAnnotation getManualAnnotation() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public List<FeatureIdentity> getPeakIdentities() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public FeatureIdentity getPreferredFeatureIdentity() {
        return null;
    }

    /**
     * @param identity Preferred identity
     */
    @Override
    public void setPreferredFeatureIdentity(FeatureIdentity identity) {

    }

    /**
     * @return
     */
    @Override
    public FeatureInformation getFeatureInformation() {
        return null;
    }

    /**
     * @param featureInformation object
     */
    @Override
    public void setFeatureInformation(FeatureInformation featureInformation) {

    }

    /**
     * @return
     */
    @Override
    public Float getMaxDataPointIntensity() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public Feature getBestFeature() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public Scan getMostIntenseFragmentScan() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public @NotNull List<Scan> getAllFragmentScans() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public IsotopePattern getBestIsotopePattern() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public ObservableMap<DataType, Object> getMap() {
        return null;
    }

    @Override
    public <T> T get(Class<? extends DataType<T>> type) {
        try {
            DataType<T> keyInstance = type.getDeclaredConstructor().newInstance();
            return (T) map.get(keyInstance);
        } catch (Exception e) {
            return null;
        }
    }

    public Map<RawDataFile, ? extends Feature> getFilesFeatures() {
        return features;
    }

    @Override
    public DummyModularFeatureList getFeatureList() {
        return flist;
    }

    /**
     * @param flist
     */
    @Override
    public void setFeatureList(@NotNull FeatureList flist) {

    }

    /**
     * @return
     */
    @Override
    public @NotNull List<CompoundDBAnnotation> getCompoundAnnotations() {
        return null;
    }

    /**
     * @param annotations sets all compound annotations.
     */
    @Override
    public void setCompoundAnnotations(List<CompoundDBAnnotation> annotations) {

    }

    /**
     * @param id
     */
    @Override
    public void addCompoundAnnotation(CompoundDBAnnotation id) {

    }

    /**
     * @param id
     */
    @Override
    public void addSpectralLibraryMatch(SpectralDBAnnotation id) {

    }

    /**
     * @return
     */
    @Override
    public boolean isIdentified() {
        return false;
    }

    /**
     * @return
     */
    @Override
    public RowGroup getGroup() {
        return null;
    }

    /**
     * @param group
     */
    @Override
    public void setGroup(RowGroup group) {

    }

    /**
     * @return
     */
    @Override
    public @Nullable List<IonIdentity> getIonIdentities() {
        return null;
    }

    /**
     * @param ions list of ion identities
     */
    @Override
    public void setIonIdentities(@Nullable List<IonIdentity> ions) {

    }

    /**
     * @return
     */
    @Override
    public List<ResultFormula> getFormulas() {
        return null;
    }

    /**
     * @param formulas
     */
    @Override
    public void setFormulas(List<ResultFormula> formulas) {

    }

    /**
     * @param formula
     * @param preferred
     */
    @Override
    public void addFormula(ResultFormula formula, boolean preferred) {

    }

    /**
     * @param matches new list of matches
     */
    @Override
    public void setSpectralLibraryMatch(List<SpectralDBAnnotation> matches) {

    }

    /**
     * @return
     */
    @Override
    public @NotNull List<SpectralDBAnnotation> getSpectralLibraryMatches() {
        return null;
    }

    /**
     * @param matchedLipid the matched lipid
     */
    @Override
    public void addLipidAnnotation(MatchedLipid matchedLipid) {

    }

    /**
     * @return
     */
    @Override
    public Stream<ModularFeature> streamFeatures() {
        return null;
    }

    /**
     * @param matches
     */
    @Override
    public void addSpectralLibraryMatches(List<SpectralDBAnnotation> matches) {

    }

    /**
     * @return
     */
    @Override
    public @Nullable Range<Float> getMobilityRange() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public boolean hasIsotopePattern() {
        return false;
    }

    /**
     * @return
     */
    @Override
    public @Nullable Object getPreferredAnnotation() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public @Nullable String getPreferredAnnotationName() {
        return null;
    }

    /**
     * @param lipid
     */
    @Override
    public void addLipidValidation(FoundLipid lipid) {

    }

    public void setMaxHeight(float v) {
        height = v;
    }


    // Add other method stubs as needed
}


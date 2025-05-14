package util.lipidvalidationtest;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.*;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.util.MemoryMapStorage;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Dummy implementation of RawDataFile for testing purposes.
 */
public class DummyRawDataFile implements RawDataFile {

    private PolarityType polarity;

    public void setPolarity(PolarityType polarity) {
        this.polarity = polarity;
    }

    /**
     * @return
     */
    @Override
    public @NotNull String getName() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public @Nullable String getAbsolutePath() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public int getNumOfScans() {
        return 0;
    }

    /**
     * @param msLevel
     * @return
     */
    @Override
    public int getNumOfScans(int msLevel) {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public int getMaxRawDataPoints() {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public @NotNull Range<Double> getDataMZRange() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public boolean isContainsZeroIntensity() {
        return false;
    }

    /**
     * @return
     */
    @Override
    public boolean isContainsEmptyScans() {
        return false;
    }

    /**
     * @return
     */
    @Override
    public MassSpectrumType getSpectraType() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public @NotNull Range<Float> getDataRTRange() {
        return null;
    }

    /**
     * @param msLevel
     * @return
     */
    @Override
    public @NotNull Range<Double> getDataMZRange(int msLevel) {
        return null;
    }

    /**
     * @param msLevel
     * @return
     */
    @Override
    public @NotNull Range<Float> getDataRTRange(Integer msLevel) {
        return null;
    }

    /**
     * @param msLevel
     * @return
     */
    @Override
    public double getDataMaxBasePeakIntensity(int msLevel) {
        return 0;
    }

    /**
     * @param msLevel
     * @return
     */
    @Override
    public double getDataMaxTotalIonCurrent(int msLevel) {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public @NotNull List<PolarityType> getDataPolarity() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public Color getColorAWT() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public javafx.scene.paint.Color getColor() {
        return null;
    }

    /**
     * @param color
     */
    @Override
    public void setColor(javafx.scene.paint.Color color) {

    }

    /**
     * @return
     */
    @Override
    public ObjectProperty<javafx.scene.paint.Color> colorProperty() {
        return null;
    }

    /**
     *
     */
    @Override
    public void close() {

    }

    /**
     * @return
     */
    @Override
    public @Nullable MemoryMapStorage getMemoryMapStorage() {
        return null;
    }

    /**
     * @param newScan
     * @throws IOException
     */
    @Override
    public void addScan(Scan newScan) throws IOException {

    }

    /**
     * @return
     */
    @Override
    public @NotNull ObservableList<Scan> getScans() {
        return null;
    }

    /**
     * @param scan   the scan that was changed
     * @param old    old mass list
     * @param masses new mass list
     */
    @Override
    public void applyMassListChanged(Scan scan, MassList old, MassList masses) {

    }

    /**
     * @return
     */
    @Override
    public @NotNull ObservableList<FeatureList.FeatureListAppliedMethod> getAppliedMethods() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public List<OtherDataFile> getOtherDataFiles() {
        return null;
    }
}

package io.github.mzmine.datamodel;

import com.google.common.collect.Range;
import io.github.mzmine.MobilogramListRow;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public interface MobilogramList {
    public interface MobilogramListAppliedMethod {

        @Nonnull
        public String getDescription();

        @Nonnull
        public String getParameters();

    }

    /**
     * @return Short descriptive name for the Mobilogram list
     */
    public String getName();

    /**
     * Change the name of this Mobilogram list
     */
    public void setName(String name);

    /**
     * Returns number of raw data files participating in the Mobilogram list
     */
    public int getNumberOfRawDataFiles();

    /**
     * Returns all raw data files participating in the Mobilogram list
     */
    public RawDataFile[] getRawDataFiles();

    /**
     * Returns true if this Mobilogram list contains given file
     */
    public boolean hasRawDataFile(RawDataFile file);

    /**
     * Returns a raw data file
     *
     * @param position Position of the raw data file in the matrix (running numbering from left
     *        0,1,2,...)
     */
    public RawDataFile getRawDataFile(int position);

    /**
     * Returns number of rows in the alignment result
     */
    public int getNumberOfRows();

    /**
     * Returns the Mobilogram of a given raw data file on a give row of the Mobilogram list
     *
     * @param row Row of the Mobilogram list
     * @param rawDataFile Raw data file where the Mobilogram is detected/estimated
     */
    public IMSFeature getMobilogram(int row, RawDataFile rawDataFile);

    /**
     * Returns all Mobilograms for a raw data file
     */
    public IMSFeature[] getMobilograms(RawDataFile rawDataFile);

    /**
     * Returns all Mobilograms on one row
     */
    public MobilogramListRow getRow(int row);

    /**
     * Returns all Mobilogram list rows
     */
    public MobilogramListRow[] getRows();

    /**
     * Creates a stream of MobilogramListRows
     *
     * @return
     */
    public Stream<MobilogramListRow> stream();

    /**
     * Creates a parallel stream of MobilogramListRows
     *
     * @return
     */
    public Stream<MobilogramListRow> parallelStream();

    /**
     * Returns all rows with average retention time within given range
     *
     * @param startRT Start of the retention time range
     * @param endRT End of the retention time range
     */
    public MobilogramListRow[] getRowsInsideScanRange(Range<Double> rtRange);

    /**
     * Returns all rows with average m/z within given range
     *
     * @param startMZ Start of the m/z range
     * @param endMZ End of the m/z range
     */
    public MobilogramListRow[] getRowsInsideMZRange(Range<Double> mzRange);

    /**
     * Returns all rows with average m/z and retention time within given range
     *
     * @param startRT Start of the retention time range
     * @param endRT End of the retention time range
     * @param startMZ Start of the m/z range
     * @param endMZ End of the m/z range
     */
    public MobilogramListRow[] getRowsInsideScanAndMZRange(Range<Double> rtRange,
                                                           Range<Double> mzRange);

    /**
     * Returns all Mobilograms overlapping with a retention time range
     *
     * @param startRT Start of the retention time range
     * @param endRT End of the retention time range
     */
    public IMSFeature[] getMobilogramsInsideScanRange(RawDataFile file, Range<Double> rtRange);

    /**
     * Returns all Mobilograms in a given m/z range
     *
     * @param startMZ Start of the m/z range
     * @param endMZ End of the m/z range
     */
    public IMSFeature[] getMobilogramsInsideMZRange(RawDataFile file, Range<Double> mzRange);

    /**
     * Returns all Mobilograms in a given m/z & retention time ranges
     *
     * @param startRT Start of the retention time range
     * @param endRT End of the retention time range
     * @param startMZ Start of the m/z range
     * @param endMZ End of the m/z range
     */
    public IMSFeature[] getMobilogramsInsideScanAndMZRange(RawDataFile file, Range<Double> rtRange,
                                                           Range<Double> mzRange);

    /**
     * Returns maximum raw data point intensity among all Mobilograms in this Mobilogram list
     *
     * @return Maximum intensity
     */
    public double getDataPointMaxIntensity();

    /**
     * Add a new row to the Mobilogram list
     */
    public void addRow(MobilogramListRow row);

    /**
     * Removes a row from this Mobilogram list
     *
     */
    public void removeRow(int row);

    /**
     * Removes a row from this Mobilogram list
     *
     */
    public void removeRow(MobilogramListRow row);

    /**
     * Returns a row number of given Mobilogram
     */
    public int getMobilogramRowNum(IMSFeature Mobilogram);

    /**
     * Returns a row containing given Mobilogram
     */
    public MobilogramListRow getMobilogramRow(IMSFeature Mobilogram);

    public void addDescriptionOfAppliedTask(MobilogramListAppliedMethod appliedMethod);

    /**
     * Returns all tasks (descriptions) applied to this Mobilogram list
     */
    public MobilogramListAppliedMethod[] getAppliedMethods();

    /**
     * Returns the whole m/z range of the Mobilogram list
     */
    public Range<Double> getRowsMZRange();

    /**
     * Returns the whole retention time range of the Mobilogram list
     */
    public Range<Double> getRowsRTRange();

    /**
     * Returns the whole mobility range of the Mobilogram list
     */
    public Range<Double> getRowsMobilityRange();


}

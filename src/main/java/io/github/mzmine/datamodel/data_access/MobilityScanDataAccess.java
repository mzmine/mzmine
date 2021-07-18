package io.github.mzmine.datamodel.data_access;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.impl.masslist.FrameMassList;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MobilityScanDataAccess implements MobilityScan {

  protected final IMSRawDataFile dataFile;
  protected final MobilityScanDataType type;
  protected final int totalFrames;

  protected final List<Frame> eligibleFrames;
  protected final double[] mzs;
  protected final double[] intensities;

  // current data
  protected final double[] mobilities;
  protected Frame currentFrame;
  protected MobilityScan currentMobilityScan;
  protected int currentNumberOfDataPoints = -1;

  protected int currentNumberOfMobilityScans = -1;
  protected int currentMobilityScanIndex = -1;
  protected int currentFrameIndex = -1;

  /**
   * The intended use of this memory access is to loop over all scans and access data points via
   * {@link #getMzValue(int)} and {@link #getIntensityValue(int)}
   *
   * @param dataFile  target data file to loop over all scans or mass lists
   * @param type      processed or raw data
   * @param selection processed or raw data
   */
  protected MobilityScanDataAccess(IMSRawDataFile dataFile, MobilityScanDataType type,
      ScanSelection selection) {
    this(dataFile, type, (List<Frame>) selection.getMatchingScans(dataFile.getFrames()));
  }

  public MobilityScanDataAccess(@NotNull final IMSRawDataFile dataFile,
      @NotNull final MobilityScanDataType type, @NotNull final List<Frame> frames) {
    this.dataFile = dataFile;
    this.type = type;

    // count matching scans
    eligibleFrames = frames;
    totalFrames = eligibleFrames.size();

    final int length = getMaxNumberOfDataPoints(eligibleFrames);
    mzs = new double[length];
    intensities = new double[length];

    final int maxNumMobilityScans = eligibleFrames.stream()
        .mapToInt(Frame::getNumberOfMobilityScans).max().orElse(0);
    mobilities = new double[maxNumMobilityScans];
  }

  /**
   * @return Number of data points in the current scan depending of the defined DataType
   * (RAW/CENTROID)
   */
  @Override
  public int getNumberOfDataPoints() {
    return currentNumberOfDataPoints;
  }

  public MobilityScan getCurrentMobilityScan() {
    return currentMobilityScan;
  }

  @NotNull
  @Override
  public RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  public double getMobility() {
    return mobilities[currentMobilityScanIndex];
  }

  @Override
  public MobilityType getMobilityType() {
    return currentFrame.getMobilityType();
  }

  /**
   * @return The current frame.
   */
  @Override
  public Frame getFrame() {
    return currentFrame;
  }

  @Override
  public float getRetentionTime() {
    return currentFrame.getRetentionTime();
  }

  @Override
  public int getMobilityScanNumber() {
    return currentMobilityScan.getMobilityScanNumber();
  }

  @Nullable
  @Override
  public ImsMsMsInfo getMsMsInfo() {
    return currentMobilityScan.getMsMsInfo();
  }

  @Override
  public void addMassList(@NotNull MassList massList) {
    throw new UnsupportedOperationException("Cannot set a mass list for a MobilityScanDataAccess.");
  }

  public boolean hasNextMobilityScan() {
    return currentMobilityScanIndex + 1 < currentNumberOfMobilityScans;
  }

  /**
   * Set the data to the next scan, if available. Returns the scan for additional data access. m/z
   * and intensity values should be accessed from this data class via {@link #getMzValue(int)} and
   * {@link #getIntensityValue(int)}
   *
   * @return the scan or null
   * @throws MissingMassListException if DataType.CENTROID is selected and mass list is missing in
   *                                  the current scan
   */
  public MobilityScan nextMobilityScan() throws MissingMassListException {
    currentMobilityScanIndex++;
    currentMobilityScan = currentFrame.getMobilityScan(currentMobilityScanIndex);
    if (type == MobilityScanDataType.CENTROID) {
      final MassList ml = currentMobilityScan.getMassList();
      if (ml == null) {
        throw new MissingMassListException(
            "Mobility scan " + currentMobilityScanIndex + " does not contain a mass list.",
            currentFrame);
      }
      currentNumberOfDataPoints = ml.getNumberOfDataPoints();
      ml.getMzValues(mzs);
      ml.getIntensityValues(intensities);
    } else if (type == MobilityScanDataType.RAW) {
      currentNumberOfDataPoints = currentMobilityScan.getNumberOfDataPoints();
      currentMobilityScan.getMzValues(mzs);
      currentMobilityScan.getIntensityValues(intensities);
    }
    return currentMobilityScan;
  }

  public void resetMobilityScan() {
    currentMobilityScanIndex = -1;
    currentMobilityScan = null;
    currentNumberOfDataPoints = 0;
  }

  public boolean hasNextFrame() {
    return currentFrameIndex + 1 < totalFrames;
  }

  /**
   * Sets the next frame. The mobility scan index is reset to -1, therefore {@link
   * #nextMobilityScan} has to be called before accessing new scan data.
   *
   * @return the next Frame.
   */
  public Frame nextFrame() {
    currentFrameIndex++;
    currentFrame = eligibleFrames.get(currentFrameIndex);
    currentNumberOfMobilityScans = currentFrame.getNumberOfMobilityScans();
    currentMobilityScanIndex = -1;
    currentFrame.getMobilities().get(0, mobilities, 0, currentNumberOfMobilityScans);
    return currentFrame;
  }

  /**
   * Resets the {@link MobilityScanDataAccess} to the initial state equal to the initialisation.
   */
  public void resetFrame() {
    currentFrameIndex = -1;
    currentFrame = null;
    currentNumberOfMobilityScans = -1;
    currentMobilityScanIndex = -1;
    currentMobilityScan = null;
  }

  /**
   * @return
   */
  public MassList getMassList() {
    return getCurrentMobilityScan().getMassList();
  }

  /**
   * Get mass-to-charge ratio at index
   *
   * @param index data point index
   * @return
   */
  @Override
  public double getMzValue(int index) {
    assert index < getNumberOfDataPoints() && index >= 0;
    return mzs[index];
  }

  /**
   * Get intensity at index
   *
   * @param index data point index
   * @return
   */
  @Override
  public double getIntensityValue(int index) {
    assert index < getNumberOfDataPoints() && index >= 0;
    if (intensities[index] > 1E4) {
      return intensities[index];
    }
    return intensities[index];
  }

  /**
   * Number of selected scans
   *
   * @return
   */
  public int getNumberOfScans() {
    return totalFrames;
  }

  /**
   * Maximum number of data points is used to create the arrays that back the data
   *
   * @return
   */
  private int getMaxNumberOfDataPoints(List<Frame> frames) {
    return switch (type) {
      case RAW -> frames.stream().mapToInt(Frame::getMaxMobilityScanDataPoints).max().orElse(0);
      case CENTROID -> Math.max(
          frames.stream().filter(f -> f.getMassList() instanceof FrameMassList).mapToInt(
              frame -> ((FrameMassList) frame.getMassList()).getMaxMobilityScanDatapoints()).max()
              .orElse(0),
          frames.stream().filter(f -> f.getMassList() instanceof ScanPointerMassList)
              .mapToInt(Frame::getMaxMobilityScanDataPoints).max().orElse(0));
    };
   /* int forloop = 0;
    if (type == MobilityScanDataType.CENTROID) {
      for (Frame frame : frames) {
        int dp = ((FrameMassList)frame.getMassList()).getMaxMobilityScanDatapoints();
        if(dp > forloop)
          forloop = dp;
      }
    }
    return forloop;*/
  }

  // ###############################################
  // general MassSpectrum methods

  @Override
  public MassSpectrumType getSpectrumType() {
    return switch (type) {
      case RAW -> currentFrame.getSpectrumType();
      case CENTROID -> MassSpectrumType.CENTROIDED;
    };
  }

  @Nullable
  @Override
  public Double getBasePeakMz() {
    Integer index = getBasePeakIndex();
    return index != null && index >= 0 ? getMzValue(index) : null;
  }

  @Nullable
  @Override
  public Double getBasePeakIntensity() {
    Integer index = getBasePeakIndex();
    return index != null && index >= 0 ? getIntensityValue(index) : null;
  }

  @Nullable
  @Override
  public Integer getBasePeakIndex() {
    switch (type) {
      case RAW:
        return getCurrentMobilityScan().getBasePeakIndex();
      case CENTROID:
        MassList masses = getMassList();
        return masses == null ? null : masses.getBasePeakIndex();
      default:
        throw new IllegalStateException("Unexpected value: " + type);
    }
  }

  @Nullable
  @Override
  public Range<Double> getDataPointMZRange() {
    switch (type) {
      case RAW:
        return getCurrentMobilityScan().getDataPointMZRange();
      case CENTROID:
        MassList masses = getMassList();
        return masses == null ? null : masses.getDataPointMZRange();
      default:
        throw new IllegalStateException("Unexpected value: " + type);
    }
  }

  public List<Frame> getEligibleFrames() {
    return eligibleFrames;
  }

  @Nullable
  @Override
  public Double getTIC() {
    return ArrayUtils.sum(intensities, 0, currentNumberOfDataPoints);
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all scans and data points");
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all scans and data points");
  }

  @Override
  public Stream<DataPoint> stream() {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all scans and data points");
  }

  @NotNull
  @Override
  public Iterator<DataPoint> iterator() {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all scans and data points");
  }
}

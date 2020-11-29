package io.github.mzmine.datamodel;

import com.google.common.collect.Range;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a raw data file, that offers the additional mobility dimension within the scans.
 */
public interface IMSRawDataFile extends RawDataFile {

  /**
   * @param frameNum the Frame number.
   * @return The frame. Null if no frame with that number exists.
   */
  @Nullable
  public Frame getFrame(int frameNum);

  /**
   * @return List of all frames in this raw data file. List may be empty if no frames exist.
   */
  @Nonnull
  public List<Frame> getFrames();

  /**
   * @param msLevel The ms level
   * @return List of frames with the given msLevel. May be empty.
   */
  @Nonnull
  public List<Frame> getFrames(int msLevel);

  /**
   * @param msLevel
   * @param rtRange
   * @return List of frames with given ms mlevel in the specified rt window. May be empty.
   */
  @Nonnull
  public List<Frame> getFrames(int msLevel, Range<Float> rtRange);

  /**
   * @return The number of frames in this raw data file. equivalent to {@link
   * IMSRawDataFile#getFrames()}.size()
   */
  public int getNumberOfFrames();

  /**
   * @return The frame numbers in this raw data file. Might be empty.
   */
  @Nonnull
  public Set<Integer> getFrameNumbers();

  /**
   * @param msLevel The ms level of the given frames.
   * @return The frame numbers in the specified ms level. Might be empty.
   */
  @Nonnull
  public List<Integer> getFrameNumbers(int msLevel);

  /**
   * @param msLevel
   * @param rtRange
   * @return The frame numbers in the specified ms level and rt range. Might be empty.
   */
  @Nonnull
  public List<Integer> getFrameNumbers(int msLevel, @Nonnull Range<Float> rtRange);

  /**
   * @return The mobility range of this raw data file. Might be empty.
   */
  @Nonnull
  public Range<Double> getDataMobilityRange();

  /**
   * @param msLevel
   * @return The mobility range for the given ms level. Might be empty.
   */
  @Nonnull
  public Range<Double> getDataMobilityRange(int msLevel);

  /**
   * @param rt the retention time (in minutes)
   * @return The frame closest to the specified retention time. null if the given time is outside of
   * the {@link RawDataFile#getDataRTRange()} or no frames exist.
   */
  @Nullable
  public Frame getFrameAtRt(double rt);

  /**
   * @param rt      the retention time (in seconds)
   * @param msLevel the ms level
   * @return The frame closest to the given retention time at the specified ms level. null if the
   * given time is outside of the {@link RawDataFile#getDataRTRange()} or no frames exist.
   */
  @Nullable
  public Frame getFrameAtRt(double rt, int msLevel);

  /**
   * @return The {@link MobilityType} of this data file. {@link MobilityType#NONE} if no mobility
   * dimension was recorded.
   */
  @Nonnull
  public MobilityType getMobilityType();

}

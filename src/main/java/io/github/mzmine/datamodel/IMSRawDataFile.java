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
   * @return The frame.
   */
  @Nullable
  public Frame getFrame(int frameNum);

  @Nonnull
  public List<Frame> getFrames();

  @Nonnull
  public List<Frame> getFrames(int msLevel);

  @Nonnull
  public List<Frame> getFrames(int msLevel, Range<Double> rtRange);

  public int getNumberOfFrames();

  /**
   * @return The numbers of the frames in this raw data file.
   */
  @Nonnull
  public Set<Integer> getFrameNumbers();

  /**
   * @param msLevel The ms level of the given frames.
   * @return The frame numbers in the specified ms level. Might be empty.
   */
  @Nonnull
  public List<Integer> getFrameNumbers(int msLevel);

  @Nonnull
  public List<Integer> getFrameNumbers(int msLevel, @Nonnull Range<Double> rtRange);

  /**
   * @return The mobility range of this raw data file.
   */
  @Nonnull
  public Range<Double> getDataMobilityRange();

  @Nonnull
  public Range<Double> getDataMobilityRange(int msLevel);

  /**
   * @param rt the retention time (in minutes)
   * @return The frame closest to the specified retention time. null if the given time is outside
   * of the {@link RawDataFile#getDataRTRange()}
   */
  @Nullable
  public Frame getFrameAtRt(double rt);

  /**
   * @param rt      the retention time (in seconds)
   * @param msLevel the ms level
   * @return The frame closest to the given retention time at the specified ms level. null if the
   * given time is outside of the {@link RawDataFile#getDataRTRange()}
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

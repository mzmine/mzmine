/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel;

import com.google.common.collect.Range;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import it.unimi.dsi.fastutil.doubles.DoubleImmutableList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a raw data file, that offers the additional mobility dimension within the scans.
 *
 * @author https://github.com/SteffenHeu
 */
public interface IMSRawDataFile extends RawDataFile {

  /**
   * @param frameNum the Frame number.
   * @return The frame. Null if no frame with that number exists.
   */
  @Nullable
  Frame getFrame(int frameNum);

  /**
   * @return Set of all frames in this raw data file. List may be empty if no frames exist.
   */
  @NotNull
  List<? extends Frame> getFrames();

  /**
   * @param msLevel The ms level
   * @return List of frames with the given msLevel. May be empty.
   */
  @NotNull
  List<? extends Frame> getFrames(int msLevel);

  /**
   * @param msLevel
   * @param rtRange
   * @return List of frames with given ms mlevel in the specified rt window. May be empty.
   */
  @NotNull
  List<? extends Frame> getFrames(int msLevel, Range<Float> rtRange);

  /**
   * @return The number of frames in this raw data file. equivalent to
   *         {@link IMSRawDataFile#getFrames()}.size()
   */
  int getNumberOfFrames();

  /**
   * @param msLevel The ms level of the given frames.
   * @return The frame numbers in the specified ms level. Might be empty.
   */
  @NotNull
  List<Scan> getFrameNumbers(int msLevel);

  /**
   * @param msLevel
   * @param rtRange
   * @return The frame numbers in the specified ms level and rt range. Might be empty.
   */
  @NotNull
  List<Scan> getFrameNumbers(int msLevel, @NotNull Range<Float> rtRange);

  /**
   * @return The mobility range of this raw data file. Might be empty.
   */
  @NotNull
  Range<Double> getDataMobilityRange();

  /**
   * @param msLevel
   * @return The mobility range for the given ms level. Might be empty.
   */
  @NotNull
  Range<Double> getDataMobilityRange(int msLevel);

  /**
   * @param rt the retention time (in minutes)
   * @return The frame closest to the specified retention time. null if the given time is outside of
   *         the {@link RawDataFile#getDataRTRange()} or no frames exist.
   */
  @Nullable
  Frame getFrameAtRt(double rt);

  /**
   * @param rt the retention time (in seconds)
   * @param msLevel the ms level
   * @return The frame closest to the given retention time at the specified ms level. null if the
   *         given time is outside of the {@link RawDataFile#getDataRTRange()} or no frames exist.
   */
  @Nullable
  Frame getFrameAtRt(double rt, int msLevel);

  /**
   * @return The {@link MobilityType} of this data file. {@link MobilityType#NONE} if no mobility
   *         dimension was recorded.
   */
  @NotNull
  MobilityType getMobilityType();

  @Nullable CCSCalibration getCCSCalibration();

  void setCCSCalibration(@Nullable CCSCalibration calibration);

  int addMobilityValues(double[] mobilities);

  DoubleImmutableList getSegmentMobilities(int segment);
}

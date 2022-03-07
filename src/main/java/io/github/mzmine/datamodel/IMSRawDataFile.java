/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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

  DoubleImmutableList getSegmentMobilities(int segment);
}

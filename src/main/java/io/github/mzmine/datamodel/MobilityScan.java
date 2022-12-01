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
import io.github.mzmine.util.MemoryMapStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mass spectrum acquired during an ion mobility experiment. The main implementation of this
 * interface, ({@link io.github.mzmine.datamodel.impl.StoredMobilityScan}, is created on demand by
 * the respective parent {@link Frame}. This means, that if available, existing instances shall be
 * reused as done in, e.g. {@link io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries#copy(MemoryMapStorage)}
 * and other methods that copy lists of scans.
 * <p></p>
 * During project import, the instances of this interface are cached in a {@link
 * io.github.mzmine.modules.io.projectload.CachedIMSFrame} to minimize ram consumption by using the
 * same instances throughout all feature lists.
 *
 * @author https://github.com/SteffenHeu
 */
public interface MobilityScan extends Scan {

  static final double DEFAULT_MOBILITY = -1.0d;

  @NotNull RawDataFile getDataFile();

  /**
   * @return The mobility of this sub-spectrum. The unit will depend on the respective mass
   * spectrometer and can be checked via {@link MobilityScan#getMobilityType()}.
   */
  double getMobility();

  /**
   * See {@link MobilityType}
   *
   * @return The type of mobility acquired in this mass spectrum.
   */
  MobilityType getMobilityType();

  /**
   * @return THe frame this spectrum belongs to.
   */
  Frame getFrame();

  /**
   * @return The retention time of the frame when this spectrum was acquired.
   */
  float getRetentionTime();

  /**
   * @return The index of this mobility subscan.
   */
  int getMobilityScanNumber();

  @Nullable MassList getMassList();

  @Override
  default int compareTo(@NotNull Scan s) {
    int result = Integer.compare(this.getScanNumber(), s.getScanNumber());
    if (result != 0) {
      return result;
    }
    result = Float.compare(this.getRetentionTime(), s.getRetentionTime());
    if (result != 0 || !(s instanceof MobilityScan ms)) {
      return result;
    }

    return Integer.compare(this.getMobilityScanNumber(), ms.getMobilityScanNumber());
  }

  /**
   * Returns the frame id. The mobility scan number can be accessed via {@link
   * MobilityScan#getMobilityScanNumber()}.
   *
   * @return The frame Id. {@link Frame#getScanNumber()}.
   */
  @Override
  default int getScanNumber() {
    return getFrame().getFrameId();
  }

  @NotNull
  @Override
  default String getScanDefinition() {
    return getFrame().getScanDefinition() + " - Mobility scan #" + getMobilityScanNumber();
  }

  @NotNull
  @Override
  default Range<Double> getScanningMZRange() {
    return getFrame().getScanningMZRange();
  }

  @NotNull
  @Override
  default PolarityType getPolarity() {
    return getFrame().getPolarity();
  }

  @Override
  default int getMSLevel() {
    return getFrame().getMSLevel();
  }

  /**
   *
   * @return The injection time of the frame or null.
   */
  @Override
  @Nullable Float getInjectionTime();
}

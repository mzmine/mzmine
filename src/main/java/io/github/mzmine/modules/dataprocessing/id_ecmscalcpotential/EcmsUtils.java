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

package io.github.mzmine.modules.dataprocessing.id_ecmscalcpotential;

public class EcmsUtils {

  /**
   * @param length Tubing length. Unit must be the same as id unit.
   * @param id     Tubing ID. Unit must be the same as length.
   * @return The inner tubing volume (in cubic  input unit) mm = mm³ = uL.
   */
  public static double getTubingVolume(final double length, final double id) {
    return Math.pow(id / 2, 2d) * Math.PI * length;
  }

  /**
   * @param flowRate The flow rate. Volume unit must be the same as unit of volume parameter.
   * @param volume   The tubing dead volume. Unit must be the same as volume-unit of flow rate
   *                 parameter.
   * @return Delay time in unit of flow rate parameter.
   */
  public static double getDelayTime(final double flowRate, final double volume) {
    return volume / flowRate;
  }

  /**
   * @param rt                 Retention time in minutes.
   * @param potentialRampSpeed Potential ramp speed in mV/s.
   * @param delayTime          Delay time in s.
   * @return The potential at the given retention time.
   */
  public static double getPotentialAtRt(final float rt, final double delayTime, final double potentialRampSpeed) {
    return (rt * 60d - delayTime) * potentialRampSpeed;
  }

  /**
   * @param delayTime   Delay time in s.
   * @param rampSpeed   Ramp Speed in mV/s.
   * @param potential   Potential in mV.
   * @return            The retention time of a given potential.
   */
  public static float getRtAtPotential(final double delayTime, final double rampSpeed, final double potential) {
    return (float) ((float) delayTime + (potential / (rampSpeed * 60d)));
  }
}

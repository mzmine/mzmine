/*
 * Copyright 2006-2022 The MZmine Development Team
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

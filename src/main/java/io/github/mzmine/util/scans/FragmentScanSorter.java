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

package io.github.mzmine.util.scans;

import static java.util.Comparator.comparingInt;
import static java.util.Comparator.nullsLast;
import static java.util.Comparator.reverseOrder;

import io.github.mzmine.datamodel.Scan;
import java.util.Comparator;

/**
 * Sorts fragment scans from best to lesser representative MS/MS scans. Generally MS2>MS3>MS4
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public abstract class FragmentScanSorter {

  /**
   * MS1>MS2>MS3 then highest TIC then highest number of data points
   */
  public static final Comparator<Scan> DEFAULT_TIC = comparingInt(Scan::getMSLevel) //
      .thenComparing(Scan::getTIC, nullsLast(reverseOrder()))
      .thenComparing(Scan::getNumberOfDataPoints, reverseOrder());

  /**
   * MS1>MS2>MS3 then highest number of data points then highest TIC
   */
  public static final Comparator<Scan> DEFAULT_NUMBER_OF_DATA_POINTS = comparingInt(
      Scan::getMSLevel) //
      .thenComparing(Scan::getNumberOfDataPoints, reverseOrder())
      .thenComparing(Scan::getTIC, nullsLast(reverseOrder()));


}

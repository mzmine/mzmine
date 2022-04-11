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

package io.github.mzmine.util.scans.sorting;

import java.util.Comparator;

import org.jetbrains.annotations.NotNull;

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;

/**
 * Scan needs at least one MassList
 * 
 * @author Robin Schmid
 *
 */
public class ScanSorter implements Comparator<Scan> {

  private final MassListSorter comp;

  /**
   * Scans need a MassList
   *
   * @param noiseLevel
   * @param sortMode sorting mode
   */
  public ScanSorter(double noiseLevel, ScanSortMode sortMode) {
    comp = new MassListSorter(noiseLevel, sortMode);
  }

  @Override
  public int compare(Scan a, Scan b) {
    MassList ma = a.getMassList();
    MassList mb = b.getMassList();
    if (ma == null)
      throw new RuntimeException(new MissingMassListException(a));
    else if (mb == null)
      throw new RuntimeException(new MissingMassListException(b));
    else
      return comp.compare(ma, mb);
  }

}

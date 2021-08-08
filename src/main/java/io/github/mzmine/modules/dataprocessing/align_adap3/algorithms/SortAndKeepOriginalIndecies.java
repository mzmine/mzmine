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
package io.github.mzmine.modules.dataprocessing.align_adap3.algorithms;

import java.util.Comparator;

/**
 * <p>
 * SortAndKeepOriginalIndecies class.
 * </p>
 *
 * @author owenmyers Modified by Dharak Shah to include in MSDK
 */
public class SortAndKeepOriginalIndecies implements Comparator<Integer> {
  private final double[] dataArr;

  /**
   * <p>
   * Constructor for SortAndKeepOriginalIndecies.
   * </p>
   *
   * @param dataInArr an array of double.
   */
  public SortAndKeepOriginalIndecies(double[] dataInArr) {
    this.dataArr = dataInArr;
  }

  /**
   * <p>
   * makeArrOfIndecies.
   * </p>
   *
   * @return an array of {@link Integer} objects.
   */
  public Integer[] makeArrOfIndecies() {
    Integer[] indecies = new Integer[dataArr.length];
    for (int i = 0; i < dataArr.length; i++) {
      indecies[i] = i;
    }
    return indecies;
  }

  /** {@inheritDoc} */
  @Override
  public int compare(Integer index1, Integer index2) {
    return Double.compare(dataArr[index1], dataArr[index2]);
  }


}

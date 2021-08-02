/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
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

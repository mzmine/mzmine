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
 *
 *
 */

package stats;

import io.github.mzmine.modules.dataanalysis.significance.StatisticUtils;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StatsUtilsTest {

  double[][] data = new double[][]{
      {0, 2, 1, 2},
      {1, 2, 5, 0},
      {1, 1, 1, 0}};

  @Test
  void testCentering() {

    final Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(data);
    final RealMatrix centered = StatisticUtils.performMeanCenter(matrix, false);

    Assertions.assertEquals((double) -2 /3, centered.getEntry(0, 0));
    Assertions.assertEquals(2 - (double) 5 /3 , centered.getEntry(1, 1));
    Assertions.assertEquals(1- (double) 7 /3, centered.getEntry(0, 2));
    Assertions.assertEquals(0- (double) 2 /3, centered.getEntry(2, 3));
  }

  @Test
  void test() {
    final Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(data);
    final RealMatrix unitVar = StatisticUtils.scaleToUnitVariance(matrix, false);

    Assertions.assertEquals(0, unitVar.getEntry(0, 0));
    Assertions.assertEquals(1, unitVar.getEntry(1, 1));
    Assertions.assertEquals(0.5, unitVar.getEntry(2, 1));
    Assertions.assertEquals((double) 1 /5, unitVar.getEntry(0, 2));
    Assertions.assertEquals(0, unitVar.getEntry(2, 3));
  }
}

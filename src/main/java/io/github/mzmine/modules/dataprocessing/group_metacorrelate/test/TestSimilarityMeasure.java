/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.test;

import net.sf.mzmine.modules.featurelistmethods.grouping.metacorrelate.datastructure.CorrelationData.SimilarityMeasure;
import net.sf.mzmine.util.maths.similarity.Similarity;

public class TestSimilarityMeasure {

  public static void main(String[] args) {
    double noise = 1E4;
    double small = 2E4;
    double a = noise * 2;
    double b = noise * 2;

    double[][] zeroLineGoHigh = new double[][] {{noise, noise}, {noise, noise}, {noise, noise},
        {noise, noise}, {noise, noise}, {small, small}};

    int size = 10;
    double[][] good = new double[10][2];
    for (int i = 0; i < good.length; i++) {
      good[i][0] = a * (i + 1);
      good[i][1] = b * (i + 1);
    }

    double[][] nonRatioA = new double[10][2];
    for (int i = 0; i < good.length; i++) {
      nonRatioA[i][0] = a + 10 * (i + 1);
      nonRatioA[i][1] = b * (i + 1);
    }
    double[][] nonRatioB = new double[10][2];
    for (int i = 0; i < good.length; i++) {
      nonRatioB[i][0] = a * (i + 1);
      nonRatioB[i][1] = b + 10 * (i + 1);
    }

    double[][] missingTopA1 = clone(good);
    missingTopA1[size - 1][0] = noise;
    double[][] missingTopB1 = clone(good);
    missingTopB1[size - 1][1] = noise;

    double[][] missingStartA1 = clone(good);
    missingStartA1[0][0] = noise;
    double[][] missingStartB1 = clone(good);
    missingStartB1[0][1] = noise;

    double[][] missingMidA1 = clone(good);
    missingMidA1[size / 2][0] = noise;
    double[][] missingMidB1 = clone(good);
    missingMidB1[size / 2][1] = noise;

    System.out.println("\n zero line");
    test(zeroLineGoHigh);
    System.out.println("\n Good");
    test(good);
    System.out.println("\n nonRatioA");
    test(nonRatioA);
    System.out.println("\n nonRatioB");
    test(nonRatioB);
    System.out.println("\n Top A1");
    test(missingTopA1);
    System.out.println("\n Top B1");
    test(missingTopB1);
    System.out.println("\n Mid A1");
    test(missingMidA1);
    System.out.println("\n Mid B1");
    test(missingMidB1);
    System.out.println("\n Start A1");
    test(missingStartA1);
    System.out.println("\n Start B1");
    test(missingStartB1);
  }

  private static double[][] clone(double[][] good) {
    double[][] data = new double[good.length][];
    for (int i = 0; i < data.length; i++) {
      data[i] = good[i].clone();
    }
    return data;
  }

  private static void test(double[][] zeroLineGoHigh) {
    for (double[] d : zeroLineGoHigh)
      System.out.print(d[0] + "," + d[1] + " ; ");
    System.out.println("\nPearson " + SimilarityMeasure.PEARSON.calc(zeroLineGoHigh));
    System.out.println("Spearman " + SimilarityMeasure.SPEARMAN.calc(zeroLineGoHigh));
    System.out.println("Cosine " + SimilarityMeasure.COSINE_SIM.calc(zeroLineGoHigh));
    System.out.println(
        "Cor var proportionality " + SimilarityMeasure.LOG_RATIO_VARIANCE_1.calc(zeroLineGoHigh));
    System.out.println("Cor var 2 " + SimilarityMeasure.LOG_RATIO_VARIANCE_2.calc(zeroLineGoHigh));

    System.out.println("Slope: " + Similarity.REGRESSION_SLOPE.calc(zeroLineGoHigh));
    System.out
        .println("Significance: " + Similarity.REGRESSION_SLOPE_SIGNIFICANCE.calc(zeroLineGoHigh));
  }

}

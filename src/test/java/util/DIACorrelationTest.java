/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package util;

import io.github.mzmine.datamodel.features.correlation.CorrelationData;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureCorrelationUtil.DIA;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DIACorrelationTest {

  private static final Logger logger = Logger.getLogger(DIACorrelationTest.class.getName());

  @Test
  void testDIACorrelation() {

    // values from gaussian function

    // main values
    double[] mainX = {-2, -1.9, -1.8, -1.7, -1.6, -1.5, -1.4, -1.3, -1.2, -1.1, -1, -0.9, -0.8,
        -0.7, -0.6, -0.5, -0.4, -0.3, -0.2, -0.1, 0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1,
        1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2};

    double[] mainY = new double[mainX.length];

    for (int i = 0; i < mainX.length; i++) {
      mainY[i] = gauss(mainX[i], 0.25, 0);
    }

    // shift x values so we have to interpolate
    double[] xShiftedBy0_05 = {-1.95, -1.85, -1.75, -1.65, -1.55, -1.45, -1.35, -1.25, -1.15, -1.05,
        -0.95, -0.85, -0.75, -0.65, -0.55, -0.45, -0.35, -0.25, -0.15, -0.05, 0.05, 0.15, 0.25,
        0.35, 0.45, 0.55, 0.65, 0.75, 0.85, 0.95, 1.05, 1.15, 1.25, 1.35, 1.45, 1.55, 1.65, 1.75,
        1.85, 1.95, 2.05};
    double[] y_shifted = new double[xShiftedBy0_05.length];
    for (int i = 0; i < mainX.length; i++) {
      y_shifted[i] = gauss(xShiftedBy0_05[i], 0.25, 0);
    }
    final CorrelationData correlationData = DIA.corrFeatureShape(mainX, mainY, xShiftedBy0_05,
        y_shifted, 5, 2, 0.0001);
    logger.info(() -> "Pearson shifted " + correlationData.getPearsonR());
    logger.info(() -> "Cosine shifted " + correlationData.getCosineSimilarity());

    // distort shape by making it wider
    double[] y_distorted = new double[xShiftedBy0_05.length];
    for (int i = 0; i < mainX.length; i++) {
      y_distorted[i] = gauss(xShiftedBy0_05[i], 0.4, 0);
    }
    final CorrelationData correlationData2 = DIA.corrFeatureShape(mainX, mainY, xShiftedBy0_05,
        y_distorted, 5, 2, 0.0001);
    logger.info(() -> "Pearson shifted " + correlationData2.getPearsonR());
    logger.info(() -> "Cosine shifted " + correlationData2.getCosineSimilarity());
    Assertions.assertTrue(correlationData.getPearsonR() > correlationData2.getPearsonR());
    Assertions.assertTrue(
        correlationData.getCosineSimilarity() > correlationData2.getCosineSimilarity());

    // shift and distort
    double[] y_shifted_distorted = new double[xShiftedBy0_05.length];
    for (int i = 0; i < mainX.length; i++) {
      y_shifted_distorted[i] = gauss(xShiftedBy0_05[i], 0.4, 0.25);
    }
    final CorrelationData correlationData3 = DIA.corrFeatureShape(mainX, mainY, xShiftedBy0_05,
        y_shifted_distorted, 5, 2, 0.0001);
    logger.info(() -> "Pearson shifted_distorted " + correlationData3.getPearsonR());
    logger.info(() -> "Cosine shifted_distorted " + correlationData3.getCosineSimilarity());
    Assertions.assertTrue(correlationData2.getPearsonR() > correlationData3.getPearsonR());
    Assertions.assertTrue(
        correlationData2.getCosineSimilarity() > correlationData3.getCosineSimilarity());

    // less points, more to interpolate
    double[] x_lessPoints = new double[10];
    for (int i = 0; i < x_lessPoints.length; i++) {
      x_lessPoints[i] = xShiftedBy0_05[(int) (i * ((double) xShiftedBy0_05.length
          / x_lessPoints.length))];
    }
    double[] y_lessPoints = new double[x_lessPoints.length];
    for (int i = 0; i < x_lessPoints.length; i++) {
      y_lessPoints[i] = gauss(x_lessPoints[i], 0.25, 0);
    }
    final CorrelationData correlationData4 = DIA.corrFeatureShape(mainX, mainY, x_lessPoints,
        y_lessPoints, 5, 2, 0.0001);
    logger.info(() -> "Pearson less points " + correlationData4.getPearsonR());
    logger.info(() -> "Cosine less points " + correlationData4.getCosineSimilarity());
//    Assertions.assertTrue(correlationData2.getPearsonR() > correlationData4.getPearsonR());
//    Assertions.assertTrue(correlationData2.getCosineSimilarity() > correlationData4.getCosineSimilarity());

    /*final double[][] interpolatedShape = DIA.getInterpolatedShape(mainX, mainY, x_lessPoints,
        y_lessPoints);
    for (int i = 0; i < interpolatedShape[0].length; i++) {
      System.out.println(interpolatedShape[0][i]);
    }
    System.out.println();
    System.out.println();
    for (int i = 0; i < interpolatedShape[0].length; i++) {
      System.out.println(interpolatedShape[1][i]);
    }*/

    final CorrelationData correlationData5 = DIA.corrFeatureShape(x_lessPoints,
        y_lessPoints, mainX, mainY, 5, 2, 0.0001);
    logger.info(() -> "Pearson less points_reversed " + correlationData5.getPearsonR());
    logger.info(() -> "Cosine less points_reversed " + correlationData5.getCosineSimilarity());
  }

  private double gauss(double x, double sigma, double mu) {
    return 1 / (sigma * Math.sqrt(2 * Math.PI)) * Math.exp(
        -0.5 * (Math.pow(mu - x, 2) / Math.pow(sigma, 2)));
  }
}

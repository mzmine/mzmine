/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.modules.alignment.ransac;

import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math.stat.regression.SimpleRegression;
import Jama.Matrix;

import java.util.Collections;

public class RANSAC {

    /**
     * input:
     * data - a set of observed data points
     * n - the minimum number of data values required to fit the model
     * k - the maximum number of iterations allowed in the algorithm
     * t - a threshold value for determining when a data point fits a model
     * d - the number of close data values required to assert that a model fits well to data
     *
     * output:
     * model which best fit the data
     */
    private int n;
    private double d = 1;
    private int k = 0;
    private Random rnd;
    private int AlsoNumber;
    private double numRatePoints,  t;
    private boolean nonLinear;

    public RANSAC(RansacAlignerParameters parameters) {

        this.numRatePoints = (Double) parameters.getParameterValue(RansacAlignerParameters.NMinPoints);

        this.t = (Double) parameters.getParameterValue(RansacAlignerParameters.Margin);

        this.k = (Integer) parameters.getParameterValue(RansacAlignerParameters.Iterations);

        this.nonLinear = (Boolean) parameters.getParameterValue(RansacAlignerParameters.nonLinear);

    }

    /**
     * Set all parameters and start ransac.
     * @param data vector with the points which represent all possible alignments.
     */
    public void alignment(Vector<AlignStructMol> data) {
        try {
            rnd = new Random();
            // If the model is non linear 4 points are taken to build the model,
            // if it is linear only 2 points are taken.
            if (nonLinear) {
                n = 4;
            } else {
                n = 2;
            }

            // Minimun number of points required to assert that a model fits well to data
            if (data.size() < 10) {
                d = 3;
            } else {
                d = data.size() * numRatePoints;
            }

            // Calculate the number of trials if the user has not define them
            if (k == 0) {
                k = (int) getK();
            }

            ransac(data);
        } catch (Exception exepcion) {
        }
    }

    /**
     * Calculate k (number of trials)
     * @return number of trials "k" required to select a subset of n good data points.
     */
    public double getK() {
        double w = numRatePoints;
        double b = Math.pow(w, n);
        return Math.log10(1 - 0.99) / Math.log10(1 - b) + (Math.sqrt(1 - b) / b);
    }

    /**
     * RANSAC algorithm
     * @param data vector with the points which represent all possible alignments.
     */
    public void ransac(Vector<AlignStructMol> data) {
        double besterr = 9.9E99;

        for (int iterations = 0; iterations < k; iterations++) {
            AlsoNumber = n;
            // Get the initial points
            boolean initN = getInitN(data);
            if (!initN) {
                continue;
            }

            // Calculate the model
            if (nonLinear) {
                getAllModelPointsCurve(data);
            } else {
                getAllModelPoints(data);
            }

            // If the model has the minimun number of points
            if (AlsoNumber >= d) {
                // Get the error of the model based on the number of points
                double error = 9.9E99;
                try {
                    error = newError(data);
                } catch (Exception ex) {
                    Logger.getLogger(RANSAC.class.getName()).log(Level.SEVERE, null, ex);
                }

                // If the error is less than the error of the last model
                if (error < besterr) {
                    besterr = error;
                    for (int i = 0; i < data.size(); i++) {
                        AlignStructMol alignStruct = data.elementAt(i);
                        if (alignStruct.ransacAlsoInLiers || alignStruct.ransacMaybeInLiers) {
                            alignStruct.Aligned = true;
                        } else {
                            alignStruct.Aligned = false;
                        }

                        alignStruct.ransacAlsoInLiers = false;
                        alignStruct.ransacMaybeInLiers = false;

                    }
                }
            }
            // remove the model
            for (int i = 0; i < data.size(); i++) {
                AlignStructMol alignStruct = data.elementAt(i);
                alignStruct.ransacAlsoInLiers = false;
                alignStruct.ransacMaybeInLiers = false;
            }


        }
    }

    /**
     * Take the initial points ramdoly. The points are divided by the initial number
     * of points. If the fractions contain enough number of points took one point
     * from each part.
     * @param data vector with the points which represent all possible alignments.
     * @return false if there is any problem.
     */
    private boolean getInitN(Vector<AlignStructMol> data) {

        int fractionNPoints = (data.size() / n) - 1;

        if (fractionNPoints > 2) {

            if (!nonLinear) {
                // Take 2 points
                int index = rnd.nextInt(fractionNPoints / 2);
                data.elementAt(index).ransacMaybeInLiers = true;

                index = rnd.nextInt(fractionNPoints);
                index += fractionNPoints;
                data.elementAt(index).ransacMaybeInLiers = true;
            } else {
                // Take 4 points
                int index = rnd.nextInt(fractionNPoints / 2);
                data.elementAt(index).ransacMaybeInLiers = true;

                index = rnd.nextInt(fractionNPoints);
                index += fractionNPoints;
                data.elementAt(index).ransacMaybeInLiers = true;

                index = rnd.nextInt(fractionNPoints);
                index += (fractionNPoints * 2);
                data.elementAt(index).ransacMaybeInLiers = true;

                index = rnd.nextInt(fractionNPoints);
                index += (fractionNPoints * 3);
                data.elementAt(index).ransacMaybeInLiers = true;
            }
            return true;
        } else if (data.size() > 1) {
            for (int i = 0; i < n; i++) {
                int index = rnd.nextInt(data.size());
                if (data.elementAt(index).ransacMaybeInLiers) {
                    i--;
                } else {
                    data.elementAt(index).ransacMaybeInLiers = true;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Build the model creating a line with the 2 points
     * @param data vector with the points which represent all possible alignments.
     */
    public void getAllModelPoints(Vector<AlignStructMol> data) {

        // Create the regression line using the two points
        SimpleRegression regression = new SimpleRegression();

        for (int i = 0; i < data.size(); i++) {
            AlignStructMol point = data.elementAt(i);
            if (point.ransacMaybeInLiers) {
                regression.addData(point.RT, point.RT2);
            }
        }

        // Add all the points which fit the model (the difference between the point
        // and the regression line is less than "t"
        for (AlignStructMol point : data) {
            double intercept = regression.getIntercept();
            double slope = regression.getSlope();

            double y = point.RT2;
            double bestY = intercept + (point.RT * slope);
            if (Math.abs(y - bestY) < t) {
                point.ransacAlsoInLiers = true;
                AlsoNumber++;
            } else {
                point.ransacAlsoInLiers = false;
            }
        }

    }

    /**
     *
     * @param data vector with the points which represent all possible alignments.
     */
    public void getAllModelPointsCurve(Vector<AlignStructMol> data) {
       
        Vector<AlignStructMol> points = new Vector<AlignStructMol>();
        for (int i = 0; i < data.size(); i++) {
            AlignStructMol point = data.elementAt(i);
            if (point.ransacMaybeInLiers) {
                points.add(point);
            }
        }
        Collections.sort(points, new AlignStructMol());
        Vector<double[]> fourPoints = new Vector<double[]>();
        for (AlignStructMol p : points) {
            double[] v = new double[2];
            v[0] = p.RT;
            v[1] = p.RT2;
            fourPoints.addElement(v);
        }


        try {
            double[] curve = getPolinomialCoefficients(fourPoints);
            for (AlignStructMol point : data) {
                double y = point.RT2;
                double bestY = curve[0] * Math.pow(point.RT, 3) + curve[1] * Math.pow(point.RT, 2) + curve[2] * point.RT + curve[3];
                
                if (Math.abs(y - bestY) < t) {
                    point.ransacAlsoInLiers = true;
                    AlsoNumber++;
                } else {
                    point.ransacAlsoInLiers = false;
                }
            }
        } catch (Exception e) {
        }
    }

    private double[] getPolinomialCoefficients(Vector<double[]> points) {
        double[][] m = new double[4][4];
        double[] y = new double[4];
        for (int i = 0; i < 4; i++) {
            double[] point = points.elementAt(i);
            y[i] = point[1];
            m[i][0] = Math.pow(point[0], 3);
            m[i][1] = Math.pow(point[0], 2);
            m[i][2] = point[0];
            m[i][3] = 1;
        }

        Matrix matrix = new Matrix(m);
        Matrix iMatrix = matrix.inverse();

        double[][] im = iMatrix.getArray();
        double[] values = new double[4];
        for (int i = 0; i < 4; i++) {
            values[i] = im[i][0] * y[0] + im[i][1] * y[1] + im[i][2] * y[2] + im[i][3] * y[3];
        }

        return values;
    }
   
    /**
     * calculate the error in the model
     * @param data vector with the points which represent all possible alignments.
     * @param regression regression of the alignment points
     * @return the error in the model
     */
    public double newError(Vector<AlignStructMol> data) throws Exception {

        double numT = 1;
        for (int i = 0; i < data.size(); i++) {
            if (data.elementAt(i).ransacAlsoInLiers || data.elementAt(i).ransacMaybeInLiers) {
                numT++;
            }
        }
        return 1 / numT;

    }
}
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

import Jama.Matrix;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.main.MZmineCore;
import org.apache.commons.math.stat.regression.SimpleRegression;

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
	private AlignmentChart chart;
	private boolean isCurve,  showChart;
	private RansacAlignerParameters parameters;

	public RANSAC(RansacAlignerParameters parameters) {
		this.parameters = parameters;

		this.numRatePoints = (Double) parameters.getParameterValue(RansacAlignerParameters.NMinPoints);

		this.t = (Double) parameters.getParameterValue(RansacAlignerParameters.Margin);

		this.k = (Integer) parameters.getParameterValue(RansacAlignerParameters.OptimizationIterations);

		this.isCurve = (Boolean) parameters.getParameterValue(RansacAlignerParameters.curve);

		this.showChart = (Boolean) parameters.getParameterValue(RansacAlignerParameters.chart);

		if (showChart) {
			chart = new AlignmentChart("result");
		}
	}

	/**
	 * Set all parameters and start ransac.
	 * @param data vector with the points which represent all possible alignments.
	 */
	public void alignment(Vector<AlignStructMol> data) {

		this.rnd = new Random();
		// If the model is a curve 3 points are taken to build the model,
		// if it is a line only 2 points are taken.
		if (isCurve) {
			n = 3;
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
			k = (int) this.getK();
		}

		// Visualization of the aligmnet
		if (showChart) {
			chart.setVisible(true);
			MZmineCore.getDesktop().addInternalFrame(chart);
		}
		this.ransac(data);
	}

	/**
	 * Calculate k (number of trials)
	 * @return number of trials "k" required to select a subset of n good data points.
	 */
	public double getK() {
		double w = 0.05;
		double b = Math.pow(w, 2);
		return 3 * (1 / b);
	}

	/**
	 * RANSAC algorithm
	 * @param data vector with the points which represent all possible alignments.
	 */
	public void ransac(Vector<AlignStructMol> data) {
		double besterr = 9.9E99;

		for (int iterations = 0; iterations < this.k; iterations++) {
			this.AlsoNumber = this.n;
			// Get the initial points
			boolean initN = this.getInitN(data);
			if (!initN) {
				continue;
			}

			// Calculate the model
			if (isCurve) {
				this.getAllModelPointsCurve(data);
			} else {
				this.getAllModelPoints(data);
			}

			// If the model has the minimun number of points
			if (this.AlsoNumber >= this.d) {
				// Get the error of the model based on the number of points
				double error = 10000;
				try {
					error = this.newError(data);
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

					// Remove conficts on the alignments
					this.deleteRepeatsAlignments(data);

					// Visualizantion of the new selected model
					if (showChart) {
						chart.removeSeries();
						chart.addSeries(data, "na", true);
						chart.printAlignmentChart();
						chart.moveToFront();
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

		if (fractionNPoints > 8) {

			if (!isCurve) {
				// Take 2 points
				int index = rnd.nextInt(fractionNPoints);
				data.elementAt(index).ransacMaybeInLiers = true;

				index = rnd.nextInt(fractionNPoints);
				index += fractionNPoints;
				data.elementAt(index).ransacMaybeInLiers = true;
			} else {
				// Take 3 points
				int index = rnd.nextInt(fractionNPoints);
				data.elementAt(index).ransacMaybeInLiers = true;

				index = rnd.nextInt(fractionNPoints);
				index += fractionNPoints;
				data.elementAt(index).ransacMaybeInLiers = true;

				index = rnd.nextInt(fractionNPoints);
				index += fractionNPoints;
				data.elementAt(index).ransacMaybeInLiers = true;
			}
			return true;
		} else if (data.size() > 1) {
			for (int i = 0; i < this.n; i++) {
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
		int indexRow1 = 0;
		int indexRow2 = 0;

		// Create the regression line using the two points
		SimpleRegression regression = new SimpleRegression();

		for (int i = 0; i < data.size(); i++) {
			AlignStructMol point = data.elementAt(i);
			if (point.ransacMaybeInLiers) {
				regression.addData(point.row1.getPeaks()[indexRow1].getRT(), point.row2.getPeaks()[indexRow2].getRT());
			}
		}

		// Add all the points which fit the model (the difference between the point
		// and the regression line is less than "t"
		for (int i = 0; i < data.size(); i++) {
			AlignStructMol point = data.elementAt(i);
			double intercept = regression.getIntercept();
			double slope = regression.getSlope();

			double y = point.row2.getPeaks()[indexRow2].getRT();
			double bestY = intercept + (point.row1.getPeaks()[indexRow1].getRT() * slope);
			if (Math.abs(y - bestY) < t) {
				point.ransacAlsoInLiers = true;
				this.AlsoNumber++;
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
		int indexRow1 = 0;
		int indexRow2 = 0;

		// Obtain the variables of the curve equation
		Vector<double[]> threePoints = new Vector<double[]>();
		for (int i = 0; i < data.size(); i++) {
			AlignStructMol point = data.elementAt(i);
			if (point.ransacMaybeInLiers) {
				double[] pointCoord = new double[2];
				pointCoord[0] = point.row1.getPeaks()[indexRow1].getRT();
				pointCoord[1] = point.row2.getPeaks()[indexRow2].getRT();
				threePoints.addElement(pointCoord);
			}
		}
		// Add all the points which fit the model (the difference between the point
		// and the curve is less than "t"
		try {
			double[] curve = this.getCurveEquation(threePoints);
			for (AlignStructMol point : data) {
				double y = point.row2.getPeaks()[indexRow2].getRT();
				double bestY = curve[0] * Math.pow(point.row1.getPeaks()[indexRow1].getRT(), 2) + curve[1] * point.row1.getPeaks()[indexRow1].getRT() + curve[2];
				if (Math.abs(y - bestY) < t) {
					point.ransacAlsoInLiers = true;
					this.AlsoNumber++;
				} else {
					point.ransacAlsoInLiers = false;
				}
			}
		} catch (Exception e) {
		}

	}

	/**
	 *
	 * @param threePoints Three XY coordinates
	 * @return the three variables of the curve equation
	 */
	private double[] getCurveEquation(Vector<double[]> threePoints) {
		double[][] m = new double[3][3];
		double[] y = new double[3];
		for (int i = 0; i < 3; i++) {
			double[] point = threePoints.elementAt(i);
			y[i] = point[1];
			m[i][0] = Math.pow(point[0], 2);
			m[i][1] = point[0];
			m[i][2] = 1;
		}

		Matrix matrix = new Matrix(m);

		Matrix iMatrix = matrix.inverse();
		double[][] im = iMatrix.getArray();
		double[] values = new double[3];
		for (int i = 0; i < 3; i++) {
			values[i] = im[i][0] * y[0] + im[i][1] * y[1] + im[i][2] * y[2];
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

	/**
	 * If the same lipid is aligned with two differents lipids delete the alignment farest to the ransac regression line of all aligned points.
	 * @param data vector with the points which represent all possible alignments.
	 */
	private void deleteRepeatsAlignments(Vector<AlignStructMol> data) {
		for (AlignStructMol structMol1 : data) {
			if (structMol1.Aligned) {
				for (AlignStructMol structMol2 : data) {
					if (structMol1 != structMol2 && structMol2.Aligned) {
						if ((structMol1.row1 == structMol2.row1 || structMol1.row1 == structMol2.row2) || (structMol1.row2 == structMol2.row1 || structMol1.row2 == structMol2.row2)) {
							if (Math.abs(structMol1.row1.getDataPointMaxIntensity() - structMol1.row2.getDataPointMaxIntensity()) < Math.abs(structMol2.row1.getDataPointMaxIntensity() - structMol2.row2.getDataPointMaxIntensity())) {
								structMol2.Aligned = false;
							} else {
								structMol1.Aligned = false;
							}
						}
					}
				}
			}
		}
	}
}
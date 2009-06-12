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

public class RANSACDEVST {

	/**
	 * input:
	 * data - a set of observed data points
	 * model - a model that can be fitted to data points
	 * n - the minimum number of data values required to fit the model
	 * k - the maximum number of iterations allowed in the algorithm
	 * t - a threshold value for determining when a data point fits a model
	 * d - the number of close data values required to assert that a model fits well to data
	 *
	 * output:
	 * bestfit - model parameters which best fit the data (or nil if no good model is found)
	 */
	int n;
	double d;
	int k = 0;
	Random rnd;
	int AlsoNumber;
	double numRatePoints, margin;
	AlignmentChart chart;
	boolean isCurve, showChart;
	RansacAlignerParameters parameters;

	public RANSACDEVST(RansacAlignerParameters parameters) {
		this.parameters = parameters;
		this.numRatePoints = (Double) parameters.getParameterValue(RansacAlignerParameters.NMinPoints);
		this.margin = (Double) parameters.getParameterValue(RansacAlignerParameters.Margin);
		this.k = (Integer) parameters.getParameterValue(RansacAlignerParameters.OptimizationIterations);
		this.isCurve = (Boolean) parameters.getParameterValue(RansacAlignerParameters.curve);
		this.showChart = (Boolean) parameters.getParameterValue(RansacAlignerParameters.chart);

		if (showChart) {
			chart = new AlignmentChart("result");
		}
	}

	/**
	 * Set all parameters and start ransac.
	 * @param v vector with the points which represent all possible alignments.
	 */
	public void alignment(Vector<AlignStructMol> v) {
		this.rnd = new Random();
		if (isCurve) {
			n = 3;
		} else {
			n = 2;
		}
		if (v.size() < 10) {
			d = 3;
		} else {
			d = v.size() * numRatePoints;
		}
		if (k == 0) {
			k = (int) this.getK();
		}
		if (showChart) {
			//chart.addSeries(v, "na", true);
			//chart.printAlignmentChart();
			chart.setVisible(true);
			MZmineCore.getDesktop().addInternalFrame(chart);
		}
		this.ransac(v);
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
	 * @param v vector with the points which represent all possible alignments.
	 */
	public void ransac(Vector<AlignStructMol> v) {
		double besterr = 9.9E99;
		for (int iterations = 0; iterations < this.k; iterations++) {
			this.AlsoNumber = this.n;
			boolean initN = this.getInitN(v);
			if (!initN) {
				continue;
			}

			if (isCurve) {
				this.getAllModelPointsCurve(v);
			} else {
				this.getAllModelPoints(v);
			}


			//System.out.println("also: " + this.AlsoNumber);
			if (this.AlsoNumber >= this.d) {
				double error = 10000;
				try {
					error = this.newError(v, null);
				} catch (Exception ex) {
					Logger.getLogger(RANSACDEVST.class.getName()).log(Level.SEVERE, null, ex);
				}

				if (error < besterr) {
					System.out.println(error);
					besterr = error;
					for (int i = 0; i < v.size(); i++) {
						AlignStructMol alignStruct = v.elementAt(i);
						if (alignStruct.ransacAlsoInLiers || alignStruct.ransacMaybeInLiers) {
							alignStruct.Aligned = true;
						} else {
							alignStruct.Aligned = false;
						}

						alignStruct.ransacAlsoInLiers = false;
						alignStruct.ransacMaybeInLiers = false;
					}
					this.deleteRepeatsAlignments(v);
					if (showChart) {
						chart.removeSeries();
						chart.addSeries(v, "na", true);
						chart.printAlignmentChart();
					}
				}
			}

			for (int i = 0; i < v.size(); i++) {
				AlignStructMol alignStruct = v.elementAt(i);
				alignStruct.ransacAlsoInLiers = false;
				alignStruct.ransacMaybeInLiers = false;
			}

		}
	}

	/**
	 * Take the initial 2 points ramdoly (but the points have to be separated)
	 * @param v vector with the points which represent all possible alignments.
	 * @return false if there are any problem.
	 */
	private boolean getInitN(Vector<AlignStructMol> v) {
		int quarter = (v.size() / 4) - 1;

		if (quarter > 8) {
			int index = rnd.nextInt(quarter);
			v.elementAt(index).ransacMaybeInLiers = true;

			index = rnd.nextInt(quarter);
			index += (quarter * 2);
			v.elementAt(index).ransacMaybeInLiers = true;

			index = rnd.nextInt(quarter);
			index += (quarter * 3);
			v.elementAt(index).ransacMaybeInLiers = true;
			return true;
		} else if (v.size() > 1) {
			for (int i = 0; i < this.n; i++) {
				int index = rnd.nextInt(v.size());
				if (v.elementAt(index).ransacMaybeInLiers) {
					i--;
				} else {
					v.elementAt(index).ransacMaybeInLiers = true;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Take the model
	 * @param v vector with the points which represent all possible alignments.
	 * @return regression of the points inside the model
	 */
	public void getAllModelPoints(Vector<AlignStructMol> v) {
		int indexRow1 = 0;
		int indexRow2 = 0;
		SimpleRegression regression = new SimpleRegression();

		for (int i = 0; i < v.size(); i++) {
			AlignStructMol point = v.elementAt(i);
			if (point.ransacMaybeInLiers) {
				regression.addData(point.row1.getPeaks()[indexRow1].getRT(), point.row2.getPeaks()[indexRow2].getRT());
			}
		}


		for (int i = 0; i < v.size(); i++) {
			AlignStructMol point = v.elementAt(i);
			double intercept = regression.getIntercept();
			double slope = regression.getSlope();

			double y = point.row2.getPeaks()[indexRow2].getRT();
			double bestY = intercept + (point.row1.getPeaks()[indexRow1].getRT() * slope);
			if (Math.abs(y - bestY) < margin) {
				point.ransacAlsoInLiers = true;
				this.AlsoNumber++;
			} else {
				point.ransacAlsoInLiers = false;
			}
		}

	}

	public void getAllModelPointsCurve(Vector<AlignStructMol> v) {
		int indexRow1 = 0;
		int indexRow2 = 0;
		Vector<double[]> threePoints = new Vector<double[]>();
		for (int i = 0; i < v.size(); i++) {
			AlignStructMol point = v.elementAt(i);
			if (point.ransacMaybeInLiers) {
				double[] pointCoord = new double[2];
				//indexRow1 = rnd.nextInt(point.row1.getPeaks().length);
				//	indexRow2 = rnd.nextInt(point.row2.getPeaks().length);
				pointCoord[0] = point.row1.getPeaks()[indexRow1].getRT();
				pointCoord[1] = point.row2.getPeaks()[indexRow2].getRT();
				threePoints.addElement(pointCoord);
			}
		}
		try {
			double[] curve = this.getCurveEquation(threePoints);
			for (AlignStructMol point : v) {
				double y = point.row2.getPeaks()[indexRow2].getRT();
				double bestY = curve[0] * Math.pow(point.row1.getPeaks()[indexRow1].getRT(), 2) + curve[1] * point.row1.getPeaks()[indexRow1].getRT() + curve[2];
				if (Math.abs(y - bestY) < margin) {
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
	 * @param v vector with the points which represent all possible alignments.
	 * @param regression regression of the alignment points
	 * @return the error in the model
	 */
	public double newError(Vector<AlignStructMol> v, SimpleRegression regression) throws Exception {

		double numT = 1;
		for (int i = 0; i < v.size(); i++) {
			if (v.elementAt(i).ransacAlsoInLiers || v.elementAt(i).ransacMaybeInLiers) {
				numT++;
			}
		}
		return 1 / numT;

	}

	/**
	 * If the same lipid is aligned with two differents lipids delete the alignment farest to the ransac regression line of all aligned points.
	 * @param v vector with the points which represent all possible alignments.
	 */
	private void deleteRepeatsAlignments(Vector<AlignStructMol> v) {
		for (AlignStructMol structMol1 : v) {
			if (structMol1.Aligned) {
				for (AlignStructMol structMol2 : v) {
					if (structMol1 != structMol2 && structMol2.Aligned) {
						if ((structMol1.row1 == structMol2.row1 || structMol1.row1 == structMol2.row2)|| (structMol1.row2 == structMol2.row1 || structMol1.row2 == structMol2.row2)) {
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
/* Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.savitzkygolayconnector;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.ConnectedMzPeak;
import net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.ConnectedPeak;

public class SavitzkyGolayPeak extends ConnectedPeak {

	//private Logger logger = Logger.getLogger(this.getClass().getName());

	private static final float[][] SGCoefficientsFirstDerivative = {
			{ 0.0f },
			{ 0.0f, 0.5f },
			{ 0.0f, 0.100f, 0.200f },
			{ 0.0f, 0.036f, 0.071f, 0.107f },
			{ 0.0f, 0.017f, 0.033f, 0.050f, 0.067f },
			{ 0.0f, 0.009f, 0.018f, 0.027f, 0.036f, 0.045f },
			{ 0.0f, 0.005f, 0.011f, 0.016f, 0.022f, 0.027f, 0.033f },
			{ 0.0f, 0.004f, 0.007f, 0.011f, 0.014f, 0.018f, 0.021f, 0.025f },
			{ 0.0f, 0.002f, 0.005f, 0.007f, 0.010f, 0.012f, 0.015f, 0.017f,
					0.020f },
			{ 0.0f, 0.002f, 0.004f, 0.005f, 0.007f, 0.009f, 0.011f, 0.012f,
					0.014f, 0.016f },
			{ 0.0f, 0.001f, 0.003f, 0.004f, 0.005f, 0.006f, 0.008f, 0.009f,
					0.010f, 0.012f, 0.013f },
			{ 0.0f, 0.001f, 0.002f, 0.003f, 0.007f, 0.005f, 0.006f, 0.007f,
					0.008f, 0.009f, 0.010f, 0.011f },
			{ 0.0f, 0.001f, 0.002f, 0.002f, 0.003f, 0.004f, 0.005f, 0.005f,
					0.006f, 0.007f, 0.008f, 0.008f, 0.009f } };

	private static final float[][] SGCoefficientsSecondDerivative = {
			{ 0.0f },
			{ -1.0f, 0.5f },
			{ -0.143f, -0.071f, 0.143f },
			{ -0.048f, -0.036f, 0.0f, 0.060f },
			{ -0.022f, -0.018f, -0.009f, 0.008f, 0.030f },
			{ -0.012f, -0.010f, -0.007f, -0.001f, 0.007f, 0.017f },
			{ -0.007f, -0.006f, -0.005f, -0.002f, 0.001f, 0.005f, 0.011f },
			{ -0.005f, -0.004f, -0.004f, -0.002f, -0.001f, 0.002f, 0.004f,
					0.007f },
			{ -0.003f, -0.003f, -0.003f, -0.002f, -0.001f, 0.000f, 0.002f,
					0.003f, 0.005f },
			{ -0.002f, -0.002f, -0.002f, -0.002f, -0.001f, 0.000f, 0.000f,
					0.001f, 0.003f, 0.004f },
			{ -0.002f, -0.002f, -0.001f, -0.001f, -0.001f, -0.001f, 0.000f,
					0.001f, 0.001f, 0.002f, 0.003f },
			{ -0.001f, -0.001f, -0.001f, -0.001f, -0.001f, -0.001f, 0.000f,
					0.000f, 0.001f, 0.001f, 0.002f, 0.002f },
			{ -0.001f, -0.001f, -0.001f, -0.001f, -0.001f, -0.001f, 0.000f,
					0.000f, 0.000f, 0.001f, 0.001f, 0.001f, 0.002f } };

	private float maxValueDerivative = 0.0f;


	public SavitzkyGolayPeak(RawDataFile dataFile, ConnectedMzPeak mzValue) {
		super(dataFile, mzValue);
	}

	public float[] getDerivative(boolean first) {
		ConnectedMzPeak[] mzValues = this.getConnectedMzPeaks();
		float[] derivative = new float[mzValues.length];
		int M = 0;

		
		for (int k = 0; k < mzValues.length; k++) {

			// Determine boundaries
			if (k < 13)
				M = k;
			if (k+M > mzValues.length - 1)
				M = mzValues.length - (k+1);
			
			// Perform derivative using Savitzky Golay coefficients
			for (int i = -M; i <= M; i++) {
				derivative[k] += mzValues[k+i].getMzPeak().getIntensity()
						* getSGCoefficient(first, M, i);
			}
			if (Math.abs(derivative[k]) > Math.abs(maxValueDerivative))
			  maxValueDerivative = derivative[k];

		}

		return derivative;
	}
	
	public float getDerivativeThreshold(){
		return Math.abs(maxValueDerivative) * 0.05f;
	}

	private Float getSGCoefficient(boolean first, int M, int signedC) {

		float coefficient;
		int C = Math.abs(signedC);
		if (first)
			coefficient = SGCoefficientsFirstDerivative[M][C];
		else
			coefficient = SGCoefficientsSecondDerivative[M][C];

		return coefficient;

	}
}

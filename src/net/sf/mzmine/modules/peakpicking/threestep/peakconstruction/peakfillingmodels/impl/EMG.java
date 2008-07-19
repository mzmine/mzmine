package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.peakfillingmodels.impl;

import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleMzPeak;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.ConnectedPeak;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.peakfillingmodels.PeakFillingModel;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ConnectedMzPeak;

public class EMG implements PeakFillingModel {

	private float xRight = -1, xLeft = -1;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public ChromatographicPeak fillingPeak(
			ChromatographicPeak originalDetectedShape) {

		float C = 0.2f; //Fixed level of excess;
		float heightMax = originalDetectedShape.getHeight() * (1.0f - (C / 10));
		float RT = originalDetectedShape.getRT();
		ConnectedMzPeak[] listMzPeaks = ((ConnectedPeak) originalDetectedShape)
				.getAllMzPeaks();
		float FWHM = calculateWidth(listMzPeaks, heightMax, RT) * (1.0f - (C));

		if (FWHM < 0) {
			return originalDetectedShape;
		}

		/*
		 * Calculates asymmetry of the peak using the formula
		 * 
		 * Ap = FWHM / ([1.76 (b/a)^2] - [11.15 (b/a)] + 28)
		 * 
		 * This formula is a variation of Foley J.P., Dorsey J.G. "Equations for
		 * // calculation of chormatographic figures of Merit for Ideal and
		 * Skewed Peaks", Anal. Chem. 1983, 55, 730-737.
		 */

		float a = (RT - xLeft);
		float b = (xRight - RT);
		float paramA = b / a;
		float paramB = (1.76f * (float) Math.pow(paramA, 2))
				- (11.15f * paramA) + 28.0f;
		float Ap = (FWHM) / paramB;
		if (b > a)
			Ap *= -1;

		float t, shapeHeight;

		t = listMzPeaks[0].getScan().getRetentionTime();
		shapeHeight = calculateEMGIntensity(heightMax, RT, FWHM, Ap, C, t);

		ConnectedMzPeak newMzPeak = listMzPeaks[0].clone();
		((SimpleMzPeak) newMzPeak.getMzPeak()).setIntensity(shapeHeight);

		ConnectedPeak filledPeak = new ConnectedPeak(originalDetectedShape
				.getDataFile(), newMzPeak);

		for (int i = 1; i < listMzPeaks.length; i++) {

			t = listMzPeaks[i].getScan().getRetentionTime();
			shapeHeight = calculateEMGIntensity(heightMax, RT, FWHM, Ap, C, t);

			newMzPeak = listMzPeaks[i].clone();
			((SimpleMzPeak) newMzPeak.getMzPeak()).setIntensity(shapeHeight);

			filledPeak.addMzPeak(newMzPeak);
		}

		return filledPeak;
	}

	/**
	 * This method calculates the width of the chromatographic peak at half
	 * intensity
	 * 
	 * @param SimpleMzPeak
	 * @return float
	 */
	private float calculateWidth(ConnectedMzPeak[] listMzPeaks, float height,
			float RT) {

		boolean leftside = false;
		float halfIntensity = height / 2;
		ConnectedMzPeak[] rangeDataPoints = listMzPeaks.clone();

		for (int i = 0; i < rangeDataPoints.length - 1; i++) {

			if (rangeDataPoints[i].getMzPeak().getIntensity() >= height)
				continue;

			// Left side of the curve
			if (!leftside) {
				if ((rangeDataPoints[i].getMzPeak().getIntensity() <= halfIntensity)
						&& (rangeDataPoints[i].getScan().getRetentionTime() < RT)
						&& (rangeDataPoints[i + 1].getMzPeak().getIntensity() >= halfIntensity)) {

					// First point with intensity just less than half of total
					// intensity
					float leftY1 = rangeDataPoints[i].getMzPeak()
							.getIntensity();
					float leftX1 = rangeDataPoints[i].getScan()
							.getRetentionTime();

					// Second point with intensity just bigger than half of
					// total
					// intensity
					float leftY2 = rangeDataPoints[i + 1].getMzPeak()
							.getIntensity();
					float leftX2 = rangeDataPoints[i + 1].getScan()
							.getRetentionTime();

					// We calculate the slope with formula m = Y1 - Y2 / X1 - X2
					float mLeft = (leftY1 - leftY2) / (leftX1 - leftX2);

					// We calculate the desired point (at half intensity) with
					// the
					// linear equation
					// X = X1 + [(Y - Y1) / m ], where Y = half of total
					// intensity
					xLeft = leftX1 + (((halfIntensity) - leftY1) / mLeft);
					leftside = true;
					continue;
				}
			}

			// Right side of the curve
			if (leftside) {
				if ((rangeDataPoints[i].getMzPeak().getIntensity() >= halfIntensity)
						&& (rangeDataPoints[i].getScan().getRetentionTime() > RT)
						&& (rangeDataPoints[i + 1].getMzPeak().getIntensity() <= halfIntensity)) {

					// First point with intensity just bigger than half of total
					// intensity
					float rightY1 = rangeDataPoints[i].getMzPeak()
							.getIntensity();
					float rightX1 = rangeDataPoints[i].getScan()
							.getRetentionTime();

					// Second point with intensity just less than half of total
					// intensity
					float rightY2 = rangeDataPoints[i + 1].getMzPeak()
							.getIntensity();
					float rightX2 = rangeDataPoints[i + 1].getScan()
							.getRetentionTime();

					// We calculate the slope with formula m = Y1 - Y2 / X1 - X2
					float mRight = (rightY1 - rightY2) / (rightX1 - rightX2);

					// We calculate the desired point (at half intensity) with
					// the
					// linear equation
					// X = X1 + [(Y - Y1) / m ], where Y = half of total
					// intensity
					xRight = rightX1 + (((halfIntensity) - rightY1) / mRight);
					break;
				}
			}
		}

		boolean negative = (((xRight - xLeft) / 2) < 0);
		
		// We verify the values to confirm we find the desired points. If not we
		// return the same mass value.
		if ((negative) || (xRight == -1) || (xLeft == -1)){
			float beginning = rangeDataPoints[0].getScan().getRetentionTime();
			float ending = rangeDataPoints[rangeDataPoints.length-1].getScan().getRetentionTime();
			xRight = RT + (ending-beginning)/4.71f;
			xLeft = RT - (ending-beginning)/4.71f;
		}

		// The center of left and right points is the exact mass of our peak.
		float FWHM = (xRight - xLeft) / 2.355f;

		return FWHM;
	}

	/**
	 * 
	 * This method calculates the height of Exponential Modified Gaussian
	 * function, using the mathematical model proposed by Zs. Pápai and T. L.
	 * Pap "Determination of chromatographic peak parameters by non-linear curve
	 * fitting using statistical moments", The Analyst,
	 * http://www.rsc.org/publishing/journals/AN/article.asp?doi=b111304f
	 * 
	 * @param heightMax
	 * @param RT
	 * @param Dp
	 * @param Ap
	 * @param C
	 * @param t
	 * @return
	 */
	private float calculateEMGIntensity(float heightMax, float RT, float Dp,
			float Ap, float C, float t) {
		float shapeHeight;

		double partA1 = Math.pow((t - RT), 2) / (2 * Math.pow(Dp, 2));
		double partA = heightMax * Math.exp(-1 * partA1);

		double partB1 = (t - RT) / Dp;
		double partB2 = (Ap / 6.0f) * (Math.pow(partB1, 2) - (3 * partB1));
		double partB3 = (C / 24)
				* (Math.pow(partB1, 4) - (6 * Math.pow(partB1, 2)) + 3);
		double partB = 1 + partB2 + partB3;

		shapeHeight = (float) (partA * partB);

		if (shapeHeight < 0)
			shapeHeight = 0;

		return shapeHeight;
	}

}

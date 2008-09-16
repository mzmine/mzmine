package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.peakfillingmodels.impl;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleMzPeak;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.ConnectedPeak;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.peakfillingmodels.PeakFillingModel;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ConnectedMzPeak;
import net.sf.mzmine.util.Range;

public class GaussianPeakFillingModel implements PeakFillingModel {

	private float xRight = -1, xLeft = -1;
	private float rtMain, intensityMain, FWHM, partC, part2C2;

	public ChromatographicPeak fillingPeak(
			ChromatographicPeak originalDetectedShape, float[] params) {

		xRight = -1;
		xLeft = -1;

		ConnectedMzPeak[] listMzPeaks = ((ConnectedPeak) originalDetectedShape)
				.getAllMzPeaks();

		RawDataFile dataFile = originalDetectedShape.getDataFile();
		Range originalRange = originalDetectedShape.getRawDataPointsRTRange();
		float rangeSize = originalRange.getSize();
		float mass = originalDetectedShape.getMZ();
		Range extendedRange = new Range(originalRange.getMin() - rangeSize,
				originalRange.getMax() + rangeSize);
		int[] listScans = dataFile.getScanNumbers(1, extendedRange);
		
		float C, factor;

		C = params[0]; // level of excess;
		factor = (1.0f - (Math.abs(C) / 10.0f));

		intensityMain = originalDetectedShape.getHeight() * factor;
		rtMain = originalDetectedShape.getRT();
		// FWFM (Full Width at Half Maximum)

		FWHM = calculateWidth(listMzPeaks) * factor;

		if (FWHM < 0) {
			return originalDetectedShape;
		}

		partC = FWHM / 2.354820045f;
		part2C2 = 2f * (float) Math.pow(partC, 2);

		// Calculate intensity of each point in the shape.
		float t, shapeHeight;
		Scan scan;
		float tempIntensity= Float.MIN_VALUE;
		int indexListMzPeaks = 0;
		for (int i = 1; i < listMzPeaks.length; i++) {
			if (listMzPeaks[i].getMzPeak().getIntensity() > tempIntensity){
				tempIntensity = listMzPeaks[i].getMzPeak().getIntensity();
				indexListMzPeaks = i;
			}
		}
		
		ConnectedMzPeak newMzPeak = listMzPeaks[indexListMzPeaks].clone();
				
		t = newMzPeak.getScan().getRetentionTime();
		shapeHeight = getIntensity(t);

		((SimpleMzPeak) newMzPeak.getMzPeak()).setIntensity(shapeHeight);

		ConnectedPeak filledPeak = new ConnectedPeak(originalDetectedShape
				.getDataFile(), newMzPeak);

		for (int scanIndex : listScans) {

			scan = dataFile.getScan(scanIndex);
			t = scan.getRetentionTime();
			shapeHeight = getIntensity(t);
			
			// Level of significance
			if (shapeHeight < (intensityMain * 0.001))
				continue;

			newMzPeak = getDetectedMzPeak(listMzPeaks, scan);
			
			if (newMzPeak != null) {
				((SimpleMzPeak) newMzPeak.getMzPeak())
						.setIntensity(shapeHeight);
			} else {
				newMzPeak = new ConnectedMzPeak(scan, new SimpleMzPeak(
						new SimpleDataPoint(mass, shapeHeight)));
			}
			
			filledPeak.addMzPeak(newMzPeak);
		}

		return filledPeak;
	}

	/**
	 * This method calculates the width of the chromatographic peak at half
	 * intensity
	 * 
	 * @param listMzPeaks
	 * @param height
	 * @param RT
	 * @return FWHM
	 */
	private float calculateWidth(ConnectedMzPeak[] listMzPeaks) {

		float halfIntensity = intensityMain / 2, intensity = 0, intensityPlus = 0, retentionTime = 0;
		ConnectedMzPeak[] rangeDataPoints = listMzPeaks; // .clone();

		for (int i = 0; i < rangeDataPoints.length - 1; i++) {

			intensity = rangeDataPoints[i].getMzPeak().getIntensity();
			intensityPlus = rangeDataPoints[i + 1].getMzPeak().getIntensity();
			retentionTime = rangeDataPoints[i].getScan().getRetentionTime();

			if (intensity > intensityMain)
				continue;

			// Left side of the curve
			if (retentionTime < rtMain) {
				if ((intensity <= halfIntensity)
						&& (intensityPlus >= halfIntensity)) {

					// First point with intensity just less than half of total
					// intensity
					float leftY1 = intensity;
					float leftX1 = retentionTime;

					// Second point with intensity just bigger than half of
					// total
					// intensity
					float leftY2 = intensityPlus;
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
					continue;
				}
			}

			// Right side of the curve
			if (retentionTime > rtMain) {
				if ((intensity >= halfIntensity)
						&& (intensityPlus <= halfIntensity)) {

					// First point with intensity just bigger than half of total
					// intensity
					float rightY1 = intensity;
					float rightX1 = retentionTime;

					// Second point with intensity just less than half of total
					// intensity
					float rightY2 = intensityPlus;
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

		if ((xRight <= -1) && (xLeft > 0)) {
			float beginning = rangeDataPoints[0].getScan().getRetentionTime();
			float ending = rangeDataPoints[rangeDataPoints.length - 1]
					.getScan().getRetentionTime();
			xRight = rtMain + (ending - beginning) / 4.71f;
		}

		if ((xRight > 0) && (xLeft <= -1)) {
			float beginning = rangeDataPoints[0].getScan().getRetentionTime();
			float ending = rangeDataPoints[rangeDataPoints.length - 1]
					.getScan().getRetentionTime();
			xLeft = rtMain - (ending - beginning) / 4.71f;
		}

		boolean negative = (((xRight - xLeft)) < 0);

		if ((negative) || ((xRight == -1) && (xLeft == -1))) {
			float beginning = rangeDataPoints[0].getScan().getRetentionTime();
			float ending = rangeDataPoints[rangeDataPoints.length - 1]
					.getScan().getRetentionTime();
			xRight = rtMain + (ending - beginning) / 9.42f;
			xLeft = rtMain - (ending - beginning) / 9.42f;
		}

		 
		float FWHM = (xRight - xLeft);

		return FWHM;
	}

	public float getIntensity(float rt) {

		// Using the Gaussian function we calculate the intensity at given m/z
		float diff2 = (float) Math.pow(rt - rtMain, 2);
		float exponent = -1 * (diff2 / part2C2);
		float eX = (float) Math.exp(exponent);
		float intensity = intensityMain * eX;
		return intensity;
	}
	
	public ConnectedMzPeak getDetectedMzPeak(ConnectedMzPeak[] listMzPeaks, Scan scan){
		int scanNumber = scan.getScanNumber();
		for (int i=0; i<listMzPeaks.length; i++){
			if (listMzPeaks[i].getScan().getScanNumber() == scanNumber)
				return listMzPeaks[i].clone();
		}
		return null;
	}

}

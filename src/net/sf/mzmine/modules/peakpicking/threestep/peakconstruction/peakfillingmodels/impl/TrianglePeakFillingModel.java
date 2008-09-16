package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.peakfillingmodels.impl;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.impl.SimpleMzPeak;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.ConnectedPeak;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.peakfillingmodels.PeakFillingModel;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ConnectedMzPeak;

public class TrianglePeakFillingModel implements PeakFillingModel {

	private float rtRight = -1, rtLeft = -1;
	private float rtMain, intensityMain, alpha, beta;

	public ChromatographicPeak fillingPeak(
			ChromatographicPeak originalDetectedShape, float[] params) {

		ConnectedMzPeak[] listMzPeaks = ((ConnectedPeak) originalDetectedShape)
				.getAllMzPeaks();

		float C, factor;

		C = params[0]; // level of excess;
		factor = (1.0f - (Math.abs(C) / 10.0f));

		intensityMain = originalDetectedShape.getHeight() * factor;
		rtMain = originalDetectedShape.getRT();

		rtRight = -1;
		rtLeft = -1;
		calculateBase(listMzPeaks);

		alpha = (float) Math.atan(intensityMain / (rtMain - rtLeft));
		beta = (float) Math.atan(intensityMain / (rtRight - rtMain));

		// Calculate intensity of each point in the shape.
		float t, shapeHeight;
		ConnectedMzPeak newMzPeak = listMzPeaks[0].clone();
		t = listMzPeaks[0].getScan().getRetentionTime();
		shapeHeight = getIntensity(t);
		((SimpleMzPeak) newMzPeak.getMzPeak()).setIntensity(shapeHeight);
		ConnectedPeak filledPeak = new ConnectedPeak(originalDetectedShape
				.getDataFile(), newMzPeak);

		for (int i = 1; i < listMzPeaks.length; i++) {

			t = listMzPeaks[i].getScan().getRetentionTime();
			shapeHeight = getIntensity(t);

			newMzPeak = listMzPeaks[i].clone();
			((SimpleMzPeak) newMzPeak.getMzPeak()).setIntensity(shapeHeight);
			filledPeak.addMzPeak(newMzPeak);
		}

		return filledPeak;
	}

	public float getIntensity(float rt) {

		float intensity = 0;
		if ((rt > rtLeft) && (rt < rtRight)) {
			if (rt <= rtMain) {
				intensity = (float) Math.tan(alpha) * (rt - rtLeft);
			}
			if (rt > rtMain) {
				intensity = (float) Math.tan(beta) * (rtRight - rt);
			}
		}

		return intensity;
	}

	private void calculateBase(ConnectedMzPeak[] listMzPeaks) {

		float halfIntensity = intensityMain / 2, intensity = 0, intensityPlus = 0, retentionTime = 0;
		ConnectedMzPeak[] rangeDataPoints = listMzPeaks;

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
					rtLeft = retentionTime;
					continue;
				}
			}

			// Right side of the curve
			if (retentionTime > rtMain) {
				if ((intensity >= halfIntensity)
						&& (intensityPlus <= halfIntensity)) {

					rtRight = retentionTime;
					break;
				}
			}
		}

		if ((rtRight <= -1) && (rtLeft > 0)) {
			float beginning = rangeDataPoints[0].getScan().getRetentionTime();
			float ending = rangeDataPoints[rangeDataPoints.length - 1]
					.getScan().getRetentionTime();
			rtRight = rtMain + ((ending - beginning) / 4.71f);
		}

		if ((rtRight > 0) && (rtLeft <= -1)) {
			float beginning = rangeDataPoints[0].getScan().getRetentionTime();
			float ending = rangeDataPoints[rangeDataPoints.length - 1]
					.getScan().getRetentionTime();
			rtLeft = rtMain - ((ending - beginning) / 4.71f);
		}

		boolean negative = (((rtRight - rtLeft)) < 0);

		if ((negative) || ((rtRight == -1) && (rtLeft == -1))) {
			float beginning = rangeDataPoints[0].getScan().getRetentionTime();
			float ending = rangeDataPoints[rangeDataPoints.length - 1]
					.getScan().getRetentionTime();
			rtRight = rtMain + ((ending - beginning) / 9.42f);
			rtLeft = rtMain - ((ending - beginning) / 9.42f);
		}
	}

}

package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.savitzkygolay;

public final class SGDerivative {
	
	/**
	 * This method returns the second smoothed derivative values of an array.
	 * 
	 * @param chromatoIntensities
	 * @return
	 */
	public static float[] calculateDerivative(float[] chromatoIntensities, boolean firstDerivative, int levelOfFilter) {

		float[] derivative = new float[chromatoIntensities.length];
		int M = 0;

		for (int k = 0; k < derivative.length; k++) {

			// Determine boundaries
			if (k <= levelOfFilter)
				M = k;
			if (k + M > derivative.length - 1)
				M = derivative.length - (k + 1);

			// Perform derivative using Savitzky Golay coefficients
			for (int i = -M; i <= M; i++) {
				derivative[k] += chromatoIntensities[k + i]
						* getSGCoefficient(M, i, firstDerivative);
			}
			//if ((Math.abs(derivative[k])) > maxValueDerivative)
				//maxValueDerivative = Math.abs(derivative[k]);

		}

		return derivative;
	}

	/**
	 * This method return the Savitzky-Golay 2nd smoothed derivative coefficient
	 * from an array
	 * 
	 * @param M
	 * @param signedC
	 * @return
	 */
	private static Float getSGCoefficient(int M, int signedC, boolean firstDerivate) {

		int C = Math.abs(signedC), sign = 1;
		if (firstDerivate){
		if (signedC < 0)
			sign = -1;
		return sign * SGCoefficients.SGCoefficientsFirstDerivativeQuartic[M][C];
		}
		else{
			return SGCoefficients.SGCoefficientsSecondDerivative[M][C];
		}

	}


}

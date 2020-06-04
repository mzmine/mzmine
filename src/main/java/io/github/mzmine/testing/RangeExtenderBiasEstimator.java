package io.github.mzmine.testing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;

public class RangeExtenderBiasEstimator extends MassMeasurementBiasEstimator
{
	protected double stretchGapToMeanRatio;
	protected int mostErrorsStart;
	protected int mostErrorsEnd;
	protected double mostErrorsStartValue;
	protected double mostErrorsEndValue;

	public RangeExtenderBiasEstimator(ArrayList<Double> errors, int mostErrorsStart, int mostErrorsEnd, double stretchGapToMeanRatio)
	{
		super(errors);
		this.mostErrorsStart = mostErrorsStart;
		this.mostErrorsEnd = mostErrorsEnd;
		this.stretchGapToMeanRatio = stretchGapToMeanRatio;
	}

	public Double getBiasEstimate()
	{
		// Collections.sort(errors);
		double meanGapInRange = (errors.get(mostErrorsEnd) - errors.get(mostErrorsStart))
								/ (mostErrorsEnd - mostErrorsStart);

		// double tolerance = meanGapInRange * stretchGapToMeanRatio;
		double tolerance = 0.4;
		while((mostErrorsStart > 0 && errors.get(mostErrorsStart) - errors.get(mostErrorsStart - 1) < tolerance)
			|| (mostErrorsEnd < errors.size()-1 && errors.get(mostErrorsEnd + 1) - errors.get(mostErrorsEnd) < tolerance))
		{
			if(mostErrorsStart > 0 && errors.get(mostErrorsStart) - errors.get(mostErrorsStart - 1) < tolerance)
			{
				mostErrorsStart--;
			}
			if(mostErrorsEnd < errors.size()-1 && errors.get(mostErrorsEnd + 1) - errors.get(mostErrorsEnd) < tolerance)
			{
				mostErrorsEnd++;
			}

			meanGapInRange = (errors.get(mostErrorsEnd) - errors.get(mostErrorsStart))
								/ (mostErrorsEnd - mostErrorsStart);

			// tolerance = meanGapInRange * stretchGapToMeanRatio;
		}

		this.mostErrorsStart = mostErrorsStart;
		this.mostErrorsEnd = mostErrorsEnd;
		this.mostErrorsStartValue = errors.get(mostErrorsStart);
		this.mostErrorsEndValue = errors.get(mostErrorsEnd);

		System.out.printf("Found stretched range with %d/%d errors%n", mostErrorsEnd - mostErrorsStart + 1, errors.size());
		System.out.printf("Smallest value %f, biggest value %f, range length %f%n", 
						   errors.get(mostErrorsStart), errors.get(mostErrorsEnd), errors.get(mostErrorsEnd) - errors.get(mostErrorsStart));
		System.out.println("Mean gap length: " + meanGapInRange);

		ArithmeticMeanBiasEstimator meanEstimator = new ArithmeticMeanBiasEstimator(
			new ArrayList<Double>(errors.subList(mostErrorsStart, mostErrorsEnd+1)));
		double estimate = meanEstimator.getBiasEstimate();
		System.out.println("Arithmetic mean of errors found in the choosen range " + estimate);
		return estimate;

	}

	public int getMostErrorsStart()
	{
		return mostErrorsStart;
	}

	public int getMostErrorsEnd()
	{
		return mostErrorsEnd;
	}

	public double getMostErrorsStartValue()
	{
		return mostErrorsStartValue;
	}

	public double getMostErrorsEndValue()
	{
		return mostErrorsEndValue;
	}

}
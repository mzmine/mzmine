package io.github.mzmine.testing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;

class RangeExtenderBiasEstimator extends MassMeasurementBiasEstimator
{
	protected double stretchGapToMeanRatio;
	// protected double mostErrorsStart;
	// protected double mostErrorsEnd;
	protected int mostErrorsStart;
	protected int mostErrorsEnd;
	protected double mostErrorsStartValue;
	protected double mostErrorsEndValue;

	public RangeExtenderBiasEstimator(ArrayList<Double> errors, double stretchGapToMeanRatio,
									  // double mostErrorsStart, double mostErrorsEnd)
									  int mostErrorsStart, int mostErrorsEnd)
	{
		super(errors);
		this.stretchGapToMeanRatio = stretchGapToMeanRatio;
		this.mostErrorsStart = mostErrorsStart;
		this.mostErrorsEnd = mostErrorsEnd;
	}

	public Double getBiasEstimate()
	{
		// Collections.sort(errors);
		double sumInRange = 0;
		for(int i = mostErrorsStart; i <= mostErrorsEnd; i++)
		{
			sumInRange += errors.get(i);
		}

		double tolerance = (sumInRange / (mostErrorsEnd - mostErrorsStart + 1)) * stretchGapToMeanRatio;
		while((mostErrorsStart > 0 && errors.get(mostErrorsStart) - errors.get(mostErrorsStart - 1) < tolerance)
			|| (mostErrorsEnd < errors.size()-1 && errors.get(mostErrorsEnd + 1) - errors.get(mostErrorsEnd) < tolerance))
		{
			if(mostErrorsStart > 0 && errors.get(mostErrorsStart) - errors.get(mostErrorsStart - 1) < tolerance)
			{
				mostErrorsStart--;
				sumInRange += errors.get(mostErrorsStart);
			}
			if(mostErrorsEnd < errors.size()-1 && errors.get(mostErrorsEnd + 1) - errors.get(mostErrorsEnd) < tolerance)
			{
				mostErrorsEnd++;
				sumInRange += errors.get(mostErrorsEnd);
			}

			// double tolerance = (sumInRange / (mostErrorsEnd - mostErrorsStart + 1)) * stretchGapToMeanRatio;
			tolerance = (sumInRange / (mostErrorsEnd - mostErrorsStart + 1)) * stretchGapToMeanRatio;
		}

		this.mostErrorsStart = mostErrorsStart;
		this.mostErrorsEnd = mostErrorsEnd;
		this.mostErrorsStartValue = errors.get(mostErrorsStart);
		this.mostErrorsEndValue = errors.get(mostErrorsEnd);

		System.out.printf("Found stretched range with %d/%d errors%n", mostErrorsEnd - mostErrorsStart + 1, errors.size());
		System.out.printf("Smallest value %f, biggest value %f, range length %f%n", 
						   errors.get(mostErrorsStart), errors.get(mostErrorsEnd), errors.get(mostErrorsEnd) - errors.get(mostErrorsStart));

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
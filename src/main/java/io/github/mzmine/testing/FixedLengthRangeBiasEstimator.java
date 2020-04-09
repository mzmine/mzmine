package io.github.mzmine.testing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;

class FixedLengthRangeBiasEstimator extends MassMeasurementBiasEstimator
{
	protected double maxRangeLength;
	protected int mostErrorsStart;
	protected int mostErrorsEnd;
	protected double mostErrorsStartValue;
	protected double mostErrorsEndValue;

	public FixedLengthRangeBiasEstimator(ArrayList<Double> errors, double maxRangeLength)
	{
		super(errors);
		this.maxRangeLength = maxRangeLength;
	}

	public Double getBiasEstimate()
	{
		Collections.sort(errors);
		int endIndex = 0;
		int mostErrorsStart = 0;
		int mostErrorsEnd = 0;
		for(int startIndex = 0; startIndex < errors.size(); startIndex++)
		{
			while(endIndex + 1 < errors.size() && errors.get(endIndex+1) - errors.get(startIndex) <= maxRangeLength)
			{
				endIndex++;
			}
			if(endIndex - startIndex > mostErrorsEnd - mostErrorsStart)
			{
				mostErrorsStart = startIndex;
				mostErrorsEnd = endIndex;
			}
		}

		this.mostErrorsStart = mostErrorsStart;
		this.mostErrorsEnd = mostErrorsEnd;
		this.mostErrorsStartValue = errors.get(mostErrorsStart);
		this.mostErrorsEndValue = errors.get(mostErrorsEnd);

		System.out.println("Finding range with most errors and max length of " + maxRangeLength);
		System.out.printf("Found range with %d/%d errors%n", mostErrorsEnd - mostErrorsStart + 1, errors.size());
		System.out.printf("Smallest value %f, biggest value %f, range length %f%n", 
						   errors.get(mostErrorsStart), errors.get(mostErrorsEnd), errors.get(mostErrorsEnd) - errors.get(mostErrorsStart));
		double meanGapInRange = (errors.get(mostErrorsEnd) - errors.get(mostErrorsStart))
								/ (mostErrorsEnd - mostErrorsStart);
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
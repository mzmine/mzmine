package io.github.mzmine.testing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;

class FixedLengthRangeBiasEstimator extends MassMeasurementBiasEstimator
{
	protected double maxRangeLength;

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

		System.out.println("Finding range with most errors and max length of " + maxRangeLength);
		System.out.printf("Found range with %d/%d errors%n", mostErrorsEnd - mostErrorsStart + 1, errors.size());
		System.out.printf("Smallest value %f, biggest value %f, range length %f%n", 
						   errors.get(mostErrorsStart), errors.get(mostErrorsEnd), errors.get(mostErrorsEnd) - errors.get(mostErrorsStart));


		HashMap<String, Double> lines = new HashMap<String, Double>();
		lines.put("Range smallest value", errors.get(mostErrorsStart));
		lines.put("Range biggest value", errors.get(mostErrorsEnd));

		DistributionPlot.main("ppm errors distribution", errors, lines);

		ArithmeticMeanBiasEstimator meanEstimator = new ArithmeticMeanBiasEstimator(
			new ArrayList<Double>(errors.subList(mostErrorsStart, mostErrorsEnd+1)));
		double estimate = meanEstimator.getBiasEstimate();
		System.out.println("Arithmetic mean of errors found in the choosen range " + estimate);
		return estimate;

	}
}
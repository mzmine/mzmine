package io.github.mzmine.testing;

import java.util.ArrayList;
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
		// int start_index = 0;
		int end_index = 0;
		// int mostErrors = 0;
		int mostErrorsStart = 0;
		int mostErrorsEnd = 0;
		// for(int i = 0; i < error.size(); i++)
		for(int start_index = 0; start_index < errors.size(); start_index++)
		{
			// while(end_index + 1 < errors.size() && errors[end_index+1] - errors[start_index] <= maxRangeLength)
			while(end_index + 1 < errors.size() && errors.get(end_index+1) - errors.get(start_index) <= maxRangeLength)
			{
				end_index++;
			}
			if(end_index - start_index > mostErrorsEnd - mostErrorsStart)
			{
				mostErrorsStart = start_index;
				mostErrorsEnd = end_index;
			}
		}

		System.out.println("Finding range with most errors and max length of " + maxRangeLength);
		System.out.printf("Found range with %d/%d errors%n", mostErrorsEnd - mostErrorsStart + 1, errors.size());
		System.out.printf("Smallest value %f, biggest value %f, range length %f%n", 
						   errors.get(mostErrorsStart), errors.get(mostErrorsEnd), errors.get(mostErrorsEnd) - errors.get(mostErrorsStart));


		ArrayList<Double> lines = new ArrayList<Double>();
		lines.add(errors.get(mostErrorsStart));
		lines.add(errors.get(mostErrorsEnd));

		// DistributionPlot plot = new DistributionPlot();
		DistributionPlot.main("ppm errors distribution", errors, lines);

		// ArithmeticMeanBiasEstimator meanEstimator = new ArithmeticMeanBiasEstimator((ArrayList<Double>)errors.subList(mostErrorsStart, mostErrorsEnd+1));
		ArithmeticMeanBiasEstimator meanEstimator = new ArithmeticMeanBiasEstimator(
			new ArrayList<Double>(errors.subList(mostErrorsStart, mostErrorsEnd+1)));
		double estimate = meanEstimator.getBiasEstimate();
		System.out.println("Arithmetic mean of errors found in the choosen range " + estimate);
		return estimate;

	}
}
package io.github.mzmine.modules.dataprocessing.masscalibration.errormodelling;

import java.util.ArrayList;

class ArithmeticMeanBiasEstimator extends MassMeasurementBiasEstimator
{
	public ArithmeticMeanBiasEstimator(ArrayList<Double> errors)
	{
		super(errors);
	}

	public Double getBiasEstimate()
	{
		double sum = errors.stream().mapToDouble(Double::doubleValue).sum();
		return sum / errors.size();
	}
}
package io.github.mzmine.testing;

import java.util.ArrayList;

abstract class MassMeasurementBiasEstimator
{
	protected ArrayList<Double> errors;

	public MassMeasurementBiasEstimator(ArrayList<Double> errors)
	{
		this.errors = errors;
	}

	abstract public Double getBiasEstimate();
}
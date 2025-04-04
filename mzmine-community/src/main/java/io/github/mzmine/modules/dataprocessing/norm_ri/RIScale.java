package io.github.mzmine.modules.dataprocessing.norm_ri;

import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.time.LocalDate;

record RIScale(LocalDate date, String fileName, PolynomialSplineFunction interpolator) {
}
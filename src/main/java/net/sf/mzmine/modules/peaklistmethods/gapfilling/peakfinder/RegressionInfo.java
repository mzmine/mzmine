/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.mzmine.modules.peaklistmethods.alignment.ransac.RTs;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.fitting.PolynomialFitter;
import org.apache.commons.math.optimization.general.GaussNewtonOptimizer;

public class RegressionInfo {

    private List<RTs> data;
    private PolynomialFunction function;

    public RegressionInfo() {
	this.data = new ArrayList<RTs>();

    }

    public void setFunction() {
	function = getPolynomialFunction();
    }

    public double predict(double RT) {
	try {
	    return function.value(RT);
	} catch (Exception ex) {
	    return -1;
	}
    }

    public void addData(double RT, double RT2) {
	this.data.add(new RTs(RT, RT2));
    }

    private PolynomialFunction getPolynomialFunction() {
	Collections.sort(data, new RTs());
	PolynomialFitter fitter = new PolynomialFitter(3,
		new GaussNewtonOptimizer(true));
	for (RTs rt : data) {
	    fitter.addObservedPoint(1, rt.RT, rt.RT2);
	}
	try {
	    return fitter.fit();

	} catch (Exception ex) {
	    return null;
	}
    }

}
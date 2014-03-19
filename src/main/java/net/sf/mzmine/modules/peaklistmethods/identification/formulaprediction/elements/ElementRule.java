/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IIsotope;

public class ElementRule {

    private String elementSymbol;
    private IIsotope elementObject;
    private double elementMass;
    private int minCount, maxCount;

    public ElementRule(String elementSymbol, int min, int max) {
	initRule(elementSymbol, min, max);
    }

    public ElementRule(String stringRepresentation) {

	Pattern p = Pattern.compile("([a-zA-Z]+)\\[([0-9]+)-([0-9]+)\\]");
	Matcher m = p.matcher(stringRepresentation);
	if (!m.matches()) {
	    throw new IllegalArgumentException("Invalid element rule format: "
		    + stringRepresentation);
	}

	elementSymbol = m.group(1);
	minCount = Integer.parseInt(m.group(2));
	maxCount = Integer.parseInt(m.group(3));

	if (minCount < 0)
	    minCount = 0;
	if (maxCount < 0)
	    maxCount = 0;

	initRule(elementSymbol, minCount, maxCount);

    }

    private void initRule(String elementSymbol, int min, int max) {

	this.elementSymbol = elementSymbol;
	this.minCount = min;
	this.maxCount = max;

	try {
	    // Use CDK to obtain element's mass
	    Isotopes isotopeFactory = Isotopes.getInstance();
	    elementObject = isotopeFactory.getMajorIsotope(elementSymbol);
	    elementMass = elementObject.getExactMass();
	} catch (IOException e) {
	    // This can never happen
	    e.printStackTrace();
	}
    }

    public String getElementSymbol() {
	return elementSymbol;
    }

    public IIsotope getElementObject() {
	return elementObject;
    }

    public int getMinCount() {
	return minCount;
    }

    public void setMinCount(int min) {
	this.minCount = min;
    }

    public int getMaxCount() {
	return maxCount;
    }

    public void setMaxCount(int max) {
	this.maxCount = max;
    }

    public double getMass() {
	return elementMass;
    }

    public boolean equals(Object o) {
	if (!(o instanceof ElementRule))
	    return false;
	ElementRule otherRule = (ElementRule) o;
	return elementSymbol.equals(otherRule.elementSymbol);
    }

    public String toString() {
	return elementSymbol + "[" + minCount + "-" + maxCount + "]";
    }

}

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

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction;

import java.util.Map;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;

import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class ResultFormula {

    private final IMolecularFormula cdkFormula;
    private Double rdbeValue, isotopeScore, msmsScore;
    private IsotopePattern predictedIsotopePattern;
    private Map<DataPoint, String> msmsAnnotation;

    public ResultFormula(IMolecularFormula cdkFormula,
	    IsotopePattern predictedIsotopePattern, Double rdbeValue,
	    Double isotopeScore, Double msmsScore,
	    Map<DataPoint, String> msmsAnnotation) {

	this.cdkFormula = cdkFormula;
	this.predictedIsotopePattern = predictedIsotopePattern;
	this.isotopeScore = isotopeScore;
	this.msmsScore = msmsScore;
	this.msmsAnnotation = msmsAnnotation;
	this.rdbeValue = rdbeValue;

    }

    public Double getRDBE() {
	return rdbeValue;
    }

    public Map<DataPoint, String> getMSMSannotation() {
	return msmsAnnotation;
    }

    public String getFormulaAsString() {
	return MolecularFormulaManipulator.getString(cdkFormula);
    }

    public String getFormulaAsHTML() {
	return MolecularFormulaManipulator.getHTML(cdkFormula);
    }

    public IMolecularFormula getFormulaAsObject() {
	return cdkFormula;
    }

    public IsotopePattern getPredictedIsotopes() {
	return predictedIsotopePattern;
    }

    public Double getIsotopeScore() {
	return isotopeScore;
    }

    public Double getMSMSScore() {
	return msmsScore;
    }

    public double getExactMass() {
	return MolecularFormulaManipulator.getTotalExactMass(cdkFormula);
    }

}

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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements;

import java.io.IOException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.mzmine.parameters.UserParameter;

import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IIsotope;
import org.w3c.dom.Element;

/**
 * Parameter to setup element composition range represented by
 * MolecularFormulaRange (CDK class).
 */
public class ElementsParameter implements
	UserParameter<MolecularFormulaRange, ElementsTableComponent> {

    private String name, description;
    private MolecularFormulaRange value;

    public ElementsParameter(String name, String description) {
	this.name = name;
	this.description = description;
	this.value = new MolecularFormulaRange();
	try {
	    IsotopeFactory iFac = Isotopes.getInstance();
	    value.addIsotope(iFac.getMajorIsotope("C"), 0, 100);
	    value.addIsotope(iFac.getMajorIsotope("H"), 0, 100);
	    value.addIsotope(iFac.getMajorIsotope("N"), 0, 50);
	    value.addIsotope(iFac.getMajorIsotope("O"), 0, 50);
	    value.addIsotope(iFac.getMajorIsotope("P"), 0, 30);
	    value.addIsotope(iFac.getMajorIsotope("S"), 0, 30);
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getName()
     */
    @Override
    public String getName() {
	return name;
    }

    /**
     * @see net.sf.mzmine.data.Parameter#getDescription()
     */
    @Override
    public String getDescription() {
	return description;
    }

    @Override
    public ElementsTableComponent createEditingComponent() {
	ElementsTableComponent editor = new ElementsTableComponent();
	editor.setElements(value);
	return editor;
    }

    @Override
    public ElementsParameter cloneParameter() {
	ElementsParameter copy = new ElementsParameter(name, description);
	copy.value = value;
	return copy;
    }

    @Override
    public void setValueFromComponent(ElementsTableComponent component) {
	value = component.getElements();
    }

    @Override
    public void setValueToComponent(ElementsTableComponent component,
	    MolecularFormulaRange newValue) {
	component.setElements(newValue);
    }

    @Override
    public MolecularFormulaRange getValue() {
	return value;
    }

    @Override
    public void setValue(MolecularFormulaRange newValue) {
	this.value = newValue;
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {

	try {
	    MolecularFormulaRange newValue = new MolecularFormulaRange();
	    IsotopeFactory iFac = Isotopes.getInstance();

	    String s = xmlElement.getTextContent();
	    Pattern p = Pattern.compile("([a-zA-Z]+)\\[([0-9]+)-([0-9]+)\\]");
	    Matcher m = p.matcher(s);
	    while (m.find()) {
		String elementSymbol = m.group(1);
		int minCount = Integer.parseInt(m.group(2));
		int maxCount = Integer.parseInt(m.group(3));
		newValue.addIsotope(iFac.getMajorIsotope(elementSymbol),
			minCount, maxCount);
	    }
	    this.value = newValue;

	} catch (IOException e) {
	    e.printStackTrace();
	}

    }

    @Override
    public void saveValueToXML(Element xmlElement) {
	if (value == null)
	    return;
	StringBuilder s = new StringBuilder();
	for (IIsotope i : value.isotopes()) {
	    s.append(i.getSymbol());
	    s.append("[");
	    s.append(value.getIsotopeCountMin(i));
	    s.append("-");
	    s.append(value.getIsotopeCountMax(i));
	    s.append("]");
	}
	xmlElement.setTextContent(s.toString());
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
	if ((value == null) || (value.getIsotopeCount() == 0)) {
	    errorMessages.add("Please set the chemical elements");
	    return false;
	}
	return true;
    }

}

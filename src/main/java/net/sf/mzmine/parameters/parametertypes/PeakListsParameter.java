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

package net.sf.mzmine.parameters.parametertypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 
 */
public class PeakListsParameter implements
	UserParameter<String[], PeakListsComponent> {

    private int minCount, maxCount;
    private String values[];
    private int inputsize = 300;

    public PeakListsParameter() {
	this(1, Integer.MAX_VALUE);
    }

    public PeakListsParameter(int minCount) {
	this(minCount, Integer.MAX_VALUE);
    }

    public PeakListsParameter(int minCount, int maxCount) {
	this.minCount = minCount;
	this.maxCount = maxCount;
    }

    public PeakListsParameter(int minCount, int maxCount, int inputsize) {
	this.minCount = minCount;
	this.maxCount = maxCount;
	this.inputsize = inputsize;
    }

    @Override
    public String[] getValue() {
	return values;
    }

    public PeakList[] getMatchingPeakLists() {

	if ((values == null) || (values.length == 0))
	    return new PeakList[0];

	PeakList allPeakLists[] = MZmineCore.getCurrentProject().getPeakLists();
	ArrayList<PeakList> matchingPeakLists = new ArrayList<PeakList>();

	plCheck: for (PeakList pl : allPeakLists) {
	    for (String singleValue : values) {
		final String fileName = pl.getName();

		// Generate a regular expression, replacing * with .*
		try {
		    final StringBuilder regex = new StringBuilder("^");
		    String sections[] = singleValue.split("\\*", -1);
		    for (int i = 0; i < sections.length; i++) {
			if (i > 0)
			    regex.append(".*");
			regex.append(Pattern.quote(sections[i]));
		    }
		    regex.append("$");

		    if (fileName.matches(regex.toString())) {
			matchingPeakLists.add(pl);
			continue plCheck;
		    }
		} catch (PatternSyntaxException e) {
		    e.printStackTrace();
		    continue;
		}

	    }
	}
	return matchingPeakLists.toArray(new PeakList[0]);
    }

    @Override
    public void setValue(String newValue[]) {
	this.values = newValue;
    }

    public void setValue(PeakList newValue[]) {
	this.values = new String[newValue.length];
	for (int i = 0; i < newValue.length; i++) {
	    this.values[i] = newValue[i].getName();
	}
    }

    @Override
    public PeakListsParameter cloneParameter() {
	PeakListsParameter copy = new PeakListsParameter(minCount, maxCount);
	copy.values = values;
	return copy;
    }

    @Override
    public String getName() {
	return "Peak lists (input)";
    }

    @Override
    public String getDescription() {
	return "Peak lists that this module will take as its input.";
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
	PeakList matchingPeakLists[] = getMatchingPeakLists();
	if (matchingPeakLists.length < minCount) {
	    errorMessages.add("At least " + minCount
		    + " peak lists must be selected");
	    return false;
	}
	if (matchingPeakLists.length > maxCount) {
	    errorMessages.add("Maximum " + maxCount
		    + " peak lists may be selected");
	    return false;
	}
	return true;
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
	ArrayList<String> newValues = new ArrayList<String>();
	NodeList items = xmlElement.getElementsByTagName("item");
	for (int i = 0; i < items.getLength(); i++) {
	    String itemString = items.item(i).getTextContent();
	    newValues.add(itemString);
	}
	this.values = newValues.toArray(new String[0]);
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
	if (values == null)
	    return;
	Document parentDocument = xmlElement.getOwnerDocument();
	for (String item : values) {
	    Element newElement = parentDocument.createElement("item");
	    newElement.setTextContent(item.toString());
	    xmlElement.appendChild(newElement);
	}
    }

    @Override
    public PeakListsComponent createEditingComponent() {
	final int rows = Math.min(1, maxCount);
	return new PeakListsComponent(rows, inputsize);
    }

    @Override
    public void setValueFromComponent(PeakListsComponent component) {
	values = component.getValue();
    }

    @Override
    public void setValueToComponent(PeakListsComponent component,
	    String[] newValue) {
	component.setValue(newValue);
    }

}

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

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 
 */
public class RawDataFilesParameter implements
	UserParameter<String[], RawDataFilesComponent> {

    private int minCount, maxCount;
    private String values[];

    public RawDataFilesParameter() {
	this(1, Integer.MAX_VALUE);
    }

    public RawDataFilesParameter(int minCount) {
	this(minCount, Integer.MAX_VALUE);
    }

    public RawDataFilesParameter(int minCount, int maxCount) {
	this.minCount = minCount;
	this.maxCount = maxCount;
    }

    @Override
    public String[] getValue() {
	return values;
    }

    public RawDataFile[] getMatchingRawDataFiles() {

	if ((values == null) || (values.length == 0))
	    return new RawDataFile[0];

	RawDataFile allDataFiles[] = MZmineCore.getProjectManager()
		.getCurrentProject().getDataFiles();
	ArrayList<RawDataFile> matchingDataFiles = new ArrayList<RawDataFile>();

	fileCheck: for (RawDataFile file : allDataFiles) {
	    for (String singleValue : values) {
		final String fileName = file.getName();

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
			matchingDataFiles.add(file);
			continue fileCheck;
		    }
		} catch (PatternSyntaxException e) {
		    e.printStackTrace();
		    continue;
		}

	    }
	}
	return matchingDataFiles.toArray(new RawDataFile[0]);
    }

    @Override
    public void setValue(String newValue[]) {
	this.values = newValue;
    }

    public void setValue(RawDataFile newValue[]) {
	this.values = new String[newValue.length];
	for (int i = 0; i < newValue.length; i++) {
	    this.values[i] = newValue[i].getName();
	}
    }

    @Override
    public RawDataFilesParameter cloneParameter() {
	RawDataFilesParameter copy = new RawDataFilesParameter(minCount,
		maxCount);
	copy.values = values;
	return copy;
    }

    @Override
    public String getName() {
	return "Raw data files (input)";
    }

    @Override
    public String getDescription() {
	return "Raw data files that this module will take as its input.";
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
	RawDataFile matchingFiles[] = getMatchingRawDataFiles();
	if (matchingFiles.length < minCount) {
	    errorMessages.add("At least " + minCount
		    + " raw data files must be selected");
	    return false;
	}
	if (matchingFiles.length > maxCount) {
	    errorMessages.add("Maximum " + maxCount
		    + " raw data files may be selected");
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
    public RawDataFilesComponent createEditingComponent() {
	final int rows = Math.min(4, maxCount);
	return new RawDataFilesComponent(rows);
    }

    @Override
    public void setValueFromComponent(RawDataFilesComponent component) {
	values = component.getValue();
    }

    @Override
    public void setValueToComponent(RawDataFilesComponent component,
	    String[] newValue) {
	component.setValue(newValue);
    }

}

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

package net.sf.mzmine.modules.projectmethods.projectload.version_2_3;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.projectmethods.projectload.UserParameterOpenHandler;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class UserParameterOpenHandler_2_3 extends DefaultHandler implements
	UserParameterOpenHandler {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private StringBuffer charBuffer;

    private UserParameter<?, ?> currentParameter;
    private ArrayList<String> currentOptions;
    private Hashtable<RawDataFile, Object> currentValues;
    private String currentDataFileID;

    private MZmineProject newProject;
    private Hashtable<String, RawDataFile> dataFilesIDMap;

    private int parsedParams, totalParams;

    private boolean canceled = false;

    public UserParameterOpenHandler_2_3(MZmineProject newProject,
	    Hashtable<String, RawDataFile> dataFilesIDMap) {
	this.newProject = newProject;
	this.dataFilesIDMap = dataFilesIDMap;
	currentOptions = new ArrayList<String>();
	currentValues = new Hashtable<RawDataFile, Object>();
    }

    /**
     * Load the user parameters
     */
    public void readUserParameters(InputStream inputStream) throws IOException,
	    ParserConfigurationException, SAXException {

	logger.info("Loading user parameters");
	charBuffer = new StringBuffer();

	// Parse the XML file
	SAXParserFactory factory = SAXParserFactory.newInstance();
	SAXParser saxParser = factory.newSAXParser();
	saxParser.parse(inputStream, this);

    }

    /**
     * @return the progress of these functions loading the peak list from the
     *         zip file.
     */
    public double getProgress() {
	if (totalParams == 0)
	    return 0;
	return (double) parsedParams / totalParams;
    }

    public void cancel() {
	canceled = true;
    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String namespaceURI, String lName, String qName,
	    Attributes attrs) throws SAXException {

	if (canceled)
	    throw new SAXException("Parsing canceled");

	// <PARAMETERS>
	if (qName.equals(UserParameterElementName_2_3.PARAMETERS
		.getElementName())) {
	    String count = attrs.getValue(UserParameterElementName_2_3.COUNT
		    .getElementName());
	    totalParams = Integer.parseInt(count);
	}

	// <PARAMETER>
	if (qName.equals(UserParameterElementName_2_3.PARAMETER
		.getElementName())) {

	    String name = attrs.getValue(UserParameterElementName_2_3.NAME
		    .getElementName());
	    String type = attrs.getValue(UserParameterElementName_2_3.TYPE
		    .getElementName());

	    if (type.equals(DoubleParameter.class.getSimpleName())) {
		currentParameter = new DoubleParameter(name, null);
	    } else if (type.equals(StringParameter.class.getSimpleName())) {
		currentParameter = new StringParameter(name, null);
	    } else if (type.equals(ComboParameter.class.getSimpleName())) {
		currentParameter = new ComboParameter<String>(name, null,
			new String[0]);
	    } else {
		throw new SAXException("Unknown parameter type: " + type);
	    }

	    logger.finest("Loading parameter " + name);

	    currentOptions.clear();
	    currentValues.clear();

	}

	// <VALUE>
	if (qName.equals(UserParameterElementName_2_3.VALUE.getElementName())) {
	    currentDataFileID = attrs
		    .getValue(UserParameterElementName_2_3.DATA_FILE
			    .getElementName());
	}

    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public void endElement(String namespaceURI, String sName, String qName)
	    throws SAXException {

	if (canceled)
	    throw new SAXException("Parsing canceled");

	// <OPTION>
	if (qName.equals(UserParameterElementName_2_3.OPTION.getElementName())) {
	    String optionValue = getTextOfElement();
	    currentOptions.add(optionValue);
	}

	// <VALUE>
	if (qName.equals(UserParameterElementName_2_3.VALUE.getElementName())) {
	    RawDataFile currentDataFile = dataFilesIDMap.get(currentDataFileID);
	    String valueString = getTextOfElement();
	    Object value;
	    if (currentParameter instanceof DoubleParameter) {
		value = new Double(valueString);
	    } else
		value = valueString;
	    currentValues.put(currentDataFile, value);
	}

	// <PARAMETER>
	if (qName.equals(UserParameterElementName_2_3.PARAMETER
		.getElementName())) {
	    if (currentParameter instanceof ComboParameter) {
		String newChoices[] = currentOptions.toArray(new String[0]);
		((ComboParameter<String>) currentParameter)
			.setChoices(newChoices);
	    }
	    newProject.addParameter(currentParameter);

	    for (RawDataFile dataFile : currentValues.keySet()) {
		Object value = currentValues.get(dataFile);
		newProject.setParameterValue(currentParameter, dataFile, value);
	    }

	    parsedParams++;

	}

    }

    /**
     * Return a string without tab an EOF characters
     * 
     * @return String element text
     */
    private String getTextOfElement() {
	String text = charBuffer.toString();
	text = text.replaceAll("[\n\r\t]+", "");
	text = text.replaceAll("^\\s+", "");
	charBuffer.setLength(0);
	return text;
    }

    /**
     * characters()
     * 
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char buf[], int offset, int len) throws SAXException {
	charBuffer = charBuffer.append(buf, offset, len);
    }

}

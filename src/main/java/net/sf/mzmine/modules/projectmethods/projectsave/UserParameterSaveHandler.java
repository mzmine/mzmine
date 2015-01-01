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

package net.sf.mzmine.modules.projectmethods.projectsave;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.project.impl.MZmineProjectImpl;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

class UserParameterSaveHandler {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private MZmineProjectImpl project;
    private Hashtable<RawDataFile, String> dataFilesIDMap;
    private int numOfParameters, completedParameters;
    private OutputStream finalStream;
    private boolean canceled = false;

    UserParameterSaveHandler(OutputStream finalStream,
	    MZmineProjectImpl project,
	    Hashtable<RawDataFile, String> dataFilesIDMap) {
	this.finalStream = finalStream;
	this.project = project;
	this.dataFilesIDMap = dataFilesIDMap;
    }

    /**
     * Function which creates an XML file with user parameters
     */
    void saveParameters() throws SAXException, IOException,
	    TransformerConfigurationException {

	logger.info("Saving user parameters");

	StreamResult streamResult = new StreamResult(finalStream);
	SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory
		.newInstance();

	TransformerHandler hd = tf.newTransformerHandler();

	Transformer serializer = hd.getTransformer();
	serializer.setOutputProperty(OutputKeys.INDENT, "yes");
	serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

	hd.setResult(streamResult);
	hd.startDocument();

	UserParameter<?, ?> projectParameters[] = project.getParameters();

	AttributesImpl atts = new AttributesImpl();

	atts.addAttribute("", "",
		UserParameterElementName.COUNT.getElementName(), "CDATA",
		String.valueOf(projectParameters.length));

	hd.startElement("", "",
		UserParameterElementName.PARAMETERS.getElementName(), atts);

	atts.clear();

	// <PARAMETER>
	for (UserParameter<?, ?> parameter : project.getParameters()) {

	    if (canceled)
		return;

	    logger.finest("Saving user parameter " + parameter.getName());

	    atts.addAttribute("", "",
		    UserParameterElementName.NAME.getElementName(), "CDATA",
		    parameter.getName());

	    atts.addAttribute("", "",
		    UserParameterElementName.TYPE.getElementName(), "CDATA",
		    parameter.getClass().getSimpleName());

	    hd.startElement("", "",
		    UserParameterElementName.PARAMETER.getElementName(), atts);

	    atts.clear();

	    fillParameterElement(parameter, hd);

	    hd.endElement("", "",
		    UserParameterElementName.PARAMETER.getElementName());
	    completedParameters++;
	}

	hd.endElement("", "",
		UserParameterElementName.PARAMETERS.getElementName());

	hd.endDocument();

    }

    /**
     * Create the part of the XML document related to the scans
     * 
     * @param scan
     * @param element
     */
    private void fillParameterElement(UserParameter<?, ?> parameter,
	    TransformerHandler hd) throws SAXException, IOException {

	AttributesImpl atts = new AttributesImpl();

	RawDataFile dataFiles[] = project.getDataFiles();

	if (parameter instanceof ComboParameter) {
	    Object choices[] = ((ComboParameter<?>) parameter).getChoices();

	    for (Object choice : choices) {
		hd.startElement("", "",
			UserParameterElementName.OPTION.getElementName(), atts);

		hd.characters(choice.toString().toCharArray(), 0, choice
			.toString().length());
		hd.endElement("", "",
			UserParameterElementName.OPTION.getElementName());
	    }

	}

	for (RawDataFile dataFile : dataFiles) {

	    Object value = project.getParameterValue(parameter, dataFile);

	    if (value == null)
		continue;

	    String valueString = String.valueOf(value);
	    String dataFileID = dataFilesIDMap.get(dataFile);

	    atts.addAttribute("", "",
		    UserParameterElementName.DATA_FILE.getElementName(),
		    "CDATA", dataFileID);

	    hd.startElement("", "",
		    UserParameterElementName.VALUE.getElementName(), atts);

	    atts.clear();

	    hd.characters(valueString.toCharArray(), 0, valueString.length());
	    hd.endElement("", "",
		    UserParameterElementName.VALUE.getElementName());

	}

    }

    /**
     * 
     * @return the progress of these functions saving the raw data information
     *         to the zip file.
     */
    double getProgress() {
	if (numOfParameters == 0)
	    return 0;
	return (double) completedParameters / numOfParameters;
    }

    void cancel() {
	canceled = true;
    }
}

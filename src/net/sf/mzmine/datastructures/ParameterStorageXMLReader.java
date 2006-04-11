/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package net.sf.mzmine.datastructures;
import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.obsoletedistributionframework.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;
import net.sf.mzmine.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;


import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

public class ParameterStorageXMLReader extends DefaultHandler {

	private String charBuffer;
	private ParameterStorage parameterStorage;


	public ParameterStorageXMLReader(ParameterStorage _parameterStorage) {
		parameterStorage = _parameterStorage;
	}

    //===========================================================
    // SAX DocumentHandler methods
    //===========================================================


	/**
	 * startDocument()
	 */
    public void startDocument() throws SAXException {
    }


	/**
	 * endDocument()
	 */
    public void endDocument() throws SAXException {
    }


	/**
	 * startElement()
	 */
    public void startElement(String namespaceURI,
                             String lName, // local name
                             String qName, // qualified name
                             Attributes attrs)
    throws SAXException
    {

		charBuffer = new String();

		if (qName.equalsIgnoreCase(parameterStorage.getGeneralParameters().getParameterTagName())) { parameterStorage.getGeneralParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getAlignmentResultExporterParameters().getParameterTagName())) { parameterStorage.getAlignmentResultExporterParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getBatchModeDialogParameters().getParameterTagName())) { parameterStorage.getBatchModeDialogParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getMeanFilterParameters().getParameterTagName())) { parameterStorage.getMeanFilterParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getSavitzkyGolayFilterParameters().getParameterTagName())) { parameterStorage.getSavitzkyGolayFilterParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getChromatographicMedianFilterParameters().getParameterTagName())) { parameterStorage.getChromatographicMedianFilterParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getCropFilterParameters().getParameterTagName())) { parameterStorage.getCropFilterParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getLocalPickerParameters().getParameterTagName())) { parameterStorage.getLocalPickerParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getRecursiveThresholdPickerParameters().getParameterTagName())) { parameterStorage.getRecursiveThresholdPickerParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getCentroidPickerParameters().getParameterTagName())) { parameterStorage.getCentroidPickerParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getSimpleDeisotoperParameters().getParameterTagName())) { parameterStorage.getSimpleDeisotoperParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getCombinatorialDeisotoperParameters().getParameterTagName())) { parameterStorage.getCombinatorialDeisotoperParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getIncompleteIsotopePatternFilterParameters().getParameterTagName())) { parameterStorage.getIncompleteIsotopePatternFilterParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getJoinAlignerParameters().getParameterTagName())) { parameterStorage.getJoinAlignerParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getFastAlignerParameters().getParameterTagName())) { parameterStorage.getFastAlignerParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getAlignmentResultVisualizerCDAPlotViewParameters().getParameterTagName())) { parameterStorage.getAlignmentResultVisualizerCDAPlotViewParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getAlignmentResultVisualizerSammonsPlotViewParameters().getParameterTagName())) { parameterStorage.getAlignmentResultVisualizerSammonsPlotViewParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getAlignmentResultFilterByGapsParameters().getParameterTagName())) { parameterStorage.getAlignmentResultFilterByGapsParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getSimpleGapFillerParameters().getParameterTagName())) { parameterStorage.getSimpleGapFillerParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getLinearNormalizerParameters().getParameterTagName())) { parameterStorage.getLinearNormalizerParameters().loadXMLAttributes(attrs); }
		if (qName.equalsIgnoreCase(parameterStorage.getStandardCompoundNormalizerParameters().getParameterTagName())) { parameterStorage.getStandardCompoundNormalizerParameters().loadXMLAttributes(attrs); }

    }



	/**
	 * endElement()
	 */
    public void endElement(String namespaceURI,
                           String sName, // simple name
                           String qName  // qualified name
                          )
    throws SAXException
    {

		if (qName.equalsIgnoreCase("SomeTag")) {
		}

    }


	/**
	 * characters()
	 */
    public void characters(char buf[], int offset, int len)
    throws SAXException
    {
        String s = new String(buf, offset, len);
        charBuffer = charBuffer.concat(new String(buf, offset, len));

    }


}
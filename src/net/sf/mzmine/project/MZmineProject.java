/*
 * Copyright 2006 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.project;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterValue;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.userinterface.Desktop;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class represents a MZmine project. That includes raw data files,
 * processed raw data files, peak lists, alignment results....
 */
public class MZmineProject {

    private Desktop desktop;

    private static MZmineProject currentProject;

    private Vector<OpenedRawDataFile> projectFiles;
    private Vector<AlignmentResult> projectResults;
    private Hashtable<Parameter, ParameterValue> projectParameters; // Maps project parameters to their current values

    public MZmineProject() {
    	currentProject = this;
    	
        projectFiles = new Vector<OpenedRawDataFile>();
        projectResults = new Vector<AlignmentResult>();
        projectParameters = new Hashtable<Parameter, ParameterValue>();
        
    }

    public static MZmineProject getCurrentProject() {
        assert currentProject != null;
        return currentProject;
    }

    public void setParameterValue(Parameter parameter, ParameterValue value) {
    	projectParameters.put(parameter, value);
    }

    /**
     * Returns current value of the parameter.
     * If current value is not set in the project, then returns default value of the parameter and sets current value equal to default value.
     * @param parameter
     * @return Current value of the parameter or null if parameter is null
     */
    public ParameterValue getParameterValue(Parameter parameter) {
    	if (parameter==null) return null;
        return projectParameters.get(parameter);
    }

    /**
     * Reads all parameter settings from a file
     *
     * @param paramFile Parameter settings file
     */
    public void readParameters(File paramFile) throws IOException {

        // Read XML file to a DOM document
        DocumentBuilder docBuilder;
        Document doc;
        try {
            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = docBuilder.parse(paramFile);
        } catch (ParserConfigurationException e) {
            throw new IOException(e.toString());
        } catch (SAXException e) {
            throw new IOException(e.toString());
        }

        // Let parameter objects fetch their values from document
        NodeList n = doc.getElementsByTagName("RawDataFilters");
        Element filtersParameters = (Element) (n.item(0));
        /*
         * meanFilterParameters.readFromXML(filtersParameters);
         * chromatographicMedianFilterParameters.readFromXML(filtersParameters);
         * savitzkyGolayFilterParameters.readFromXML(filtersParameters);
         * zoomScanFilterParameters.readFromXML(filtersParameters);
         */

    }

    /**
     * Writes all parameter values to file
     *
     * @param paramFile Parameter settings file
     */
    public void writeParameters(File paramFile) throws IOException {
        FileWriter paramFileWriter;

        // Create new DOM document
        DocumentBuilder docBuilder;
        Document doc;
        try {
            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new IOException(e.toString());
        }

        Element rootElement = doc.createElement("MZmineParameters");
        doc.appendChild(rootElement);

        // Ask parameter object to add their elements to the document
        // Raw data filters
        Element filtersParameters = doc.createElement("RawDataFilters");
        rootElement.appendChild(filtersParameters);
        /*
         * filtersParameters.appendChild(meanFilterParameters.addToXML(doc));
         * filtersParameters.appendChild(chromatographicMedianFilterParameters.addToXML(doc));
         * filtersParameters.appendChild(savitzkyGolayFilterParameters.addToXML(doc));
         * filtersParameters.appendChild(zoomScanFilterParameters.addToXML(doc));
         */

        // Write a DOM document to a file
        try {
            Source source = new DOMSource(doc);
            FileOutputStream ostream = new FileOutputStream(paramFile);
            Result result = new StreamResult(ostream);
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(source, result);
            ostream.close();
        } catch (TransformerConfigurationException e) {
            throw new IOException(e.toString());
        } catch (TransformerException e) {
            throw new IOException(e.toString());
        }

    }

    public void addFile(OpenedRawDataFile newFile) {
        projectFiles.add(newFile);
        desktop.addDataFile(newFile);
    }

    public void removeFile(OpenedRawDataFile file) {
        projectFiles.remove(file);
        desktop.removeDataFile(file);
    }

    public OpenedRawDataFile[] getDataFiles() {
        return projectFiles.toArray(new OpenedRawDataFile[0]);
    }

    public void addAlignmentResult(AlignmentResult newResult) {
		projectResults.add(newResult);
		desktop.addAlignmentResult(newResult);
	}

	public void removeAlignmentResult(AlignmentResult result) {
		projectResults.remove(result);
		desktop.removeAlignmentResult(result);
	}

    public AlignmentResult[] getAlignmentResults() {
        return projectResults.toArray(new AlignmentResult[0]);
    }

    /**
     *
     */
    public void initModule(MZmineCore core) {

        this.desktop = core.getDesktop();

    }

}

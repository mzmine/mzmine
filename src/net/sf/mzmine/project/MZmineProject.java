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

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.userinterface.Desktop;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class represents a MZmine project. That includes raw data files,
 * processed raw data files, peak lists, alignment results....
 */
public class MZmineProject {

    private Desktop desktop;

    private static MZmineProject currentProject;

    private Vector<OpenedRawDataFile> projectFiles;
    private Vector<PeakList> projectResults;
    private Vector<Parameter> projectParameters;

    public MZmineProject() {
    	currentProject = this;
    	
        projectFiles = new Vector<OpenedRawDataFile>();
        projectResults = new Vector<PeakList>();
        projectParameters = new Vector<Parameter>();
        
    }

    public static MZmineProject getCurrentProject() {
        assert currentProject != null;
        return currentProject;
    }

    /**
     * @param parameter
     */
    public void addParameter(Parameter parameter) {
    	projectParameters.add(parameter);
    }

    /**
     * @param parameter
     */
    public void removeParameter(Parameter parameter) {
    	projectParameters.remove(parameter);  	
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

    public void addAlignmentResult(PeakList newResult) {
		projectResults.add(newResult);
		desktop.addAlignmentResult(newResult);
	}

	public void removeAlignmentResult(PeakList result) {
		projectResults.remove(result);
		desktop.removeAlignmentResult(result);
	}

    public PeakList[] getAlignmentResults() {
        return projectResults.toArray(new PeakList[0]);
    }

    /**
     *
     */
    public void initModule(MZmineCore core) {

        this.desktop = core.getDesktop();

    }

}

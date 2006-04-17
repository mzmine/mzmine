/*
 * Copyright 2006 Okinawa Institute of Science and Technology
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

/**
 * 
 */
package net.sf.mzmine.io.mzxml;

import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.mzmine.io.RawDataFile.PreloadLevel;
import net.sf.mzmine.taskcontrol.DistributableTask;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.util.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 */
public class MZXMLFileOpeningTask extends DefaultHandler implements DistributableTask {

    private File originalFile;

    private MZXMLFile buildingFile;

    private TaskStatus status;

    private int totalScans;

    private int parsedScans;

    private String errorMessage;

    private StringBuffer charBuffer;

    private int scanIndexID; // While reading <offset> tag for a scan index,

    private boolean readingIndex;

    private boolean readingScan;

    private MZXMLScan buildingScan;

    /**
     * 
     */
    public MZXMLFileOpeningTask(File fileToOpen, PreloadLevel preloadLevel) {

        originalFile = fileToOpen;
        status = TaskStatus.WAITING;

        charBuffer = new StringBuffer(256);
        // Get current date which is also required in conversions
        
        buildingFile = new MZXMLFile(fileToOpen, preloadLevel);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Opening file " + originalFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        return totalScans == 0 ? 0 : (float) parsedScans / totalScans;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getResult()
     */
    public Object getResult() {
        return buildingFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getPriority()
     */
    public TaskPriority getPriority() {
        return TaskPriority.NORMAL;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;
        Logger.put("Started parsing file " + originalFile);

        // Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();

        try {
            SAXParser saxParser = factory.newSAXParser();
            // Parse the file

            saxParser.parse(originalFile, this);
            saxParser.reset();

        } catch (Exception e) {
            /* we may already have set the status to CANCELED */
            if (status == TaskStatus.PROCESSING)
                status = TaskStatus.ERROR;
            errorMessage = e.toString();
            e.printStackTrace();
            return;
        }

        status = TaskStatus.FINISHED;

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        status = TaskStatus.CANCELED;

    }

    public void startElement(String namespaceURI, String lName, // local name
            String qName, // qualified name
            Attributes attrs) throws SAXException {

        if (status == TaskStatus.CANCELED)
            throw new SAXException("Parsing Cancelled");

        // <scan>
        if (qName.equalsIgnoreCase("scan")) {

            readingScan = true;
            buildingScan = new MZXMLScan();

        }
        
        // if reading a scan, pass this method to the scan SAX handler
        if (readingScan) {
            buildingScan.startElement(namespaceURI, lName, qName, attrs);
            return;
        }

        // clean the current char buffer for the new element
        charBuffer.setLength(0);

        // <msRun>
        if (qName.equals("msRun")) {
            totalScans = Integer.parseInt(attrs.getValue("scanCount"));
        }



        // <index>
        if (qName.equalsIgnoreCase("index")) {

            String indexName = attrs.getValue("name");
            if (indexName.equals("scan"))
                readingIndex = true;

        }

        // <offset>
        if (qName.equalsIgnoreCase("offset")) {
            scanIndexID = Integer.parseInt(attrs.getValue("id"));
        }

        // <msManufacturer>
        if (qName.equalsIgnoreCase("msManufacturer")) {
            buildingFile.addDataDescription("MS manufacturer: "
                    + attrs.getValue("value"));
        }

        // <msModel>
        if (qName.equalsIgnoreCase("msModel")) {
            buildingFile.addDataDescription("MS model: "
                    + attrs.getValue("value"));
        }

        // <parentFile>
        if (qName.equalsIgnoreCase("parentFile")) {
            buildingFile.addDataDescription("Original raw file: "
                    + attrs.getValue("fileName") + " ["
                    + attrs.getValue("fileType") + "]");
        }

    }

    /**
     * endElement()
     */
    public void endElement(String namespaceURI, String sName, // simple name
            String qName // qualified name
    ) throws SAXException {

        // </scan>
        if (qName.equalsIgnoreCase("scan")) {

            readingScan = false;
            parsedScans++;

            /* scan reading is finished, add it to the file */
            buildingFile.addScan(buildingScan);

        }

        if (readingScan) {
            buildingScan.endElement(namespaceURI, sName, qName);
            return;
        }

        // </index>
        if (qName.equalsIgnoreCase("index")) {
            readingIndex = false;
        }

        // </offset>
        if (qName.equalsIgnoreCase("offset")) {
            if (readingIndex)
                buildingFile.addIndexEntry(new Integer(scanIndexID), new Long(
                        charBuffer.toString()));
        }

    }

    /**
     * characters()
     * 
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char buf[], int offset, int len) throws SAXException {

        if (readingScan) {
            buildingScan.characters(buf, offset, len);
            return;
        }
        charBuffer = charBuffer.append(buf, offset, len);
    }

}

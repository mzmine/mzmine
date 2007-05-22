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

package net.sf.mzmine.io.impl;

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.io.mzxml.MZXMLFileWriter;
import net.sf.mzmine.modules.DataProcessingMethod;


/**
 *
 */
public class OpenedRawDataFileImpl implements OpenedRawDataFile {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    private Vector<Operation> processingHistory;
    private String dataDescription;
    private RawDataFile currentFile;
    private File originalFile;
    private PeakList peakList;
    
    public OpenedRawDataFileImpl(RawDataFile rawDataFile, String dataDescription) {
        processingHistory = new Vector<Operation>();
        this.dataDescription = dataDescription;
        this.currentFile = rawDataFile;
        this.originalFile = rawDataFile.getFile();
    }
    
    
    
    /**
     * @see net.sf.mzmine.io.OpenedRawDataFile#getHistory()
     */
    public Vector<Operation> getProcessingHistory() {
        return processingHistory;
    }

    /**
     * @see net.sf.mzmine.io.OpenedRawDataFile#getDataDescription()
     */
    public String getDataDescription() {
        return dataDescription;
    }

    /**
     * @see net.sf.mzmine.io.OpenedRawDataFile#getCurrentFile()
     */
    public RawDataFile getCurrentFile() {
        return currentFile;
    }

    /**
     * @see net.sf.mzmine.io.OpenedRawDataFile#updateFile(net.sf.mzmine.io.RawDataFile, net.sf.mzmine.modules.DataProcessingMethod, net.sf.mzmine.data.impl.SimpleParameterSet)
     */
    public synchronized void updateFile(RawDataFile newFile, DataProcessingMethod processingMethod,
            ParameterSet parameters) {
        
        Operation op = new Operation();
        op.oldFileName = currentFile.getFile();
        op.newFileName = newFile.getFile();
        op.processingMethod = processingMethod;
        op.parameters = parameters;
        processingHistory.add(op);
        currentFile = newFile;
        
        // discard current peaklist
        peakList = null;
        
    }



    /**
     * @see net.sf.mzmine.io.OpenedRawDataFile#addFileChangedListener()
     */
    public void addFileChangedListener() {
        // TODO Auto-generated method stub
        
    }



    /**
     * @see net.sf.mzmine.io.OpenedRawDataFile#getOriginalFile()
     */
    public File getOriginalFile() {
        return originalFile;
    }



    /**
     * 
     */
    public RawDataFileWriter createNewTemporaryFile() throws IOException {

        // Create new temp file
        File workingCopy;
        try {
            workingCopy = File.createTempFile("MZmine", null);
            logger.finest("Creating temporary file " + workingCopy.getAbsolutePath());
            workingCopy.deleteOnExit();
        } catch (SecurityException e) {
            logger.severe("Could not prepare newly created temporary copy for deletion on exit.");
            throw new IOException(
                    "Could not prepare newly created temporary copy for deletion on exit.");
        }

        return new MZXMLFileWriter(currentFile, workingCopy, currentFile.getPreloadLevel());

    }



    /**
     * @see net.sf.mzmine.io.OpenedRawDataFile#addHistoryEntry(net.sf.mzmine.modules.DataProcessingMethod, net.sf.mzmine.data.impl.SimpleParameterSet)
     */
    public void addHistoryEntry(File file, DataProcessingMethod method, ParameterSet param) {
        Operation op = new Operation();
        op.oldFileName = file;
        op.newFileName = file;
        op.processingMethod = method;
        op.parameters = param;
        processingHistory.add(op);        
    }



    /**
     * @see net.sf.mzmine.io.OpenedRawDataFile#addHistoryEntry(net.sf.mzmine.io.OpenedRawDataFile.Operation)
     */
    public void addHistoryEntry(Operation op) {
        processingHistory.add(op);        
    }
    
    @Override
	public String toString() {
        return originalFile.getName();
    }



    /**
     * @see net.sf.mzmine.io.OpenedRawDataFile#getPeakList()
     */
    public PeakList getPeakList() {
        return peakList;
    }



    /**
     * @see net.sf.mzmine.io.OpenedRawDataFile#setPeakList(net.sf.mzmine.data.PeakList)
     */
    public void setPeakList(PeakList p) {
        this.peakList = p;
    }



    /**
     * @see net.sf.mzmine.io.OpenedRawDataFile#hasPeakList()
     */
    public boolean hasPeakList() {
        return (peakList != null);
    }




}

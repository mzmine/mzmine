/**
 * 
 */
package net.sf.mzmine.io.impl;

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.io.OpenedRawDataFile.Operation;
import net.sf.mzmine.io.mzxml.MZXMLFileWriter;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;


/**
 *
 */
public class OpenedRawDataFileImpl implements OpenedRawDataFile {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    private Vector<Operation> processingHistory;
    private String dataDescription;
    private RawDataFile currentFile;
    private File originalFile;
    
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
     * @see net.sf.mzmine.io.OpenedRawDataFile#updateFile(net.sf.mzmine.io.RawDataFile, net.sf.mzmine.methods.Method, net.sf.mzmine.methods.MethodParameters)
     */
    public synchronized void updateFile(RawDataFile newFile, Method processingMethod,
            MethodParameters parameters) {
        
        Operation op = new Operation();
        op.oldFileName = currentFile.getFile();
        op.newFileName = newFile.getFile();
        op.processingMethod = processingMethod;
        op.parameters = parameters;
        processingHistory.add(op);
        currentFile = newFile;
        
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
     * @see net.sf.mzmine.io.OpenedRawDataFile#addHistoryEntry(net.sf.mzmine.methods.Method, net.sf.mzmine.methods.MethodParameters)
     */
    public void addHistoryEntry(File file, Method method, MethodParameters param) {
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
    
    public String toString() {
        return originalFile.getName();
    }

}

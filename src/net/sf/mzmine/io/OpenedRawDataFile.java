/**
 * 
 */
package net.sf.mzmine.io;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;


/**
 *
 */
public interface OpenedRawDataFile {
    
    class Operation {
        public File oldFileName;
        public File newFileName;
        public Method processingMethod;
        public MethodParameters parameters;
    }
    
    public Vector<Operation> getProcessingHistory();

    public void addHistoryEntry(File file, Method method, MethodParameters param);
    public void addHistoryEntry(Operation op);
    
    /**
     * @return Original filename
     */
    public String toString();
    
    public String getDataDescription();
    
    public RawDataFile getCurrentFile();
    
    public File getOriginalFile();
    
    public RawDataFileWriter createNewTemporaryFile() throws IOException;
    
    public void updateFile(RawDataFile newFile, Method processingMethod, MethodParameters parameters);
    
    public void addFileChangedListener(); // TODO, how to remove listeners?
    
    
}

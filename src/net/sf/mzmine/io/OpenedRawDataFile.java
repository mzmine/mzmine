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

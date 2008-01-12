/*
 * Copyright 2006-2007 The MZmine Development Team
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

/**
 * IO controller
 * 
 */
public interface IOController {

    /**
     * This method is non-blocking, it places a request to open these files and
     * exits immediately.
     */
    public void openFiles(File[] files, PreloadLevel preloadLevel);
 
    /**
     * 
     * @param name
     * @return
     * @throws IOException
     */
    public RawDataFileWriter createNewFile(String fileName,String suffix, PreloadLevel preloadLevel) throws IOException;
 
    /**
     * 
     * @param name
     * @return
     * @throws IOException
     */
    public RawDataFileWriter createNewFile(File file, PreloadLevel preloadLevel) throws IOException;
 
    /**
     * 
     * @param projectFile
     * @return void
     * @throws IOException
     */
    public void openProject(File projectFile) throws IOException; 
    /**
     * 
     * @param projectFile
     * @return none
     * @throws IOException
     */
    public void saveProject(File projectFile) throws IOException; 

}

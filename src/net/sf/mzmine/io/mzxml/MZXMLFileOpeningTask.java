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

import net.sf.mzmine.taskcontrol.Task;

/**
 *
 */
public class MZXMLFileOpeningTask implements Task {

    File originalFile;
    
    /**
     * 
     */
    public MZXMLFileOpeningTask(File fileToOpen) {
        originalFile = fileToOpen;
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
    public int getFinishedPercentage() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getResult()
     */
    public Object getResult() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getPriority()
     */
    public TaskPriority getPriority() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        // TODO Auto-generated method stub

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        
        
    }

}

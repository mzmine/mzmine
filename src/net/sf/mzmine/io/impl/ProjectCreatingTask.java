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

package net.sf.mzmine.io.impl;


import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.taskcontrol.Task;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;

/**
 * 
 */
public class ProjectCreatingTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private File projectDir;
    private TaskStatus status;
    private String errorMessage;
    
    private enum Finished{
    	STARTED(0.1f),
    	COMPLETE(1.0f);
    	
    	private final float value;
    	Finished(float value){
    		this.value=value;
    	}
    	public float getValue(){
    		return this.value;
    	}
    }
    private Finished finished;
    MZmineProjectImpl project;
 
    public ProjectCreatingTask(File projectDir) {
        this.projectDir= projectDir;
        status = TaskStatus.WAITING;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Creating project" + projectDir;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        return finished.getValue();
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
    public MZmineProjectImpl getResult() {
        return project;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        // Update task status
        logger.info("Started creating project" + projectDir);
        status = TaskStatus.PROCESSING;
        finished=Finished.STARTED;
        
        try {
        	projectDir.mkdir();
        	project = new MZmineProjectImpl(projectDir);
        	project.setLocation(projectDir);
 
        	//initialize db4o database
        	File dbFile = new File(projectDir, "mzmine.db4o");
			ObjectContainer db = Db4o.openFile(dbFile.toString());
			db.set(project);
			db.commit();
			db.close();
        	
		} catch (Throwable e) {
            logger.log(Level.SEVERE, "Could not create project : " 
                    + projectDir.toString(), e);
            errorMessage = e.toString();
            status = TaskStatus.ERROR;
            return;
        }

        logger.info("Finished openning " + projectDir);
        finished=Finished.COMPLETE;
        status = TaskStatus.FINISHED;

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        logger.info("Cancelling opening of project" + projectDir);
        status = TaskStatus.CANCELED;
    }

}

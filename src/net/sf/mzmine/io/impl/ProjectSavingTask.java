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
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.taskcontrol.Task;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;

/**
 * 
 */
public class ProjectSavingTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private File projectDestFile;
    private TaskStatus status;
    private String errorMessage;
    private MZmineProjectImpl project;
    private float finished=(float)0.0;
    private static final float FINISHED_STARTED=(float)0.1;
    private static final float FINISHED_OBJECT_FREEZED=(float)0.3;
    private static final float FINISHED_START_ZIPPING=(float)0.4;
    private static final float FINISHED_COMPLETE=(float)1.0;
    /**
     * 
     */
    public ProjectSavingTask(File projectFile) {

        this.projectDestFile = projectFile;
        status = TaskStatus.WAITING;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Saving project to " + projectDestFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        return finished;
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
        logger.info("Started saving project" + projectDestFile);
        status = TaskStatus.PROCESSING;
        finished=FINISHED_STARTED;
        try {        	
            //get temporary dir 
            File tempDirPath=MZmineCore.getCurrentProject().getLocation();

            //Store all project in db4o database
        	File dbFile=new File(tempDirPath,"mzmine.db4o");
        	ObjectContainer db=Db4o.openFile(dbFile.toString());
        	db.set(MZmineCore.getCurrentProject());
        	//db.set(MZmineCore.getDesktop());
        	db.commit();
        	db.close();
        	
        	finished=FINISHED_OBJECT_FREEZED;
 
        	//archive folder into zip file
        	finished=FINISHED_START_ZIPPING;
        	projectDestFile.createNewFile();
        	FileOutputStream fos=new FileOutputStream(projectDestFile);
        	ZipOutputStream zos=new ZipOutputStream(fos);
        	zos.setLevel(9);
        	
        	
        	//gather files to archive
        	ArrayList <File> files=new ArrayList<File>();
        	files.add(dbFile);
        	for (RawDataFile file:MZmineCore.getCurrentProject().getDataFiles()){
        		if (!file.getScanDataFileName().equals(null)){
        			File scanFile=new File(tempDirPath,file.getScanDataFileName());
        		files.add(scanFile);
        		}
        	}
        	
        	ZipEntry entry=null;
        	byte buf[] = new byte[1024];
        	BufferedInputStream input=null;
        	String projectDestFileName=projectDestFile.getName();
        	int i;
        	for (i=0;i<files.size();i++){
        		File file=files.get(i);
        		
        		entry=new ZipEntry(projectDestFileName+File.separator+file.getName());
        		zos.putNextEntry(entry);
        		input=new BufferedInputStream(new FileInputStream(file));
        		int count=0;
        		while((count=input.read(buf, 0,1024))!=-1){
        			zos.write(buf, 0, count);
        		}
        		input.close();
        		zos.closeEntry();
        		finished=(FINISHED_START_ZIPPING+(FINISHED_COMPLETE-FINISHED_START_ZIPPING)/files.size()*(i+1));
        	}
        	zos.close();
        	
        	//remove db4o file
        	dbFile.delete();

		} catch (Throwable e) {
            logger.log(Level.SEVERE, "Could not save project "
                    + projectDestFile.getPath(), e);
            errorMessage = e.toString();
            status = TaskStatus.ERROR;
            return;
        }

        logger.info("Finished saving " + projectDestFile);

        status = TaskStatus.FINISHED;
        finished=FINISHED_COMPLETE;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        logger.info("Cancelling saving of project" + projectDestFile);
        status = TaskStatus.CANCELED;
    }

}

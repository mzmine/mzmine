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
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.userinterface.dialogs.RawFileSettingDialog;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.query.Predicate;

/**
 * 
 */
public class ProjectOpeningTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private File projectFile;
    private TaskStatus status;
    private String errorMessage;
    
    private enum Finished{
    	STARTED(0.1f),
    	ZIP_OPENED(0.4f),
    	OBJECT_LOADED(0.7f),
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
 
    public ProjectOpeningTask(File projectFile) {
        this.projectFile= projectFile;
        status = TaskStatus.WAITING;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Opening project" + projectFile;
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
        logger.info("Started openning project" + projectFile);
        status = TaskStatus.PROCESSING;
        finished=Finished.STARTED;
        
        try {
        	
        	//load zipfile and extract into temporary dir
        	File tempDirPath=File.createTempFile("mzmine",""); 	
        	tempDirPath.delete();
        	tempDirPath.mkdir();
        	tempDirPath.deleteOnExit();
        	ZipInputStream zis=new ZipInputStream(new FileInputStream(projectFile));
        	BufferedOutputStream output=null;
        	while (true){
        		ZipEntry entry=zis.getNextEntry(); 
        		if (entry==null){
        			break;
        		}
        		String fileName=entry.getName().split("/")[1];
        		File tempFile = new File(tempDirPath,fileName);
        		FileOutputStream tempFileOut = new FileOutputStream(tempFile);
        		output=new BufferedOutputStream(tempFileOut);
        		
        		byte buf[] = new byte[1024];
        		int count=0;
        		while((count=zis.read(buf, 0,1024))!=-1){
        			output.write(buf, 0, count);
        		}
        		output.close();
        		zis.closeEntry();
        	}
        	finished=Finished.ZIP_OPENED;

			
        	//load from db4o database
        	File dbFile = new File(tempDirPath, "mzmine.db4o");
			ObjectContainer db = Db4o.openFile(dbFile.toString());
			try {
	        	List <MZmineProjectImpl>result_project= db.query(new Predicate<MZmineProjectImpl>() {
	        	    public boolean match(MZmineProjectImpl mzmineProject) {
	        	     return true;	
	        	    }
	        	});
				project = (MZmineProjectImpl) result_project.get(0);
				//activate the object within 20 references away from MZmineProjectObject
				//the depth 20 might become insufficient if you make change in object structure
				db.activate(project,20);
				

			}catch(Exception e){
				logger.fine(e.getMessage());
			}finally {
				db.close();
			}
		
			finished=Finished.OBJECT_LOADED;
			
        	//reset project tempDirPath
			project.setLocation(tempDirPath);
			
			//Check whether I can find the raw files.
			boolean foundFiles=true;
			ArrayList <RawDataFile> lostFiles=new ArrayList <RawDataFile>();
			for (RawDataFile file:project.getDataFiles()){
				if (!file.getFilePath().toString().equals("")){
					if (!file.getFilePath().exists()){
						foundFiles=false;
						lostFiles.add(file);
					}
				}
			}
			
			if (foundFiles ==false){
				RawFileSettingDialog dialog=new RawFileSettingDialog(lostFiles);
		        dialog.setVisible(true);
		        File filePaths[]=dialog.getResult();
		        if (!filePaths.toString().equals("")){//this is a real rawDataFile
		        	RawDataFile rawFiles[]=project.getDataFiles();
		        	int i;
		        	for (i=0;i<filePaths.length;i++){
		        		rawFiles[i].setFilePath(filePaths[i]);	
		        	}
		        }else{
		        	//raise some exception here
		        }

			}
			//update scanDataFile in rawDataFiles
			
			for (RawDataFile file :project.getDataFiles()){
				if (!file.getScanDataFileName().equals(null)){
				File filePath=new File(project.getLocation(),file.getScanDataFileName());
				file.updateScanDataFile(filePath);
				}
			}
			
		} catch (Throwable e) {
            logger.log(Level.SEVERE, "Could not open project "
                    + projectFile.getPath(), e);
            errorMessage = e.toString();
            status = TaskStatus.ERROR;
            return;
        }

        logger.info("Finished openning " + projectFile);
        finished=Finished.COMPLETE;
        status = TaskStatus.FINISHED;

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        logger.info("Cancelling opening of project" + projectFile);
        status = TaskStatus.CANCELED;
    }

}

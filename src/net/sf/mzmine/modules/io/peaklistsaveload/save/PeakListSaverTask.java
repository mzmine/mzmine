/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.io.peaklistsaveload.save;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.modules.io.peaklistsaveload.PeakListElementName;
import net.sf.mzmine.taskcontrol.Task;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class PeakListSaverTask implements Task{
	
	private PeakList peakList;
	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;
	private int processedRows, totalRows;

	// parameter values
	private String fileName;
	
	public PeakListSaverTask(PeakList peakList, PeakListSaverParameters parameters){
		this.peakList = peakList;

		fileName = (String) parameters
				.getParameterValue(PeakListSaverParameters.filename);
		
		this.peakList = peakList;

	}

	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public double getFinishedPercentage() {
		if (totalRows == 0) {
			return 0.0f;
		}
		return (double) processedRows / (double) totalRows;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getTaskDescription() {
		return "Saving peak list " + peakList + " to " + fileName;
	}
	
	public void run() {
		
        try {

        	File savingFile = new File(fileName);

            // load current configuration from XML
            SAXReader reader = new SAXReader();
            Document document = DocumentFactory.getInstance().createDocument();
            Element saveRoot = document.addElement(PeakListElementName.PEAKLIST.toString());
            Element info = saveRoot.addElement(PeakListElementName.PEAKLIST_INFO.toString());
            setPeakListInfo(info);
            
            
            
            
            // write the saving file
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter writer = new XMLWriter(new FileWriter(savingFile), format);
            writer.write(document);
            writer.close();


        }
        catch(Exception e){
        	
        }
		// TODO Auto-generated method stub
		
	}
	
	private void setPeakListInfo(Element info){
		
		info.addAttribute(PeakListElementName.NAME.toString(), peakList.getName());
		String dateText = "";
		if(true){//(peakList.getDateCreated() == null){
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        java.util.Date date = new java.util.Date();
        dateText = dateFormat.format(date);
		}
		info.addAttribute(PeakListElementName.DATE.toString(), dateText);
		info.addAttribute(PeakListElementName.NAME.toString(), peakList.getName());
		info.addAttribute(PeakListElementName.NAME.toString(), peakList.getName());
		
		
	}

}

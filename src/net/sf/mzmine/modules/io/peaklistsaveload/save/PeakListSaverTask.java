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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzDataPoint;
import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.modules.io.peaklistsaveload.PeakListElementName;
import net.sf.mzmine.taskcontrol.Task;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.Ostermiller.util.Base64;

public class PeakListSaverTask implements Task{
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
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
		
		totalRows = peakList.getNumberOfRows();

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
        	
        	status = TaskStatus.PROCESSING;
    		logger.info("Started saving peak list " + peakList.getName());


        	File savingFile = new File(fileName);

            // load current configuration from XML
            //SAXReader reader = new SAXReader();
            Document document = DocumentFactory.getInstance().createDocument();
            Element saveRoot = document.addElement(PeakListElementName.PEAKLIST.getElementName());
            Element newElement = saveRoot.addElement(PeakListElementName.PEAKLIST_INFO.getElementName());
            fillPeakListElement(newElement);
            
            RawDataFile[] rawDataFiles = peakList.getRawDataFiles();
            
            for (RawDataFile file: rawDataFiles){
            	newElement = saveRoot.addElement(PeakListElementName.RAWFILE.getElementName());
            	newElement.addAttribute(PeakListElementName.NAME.getElementName(), file.getName());
            	newElement.addAttribute(PeakListElementName.RTRANGE.getElementName(), String.valueOf(file.getDataMZRange(1)) );
            	newElement.addAttribute(PeakListElementName.MZRANGE.getElementName(), String.valueOf(file.getDataRTRange(1)) );
            }
            
            
            int numOfRows = peakList.getNumberOfRows();
            PeakListRow row;
            //Element newElement;
            for (int i=0;i<numOfRows;i++){
            	
            	if (status == TaskStatus.CANCELED){
            		return;
            	}
            	
            	row = peakList.getRow(i);
            	newElement = saveRoot.addElement(PeakListElementName.ROW.getElementName());
            	fillRowElement(row, newElement);
            	processedRows++;
            }
            
            
            // write the saving file
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter writer = new XMLWriter(new FileWriter(savingFile), format);
            writer.write(document);
            writer.close();


        }
        catch(Exception e){
			/* we may already have set the status to CANCELED */
			if (status == TaskStatus.PROCESSING)
				status = TaskStatus.ERROR;
			errorMessage = e.toString();
			e.printStackTrace();
			return;
        }
        
		logger.info("Finished saving " + peakList.getName() + ", saved "
				+ processedRows + " rows");
		status = TaskStatus.FINISHED;		
	}
	
	private void fillPeakListElement(Element info){
		
		info.addAttribute(PeakListElementName.NAME.getElementName(), peakList.getName());
		String dateText = "";
		if(true){//(peakList.getDateCreated() == null){
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        java.util.Date date = new java.util.Date();
        dateText = dateFormat.format(date);
		}
		info.addAttribute(PeakListElementName.NAME.getElementName(), peakList.getName());
		info.addAttribute(PeakListElementName.PEAKLIST_DATE.getElementName(), dateText);
		
	}
	
	private void fillRowElement(PeakListRow row, Element element){
		element.addAttribute(PeakListElementName.ID.getElementName(), String.valueOf(row.getID()) );
		ChromatographicPeak[] peaks = row.getPeaks();
		PeakIdentity identity = row.getPreferredCompoundIdentity();
		Element newElement;
		if (identity != null){
			newElement = element.addElement(PeakListElementName.PEAK_IDENTITY.getElementName());
			fillIdentityElement(identity, newElement);
		}
		
		for (ChromatographicPeak p: peaks){
			newElement = element.addElement(PeakListElementName.PEAK.getElementName());
			fillPeakElement(p, newElement);
		}
		
	}
	
	private void fillIdentityElement(PeakIdentity identity, Element element){
		element.addAttribute(PeakListElementName.NAME.getElementName(), identity.getName());
		element.addAttribute(PeakListElementName.FORMULA.getElementName(), identity.getCompoundFormula());
		element.addAttribute(PeakListElementName.IDENTIFICATION.getElementName(), identity.getIdentificationMethod());
	}
	
	private void fillPeakElement(ChromatographicPeak peak, Element element){
		element.addAttribute(PeakListElementName.NAME.getElementName(), peak.getDataFile().getName());
		element.addAttribute(PeakListElementName.MASS.getElementName(), String.valueOf(peak.getMZ()) );
		element.addAttribute(PeakListElementName.RT.getElementName(), String.valueOf(peak.getRT()) );
		element.addAttribute(PeakListElementName.HEIGHT.getElementName(), String.valueOf(peak.getHeight()) );
		element.addAttribute(PeakListElementName.AREA.getElementName(), String.valueOf(peak.getArea()) );
		element.addAttribute(PeakListElementName.STATUS.getElementName(), peak.getPeakStatus().toString() );
		element.addAttribute(PeakListElementName.FRAGMENT.getElementName(), String.valueOf(peak.getMostIntenseFragmentScanNumber()) );
		
		int[] scanNumbers = peak.getScanNumbers();
		Element newElement;
		MzPeak mzPeak;
		MzDataPoint[] rawDataPoints;
		for (int scan: scanNumbers ){
			mzPeak = peak.getMzPeak(scan);
			if (mzPeak == null)
				continue;
			newElement = element.addElement(PeakListElementName.MZPEAK.getElementName());
			newElement.addAttribute(PeakListElementName.SCAN.getElementName(), String.valueOf(scan));
			newElement.addAttribute(PeakListElementName.RT.getElementName(), String.valueOf(peak.getDataFile().getScan(scan).getRetentionTime()));
			newElement.addAttribute(PeakListElementName.MASS.getElementName(), String.valueOf(mzPeak.getMZ()));
			newElement.addAttribute(PeakListElementName.HEIGHT.getElementName(), String.valueOf(mzPeak.getIntensity()));
			
			/*rawDataPoints = mzPeak.getRawDataPoints();
			newElement.addAttribute("DataPoints", String.valueOf(rawDataPoints.length));
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			DataOutputStream datastream = new DataOutputStream(byteStream);
			
			for (MzDataPoint dataPoint: rawDataPoints){
				try {
					datastream.writeFloat((float)dataPoint.getMZ());
					datastream.writeFloat((float)dataPoint.getIntensity());
					datastream.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			byte[] bytes = Base64.encode(byteStream.toByteArray());
			newElement.addText(new String(bytes));*/
		}

	}

}

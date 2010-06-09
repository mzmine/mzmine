/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.peptidesearch;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.proteomics.Peptide;
import net.sf.mzmine.data.proteomics.PeptideIdentityDataFile;
import net.sf.mzmine.data.proteomics.PeptideScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.peptidesearch.fileformats.MascotParser;
import net.sf.mzmine.project.ProjectEvent;
import net.sf.mzmine.project.ProjectEvent.ProjectEventType;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.MascotParserUtils;

public class PeptideSearchTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private PeakList peakList;

	private TaskStatus status = TaskStatus.WAITING;;
	private String errorMessage;
	private int finishedLines = 0;
	
	private int numOfQueries = 0;

	private String identificationFile;
	private double significanceThreshold;
	private PeptideSearchParameters parameters;
	
	PeptideSearchTask(PeakList peakList, PeptideSearchParameters parameters) {

		this.peakList = peakList;
		this.parameters = parameters;

		identificationFile = (String) parameters
				.getParameterValue(PeptideSearchParameters.proteinFile);
		
		significanceThreshold = (Double) parameters
				.getParameterValue(PeptideSearchParameters.significanceThreshold);
	}
	
	
	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		if (numOfQueries == 0)
			return 0;
		return ((double) finishedLines) / numOfQueries;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getStatus()
	 */
	public TaskStatus getStatus() {
		return status;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Peak identification of " + peakList + " using protein identification file "
				+ identificationFile;
	}

	
	public void run() {
		status = TaskStatus.PROCESSING;
		logger.info("Started parsing file " + identificationFile);


		PeptideFileParser parser = null;
		File identityFile = new File(identificationFile);
		PeptideIdentityDataFile pepDataFile = new PeptideIdentityDataFile(identificationFile);
		pepDataFile.setSignificanceThreshold(significanceThreshold);
		
		try {

			String extension = identityFile.getName().substring(
					identityFile.getName().lastIndexOf(".") + 1).toLowerCase();
			
			//Base on the extension a different parse is used.
			if (extension.endsWith("dat")) {
				logger.info("Parse file with Mascot Parser with extension " + extension);
				parser = new MascotParser(identityFile);
			}
			
			numOfQueries = parser.getNumOfQueries();
			logger.info("Parse number of queries " + numOfQueries);
			
			
			parser.parseParameters(pepDataFile);
			logger.info("Parse Mascot parameters ");
			
			parser.parseDefaultMasses(pepDataFile);
			logger.info("Parse default masses ");
			
			logger.info("Parsing queries ...");

			//Retrieve information one by one of the peptides
			for (int i=1; i<=numOfQueries; i++){
				//logger.info("Parsing queriy = "+i);
				parser.parseQuery(i,pepDataFile);
				this.findMatch(pepDataFile.getPeptideScan(i),peakList);
				finishedLines++;
				
			}
			
		} catch (Exception e) {
			 e.printStackTrace();
			logger.log(Level.SEVERE, "Could not open file "
					+ identityFile.getPath(), e);
			errorMessage = ExceptionUtils.exceptionToString(e);
			status = TaskStatus.ERROR;
			return;
		}


		logger.info("Finished parsing " + identificationFile + ", parsed "
				+ numOfQueries + " queries");


		// Add task description to peakList
		peakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
				"Peak identification by peptide identity using MS/MS file " + identificationFile,
				parameters));

		// Notify the project manager that peaklist contents have changed
		ProjectEvent newEvent = new ProjectEvent(
				ProjectEventType.PEAKLIST_CONTENTS_CHANGED, peakList);
		MZmineCore.getProjectManager().fireProjectListeners(newEvent);

		// Update task status
		status = TaskStatus.FINISHED;

		
	}
	
	/**
	 * Finds a peak which match with the peptide's information. 
	 * First look for the raw data file in the peak list that contains 
	 * a scan with same number, ms level and data points. Then gets the 
	 * parent scan number and verifies if one of the peaks (from peaklist 
	 * and those that belongs to the selected raw data file) contains 
	 * this scan.
	 * 
	 * @param peptideScan
	 * @param peakList
	 * @return boolean	True if a match is found.
	 */
	public boolean findMatch(PeptideScan peptideScan, PeakList peakList) {
		
		if (peptideScan == null)
			return false;
		
		int rawScanNumber = peptideScan.getScanNumber();
		int precursorScanNumber = -1;
		//logger.info("Number of peptides = "+ peptideScan.getPeptides().length);
		Peptide highScorePeptide = peptideScan.getHighScorePeptide();
		Scan scan;
		
		RawDataFile[] rawDataFiles = peakList.getRawDataFiles();
		RawDataFile rawFile = null;
		
		for (int i=0; i<rawDataFiles.length; i++){
			scan = rawDataFiles[i].getScan(rawScanNumber);
			if ( scan != null){
				if ((scan.getMSLevel() >= 2) && 
						(MascotParserUtils.compareDataPointsByMass(scan.getDataPoints(),peptideScan.getDataPoints())) ){
					precursorScanNumber = scan.getParentScanNumber();
					rawFile = rawDataFiles[i];
				}
			}
		}
		
		if (precursorScanNumber < 0)
			return false;
		
		
		boolean flag = false;
		
		ChromatographicPeak[] peaks = peakList.getPeaks(rawFile);
		int massPeak;
		int expectedMassPeptide = (int) peptideScan.getPrecursorMZ();
		for (int j=0;j < peaks.length ; j++){
			massPeak = (int) peaks[j].getMZ();
			if (massPeak == expectedMassPeptide){
				int[] scanNumbers =  peaks[j].getScanNumbers();
				for (int i=0; i< scanNumbers.length; i++){
					if (scanNumbers[i] == precursorScanNumber){
						
						PeakIdentity[] identities =  peakList.getPeakRow(peaks[j]).getPeakIdentities();
						double maxScore = 0;
						double score = 0;
						boolean preffered = false;
						
						if (identities.length > 0){
							for (int k=0;k<identities.length;k++){
								score = ((PeptideIdentity) identities[k]).getPeptide().getIonScore();
								if (score > maxScore)
									maxScore = score;
							}
							if (maxScore < highScorePeptide.getIonScore())
								preffered = true;
						}
						else
							preffered = true;
						
						PeptideIdentity identity = new PeptideIdentity(highScorePeptide);
						peakList.getPeakRow(peaks[j]).addPeakIdentity(identity, preffered);
						flag = true;
						//logger.info("Scan number = "+ precursorScanNumber+" peptide " +highScorePeptide.toString());
						//logger.info("Matched with peak Scan number = "+ scanNumbers[i] + "from peak " + scanNumbers[0]+" - " + scanNumbers[scanNumbers.length-1]);
					}
				}
			}
		}
		
		return flag;
		
	}

	
	
	public Object[] getCreatedObjects() {
		return null;
	}
	
	
	
}

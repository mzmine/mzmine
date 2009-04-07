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

package net.sf.mzmine.modules.identification.pubchem;

import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceLocator;
import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceSoap;
import gov.nih.nlm.ncbi.www.soap.eutils.esearch.ESearchRequest;
import gov.nih.nlm.ncbi.www.soap.eutils.esearch.ESearchResult;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.modules.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.isotopes.isotopeprediction.FormulaAnalyzer;
import net.sf.mzmine.project.ProjectEvent;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class PubChemSearchTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	public static final NumberFormat massFormater = MZmineCore.getMZFormat();

	private static EUtilsServiceSoap eutils_soap;
	private TaskStatus status;
	private String errorMessage;
	private int finishedLines = 0, numItems;
	private double valueOfQuery, range;
	private int charge;
	private double ion;
	private int numOfResults;
	private PubChemSearchWindow window;
	private PeakList peakList;
	private IonizationType ionName;
	private boolean singleRow = false, chargedMol = false,
			isotopeFilter = false;
	private double isotopeScoreThreshold;
	private FormulaAnalyzer analyzer = new FormulaAnalyzer();
	private ChromatographicPeak peak;
	private PubChemSearchParameters parameters;

	/**
	 * 
	 * @param parameters
	 * @param peakList
	 * @param peakListRow
	 * @param peak
	 */
	PubChemSearchTask(PubChemSearchParameters parameters, PeakList peakList,
			PeakListRow peakListRow, ChromatographicPeak peak) {

		if ((peak != null) && (peakListRow != null)) {
			window = new PubChemSearchWindow(peakListRow, peak);
			singleRow = true;
		}

		this.peakList = peakList;
		this.peak = peak;
		this.parameters = parameters;

		status = TaskStatus.WAITING;
		valueOfQuery = (Double) parameters
				.getParameterValue(PubChemSearchParameters.neutralMass);
		range = (Double) parameters
				.getParameterValue(PubChemSearchParameters.mzToleranceField);
		numOfResults = (Integer) parameters
				.getParameterValue(PubChemSearchParameters.numOfResults);
		charge = (Integer) parameters
				.getParameterValue(PubChemSearchParameters.charge);
		chargedMol = (Boolean) parameters
				.getParameterValue(PubChemSearchParameters.chargedMol);
		isotopeFilter = (Boolean) parameters
				.getParameterValue(PubChemSearchParameters.isotopeFilter);

		if (isotopeFilter) {
			if (!(peak instanceof IsotopePattern))
				isotopeFilter = false;
		}

		isotopeScoreThreshold = (Double) parameters
				.getParameterValue(PubChemSearchParameters.isotopeScoreTolerance);
		ionName = (IonizationType) parameters
				.getParameterValue(PubChemSearchParameters.ionizationMethod);

		ion = ionName.getAddedMass();

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
		return ((double) finishedLines) / numItems;
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
		return "Peak identification of " + massFormater.format(valueOfQuery)
				+ " using PubChem Compound database";
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		status = TaskStatus.PROCESSING;

		try {

			// connect and read PubChem database contents
			EUtilsServiceLocator eutils_locator = new EUtilsServiceLocator();
			eutils_soap = eutils_locator.geteUtilsServiceSoap();

			// Set conditions of search
			ESearchRequest reqSearch = new ESearchRequest();
			reqSearch.setDb("pccompound");
			reqSearch.setRetMax(String.valueOf(numOfResults));
			reqSearch.setSort("CID(up)");

			ESearchResult resSearch;
			PubChemCompound compound;
			String pubChemID, complementQuery;
			int numIDs;

			if ((chargedMol)
					&& (ionName.equals(IonizationType.NO_IONIZATION)))
				complementQuery = " AND NOT 0[CHRG]";
			else
				complementQuery = "";

			// This task is performed for one single peak or a peak list?
			if (singleRow) {

				Desktop desktop = MZmineCore.getDesktop();
				desktop.addInternalFrame(window);

				reqSearch.setTerm(String.valueOf(valueOfQuery - range) + ":"
						+ String.valueOf(valueOfQuery + range)
						+ "[MonoisotopicMass]" + complementQuery);

				resSearch = eutils_soap.run_eSearch(reqSearch);

				// Get the number of results
				numIDs = resSearch.getIdList().length;
				numItems = numIDs;

				int i = 0;
				IsotopePattern ip2;

				// Process each one of the result ID's.
				while (i < numIDs) {

					if (status != TaskStatus.PROCESSING) {
						return;
					}

					pubChemID = resSearch.getIdList()[i];
					compound = new PubChemCompound(pubChemID, null, null, null,
							null);

					getSummary(compound, valueOfQuery);
					getName(pubChemID, compound);

					// Generate IsotopePattern to compare and set score
					if (isotopeFilter) {

						ip2 = analyzer.getIsotopePattern(compound
								.getCompoundFormula(), 0.01, charge, ionName
								.isPositiveCharge(), 0, true, true, ionName);

						double score = IsotopePatternScoreCalculator.getScore(
								 ip2, ((IsotopePattern) peak));

						compound.setIsotopePatterScore(String.valueOf(score));
						compound.setIsotopePattern(ip2);

						if (score >= isotopeScoreThreshold) {
							// Add compound to the list of possible candidate and
							// display it in window of results.
							window.addNewListItem(compound);
						}

					} else {
						// Add compound to the list of possible candidate and
						// display it in window of results.
						window.addNewListItem(compound);
					}

					ip2 = null;

					i++;
					finishedLines++;

					if (finishedLines >= numOfResults)
						break;
				}
			} else {

				PeakListRow[] peakListRows = peakList.getRows();
				numItems = peakListRows.length;

				for (PeakListRow row : peakListRows) {

					valueOfQuery = row.getAverageMZ();
					valueOfQuery *= charge;
					valueOfQuery -= ion;

					reqSearch.setTerm(String.valueOf(valueOfQuery - range)
							+ ":" + String.valueOf(valueOfQuery + range)
							+ complementQuery);
					resSearch = eutils_soap.run_eSearch(reqSearch);

					// Number of results
					numIDs = resSearch.getIdList().length;

					for (int i = 0; i < numIDs; i++) {
						pubChemID = resSearch.getIdList()[i];
						compound = new PubChemCompound(pubChemID, null, null,
								null, null);
						getSummary(compound, valueOfQuery);
						getName(pubChemID, compound);

						row.addPeakIdentity(compound, false);
					}
					finishedLines++;
				}
				
		        // Add task description to peakList
		        ((SimplePeakList)peakList).addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod("Peak identification of using PubChem databases",
		        		parameters));
                
		        // Notify the project manager that peaklist contents have changed
                MZmineCore.getProjectManager().fireProjectListeners(
                        ProjectEvent.PEAKLIST_CONTENTS_CHANGED);



			}

		} catch (Exception e) {
			logger.log(Level.WARNING, "Could not connect to PubChem ", e);
			status = TaskStatus.ERROR;
			errorMessage = e.toString();
			e.printStackTrace();
			return;
		}

		status = TaskStatus.FINISHED;

	}

	/**
	 * This method retrieve the SDF file of the compound from PubChem
	 * 
	 * @param compound
	 * @param mass
	 * @throws Exception
	 */
	private static void getSummary(PubChemCompound compound, double mass)
			throws Exception {

		URL url = new URL(
				"http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid="
						+ compound.getID() + "&disopt=DisplaySDF");

		InputStream in = url.openStream();

		if (in == null) {
			throw new Exception("Got a null content PubChem connection!");
		}

		BufferedReader is = new BufferedReader(new InputStreamReader(in,
				"UTF-8"));
		String responseLine, nextLine, structure = "";

		while ((responseLine = is.readLine()) != null) {

			structure += responseLine + "\n";

			if (responseLine.matches(".*PUBCHEM_IUPAC_TRADITIONAL_NAME.*")) {
				nextLine = is.readLine();
				compound.setCompoundName(nextLine);
				structure += nextLine + "\n";
				continue;
			}

			if (responseLine.matches(".*PUBCHEM_MOLECULAR_FORMULA.*")) {
				nextLine = is.readLine();
				compound.setCompoundFormula(nextLine);
				structure += nextLine + "\n";
				continue;
			}

			if (responseLine.matches(".*PUBCHEM_MONOISOTOPIC_WEIGHT.*")) {
				nextLine = is.readLine();
				double massDiff = mass - Double.parseDouble(nextLine);
				massDiff = Math.abs(massDiff);
				compound.setExactMassDifference(String.valueOf(massDiff));
				structure += nextLine + "\n";
				continue;
			}

		}

		compound.setStructure(structure);
		is.close();

	}

	/**
	 * This method exists due a lack of information in SDF file (missing name)
	 * from PubChem
	 * 
	 * @param id
	 * @param compound
	 */
	private static void getName(String id, PubChemCompound compound) {
		try {

			URL endpoint = new URL(
					"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pccompound&id="
							+ id + "&report=brief&mode=text");

			InputStream is = endpoint.openStream();
			if (is == null) {
				throw new Exception(
						"Got a null PubChem input stream connection");
			}
			StringBuffer putBackTogether = new StringBuffer();
			Reader reader = new InputStreamReader(is, "UTF-8");
			char[] cb = new char[1024];

			int amtRead = reader.read(cb);
			while (amtRead > 0) {
				putBackTogether.append(cb, 0, amtRead);
				amtRead = reader.read(cb);
			}
			String name = putBackTogether.toString();
			int index = name.indexOf(id, 0);
			name = name.substring(index + id.length());

			compound.setCompoundName(name);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

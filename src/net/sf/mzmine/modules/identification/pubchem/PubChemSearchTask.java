/*
 * Copyright 2006-2008 The MZmine Development Team
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
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.isotopes.isotopeprediction.FormulaAnalyzer;
import net.sf.mzmine.taskcontrol.Task;

public class PubChemSearchTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

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
	private TypeOfIonization ionName;
	private boolean singleRow = false, chargedMol = false,
			isotopeFilter = false;
	private double isotopeScoreThreshold;
	private FormulaAnalyzer analyzer = new FormulaAnalyzer();
	private ChromatographicPeak peak;

	PubChemSearchTask(PubChemSearchParameters parameters, PeakList peakList,
			PeakListRow peakListRow, ChromatographicPeak peak) {

		if ((peak != null) && (peakListRow != null)) {
			window = new PubChemSearchWindow(peakListRow, peak);
			singleRow = true;
		}

		this.peakList = peakList;
		this.peak = peak;

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
		ionName = (TypeOfIonization) parameters
				.getParameterValue(PubChemSearchParameters.ionizationMethod);

		ion = ionName.getMass();
		ion *= ionName.getSign();

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
		return "Peak identification of " + valueOfQuery
				+ " using PubChem databases ";
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

			ESearchRequest reqSearch = new ESearchRequest();
			reqSearch.setDb("pccompound");
			reqSearch.setRetMax(String.valueOf(numOfResults * 5));
			reqSearch.setSort("CID(up)");

			ESearchResult resSearch;
			PubChemCompound compound;
			String pubChemID, complementQuery;
			int numIDs;

			if ((chargedMol)
					&& (ionName.equals(TypeOfIonization.NO_IONIZATION)))
				complementQuery = " AND NOT 0[CHRG]";
			else
				complementQuery = "";

			if (singleRow) {
				
				Desktop desktop = MZmineCore.getDesktop();
				desktop.addInternalFrame(window);

				reqSearch.setTerm(String.valueOf(valueOfQuery - range) + ":"
						+ String.valueOf(valueOfQuery + range)
						+ "[MonoisotopicMass]" + complementQuery);
				
				resSearch = eutils_soap.run_eSearch(reqSearch);

				// results output
				numIDs = resSearch.getIdList().length;
				numItems = numOfResults;

				int i = 0;
				boolean goodCandidate = false;
				IsotopePattern ip2;

				while (i < numIDs) {

					if (status != TaskStatus.PROCESSING) {
						return;
					}

					pubChemID = resSearch.getIdList()[i];
					compound = new PubChemCompound(pubChemID, null, null, null,
							null, "PubChem", null);
					
					getSummary(compound, valueOfQuery );
					getName(pubChemID, compound);

					if (isotopeFilter) {

						ip2 = analyzer.getIsotopePattern(compound
								.getCompoundFormula(), 0.01, charge, ionName
								.isPositiveCharge(), 0, true, true, ionName);

						double score = IsotopePatternScoreCalculator.getScore(
								((IsotopePattern) peak), ip2);

						compound.setIsotopePatterScore(String.valueOf(score));
						compound.setIsotopePattern(ip2);

						if (score >= isotopeScoreThreshold) {
							goodCandidate = true;
						}

					} else {
						goodCandidate = true;
					}

					ip2 = null;

					// Add compound to the list of possible candidate and
					// display it in window of results.
					if (goodCandidate) {

						window.addNewListItem(compound);
						finishedLines++;

					}

					i++;
					goodCandidate = false;

					if (finishedLines >= numOfResults)
						break;
				}
			} 
			else {

				PeakListRow[] peakListRows = peakList.getRows();
				numItems = peakListRows.length;

				for (PeakListRow row : peakListRows) {

					valueOfQuery = row.getAverageMZ();
					valueOfQuery /= charge;
					valueOfQuery += ion;

					reqSearch.setTerm(String.valueOf(valueOfQuery - range)
							+ ":" + String.valueOf(valueOfQuery + range)
							+ complementQuery);
					resSearch = eutils_soap.run_eSearch(reqSearch);

					// results output
					numIDs = resSearch.getIdList().length;

					for (int i = 0; i < numIDs; i++) {
						pubChemID = resSearch.getIdList()[i];
						compound = new PubChemCompound(pubChemID, null, null,
								null, null, "PubChem", null);
						getSummary(compound, valueOfQuery);
						getName(pubChemID, compound);

						row.addCompoundIdentity(compound);
					}
					finishedLines++;
				}

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

	private static void getSummary(PubChemCompound compound, double mass)
			throws Exception {

		URL url = new URL(
				"http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid="
						+ compound.getCompoundID() + "&disopt=DisplaySDF");

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

	private static void getName(String id, PubChemCompound compound) {
		try {

			URL endpoint = new URL(
					"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pccompound&id="
							+ id + "&report=brief&mode=text");
			
			InputStream is = endpoint.openStream();
			if (is == null) {
				throw new Exception("Got a null PubChem input stream connection");
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

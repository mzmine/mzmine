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
import gov.nih.nlm.ncbi.www.soap.eutils.elink.ELinkRequest;
import gov.nih.nlm.ncbi.www.soap.eutils.elink.ELinkResult;
import gov.nih.nlm.ncbi.www.soap.eutils.elink.LinkSetDbType;
import gov.nih.nlm.ncbi.www.soap.eutils.esearch.ESearchRequest;
import gov.nih.nlm.ncbi.www.soap.eutils.esearch.ESearchResult;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ESummaryRequest;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ESummaryResult;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimpleCompoundIdentity;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
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
    private boolean singleRow = false, chargedMol = false;

    PubChemSearchTask(PubChemSearchParameters parameters, PeakList peakList,
            PeakListRow peakListRow) {

        if (peakListRow != null) {
            window = new PubChemSearchWindow(peakListRow);
            singleRow = true;
        }

        this.peakList = peakList;

        status = TaskStatus.WAITING;
        valueOfQuery = (Double) parameters.getParameterValue(PubChemSearchParameters.neutralMass);
        range = (Double) parameters.getParameterValue(PubChemSearchParameters.mzToleranceField);
        numOfResults = (Integer) parameters.getParameterValue(PubChemSearchParameters.numOfResults);
        charge = (Integer) parameters.getParameterValue(PubChemSearchParameters.charge);
        chargedMol = (Boolean) parameters.getParameterValue(PubChemSearchParameters.chargedMol);

        ionName = (TypeOfIonization) parameters.getParameterValue(PubChemSearchParameters.ionizationMethod);

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
            reqSearch.setRetMax(String.valueOf(numOfResults));
            reqSearch.setSort("CID(up)");

            ESearchResult resSearch;
            SimpleCompoundIdentity compound;
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
                numItems = numIDs;

                for (int i = 0; i < numIDs; i++) {
                    pubChemID = resSearch.getIdList()[i];
                    compound = new SimpleCompoundIdentity(pubChemID, null,
                            null, null, null, "PubChem", null);
                    getSummary("pccompound", pubChemID, compound);
                    getLink(pubChemID, compound);
                    getName(pubChemID, compound);

                    window.addNewListItem(compound);
                    finishedLines++;
                }
            } else {

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
                        compound = new SimpleCompoundIdentity(pubChemID, null,
                                null, null, null, "PubChem", null);
                        getSummary("pccompound", pubChemID, compound);
                        getLink(pubChemID, compound);
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

    public void getLink(String id, SimpleCompoundIdentity compound)
            throws RemoteException {

        ELinkRequest reqLink = new ELinkRequest();
        reqLink.setDb("mesh");
        reqLink.setDbfrom("pccompound");
        reqLink.setId(new String[] { id });
        ELinkResult resLink = eutils_soap.run_eLink(reqLink);

        // results output
        for (int i = 0; i < resLink.getLinkSet().length; i++) {
            LinkSetDbType[] list = resLink.getLinkSet()[i].getLinkSetDb();
            if (list != null)
                if (list.length > 0) {
                    for (int k = 0; k < list[0].getLink().length; k++) {
                        String s = list[0].getLink()[k].getId().toString();
                        getSummary("mesh", s, compound);
                    }
                }
        }

    }

    public void getSummary(String db, String id, SimpleCompoundIdentity compound)
            throws RemoteException {

        ESummaryRequest reqSummary = new ESummaryRequest();
        reqSummary.setDb(db);
        reqSummary.setId(id);
        ESummaryResult resSum = eutils_soap.run_eSummary(reqSummary);
        String formula = "MolecularFormula";
        Vector<String> names = new Vector<String>();
        String itemName = null, itemContent = null;

        // results output

        for (int i = 0; i < resSum.getDocSum().length; i++) {
            for (int k = 0; k < resSum.getDocSum()[i].getItem().length; k++) {
                itemName = resSum.getDocSum()[i].getItem()[k].getName();
                itemContent = resSum.getDocSum()[i].getItem()[k].getItemContent();
                // logger.finest(" " + itemName + ": " + itemContent);
                if (itemName.matches(".*ScopeNote.*"))
                    compound.setScopeNote(itemContent);
                if (itemName.matches(".*IUPACName.*"))
                    compound.setCompoundName(itemContent);
                if (itemName.matches(formula + ".*"))
                    compound.setCompoundFormula(itemContent);
                if (itemName.matches(".*Name.*"))
                    names.add(itemContent);
            }
            String[] alternateNames = names.toArray(new String[0]);
            compound.setAlternateNames(alternateNames);
        }

    }

    private void getName(String id, SimpleCompoundIdentity compound) {
        try {

            URL endpoint = new URL(
                    "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pccompound&id="
                            + id + "&report=brief&mode=text");
            URLConnection uc = endpoint.openConnection();
            if (uc == null) {
                throw new Exception("Got a null URLConnection object!");
            }
            InputStream is = uc.getInputStream();
            if (is == null) {
                throw new Exception("Got a null content object!");
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
            name = name.substring(index + 4);

            compound.setCompoundName(name);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

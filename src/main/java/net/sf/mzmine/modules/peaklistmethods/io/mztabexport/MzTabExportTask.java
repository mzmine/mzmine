/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.io.mztabexport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import uk.ac.ebi.pride.jmztab.model.Assay;
import uk.ac.ebi.pride.jmztab.model.CVParam;
import uk.ac.ebi.pride.jmztab.model.MZTabColumnFactory;
import uk.ac.ebi.pride.jmztab.model.MZTabDescription;
import uk.ac.ebi.pride.jmztab.model.Metadata;
import uk.ac.ebi.pride.jmztab.model.MsRun;
import uk.ac.ebi.pride.jmztab.model.Section;
import uk.ac.ebi.pride.jmztab.model.SmallMolecule;
import uk.ac.ebi.pride.jmztab.model.SmallMoleculeColumn;
import uk.ac.ebi.pride.jmztab.model.StudyVariable;

class MzTabExportTask extends AbstractTask {

    private int processedRows = 0, totalRows = 0;

    // parameter values
    private final MZmineProject project;
    private final File fileName;
    private final PeakList peakList;
    private final boolean exportall;

    MzTabExportTask(MZmineProject project, ParameterSet parameters) {
        this.project = project;
        this.peakList = parameters.getParameter(MzTabExportParameters.peakList)
                .getValue().getMatchingPeakLists()[0];
        this.fileName = parameters.getParameter(MzTabExportParameters.filename)
                .getValue();
        this.exportall = parameters.getParameter(
                MzTabExportParameters.exportall).getValue();
    }

    public double getFinishedPercentage() {
        if (totalRows == 0) {
            return 0;
        }
        return (double) processedRows / (double) totalRows;
    }

    public String getTaskDescription() {
        return "Exporting peak list " + peakList + " to " + fileName;
    }

    public void run() {

        setStatus(TaskStatus.PROCESSING);

        try {

            // Get number of rows
            totalRows = peakList.getNumberOfRows();

            // Metadata
            Metadata mtd = new Metadata();
            mtd.setMZTabMode(MZTabDescription.Mode.Summary);
            mtd.setMZTabType(MZTabDescription.Type.Quantification);
            mtd.setDescription(peakList.getName());
            mtd.addSoftwareParam(1, new CVParam("MS", "MS:1002342", "MZmine",
                    MZmineCore.getMZmineVersion()));
            mtd.setSmallMoleculeQuantificationUnit(new CVParam("PRIDE",
                    "PRIDE:0000330", "Arbitrary quantification unit", null));
            mtd.addSmallMoleculeSearchEngineScoreParam(1, new CVParam("MS",
                    "MS:1001153", "search engine specific score", null));

            // Create stable columns
            MZTabColumnFactory factory = MZTabColumnFactory
                    .getInstance(Section.Small_Molecule);

            // Variable descriptions
            int parameterCounter = 0;
            for (UserParameter<?, ?> p : project.getParameters()) {
                for (Object e : ((ComboParameter<?>) p).getChoices()) {
                    parameterCounter++;
                    mtd.addStudyVariableDescription(parameterCounter,
                            String.valueOf(p) + ": " + String.valueOf(e));
                    StudyVariable studyVariable = new StudyVariable(
                            parameterCounter);
                    factory.addAbundanceOptionalColumn(studyVariable);
                }
            }

            final RawDataFile rawDataFiles[] = peakList.getRawDataFiles();
            int fileCounter = 0;
            for (RawDataFile file : rawDataFiles) {
                fileCounter++;

                /**
                 * TO DO: Add path to original imported raw file to MZmine and
                 * write it out here instead
                 * */
                // MS run location
                MsRun msRun = new MsRun(fileCounter);
                msRun.setLocation(new URL("file:///" + file.getName()));
                mtd.addMsRun(msRun);
                mtd.addAssayMsRun(fileCounter, msRun);

                // Add samples to study variable assay
                for (UserParameter<?, ?> p : project.getParameters()) {
                    Assay assay = mtd.getAssayMap().get(fileCounter);
                    for (StudyVariable studyVariable : mtd
                            .getStudyVariableMap().values()) {
                        if (studyVariable.getDescription().equals(
                                String.valueOf(p)
                                        + ": "
                                        + String.valueOf(project
                                                .getParameterValue(p, file)))) {
                            mtd.addStudyVariableAssay(studyVariable.getId(),
                                    assay);
                        }
                    }
                }

                // Additional columns
                factory.addBestSearchEngineScoreOptionalColumn(
                        SmallMoleculeColumn.BEST_SEARCH_ENGINE_SCORE, 1);
                factory.addOptionalColumn(new Assay(fileCounter), "peak_mz",
                        String.class);
                factory.addOptionalColumn(new Assay(fileCounter), "peak_rt",
                        String.class);
                factory.addOptionalColumn(new Assay(fileCounter),
                        "peak_height", String.class);
                factory.addURIOptionalColumn();
                factory.addAbundanceOptionalColumn(new Assay(fileCounter));
            }

            // Write to file
            FileWriter writer = new FileWriter(fileName);
            BufferedWriter out = new BufferedWriter(writer);
            out.write(mtd.toString());
            out.write("\n");
            out.write(factory.toString());
            out.write("\n");

            // Write data rows
            for (PeakListRow peakListRow : peakList.getRows()) {

                // Cancel?
                if (isCanceled()) {
                    return;
                }

                PeakIdentity peakIdentity = peakListRow
                        .getPreferredPeakIdentity();
                if (exportall || peakIdentity != null) {
                    SmallMolecule sm = new SmallMolecule(factory, mtd);
                    if (peakIdentity != null) {
                        // Identity information
                        String identifier = escapeString(peakIdentity
                                .getPropertyValue("ID"));
                        String database = peakIdentity
                                .getPropertyValue("Identification method");
                        String formula = peakIdentity
                                .getPropertyValue("Molecular formula");
                        String description = escapeString(peakIdentity
                                .getPropertyValue("Name"));
                        String url = peakIdentity.getPropertyValue("URL");

                        if (identifier != null) {
                            sm.setIdentifier(identifier);
                        }
                        if (database != null) {
                            sm.setDatabase(database);
                        }
                        if (formula != null) {
                            sm.setChemicalFormula(formula);
                        }
                        if (description != null) {
                            sm.setDescription(description);
                        }
                        if (url != null) {
                            sm.setURI(url);
                        }
                    }

                    Double rowMZ = peakListRow.getAverageMZ();
                    int rowCharge = peakListRow.getRowCharge();
                    String rowRT = String.valueOf(peakListRow.getAverageRT());

                    if (rowMZ != null) {
                        sm.setExpMassToCharge(rowMZ);
                    }
                    if (rowCharge > 0) {
                        sm.setCharge(rowCharge);
                    }
                    if (rowRT != null) {
                        sm.setRetentionTime(rowRT);
                    }

                    int dataFileCount = 0;
                    for (RawDataFile dataFile : rawDataFiles) {
                        dataFileCount++;
                        Feature peak = peakListRow.getPeak(dataFile);
                        if (peak != null) {
                            String peakMZ = String.valueOf(peak.getMZ());
                            String peakRT = String.valueOf(String.valueOf(peak
                                    .getRT()));
                            String peakHeight = String
                                    .valueOf(peak.getHeight());
                            Double peakArea = peak.getArea();

                            sm.setOptionColumnValue(new Assay(dataFileCount),
                                    "peak_mz", peakMZ);
                            sm.setOptionColumnValue(new Assay(dataFileCount),
                                    "peak_rt", peakRT);
                            sm.setOptionColumnValue(new Assay(dataFileCount),
                                    "peak_height", peakHeight);
                            sm.setAbundanceColumnValue(
                                    new Assay(dataFileCount), peakArea);
                        }
                    }

                    out.write(sm.toString());
                    out.write("\n");
                }

            }

            out.flush();
            out.close();
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Could not export peak list to file " + fileName
                    + ": " + e.getMessage());
            return;
        }

        if (getStatus() == TaskStatus.PROCESSING)
            setStatus(TaskStatus.FINISHED);

    }

    private String escapeString(final String inputString) {

        if (inputString == null)
            return "";

        // Remove all special characters e.g. \n \t
        return inputString.replaceAll("[\\p{Cntrl}]", " ");
    }

}

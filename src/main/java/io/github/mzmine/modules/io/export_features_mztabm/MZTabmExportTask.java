/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package io.github.mzmine.modules.io.export_features_mztabm;

import de.isas.mztab2.io.MzTabNonValidatingWriter;
import de.isas.mztab2.io.MzTabValidatingWriter;
import de.isas.mztab2.model.*;
import io.github.mzmine.datamodel.*;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;
import uk.ac.ebi.pride.jmztab2.model.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MZTabmExportTask extends AbstractTask {

    private int processedRows = 0, totalRows = 0;

    // parameter values
    private final MZmineProject project;
    private final File fileName;
    private String plNamePattern = "{}";
    private FeatureList[] featureLists;
    private final boolean exportAll;
    private final boolean validateOnWrite;

    MZTabmExportTask(MZmineProject project, ParameterSet parameters, @NotNull Instant moduleCallDate) {
        super(null, moduleCallDate); // no new data stored -> null
        this.project = project;
        this.featureLists
                = parameters.getParameter(MZTabmExportParameters.featureLists).getValue().getMatchingFeatureLists();
        this.fileName = parameters.getParameter(MZTabmExportParameters.filename).getValue();
        this.exportAll = parameters.getParameter(MZTabmExportParameters.exportAll).getValue();
        this.validateOnWrite = parameters.getParameter(MZTabmExportParameters.validateOnWrite).getValue();
    }

    @Override
    public double getFinishedPercentage() {
        if (totalRows == 0) {
            return 0;
        }
        return (double) processedRows / (double) totalRows;
    }

    @Override
    public String getTaskDescription() {
        return "Exporting feature list(s) " + Arrays.toString(featureLists) + " to MzTab-m file(s)";
    }

    @Override
    public void run() {
        setStatus(TaskStatus.PROCESSING);

        boolean substitute = fileName.getPath().contains(plNamePattern);

        // Total Number of rows
        for (FeatureList featureList : featureLists) {
            totalRows += featureList.getNumberOfRows();
        }

        //Process feature Lists
        for (FeatureList featureList : featureLists) {
            File curFile = fileName;
            try {
                //Filename
                if (substitute) {
                    // Cleanup from illegal filename characters
                    //not small alphabets, large alphabets, numbers, dots or dashes
                    String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
                    // Substitute
                    String newFilename
                            = fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
                    curFile = new File(newFilename);
                }

                MzTab mzTabFile = new MzTab();

                //Metadata
                Metadata mtd = new Metadata();
                mtd.setMzTabVersion("2.0.0-M");
                mtd.setMzTabID("1");
                mtd.setDescription(featureList.getName());
                mtd.addSoftwareItem(new Software().id(1).parameter(
                        new Parameter().cvLabel("MS").cvAccession("MS:1002342").name("MZmine")
                                .value(String.valueOf(MZmineCore.getMZmineVersion()))));
                mtd.setSmallMoleculeQuantificationUnit(
                        new Parameter().cvLabel("PRIDE").cvAccession("PRIDE:0000330")
                                .name("Arbitrary quantification unit"));
                mtd.setSmallMoleculeFeatureQuantificationUnit(
                        new Parameter().cvLabel("PRIDE").cvAccession("PRIDE:0000330")
                                .name("Arbitrary quantification unit"));
                mtd.addIdConfidenceMeasureItem(new Parameter().id(1).cvLabel("MS").cvAccession("MS:1001153")
                        .name("search engine specific score"));
                mtd.setSmallMoleculeIdentificationReliability(
                        new Parameter().cvLabel("MS").cvAccession("MS:1002896")
                                .name("compound identification confidence level"));
                mtd.setQuantificationMethod(new Parameter().cvLabel("MS").cvAccession("MS:1001834")
                        .name("LC-MS label-free quantification analysis"));
                mtd.addCvItem(new CV().id(1).label("MS").fullName("PSI-MS controlled vocabulary").
                        version("4.0.9").
                        uri("https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/master/psi-ms.obo"));

                List<IOptColumnMappingBuilder> feature_mzList = new ArrayList<>();
                List<IOptColumnMappingBuilder> feature_rtList = new ArrayList<>();
                List<IOptColumnMappingBuilder> feature_heightList = new ArrayList<>();

                final RawDataFile rawDataFiles[] = featureList.getRawDataFiles().toArray(RawDataFile[]::new);
                int fileCounter = 0;
                // Study Variable name and descriptions
                HashMap<String, List<RawDataFile>> svhash = new HashMap<>();
                HashMap<RawDataFile, Assay> rawDataFileToAssay = new HashMap<>();

                for (RawDataFile file : rawDataFiles) {
                    fileCounter++;
                    // MS run location
                    MsRun msRun = new MsRun();
                    msRun.id(fileCounter);
                    msRun.setLocation("file://" + file.getName());
                    int dotIn = file.getName().indexOf(".");
                    String fileFormat = "";
                    if (dotIn != -1) {
                        fileFormat = file.getName().substring(dotIn + 1);
                    }
                    msRun.setFormat(new Parameter().cvLabel("MS").cvAccession("MS:1000584").name(fileFormat + " file"));
                    msRun.setIdFormat(new Parameter().cvLabel("MS").cvAccession("MS:1000774").name("multiple peak list nativeID format"));

                    List<Parameter> polPara = new ArrayList<>();
                    for (PolarityType scanPol : file.getDataPolarity()) {
                        Integer pol = scanPol.getSign();
                        String polarity = "";
                        String polCVA = "";
                        switch (pol) {
                            case 1 -> {
                                polarity = "positive scan";
                                polCVA = "MS:1000130";
                            }
                            case -1 -> {
                                polarity = "negative scan";
                                polCVA = "MS:1000129";
                            }
                            default -> {
                                setStatus(TaskStatus.ERROR);
                                setErrorMessage("Invalid scan polarity " + pol + " encountered for file "
                                        + file.getName() + ".");
                                return;
                            }
                        }
                        Parameter p = new Parameter().cvLabel("MS").cvAccession(polCVA).name(polarity);
                        polPara.add(p);
                    }

                    msRun.setScanPolarity(polPara);
                    mtd.addMsRunItem(msRun);
                    // Add Assay
                    Assay assay = new Assay();
                    rawDataFileToAssay.put(file, assay);
                    assay.id(fileCounter);
                    assay.addMsRunRefItem(msRun);
                    mtd.addAssayItem(assay);

                    initializeStudyVariableHashMap(file, svhash);

                    //Optional Columns
                    // TODO: rename "peak_mz" to "feature_mz" in both import and export and test if they work
                    feature_mzList.add(OptColumnMappingBuilder.forIndexedElement(assay).withName("peak_mz"));
                    feature_rtList.add(OptColumnMappingBuilder.forIndexedElement(assay).withName("peak_rt"));
                    feature_heightList
                            .add(OptColumnMappingBuilder.forIndexedElement(assay).withName("peak_height"));
                }
                int studyVarCount = 0;
                for (String key : svhash.keySet()) {
                    studyVarCount++;
                    StudyVariable studyVariable = new StudyVariable().id(studyVarCount).name(key)
                            .description(key).
                            averageFunction(
                                    new Parameter().cvLabel("MS").cvAccession("MS:1002883").name("mean"));
                    for (RawDataFile file : svhash.get(key)) {
                        studyVariable = studyVariable.addAssayRefsItem(rawDataFileToAssay.get(file));
                    }
                    mtd.addStudyVariableItem(studyVariable);
                }
                // add default mandatory "undefined" study variable if nothing else was defined in svhash
                if (svhash.isEmpty()) {
                    StudyVariable studyVariable = new StudyVariable().id(1).name("undefined");
                    studyVariable = initializeStudyVariableHashMap(rawDataFiles, studyVariable, rawDataFileToAssay, svhash);
                    mtd.addStudyVariableItem(studyVariable);
                }

                //Write data rows
                Map<Parameter, Database> databases = new LinkedHashMap<>();

                for (int i = 0; i < featureList.getRows().size(); ++i) {
                    FeatureListRow featureListRow = featureList.getRows().get(i);
                    SmallMoleculeSummary sm = new SmallMoleculeSummary();
                    sm.setSmlId(i + 1);
                    SmallMoleculeFeature smf = new SmallMoleculeFeature();
                    smf.setSmfId(i + 1);
                    SmallMoleculeEvidence sme = new SmallMoleculeEvidence();
                    sme.setSmeId(i + 1);

                    sme.setMsLevel(new Parameter().cvLabel("MS").cvAccession("MS:1000511").name("ms level").value("1"));
                    sme.setEvidenceInputId(String.valueOf(i + 1));
                    List<Double> confidences = new ArrayList<>();
                    confidences.add(0.0);
                    sme.setIdConfidenceMeasure(confidences);

                    //Cancelled?
                    if (isCanceled()) {
                        return;
                    }
                    sm.setReliability("2");
                    FeatureIdentity featureIdentity = featureListRow.getPreferredFeatureIdentity();
                    List<IonIdentity> ionIdentities = featureListRow.getIonIdentities();
                    // set default identification method
                    sme.setIdentificationMethod(new Parameter().name("Unknown"));
                    if (exportAll) {
                        // support ion identity output
                        if (ionIdentities != null && !ionIdentities.isEmpty()) {
                            throw new UnsupportedOperationException("Export of IonIdentityNetworking results to mzTab-M is currently not supported!");
                        }
                        // support regular id output under the "manual" category
                        if (featureIdentity != null) {
                            handleFeatureIdentity(featureIdentity, databases, sm, sme);
                        }

                        Double rowMZ = featureListRow.getAverageMZ();
                        int rowCharge = 0;
                        Float rowRT = featureListRow.getAverageRT();

                        if (rowMZ != null) {
                            smf.setExpMassToCharge(rowMZ);
                            sme.setExpMassToCharge(rowMZ);
                            //FIXME replace experimental by theoretical value from id method or database
                            sme.setTheoreticalMassToCharge(rowMZ);
                        }
                        if (rowRT != null) {
                            smf.setRetentionTimeInSeconds(Double.valueOf(rowRT));
                        }
                        HashMap<String, List<Double>> sampleVariableAbundancehash = new HashMap<>();
                        handleFeatureAndAssayAbundances(
                                rawDataFiles,
                                featureListRow,
                                rawDataFileToAssay,
                                sme,
                                rowCharge,
                                sm,
                                feature_mzList,
                                feature_rtList,
                                feature_heightList,
                                smf,
                                svhash,
                                sampleVariableAbundancehash
                        );
                        handleSummaryStudyVariableAbundances(
                                sampleVariableAbundancehash,
                                sm
                        );
                    }

                    sm.addSmfIdRefsItem(smf.getSmfId());
                    smf.addSmeIdRefsItem(sme.getSmeId());
                    mzTabFile.addSmallMoleculeSummaryItem(sm);
                    mzTabFile.addSmallMoleculeFeatureItem(smf);
                    mzTabFile.addSmallMoleculeEvidenceItem(sme);
                }
                //cv term
                int dbId = 1;
                //set ids sequentially, starting from 1
                if (databases.isEmpty()) {
                    // default none database to signify use cases with no identification
                    Parameter noDatabase = new Parameter().name("no database").value(null);
                    Database db = new Database().id(dbId).param(noDatabase).prefix(null).uri(null).version("Unknown");
                    mtd.addDatabaseItem(db);
                } else {
                    for (Map.Entry<Parameter, Database> entry : databases.entrySet()) {
                        mtd.addDatabaseItem(entry.getValue().id(dbId++));
                    }
                }

                mzTabFile.metadata(mtd);
                if (validateOnWrite) {
                    MzTabValidatingWriter validatingWriter = new MzTabValidatingWriter();
                    Optional<List<ValidationMessage>> validationResult = validatingWriter.write(curFile.toPath(), mzTabFile);
                    if (validationResult.isPresent()) {
                        if (!validationResult.get().isEmpty()) {
                            String message = validationResult.get().stream().map((msg)
                                    -> msg.getMessage()
                            ).collect(Collectors.joining("|"));
                            setStatus(TaskStatus.ERROR);
                            setErrorMessage("Could not export feature list to file " + curFile + ": " + message);
                        }
                    }
                } else {
                    MzTabNonValidatingWriter writer = new MzTabNonValidatingWriter();
                    writer.write(curFile.toPath(), mzTabFile);
                }
            } catch (IOException | UnsupportedOperationException e) {
                e.printStackTrace();
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Could not export feature list to file " + curFile + ": " + e.getMessage());
                return;
            }
        }
        if (getStatus() == TaskStatus.PROCESSING) {
            setStatus(TaskStatus.FINISHED);
        }
    }

    private void initializeStudyVariableHashMap(final RawDataFile file, HashMap<String, List<RawDataFile>> svhash) {
        for (UserParameter<?, ?> p : project.getParameters()) {
            if (p.getName().contains("study variable")) {
                if (svhash.containsKey(String.valueOf(project.getParameterValue(p, file)))) {
                    svhash.get(String.valueOf(project.getParameterValue(p, file))).add(file);
                } else {
                    List<RawDataFile> l = new ArrayList<>();
                    l.add(file);
                    svhash.put(String.valueOf(project.getParameterValue(p, file)), l);
                }
                break;
            }
        }
    }
    
    private StudyVariable initializeStudyVariableHashMap(final RawDataFile[] rawDataFiles, StudyVariable studyVariable, HashMap<RawDataFile, Assay> rawDataFileToAssay, HashMap<String, List<RawDataFile>> svhash) {
        for (RawDataFile file : rawDataFiles) {
            studyVariable = studyVariable.addAssayRefsItem(rawDataFileToAssay.get(file));
            initializeStudyVariableHashMap(file, svhash);
        }
        return studyVariable;
    }

    private void handleSummaryStudyVariableAbundances(HashMap<String, List<Double>> sampleVariableAbundancehash, SmallMoleculeSummary sm) {
        for (String studyVariable : sampleVariableAbundancehash.keySet()) {
            Double averageSV = 0.0;
            //Using mean as average function for abundance of Study Variable
            for (Double d : sampleVariableAbundancehash.get(studyVariable)) {
                averageSV += d;
            }
            int totalSV = sampleVariableAbundancehash.get(studyVariable).size();
            if (totalSV == 0) {
                averageSV = 0.0;
            } else {
                averageSV /= totalSV;
            }
            //Coefficient of variation
            Double covSV = 0.0;
            for (Double d : sampleVariableAbundancehash.get(studyVariable)) {
                covSV += (d - averageSV) * (d - averageSV);
            }
            if (totalSV == 0 || totalSV == 1) {
                covSV = 0.0;
            } else {
                covSV /= (totalSV - 1);
                covSV = Math.sqrt(covSV);
                if (averageSV != 0.0) {
                    covSV = (covSV / averageSV) * 100.0;
                }
            }
            sm.addAbundanceStudyVariableItem(averageSV);
            sm.addAbundanceVariationStudyVariableItem(covSV);
        }
    }

    private void handleFeatureAndAssayAbundances(final RawDataFile[] rawDataFiles, FeatureListRow featureListRow, HashMap<RawDataFile, Assay> rawDataFileToAssay, SmallMoleculeEvidence sme, int rowCharge, SmallMoleculeSummary sm, List<IOptColumnMappingBuilder> feature_mzList, List<IOptColumnMappingBuilder> feature_rtList, List<IOptColumnMappingBuilder> feature_heightList, SmallMoleculeFeature smf, HashMap<String, List<RawDataFile>> svhash, HashMap<String, List<Double>> sampleVariableAbundancehash) {
        for (int dataFileCount = 0; dataFileCount < rawDataFiles.length; dataFileCount++) {
            RawDataFile dataFile = rawDataFiles[dataFileCount];
            Feature feature = featureListRow.getFeature(dataFile);
            if (feature != null) {
                //Spectra ref
                List<SpectraRef> sr = new ArrayList<>();
                for (Scan scan : feature.getScanNumbers()) {
                    sr.add(new SpectraRef().msRun(rawDataFileToAssay.get(feature.getRawDataFile()).
                            getMsRunRef().get(0)).reference("index=" + scan.getScanNumber()));
                }
                if (sr.isEmpty()) {
                    sr.add(new SpectraRef().msRun(rawDataFileToAssay.get(feature.getRawDataFile()).
                            getMsRunRef().get(0)).reference("index=0"));
                }
                sme.setSpectraRef(sr);
                rowCharge = feature.getCharge();

                String featureMZ = String.valueOf(feature.getMZ());
                String featureRT = String.valueOf(feature.getRT());
                String featureHeight = String.valueOf(feature.getHeight());
                Double featureArea = (double) feature.getArea();
                sm.addOptItem(feature_mzList.get(dataFileCount).build(featureMZ));
                sm.addOptItem(feature_rtList.get(dataFileCount).build(featureRT));
                sm.addOptItem(feature_heightList.get(dataFileCount).build(featureHeight));
                sm.addAbundanceAssayItem(featureArea);
                smf.addAbundanceAssayItem(featureArea);
                for (String sampleVariable : svhash.keySet()) {
                    if (svhash.get(sampleVariable).contains(dataFile)) {
                        if (sampleVariableAbundancehash.containsKey(sampleVariable)) {
                            sampleVariableAbundancehash.get(sampleVariable).add(featureArea);
                        } else {
                            List<Double> l = new ArrayList<>();
                            l.add(featureArea);
                            sampleVariableAbundancehash.put(sampleVariable, l);
                        }
                    }
                }
            }
        }
        if (rowCharge > 0) {
            smf.setCharge(rowCharge);
            sme.setCharge(rowCharge);
        }
    }

    private void handleFeatureIdentity(FeatureIdentity featureIdentity, Map<Parameter, Database> databases, SmallMoleculeSummary sm, SmallMoleculeEvidence sme) {
        boolean shouldAdd = true;
        Parameter dbParam = new Parameter().
                name(featureIdentity.getPropertyValue(FeatureIdentity.PROPERTY_METHOD));
        String dbURI = (featureIdentity.getPropertyValue(FeatureIdentity.PROPERTY_URL) == null
                || featureIdentity.getPropertyValue(FeatureIdentity.PROPERTY_URL).equals(""))
                ? "mzmine://" + featureIdentity.getClass().getSimpleName()
                : featureIdentity.getPropertyValue(FeatureIdentity.PROPERTY_URL);
        databases.putIfAbsent(dbParam, new Database().param(dbParam)
                .prefix(featureIdentity.getClass().getSimpleName())
                .version(String.valueOf(MZmineCore.getMZmineVersion()))
                .uri(dbURI));

        //Identity Information
        String identifier = escapeString(featureIdentity.getPropertyValue(FeatureIdentity.PROPERTY_ID));
        String method = featureIdentity.getPropertyValue(FeatureIdentity.PROPERTY_METHOD);
        String formula = featureIdentity.getPropertyValue(FeatureIdentity.PROPERTY_FORMULA);
        String description = escapeString(featureIdentity.getPropertyValue(FeatureIdentity.PROPERTY_NAME));
        String url = featureIdentity.getPropertyValue(FeatureIdentity.PROPERTY_URL);
        sm.addDatabaseIdentifierItem(identifier);
        sme.setDatabaseIdentifier(identifier);
        sme.setIdentificationMethod(new Parameter().name(method));
        ArrayList<String> formulaList = new ArrayList<>();
        formulaList.add(formula);
        sm.setChemicalFormula(formulaList);
        sme.setChemicalFormula(formula);
        ArrayList<String> chemicalName = new ArrayList<>();
        chemicalName.add(description);
        sm.setChemicalName(chemicalName);
        sme.setChemicalName(description);
        ArrayList<String> uris = new ArrayList<>();
        uris.add(url);
        sm.setUri(uris);
        sme.setUri(url);
        if (url == null || url.equals("")) {
            sme.setUri("null");
        }
    }

    private String escapeString(final String inputString) {

        if (inputString == null) {
            return "";
        }

        // Remove all special characters e.g. \n \t
        return inputString.replaceAll("[\\p{Cntrl}]", " ");
    }

}

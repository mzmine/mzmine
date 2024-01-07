/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.export_features_mztabm;

import de.isas.mztab2.io.MzTabValidatingWriter;
import de.isas.mztab2.model.*;
import io.github.mzmine.datamodel.*;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;
import uk.ac.ebi.pride.jmztab2.model.*;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class MZTabmExportTask extends AbstractTask {

  private int processedRows = 0, totalRows = 0;

  // parameter values
  private final MZmineProject project;
  private final File fileName;
  private String plNamePattern = "{}";
  private FeatureList[] featureLists;
  private final boolean exportAll;

  MZTabmExportTask(MZmineProject project, ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.project = project;
    this.featureLists =
        parameters.getParameter(MZTabmExportParameters.featureLists).getValue().getMatchingFeatureLists();
    this.fileName = parameters.getParameter(MZTabmExportParameters.filename).getValue();
    this.exportAll = parameters.getParameter(MZTabmExportParameters.exportAll).getValue();
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
          String newFilename =
              fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
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
        Hashtable<String, List<RawDataFile>> svhash = new Hashtable<>();
        Hashtable<RawDataFile, Assay> rawDataFileToAssay = new Hashtable<>();

        for (RawDataFile file : rawDataFiles) {
          fileCounter++;
          // MS run location
          MsRun msRun = new MsRun();
          msRun.id(fileCounter);
          msRun.setLocation("file://" + file.getName());
          int dotIn = file.getName().indexOf(".");
          String fileFormat = "";
          if(dotIn != -1){
            fileFormat = file.getName().substring(dotIn+1);
          }
          msRun.setFormat(new Parameter().cvLabel("MS").cvAccession("MS:1000584").name(fileFormat+" file"));
          msRun.setIdFormat(new Parameter().cvLabel("MS").cvAccession("MS:1000774").name("multiple peak list nativeID format"));


          List<Parameter> polPara = new ArrayList<>();
          for(PolarityType scanPol : file.getDataPolarity()){
            Integer pol = scanPol.getSign();
            String polarity = "";
            String polCVA = "";
            if(pol == 1){
              polarity = "positive scan";
              polCVA = "MS:1000130";
            }
            else if(pol == -1){
              polarity = "negative scan";
              polCVA = "MS:1000129";
            }
            else{
              setStatus(TaskStatus.ERROR);
              setErrorMessage("Invalid scan polarity " + pol + " encountered for file "+
                  file.getName() + ".");
              return;
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
          sme.setEvidenceInputId(String.valueOf(i+1));
          List<Double> confidences = new ArrayList<>();
          confidences.add(0.0);
          sme.setIdConfidenceMeasure(confidences);

          //Cancelled?
          if (isCanceled()) {
            return;
          }
          sm.setReliability("2");
          FeatureIdentity featureIdentity = featureListRow.getPreferredFeatureIdentity();
          if (exportAll || featureIdentity != null) {
            if (featureIdentity != null) {
              boolean shouldAdd = true;
              Parameter dbParam = new Parameter().
                  name(featureIdentity.getPropertyValue(FeatureIdentity.PROPERTY_METHOD));
              String dbURI = (featureIdentity.getPropertyValue(FeatureIdentity.PROPERTY_URL) == null ||
                  featureIdentity.getPropertyValue(FeatureIdentity.PROPERTY_URL).equals("")) ?
                  "mzmine://"+ featureIdentity.getClass().getSimpleName():
                  featureIdentity.getPropertyValue(FeatureIdentity.PROPERTY_URL);
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
              if(url == null || url.equals("")){
                sme.setUri("null");
              }
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
            int dataFileCount = 0;
            Hashtable<String, List<Double>> sampleVariableAbundancehash = new Hashtable<>();
            for (RawDataFile dataFile : rawDataFiles) {
              dataFileCount++;
              Feature feature = featureListRow.getFeature(dataFile);
              if (feature != null) {
                //Spectra ref
                List<SpectraRef> sr = new ArrayList<>();
                for(Scan scan : feature.getScanNumbers()){
                  sr.add(new SpectraRef().msRun(rawDataFileToAssay.get(feature.getRawDataFile()).
                      getMsRunRef().get(0)).reference("index=" + scan.getScanNumber()));
                }
                if(sr.size() == 0){
                sr.add(new SpectraRef().msRun(rawDataFileToAssay.get(feature.getRawDataFile()).
                    getMsRunRef().get(0)).reference("index=0"));
                }
                sme.setSpectraRef(sr);
                rowCharge = feature.getCharge();

                String featureMZ = String.valueOf(feature.getMZ());
                String featureRT = String.valueOf(feature.getRT());
                String featureHeight = String.valueOf(feature.getHeight());
                Double featureArea = (double) feature.getArea();
                sm.addOptItem(feature_mzList.get(dataFileCount - 1).build(featureMZ));
                sm.addOptItem(feature_rtList.get(dataFileCount - 1).build(featureRT));
                sm.addOptItem(feature_heightList.get(dataFileCount - 1).build(featureHeight));
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
                  covSV = ( covSV / averageSV ) * 100.0;
                }
              }
              sm.addAbundanceStudyVariableItem(averageSV);
              sm.addAbundanceVariationStudyVariableItem(covSV);
            }

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
        for(Map.Entry<Parameter, Database> entry: databases.entrySet()) {
          mtd.addDatabaseItem(entry.getValue().id(dbId++));
        }

        mzTabFile.metadata(mtd);
        MzTabValidatingWriter validatingWriter = new MzTabValidatingWriter();
        validatingWriter.write(curFile.toPath(), mzTabFile);
      } catch (Exception e) {
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

  private String escapeString(final String inputString) {

      if (inputString == null) {
          return "";
      }

    // Remove all special characters e.g. \n \t
    return inputString.replaceAll("[\\p{Cntrl}]", " ");
  }


}

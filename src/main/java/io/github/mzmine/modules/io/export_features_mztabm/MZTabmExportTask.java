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

import de.isas.mztab2.io.MzTabNonValidatingWriter;
import de.isas.mztab2.model.Assay;
import de.isas.mztab2.model.CV;
import de.isas.mztab2.model.Database;
import de.isas.mztab2.model.Metadata;
import de.isas.mztab2.model.MsRun;
import de.isas.mztab2.model.MzTab;
import de.isas.mztab2.model.Parameter;
import de.isas.mztab2.model.SmallMoleculeEvidence;
import de.isas.mztab2.model.SmallMoleculeFeature;
import de.isas.mztab2.model.SmallMoleculeSummary;
import de.isas.mztab2.model.Software;
import de.isas.mztab2.model.SpectraRef;
import de.isas.mztab2.model.StudyVariable;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.ac.ebi.pride.jmztab2.model.IOptColumnMappingBuilder;
import uk.ac.ebi.pride.jmztab2.model.OptColumnMappingBuilder;
import uk.ac.ebi.pride.jmztab2.model.OptColumnMappingBuilder.GlobalOptColumnMappingBuilder;

public class MZTabmExportTask extends AbstractTask {

  private int processedRows = 0, totalRows = 0;

  // parameter values
  private final MZmineProject project;
  private final File fileName;
  private String plNamePattern = "{}";
  private FeatureList[] featureLists;
  private final boolean exportAll;
  private Metadata mtd;
  private SmallMoleculeSummary sm;
  private SmallMoleculeFeature smf;
  private SmallMoleculeEvidence sme;

  MZTabmExportTask(MZmineProject project, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.project = project;
    this.featureLists = parameters.getParameter(MZTabmExportParameters.featureLists).getValue()
        .getMatchingFeatureLists();
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

    //Optional Columns
    List<IOptColumnMappingBuilder> feature_mzList = new ArrayList<>();
    List<IOptColumnMappingBuilder> feature_rtList = new ArrayList<>();
    List<IOptColumnMappingBuilder> feature_heightList = new ArrayList<>();

    //Process feature Lists
    for (FeatureList featureList : featureLists) {

      //TODO why the file bit is in the loop
      File curFile = fileName;
      try {
        //Filename cleanup
        if (substitute) {
          // Cleanup from illegal filename characters
          //not small alphabets, large alphabets, numbers, dots or dashes
          String newFilename = generateNewFilename(featureList);
          curFile = new File(newFilename);
        }

        MzTab mzTabFile = new MzTab();

        //Metadata
        mtd = generateMetadata(featureList);

        final RawDataFile[] rawDataFiles = featureList.getRawDataFiles()
            .toArray(RawDataFile[]::new);
        int fileCounter = 0;

        // Study Variable name and descriptions
        Hashtable<String, List<RawDataFile>> svhash = new Hashtable<>();
        Hashtable<RawDataFile, Assay> rawDataFileToAssay = new Hashtable<>();

        for (RawDataFile file : rawDataFiles) {
          fileCounter++;

          // MS run location
          MsRun msRun = getMsRunData(fileCounter, file);
          mtd.addMsRunItem(msRun);

//          if (msRun == null) {
//            return;
//          }
          // Add Assay
          Assay assay = new Assay();
          rawDataFileToAssay.put(file, assay);
          assay.id(fileCounter);
          assay.addMsRunRefItem(msRun);

          mtd.addAssayItem(assay);

          feature_mzList.add(
              OptColumnMappingBuilder.forIndexedElement(assay).withName("feature_mz"));
          feature_rtList.add(
              OptColumnMappingBuilder.forIndexedElement(assay).withName("feature_rt"));
          feature_heightList.add(
              OptColumnMappingBuilder.forIndexedElement(assay).withName("feature_height"));

          createSVHash(svhash, file);
        }

        getStudyVariables(svhash, rawDataFileToAssay);

        //Write data rows
        Map<Parameter, Database> databases = new LinkedHashMap<>();

        for (int i = 0; i < featureList.getRows().size(); ++i) {
          FeatureListRow featureListRow = featureList.getRows().get(i);
          sm = new SmallMoleculeSummary();
          sm.setSmlId(i + 1);
          smf = new SmallMoleculeFeature();
          smf.setSmfId(i + 1);
          sme = new SmallMoleculeEvidence();
          sme.setSmeId(i + 1);

          sme.setMsLevel(
              new Parameter().cvLabel("MS").cvAccession("MS:1000511").name("ms level").value("1"));
          sme.setEvidenceInputId(String.valueOf(i + 1));
          List<Double> confidences = new ArrayList<>();
          confidences.add(0.0);
          sme.setIdConfidenceMeasure(confidences);

          //Cancelled?
          if (isCanceled()) {
            return;
          }
          sm.setReliability("2");

          final Collection<DataType> dataTypes = featureListRow.getTypes()
              .values(); //todo values() returns raw data type - discuss??

          List<GlobalOptColumnMappingBuilder> globalOptColumns = new ArrayList<>();

          //introduce column count for optional column placement
          int columnCount = 0;

          //Get available annotations
          if (exportAll || !dataTypes.isEmpty()) {

            Collection<DataType> listType = filterForTypesWithAnnotation(dataTypes);

            for (DataType type : listType) {

              // get the actual value of the ListWithSubsType stored in the feature list, we know it's a list
              final List annotationList = (List) featureListRow.get(type);

              if (annotationList == null || annotationList.isEmpty()) {
                continue;
              }
              ListWithSubsType<?> listWithSubsType = (ListWithSubsType) type;

              //get the list of subTypes
              final List<DataType> subDataTypeList = ((ListWithSubsType) type).getSubDataTypes();

              //detect which preferred type of annotation is present
              //order of priority goes as follows from more specific
              // [lipid annotations] > [spectral databases] > [compound databases]
              //preferred annotation might differ for each feature
              String preferredAnnotation = "";
              for (int j = 0; j < subDataTypeList.size(); j++) {
                final String typeUniqueID = type.getUniqueID();
                final String subtypeValue = listWithSubsType.getFormattedSubColValue(j,
                    annotationList);
                if (subtypeValue != null) {
                  if (typeUniqueID == "lipid_annotations") {
                    preferredAnnotation = "lipid_annotations";
                  } else {
                    if (typeUniqueID == "spectral_db_matches") {
                      preferredAnnotation = "spectral_db_matches";
                    } else if (typeUniqueID == "compound_db_identity") {
                      preferredAnnotation = "compound_db_identity";
                    }
                  }
                }
              }

              // export annotations
              for (int j = 0; j < subDataTypeList.size(); j++) {
                final String uniqueID = subDataTypeList.get(j).getUniqueID();
                final String subtypeValue = listWithSubsType.getFormattedSubColValue(j,
                    annotationList);
//                   System.out.println(listWithSubsType.getUniqueID() + "\t" + uniqueID + "\t" + subtypeValue);
                globalOptColumns.add(OptColumnMappingBuilder.forGlobal()
                    .withName(type.getUniqueID() + "_" + uniqueID));
                sme.addOptItem(globalOptColumns.get(columnCount).build(subtypeValue));
                if (type.getUniqueID() == preferredAnnotation) {
                  updateWithPreferredAnnotation(preferredAnnotation, uniqueID, subtypeValue);
                }
                columnCount++;
              }
            }

            Double rowMZ = featureListRow.getAverageMZ();
            Float rowRT = featureListRow.getAverageRT();
            Integer rowCharge = featureListRow.getRowCharge().intValue();

            if (rowMZ != null) {
              smf.setExpMassToCharge(rowMZ.doubleValue());
              sme.setExpMassToCharge(rowMZ.doubleValue());
            }
            if (rowRT != null) {
              smf.setRetentionTimeInSeconds(rowRT.doubleValue());
            }

            int dataFileCount = 0;
            Hashtable<String, List<Double>> sampleVariableAbundancehash = new Hashtable<>();
            for (RawDataFile dataFile : rawDataFiles) {
              dataFileCount++;
              Feature feature = featureListRow.getFeature(dataFile);
              if (feature != null) {
                //Spectra ref
                List<SpectraRef> sr = new ArrayList<>();
                for (Scan scan : feature.getScanNumbers()) {
                  sr.add(new SpectraRef().msRun(
                          rawDataFileToAssay.get(feature.getRawDataFile()).getMsRunRef().get(0))
                      .reference("index=" + scan.getScanNumber()));
                }
                if (sr.size() == 0) {
                  sr.add(new SpectraRef().msRun(
                          rawDataFileToAssay.get(feature.getRawDataFile()).getMsRunRef().get(0))
                      .reference("index=0"));
                }
                //TODO alternative representation of spectra ref
                sme.setSpectraRef(sr);

                String featureMZ = String.valueOf(feature.getMZ());
                String featureRT = String.valueOf(feature.getRT());
                String featureHeight = String.valueOf(feature.getHeight());
                Double featureArea = (double) feature.getArea();
                Integer featureCharge = feature.getCharge();
                sm.addOptItem(feature_mzList.get(dataFileCount - 1).build(featureMZ));
                sm.addOptItem(feature_rtList.get(dataFileCount - 1).build(featureRT));
                sm.addOptItem(feature_heightList.get(dataFileCount - 1).build(featureHeight));

                if (featureCharge != 0) {
                  smf.setCharge(featureCharge);
                  sme.setCharge(featureCharge);
                } else {
                  smf.setCharge(null);
                  sme.setCharge(null);
                }

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
              addSVAbundance(sampleVariableAbundancehash, studyVariable);
            }

            sm.addSmfIdRefsItem(smf.getSmfId());
            smf.addSmeIdRefsItem(sme.getSmeId());
            mzTabFile.addSmallMoleculeSummaryItem(sm);
            mzTabFile.addSmallMoleculeFeatureItem(smf);
            mzTabFile.addSmallMoleculeEvidenceItem(sme);
          }
        }
        //cv term
        int dbId = 1;
        //set ids sequentially, starting from 1
        for (Entry<Parameter, Database> entry : databases.entrySet()) {
          mtd.addDatabaseItem(entry.getValue().id(dbId++));
        }

        mzTabFile.metadata(mtd);
        MzTabNonValidatingWriter mzTabWriter = new MzTabNonValidatingWriter();
        mzTabWriter.write(curFile.toPath(), mzTabFile);
        //TODO - change later for validating writer
//        MzTabValidatingWriter validatingWriter = new MzTabValidatingWriter();
//        validatingWriter.write(curFile.toPath(), mzTabFile);
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

  private void updateWithPreferredAnnotation(String preferredAnnotation, String uniqueID, String subtypeValue) {
    sme.setDatabaseIdentifier(preferredAnnotation);
    if (uniqueID == "mol_formula") {
      sme.setChemicalFormula(subtypeValue);
    }
    if (uniqueID == "ion_adduct") {
      sme.setAdductIon(subtypeValue);
    }
    if (uniqueID == "compound_name") {
      sme.setChemicalName(subtypeValue);
    }
    if (uniqueID == "smiles") {
      sme.setSmiles(subtypeValue);
    }
    if (uniqueID == "inchi") {
      sme.setInchi(subtypeValue);
    }
    if (uniqueID == "precursor_mz") {
      sme.setTheoreticalMassToCharge(Double.parseDouble(subtypeValue));
    }
  }

  private void addSVAbundance(Hashtable<String, List<Double>> sampleVariableAbundancehash,
      String studyVariable) {
    //Using mean as average function for abundance of Study Variable
    Double sumSV = sampleVariableAbundancehash.get(studyVariable).stream().collect(
        Collectors.summingDouble(Double::doubleValue));

    int totalSV = sampleVariableAbundancehash.get(studyVariable).size();
    final Double averageSV = totalSV == 0 ? 0.0 : sumSV / totalSV;

    //Coefficient of variation
    Double covSV = sampleVariableAbundancehash.get(studyVariable).stream().map(d -> (d - averageSV)*(d - averageSV)).collect(Collectors.summingDouble(Double::doubleValue));

    covSV = (totalSV == 0 || totalSV == 1) ?
        0.0 :
        Math.sqrt(covSV/(totalSV - 1));

    if (averageSV != 0.0)
      covSV = (covSV / averageSV) * 100.0;

    sm.addAbundanceStudyVariableItem(averageSV);
    sm.addAbundanceVariationStudyVariableItem(covSV);
  }

  private void getStudyVariables(Hashtable<String, List<RawDataFile>> svhash,
      Hashtable<RawDataFile, Assay> rawDataFileToAssay) {
    int studyVarCount = 0;
    for (String key : svhash.keySet()) {
      studyVarCount++;
      StudyVariable studyVariable = new StudyVariable().id(studyVarCount).name(key).description(key)
          .averageFunction(new Parameter().cvLabel("MS").cvAccession("MS:1002883").name("mean"));
      for (RawDataFile file : svhash.get(key)) {
        studyVariable = studyVariable.addAssayRefsItem(rawDataFileToAssay.get(file));
      }
      mtd.addStudyVariableItem(studyVariable);
    }
  }

  private void createSVHash(Hashtable<String, List<RawDataFile>> svhash, RawDataFile file) {
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

  @Nullable
  private MsRun getMsRunData(int fileCounter, RawDataFile file) {
    MsRun msRun = new MsRun();
    msRun.id(fileCounter);
    msRun.setLocation("file://" + file.getName());
    int dotIn = file.getName().indexOf(".");
    String fileFormat = "";
    if (dotIn != -1) {
      fileFormat = file.getName().substring(dotIn + 1);
    }
    msRun.setFormat(
        new Parameter().cvLabel("MS").cvAccession("MS:1000584").name(fileFormat + " file"));
    msRun.setIdFormat(new Parameter().cvLabel("MS").cvAccession("MS:1000774")
        .name("multiple peak list nativeID format"));

    List<Parameter> polPara = getPolarityParameters(file);

    if (polPara == null) {
      return null;
    }

    msRun.setScanPolarity(polPara);
    return msRun;
  }

  @Nullable
  private List<Parameter> getPolarityParameters(RawDataFile file) {
    List<Parameter> polPara = new ArrayList<>();
    for (PolarityType scanPol : file.getDataPolarity()) {
      Integer pol = scanPol.getSign();
      String polarity = "";
      String polCVA = "";
      if (pol == 1) {
        polarity = "positive scan";
        polCVA = "MS:1000130";
      } else if (pol == -1) {
        polarity = "negative scan";
        polCVA = "MS:1000129";
      } else {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(
            "Invalid scan polarity " + pol + " encountered for file " + file.getName() + ".");
        return null;
      }
      Parameter p = new Parameter().cvLabel("MS").cvAccession(polCVA).name(polarity);
      polPara.add(p);
    }
    return polPara;
  }

  @NotNull
  private String generateNewFilename(FeatureList featureList) {
    String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
    // Substitute
    String newFilename = fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
    return newFilename;
  }

  private static boolean isMatchesType(DataType dataType) {
    return (dataType instanceof ListWithSubsType<?>) || (dataType instanceof AnnotationType);
  }

  @Nullable
  private static Collection<DataType> filterForTypesWithAnnotation(Collection<DataType> types) {
    return types.stream().filter(Objects::nonNull).filter(MZTabmExportTask::isMatchesType).toList();
  }

  @NotNull
  private static Metadata generateMetadata(FeatureList featureList) {
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
    mtd.addCvItem(new CV().id(1).label("MS").fullName("PSI-MS controlled vocabulary")
        .version("4.0.9"). //todo update version 4.1.108
            uri("https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/master/psi-ms.obo"));
    mtd.addDatabaseItem(
        new Database().id(1).prefix("mzmdb").version(MZmineCore.getMZmineVersion().toString())
            .uri("https://mzmine.github.io/")
            .param(new Parameter().name("[, , MZmine database, ]")));
    //todo set study variable
    return mtd;
  }

  private String escapeString(final String inputString) {

    if (inputString == null) {
      return "";
    }

    // Remove all special characters e.g. \n \t
    return inputString.replaceAll("[\\p{Cntrl}]", " ");
  }


}

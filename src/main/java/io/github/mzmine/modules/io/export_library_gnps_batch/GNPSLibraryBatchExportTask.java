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

package io.github.mzmine.modules.io.export_library_gnps_batch;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch.ATT;
import io.github.mzmine.modules.io.spectraldbsubmit.param.LibraryMetaDataParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.param.LibrarySubmitIonParameters;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;


public class GNPSLibraryBatchExportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(GNPSLibraryBatchExportTask.class.getName());
  private final DecimalFormat scoreFormat = new DecimalFormat("0.000");
  private final String tab = "\t";
  private final String nl = "\n";

  // batch import header
  // tab separated
  public static final String HEADER_BATCH =
      "FILENAME,SEQ,COMPOUND_NAME,MOLECULEMASS,INSTRUMENT,IONSOURCE,EXTRACTSCAN,SMILES,INCHI,INCHIAUX,CHARGE,IONMODE,PUBMED,ACQUISITION,EXACTMASS,DATACOLLECTOR,ADDUCT,INTEREST,LIBQUALITY,GENUS,SPECIES,STRAIN,CASNUMBER,PI";


  private LibraryMethodeMetaDataParameters methodParam;
  private File outputLibrary;
  private GnpsResults res;
  private double minMatchScoreGNPS;
  private File outputLibraryBatch;
  private String mgfName;

  public GNPSLibraryBatchExportTask(LibraryMethodeMetaDataParameters methodParam, String mgfName,
      File outputLibrary, GnpsResults res, double minMatchScoreGNPS) {
    this.methodParam = methodParam;
    this.mgfName = mgfName;
    this.outputLibrary = outputLibrary;
    outputLibraryBatch = FileAndPathUtil.getRealFilePath(outputLibrary, "tsv");
    this.res = res;
    this.minMatchScoreGNPS = minMatchScoreGNPS;
  }

  @Override
  public String getTaskDescription() {
    return "json and batch export of GNPS library from FBMNxIIN results";
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  /**
   * Find all IIN with identity (spectral match) and export conneted nodes as new library entries
   *
   * @param methodParam
   *
   * @param outputLibrary
   * @param res
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    Map<Integer, GNPSResultsIdentity> matches = res.getMatches();
    Map<Integer, DataPoint[]> msmsData = res.getMsmsData();
    Map<Integer, IonIdentityNetworkResult> nets = res.getNets();
    AtomicInteger totalNew = new AtomicInteger(0);
    // create parameters:
    LibraryMetaDataParameters meta = new LibraryMetaDataParameters(methodParam);
    String description = meta.getParameter(LibraryMetaDataParameters.DESCRIPTION).getValue();
    LibrarySubmitIonParameters param = new LibrarySubmitIonParameters();
    param.getParameter(LibrarySubmitIonParameters.META_PARAM).setValue(meta);

    try {
      if (!outputLibrary.getParentFile().exists())
        outputLibrary.getParentFile().mkdirs();
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Cannot create folder " + outputLibrary.getParent(), e);
    }

    boolean writeHeader = !outputLibraryBatch.exists();
    // open file output
    try (BufferedWriter json = new BufferedWriter((new FileWriter(outputLibrary, false)));
        BufferedWriter gnpsBatch = new BufferedWriter(new FileWriter(outputLibraryBatch, true))) {
      // export batch header only if file does not exist
      // otherwise append
      if (writeHeader) {
        gnpsBatch.write(HEADER_BATCH.replaceAll(",", tab));
        gnpsBatch.write(nl);
      }

      // for all networks
      for (IonIdentityNetworkResult net : nets.values()) {
        // has identity
        GNPSResultsIdentity bestMatch = net.getBestLibraryMatch(matches);
        // >min match score
        if (bestMatch != null && bestMatch.getMatchScore() >= minMatchScoreGNPS) {
          // all possible new library entries of this ion network
          net.stream().filter(node -> hasMSMS(node, msmsData, 3, 0.001)).forEach(node -> {
            // export to library
            int id = toIndex(node);
            DataPoint[] signals = msmsData.get(id);
            totalNew.getAndIncrement();
            logger.log(Level.INFO,
                "new lib:" + totalNew.get() + "  Exporting node " + id + " with signals="
                + signals.length + "  for entry: " + bestMatch.getName() + " old->new ("
                + bestMatch.getResult(ATT.ADDUCT) + "->"
                + IonIdentityNetworkResult.getIonString(node) + ")");

            // map all parameters
            createEntryParameters(node, bestMatch, meta, param);
            // json export
            exportJsonLibraryEntry(json, param, signals);
            // GNPS batch library export file:
            exportGNPSBatchLibraryEntry(gnpsBatch, param, mgfName, toIndex(node));


            // reset description as it is changed for every entry
            meta.getParameter(LibraryMetaDataParameters.DESCRIPTION).setValue(description);
          });
        }
      }

      logger.info(totalNew.get() + " added new entries to " + outputLibrary.getAbsolutePath());
      // close file output automatically
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error while writing to " + outputLibrary.getAbsolutePath(), e);
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error while writing to " + outputLibrary.getAbsolutePath());
      return;
    }

    setStatus(TaskStatus.FINISHED);
  }

  private String exportGNPSBatchLibraryEntry(BufferedWriter writer,
      LibrarySubmitIonParameters param, String mgfName, int specIndex) {

    // write
    String batchRow = GnpsLibraryGenerator.generateBatchRow(param, mgfName, specIndex);

    // write it
    try {
      writer.write(batchRow);
      writer.write(nl);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error while writing GNPS batch row " + batchRow + " to "
                               + outputLibraryBatch.getAbsolutePath(), e);
      e.printStackTrace();
    }
    return batchRow;
  }

  private String exportJsonLibraryEntry(BufferedWriter writer, LibrarySubmitIonParameters param,
      DataPoint[] signals) {
    // write
    String json = GnpsLibraryGenerator.generateJSON(param, signals);

    // write it
    try {
      writer.write(json);
      writer.write(nl);
    } catch (IOException e) {
      logger.log(Level.SEVERE,
          "Error while writing " + json + " to " + outputLibrary.getAbsolutePath(), e);
      e.printStackTrace();
    }
    return json;
  }

  /**
   * Create the entries parameters for export
   *
   * @param node
   * @param bestMatch
   * @param meta
   * @param param is changed and also the return value
   * @return
   */
  private LibrarySubmitIonParameters createEntryParameters(Node node, GNPSResultsIdentity bestMatch,
      LibraryMetaDataParameters meta, LibrarySubmitIonParameters param) {

    String description = meta.getParameter(LibraryMetaDataParameters.DESCRIPTION).getValue();

    String combinedDescription =
        "created by [IIN] (GNPS score=" + scoreFormat.format(bestMatch.getMatchScore()) + ", "
        + bestMatch.getResult(ATT.ADDUCT) + "), " + description + ", original lib entry: "
        + bestMatch.getResult(ATT.GNPS_LIBRARY_URL);
    meta.getParameter(LibraryMetaDataParameters.DESCRIPTION).setValue(combinedDescription);

    // By Library match
    boolean isMatchedNode = Integer.compare(bestMatch.getNodeID(), toIndex(node)) == 0;
    String nameAddition = isMatchedNode ? " [IIN-based: Match]"
        : " [IIN-based on: " + bestMatch.getResult(ATT.SPECTRUM_ID) + "]";
    String newName = bestMatch.getResult(ATT.COMPOUND_NAME).toString() + nameAddition;
    meta.getParameter(LibraryMetaDataParameters.COMPOUND_NAME).setValue(newName);
    meta.getParameter(LibraryMetaDataParameters.SMILES)
        .setValue(bestMatch.getResult(ATT.SMILES).toString());
    meta.getParameter(LibraryMetaDataParameters.INCHI)
        .setValue(bestMatch.getResult(ATT.INCHI).toString());
    // not given in GNPS output (graphml)
    meta.getParameter(LibraryMetaDataParameters.FORMULA).setValue("");
    meta.getParameter(LibraryMetaDataParameters.INCHI_AUX).setValue("");
    meta.getParameter(LibraryMetaDataParameters.CAS).setValue("");
    meta.getParameter(LibraryMetaDataParameters.PUBMED).setValue("");

    // by IIN
    double neutralMass = (double) bestMatch.getResult(ATT.NEUTRAL_M_MASS);
    meta.getParameter(LibraryMetaDataParameters.EXACT_MASS).setValue(neutralMass);
    param.getParameter(LibrarySubmitIonParameters.ADDUCT)
        .setValue(IonIdentityNetworkResult.getIonString(node));
    param.getParameter(LibrarySubmitIonParameters.MZ)
        .setValue((double) node.getAttribute(NodeAtt.PRECURSOR_MASS.key));
    param.getParameter(LibrarySubmitIonParameters.CHARGE).setValue(0);
    return param;
  }


  /**
   * Node index was peak list row index
   *
   * @param n
   * @return
   */
  private Integer toIndex(Node n) {
    return Integer.parseInt(n.getId());
  }


  /**
   * minimum signals above cutOffFromMaxIntensity
   *
   * @param n
   * @param msmsData
   * @param minSignals
   * @param cutOffFromMaxIntensity 0.01 is 1 % of max intensity
   * @return
   */
  private boolean hasMSMS(Node n, Map<Integer, DataPoint[]> msmsData, int minSignals,
      final double cutOffFromMaxIntensity) {
    DataPoint[] signals = msmsData.get(toIndex(n));
    if (signals == null)
      return false;
    final double max = Arrays.stream(signals).mapToDouble(DataPoint::getIntensity).max().orElse(0);
    long dp = Arrays.stream(signals).mapToDouble(DataPoint::getIntensity)
        .filter(intensity -> intensity >= max * cutOffFromMaxIntensity).count();
    return dp >= minSignals;
  }

  private boolean hasMSMS(Node n, Map<Integer, DataPoint[]> msmsData, int minSignals) {
    DataPoint[] signals = msmsData.get(Integer.parseInt(n.getId()));
    return signals != null && signals.length >= minSignals;
  }
}
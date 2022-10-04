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

package io.github.mzmine.modules.io.export_msn_tree;

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.PrecursorIonTree;
import io.github.mzmine.datamodel.PrecursorIonTreeNode;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.io.CSVUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MSnTreeExportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(MSnTreeExportTask.class.getName());

  private final File outFile;
  private final RawDataFile[] raws;
  private final MZTolerance mzTol;
  private final String sep;
  private final NumberFormat mzFormat;
  private final String description;
  private int total = 0;
  private int done;

  public MSnTreeExportTask(ParameterSet parameters, Instant moduleCallDate) {
    super(null, moduleCallDate);
    outFile = parameters.getValue(MSnTreeExportParameters.FILENAME);
    raws = parameters.getValue(MSnTreeExportParameters.RAW_FILES).getMatchingRawDataFiles();
    mzTol = parameters.getValue(MSnTreeExportParameters.MZ_TOL);
    sep = parameters.getValue(MSnTreeExportParameters.SEPARATOR);
    description = String.format("Exporting %d raw files as MSn trees to tabular file %s",
        raws.length, outFile.getAbsolutePath());

    mzFormat = MZmineCore.getConfiguration().getMZFormat();
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return total == 0 ? 0 : done / (double) total;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    total = raws.length;
    int treeID = 1;

    try (BufferedWriter bw = Files.newBufferedWriter(outFile.toPath(), StandardCharsets.UTF_8)) {
      // write header
      bw.append(getHeader()).append("\n");

      for (RawDataFile raw : raws) {
        final List<PrecursorIonTree> trees = ScanUtils.getMSnFragmentTrees(raw, mzTol);
        for (PrecursorIonTree tree : trees) {
          List<String> lines = treeToCSV(raw, tree, new ArrayList<>(), treeID);
          for (String line : lines) {
            bw.append(line).append("\n");
          }
          treeID++;
        }
        done++;
      }
    } catch (MissingMassListException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Missing mass list, run mass detection on all scans");
      logger.log(Level.WARNING, e.getMessage(), e);
      return;
    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + outFile.getAbsolutePath() + " for writing.");
      logger.log(Level.WARNING,
          String.format("Error writing new CSV format to file: %s for raw files: %s. Message: %s",
              outFile.getAbsolutePath(),
              Arrays.stream(raws).map(RawDataFile::getName).collect(Collectors.joining(", ")),
              e.getMessage()), e);
      return;
    }

    setStatus(TaskStatus.FINISHED);
  }

  private List<String> treeToCSV(RawDataFile raw, PrecursorIonTree tree, List<String> lines,
      int treeID) {
    ExtractedValues commonValues = extractCommonValues(tree);
    treeNodeToCSV(raw, tree.getRoot(), lines, treeID, commonValues);
    return lines;
  }

  private void treeNodeToCSV(RawDataFile raw, PrecursorIonTreeNode treeNode, List<String> lines,
      int treeID, ExtractedValues commonValues) {
    // get formatted list of precursors as string
    String formattedPrecursorMzList = getPrecursorMzListFormatted(treeNode);
    // export all spectra
    for (var spec : treeNode.getFragmentScans()) {
      String line = spectrumToCSV(raw, spec, treeID, commonValues, formattedPrecursorMzList);
      lines.add(line);
    }

    // add children
    for (var child : treeNode.getChildPrecursors()) {
      treeNodeToCSV(raw, child, lines, treeID, commonValues);
    }
  }

  private String getPrecursorMzListFormatted(PrecursorIonTreeNode treeNode) {
    return CSVUtils.getListString(treeNode.getPrecursorMZList(), ",", mzFormat);
  }

  private String spectrumToCSV(RawDataFile raw, Scan spec, int treeID, ExtractedValues commonValues,
      String formattedPrecursorMzList) {
    return Arrays.stream(MSnFields.values())
        .map(field -> getValue(field, raw, spec, treeID, commonValues, formattedPrecursorMzList))
        .map(value -> CSVUtils.escape(value, sep)).collect(Collectors.joining(sep));
  }

  private String getValue(MSnFields field, RawDataFile raw, Scan spec, int treeID,
      ExtractedValues common, String formattedPrecursorMzList) {
    return switch (field) {
      case FILENAME -> raw.getName();
      case SCAN_NUMBER -> "" + spec.getScanNumber();
      case TREE_ID -> "" + treeID;
      case MS_LEVEL -> "" + spec.getMSLevel();
      case PRECURSOR_MZ -> mzFormat.format(spec.getPrecursorMz());
      case PRECURSOR_MS2 -> common.PRECURSOR_MS2;
      case PRECURSOR_LIST -> formattedPrecursorMzList;
      case N_SIGNALS -> {
        MassList massList = spec.getMassList();
        if (massList == null) {
          throw new MissingMassListException(spec);
        }
        yield "" + massList.getNumberOfDataPoints();
      }
      case MAX_MSN -> "" + common.MAX_MSN;
      case N_PREC -> "" + common.N_PREC;
      case N_PREC_MS3 -> "" + common.N_PREC_MS3;
      case N_PREC_MS4 -> "" + common.N_PREC_MS4;
      case N_PREC_MS5 -> "" + common.N_PREC_MS5;
      case N_PREC_MS6 -> "" + common.N_PREC_MS6;
      case N_SPEC -> "" + common.N_SPEC;
      case N_MS2 -> "" + common.N_MS2;
      case N_MS3 -> "" + common.N_MS3;
      case N_MS4 -> "" + common.N_MS4;
      case N_MS5 -> "" + common.N_MS5;
      case N_MS6 -> "" + common.N_MS6;
      case MZS -> {
        MassList massList = spec.getMassList();
        if (massList == null) {
          throw new MissingMassListException(spec);
        }
        yield CSVUtils.getMzListFormatted(massList, ",");
      }
      case INTENSITIES -> {
        MassList massList = spec.getMassList();
        if (massList == null) {
          throw new MissingMassListException(spec);
        }
        yield CSVUtils.getIntensityListFormatted(massList, ",");
      }
    };
  }

  private String getHeader() {
    return Arrays.stream(MSnFields.values()).map(Object::toString).collect(Collectors.joining(sep));
  }

  private ExtractedValues extractCommonValues(PrecursorIonTree tree) {
    PrecursorIonTreeNode root = tree.getRoot();
    return new ExtractedValues(mzFormat.format(root.getPrecursorMZ()), tree.getMaxMSLevel(),
        tree.countPrecursor(), tree.countSpectra(),
        // count precursor ions
        tree.countPrecursor(3), tree.countPrecursor(4), tree.countPrecursor(5),
        tree.countPrecursor(6),
        // count spectra
        tree.countSpectra(2), tree.countSpectra(3), tree.countSpectra(4), tree.countSpectra(5),
        tree.countSpectra(6));
  }


  enum MSnFields {
    FILENAME, SCAN_NUMBER, TREE_ID, MS_LEVEL, PRECURSOR_MZ, PRECURSOR_MS2, PRECURSOR_LIST, N_SIGNALS,

    // tree specific
    MAX_MSN, N_PREC, N_PREC_MS3, N_PREC_MS4, N_PREC_MS5, N_PREC_MS6, //
    N_SPEC, N_MS2, N_MS3, N_MS4, N_MS5, N_MS6, // data fields
    MZS, INTENSITIES
  }

  record ExtractedValues(String PRECURSOR_MS2, int MAX_MSN, int N_PREC, int N_SPEC, int N_PREC_MS3,
                         int N_PREC_MS4, int N_PREC_MS5, int N_PREC_MS6, int N_MS2, int N_MS3,
                         int N_MS4, int N_MS5, int N_MS6) {

  }
}

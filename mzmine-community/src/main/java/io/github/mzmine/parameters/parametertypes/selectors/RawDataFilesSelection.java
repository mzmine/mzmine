/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.selectors;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.TextUtils;
import io.github.mzmine.util.io.JsonUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class RawDataFilesSelection implements Cloneable {

  private static final Logger logger = Logger.getLogger(RawDataFilesSelection.class.getName());

  private RawDataFilesSelectionType selectionType;
  private String namePattern;
  private RawDataFilePlaceholder[] batchLastFiles;
  private RawDataFilePlaceholder[] specificFiles;
  private RawDataFilePlaceholder[] evaluatedSelection = null;

  public RawDataFilesSelection() {
    this(RawDataFilesSelectionType.GUI_SELECTED_FILES);
  }

  public RawDataFilesSelection(RawDataFilesSelectionType selectionType) {
    this.selectionType = selectionType;
  }

  public RawDataFilesSelection(RawDataFile[] specificDataFiles) {
    this.selectionType = RawDataFilesSelectionType.SPECIFIC_FILES;
    setSpecificFiles(specificDataFiles);
  }

  public RawDataFilePlaceholder[] getEvaluationResult() {
    if (evaluatedSelection != null) {
      return Arrays.copyOf(evaluatedSelection, evaluatedSelection.length);
    }
    throw new IllegalStateException("Raw data file selection has not been evaluated.");
  }

  public RawDataFile[] getMatchingRawDataFiles() {

    if (evaluatedSelection != null) {
      var value = RawDataFilePlaceholder.getMatchingFilesNullable(evaluatedSelection);
      for (var raw : value) {
        if (raw == null) {
          throw new IllegalStateException(
              "Data file selection points to a missing file (maybe it was removed after first evaluation).");
        }
      }
      // Raw data file selection are only evaluated once - to keep the parameter value the same
      // even if raw data files are removed or renamed
      logger.fine(
          "Using the already evaluated list of raw data files. This might be expected at this point depending on the module.");
      return value;
    }

    final RawDataFile[] matchingFiles;
    switch (selectionType) {
      case GUI_SELECTED_FILES -> matchingFiles = MZmineCore.getDesktop().getSelectedDataFiles();
      case ALL_FILES ->
          matchingFiles = ProjectService.getProjectManager().getCurrentProject().getDataFiles();
      case SPECIFIC_FILES -> matchingFiles = getSpecificFiles();
      case NAME_PATTERN -> {
        if (Strings.isNullOrEmpty(namePattern)) {
          return new RawDataFile[0];
        }
        ArrayList<RawDataFile> matchingDataFiles = new ArrayList<>();
        RawDataFile[] allDataFiles = ProjectService.getProjectManager().getCurrentProject()
            .getDataFiles();

        final String regex = TextUtils.createRegexFromWildcards(namePattern);

        for (RawDataFile file : allDataFiles) {
          final String fileName = file.getName();

          if (fileName.matches(regex)) {
            if (!matchingDataFiles.contains(file)) {
              matchingDataFiles.add(file);
            }
          }
        }
        matchingFiles = matchingDataFiles.toArray(new RawDataFile[0]);
      }
      case BATCH_LAST_FILES -> matchingFiles = getBatchLastFiles();
      default -> throw new IllegalStateException("Unexpected value: " + selectionType);
    }

    evaluatedSelection = new RawDataFilePlaceholder[matchingFiles.length];
    for (int i = 0; i < matchingFiles.length; i++) {
      RawDataFile matchingFile = matchingFiles[i];
      evaluatedSelection[i] = new RawDataFilePlaceholder(matchingFile);
    }
    // only debugging
    // the RawDataFilesComponent auto updates the selection every second so this would spam
//    logger.finest(
//        () -> "Setting file selection. Evaluated files: " + Arrays.toString(evaluatedSelection));

    return matchingFiles;
  }

  public RawDataFilesSelectionType getSelectionType() {
    return selectionType;
  }

  public void setSelectionType(RawDataFilesSelectionType selectionType) {
    this.selectionType = selectionType;
  }

  public void resetSelection() {
//    if (evaluatedSelection != null) {
//      logger.finest(
//          () -> "Resetting file selection. Previously evaluated files: " + Arrays.toString(
//              evaluatedSelection));
//    }
    evaluatedSelection = null;
  }

  RawDataFile[] getSpecificFiles() {
    return RawDataFilePlaceholder.getMatchingFilesFilterNull(specificFiles);
  }

  public void setSpecificFiles(RawDataFile[] specificFiles) {
    resetSelection();
    this.specificFiles = RawDataFilePlaceholder.of(specificFiles);
  }

  public void setSpecificFiles(RawDataFilePlaceholder[] specificFiles) {
    resetSelection();
    this.specificFiles = Arrays.copyOf(specificFiles, specificFiles.length);
  }

  public RawDataFilePlaceholder[] getSpecificFilesPlaceholders() {
    return this.specificFiles;
  }

  public String getNamePattern() {
    return namePattern;
  }

  public void setNamePattern(String namePattern) {
    resetSelection();
    this.namePattern = namePattern;
  }

  public void setBatchLastFiles(RawDataFile[] batchLastFiles) {
    resetSelection();
    this.batchLastFiles = RawDataFilePlaceholder.of(batchLastFiles);
  }

  /**
   * @return the matching raw data files set in batch last files
   */
  @NotNull RawDataFile[] getBatchLastFiles() {
    return RawDataFilePlaceholder.getMatchingFilesFilterNull(batchLastFiles);
  }

  public RawDataFilesSelection clone() {
    RawDataFilesSelection newSelection = new RawDataFilesSelection();
    newSelection.selectionType = selectionType;
    // needs to keep specific files in all cases
    // selection is only cleared for the evaluatedSelection to decouple preview dialogs and task run
    // and applied methods from each other
    newSelection.specificFiles = specificFiles;
    newSelection.namePattern = namePattern;
    // batchLastFiles are never cloned and this should be fine as they are always set by batch
    return newSelection;
  }

  public RawDataFilesSelection cloneAndKeepSelection() {
    RawDataFilesSelection newSelection = new RawDataFilesSelection();
    newSelection.selectionType = selectionType;
    newSelection.specificFiles = specificFiles;
    newSelection.namePattern = namePattern;
    newSelection.evaluatedSelection = evaluatedSelection;
    // batchLastFiles are never cloned and this should be fine as they are always set by batch
    return newSelection;
  }

  public String toString() {
    if (evaluatedSelection != null) {
      return selectionType + ", " + JsonUtils.writeStringOrElse(
          Arrays.stream(evaluatedSelection).map(RawDataFilePlaceholder::getName).toList(), "[]");
    }
    return selectionType + ", Evaluation not executed.";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RawDataFilesSelection that = (RawDataFilesSelection) o;

    if (getSelectionType() != that.getSelectionType() || !Objects.equals(getNamePattern(),
        that.getNamePattern())) {
      return false;
    }

    if (getSelectionType() == RawDataFilesSelectionType.BATCH_LAST_FILES && //
        ((batchLastFiles != null && that.batchLastFiles != null && !List.of(batchLastFiles)
            .containsAll(List.of(that.batchLastFiles))) // not all files equal
            || (batchLastFiles == null && that.batchLastFiles != null) //
            || (batchLastFiles != null && that.batchLastFiles == null))) {
      return false;
    }

    if (getSelectionType() == RawDataFilesSelectionType.SPECIFIC_FILES && //
        !List.of(getSpecificFiles()).containsAll(List.of(that.getSpecificFiles()))) {
      return false;
    }

    if ((this.evaluatedSelection != null && that.evaluatedSelection != null && //
        !List.of(evaluatedSelection).containsAll(List.of(that.evaluatedSelection))) || //
        ((evaluatedSelection == null && that.evaluatedSelection != null) || (
            evaluatedSelection != null && that.evaluatedSelection == null))) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(getSelectionType(), getNamePattern());
    result = 31 * result + Arrays.hashCode(batchLastFiles);
    result = 31 * result + Arrays.hashCode(getSpecificFiles());
    result = 31 * result + Arrays.hashCode(evaluatedSelection);
    return result;
  }
}

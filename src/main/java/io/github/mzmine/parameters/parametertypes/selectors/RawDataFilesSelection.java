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

package io.github.mzmine.parameters.parametertypes.selectors;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class RawDataFilesSelection implements Cloneable {

  private static final Logger logger = Logger.getLogger(RawDataFilesSelection.class.getName());

  private RawDataFilesSelectionType selectionType;
  private String namePattern;
  private RawDataFile[] batchLastFiles;
  private RawDataFilePlaceholder[] specificFiles;
  private RawDataFilePlaceholder[] evaluatedSelection = null;

  public RawDataFilesSelection() {
    this(RawDataFilesSelectionType.GUI_SELECTED_FILES);
  }

  public RawDataFilesSelection(RawDataFilesSelectionType selectionType) {
    this.selectionType = selectionType;
  }

  public RawDataFilePlaceholder[] getEvaluationResult() {
    if (evaluatedSelection != null) {
      return Arrays.copyOf(evaluatedSelection, evaluatedSelection.length);
    }
    throw new IllegalStateException("Raw data file selection has not been evaluated.");
  }

  public RawDataFile[] getMatchingRawDataFiles() {

    if (evaluatedSelection != null) {
      var value = Arrays.stream(evaluatedSelection).map(RawDataFilePlaceholder::getMatchingFile)
          .toArray(RawDataFile[]::new);
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
      case ALL_FILES -> matchingFiles = MZmineCore.getProjectManager().getCurrentProject()
          .getDataFiles();
      case SPECIFIC_FILES -> matchingFiles = getSpecificFiles();
      case NAME_PATTERN -> {
        if (Strings.isNullOrEmpty(namePattern)) {
          return new RawDataFile[0];
        }
        ArrayList<RawDataFile> matchingDataFiles = new ArrayList<>();
        RawDataFile[] allDataFiles = MZmineCore.getProjectManager().getCurrentProject()
            .getDataFiles();

        fileCheck:
        for (RawDataFile file : allDataFiles) {

          final String fileName = file.getName();

          final String regex = TextUtils.createRegexFromWildcards(namePattern);

          if (fileName.matches(regex)) {
            if (matchingDataFiles.contains(file)) {
              continue;
            }
            matchingDataFiles.add(file);
            continue fileCheck;
          }
        }
        matchingFiles = matchingDataFiles.toArray(new RawDataFile[0]);
      }
      case BATCH_LAST_FILES -> matchingFiles = Objects.requireNonNullElseGet(batchLastFiles,
          () -> new RawDataFile[0]);
      default -> throw new IllegalStateException("Unexpected value: " + selectionType);
    }

    evaluatedSelection = new RawDataFilePlaceholder[matchingFiles.length];
    for (int i = 0; i < matchingFiles.length; i++) {
      RawDataFile matchingFile = matchingFiles[i];
      evaluatedSelection[i] = new RawDataFilePlaceholder(matchingFile);
    }
    logger.finest(
        () -> "Setting file selection. Evaluated files: " + Arrays.toString(evaluatedSelection));

    return matchingFiles;
  }

  public RawDataFilesSelectionType getSelectionType() {
    return selectionType;
  }

  public void setSelectionType(RawDataFilesSelectionType selectionType) {
    this.selectionType = selectionType;
  }

  public void resetSelection() {
    if (evaluatedSelection != null) {
      logger.finest(() -> "Resetting file selection. Previously evaluated files: " + Arrays
          .toString(evaluatedSelection));
    }
    evaluatedSelection = null;
  }

  RawDataFile[] getSpecificFiles() {
    MZmineProject currentProject = MZmineCore.getProjectManager().getCurrentProject();
    if (currentProject == null) {
      return new RawDataFile[0];
    }

    if (specificFiles == null) {
      return null;
    }

    return Arrays.stream(specificFiles).<RawDataFile>mapMulti((specificFile, c) -> {
      for (RawDataFile file : MZmineCore.getProjectManager().getCurrentProject()
          .getCurrentRawDataFiles()) {
        if (file.getName().equals(specificFile.getName()) && (file.getAbsolutePath() == null
                                                              || specificFile.getAbsolutePath()
                                                                 == null || file.getAbsolutePath()
                                                                  .equals(
                                                                      specificFile.getAbsolutePath()))) {
          c.accept(file);
          break;
        }
      }
    }).toArray(RawDataFile[]::new);
  }

  public void setSpecificFiles(RawDataFile[] specificFiles) {
    resetSelection();
    RawDataFilePlaceholder[] placeholder = new RawDataFilePlaceholder[specificFiles.length];
    for (int i = 0; i < specificFiles.length; i++) {
      RawDataFile specificFile = specificFiles[i];
      placeholder[i] = new RawDataFilePlaceholder(specificFile);
    }
    this.specificFiles = placeholder;
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
    this.batchLastFiles = batchLastFiles;
  }

  public RawDataFilesSelection clone() {
    RawDataFilesSelection newSelection = new RawDataFilesSelection();
    newSelection.selectionType = selectionType;
    newSelection.specificFiles = specificFiles;
    newSelection.namePattern = namePattern;
    return newSelection;
  }

  public RawDataFilesSelection cloneAndKeepSelection() {
    RawDataFilesSelection newSelection = new RawDataFilesSelection();
    newSelection.selectionType = selectionType;
    newSelection.specificFiles = specificFiles;
    newSelection.namePattern = namePattern;
    newSelection.evaluatedSelection = evaluatedSelection;
    return newSelection;
  }

  public String toString() {
    if (evaluatedSelection != null) {
      StringBuilder str = new StringBuilder();
      RawDataFile[] files = getEvaluationResult();
      for (int i = 0; i < files.length; i++) {
        if (i > 0) {
          str.append("\n");
        }
        str.append(files[i].getName());
      }
      return str.toString();
    }
    return "Evaluation not executed.";
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

    if (getSelectionType() != that.getSelectionType() || !Objects
        .equals(getNamePattern(), that.getNamePattern())) {
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

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

package io.github.mzmine.util;

import com.Ostermiller.util.CSVParser;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.DoubleType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CSVParsingUtils {

  private static final Logger logger = Logger.getLogger(
      CSVParsingUtils.class.getName());

  /**
   * Searches an array of strings for a specified list of import types. Returns
   * all selected import types or null if they were found.
   *
   * @param importTypes  A list of {@link ImportType}s. Only if a type
   *                     {@link ImportType#isSelected()}, it will be included in
   *                     the output list.
   * @param firstLine    The column headers
   * @param errorMessage A string property to place an error message on failure.
   *                     Stored property is null unless an error occurs.
   * @return A new list of the selected import types with their line indices
   * set, or null if a selected column was not found.
   */
  @Nullable
  public static List<ImportType> findLineIds(List<ImportType> importTypes,
      String[] firstLine, @NotNull StringProperty errorMessage) {
    List<ImportType> lines = new ArrayList<>();
    for (ImportType importType : importTypes) {
      if (importType.isSelected()) {
        ImportType type = new ImportType(importType.isSelected(),
            importType.getCsvColumnName(), importType.getDataType());
        lines.add(type);
      }
    }

    for (ImportType importType : lines) {
      for (int i = 0; i < firstLine.length; i++) {
        String columnName = firstLine[i];
        if (columnName.trim()
            .equalsIgnoreCase(importType.getCsvColumnName().trim())) {
          if (importType.getColumnIndex() != -1) {
            logger.warning(
                () -> "Library file contains two columns called \"" + columnName
                    + "\".");
          }
          importType.setColumnIndex(i);
        }
      }
    }
    final List<ImportType> nullMappings = lines.stream()
        .filter(val -> val.getColumnIndex() == -1).toList();
    if (!nullMappings.isEmpty()) {
      final String error = "Did not find specified column " + Arrays.toString(
          nullMappings.stream().map(ImportType::getCsvColumnName).toArray())
          + " in file.";
      logger.warning(() -> error);
      errorMessage.set(error);
      return null;
    }
    return lines;
  }

  public static CompoundDBAnnotation csvLineToCompoundDBAnnotation(
      final String[] line, final List<ImportType> types) {

    final CompoundDBAnnotation annotation = new SimpleCompoundDBAnnotation();
    for (ImportType type : types) {
      final int columnIndex = type.getColumnIndex();

      if (columnIndex == -1 || line[columnIndex] == null
          || line[columnIndex].isBlank()) {
        continue;
      }

      try {
        switch (type.getDataType()) {
          case FloatType ft ->
              annotation.put(ft, Float.parseFloat(line[columnIndex]));
          case IntegerType it ->
              annotation.put(it, Integer.parseInt(line[columnIndex]));
          case DoubleType dt ->
              annotation.put(dt, Double.parseDouble(line[columnIndex]));
          case IonAdductType iat -> {
            final IonType ionType = IonType.parseFromString(line[columnIndex]);
            annotation.put(IonTypeType.class, ionType);
          }
          case StringType st -> annotation.put(st, line[columnIndex]);
          default -> throw new RuntimeException(
              "Don't know how to parse data type " + type.getDataType()
                  .getClass().getName());
        }
      } catch (NumberFormatException e) {
        // silent - e.g. #NV from excel
      }
    }

    // try to calculate the neutral mass if it was not present.
    if (annotation.get(NeutralMassType.class) == null) {
      annotation.putIfNotNull(NeutralMassType.class,
          CompoundDBAnnotation.calcNeutralMass(annotation));
    }

    return annotation;
  }

  /**
   * Reads the given csv file and returns a {@link CompoundDbLoadResult}
   * containing a list of valid (see
   * {@link CompoundDBAnnotation#isBaseAnnotationValid(CompoundDBAnnotation,
   * boolean)} annotations or an error message.
   *
   * @param peakListFile   The csv file.
   * @param fieldSeparator The field separator.
   * @param types          The list of possible import types. All types that are
   *                       selected ({@link ImportType#isSelected()} must be
   *                       found in the csv file.
   * @param ionLibrary     An ion library or null. If the library is null, a
   *                       precursor mz must be given in the file. Otherwise,
   *                       all formulas will be ionised by
   *                       {@link
   *                       CompoundDBAnnotation#buildCompoundsWithAdducts(CompoundDBAnnotation,
   *                       IonNetworkLibrary)}.
   */
  public static CompoundDbLoadResult getAnnotationsFromCsvFile(
      final File peakListFile, char fieldSeparator,
      @NotNull List<ImportType> types, @Nullable IonNetworkLibrary ionLibrary) {
    try (final FileReader dbFileReader = new FileReader(peakListFile)) {
      List<CompoundDBAnnotation> list = new ArrayList<>();

      String[][] peakListValues = CSVParser.parse(dbFileReader, fieldSeparator);

      final SimpleStringProperty errorMessage = new SimpleStringProperty();
      final List<ImportType> lineIds = CSVParsingUtils.findLineIds(types,
          peakListValues[0], errorMessage);

      if (lineIds == null) {
        return new CompoundDbLoadResult(List.of(), TaskStatus.ERROR,
            errorMessage.get());
      }

      for (int i = 1; i < peakListValues.length; i++) {
        final CompoundDBAnnotation baseAnnotation = CSVParsingUtils.csvLineToCompoundDBAnnotation(
            peakListValues[i], lineIds);

        if (!CompoundDBAnnotation.isBaseAnnotationValid(baseAnnotation,
            ionLibrary != null)) {
          logger.info(String.format(
              "Invalid base annotation for compound %s in line %d. Skipping annotation.",
              baseAnnotation, i));
          continue;
        }

        if (ionLibrary != null) {
          final List<CompoundDBAnnotation> ionizedAnnotations = CompoundDBAnnotation.buildCompoundsWithAdducts(
              baseAnnotation, ionLibrary);
          list.addAll(ionizedAnnotations);
        } else {
          list.add(baseAnnotation);
        }
      }

      if (list.isEmpty()) {
        return new CompoundDbLoadResult(List.of(), TaskStatus.ERROR,
            "Did not find any valid compounds in file.");
      }

      return new CompoundDbLoadResult(list, TaskStatus.FINISHED, null);

    } catch (Exception e) {
      logger.log(Level.WARNING, "Could not read file " + peakListFile, e);
      return new CompoundDbLoadResult(List.of(), TaskStatus.ERROR,
          e.getMessage());
    }
  }

  public record CompoundDbLoadResult(
      @NotNull List<CompoundDBAnnotation> annotations,
      @NotNull TaskStatus status, @Nullable String errorMessage) {

  }


}

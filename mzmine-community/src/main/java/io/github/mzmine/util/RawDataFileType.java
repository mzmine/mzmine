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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.util.files.ExtensionFilters;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * Enum of supported data file formats
 */
public enum RawDataFileType {

  MZML(ExtensionFilters.MZML, false), //
  IMZML(ExtensionFilters.IMZML, false), //
  MZML_IMS(ExtensionFilters.MZML, false), //
  MZXML(ExtensionFilters.MZXML, false), //
  MZDATA(ExtensionFilters.MZDATA, false), //
  NETCDF(ExtensionFilters.NETCDF, false), //
  THERMO_RAW(ExtensionFilters.THERMO_OR_WATERS_RAW, false), //
  WATERS_RAW(ExtensionFilters.THERMO_OR_WATERS_RAW, true), //
  WATERS_RAW_IMS(ExtensionFilters.THERMO_OR_WATERS_RAW, true), //
  MZML_ZIP(ExtensionFilters.MZML_ZIP_GZIP, false), //
  MZML_GZIP(ExtensionFilters.MZML_ZIP_GZIP, false), //
  ICPMSMS_CSV(ExtensionFilters.CSV, false), //
  BRUKER_TDF(ExtensionFilters.BRUKER_OR_AGILENT_D, true), //
  BRUKER_TSF(ExtensionFilters.BRUKER_OR_AGILENT_D, true), //
  BRUKER_BAF(ExtensionFilters.BRUKER_OR_AGILENT_D, true), //
  //  AIRD, //
  SCIEX_WIFF(ExtensionFilters.WIFF, false), //
  SCIEX_WIFF2(ExtensionFilters.WIFF2, false), //
  AGILENT_D(ExtensionFilters.BRUKER_OR_AGILENT_D, true), //
  AGILENT_D_IMS(ExtensionFilters.BRUKER_OR_AGILENT_D, true);


  private final ExtensionFilter extensionFilter;
  private final boolean isFolder;

  RawDataFileType(ExtensionFilter extensionFilter, boolean isFolder) {
    this.extensionFilter = extensionFilter;
    this.isFolder = isFolder;
  }

  public static List<RawDataFileType> getAllFolderTypes() {
    return Arrays.stream(values()).filter(RawDataFileType::isFolder).toList();
  }

  public static List<RawDataFileType> getAllNonFolderTypes() {
    return Arrays.stream(values()).filter(rawDataFileType -> !rawDataFileType.isFolder()).toList();
  }

  public static List<File> getAdditionalRequiredFiles(RawDataFile raw) {
    final File file = raw.getAbsoluteFilePath();
    final RawDataFileType type = RawDataFileTypeDetector.detectDataFileType(file);

    return switch (type) {
      case MZML, MZXML, MZML_IMS, MZDATA, NETCDF, THERMO_RAW, MZML_ZIP, MZML_GZIP, ICPMSMS_CSV,
           BRUKER_TDF, BRUKER_TSF, BRUKER_BAF, AGILENT_D, AGILENT_D_IMS, WATERS_RAW,
           WATERS_RAW_IMS -> List.of();
      case IMZML -> {
        final String extension = FileAndPathUtil.getExtension(file.getName());
        yield List.of(new File(file.getParent(), file.getName().replace(extension, "ibd")));
      }
      case SCIEX_WIFF, SCIEX_WIFF2 -> {
        final String extension = FileAndPathUtil.getExtension(file.getName());
        yield List.of(new File(file.getParent(), file.getName().replace(extension, "wiff.scan")));
      }
    };
  }

  public ExtensionFilter getExtensionFilter() {
    return extensionFilter;
  }

  public boolean isFolder() {
    return isFolder;
  }
}

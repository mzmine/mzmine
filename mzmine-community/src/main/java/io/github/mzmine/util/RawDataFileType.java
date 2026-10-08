/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.util.files.ExtensionFilters;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

/**
 * Enum of supported data file formats
 */
public enum RawDataFileType implements UniqueIdSupplier {

  MZML(ExtensionFilters.MZML, false), //
  IMZML(ExtensionFilters.IMZML, false), //
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
  AGILENT_D_IMS(ExtensionFilters.BRUKER_OR_AGILENT_D, true), //
  SHIMADZU_LCD(ExtensionFilters.SHIMADZU, false), //
  MBI(ExtensionFilters.MBI, false);


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
      case MZML, MZXML, MZDATA, NETCDF, THERMO_RAW, MZML_ZIP, MZML_GZIP, ICPMSMS_CSV, BRUKER_TDF,
           BRUKER_TSF, BRUKER_BAF, AGILENT_D, AGILENT_D_IMS, WATERS_RAW, WATERS_RAW_IMS,
           SHIMADZU_LCD, MBI -> List.of();
      case IMZML -> {
        final String extension = FileAndPathUtil.getExtension(file.getName());
        yield List.of(new File(file.getParent(), file.getName().replace(extension, "ibd")));
      }
      case SCIEX_WIFF, SCIEX_WIFF2 -> {
        final String extension = FileAndPathUtil.getExtension(file.getName());
        yield List.of(new File(file.getParent(), file.getName().replace(extension, "wiff.scan")),
            new File(file.getParent(), file.getName().replace(extension, "timeseries.data")));
      }
    };
  }

  public ExtensionFilter getExtensionFilter() {
    return extensionFilter;
  }

  public boolean isFolder() {
    return isFolder;
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case IMZML -> "imzml";
      case MZXML -> "mzxml";
      case MZDATA -> "mzdata";
      case NETCDF -> "netcdf";
      case THERMO_RAW -> "raw_thermo";
      case WATERS_RAW -> "raw_waters";
      case WATERS_RAW_IMS -> "raw_waters_ims";
      case MZML_ZIP -> "mzml_zip";
      case MZML_GZIP -> "mzml_gzip";
      case ICPMSMS_CSV -> "csv_icpms";
      case BRUKER_TDF -> "tdf";
      case BRUKER_TSF -> "tsf";
      case BRUKER_BAF -> "baf";
      case SCIEX_WIFF -> "wiff";
      case SCIEX_WIFF2 -> "wiff2";
      case AGILENT_D -> "d_agilent";
      case AGILENT_D_IMS -> "d_agilent_ims";
      case SHIMADZU_LCD -> "lcd";
      case MBI -> "mbi";
      case MZML -> "mzml";
    };
  }
}

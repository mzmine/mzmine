package io.github.mzmine.modules.io.export_rawdata_netcdf;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import io.github.msdk.MSDKRuntimeException;
import io.github.msdk.datamodel.FileType;
import io.github.msdk.datamodel.SimpleRawDataFile;
import ucar.nc2.NetcdfFile;

/**
 * <p>NetCDFRawDataFile class.</p>
 *
 */
public class NetCDFRawDataFile extends SimpleRawDataFile {

  private NetcdfFile inputFile;

  /**
   * <p>Constructor for NetCDFRawDataFile.</p>
   *
   * @param rawDataFileName a {@link String} object.
   * @param originalRawDataFile a {@link Optional} object.
   * @param rawDataFileType a {@link FileType} object.
   * @param inputFile a {@link NetcdfFile} object.
   */
  public NetCDFRawDataFile(String rawDataFileName, Optional<File> originalRawDataFile,
      FileType rawDataFileType, NetcdfFile inputFile) {
    super(rawDataFileName, originalRawDataFile, rawDataFileType);
    this.inputFile = inputFile;
  }

  /** {@inheritDoc} */
  @Override
  public void dispose() {
    try {
      inputFile.close();
    } catch (IOException e) {
      new MSDKRuntimeException(e);
    }
    super.dispose();
  }

}

/*
 * (C) Copyright 2015-2018 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.mzmine.modules.dataprocessing.id_sirius;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.msdk.MSDKException;

/**
 * <p>
 * Class NativeLibraryLoader
 * </p>
 * This class allows to dynamically load native libraries from .jar files (also works with IDE) with
 * updating java.library.path variable
 */
public class NativeLibraryLoader {

  private static final Logger logger = LoggerFactory.getLogger(NativeLibraryLoader.class);

  private static final String GLPK_RESOURCES_FOLDER = "glpk-4.60";

  private NativeLibraryLoader() {}

  /**
   * GLPK java wrapper is a bit stupid and REQUIRES the glpk libs to be present on the
   * java.library.path, otherwise it will crash during initialization. So this method will copy
   * those libraries from resources into the first writable folder in java.library.path
   * 
   */
  public static void loadNativeGLPKLibriaries() throws MSDKException, IOException {

    logger.debug("Started loading GLPK libraries");

    final String javaLibPath = System.getProperty("java.library.path");
    if (javaLibPath == null)
      throw new MSDKException("Cannot read java.library.path system property");
    logger.debug("java.library.path = " + javaLibPath);

    final String javaLibPathSplit[] = javaLibPath.split(File.pathSeparator);
    
    // Find the first writable folder
    for (String libDirectory : javaLibPathSplit) {
      final Path libPath = Paths.get(libDirectory);
      // Files.isWritable is more reliable then File.canWrite()
      // See https://bugs.openjdk.java.net/browse/JDK-8148211
      if (Files.exists(libPath) && Files.isWritable(libPath)) {
        copyGLPKLibraryFiles(libDirectory);
        return;
      }
    }

    // Could not find a suitable library path
    throw new MSDKException(
        "The java.library.path system property does not contain any writable folders, cannot copy GLPK libraries (java.library.path = "
            + javaLibPath + ")");
  }

  public static void copyGLPKLibraryFiles(final String targetLibraryPath)
      throws MSDKException, IOException {

    final String arch = getArch();
    final String osname = getOsName();
    logger.debug("OS type = {} and OS arch = {}", osname, arch);

    // GLPK requires two libraries
    final String[] requiredLibraryFiles = new String[2];
    switch (osname) {
      case "windows":
        requiredLibraryFiles[0] = "glpk_4_60.dll";
        requiredLibraryFiles[1] = "glpk_4_60_java.dll";
        break;
      case "linux":
        requiredLibraryFiles[0] = "libglpk.so.40";
        requiredLibraryFiles[1] = "libglpk_java.so";
        break;
      case "mac":
        requiredLibraryFiles[0] = "libglpk.dylib";
        requiredLibraryFiles[1] = "libglpk_java.dylib";
        break;
      default:
        throw new MSDKException("Unsupported OS (" + osname + "), cannot load GLPK libraries");
    }

    for (String libName : requiredLibraryFiles) {

      // Resources are always separated by "/", even on Windows
      final String libResourcePath = GLPK_RESOURCES_FOLDER + "/" + osname + arch + "/" + libName;

      // Open library file as InputStream
      InputStream inFileStream =
          NativeLibraryLoader.class.getClassLoader().getResourceAsStream(libResourcePath);
      if (inFileStream == null)
        throw new MSDKException("Failed to open resource " + libResourcePath);

      // Target filename where the library will be copied
      final Path targetLibFilePath =
          FileSystems.getDefault().getPath(targetLibraryPath, libName).toAbsolutePath();

      // Copy the library file
      logger.debug("Copying library file " + libResourcePath + " to " + targetLibFilePath);
      Files.copy(inFileStream, targetLibFilePath, StandardCopyOption.REPLACE_EXISTING);

      // Close the input
      inFileStream.close();

    }

  }



  /**
   * <p>
   * Method returns architecture of the computer, 32 or 64
   * </p>
   * 
   * @return formatted architecture
   * @throws MSDKException - if any
   */
  private static String getArch() throws MSDKException {
    final String arch = System.getProperty("os.arch");
    if (arch == null)
      throw new MSDKException("Can not identify os.arch property");

    return arch.endsWith("64") ? "64" : "32";
  }

  /**
   * <p>
   * Method returns formatted OS name - windows, linux, mac, or unknown
   * </p>
   * 
   * @return OS name
   * @throws MSDKException - if any
   */
  public static String getOsName() throws MSDKException {
    final String osname = System.getProperty("os.name");

    if (osname == null)
      return "unknown";

    if (osname.toLowerCase().contains("win")) {
      return "windows";
    }
    if (osname.toLowerCase().contains("linux")) {
      return "linux";
    }
    if (osname.toLowerCase().contains("mac")) {
      return "mac";
    }

    return "unknown";
  }


}

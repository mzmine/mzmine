/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ZIP related utilities
 */
public class ZipUtils {

  /**
   * Known issue to unzip all assets download from zenodo. Somehow generates error. Use
   * {@link #unzipFile(File, File)} instead
   *
   * @param zipStream
   * @param destinationFolder
   * @return
   * @throws IOException
   */
  public static List<File> unzipStream(ZipInputStream zipStream, File destinationFolder)
      throws IOException {

    if (!destinationFolder.exists()) {
      destinationFolder.mkdirs();
    }

    List<File> files = new ArrayList<>();

    ZipEntry entry;
    int readLen;
    byte readBuffer[] = new byte[10000000];
    while ((entry = zipStream.getNextEntry()) != null) {
      File extractedFile = new File(destinationFolder, entry.getName());
      if (entry.isDirectory()) {
        extractedFile.mkdirs();
        continue;
      }
      try (FileOutputStream outputStream = new FileOutputStream(extractedFile)) {
        while ((readLen = zipStream.read(readBuffer)) != -1) {
          outputStream.write(readBuffer, 0, readLen);
        }
        outputStream.close();
        extractedFile.setExecutable(true);
        files.add(extractedFile);
      }
    }
    return files;
  }

  /**
   * @param stream
   * @param dir
   * @param destPath
   * @throws IOException
   */
  public static void zipDirectory(@NotNull ZipOutputStream stream, @NotNull File dir,
      @Nullable String destPath) throws IOException {
    if (!dir.isDirectory()) {
      return;
    }

    final File[] files = dir.listFiles();
    if (files == null) {
      return;
    }

    destPath = (destPath == null) ? "" : destPath;
    if (!destPath.endsWith("/")) {
      destPath = destPath + "/";
    }

    for (final File file : files) {
      if (file.isDirectory()) {
        zipDirectory(stream, file, destPath + file.getName());
      }

      if (file.isFile()) {
        stream.putNextEntry(new ZipEntry(destPath + file.getName()));
        final StreamCopy copy = new StreamCopy();
        try (final FileInputStream inputStream = new FileInputStream(file)) {
          copy.copy(inputStream, stream);
        }
      }
    }
  }

  public static void unzipDirectory(String folder, ZipFile zipFile, File destinationFolder)
      throws IOException {
    int readLen;
    byte readBuffer[] = new byte[10000000];

    ZipEntry entry = null;
    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    while (entries.hasMoreElements()) {
      entry = entries.nextElement();

      // only extract the given folder
      if (!entry.getName().startsWith(folder)) {
        continue;
      }

      File extractedFile = new File(destinationFolder, entry.getName());
      if (!extractedFile.toPath().normalize().startsWith(destinationFolder.toPath())) {
        throw new IllegalArgumentException("Bad zip entry.");
      }

      if (entry.isDirectory()) {
        extractedFile.mkdirs();
        continue;
      }
      if (!extractedFile.exists()) {
        if (!extractedFile.getParentFile().exists()) {
          extractedFile.getParentFile().mkdirs();
        }
        extractedFile.createNewFile();
      }

      final InputStream zipStream = zipFile.getInputStream(entry);
      try (FileOutputStream outputStream = new FileOutputStream(extractedFile)) {
        while ((readLen = zipStream.read(readBuffer)) != -1) {
          outputStream.write(readBuffer, 0, readLen);
        }
        outputStream.flush();
        outputStream.close();
      } catch (IOException e) {
        zipStream.close();
      }
      zipStream.close();
    }
  }

  /**
   * Uzips a file to an output directory
   *
   * @param file         zip file
   * @param outDirectory output directory
   * @return List of extracted files
   * @throws IOException on IO error or if zip file may be malicious
   */
  public static List<File> unzipFile(final File file, final File outDirectory) throws IOException {
    List<File> results = new ArrayList<>();
    Path outPath = outDirectory.toPath();
    try (org.apache.commons.compress.archivers.zip.ZipFile zipFile = org.apache.commons.compress.archivers.zip.ZipFile.builder()
        .setFile(file).get()) {
      Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
      while (entries.hasMoreElements()) {
        ZipArchiveEntry entry = entries.nextElement();
        String entryName = entry.getName();
        if (entryName.contains("..")) {
          throw new IOException("Malicious zip entry: " + entryName);
        }
        Path outputFile = outPath.resolve(entryName).toAbsolutePath();

        if (entry.isDirectory()) {
          Files.createDirectories(outputFile);
        } else {
          Path parentFile = outputFile.getParent();
          if (parentFile == null) {
            throw new AssertionError("Parent path should never be null: " + file);
          }

          Files.createDirectories(parentFile);
          Files.copy(zipFile.getInputStream(entry), outputFile,
              StandardCopyOption.REPLACE_EXISTING);
          results.add(outputFile.toFile());
        }
      }
    }
    return results;
  }
}

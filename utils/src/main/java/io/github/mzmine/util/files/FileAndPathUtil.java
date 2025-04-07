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

package io.github.mzmine.util.files;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.SPARSE;
import static java.nio.file.StandardOpenOption.WRITE;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.stage.FileChooser.ExtensionFilter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simple file operations
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class FileAndPathUtil {

  private static final Logger logger = Logger.getLogger(FileAndPathUtil.class.getName());
  private final static File USER_MZMINE_DIR = new File(FileUtils.getUserDirectory(), ".mzmine/");

  // changed on other thread so make volatile
  // flag to delete temp files as soon as possible
  private static volatile boolean earlyTempFileCleanup = true;
  private static volatile File MZMINE_TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));

  /**
   * Count the number of lines in a text file (should be seconds even for large files)
   *
   * @param fileName target file
   * @return the number of lines in file or -1 if the file does not exist or is not readable
   * @throws IOException see {@link Paths#get(String, String...)}
   */
  public static long countLines(String fileName) throws IOException {
    return countLines(Paths.get(fileName));
  }

  /**
   * Count the number of lines in a text file (should be seconds even for large files)
   *
   * @param file target file
   * @return the number of lines in file or -1 if the file does not exist or is not readable
   * @throws IOException see {@link Paths#get(String, String...)}
   */
  public static long countLines(File file) throws IOException {
    return countLines(file.toPath());
  }

  /**
   * Count the number of lines in a text file (should be seconds even for large files)
   *
   * @param file target file
   * @return the number of lines in file or -1 if the file does not exist or is not readable
   * @throws IOException see {@link Paths#get(String, String...)}
   */
  public static long countLines(Path file) throws IOException {
    if (Files.exists(file) && Files.isRegularFile(file)) {
      try (final var lines = Files.lines(file)) {
        return lines.count();
      }
    } else {
      return -1;
    }
  }

  /**
   * Returns the real file path as path/filename.fileformat
   *
   * @param path   the file path (directory of filename)
   * @param name   filename
   * @param format a format starting with a dot for example: .pdf ; or without a dot: pdf
   * @return path/name.format
   */
  public static File getRealFilePath(File path, String name, String format) {
    return new File(getFolderOfFile(path), getRealFileName(name, format));
  }

  /**
   * Returns the real file path as path/filename.fileformat
   *
   * @param filepath a filepath with file name
   * @param format   a format starting with a dot for example: .pdf ; or without a dot: pdf
   * @return filepath.format (the format will be exchanged by this format)
   */
  public static File getRealFilePath(File filepath, String format) {
    return new File(filepath.getParentFile(), getRealFileName(filepath.getName(), format));
  }


  /**
   * Adds a suffix to the filename and replaces the format
   */
  public static File getRealFilePathWithSuffix(File outputFile, String suffix, String format) {
    File out = eraseFormat(outputFile);
    return getRealFilePath(outputFile, out.getName() + suffix, format);
  }

  /**
   * Creates a new file path that add a suffix to the filename and reuses the old format if present
   */
  public static File getRealFilePathWithSuffix(File outputFile, String suffix) {
    var format = getFormat(outputFile);
    return getRealFilePathWithSuffix(outputFile, suffix, format);
  }

  /**
   * Returns the real file name as filename.fileformat
   *
   * @param name   filename
   * @param format a format starting with a dot for example .pdf
   * @return name.format
   */
  public static String getRealFileName(String name, String format) {
    String result = FilenameUtils.removeExtension(name);
    result = addFormat(result, format);
    return result;
  }

  /**
   * Returns the real file name as filename.fileformat
   *
   * @param name   filename
   * @param format a format starting with a dot for example .pdf
   * @return filename.format
   */
  public static String getRealFileName(File name, String format) {
    return getRealFileName(name.getAbsolutePath(), format);
  }

  /**
   * @param f target file
   * @return The file extension or null.
   */
  public static String getExtension(String f) {
    return FilenameUtils.getExtension(f);
  }

  /**
   * @param f target file
   * @return The file extension or null.
   */
  public static String getExtension(File f) {
    return getExtension(f.getName());
  }

  /**
   * @param f target file
   * @return The file extension or null.
   */
  public static String getFormat(File f) {
    return getExtension(f.getName());
  }

  /**
   * @param f target file
   * @return The file extension or null.
   */
  public static String getFormat(String f) {
    return getExtension(f);
  }

  /**
   * erases the format. "image.png" will be returned as "image" this method is used by
   * getRealFilePath and getRealFileName
   *
   * @return remove format from file
   */
  public static File eraseFormat(File f) {
    return new File(FilenameUtils.removeExtension(f.getPath()));
  }

  /**
   * erases the format. "image.png" will be returned as "image" this method is used by
   * getRealFilePath and getRealFileName
   *
   * @return remove format from file
   */
  public static String eraseFormat(String f) {
    return FilenameUtils.removeExtension(f);
  }

  /**
   * Adds the format. "image" will be returned as "image.format" Maybe use erase format first. this
   * method is used by getRealFilePath and getRealFileName
   *
   * @param name   a file name without format (use {@link #eraseFormat(File)} before)
   * @param format the new format
   * @return name.format
   */
  public static String addFormat(String name, String format) {
    if (format == null || format.isBlank()) {
      return name;
    }
    if (format.startsWith(".")) {
      return name + format;
    } else {
      return name + "." + format;
    }
  }

  /**
   * Returns the file if it is already a folder. Or the parent folder if the file is a data file
   *
   * @param file file or folder
   * @return the parameter file if it is a directory, otherwise its parent directory
   */
  public static File getFolderOfFile(File file) {
    if (file.isDirectory()) {
      return file;
    } else {
      return file.getParentFile();
    }
  }

  /**
   * Returns the file name from a given file. If file is a folder an empty String is returned
   *
   * @param file a path
   * @return the filename of a file
   */
  public static String getFileNameFromPath(File file) {
    if (file.isDirectory()) {
      return "";
    } else {
      return file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("\\") + 1);
    }
  }

  /**
   * Returns the file name from a given file. If the file is a folder an empty String is returned
   *
   * @return the filename from a path
   */
  public static String getFileNameFromPath(String file) {
    return getFileNameFromPath(new File(file));
  }

  /**
   * Creates a new directory.
   *
   * @param theDir target directory
   * @return true if directory existed or was created
   */
  public static boolean createDirectory(File theDir) {
    // if the directory does not exist, create it
    if (theDir.exists()) {
      return true;
    } else {
      try {
        return theDir.mkdirs();
      } catch (SecurityException se) {
        // handle it
      }
      return false;
    }
  }

  /**
   * Sort an array of files These files have to start or end with a number
   *
   * @param files files to sort
   * @return sorted array of files
   */
  public static File[] sortFilesByNumber(File[] files, boolean reverse) {
    try {
      Comparator<File> fileNumberComparator = new Comparator<>() {
        private Boolean endsWithNumber = null;

        @Override
        public int compare(File o1, File o2) {
          try {
            if (endsWithNumber == null) {
              endsWithNumber = checkEndsWithNumber(o1.getName());
            }
            int n1 = extractNumber(o1, endsWithNumber);
            int n2 = extractNumber(o2, endsWithNumber);
            return Integer.compare(n1, n2);
          } catch (Exception e) {
            return o1.compareTo(o2);
          }
        }
      };
      if (reverse) {
        fileNumberComparator = fileNumberComparator.reversed();
      }
      Arrays.sort(files, fileNumberComparator);
      return files;
    } catch (Exception ex) {
      return files;
    }
  }

  public static boolean isNumber(char c) {
    return (c >= '0' && c <= '9');
  }

  /**
   * @param file target file
   * @return true if the name ends with a number (e.g., file1.mzML=true)
   */
  public static boolean checkEndsWithNumber(File file) {
    return checkEndsWithNumber(file.getName());
  }

  /**
   * @param name filename
   * @return true if it ends with a number
   */
  public static boolean checkEndsWithNumber(String name) {
    // ends with number?
    int e = name.lastIndexOf('.');
    e = e == -1 ? name.length() : e;
    return isNumber(name.charAt(e - 1));
  }

  public static int extractNumber(File file, boolean endsWithNumber)
      throws IllegalArgumentException {
    return extractNumber(file.getName(), endsWithNumber);
  }

  public static int extractNumber(String name, boolean endsWithNumber)
      throws IllegalArgumentException {
    if (endsWithNumber) {
      // ends with number?
      int e = name.lastIndexOf('.');
      e = e == -1 ? name.length() : e;
      int f = e - 1;
      for (; f > 0; f--) {
        if (!isNumber(name.charAt(f))) {
          f++;
          break;
        }
      }
      //
      if (f < 0) {
        f = 0;
      }
      if (f - e == 0) {
        // there was no number
        throw new IllegalArgumentException(name + " does not end with number");
      }
      String number = name.substring(f, e);
      return Integer.parseInt(number);
    } else {
      int f = 0;
      for (; f < name.length(); f++) {
        if (!isNumber(name.charAt(f))) {
          break;
        }
      }
      if (f == 0) {
        // there was no number
        throw new IllegalArgumentException(name + " does not start with number");
      }
      String number = name.substring(0, f);
      return Integer.parseInt(number);
    }
  }

  /**
   * Lists all directories in directory f
   *
   * @param f directory
   * @return all sub directories
   */
  public static File[] getSubDirectories(File f) {
    return f.listFiles(File::isDirectory);
  }

  // ###############################################################################################
  // search for files

  public static List<File[]> findFilesInDir(File dir, ExtensionFilter fileFilter) {
    return findFilesInDir(dir, fileFilter, true);
  }

  public static List<File[]> findFilesInDir(File dir, ExtensionFilter fileFilter,
      boolean searchSubdir) {
    return findFilesInDir(dir, new FileTypeFilter(fileFilter, ""), searchSubdir, false);
  }

  /**
   * Flat array of all files in directory and sub directories that match the filter
   *
   * @param dir          parent directory
   * @param fileFilter   filter for file extensions
   * @param searchSubdir include all sub directories
   */
  public static File[] findFilesInDirFlat(File dir, ExtensionFilter fileFilter,
      boolean searchSubdir) {
    return findFilesInDir(dir, new FileTypeFilter(fileFilter, ""), searchSubdir, false).stream()
        .flatMap(Arrays::stream).filter(Objects::nonNull)
        .sorted(Comparator.comparing(File::getAbsolutePath)).toArray(File[]::new);
  }

  /**
   * @param dir        parent directory
   * @param fileFilter filter files
   * @return list of all files in directory and sub directories
   */
  public static List<File[]> findFilesInDir(File dir, FileFilter fileFilter) {
    return findFilesInDir(dir, fileFilter, true, false);
  }

  public static List<File[]> findFilesInDir(File dir, FileFilter fileFilter, boolean searchSubdir) {
    return findFilesInDir(dir, fileFilter, searchSubdir, false);
  }

  public static List<File[]> findFilesInDir(File dir, FileFilter fileFilter, boolean searchSubdir,
      boolean filesInSeparateFolders) {
    File[] subDir = FileAndPathUtil.getSubDirectories(dir);
    // result: each vector element stands for one file
    List<File[]> list = new ArrayList<>();
    // add all files as first
    // sort all files and return them
    File[] files = dir.listFiles(fileFilter);
    if (files != null && files.length > 0) {
      files = FileAndPathUtil.sortFilesByNumber(files, false);
      list.add(files);
    }

    if (subDir == null || subDir.length <= 0 || !searchSubdir) {
      // no subdir end directly
      return list;
    } else {
      // sort dirs
      subDir = FileAndPathUtil.sortFilesByNumber(subDir, false);
      // go in all sub and subsub... folders to find files
      if (filesInSeparateFolders) {
        findFilesInSubDirSeparatedFolders(dir, subDir, list, fileFilter);
      } else {
        findFilesInSubDir(subDir, list, fileFilter);
      }
      // return as array (unsorted because they are sorted folder wise)
      return list;
    }
  }

  /**
   * go into all subfolders and find all files and go in further subfolders files stored in separate
   * folders. one line in one folder
   *
   * @param dirs musst be sorted!
   * @param list
   */
  private static void findFilesInSubDirSeparatedFolders(File parent, File[] dirs, List<File[]> list,
      FileFilter fileFilter) {
    // go into folder and find files
    List<File> img = null;
    // each file in one folder
    for (File dir : dirs) {
      // find all suiting files
      File[] subFiles = FileAndPathUtil.sortFilesByNumber(dir.listFiles(fileFilter), false);
      // if there are some suiting files in here directory has been found!
      // create image of these
      // dirs
      if (subFiles.length > 0) {
        if (img == null) {
          img = new ArrayList<>();
        }
        // put them into the list
        img.addAll(Arrays.asList(subFiles));
      } else {
        // search in subfolders for data
        // find all subfolders, sort them and do the same iterative
        File[] subDir = FileAndPathUtil.sortFilesByNumber(FileAndPathUtil.getSubDirectories(dir),
            false);
        // call this method
        findFilesInSubDirSeparatedFolders(dir, subDir, list, fileFilter);
      }
    }
    // add to list
    if (img != null && img.size() > 0) {
      list.add(img.toArray(File[]::new));
    }
  }

  /**
   * Go into all sub-folders and find all files files stored one image in one folder!
   *
   * @param dirs musst be sorted!
   * @param list
   */
  private static void findFilesInSubDir(File[] dirs, List<File[]> list, FileFilter fileFilter) {
    // All files in one folder
    for (File dir : dirs) {
      // find all suiting files
      File[] subFiles = FileAndPathUtil.sortFilesByNumber(dir.listFiles(fileFilter), false);
      // put them into the list
      if (subFiles != null && subFiles.length > 0) {
        list.add(subFiles);
      }
      // find all subfolders, sort them and do the same iterative
      File[] subDir = FileAndPathUtil.sortFilesByNumber(FileAndPathUtil.getSubDirectories(dir),
          false);
      // call this method
      findFilesInSubDir(subDir, list, fileFilter);
    }
  }

  /**
   * The Path of the Jar.
   *
   * @return jar path
   */
  @Nullable
  public static File getPathOfJar() {
    /*
     * File f = new File(System.getProperty("java.class.path")); File dir =
     * f.getAbsoluteFile().getParentFile(); return dir;
     */
    try {
      File jar = new File(
          FileAndPathUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI()
              .getPath());
      return jar.getParentFile();
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * The main directory. Only tested on windows
   *
   * @return
   */
  @Nullable
  public static File getSoftwareMainDirectory() {
    File pathOfJar = FileAndPathUtil.getPathOfJar();
    return pathOfJar == null ? null : pathOfJar.getParentFile();
  }


  @Nullable
  public static File getMzmineDir() {
    return USER_MZMINE_DIR;
  }

  /**
   * Directory for external resources
   */
  public static File resolveInDownloadResourcesDir(String name) {
    return new File(resolveInMzmineDir("external_resources"), name);
  }

  @Nullable
  public static File resolveInMzmineDir(String name) {
    return new File(USER_MZMINE_DIR, name);
  }

  public static File getUniqueFilename(final File parent, final String fileName) {
    final File dir = parent.isDirectory() ? parent : parent.getParentFile();
    final File file = new File(dir + File.separator + fileName);

    if (!file.exists()) {
      return file;
    }

    final String extension = getExtension(file);
    final File noExtension = eraseFormat(file);

    int i = 1;
    File uniqueFile = new File(noExtension.getAbsolutePath() + "(" + i + ")." + extension);
    while (uniqueFile.exists()) {
      i++;
      uniqueFile = new File(noExtension.getAbsolutePath() + "(" + i + ")." + extension);
    }
    return uniqueFile;
  }

  /**
   * Remove all symbols not allowed in path. Replaces with _
   *
   * @param name source name (filename or path)
   * @return path safe string
   */
  public static String safePathEncode(String name) {
    return safePathEncode(name, "_");
  }

  /**
   * Remove all symbols not allowed in path
   *
   * @param name       source name (filename or path)
   * @param replaceStr replace all restricted characters by this str
   * @return path safe string
   */
  public static String safePathEncode(String name, String replaceStr) {
    return name.replaceAll("[^a-zA-Z0-9-_()\\.\\s]", replaceStr);
  }

  /**
   * Three options:
   * <p>
   * 1. a string that defines all files separated by lines \n
   * <p>
   * 2. a .txt text file with files listed separated by newline \n
   * <p>
   * 3. a wildcard path/*.mzML to grab all files
   *
   * @return extracted list of files
   */
  public static File[] parseFileInputArgument(final String input) throws IOException {
    if (input.toLowerCase().endsWith(".txt")) {
      try (var lines = Files.lines(Paths.get(input))) {
        return lines.map(File::new).toArray(File[]::new);
      }
    }

    if (input.contains("*")) {
      return searchWithGlob(input);
    }

    // try to just use the input as files separated by newline
    // matches all new line characters
    return Arrays.stream(input.split("\\R")).map(File::new).toArray(File[]::new);
  }

  /**
   * @param globPath path like path/*.mzML
   * @return all matching files
   */
  public static @NotNull File[] searchWithGlob(String globPath) throws IOException {
    var path = new File(globPath);
//    var path = Paths.get(globPath);
    return searchWithGlob(path.getParentFile().toPath(), path.getName());
  }

  /**
   * @param rootDir parent directory
   * @param pattern pattern like *.mzML
   * @return all matching files
   */
  public static @NotNull File[] searchWithGlob(Path rootDir, String pattern) throws IOException {
    PathMatcher matcher = FileSystems.getDefault()
        .getPathMatcher((pattern.startsWith("glob:") ? "" : "glob:") + pattern);

    try (Stream<Path> stream = Files.walk(rootDir, 1)) {
      return stream.filter(file -> matcher.matches(file.getFileName())).map(Path::toFile)
          .toArray(File[]::new);
    }
  }

  public static File getTempDir() {
    return MZMINE_TEMP_DIR;
  }

  /**
   * Sets the temp dir.
   */
  public static void setTempDir(@NotNull final File path) {
    MZMINE_TEMP_DIR = path;
  }

  /**
   * Creates a temp file in the set mzmine temp directory.
   */
  public static File createTempFile(String prefix, String suffix) throws IOException {
    final File tempFile = Files.createTempFile(MZMINE_TEMP_DIR.toPath(), prefix, suffix).toFile();
    logger.info("Created temp file " + tempFile.getAbsolutePath());
    return tempFile;
  }

  private static final SecureRandom random = new SecureRandom();

  private static Path generateRandomPath(String prefix, String suffix, Path dir) {
    String s = prefix + Long.toUnsignedString(random.nextLong()) + suffix;
    return generatePath(s, dir);
  }

  private static Path generatePath(String filename, Path dir) {
    Path name = dir.getFileSystem().getPath(filename);
    // check that filename does not contain parent directory, this would be invalid string
    if (name.getParent() != null) {
      throw new IllegalArgumentException("filename is invalid and contains parent directory");
    }
    return dir.resolve(name);
  }

  /**
   * Sparse files require SPARSE and CREATE_NEW
   */
  public static final Set<OpenOption> SPARSE_OPEN_OPTIONS = Set.of(CREATE_NEW, SPARSE, READ, WRITE);

  public static MemorySegment memoryMapSparseTempFile(Arena arena, long size)
      throws IOException, AccessDeniedException {
    return memoryMapSparseTempFile("mzmine", ".tmp", getTempDir().toPath(), arena, size);
  }

  /**
   * @param prefix of the filename
   * @param suffix of the filename, usually .tmp
   * @param dir    temp directory to create file in
   * @param arena  an arena to manage the {@link MemorySegment}
   * @param size   The size (in bytes) of the mapped memory backing the memory segment
   * @return a new {@link MemorySegment} that maps the sparse file
   * @throws IOException
   * @throws AccessDeniedException
   */
  public static MemorySegment memoryMapSparseTempFile(String prefix, String suffix, Path dir,
      Arena arena, long size) throws IOException, AccessDeniedException {
    try (var fc = openTempFileChannel(prefix, suffix, dir)) {
      // Create a mapped memory segment managed by the arena
      MemorySegment segment = fc.map(MapMode.READ_WRITE, 0L, size, arena);
      return segment;
    }
  }

  /**
   * @param prefix of the filename
   * @param suffix of the filename, usually .tmp
   * @param dir    temp directory to create file in
   * @return a new {@link MemorySegment} that maps the sparse file
   * @throws IOException
   * @throws AccessDeniedException
   */
  public static FileChannel openTempFileChannel(String prefix, String suffix, Path dir)
      throws IOException, AccessDeniedException {

    // only try to handle a certain number of exceptions.

    int exceptionCounter = 0;
    // filename first
    Path f = generatePath(prefix + suffix, dir);
    // run until successful or exception
    // even if file does not exist in first check - FileChannel.open is best check for success
    while (exceptionCounter < 5) {
      try {
        var channel = FileChannel.open(f, SPARSE_OPEN_OPTIONS);
        f.toFile().deleteOnExit();
        try {
          // channel keeps file alive and we can still memory map and write, read to memory mapped file
          // this will cause the file to disappear directly - tmp folder is empty
          // tested on linux
          // tested on Windows 11:
          // space is still taken from the disk of temp directory, but temp directory is empty
          // GC will remove MemoryMapStorage, Arena, MemorySegment and with this automatically call
          // munmap and unmap the file finally clearing the space on disk after GC
          // TODO test on different platforms
          // TODO macOS
          // TODO wsl
          // TODO docker
          // does not work on exFAT partition like external drive
          // will work the first time but then fail on FileChannel.open the second time with AccessDeniedException
          // NTFS works and apple file system as well
          if (earlyTempFileCleanup) {
            f.toFile().delete();
          }
        } catch (Exception e) {
          exceptionCounter++;
        }
        logger.fine("Open file channel to: " + f.toFile().getAbsolutePath());
        return channel;
      } catch (FileAlreadyExistsException e) {
        // ignore and try next file name
        exceptionCounter++;
      } catch (AccessDeniedException e) {
        exceptionCounter++;
        // on exFAT file system the FileChannel.open throws AccessDeniedException for existing files
        // maybe because sparse files are not supported and the direct delete triggers issues
        // therefore only throw exception if the file does not exist
        if (!f.toFile().exists()) {
          // if the file does not exist and we cannot write, we need to actually cancel and throw
          logger.log(Level.WARNING, //
              """
                  Access denied: Please choose a temporary directory with write access in the mzmine preferences. \
                  No write access in """ + f.toFile().getAbsolutePath() + e.getMessage());
          throw e;
        }
      }

      // try adding random numbers if file already exists
      f = generateRandomPath(prefix, suffix, dir);
    }

    throw new IOException(
        "Cannot create temp file in path %s. Please select a different directory.".formatted(
            dir.toFile().getAbsolutePath()));
  }

  /**
   * Creates a temp folder in the set mzmine temp directory.
   */
  public static Path createTempDirectory(String name) throws IOException {
    return Files.createTempDirectory(MZMINE_TEMP_DIR.toPath(), name);
  }

  public static Path getWorkingDirectory() {
    return Paths.get("").toAbsolutePath();
  }

  /**
   * strip query parameters from URL with split at ?
   */
  public static String getFileNameFromUrl(final String downloadUrl) {
    return FilenameUtils.getName(downloadUrl).split("\\?")[0];
  }

  public static void setEarlyTempFileCleanup(final boolean state) {
    FileAndPathUtil.earlyTempFileCleanup = state;
  }

  /**
   * @param files A list of files
   * @return The most frequently used path in the list of files. If two paths have the same number
   * of occurrences, the result may vary as the map type of {@link Collectors#groupingBy(Function)}
   * is a hash map.
   */
  public static @Nullable File getMajorityFilePath(@Nullable Collection<@Nullable File> files) {
    if (files == null || files.isEmpty()) {
      return null;
    }

    return files.stream().filter(Objects::nonNull)
        .collect(Collectors.groupingBy(p -> p, Collectors.counting())).entrySet().stream()
        .max(Entry.comparingByValue(Comparator.reverseOrder())).map(Entry::getKey).orElse(null);
  }

}

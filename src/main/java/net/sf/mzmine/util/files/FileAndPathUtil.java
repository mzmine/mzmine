/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.util.files;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Simple file operations
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class FileAndPathUtil {

  /**
   * Returns the real file path as path/filename.fileformat
   * 
   * @param file
   * @param name
   * @param format a format starting with a dot for example: .pdf ; or without a dot: pdf
   * @return
   */
  public static File getRealFilePath(File path, String name, String format) {
    return new File(getFolderOfFile(path), getRealFileName(name, format));
  }

  /**
   * Returns the real file path as path/filename.fileformat
   * 
   * @param file
   * @param name
   * @param format a format starting with a dot for example: .pdf ; or without a dot: pdf
   * @return
   * @throws Exception if there is no filname (selected path = folder)
   */
  public static File getRealFilePath(File filepath, String format) {
    return new File(filepath.getParentFile(), getRealFileName(filepath.getName(), format));
  }

  /**
   * Returns the real file name as filename.fileformat
   * 
   * @param name
   * @param format a format starting with a dot for example .pdf
   * @return
   */
  public static String getRealFileName(String name, String format) {
    String result = eraseFormat(name);
    result = addFormat(result, format);
    return result;
  }

  /**
   * Returns the real file name as filename.fileformat
   * 
   * @param name
   * @param format a format starting with a dot for example .pdf
   * @return
   */
  public static String getRealFileName(File name, String format) {
    return getRealFileName(name.getAbsolutePath(), format);
  }

  /**
   * erases the format. "image.png" will be returned as "image" this method is used by
   * getRealFilePath and getRealFileName
   * 
   * @param name
   * @return
   */
  public static String eraseFormat(String name) {
    int lastDot = name.lastIndexOf(".");
    if (lastDot != -1)
      return name.substring(0, lastDot);
    else
      return name;
  }

  /**
   * erases the format. "image.png" will be returned as "image" this method is used by
   * getRealFilePath and getRealFileName
   * 
   * @param name
   * @return
   */
  public static File eraseFormat(File f) {
    int lastDot = f.getName().lastIndexOf(".");
    if (lastDot != -1)
      return new File(f.getParent(), f.getName().substring(0, lastDot));
    else
      return f;
  }

  /**
   * Adds the format. "image" will be returned as "image.format" Maybe use erase format first. this
   * method is used by getRealFilePath and getRealFileName
   * 
   * @param name
   * @param format
   * @return
   */
  public static String addFormat(String name, String format) {
    if (format.startsWith(".")) {
      return name + format;
    } else
      return name + "." + format;
  }

  /**
   * Returns the file format without a dot (f.e. "pdf") or "" if there is no format
   * 
   * @param file
   * @return
   */
  public static String getFormat(File file) {
    return getFormat(file.getAbsolutePath());
  }

  /**
   * Returns the file format without a dot (f.e. "pdf") or "" if there is no format
   * 
   * @param file
   * @return
   */
  public static String getFormat(String file) {
    if (!isOnlyAFolder(file)) {
      return file.substring(file.lastIndexOf(".") + 1);
    } else
      return "";
  }

  /**
   * Returns the file if it is already a folder. Or the parent folder if the file is a data file
   * 
   * @param file
   * @return
   */
  public static File getFolderOfFile(File file) {
    if (!isOnlyAFolder(file)) {
      return file.getParentFile();
    } else
      return file;
  }

  /**
   * Returns the file name from a given file. If file is a folder an empty String is returned
   * 
   * @param file
   * @return
   */
  public static String getFileNameFromPath(File file) {
    if (!isOnlyAFolder(file)) {
      return file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("\\") + 1);
    } else
      return "";
  }

  /**
   * Returns the file name from a given file. If the file is a folder an empty String is returned
   * 
   * @param file
   * @return
   */
  public static String getFileNameFromPath(String file) {
    return getFileNameFromPath(new File(file));
  }

  /**
   * Checks if a given File is a folder or a data file
   */
  public static boolean isOnlyAFolder(File file) {
    return isOnlyAFolder(file.getAbsolutePath());
  }

  /**
   * Checks if a given File is a folder or a data file
   */
  public static boolean isOnlyAFolder(String file) {
    String realPath = file;
    int lastDot = realPath.lastIndexOf(".");
    int lastPath = realPath.lastIndexOf("/");
    int lastDash = realPath.lastIndexOf("_");
    int lastDash2 = realPath.lastIndexOf("-");

    if (lastDot != -1 && lastDot > lastPath && lastDot > lastDash && lastDot > lastDash2) {
      // file format needs at least one non digit character
      for (int i = lastDot + 1; i < file.length(); i++) {
        char c = file.charAt(i);
        if (!Character.isDigit(c))
          return false; // file
      }
      return true; // folder
    } else
      return true; // folder
  }

  /**
   * Creates a new directory.
   * 
   * @param theDir
   * @return false if directory was not created
   */
  public static boolean createDirectory(File theDir) {
    // if the directory does not exist, create it
    if (!theDir.exists()) {
      boolean result = false;
      try {
        theDir.mkdirs();
        result = true;
      } catch (SecurityException se) {
        // handle it
      }
      return result;
    } else
      return true;
  }


  /**
   * Sort an array of files These files have to start or end with a number
   * 
   * @param files
   * @return
   */
  public static File[] sortFilesByNumber(File[] files) {
    Arrays.sort(files, new Comparator<File>() {
      private Boolean endsWithNumber = null;

      @Override
      public int compare(File o1, File o2) {
        try {
          if (endsWithNumber == null)
            checkEndsWithNumber(o1.getName());
          int n1 = extractNumber(o1.getName());
          int n2 = extractNumber(o2.getName());
          return n1 - n2;
        } catch (Exception e) {
          return o1.compareTo(o2);
        }
      }

      private int extractNumber(String name) throws Exception {
        int i = 0;
        try {
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
            if (f < 0)
              f = 0;
            String number = name.substring(f, e);
            i = Integer.parseInt(number);
          } else {
            int f = 0;
            for (; f < name.length(); f++) {
              if (!isNumber(name.charAt(f))) {
                break;
              }
            }
            String number = name.substring(0, f);
            i = Integer.parseInt(number);
          }
        } catch (Exception e) {
          i = 0; // if filename does not match the format
          throw e; // then default to 0
        }
        return i;
      }

      private void checkEndsWithNumber(String name) {
        // ends with number?
        int e = name.lastIndexOf('.');
        e = e == -1 ? name.length() : e;
        endsWithNumber = new Boolean(isNumber(name.charAt(e - 1)));
      }
    });
    return files;
  }

  private static boolean isNumber(char c) {
    return (c >= '0' && c <= '9');
  }

  /**
   * Lists all directories in directory f
   * 
   * @param f
   * @return
   */
  public static File[] getSubDirectories(File f) {
    return f.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File current, String name) {
        return new File(current, name).isDirectory();
      }
    });
  }

  // ###############################################################################################
  // search for files
  /**
   * 
   * @param dir
   * @param fileFilter
   * @return
   */
  public static List<File[]> findFilesInDir(File dir, FileNameExtFilter fileFilter) {
    return findFilesInDir(dir, fileFilter, true, false);
  }

  public static List<File[]> findFilesInDir(File dir, FileNameExtFilter fileFilter,
      boolean searchSubdir) {
    return findFilesInDir(dir, fileFilter, searchSubdir, false);
  }

  public static List<File[]> findFilesInDir(File dir, FileNameExtFilter fileFilter,
      boolean searchSubdir, boolean filesInSeparateFolders) {
    File[] subDir = FileAndPathUtil.getSubDirectories(dir);
    // result: each vector element stands for one img
    ArrayList<File[]> list = new ArrayList<File[]>();
    // add all files as first image
    // sort all files and return them
    File[] files = dir.listFiles(fileFilter);
    files = FileAndPathUtil.sortFilesByNumber(files);
    if (files != null && files.length > 0)
      list.add(files);

    if (subDir == null || subDir.length <= 0 || !searchSubdir) {
      // no subdir end directly
      return list;
    } else {
      // sort dirs
      subDir = FileAndPathUtil.sortFilesByNumber(subDir);
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
   * @param dir musst be sorted!
   * @param list
   * @return
   */
  private static void findFilesInSubDirSeparatedFolders(File parent, File[] dirs,
      ArrayList<File[]> list, FileNameExtFilter fileFilter) {
    // go into folder and find files
    ArrayList<File> img = null;
    // each file in one folder
    for (int i = 0; i < dirs.length; i++) {
      // find all suiting files
      File[] subFiles = FileAndPathUtil.sortFilesByNumber(dirs[i].listFiles(fileFilter));
      // if there are some suiting files in here directory has been found! create image of these
      // dirs
      if (subFiles.length > 0) {
        if (img == null)
          img = new ArrayList<File>();
        // put them into the list
        for (int f = 0; f < subFiles.length; f++) {
          img.add(subFiles[f]);
        }
      } else {
        // search in subfolders for data
        // find all subfolders, sort them and do the same iterative
        File[] subDir =
            FileAndPathUtil.sortFilesByNumber(FileAndPathUtil.getSubDirectories(dirs[i]));
        // call this method
        findFilesInSubDirSeparatedFolders(dirs[i], subDir, list, fileFilter);
      }
    }
    // add to list
    if (img != null && img.size() > 0) {
      list.add(img.toArray(new File[img.size()]));
    }
  }


  /**
   * Go into all sub-folders and find all files files stored one image in one folder!
   * 
   * @param dir musst be sorted!
   * @param list
   * @return
   */
  private static void findFilesInSubDir(File[] dirs, ArrayList<File[]> list,
      FileNameExtFilter fileFilter) {
    // All files in one folder
    for (int i = 0; i < dirs.length; i++) {
      // find all suiting files
      File[] subFiles = FileAndPathUtil.sortFilesByNumber(dirs[i].listFiles(fileFilter));
      // put them into the list
      if (subFiles != null && subFiles.length > 0)
        list.add(subFiles);
      // find all subfolders, sort them and do the same iterative
      File[] subDir = FileAndPathUtil.sortFilesByNumber(FileAndPathUtil.getSubDirectories(dirs[i]));
      // call this method
      findFilesInSubDir(subDir, list, fileFilter);
    }
  }

  /**
   * The Path of the Jar.
   * 
   * @return
   */
  public static File getPathOfJar() {
    /*
     * File f = new File(System.getProperty("java.class.path")); File dir =
     * f.getAbsoluteFile().getParentFile(); return dir;
     */
    try {
      File jar = new File(FileAndPathUtil.class.getProtectionDomain().getCodeSource().getLocation()
          .toURI().getPath());
      return jar.getParentFile();
    } catch (Exception ex) {
      return new File("");
    }
  }
}

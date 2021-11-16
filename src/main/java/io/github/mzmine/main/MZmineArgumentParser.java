/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.main;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.Nullable;

/**
 * Parses the command line arguments
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MZmineArgumentParser {

  private static Logger logger = Logger.getLogger(MZmineArgumentParser.class.getName());

  private File batchFile;
  private File preferencesFile;
  private File tempDirectory;
  private boolean isKeepRunningAfterBatch = false;
  private KeepInRam isKeepInRam = KeepInRam.NONE;

  public void parse(String[] args) {
    Options options = new Options();

    // -b  or --batch
    Option batch = new Option("b", "batch", true, "batch mode file");
    batch.setRequired(false);
    options.addOption(batch);

    Option pref = new Option("p", "pref", true, "preferences file");
    pref.setRequired(false);
    options.addOption(pref);

    Option tmpFolder = new Option("t", "temp", true,
        "Temp directory overrides definition in preferences and JVM");
    tmpFolder.setRequired(false);
    options.addOption(tmpFolder);

    Option keepRunning = new Option("r", "running", false, "keep MZmine running in headless mode");
    keepRunning.setRequired(false);
    options.addOption(keepRunning);

    Option keepInMemory = new Option("m", "memory", true,
        "keep objects (scan data, features, etc) in memory. Options: all, features, centroids, raw, masses_features (masses_features for features and centroids)");
    keepInMemory.setRequired(false);
    options.addOption(keepInMemory);

    CommandLineParser parser = new BasicParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd;

    try {
      cmd = parser.parse(options, args);

      String sbatch = cmd.getOptionValue(batch.getLongOpt());
      if (sbatch != null) {
        logger.info(() -> "Batch file set by command line: " + sbatch);
        batchFile = new File(sbatch);
      }
      String spref = cmd.getOptionValue(pref.getLongOpt());
      if (spref != null) {
        logger.info(() -> "Preferences file set by command line: " + spref);
        preferencesFile = new File(spref);
      }

      String stemp = cmd.getOptionValue(tmpFolder.getLongOpt());
      if (stemp != null) {
        logger.info(
            () -> "Temp directory set by command line, will override all other definitions: "
                  + stemp);
        tempDirectory = new File(stemp);
      }

      isKeepRunningAfterBatch = cmd.hasOption(keepRunning.getLongOpt());
      if (isKeepRunningAfterBatch) {

        logger.info(
            () -> "the -r / --running argument was set to keep MZmine alive after batch is finished");
      }

      String keepInData = cmd.getOptionValue(keepInMemory.getLongOpt());
      if (keepInData != null) {
        isKeepInRam = KeepInRam.parse(keepInData);
        logger.info(
            () -> "the -m / --memory argument was set to " + isKeepInRam.toString()
                  + " to keep objects in RAM (scan data, features, etc) which are otherwise stored in memory mapped ");
      }

    } catch (ParseException e) {
      logger.log(Level.SEVERE, "Wrong command line arguments. " + e.getMessage(), e);
      formatter.printHelp("utility-name", options);
      System.exit(1);
    }
  }

  /**
   * The temp directory overrides all other definitions if set
   *
   * @return the temp directory override (null or a file)
   */
  @Nullable
  public File getTempDirectory() {
    return tempDirectory;
  }

  @Nullable
  public File getPreferencesFile() {
    return preferencesFile;
  }

  @Nullable
  public File getBatchFile() {
    return batchFile;
  }

  /**
   * After batch is finished, keep mzmine running
   *
   * @return true if -r or --running was set as argument
   */
  public boolean isKeepRunningAfterBatch() {
    return isKeepRunningAfterBatch;
  }

  /**
   * Keep all {@link io.github.mzmine.util.MemoryMapStorage} items in RAM (e.g., scans, features,
   * masslists)
   *
   * @return true will keep objects in memory which are usually stored in memory mapped files
   */
  public KeepInRam isKeepInRam() {
    return isKeepInRam;
  }

  public enum KeepInRam {
    NONE, ALL, FEATURES, MASS_LISTS, RAW_SCANS, MASSES_AND_FEATURES;

    public static KeepInRam parse(String s) {
      s = s.toLowerCase();
      return switch (s) {
        case "all" -> ALL;
        case "features" -> FEATURES;
        case "centroids" -> MASS_LISTS;
        case "raw" -> RAW_SCANS;
        case "masses_features" -> MASSES_AND_FEATURES;
        default -> throw new IllegalStateException("Unexpected value: " + s);
      };
    }
  }
}


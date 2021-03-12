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
import javax.annotation.Nullable;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Parses the command line arguments
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MZmineArgumentParser {

  private static Logger logger = Logger.getLogger(MZmineArgumentParser.class.getName());

  private File batchFile;
  private File preferencesFile;

  public void parse(String[] args) {
    Options options = new Options();

    Option batch = new Option("b", "batch", true, "input file path");
    batch.setRequired(false);
    options.addOption(batch);

    Option pref = new Option("p", "pref", true, "output file");
    pref.setRequired(false);
    options.addOption(pref);

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

    } catch (ParseException e) {
      logger.log(Level.SEVERE, "Wrong command line arguments. " + e.getMessage(), e);
      formatter.printHelp("utility-name", options);
      System.exit(1);
    }
  }

  @Nullable
  public File getPreferencesFile() {
    return preferencesFile;
  }

  @Nullable
  public File getBatchFile() {
    return batchFile;
  }
}


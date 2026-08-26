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

package io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds MSPepSearch command lines.
 * <p>
 * Two things were established by probing MSPepSearch 0.9.7.5 against a NIST 26 installation and are
 * load bearing here:
 * <ul>
 *   <li>{@code /PATH} does not work - combining it with a library name fails with
 *       "MS Library initiation error -10". Libraries are therefore always passed as absolute
 *       paths and {@code /PATH} is never emitted.</li>
 *   <li>The main and replicate libraries only work behind {@code /MAIN} and {@code /REPL}; passing
 *       either as {@code /LIB} fails with the same error. See {@link NistLibraryKind}.</li>
 * </ul>
 * The search options form a single concatenated leading token, in the order the MSPepSearch help
 * declares them: {@code [{sdfmk}][aijnopqrvx][{uyz}][{leh}][{IQSHLMPGD}]}. Only the presets mzmine
 * offers are built, so most of those slots are fixed:
 * <ul>
 *   <li>presearch {@code d} for the EI searches and {@code m} - the precursor m/z window, which the
 *       MS Search user interface calls the default for MS/MS;</li>
 *   <li>{@code a} alternative peak matching and {@code i} ignore precursor region for MS/MS, both
 *       recommended by NIST;</li>
 *   <li>{@code v} to get the reverse match factor column;</li>
 *   <li>{@code z} to restrict MS/MS hits to library entries with a matching precursor m/z;</li>
 *   <li>{@code h} for the high MS/MS score threshold.</li>
 * </ul>
 */
final class MsPepSearchCommand {

  /**
   * The output columns mzmine needs to build an annotation. Requested for every search so that the
   * result parser always sees the same set of fields.
   */
  private static final List<String> COMMON_OUTPUT_COLUMNS = List.of("/OutChemForm", "/OutCASrn",
      "/OutNISTrn", "/OutIK", "/OutNumMP", "/OutNumPk");

  /**
   * Additional columns that only carry information for accurate mass MS/MS searches.
   */
  private static final List<String> MSMS_OUTPUT_COLUMNS = List.of("/OutPrecursorMZ",
      "/OutDeltaPrecursorMZ", "/OutPrecursorType", "/OutInstrType", "/OutCE");

  /**
   * Additional columns that only carry information for EI searches.
   */
  private static final List<String> EI_OUTPUT_COLUMNS = List.of("/OutMW");

  private MsPepSearchCommand() {
  }

  /**
   * Builds the command line for one search run.
   *
   * @param config     the search configuration.
   * @param executable the MSPepSearch executable.
   * @param queryFile  the MSP file holding the query spectra.
   * @param outputFile the tab delimited file to write the hits to.
   * @param workDir    the MSPepSearch work folder.
   * @param mwForLoss  the nominal molecular weight for {@code /MwForLoss}, required by the hybrid
   *                   search and ignored otherwise.
   * @return the command, ready for {@link ProcessBuilder}. Every path is a separate element, so
   * paths containing spaces need no quoting.
   */
  static @NotNull List<String> build(@NotNull final NistSearchConfig config,
      @NotNull final File executable, @NotNull final File queryFile,
      @NotNull final File outputFile, @NotNull final File workDir,
      @Nullable final Integer mwForLoss) {

    final NistSearchMode mode = config.mode();

    final List<String> command = new ArrayList<>();
    command.add(executable.getAbsolutePath());
    command.add(buildOptionToken(mode));

    // search parameters. The EI searches take none: /RI is never emitted because retention indices
    // are not used for matching, and everything else the mode implies is in the option token.
    if (mode.isHighResolution()) {
      addTolerance(command, "/Z", config.precursorTolerance());
      addTolerance(command, "/M", config.fragmentTolerance());
    }
    if (mode.needsMolecularWeight() && mwForLoss != null) {
      command.add("/MwForLoss");
      command.add(String.valueOf(mwForLoss));
    }

    // libraries, always as absolute paths and never behind /PATH
    for (final NistLibrary library : config.libraries()) {
      command.add(library.kind().getArgument());
      command.add(library.dir().getAbsolutePath());
    }

    // input and output
    command.add("/WRK");
    command.add(workDir.getAbsolutePath());
    command.add("/INP");
    command.add(queryFile.getAbsolutePath());
    command.add("/OUTTAB");
    command.add(outputFile.getAbsolutePath());

    command.add("/HITS");
    command.add(String.valueOf(NistSearchConfig.MAX_HITS));
    command.add("/MinMF");
    command.add(String.valueOf(config.minMatchFactor()));

    // output columns
    command.add("/OutSpecNum");
    command.add("1");
    command.addAll(COMMON_OUTPUT_COLUMNS);
    command.addAll(mode.isHighResolution() ? MSMS_OUTPUT_COLUMNS : EI_OUTPUT_COLUMNS);

    // progress messages on stderr, used to drive the task progress bar
    command.add("/PROGRESSNS");

    return command;
  }

  /**
   * Builds the leading concatenated option token: {@code dvI}, {@code dvS} or {@code dvH} for the EI
   * searches and {@code maivzhG} for MS/MS.
   * <p>
   * MSPepSearch's "penalize rare compounds" flag {@code p} is deliberately never emitted. Its help
   * states it only applies to the main and replicate libraries of 2020 or earlier plus NIST 23, and
   * against a NIST 26 installation it terminates the process with an access violation (exit code
   * 0xC0000005) instead of reporting an error.
   */
  private static String buildOptionToken(final NistSearchMode mode) {

    final StringBuilder token = new StringBuilder();

    if (mode.isHighResolution()) {
      // m is the precursor m/z window presearch, only compatible with the precursor matching z
      // below; a and i are the peak matching options NIST recommends for MS/MS
      token.append("mai");
    } else {
      token.append('d');
    }

    // always request the reverse match factor column
    token.append('v');

    if (mode.isHighResolution()) {
      // z restricts hits to library entries whose precursor m/z matches, h is the high threshold
      token.append("zh");
    }

    // the search type is always last
    token.append(mode.getSearchTypeLetter());

    return token.toString();
  }

  /**
   * Adds one m/z tolerance of a high resolution search.
   * <p>
   * MSPepSearch takes either a ppm or an absolute tolerance, never the maximum of both the way
   * {@link MZTolerance} does, so the ppm value wins when it is set.
   */
  private static void addTolerance(final List<String> command, final String argument,
      @Nullable final MZTolerance tolerance) {

    if (tolerance == null) {
      return;
    }

    if (tolerance.getPpmTolerance() > 0) {
      command.add(argument + "PPM");
      command.add(trimTrailingZeros(tolerance.getPpmTolerance()));
    } else {
      command.add(argument);
      command.add(trimTrailingZeros(tolerance.getMzTolerance()));
    }
  }

  /**
   * Formats a tolerance without a trailing {@code .0}, which MSPepSearch would reject for the ppm
   * form.
   */
  private static String trimTrailingZeros(final double value) {

    if (value == Math.rint(value)) {
      return String.valueOf((long) value);
    }
    return String.valueOf(value);
  }
}

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

import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistEiSearchParameters.NistRetentionIndexParameters;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistEiSearchParameters.RIPenaltyRate;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistMsMsSearchParameters.HiResThreshold;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistPepSearchAdvancedParameters.Presearch;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.RIColumn;
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
 * declares them: {@code [{sdfmk}][aijnopqrvx][{uyz}][{leh}][{IQSHLMPGD}]}.
 */
public final class MsPepSearchCommand {

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
   * @param parameters the module parameters.
   * @param executable the MSPepSearch executable.
   * @param queryFile  the MSP file holding the query spectra.
   * @param outputFile the tab delimited file to write the hits to.
   * @param workDir    the MSPepSearch work folder.
   * @param mwForLoss  the nominal molecular weight for {@code /MwForLoss}, required by the hybrid
   *                   search and ignored otherwise.
   * @return the command, ready for {@link ProcessBuilder}. Every path is a separate element, so
   * paths containing spaces need no quoting.
   */
  public static @NotNull List<String> build(@NotNull final ParameterSet parameters,
      @NotNull final File executable, @NotNull final File queryFile,
      @NotNull final File outputFile, @NotNull final File workDir,
      @Nullable final Integer mwForLoss) {

    final NistSearchMode mode = parameters.getValue(NistPepSearchParameters.searchMode);
    final ParameterSet advanced = parameters.getParameter(NistPepSearchParameters.advanced)
        .getEmbeddedParameters();

    final List<String> command = new ArrayList<>();
    command.add(executable.getAbsolutePath());
    command.add(buildOptionToken(parameters, mode, advanced));

    // search parameters
    if (mode.isHighResolution()) {
      addToleranceArguments(command, parameters);
    } else {
      addRetentionIndexArguments(command, parameters);
    }
    if (mode.needsMolecularWeight() && mwForLoss != null) {
      command.add("/MwForLoss");
      command.add(String.valueOf(mwForLoss));
    }

    // libraries, always as absolute paths and never behind /PATH
    for (final NistLibrary library : libraries(parameters)) {
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
    command.add(String.valueOf(parameters.getValue(NistPepSearchParameters.maxHits)));
    command.add("/MinMF");
    command.add(String.valueOf(parameters.getValue(NistPepSearchParameters.minMatchFactor)));

    // output columns
    command.add("/OutSpecNum");
    command.add("1");
    command.addAll(COMMON_OUTPUT_COLUMNS);
    command.addAll(mode.isHighResolution() ? MSMS_OUTPUT_COLUMNS : EI_OUTPUT_COLUMNS);

    // progress messages on stderr, used to drive the task progress bar
    command.add("/PROGRESSNS");

    if (advanced.getValue(NistPepSearchAdvancedParameters.librariesInMemory)) {
      command.add("/LibInMem");
    }
    if (advanced.getValue(NistPepSearchAdvancedParameters.elevatedPriority)) {
      command.add("/HiPri");
    }

    final String extra = advanced.getValue(NistPepSearchAdvancedParameters.extraArguments);
    if (extra != null && !extra.isBlank()) {
      command.addAll(List.of(extra.trim().split("\\s+")));
    }

    return command;
  }

  /**
   * Builds the leading concatenated option token, e.g. {@code dvI} or {@code maizhG}.
   */
  private static String buildOptionToken(final ParameterSet parameters, final NistSearchMode mode,
      final ParameterSet advanced) {

    final StringBuilder token = new StringBuilder();

    final boolean matchPrecursor = mode.isHighResolution() && parameters.getParameter(
            NistPepSearchParameters.msmsParameters).getEmbeddedParameters()
        .getValue(NistMsMsSearchParameters.matchPrecursor);

    // presearch
    final Presearch presearch = advanced.getValue(NistPepSearchAdvancedParameters.presearch);
    char presearchLetter = presearch.getLetter(mode.isHighResolution());
    if (mode.isHighResolution() && !matchPrecursor && (presearchLetter == 'm'
        || presearchLetter == 'k')) {
      // MSPepSearch rejects the precursor window presearch m and the InChIKey presearch k when the
      // precursor is not matched: "Presearch type 'm' is not compatible with High Resolution search
      // option 'u'". Fall back to the standard pre-indexed presearch.
      presearchLetter = 'd';
    }
    token.append(presearchLetter);

    // search flags, in the order the help declares them: a i ... r v
    if (mode.isHighResolution()) {

      final ParameterSet msms = parameters.getParameter(NistPepSearchParameters.msmsParameters)
          .getEmbeddedParameters();
      if (msms.getValue(NistMsMsSearchParameters.alternativePeakMatching)) {
        token.append('a');
      }
      if (msms.getValue(NistMsMsSearchParameters.ignorePrecursorRegion)) {
        token.append('i');
      }
    }

    if (advanced.getValue(NistPepSearchAdvancedParameters.reverseSearch)) {
      token.append('r');
    }

    // always request the reverse match factor column
    token.append('v');

    if (mode.isHighResolution()) {

      final ParameterSet msms = parameters.getParameter(NistPepSearchParameters.msmsParameters)
          .getEmbeddedParameters();

      // precursor handling: z matches the precursor, u does not
      token.append(matchPrecursor ? 'z' : 'u');

      final HiResThreshold threshold = msms.getValue(NistMsMsSearchParameters.threshold);
      token.append(threshold.getLetter());
    }

    // the search type is always last
    token.append(mode.getSearchTypeLetter());

    return token.toString();
  }

  /**
   * Adds the precursor and fragment tolerances of a high resolution search.
   * <p>
   * MSPepSearch takes either a ppm or an absolute tolerance, never the maximum of both the way
   * {@link MZTolerance} does, so the ppm value wins when it is set.
   */
  private static void addToleranceArguments(final List<String> command,
      final ParameterSet parameters) {

    final ParameterSet msms = parameters.getParameter(NistPepSearchParameters.msmsParameters)
        .getEmbeddedParameters();

    addTolerance(command, "/Z", msms.getValue(NistMsMsSearchParameters.precursorTolerance));
    addTolerance(command, "/M", msms.getValue(NistMsMsSearchParameters.fragmentTolerance));
  }

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
   * Adds {@code /RI} when the EI search is configured to use retention indices.
   */
  private static void addRetentionIndexArguments(final List<String> command,
      final ParameterSet parameters) {

    final ParameterSet ei = parameters.getParameter(NistPepSearchParameters.eiParameters)
        .getEmbeddedParameters();

    if (!ei.getValue(NistEiSearchParameters.retentionIndex)) {
      return;
    }

    final ParameterSet ri = ei.getParameter(NistEiSearchParameters.retentionIndex)
        .getEmbeddedParameters();

    // /RI {nsp}[o][a][u][d][x][tNNrXX] - a single token, no spaces
    final StringBuilder token = new StringBuilder();
    token.append(columnLetter(ri.getValue(NistRetentionIndexParameters.column)));

    if (ri.getValue(NistRetentionIndexParameters.overrideFromSpectrum)) {
      token.append('o');
    }
    if (ri.getValue(NistRetentionIndexParameters.useOtherNonPolar)) {
      token.append('a');
    }
    if (ri.getValue(NistRetentionIndexParameters.assumeUnspecified)) {
      token.append('u');
    }

    if (ri.getValue(NistRetentionIndexParameters.penalty)) {
      // tNNrXX makes MSPepSearch reduce the match factor of hits outside the tolerance
      final RIPenaltyRate rate = ri.getParameter(NistRetentionIndexParameters.penalty)
          .getEmbeddedParameter().getValue();
      token.append('t').append(ri.getValue(NistRetentionIndexParameters.tolerance)).append('r')
          .append(rate.getCode());
    } else {
      // x reports the retention indices without letting them change the score
      token.append('x');
    }

    command.add("/RI");
    command.add(token.toString());
  }

  /**
   * The MSPepSearch column type letter of an RI column.
   */
  private static char columnLetter(@Nullable final RIColumn column) {
    return switch (column) {
      case NONPOLAR -> 'n';
      case POLAR -> 'p';
      case null, default -> 's';
    };
  }

  /**
   * The selected libraries, with at most one main and one replicate library because MSPepSearch
   * accepts only one of each.
   */
  private static List<NistLibrary> libraries(final ParameterSet parameters) {

    final List<NistLibrary> selected =
        parameters instanceof NistPepSearchParameters nist ? nist.getSelectedLibraries()
            : List.of();

    final List<NistLibrary> result = new ArrayList<>();
    boolean mainUsed = false;
    boolean replicateUsed = false;

    for (final NistLibrary library : selected) {
      switch (library.kind()) {
        case MAIN -> {
          if (!mainUsed) {
            result.add(library);
            mainUsed = true;
          }
        }
        case REPLICATE -> {
          if (!replicateUsed) {
            result.add(library);
            replicateUsed = true;
          }
        }
        case USER -> result.add(library);
      }
    }

    return result.size() > NistLibrary.MAX_LIBRARIES ? result.subList(0, NistLibrary.MAX_LIBRARIES)
        : result;
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

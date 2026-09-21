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

package io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The MSPepSearch search types mzmine can run.
 * <p>
 * The letter is the search type character of the leading MSPepSearch option token. Low resolution
 * types ({@code I}, {@code S}, {@code H}) work on unit mass EI spectra and use the NIST main and
 * replicate libraries; the high resolution type ({@code G}) works on accurate mass MS/MS spectra
 * and uses the tandem libraries.
 * <p>
 * {@link #AUTO} is not a search type of its own but a placeholder that is resolved to one of the
 * others, see {@link #resolve(FeatureList)}.
 */
public enum NistSearchMode {

  /**
   * Picks the search type from the feature list rather than from the parameters, see
   * {@link #resolve(FeatureList)}. Not a search type MSPepSearch knows, so it has to be resolved
   * before anything but {@link #isAutomatic()} is asked of it.
   */
  AUTO("Automatic (GC-EI identity or MS/MS)", null, null),
  /**
   * Low resolution identity search - the standard GC-EI workflow.
   */
  GC_EI_IDENTITY("GC-EI identity", 'I', NistLibraryContent.EI),
  /**
   * Low resolution simple similarity search, for compounds that are not in the library.
   */
  GC_EI_SIMILARITY("GC-EI similarity", 'S', NistLibraryContent.EI),
  /**
   * Low resolution hybrid similarity search. Needs the nominal molecular weight of the unknown
   * ({@code /MwForLoss}), which MSPepSearch only accepts as a single global value, so queries have
   * to be grouped by molecular weight.
   * <p>
   * No module offers this mode: the molecular weight of the unknown is rarely known well enough for
   * it to be useful, and the grouping makes the search considerably slower. The engine keeps it so
   * that offering it stays a parameter change rather than a rewrite.
   */
  GC_EI_HYBRID("GC-EI hybrid similarity", 'H', NistLibraryContent.EI),
  /**
   * High resolution generic MS/MS search - the standard LC-MS/MS workflow. "Generic" rather than
   * "peptide" so that no peptide peak annotation or weighting is applied.
   */
  MSMS_HIRES("MS/MS", 'G', NistLibraryContent.MSMS);

  private final String label;
  private final Character searchTypeLetter;
  private final NistLibraryContent libraryContent;

  NistSearchMode(final String label, @Nullable final Character searchTypeLetter,
      @Nullable final NistLibraryContent libraryContent) {
    this.label = label;
    this.searchTypeLetter = searchTypeLetter;
    this.libraryContent = libraryContent;
  }

  /**
   * The search types that can actually be searched, that is everything but {@link #AUTO}.
   */
  public static @NotNull NistSearchMode[] searchTypes() {
    return new NistSearchMode[]{GC_EI_IDENTITY, GC_EI_SIMILARITY, GC_EI_HYBRID, MSMS_HIRES};
  }

  /**
   * True if the feature list was built by a spectral deconvolution module, which is what turns
   * GC-EI chromatograms into the pseudo spectra this search annotates.
   *
   * @param featureList the feature list to search, may be null.
   */
  public static boolean isDeconvolutedGcEi(@Nullable final FeatureList featureList) {

    if (featureList == null) {
      return false;
    }

    for (final FeatureListAppliedMethod method : featureList.getAppliedMethods()) {

      final MZmineModule module = method == null ? null : method.getModule();
      if (module instanceof MZmineRunnableModule runnable
          && runnable.getModuleCategory() == MZmineModuleCategory.SPECTRALDECONVOLUTION) {
        return true;
      }
    }

    return false;
  }

  /**
   * Resolves {@link #AUTO} against the feature list that is about to be searched: a list that went
   * through spectral deconvolution holds GC-EI pseudo spectra, everything else is treated as
   * accurate mass MS/MS.
   *
   * @param featureList the feature list to search, may be null.
   * @return the search type to run, never {@link #AUTO}. Every other mode returns itself, so that
   * an explicitly chosen type is never overridden.
   */
  public @NotNull NistSearchMode resolve(@Nullable final FeatureList featureList) {

    if (this != AUTO) {
      return this;
    }

    return isDeconvolutedGcEi(featureList) ? GC_EI_IDENTITY : MSMS_HIRES;
  }

  /**
   * @return true for {@link #AUTO}, which has to be resolved before it can be searched.
   */
  public boolean isAutomatic() {
    return this == AUTO;
  }

  /**
   * @return the MSPepSearch search type character.
   */
  char getSearchTypeLetter() {
    return requireResolved(searchTypeLetter);
  }

  /**
   * @return true for accurate mass MS/MS searches, false for unit mass EI searches. Decides which
   * option group applies and whether a precursor m/z is written to the query file.
   */
  public boolean isHighResolution() {
    return requiredLibraryContent() == NistLibraryContent.MSMS;
  }

  /**
   * The libraries this search type runs against. Every library of that content in the installation
   * is searched, so that no library has to be picked by hand.
   *
   * @return {@link NistLibraryContent#MSMS} for the accurate mass MS/MS search and
   * {@link NistLibraryContent#EI} for the unit mass EI searches.
   */
  public @NotNull NistLibraryContent requiredLibraryContent() {
    return requireResolved(libraryContent);
  }

  /**
   * Guards the properties that only a real search type has, so that a forgotten
   * {@link #resolve(FeatureList)} fails here rather than producing a wrong search.
   */
  private <T> @NotNull T requireResolved(@Nullable final T value) {

    if (value == null) {
      throw new IllegalStateException(
          this + " is not a search type of its own and has to be resolved against the feature list "
              + "first, see NistSearchMode.resolve.");
    }

    return value;
  }

  /**
   * @return true if this mode needs {@code /MwForLoss}, which forces queries to be grouped by
   * nominal molecular weight.
   */
  boolean needsMolecularWeight() {
    return this == GC_EI_HYBRID;
  }

  @Override
  public String toString() {
    return label;
  }
}

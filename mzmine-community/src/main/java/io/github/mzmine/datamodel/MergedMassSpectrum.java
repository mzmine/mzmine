/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.datamodel;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;

public interface MergedMassSpectrum extends Scan {

  MergingType getMergingType();

  /**
   * @return A list of spectra used to create this merged spectrum. Never a merged spectra, which
   * are unpacked to their source spectra in case merged spectra are merged.
   */
  List<MassSpectrum> getSourceSpectra();

  /**
   * @return The merging type this spectrum is based on.
   */
  IntensityMergingType getIntensityMergingType();

  /**
   * @return The center function used to create this merged spectrum.
   */
  CenterFunction getCenterFunction();


  /**
   * @return -1 to represent the artificial state of this spectrum.
   */
  @Override
  default int getScanNumber() {
    return -1;
  }

  void saveToXML(XMLStreamWriter writer) throws XMLStreamException;

  /**
   * The merging type describes the selection of input spectra and on which level it was merged.
   * Ordered by larger merging factor.
   */
  enum MergingType implements UniqueIdSupplier {
    /**
     * First is less specific
     */
    UNKNOWN,
    /**
     * any single scan except for the single best
     */
    SINGLE_SCAN,
    /**
     * Single scan so not merged yet
     */
    SINGLE_BEST_SCAN,
    /**
     * MS1 spectrum of correlated signals like isotopes, adducts, and other signals with same
     * retention time or shape
     */
    CORRELATED_MS1,
    /**
     * Merged all {@link MobilityScan}s from a single {@link PasefMsMsInfo} (= single fragmentation
     * event). This spectrum is created by merging multiple mobility scans, but does not fulfill the
     * criteria of the other merging types. It is not acquired from multiple MS2 events or multiple
     * collision energies.
     */
    PASEF_SINGLE,
    /**
     * SAME_ENERGY merged all spectra from the same energy
     */
    SAME_ENERGY,
    /**
     * UNDEFINED_ENERGY merged all spectra from undefined energy
     */
    UNDEFINED_ENERGY,
    /**
     * SAME_PRECURSOR_IN_MSLEVEL merged all spectra of the same precursor into one spectrum. Usually
     * after merging the individual energies first.
     */
    ALL_ENERGIES,
    /**
     * ALL_MSN merged all MSn spectra for a precursor into a pseudo MS2 spectrum. The order of
     * merging is usually: 1. merge individual energies 2. merge for each precursor on all MSn
     * levels 3. merge all into one
     */
    ALL_MSN_TO_PSEUDO_MS2;

    /**
     * @return merging type from string by unique ID or name, default type if no match
     */
    public static @NotNull MergingType parseOrElse(final String value,
        @NotNull final MergingType defaultType) {
      return requireNonNullElse(UniqueIdSupplier.parseOrElse(value, values(), defaultType),
          defaultType);
    }

    @Override
    public String toString() {
      return switch (this) {
        case UNKNOWN -> "Unknown merging";
        case SINGLE_SCAN -> "Single scan";
        case SINGLE_BEST_SCAN -> "Single best scan";
        case SAME_ENERGY -> "Same energy merged";
        case UNDEFINED_ENERGY -> "Undefined energy merged";
        case ALL_ENERGIES -> "All energies merged";
        case ALL_MSN_TO_PSEUDO_MS2 -> "MSn to pseudo MS2 merged";
        case CORRELATED_MS1 -> "CORRELATED MS"; // required by SIRIUS?
        case PASEF_SINGLE -> "Single PASEF";
      };
    }

    @Override
    public @NotNull String getUniqueID() {
      return switch (this) {
        case UNKNOWN -> "UNKNOWN_MERGING";
        case SINGLE_SCAN -> "SINGLE_SCAN";
        case SINGLE_BEST_SCAN -> "SINGLE_BEST_SCAN";
        case SAME_ENERGY -> "SAME_ENERGY";
        case UNDEFINED_ENERGY -> "UNDEFINED_ENERGY";
        case ALL_ENERGIES -> "ALL_ENERGIES";
        case ALL_MSN_TO_PSEUDO_MS2 -> "ALL_MSN_TO_PSEUDO_MS2";
        case CORRELATED_MS1 -> "CORRELATED_MS";
        case PASEF_SINGLE -> "PASEF_SINGLE";
      };
    }
  }
}

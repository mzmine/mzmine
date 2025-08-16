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

package io.github.mzmine.parameters.parametertypes.combowithinput;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter.Options;
import io.github.mzmine.util.scans.ScanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Select scans based on their MS level
 *
 * @param filter        enum filter option
 * @param specificLevel only used for the option SPECIFIC_FILTER
 */
public record MsLevelFilter(Options filter, int specificLevel) implements
    ComboWithInputValue<Options, Integer> {

  public static final MsLevelFilter ALL_LEVELS = new MsLevelFilter(Options.ALL, 1);

  public MsLevelFilter(Options filter) {
    this(filter, 3);
  }

  @NotNull
  public static MsLevelFilter of(@Nullable final Integer msLevel) {
    return of(msLevel, false);
  }

  /**
   * @param msLevel         the MS level
   * @param useMSnForLevel2 use MSn instead of MS2 for msLevel == 2
   */
  @NotNull
  public static MsLevelFilter of(@Nullable final Integer msLevel, boolean useMSnForLevel2) {
    return switch (msLevel) {
      case null -> ALL_LEVELS;
      case -1 -> ALL_LEVELS;
      case 1 -> new MsLevelFilter(Options.MS1, 1);
      case 2 ->
          useMSnForLevel2 ? new MsLevelFilter(Options.MSn) : new MsLevelFilter(Options.MS2, 2);
      default -> new MsLevelFilter(Options.SPECIFIC_LEVEL, msLevel);
    };
  }

  @Override
  public String toString() {
    return switch (filter) {
      case ALL -> "All MS levels";
      case MS2, MSn, MS1, SPECIFIC_LEVEL -> getFilterString();
    };
  }

  @NotNull
  public String getFilterString() {
    return switch (filter) {
      case ALL -> "";
      case MS1 -> "MS1, level = 1";
      case MS2 -> "MS2, level = 2";
      case MSn -> "MSn, level ≥ 2";
      case SPECIFIC_LEVEL -> "MS level=" + specificLevel;
    };
  }

  @Override
  public Options getSelectedOption() {
    return filter;
  }

  @Override
  public Integer getEmbeddedValue() {
    return specificLevel;
  }

  /**
   * @param scan the tested scan
   * @return true if scan matches filter
   */
  public boolean accept(MassSpectrum scan) {
    int msLevel = ScanUtils.getMsLevel(scan).orElse(0);
    return accept(msLevel);
  }

  /**
   * @param msLevel tested level
   * @return true if ms level matches filter
   */
  public boolean accept(int msLevel) {
    return switch (filter) {
      case ALL -> true;
      case MS1 -> msLevel == 1;
      case MS2 -> msLevel == 2;
      case MSn -> msLevel > 1;
      case SPECIFIC_LEVEL -> msLevel == specificLevel;
    };
  }

  /**
   * @param scan the tested scan
   * @return true if scan does not match filter
   */
  public boolean notMatch(MassSpectrum scan) {
    return !accept(scan);
  }

  /**
   * @param msLevel tested level
   * @return false if ms level matches filter
   */
  public boolean notMatch(int msLevel) {
    return !accept(msLevel);
  }

  /**
   * @return a single MS level if not MSn or ALL option - then null
   */
  @Nullable
  public Integer getSingleMsLevelOrNull() {
    return switch (filter) {
      case ALL, MSn -> null;
      case MS1 -> 1;
      case MS2 -> 2;
      case SPECIFIC_LEVEL -> specificLevel;
    };
  }

  /**
   * Only true if this is a single MS level
   */
  public boolean isSingleMsLevel(final int msLevel) {
    return switch (filter) {
      case ALL, MSn -> false;
      case MS1 -> 1 == msLevel;
      case MS2 -> 2 == msLevel;
      case SPECIFIC_LEVEL -> specificLevel == msLevel;
    };
  }

  public boolean isMs1Only() {
    return isSingleMsLevel(1);
  }

  /**
   * Same like notMatch(1)
   *
   * @return true if MS1 is excluded so if only MS2 or MSn are selected. False if MS1 is included
   */
  public boolean isFragmentationNoMS1() {
    return notMatch(1);
  }

  public boolean isMs2Only() {
    return isSingleMsLevel(2);
  }

  public boolean isFilter() {
    return this.filter() != Options.ALL;
  }

  public enum Options implements UniqueIdSupplier {
    ALL, MS1, MSn, MS2, SPECIFIC_LEVEL;

    public static final Options[] EXCEPT_MS1 = new Options[]{MSn, MS2, SPECIFIC_LEVEL};

    @Override
    public String toString() {
      return switch (this) {
        case MS1 -> "MS1, level = 1";
        case MS2 -> "MS2, level = 2";
        case MSn -> "MSn, level ≥ 2";
        case ALL -> "All MS levels";
        case SPECIFIC_LEVEL -> "Specific MS level";
      };
    }

    @Override
    public @NotNull String getUniqueID() {
      return switch (this) {
        case MS1 -> "MS1, level = 1";
        case MS2 -> "MS2, level = 2";
        case MSn -> "MSn, level ≥ 2";
        case ALL -> "All MS levels";
        case SPECIFIC_LEVEL -> "Specific MS level";
      };
    }
  }

}

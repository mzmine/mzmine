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

package io.github.mzmine.datamodel.identities.iontype;

import static io.github.mzmine.util.StringUtils.inQuotes;

import io.github.mzmine.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parses strings to {@link IonType}
 */
public class IonTypeParser {

  private static final Logger logger = Logger.getLogger(IonTypeParser.class.getName());

  /**
   * Pattern that groups +3H2O to  +  3  H2O
   */
  private static final Pattern PART_PATTERN = Pattern.compile("([+-])(\\d*)(\\w+)");

  @Nullable
  public static IonType parse(final @Nullable String str) {
    if (str == null || str.isBlank()) {
      return null;
    }
    // clean up but keep [] for now
    String clean = str.replaceAll("[^a-zA-Z0-9+-\\[\\]]", "");
    String[] splitCharge = clean.split("]");
    // default charge is 1 - because we are usually looking at charged ions
    Integer detectedCharge = null;
    if (splitCharge.length > 1) {
      // [M+H]+ to [M+H and +
      detectedCharge = StringUtils.parseSignAndIntegerOrElse(splitCharge[1], true, null);
      clean = splitCharge[0];
    } else {
      // read charge that was not separated by ']' so maybe from M+H or M+H+
      int lastPlusMinusSignIndex = StringUtils.findLastPlusMinusSignIndex(clean, true);
      if (lastPlusMinusSignIndex > -1) {
        detectedCharge = StringUtils.parseSignAndIntegerOrElse(
            clean.substring(lastPlusMinusSignIndex - 1), true, null);
        clean = clean.substring(0, lastPlusMinusSignIndex);
      }
    }

    // remove all other characters (already cleaned before)
    clean = clean.replaceAll("[\\[\\]]", "");
    int starti = 0;

    int molMultiplier = 1;
    boolean molFound = false;
    List<IonModification> mods = new ArrayList<>();

    for (int i = 0; i < clean.length(); i++) {
      char c = clean.charAt(i);
      if (c == '+') {
        String mod = clean.substring(starti, i);
        if (!molFound) {
          // remove the M from the end of 2M or M
          molMultiplier = getMolMultiplier(mod, 1);
          molFound = true;
        } else {
          parseAndAddIonModifications(mods, mod);
        }
        starti = i;
      }
      if (c == '-') {
        String mod = clean.substring(starti, i);
        if (!molFound) {
          // remove the M from the end of 2M or M
          molMultiplier = getMolMultiplier(mod, 1);
          molFound = true;
        } else {
          parseAndAddIonModifications(mods, mod);
        }
        starti = i;
      }
    }
//

    // charge was already removed - remainder is the last modification part
    String remainder = clean.substring(starti);
    if (!molFound) {
      // remove the M from the end of 2M or M
      molMultiplier = getMolMultiplier(remainder, 1);
      molFound = true;
    } else {
      parseAndAddIonModifications(mods, remainder);
    }

    IonType ion = createIon(molMultiplier, mods);

    int chargeDiff = 0;
    if (detectedCharge == null && ion.getCharge() == 0) {
      // default to charge 1
      chargeDiff = 1;
    } else if (detectedCharge != null) {
      chargeDiff = detectedCharge - ion.getCharge();
    }
    if (chargeDiff != 0) {
      var electron = chargeDiff > 0 ? IonModification.M_PLUS : IonModification.M_MINUS;
      for (int c = 0; c < Math.abs(chargeDiff); c++) {
        mods.add(electron);
      }
      return createIon(molMultiplier, mods);
    } else {
      return ion;
    }
  }

  @NotNull
  private static IonType createIon(final int molMultiplier, final List<IonModification> mods) {
    var adducts = mods.stream().filter(Objects::nonNull).filter(m -> m.getAbsCharge() > 0)
        .toArray(IonModification[]::new);
    var modifications = mods.stream().filter(Objects::nonNull).filter(m -> m.getAbsCharge() == 0)
        .toArray(IonModification[]::new);

    var combinedMod =
        modifications.length == 0 ? null : CombinedIonModification.create(modifications);
    return new IonType(molMultiplier, CombinedIonModification.create(adducts), combinedMod);
  }

  private static void parseAndAddIonModifications(final List<IonModification> mods, String mod) {
    // mod is +Na or -H so with sign
    // handle +2H by removing the number
    var matcher = PART_PATTERN.matcher(mod);
    if (!matcher.find()) {
      return;
    }

    // keep parsing prefix number like this
    int adductMultiplier = StringUtils.parseIntegerPrefixOrElse(mod, 1);

    // need +H or -H2O to get the correct part
    String sign = StringUtils.orDefault(matcher.group(1), "+");
    String part = sign + StringUtils.orDefault(matcher.group(3), "");

    var ionPart = IonModification.parseFromString(part);

    for (int j = 0; j < Math.abs(adductMultiplier); j++) {
      mods.add(ionPart);
    }
  }

  private static int getMolMultiplier(String mod, int defaultValue) {
    if (mod.isBlank()) {
      return defaultValue;
    }
    mod = mod.substring(0, mod.length() - 1);
    if (!mod.isBlank()) {
      try {
        return Integer.parseInt(mod);
      } catch (Exception ex) {
        logger.finest("Cannot parse prefix of M in ion notation for " + inQuotes(mod));
      }
    }
    return defaultValue;
  }


}

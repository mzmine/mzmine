/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.parameters.parametertypes.selectors;

import com.google.common.base.Strings;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import java.text.NumberFormat;
import javax.annotation.concurrent.Immutable;
import org.jetbrains.annotations.Nullable;

@Immutable
public class FeatureSelection {

  private final Range<Integer> idRange;
  private final Range<Float> rtRange;
  private final Range<Double> mzRange;
  private final String name;

  public FeatureSelection(@Nullable Range<Integer> idRange, @Nullable Range<Double> mzRange,
      @Nullable Range<Float> rtRange, String name) {
    this.idRange = idRange;
    this.mzRange = mzRange;
    this.rtRange = rtRange;
    this.name = name;
  }

  public Range<Integer> getIDRange() {
    return idRange;
  }

  public Range<Double> getMZRange() {
    return mzRange;
  }

  public Range<Float> getRTRange() {
    return rtRange;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    if (idRange == null && mzRange == null && rtRange == null && Strings.isNullOrEmpty(name)) {
      return "All";
    }
    StringBuilder sb = new StringBuilder();
    if (idRange != null) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append("ID: ");
      if (idRange.lowerEndpoint().equals(idRange.upperEndpoint())) {
        sb.append(idRange.lowerEndpoint().toString());
      } else {
        sb.append(idRange.toString());
      }
    }
    if (mzRange != null) {
      NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append("m/z: ");
      if (mzRange.lowerEndpoint().equals(mzRange.upperEndpoint())) {
        sb.append(mzFormat.format(mzRange.lowerEndpoint()));
      } else {
        sb.append(mzFormat.format(mzRange.lowerEndpoint()));
        sb.append("-");
        sb.append(mzFormat.format(mzRange.upperEndpoint()));
      }
    }
    if (rtRange != null) {
      NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append("RT: ");
      if (rtRange.lowerEndpoint().equals(rtRange.upperEndpoint())) {
        sb.append(rtFormat.format(rtRange.lowerEndpoint()));
      } else {
        sb.append(rtFormat.format(rtRange.lowerEndpoint()));
        sb.append("-");
        sb.append(rtFormat.format(rtRange.upperEndpoint()));
      }
      sb.append(" min");
    }
    if (!Strings.isNullOrEmpty(name)) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append("name: ");
      sb.append(name);
    }
    return sb.toString();
  }

  public boolean checkPeakListRow(FeatureListRow row) {
    if ((idRange != null) && (!idRange.contains(row.getID()))) {
      return false;
    }

    if ((mzRange != null) && (!mzRange.contains(row.getAverageMZ()))) {
      return false;
    }

    if ((rtRange != null) && (!rtRange.contains(row.getAverageRT()))) {
      return false;
    }

    if (!Strings.isNullOrEmpty(name)) {
      if ((row.getPreferredFeatureIdentity() == null) || (
          row.getPreferredFeatureIdentity().getName() == null)) {
        return false;
      }
      if (!row.getPreferredFeatureIdentity().getName().toLowerCase().contains(name.toLowerCase())) {
        return false;
      }
    }

    return true;

  }
}

/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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

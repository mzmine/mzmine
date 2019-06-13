/*
 * Copyright (C) 2019 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ADAPpeakpicking;

import com.google.common.collect.Range;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;

import java.text.NumberFormat;
import java.util.Collection;

public class DoubleNonEmptyRangeParameter extends DoubleRangeParameter {

  public DoubleNonEmptyRangeParameter(String name, String description, NumberFormat format,
      boolean valueRequired, Range<Double> defaultValue) {

    super(name, description, format, valueRequired, defaultValue);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {

    String name = this.getName();
    Range<Double> value = this.getValue();

    if (valueRequired && (value == null)) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    if ((value != null) && (value.lowerEndpoint() >= value.upperEndpoint())) {
      errorMessages.add(name + " range maximum must be higher than minimum");
      return false;
    }

    return true;

  }

}

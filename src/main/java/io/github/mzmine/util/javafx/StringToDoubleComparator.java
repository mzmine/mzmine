/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.util.javafx;

import java.util.Comparator;

public class StringToDoubleComparator implements Comparator<String> {

  @Override
  public int compare(String o1, String o2) {
    if (o1 == null) {
      return o2 == null ? 0 : -1;
    }
    if (o2 == null) {
      return 1;
    }
    try {
      return Double.compare(Double.parseDouble(o1), Double.parseDouble(o2));
    } catch (Exception e) {
      return o1.compareTo(o2);
    }
  }
}

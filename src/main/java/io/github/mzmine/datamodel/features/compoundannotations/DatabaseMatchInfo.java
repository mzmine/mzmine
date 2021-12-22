/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.features.compoundannotations;

import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.OnlineDatabases;
import org.jetbrains.annotations.Nullable;

public record DatabaseMatchInfo(@Nullable OnlineDatabases onlineDatabase, @Nullable String id,
                                @Nullable String url) {

  public DatabaseMatchInfo(@Nullable OnlineDatabases onlineDatabase, @Nullable String id) {
    this(onlineDatabase, id, onlineDatabase != null ? onlineDatabase.getCompoundUrl(id) : null);
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    if (onlineDatabase != null) {
      b.append(onlineDatabase.getName()).append(" ");
    } else {
      b.append("N/A ");
    }

    if(id != null) {
      b.append(id);
    } else {
      b.append("no id");
    }
    return b.toString();
  }
}

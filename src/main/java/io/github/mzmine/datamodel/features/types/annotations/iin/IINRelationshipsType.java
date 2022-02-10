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

package io.github.mzmine.datamodel.features.types.annotations.iin;

import io.github.mzmine.datamodel.features.types.modifiers.EditableColumnType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.networks.IonNetworkRelation;
import java.util.List;
import java.util.Map.Entry;
import org.jetbrains.annotations.NotNull;

public class IINRelationshipsType extends
    ListDataType<Entry<IonNetwork, IonNetworkRelation>> implements EditableColumnType {

  @Override
  public @NotNull String getHeaderString() {
    return "Relationship";
  }

  @NotNull
  @Override
  public String getFormattedString(List<Entry<IonNetwork, IonNetworkRelation>> list) {
    return list == null ? ""
        : list.stream().findFirst().map(entry -> entry.getValue().getName(entry.getKey()))
            .orElse("");
  }

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "iin_relationship";
  }

}

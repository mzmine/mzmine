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

package io.github.mzmine.datamodel.identities.iontype.networks;


import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Relationships between {@link IonNetwork}s
 */
public interface IonNetworkRelation {

  /**
   * Name of the relationship to the argument network
   *
   * @param net the related network
   * @return relationship name to net, or null if there is no relationship
   */
  @Nullable
  String getName(IonNetwork net);

  /**
   * A general relationship description
   *
   * @return description
   */
  @NotNull
  String getDescription();

  /**
   * All IonNetworks in this relationship. A relationship might be between two IonNetworks ({@link
   * IonNetworkModificationRelation}) or between multiple (e.g., {@link IonNetworkHeteroCondensedRelation})
   *
   * @return an array of related networks
   */
  @NotNull
  IonNetwork[] getAllNetworks();

  /**
   * A method to check if net is the network with the lowest ID. Useful to only apply methods once,
   * e.g., exporting the relationship to text
   *
   * @param net the tested network (should be one of {@link #getAllNetworks()}
   * @return true if getID() is the lowest
   */
  default boolean isLowestIDNetwork(IonNetwork net) {
    return Arrays.stream(getAllNetworks()).noneMatch(n -> n.getID() < net.getID());
  }
}

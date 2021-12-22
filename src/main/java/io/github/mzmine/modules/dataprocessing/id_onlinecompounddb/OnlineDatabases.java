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

package io.github.mzmine.modules.dataprocessing.id_onlinecompounddb;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.databases.ChemSpiderGateway;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.databases.ChemSpiderParameters;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.databases.HMDBGateway;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.databases.KEGGGateway;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.databases.LipidMapsGateway;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.databases.MassBankEuropeGateway;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.databases.MetaCycGateway;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.databases.PubChemGateway;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.databases.YMDBGateway;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum OnlineDatabases implements MZmineModule {

  KEGG("KEGG", KEGGGateway.class), //
  PubChem("PubChem", PubChemGateway.class), //
  HMDB("Human Metabolome (HMDB)", HMDBGateway.class), //
  YMDB("Yeast Metabolome (YMDB)", YMDBGateway.class), //
  // METLIN("METLIN Database", MetLinGateway.class, MetLinParameters.class),
  LIPIDMAPS("LipidMaps", LipidMapsGateway.class), //
  // MASSBANKJapan("Japanese MassBank", MassBankJapanGateway.class), //
  MASSBANKEurope("MassBank.eu", MassBankEuropeGateway.class), //
  CHEMSPIDER("ChemSpider", ChemSpiderGateway.class, ChemSpiderParameters.class), //
  METACYC("MetaCyc", MetaCycGateway.class);

  private final @NotNull String dbName;
  private final @NotNull Class<? extends DBGateway> gatewayClass;
  private final @NotNull Class<? extends ParameterSet> parametersClass;

  OnlineDatabases(final @NotNull String dbName,
      final @NotNull Class<? extends DBGateway> gatewayClass,
      final @NotNull Class<? extends ParameterSet> parametersClass) {
    this.dbName = dbName;
    this.gatewayClass = gatewayClass;
    this.parametersClass = parametersClass;
  }

  OnlineDatabases(final @NotNull String name,
      final @NotNull Class<? extends DBGateway> gatewayClass) {
    this(name, gatewayClass, SimpleParameterSet.class);
  }

  public Class<? extends DBGateway> getGatewayClass() {
    return gatewayClass;
  }

  public @NotNull String getName() {
    return dbName;
  }

  @Nullable
  public String getCompoundUrl(@Nullable String databaseCompoundId) {
    if (databaseCompoundId == null) {
      return null;
    }

    return switch (this) {
      case PubChem -> "https://pubchem.ncbi.nlm.nih.gov/compound/" + databaseCompoundId;
      case KEGG, METACYC, CHEMSPIDER, MASSBANKEurope, LIPIDMAPS, YMDB, HMDB -> null;
    };
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return parametersClass;
  }
}

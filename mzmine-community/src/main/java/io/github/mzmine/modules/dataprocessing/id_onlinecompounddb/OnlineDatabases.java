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
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @deprecated because of old API usage. Hard to maintain. This was removed from the interfaces and
 * is only here as reference point
 */
@Deprecated
public enum OnlineDatabases implements MZmineModule, ModuleOptionsEnum<OnlineDatabases> {

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


  @Override
  public Class<? extends OnlineDatabases> getModuleClass() {
    return OnlineDatabases.class;
  }

  @Override
  public String getStableId() {
    return dbName;
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

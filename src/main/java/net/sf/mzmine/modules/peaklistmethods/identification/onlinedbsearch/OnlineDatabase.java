/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch;

import javax.annotation.Nonnull;

import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases.ChemSpiderGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases.ChemSpiderParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases.HMDBGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases.KEGGGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases.LipidMapsGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases.MassBankEuropeGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases.MassBankJapanGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases.MetaCycGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases.PubChemGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases.YMDBGateway;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;

public enum OnlineDatabase implements MZmineModule {

  KEGG("KEGG Compound Database", KEGGGateway.class), //
  PubChem("PubChem Compound Database", PubChemGateway.class), //
  HMDB("Human Metabolome Database (HMDB)", HMDBGateway.class), //
  YMDB("Yeast Metabolome Database (YMDB)", YMDBGateway.class), //
  // METLIN("METLIN Database", MetLinGateway.class, MetLinParameters.class),
  LIPIDMAPS("LipidMaps Database", LipidMapsGateway.class), //
  MASSBANKJapan("Japanese MassBank", MassBankJapanGateway.class), //
  MASSBANKEurope("European MassBank", MassBankEuropeGateway.class), //
  CHEMSPIDER("ChemSpider Database", ChemSpiderGateway.class, ChemSpiderParameters.class), //
  METACYC("MetaCyc Database", MetaCycGateway.class);

  private final String dbName;
  private final Class<? extends DBGateway> gatewayClass;
  private final Class<? extends ParameterSet> parametersClass;

  OnlineDatabase(final String dbName, final Class<? extends DBGateway> gatewayClass,
      final Class<? extends ParameterSet> parametersClass) {
    this.dbName = dbName;
    this.gatewayClass = gatewayClass;
    this.parametersClass = parametersClass;
  }

  OnlineDatabase(final String name, final Class<? extends DBGateway> gatewayClass) {
    this(name, gatewayClass, SimpleParameterSet.class);
  }

  public Class<? extends DBGateway> getGatewayClass() {
    return gatewayClass;
  }

  public @Nonnull String getName() {
    return dbName;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return parametersClass;
  }
}

/*
 * Copyright 2006-2012 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.dbsearch;

import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.databases.*;
import net.sf.mzmine.parameters.ParameterSet;

public enum OnlineDatabase implements MZmineModule {

    KEGG("KEGG Compound Database", KEGGGateway.class),
    PubChem("PubChem Compound Database", PubChemGateway.class),
    HMDB("Human Metabolome Database", HMDBGateway.class),
    // METLIN SOAP gateway is broken
    // METLIN("METLIN Database", MetLinGateway.class),
    LIPIDMAPS("LipidMaps Database", LipidMapsGateway.class),
    MASSBANK("MassBank Database", MassBankGateway.class),
    CHEMSPIDER("ChemSpider Database", ChemSpiderGateway.class, new ChemSpiderParameters());

    private final String dbName;
    private final Class<? extends DBGateway> gatewayClass;
    private final ParameterSet parameters;

    OnlineDatabase(final String name,
                   final Class<? extends DBGateway> aClass,
                   final ParameterSet parameterSet) {
        dbName = name;
        gatewayClass = aClass;
        parameters = parameterSet;
    }

    OnlineDatabase(final String name,
                   final Class<? extends DBGateway> aClass) {
        this(name, aClass, null );
    }

    public Class<? extends DBGateway> getGatewayClass() {
        return gatewayClass;
    }

    public String toString() {
        return dbName;
    }

    @Override
    public ParameterSet getParameterSet() {
        return parameters;
    }
}

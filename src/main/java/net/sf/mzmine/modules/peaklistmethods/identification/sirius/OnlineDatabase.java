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

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import javax.annotation.Nonnull;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.databases.KEGGGateway;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;

public enum OnlineDatabase implements MZmineModule {
  ;
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

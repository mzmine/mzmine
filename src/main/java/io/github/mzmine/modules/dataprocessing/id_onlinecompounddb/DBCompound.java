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

import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import java.net.URL;

/**
 * Currently only still here to not destroy robins pr. will delete after.
 */
public class DBCompound extends SimpleCompoundDBAnnotation {

  public DBCompound(OnlineDatabases db, String id, String name, String formula, URL urlDb,
      URL url2d, URL url3d) {
    super(db, id, name, formula, urlDb, url2d, url3d);
  }
}

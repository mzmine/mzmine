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

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.features.types.abstr.UrlType;
import org.jetbrains.annotations.NotNull;

/**
 * URL to MASST job on GNPS. MASST is the mass spectrometry search tool. e.g. <a
 * href="https://gnps.ucsd.edu/ProteoSAFe/status.jsp?task=fa0437e82d0a4a4493c8c2dcb4977c07">https://gnps.ucsd.edu/ProteoSAFe/status.jsp?task=fa0437e82d0a4a4493c8c2dcb4977c07</a>
 *
 * @author Robin Schmid (<a
 * href="https://github.com/robinschmid">https://github.com/robinschmid</a>)
 */
public class MasstUrlType extends UrlType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "masst_url";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "MASST";
  }
}

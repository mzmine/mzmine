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

package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import com.Ostermiller.util.CSVParser;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BioTransformerLibrary {

  private final File file;
  private final IonType[] ionTypes;

  public BioTransformerLibrary(File path, IonType... types) {
    this.file = path;
    this.ionTypes = types;
  }

  public List<CompoundDBAnnotation> parseLibrary(final AtomicBoolean canceled, final AtomicInteger parsedLines)
      throws IOException {

    final FileReader dbFileReader = new FileReader(file);
    final CSVParser parser = new CSVParser(dbFileReader, ',');

    final List<CompoundDBAnnotation> annotations = new ArrayList<>();

    parser.getLine();
    String[] line = null;
    while ((line = parser.getLine()) != null && !canceled.get()) {
      for (final IonType ionType : ionTypes) {
        annotations.add(BioTransformerAnnotation.fromCsvLine(line, ionType));
      }
      parsedLines.getAndIncrement();
    }

    return annotations;
  }
}

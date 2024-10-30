/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.datamodel.identities;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public record GlobalIonLibrary(List<IonPart> parts, List<IonType> ionTypes) {

  private static final Logger logger = Logger.getLogger(GlobalIonLibrary.class.getName());

  public static File getGlobalFile() {
    return FileAndPathUtil.resolveInMzmineDir("libraries/mzmine_global_ions.json");
  }

  public static GlobalIonLibrary loadGlobalIonLibrary() {
    File file = getGlobalFile();
    GlobalIonLibrary global = null;
    try {
      return loadJson(file);
    } catch (IOException ex) {
      // create a new global list
      var types = Arrays.stream(IonTypes.values()).map(IonTypes::asIonType)
          .collect(Collectors.toCollection(ArrayList::new));
      global = new GlobalIonLibrary(new ArrayList<>(IonParts.PREDEFINED_PARTS), types);

      if (file.exists()) {
        logger.warning(
            "Cannot load file: " + file.getAbsolutePath() + " because " + ex.getMessage());
      } else {
        logger.info("Initializing ion libraries file: " + file.getAbsolutePath());
        try {
          global.saveGlobalLibrary();
        } catch (IOException e) {
          logger.log(Level.WARNING,
              "Cannot initialize ion libraries file in: " + file.getAbsolutePath()
              + ". Will continue with default list of ions.", ex);
        }
      }
    }
    return global;
  }

  @NotNull
  public static GlobalIonLibrary loadJson(final @NotNull File file) throws IOException {
    return new ObjectMapper().readValue(file, GlobalIonLibrary.class);
  }

  public void saveGlobalLibrary() throws IOException {
    saveJson(getGlobalFile());
  }

  public void saveJson(final @NotNull File file) throws IOException {
    FileAndPathUtil.createDirectory(file.getParentFile());
    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file, this);
  }

}

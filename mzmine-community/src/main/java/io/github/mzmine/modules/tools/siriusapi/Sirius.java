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

package io.github.mzmine.modules.tools.siriusapi;

import de.unijena.bioinf.ms.nightsky.sdk.NightSkyClient;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

public class Sirius {

  private static final Logger logger = Logger.getLogger(Sirius.class.getName());

  private final NightSkyClient client;

  public Sirius(NightSkyClient client) {
    this.client = client;
  }

  public static NightSkyClient launchOrGetClient() {
    final File siriusDir = new File(FileUtils.getUserDirectory(), ".sirius-6.0");
    final File portFile = new File(siriusDir, "sirius.port");
    final File pidFile = new File(siriusDir, "sirius.pid");

    final Integer port = getPort(portFile);

    if (port == null) {
      logger.info("Sirius not running yet, launching.");
      return new NightSkyClient();
    }
    logger.info("Sirius already running at port: " + port);
    return new NightSkyClient(port.intValue());
  }

  private static @Nullable Integer getPort(File portFile) {
    String portString = null;
    if (!portFile.exists() || !portFile.canRead()) {
      return null;
    }

    try (var reader = Files.newBufferedReader(portFile.toPath(), StandardCharsets.UTF_8)) {
      portString = reader.readLine();
    } catch (IOException e) {
      logger.log(Level.WARNING, "Cannot read sirius port file.", e);
    }

    Integer port = null;
    if (portString != null && !portString.isBlank()) {
      port = Integer.parseInt(portString);
    }
    return port;
  }

  public void doSomething() {
    final de.unijena.bioinf.ms.nightsky.sdk.api.FeaturesApi features = client.features();
  }

  public static void main(String[] args) {
    new Sirius(launchOrGetClient());
    return;
  }
}

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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mzmine.util.dependencylicenses.Dependencies;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

public class DependencyLicenseTest {

  private static final Logger logger = Logger.getLogger(DependencyLicenseTest.class.getName());

  @Test
  void test() {
    final ObjectMapper mapper = new ObjectMapper();
    final URL resource = this.getClass().getClassLoader()
        .getResource("dependency/dependency-licenses.json");
    try (InputStream dps = (Files.newInputStream(Paths.get(resource.toURI()),
        StandardOpenOption.READ))) {
      final Dependencies dependency = mapper.readValue(dps, Dependencies.class);
      logger.info(dependency.toString());
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}

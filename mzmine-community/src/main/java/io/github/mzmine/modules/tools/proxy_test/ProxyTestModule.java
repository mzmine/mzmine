/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.tools.proxy_test;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.AbstractRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.CsvWriter;
import io.mzio.links.MzioMZmineLinks;
import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class ProxyTestModule extends AbstractRunnableModule {

  public ProxyTestModule() {
    super("Proxy test report", ProxyTestParameters.class, MZmineModuleCategory.TOOLS,
        "Test proxy settings and export results to a file");
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    final File file = parameters.getValue(ProxyTestParameters.exportFile);

    proxyTestReport(file);

    return ExitCode.OK;
  }

  private void proxyTestReport(File file) {
    file = FileAndPathUtil.getRealFilePath(file, "csv");

    final ProxySelector selector = ProxySelector.getDefault();
    List<String> urls = List.of("https://auth.mzio.io/", MzioMZmineLinks.MZIO.getUrl(),
        MzioMZmineLinks.USER_CONSOLE.getUrl());

    List<ProxyTestCase> cases = new ArrayList<>();

    for (String url : urls) {
      final List<Proxy> proxies = selector == null ? null : selector.select(URI.create(url));

      if (proxies == null || proxies.isEmpty()) {
        cases.add(new ProxyTestCase(url, "No proxy", null, null, null));
        continue;
      }

      for (Proxy proxy : proxies) {
        cases.add(new ProxyTestCase(url, proxy.toString(), proxy.type(), proxy.address().toString(),
            proxy.address().getClass().toString()));
      }
    }

    try {
      CsvWriter.writeToFile(file, cases, ProxyTestCase.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  record ProxyTestCase(String testUrl, String proxy, Proxy.Type type, String address,
                       String addressClass) {


  }

}

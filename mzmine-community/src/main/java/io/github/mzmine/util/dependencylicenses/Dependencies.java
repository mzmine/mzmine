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

package io.github.mzmine.util.dependencylicenses;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.scene.control.ListView;

public record Dependencies(List<Dependency> dependencies) {

  private static final List<Dependency> other = List.of(
      new Dependency("TDF Software Development Kit, Bruker Daltonics GmbH & Co.KG",
          "2.8.7.1-win32-vc141", List.of(),
          List.of(new ModuleLicense("EULA TDF-SDK (Bruker Daltonics GmbH & Co.KG)", null))),
      new Dependency("Baf2Sql Software Development Kit, Bruker Daltonics GmbH & Co.KG", "2.9.0",
          List.of(),
          List.of(new ModuleLicense("EULA BAF-SDK (Bruker Daltonics GmbH & Co.KG)", null))),
      new Dependency("ThermoRawFileParser", "1.4.3",
          List.of("https://github.com/compomics/ThermoRawFileParser"), List.of(
          new ModuleLicense("Apache License, Version 2.0",
              "https://github.com/compomics/ThermoRawFileParser?tab=Apache-2.0-1-ov-file#readme"))),
      new Dependency("MassLynxRaw library", "2014", List.of(),
          List.of(new ModuleLicense("Copyright © 2014 Waters, Inc.", null))));

  public static List<Dependency> of(String resourcePath) {
    final ObjectMapper mapper = new ObjectMapper();
    try (InputStream dps = Dependencies.class.getClassLoader().getResourceAsStream(resourcePath)) {
      final Dependencies dependency = mapper.readValue(dps, Dependencies.class);
      return dependency.dependencies();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Map<ModuleLicense, List<Dependency>> groupByLicense(List<Dependency> dependencies) {
    final Map<ModuleLicense, List<Dependency>> mapped = dependencies.stream().filter(
            dependency -> dependency.moduleLicenses() != null && !dependency.moduleLicenses().isEmpty())
        .sorted(Comparator.comparing(o -> o.moduleLicenses().getFirst()))
        .collect(Collectors.groupingBy(dependency -> dependency.moduleLicenses().getFirst()));
    mapped.forEach((license, dps) -> dps.sort(Comparator.comparing(Dependency::moduleName)));
    return mapped;
  }

  public static ListView<Dependency> asListView(List<Dependency> dps) {
    final Map<ModuleLicense, List<Dependency>> grouped = groupByLicense(dps);
    final List<Dependency> sortedList = grouped.values().stream().flatMap(Collection::stream)
        .toList();
    final ListView<Dependency> view = new ListView<>();
    view.setCellFactory(_ -> new DependencyListCell());
    view.getItems().addAll(sortedList);
    return view;
  }

  public static ListView<Dependency> listViewOfAllDependencies() {
    final List<Dependency> dependencies = new ArrayList<>(
        of("dependency/dependency-licenses.json"));
    dependencies.addAll(other);
    return asListView(dependencies);
  }
}

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

package io.github.mzmine.parameters.parametertypes.proxy;

import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldLabel;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.web.ProxyType;
import io.github.mzmine.util.web.ProxyUtils;
import io.github.mzmine.util.web.proxy.FullProxyConfig;
import io.github.mzmine.util.web.proxy.ManualProxyConfig;
import io.github.mzmine.util.web.proxy.ProxyConfigOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

public class ProxyConfigComponent extends BorderPane {

  private final ObjectProperty<ProxyConfigOption> option = new SimpleObjectProperty<>(
      ProxyConfigOption.AUTO_PROXY);

  // manual
  private final ObjectProperty<ProxyType> proxyType = new SimpleObjectProperty<>(ProxyType.HTTP);
  private final StringProperty host = new SimpleStringProperty("");
  private final ObjectProperty<Integer> port = new SimpleObjectProperty<>(80);
  private final StringProperty nonProxyHosts = new SimpleStringProperty("");

  // content for each option (some are null)
  private final Map<ProxyConfigOption, Region> optionPanes = HashMap.newHashMap(4);


  public ProxyConfigComponent(FullProxyConfig value) {
    setValue(value);
    createLayout();
  }

  private void createLayout() {
    createOptionContentPanes();

    final ComboBox<ProxyConfigOption> optionCombo = FxComboBox.createComboBox(
        "Options how to set proxy settings", ProxyConfigOption.values(), option);

    final Button testButton = FxButtons.createButton("Test",
        "Apply and test proxy configuration for important websites. Results will be printed to the logs.",
        this::testProxy);

    setTop(FxLayout.newHBox(optionCombo, testButton));

    centerProperty().bind(option.map(optionPanes::get).orElse(null));
  }

  private void createOptionContentPanes() {
    // manual
    GridPane grid = FxLayout.newGrid2Col( //
        newBoldLabel("Host name"), FxTextFields.newTextField(host, "Host name like 'localhost'"), //
        newBoldLabel("Port number"), FxTextFields.newIntegerField(port, "Port number like 80"), //
        newBoldLabel("No proxy for"), FxTextFields.newTextField(nonProxyHosts,
            "Comma-separated hosts that are excluded. Example: '*.example.com, 192.168.*'") //
    );

    optionPanes.put(ProxyConfigOption.MANUAL_PROXY, grid);
    // AUTO and NO_PROXY have no content so far but could get fallback options later

  }

  private void testProxy() {
    final FullProxyConfig value = getValue();
    ProxyUtils.applyConfig(value);

  }

  public void setValue(FullProxyConfig value) {
    option.set(value.option());

    // manual
    proxyType.set(value.manualConfig().type());
    host.set(value.manualConfig().host());
    port.set(value.manualConfig().port());
    nonProxyHosts.set(String.join(",", value.manualConfig().nonProxyHosts()));
  }

  public FullProxyConfig getValue() {
    final List<String> nonProxyHosts = getNonProxyHosts();
    final ManualProxyConfig manual = new ManualProxyConfig(proxyType.get(), host.get(), port.get(),
        nonProxyHosts);
    return new FullProxyConfig(option.get(), manual);
  }

  private List<String> getNonProxyHosts() {
    final String hosts = this.nonProxyHosts.get();
    if (hosts == null) {
      return List.of();
    }
    // split and remove spaces
    return Arrays.stream(hosts.split(",")).map(String::trim).filter(StringUtils::hasValue).toList();
  }

}

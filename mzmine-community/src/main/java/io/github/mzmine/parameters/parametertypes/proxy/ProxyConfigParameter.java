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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.parameters.AbstractParameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.web.proxy.FullProxyConfig;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class ProxyConfigParameter extends AbstractParameter<FullProxyConfig, ProxyConfigComponent> {

  @NotNull
  private FullProxyConfig value = FullProxyConfig.defaultConfig();

  public ProxyConfigParameter() {
    super("Proxy config", "Proxy configuration");
  }

  @Override
  public ProxyConfigComponent createEditingComponent() {
    return new ProxyConfigComponent(value);
  }

  @Override
  public void setValueFromComponent(ProxyConfigComponent proxyConfigComponent) {
    value = proxyConfigComponent.getValue();
  }

  @Override
  public void setValueToComponent(ProxyConfigComponent proxyConfigComponent,
      @Nullable FullProxyConfig newValue) {
    // handle null with default
    proxyConfigComponent.setValue(requireNonNullElse(newValue, FullProxyConfig.defaultConfig()));
  }

  @Override
  public FullProxyConfig getValue() {
    return value;
  }

  @Override
  public void setValue(@Nullable FullProxyConfig newValue) {
    if (newValue == null) {
      newValue = FullProxyConfig.defaultConfig();
    } else {
      this.value = newValue;
    }
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    // any checks needed? it is never null
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {

  }

  @Override
  public void saveValueToXML(Element xmlElement) {

  }

  @Override
  public UserParameter<FullProxyConfig, ProxyConfigComponent> cloneParameter() {
    final ProxyConfigParameter copy = new ProxyConfigParameter();
    copy.setValue(this.getValue());
    return copy;
  }
}

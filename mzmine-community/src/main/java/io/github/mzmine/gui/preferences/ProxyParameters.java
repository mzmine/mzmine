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

package io.github.mzmine.gui.preferences;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.util.web.Proxy;
import io.github.mzmine.util.web.ProxyType;
import io.mzio.mzmine.datamodel.parameters.Parameter;
import java.util.Map;

/**
 * Proxy server settings
 */
public class ProxyParameters extends SimpleParameterSet {

  public static final ComboParameter<ProxyType> proxyType = new ComboParameter<>("Proxy type",
      "Set if the proxy is an http or an https proxy.", ProxyType.values(), ProxyType.HTTP);

  public static final StringParameter proxyAddress = new StringParameter("Proxy address",
      "Internet address of a proxy server");

  public static final StringParameter proxyPort = new StringParameter("Proxy port",
      "TCP port of proxy server");

  public ProxyParameters() {
    super(proxyType, proxyAddress, proxyPort);
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    var map = super.getNameParameterMap();
    map.put("Proxy adress", proxyAddress);
    return map;
  }

  @Override
  public int getVersion() {
    return 2;
  }

  public void setProxy(final Proxy proxy) {
    if (proxy.address() != null) {
      setParameter(proxyAddress, proxy.address());
    }
    if (proxy.port() != null) {
      setParameter(proxyPort, proxy.port());
    }
    setParameter(proxyType, proxy.type());
  }
}

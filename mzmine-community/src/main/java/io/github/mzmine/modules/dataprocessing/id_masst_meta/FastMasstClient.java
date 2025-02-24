package io.github.mzmine.modules.dataprocessing.id_masst_meta;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.util.web.HttpUtils;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;

public class FastMasstClient implements AutoCloseable {

  private final HttpClient client;

  public FastMasstClient() {
    client = HttpUtils.createHttpClient(true);
  }

  public String searchSpectrum(double precursorMz, int charge, MassSpectrum spec,
      FastMasstRequestConfig config) {
//    client.send()
    return "";
  }

  @Override
  public void close() throws Exception {
    if (client == null) {
      return;
    }
    client.close();
  }
}

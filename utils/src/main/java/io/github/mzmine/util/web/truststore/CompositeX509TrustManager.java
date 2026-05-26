/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util.web.truststore;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.X509TrustManager;

/**
 * A TrustManager that delegates to multiple underlying TrustManagers. A certificate is trusted if
 * ANY of the delegates trusts it.
 */
class CompositeX509TrustManager implements X509TrustManager {

  private final List<X509TrustManager> delegates;

  CompositeX509TrustManager(X509TrustManager... managers) {
    this.delegates = List.of(managers);
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {
    CertificateException lastException = null;
    for (X509TrustManager tm : delegates) {
      try {
        tm.checkClientTrusted(chain, authType);
        return; // trusted by this manager
      } catch (CertificateException e) {
        if (lastException != null) {
          lastException.addSuppressed(e);
        } else {
          lastException = e;
        }
      }
    }
    if (lastException != null) {
      throw lastException;
    }
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {
    CertificateException lastException = null;
    for (X509TrustManager tm : delegates) {
      try {
        tm.checkServerTrusted(chain, authType);
        return; // trusted by this manager
      } catch (CertificateException e) {
        if (lastException != null) {
          lastException.addSuppressed(e);
        } else {
          lastException = e;
        }
      }
    }
    if (lastException != null) {
      throw lastException;
    }
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    List<X509Certificate> allIssuers = new ArrayList<>();
    for (X509TrustManager tm : delegates) {
      allIssuers.addAll(Arrays.asList(tm.getAcceptedIssuers()));
    }
    return allIssuers.toArray(new X509Certificate[0]);
  }
}

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


import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Option to load the native windows and macOS trust stores and merge them with the cacerts provided
 * with mzmine
 */
public class NativeTrustStoreManager {

  private static final Logger logger = Logger.getLogger(NativeTrustStoreManager.class.getName());

  /**
   * merge OS trust store with the cacerts from java seems like windows trust store handles external
   * custom root certs in a way that they need to be requested once and the JVM option below only
   * retrieves a static view of currently available certs
   * -Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE
   * <p>
   * if javax.net.ssl.trustStoreType is defined, then use only this trust store (already loaded)
   */
  public static void initTrustStore() {
    final String jvmTrustStoreChoice = System.getProperty("javax.net.ssl.trustStore", "");
    if (!jvmTrustStoreChoice.isEmpty()) {
      logger.info("Trust store has been set by JVM arg to " + jvmTrustStoreChoice);
      return;
    }
    try {
      final SSLContext sslContext = createMergedSSLContext();
      SSLContext.setDefault(sslContext);
      // also set the old http/1 style
      HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
    } catch (Exception ex) {
      logger.log(Level.SEVERE,
          "Could not initialize merged trust store with OS trust store. Will skip this step. "
              + ex.getMessage(), ex);
    }
  }

  /**
   * Creates an SSLContext that trusts certificates from BOTH the bundled JDK cacerts AND the
   * OS-native trust store (Windows-ROOT, KeychainStore). Falls back to default cacerts-only if
   * native store is unavailable.
   */
  public static SSLContext createMergedSSLContext() throws Exception {
    // 1. Load the default JDK cacerts TrustManager
    TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm());
    defaultTmf.init((KeyStore) null); // null = use default cacerts
    X509TrustManager defaultTm = findX509TrustManager(defaultTmf);

    // 2. Try to load the OS-native trust store
    X509TrustManager nativeTm = loadNativeTrustManager();

    // 3. Build the SSLContext
    SSLContext sslContext = SSLContext.getInstance("TLS");

    if (nativeTm != null) {
      // Merge both trust managers
      X509TrustManager compositeTm = new CompositeX509TrustManager(defaultTm, nativeTm);
      // seems like keymanager is not needed here only if client side authentication is required.
      sslContext.init(null, new TrustManager[]{compositeTm}, null);
      logger.info("SSL context initialized with merged trust store " + "(JDK cacerts + OS native)");
    } else {
      // Fall back to default only
      // seems like keymanager is not needed here only if client side authentication is required.
      sslContext.init(null, new TrustManager[]{defaultTm}, null);
      logger.info(
          "SSL context initialized with JDK cacerts only " + "(no native trust store available)");
    }

    return sslContext;
  }

  /**
   * Attempts to load the OS-native trust store. Returns null if not available on this platform.
   */
  private static X509TrustManager loadNativeTrustManager() {

    String storeType = null;
    try {
      String osName = System.getProperty("os.name", "").toLowerCase();

      final KeyStore nativeStore;
      if (osName.contains("win")) {
        storeType = "Windows-ROOT";
        nativeStore = KeyStore.getInstance(storeType);
      } else if (osName.contains("mac")) {
        storeType = "KeychainStore";
        nativeStore = KeyStore.getInstance(storeType, "Apple");
      } else if (osName.contains("nux") || osName.contains("nix")) {
        // Linux has no native keystore API
        // TODO load certs from common paths
        nativeStore = null;
      } else {
        // unknown system
        nativeStore = null;
      }

      if (nativeStore == null) {
        return null;
      }

      nativeStore.load(null, null); // no file, no password for native stores
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(
          TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(nativeStore);
      logger.info("Loaded native trust store: " + storeType);
      return findX509TrustManager(tmf);
    } catch (Exception e) {
      logger.log(Level.WARNING,
          "Could not load native trust store '" + storeType + "': " + e.getMessage(), e);
      return null;
    }
  }

  private static X509TrustManager findX509TrustManager(TrustManagerFactory tmf) {
    for (TrustManager tm : tmf.getTrustManagers()) {
      if (tm instanceof X509TrustManager x509Tm) {
        return x509Tm;
      }
    }
    throw new IllegalStateException("No X509TrustManager found");
  }

}

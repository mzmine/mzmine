/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.github.mzmine.gui.update;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
import io.github.mzmine.util.io.SemverVersionReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Checks the official mzmine release version without opening UI or starting an installation.
 */
public final class UpdateCheckService {

  public static final String VERSION_URL = "https://mzmine.github.io/version.txt";
  public static final String RELEASE_URL = "https://github.com/mzmine/mzmine/releases/latest";
  private static final Duration CACHE_TTL = Duration.ofMinutes(30);
  private static final int TIMEOUT_MILLIS = 3_000;
  private static final int REQUEST_TIMEOUT_MILLIS = 5_000;
  private static final int MAX_RESPONSE_BYTES = 8_192;
  private static final ScheduledExecutorService DEADLINE_EXECUTOR =
      Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().factory());
  private static final UpdateCheckService INSTANCE = new UpdateCheckService(
      SemverVersionReader::getMZmineVersion, UpdateCheckService::fetchVersion, Clock.systemUTC(),
      CACHE_TTL);

  private final Supplier<Semver> currentVersionSupplier;
  private final VersionFetcher versionFetcher;
  private final Clock clock;
  private final Duration cacheTtl;
  private volatile UpdateStatus cachedStatus;

  UpdateCheckService(@NotNull final Supplier<Semver> currentVersionSupplier,
      @NotNull final VersionFetcher versionFetcher, @NotNull final Clock clock,
      @NotNull final Duration cacheTtl) {
    this.currentVersionSupplier = Objects.requireNonNull(currentVersionSupplier);
    this.versionFetcher = Objects.requireNonNull(versionFetcher);
    this.clock = Objects.requireNonNull(clock);
    this.cacheTtl = Objects.requireNonNull(cacheTtl);
  }

  @NotNull
  public static UpdateCheckService getInstance() {
    return INSTANCE;
  }

  /**
   * Returns the cached result while it is fresh, otherwise performs one bounded HTTPS request.
   */
  @NotNull
  public UpdateStatus check() {
    final UpdateStatus previous = cachedStatus;
    if (isFresh(previous)) {
      return previous;
    }
    synchronized (this) {
      if (isFresh(cachedStatus)) {
        return cachedStatus;
      }
      cachedStatus = checkNow();
      return cachedStatus;
    }
  }

  private boolean isFresh(@Nullable final UpdateStatus status) {
    if (status == null) {
      return false;
    }
    try {
      return !Instant.parse(status.checkedAt()).plus(cacheTtl).isBefore(clock.instant());
    } catch (RuntimeException ignored) {
      return false;
    }
  }

  @NotNull
  UpdateStatus checkNow() {
    final Semver currentVersion = currentVersionSupplier.get();
    final String checkedAt = clock.instant().toString();
    try {
      final String versionText = versionFetcher.fetch(new URL(VERSION_URL)).trim();
      final Semver latestVersion = new Semver(versionText, SemverType.LOOSE);
      return compare(currentVersion, latestVersion, checkedAt);
    } catch (Exception e) {
      return new UpdateStatus(UpdateStatus.UNAVAILABLE, currentVersion.toString(), null, checkedAt,
          RELEASE_URL, "Could not retrieve the official mzmine version: " + e.getMessage());
    }
  }

  @NotNull
  static UpdateStatus compare(@NotNull final Semver currentVersion, @NotNull final Semver latestVersion,
      @NotNull final String checkedAt) {
    final String status;
    if (currentVersion.isLowerThan(latestVersion)) {
      status = UpdateStatus.UPDATE_AVAILABLE;
    } else if (currentVersion.isEquivalentTo(latestVersion)) {
      status = UpdateStatus.CURRENT;
    } else {
      status = UpdateStatus.DEVELOPMENT_NEWER;
    }
    return new UpdateStatus(status, currentVersion.toString(), latestVersion.toString(), checkedAt,
        RELEASE_URL, null);
  }

  @NotNull
  private static String fetchVersion(@NotNull final URL url) throws IOException {
    if (!"https".equalsIgnoreCase(url.getProtocol())) {
      throw new IOException("Update metadata must use HTTPS");
    }
    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setConnectTimeout(TIMEOUT_MILLIS);
    connection.setReadTimeout(TIMEOUT_MILLIS);
    connection.setInstanceFollowRedirects(false);
    connection.setRequestProperty("User-Agent", "mzmine-update-check");
    // decision: disconnecting the fixed connection enforces an overall deadline even for trickle data.
    final ScheduledFuture<?> deadline = DEADLINE_EXECUTOR.schedule(connection::disconnect,
        REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    try {
      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        throw new IOException("Official version service returned HTTP " + connection.getResponseCode());
      }
      try (InputStream input = connection.getInputStream()) {
        final byte[] bytes = input.readNBytes(MAX_RESPONSE_BYTES + 1);
        if (bytes.length > MAX_RESPONSE_BYTES) {
          throw new IOException("Official version response is too large");
        }
        return new String(bytes, StandardCharsets.UTF_8);
      }
    } finally {
      deadline.cancel(false);
      connection.disconnect();
    }
  }

  @FunctionalInterface
  interface VersionFetcher {

    @NotNull String fetch(@NotNull URL url) throws IOException;
  }
}

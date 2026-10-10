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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class UpdateCheckServiceTest {

  private static final String CHECKED_AT = "2026-10-07T10:15:30Z";

  @Test
  void comparesCurrentReleaseAndDevelopmentVersions() {
    final Semver current = version("4.7.1");
    assertEquals(UpdateStatus.CURRENT,
        UpdateCheckService.compare(current, version("4.7.1"), CHECKED_AT).status());
    assertEquals(UpdateStatus.UPDATE_AVAILABLE,
        UpdateCheckService.compare(current, version("4.7.2"), CHECKED_AT).status());
    assertEquals(UpdateStatus.DEVELOPMENT_NEWER,
        UpdateCheckService.compare(current, version("4.7.0"), CHECKED_AT).status());
  }

  @Test
  void reportsUnavailableForFailedFetchWithoutInventingALatestVersion() {
    final UpdateCheckService service = new UpdateCheckService(() -> version("4.7.1"), url -> {
      throw new IOException("offline");
    }, fixedClock(), Duration.ofMinutes(30));

    final UpdateStatus result = service.checkNow();

    assertEquals(UpdateStatus.UNAVAILABLE, result.status());
    assertEquals("4.7.1", result.currentVersion());
    assertNull(result.latestVersion());
    assertEquals(CHECKED_AT, result.checkedAt());
  }

  @Test
  void reusesFreshResultInsteadOfRepeatingNetworkFetches() {
    final AtomicInteger fetches = new AtomicInteger();
    final UpdateCheckService service = new UpdateCheckService(() -> version("4.7.1"), url -> {
      fetches.incrementAndGet();
      return "4.7.2";
    }, fixedClock(), Duration.ofMinutes(30));

    final UpdateStatus first = service.check();
    final UpdateStatus second = service.check();

    assertEquals(UpdateStatus.UPDATE_AVAILABLE, first.status());
    assertEquals(first, second);
    assertEquals(1, fetches.get());
  }

  private static Clock fixedClock() {
    return Clock.fixed(Instant.parse(CHECKED_AT), ZoneOffset.UTC);
  }

  private static Semver version(final String value) {
    return new Semver(value, SemverType.LOOSE);
  }
}

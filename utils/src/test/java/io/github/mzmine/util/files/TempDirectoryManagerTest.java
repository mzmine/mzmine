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

package io.github.mzmine.util.files;

import static io.github.mzmine.util.files.TempDirectoryManager.MIN_USABLE_SPACE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TempDirectoryManagerTest {

  @TempDir
  Path root;

  @Test
  void failoverSkipsLowSpaceAndFailingDirectoriesInPriorityOrder() throws IOException {
    File first = root.resolve("first").toFile();
    File full = root.resolve("full").toFile();
    File unwritable = root.resolve("unwritable").toFile();
    File available = root.resolve("available").toFile();
    var manager = new TempDirectoryManager(dir -> dir.equals(full) ? 0 : MIN_USABLE_SPACE);
    manager.setDirectories(first, full, unwritable, available);
    List<Path> attempted = new ArrayList<>();

    Path file = manager.create(0, dir -> {
      attempted.add(dir);
      if (!dir.equals(available.toPath())) {
        throw new IOException("Cannot write to " + dir);
      }
      return Files.createTempFile(dir, "test", ".tmp");
    });

    assertEquals(List.of(first.toPath(), unwritable.toPath(), available.toPath()), attempted);
    assertEquals(available.toPath(), file.getParent());
    assertEquals(available, manager.getDirectory());
    // subsequent requests try the current directory first
    attempted.clear();
    manager.create(0, dir -> attempted.add(dir));
    assertEquals(List.of(available.toPath()), attempted);
  }

  @Test
  void currentDirectoryNeedsHeadroomBeyondTheRequestedSpace() throws IOException {
    File first = root.resolve("first").toFile();
    File second = root.resolve("second").toFile();
    long size = 1_000_000_000L;
    var manager = new TempDirectoryManager(
        dir -> dir.equals(first) ? MIN_USABLE_SPACE + size - 1 : MIN_USABLE_SPACE + size);
    manager.setDirectories(first, second);

    assertEquals(first.toPath(), manager.create(0, dir -> dir));
    assertEquals(second.toPath(), manager.create(size, dir -> dir));
    assertEquals(second, manager.getDirectory());
  }

  @Test
  void returnsToFirstDirectoryWhenSpaceHasBeenReleased() throws IOException {
    File first = root.resolve("first").toFile();
    File second = root.resolve("second").toFile();
    Map<File, Long> space = new HashMap<>(Map.of(first, 0L, second, MIN_USABLE_SPACE));
    var manager = new TempDirectoryManager(space::get);
    manager.setDirectories(first, second);
    manager.create(0, dir -> dir);
    assertEquals(second, manager.getDirectory());

    manager.setDirectories(first, second);
    assertEquals(second, manager.getDirectory());
    space.put(first, MIN_USABLE_SPACE);
    space.put(second, 0L);

    assertEquals(first.toPath(), manager.create(0, dir -> dir));
    assertEquals(first, manager.getDirectory());
  }

  @Test
  void lowSpaceEverywhereUsesTheDirectoryWithMostSpace() throws IOException {
    File first = root.resolve("first").toFile();
    File second = root.resolve("second").toFile();
    File third = root.resolve("third").toFile();
    Map<File, Long> space = Map.of(first, 1000L, second, 3000L, third, 2000L);
    var manager = new TempDirectoryManager(space::get);
    manager.setDirectories(first, second, third);

    assertEquals(second.toPath(), manager.create(2500, dir -> dir));
    assertEquals(second, manager.getDirectory());
  }

  @Test
  void lowSpaceFallbackStillRequiresTheRequestedSpace() {
    File first = root.resolve("first").toFile();
    File second = root.resolve("second").toFile();
    var manager = new TempDirectoryManager(_ -> 1000L);
    manager.setDirectories(first, second);

    IOException failure = assertThrows(IOException.class, () -> manager.create(1001, dir -> dir));

    assertEquals(2, failure.getSuppressed().length);
    assertTrue(failure.getMessage().contains("10 GB"));
    assertEquals(first, manager.getDirectory());
  }

  @Test
  void operationFailureTriggersFailover() throws IOException {
    File first = root.resolve("first").toFile();
    File second = root.resolve("second").toFile();
    var manager = new TempDirectoryManager(_ -> MIN_USABLE_SPACE + 1024);
    manager.setDirectories(first, second);

    Path result = manager.create(1024, dir -> {
      if (dir.equals(first.toPath())) {
        throw new IOException("Mapping failed after opening the file");
      }
      return dir;
    });

    assertEquals(second.toPath(), result);
    assertEquals(second, manager.getDirectory());
  }

  @Test
  void exhaustionReportsEveryCandidateAndKeepsTheCurrentDirectory() {
    File first = root.resolve("first").toFile();
    File second = root.resolve("second").toFile();
    var manager = new TempDirectoryManager(_ -> MIN_USABLE_SPACE);
    manager.setDirectories(first, second);

    IOException failure = assertThrows(IOException.class, () -> manager.create(0, _ -> {
      throw new IOException("Disk full");
    }));

    assertEquals(2, failure.getSuppressed().length);
    assertEquals(first, manager.getDirectory());
  }

  @Test
  void configurationKeepsInaccessibleDirectoriesAndStartsAtTheFirstAccessibleOne()
      throws IOException {
    File invalid = Files.createFile(root.resolve("file")).toFile();
    File second = root.resolve("second").toFile();
    File nested = new File(invalid, "nested");
    File third = Files.createDirectory(root.resolve("third")).toFile();
    var manager = new TempDirectoryManager();

    manager.setDirectories(invalid, second, nested, third);

    assertArrayEquals(new File[]{invalid, second, nested, third}, manager.getDirectories());
    assertEquals(second, manager.getDirectory());
    assertTrue(second.isDirectory());
  }

  @Test
  void inaccessibleConfigurationPreservesThePreviousListAndCurrentDirectory() throws IOException {
    File first = root.resolve("first").toFile();
    File second = root.resolve("second").toFile();
    var manager = new TempDirectoryManager(dir -> dir.equals(first) ? 0 : MIN_USABLE_SPACE);
    manager.setDirectories(first, second);
    manager.create(0, dir -> dir);
    assertEquals(second, manager.getDirectory());
    File invalid = Files.createFile(root.resolve("file")).toFile();

    manager.setDirectories(invalid, new File(invalid, "nested"));

    assertArrayEquals(new File[]{first, second}, manager.getDirectories());
    assertEquals(second, manager.getDirectory());
  }

  @Test
  void missingDirectoriesAreRecreated() throws IOException {
    File directory = root.resolve("directory").toFile();
    var manager = new TempDirectoryManager(_ -> MIN_USABLE_SPACE);
    manager.setDirectories(directory);
    Files.delete(directory.toPath());

    manager.setDirectories(directory);
    assertTrue(directory.isDirectory());

    Files.delete(directory.toPath());
    assertEquals(directory.toPath(), manager.create(0, dir -> dir));
    assertTrue(directory.isDirectory());
  }

  @Test
  void configurationIsCopiedAndEmptyConfigurationRestoresSystemDirectory() {
    File first = root.resolve("first").toFile();
    File second = root.resolve("second").toFile();
    var manager = new TempDirectoryManager();
    File defaultDirectory = manager.getDirectory();
    File[] configured = {first, second, first};
    manager.setDirectories(configured);
    configured[0] = root.resolve("other").toFile();
    File[] snapshot = manager.getDirectories();
    snapshot[0] = configured[0];

    assertArrayEquals(new File[]{first, second}, manager.getDirectories());
    manager.setDirectories();
    assertEquals(defaultDirectory, manager.getDirectory());
  }

  @Test
  void gettersRemainNonBlockingWhileCreationAndConfigurationShareOneLock() throws Exception {
    var manager = new TempDirectoryManager(_ -> MIN_USABLE_SPACE);
    File first = root.resolve("first").toFile();
    File second = root.resolve("second").toFile();
    manager.setDirectories(first);
    CountDownLatch creating = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch waiting = new CountDownLatch(2);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var creation = executor.submit(() -> manager.create(0, dir -> {
        creating.countDown();
        try {
          if (!release.await(10, TimeUnit.SECONDS)) {
            throw new IOException("Timed out waiting for test to release creation");
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException(e);
        }
        return dir;
      }));
      try {
        assertTrue(creating.await(5, TimeUnit.SECONDS));
        var configuration = executor.submit(() -> {
          waiting.countDown();
          manager.setDirectories(second);
        });
        var anotherCreation = executor.submit(() -> {
          waiting.countDown();
          return manager.create(0, dir -> dir);
        });
        assertTrue(waiting.await(5, TimeUnit.SECONDS));
        var read = executor.submit(manager::getDirectory);
        var readDirectories = executor.submit(manager::getDirectories);
        assertEquals(first, read.get(1, TimeUnit.SECONDS));
        assertArrayEquals(new File[]{first}, readDirectories.get(1, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> configuration.get(100, TimeUnit.MILLISECONDS));
        assertThrows(TimeoutException.class, () -> anotherCreation.get(100, TimeUnit.MILLISECONDS));
        release.countDown();
        assertNotNull(creation.get(5, TimeUnit.SECONDS));
        configuration.get(5, TimeUnit.SECONDS);
        assertNotNull(anotherCreation.get(5, TimeUnit.SECONDS));
        assertEquals(second, manager.getDirectory());
        assertArrayEquals(new File[]{second}, manager.getDirectories());
      } finally {
        release.countDown();
      }
    }
  }
}

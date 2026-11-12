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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Summarizes a JFR recording of one benchmark thread as markdown: the time by phase (the callees of
 * entry methods, e.g., the passes of the builder), the hot methods by own and by inclusive samples,
 * the allocation by class, by allocating mzmine method and by phase, and the garbage collections.
 * The allocation is the sum of the weights of the allocation samples, an estimate of all allocated
 * bytes.
 */
final class JfrProfileSummary {

  private static final String MZMINE_PACKAGE = "io.github.mzmine.";

  private final @NotNull String threadName;
  private final @NotNull List<String> entries;
  private final List<Object2LongOpenHashMap<String>> samplesByPhase = new ArrayList<>();
  private final List<Object2LongOpenHashMap<String>> allocationByPhase = new ArrayList<>();
  private final Object2LongOpenHashMap<String> ownSamples = new Object2LongOpenHashMap<>();
  private final Object2LongOpenHashMap<String> inclusiveSamples = new Object2LongOpenHashMap<>();
  private final Object2LongOpenHashMap<String> allocationByClass = new Object2LongOpenHashMap<>();
  private final Object2LongOpenHashMap<String> allocationBySite = new Object2LongOpenHashMap<>();
  private long samples;
  private long allocated;
  private int collections;
  private @NotNull Duration pauses = Duration.ZERO;
  private long maxHeapAfterGc;

  /**
   * @param threadName only events of this thread count
   * @param entries    methods as {@code SimpleClassName.method} whose callees are the phases
   */
  private JfrProfileSummary(@NotNull String threadName, @NotNull List<String> entries) {
    this.threadName = threadName;
    this.entries = entries;
    for (int i = 0; i < entries.size(); i++) {
      samplesByPhase.add(new Object2LongOpenHashMap<>());
      allocationByPhase.add(new Object2LongOpenHashMap<>());
    }
  }

  /**
   * @param title   heading of the summary
   * @param entries methods as {@code SimpleClassName.method} whose callees are the phases, one
   *                table each
   * @param top     rows of the method and allocation tables
   * @return the summary as markdown
   */
  @NotNull
  static String summarize(@NotNull Path recording, @NotNull String title,
      @NotNull String threadName, @NotNull List<String> entries, int top) throws IOException {
    final JfrProfileSummary summary = new JfrProfileSummary(threadName, entries);
    for (final RecordedEvent event : RecordingFile.readAllEvents(recording)) {
      summary.accept(event);
    }
    return summary.toMarkdown(title, top);
  }

  private void accept(@NotNull RecordedEvent event) {
    switch (event.getEventType().getName()) {
      case "jdk.ExecutionSample" -> {
        final RecordedStackTrace stack = event.getStackTrace();
        if (stack == null || !isBenchmarkThread(event.getThread("sampledThread"))) {
          return;
        }
        final List<RecordedFrame> frames = stack.getFrames();
        if (frames.isEmpty()) {
          return;
        }
        samples++;
        ownSamples.addTo(name(frames.getFirst()), 1);
        final Set<String> seen = new HashSet<>();
        for (final RecordedFrame frame : frames) {
          if (isMzmine(frame) && seen.add(name(frame))) {
            inclusiveSamples.addTo(name(frame), 1);
          }
        }
        for (int e = 0; e < entries.size(); e++) {
          samplesByPhase.get(e).addTo(phase(stack, entries.get(e)), 1);
        }
      }
      case "jdk.ObjectAllocationSample" -> {
        final RecordedStackTrace stack = event.getStackTrace();
        if (!isBenchmarkThread(event.getThread())) {
          return;
        }
        final long weight = event.getLong("weight");
        allocated += weight;
        allocationByClass.addTo(simpleName(event.getClass("objectClass").getName()), weight);
        allocationBySite.addTo(site(stack), weight);
        for (int e = 0; e < entries.size(); e++) {
          allocationByPhase.get(e).addTo(phase(stack, entries.get(e)), weight);
        }
      }
      case "jdk.GarbageCollection" -> {
        collections++;
        pauses = pauses.plus(event.getDuration("sumOfPauses"));
      }
      case "jdk.GCHeapSummary" -> {
        // a string or a struct with the string, depending on the reader. Object, the generic
        // getValue would pick String.valueOf(char[])
        final Object when = event.getValue("when");
        if (String.valueOf(when).contains("After GC")) {
          maxHeapAfterGc = Math.max(maxHeapAfterGc, event.getLong("heapUsed"));
        }
      }
      default -> {
      }
    }
  }

  private boolean isBenchmarkThread(@Nullable RecordedThread thread) {
    return thread != null && threadName.equals(thread.getJavaName());
  }

  /**
   * @return the callee of the innermost frame of the entry method, the entry itself for its own
   * samples
   */
  @NotNull
  private static String phase(@Nullable RecordedStackTrace stack, @NotNull String entry) {
    if (stack == null) {
      return "(no stack)";
    }
    final List<RecordedFrame> frames = stack.getFrames();
    for (int i = 0; i < frames.size(); i++) {
      if (entry.equals(name(frames.get(i)))) {
        return i == 0 ? entry + " (own)" : name(frames.get(i - 1));
      }
    }
    // decision: no "truncated" phase. JFR keeps the innermost frames and the test runner makes every
    // stack deeper than the stack depth, the frames of the benchmark are kept
    return "(outside " + entry + ")";
  }

  /**
   * @return the innermost mzmine method with its line, the allocating code of mzmine
   */
  @NotNull
  private static String site(@Nullable RecordedStackTrace stack) {
    if (stack == null) {
      return "(no stack)";
    }
    for (final RecordedFrame frame : stack.getFrames()) {
      if (isMzmine(frame)) {
        return name(frame) + ":" + frame.getLineNumber();
      }
    }
    return "(outside mzmine)";
  }

  private static boolean isMzmine(@NotNull RecordedFrame frame) {
    return frame.getMethod() != null && frame.getMethod().getType().getName()
        .startsWith(MZMINE_PACKAGE);
  }

  @NotNull
  private static String name(@NotNull RecordedFrame frame) {
    if (frame.getMethod() == null) {
      return "(unknown)";
    }
    return simpleName(frame.getMethod().getType().getName()) + "." + frame.getMethod().getName();
  }

  @NotNull
  private static String simpleName(@NotNull String className) {
    return className.substring(className.lastIndexOf('.') + 1);
  }

  @NotNull
  private String toMarkdown(@NotNull String title, int top) {
    final List<String> lines = new ArrayList<>();
    lines.add("#### " + title + "\n");
    lines.add("""
        %d execution samples, %.0f MB allocated (sum of the allocation sample weights), %d \
        garbage collections with %d ms pauses, max heap after GC %.0f MB
        """.formatted(samples, allocated / 1e6, collections, pauses.toMillis(),
        maxHeapAfterGc / 1e6));
    for (int e = 0; e < entries.size(); e++) {
      lines.add("Time and allocation by the callees of `%s`:\n".formatted(entries.get(e)));
      lines.add("| callee | samples | time share | allocated MB | allocation share |");
      lines.add("|---|---|---|---|---|");
      final Object2LongOpenHashMap<String> phaseSamples = samplesByPhase.get(e);
      final Object2LongOpenHashMap<String> phaseAllocation = allocationByPhase.get(e);
      final Set<String> phases = new HashSet<>(phaseSamples.keySet());
      phases.addAll(phaseAllocation.keySet());
      phases.stream().sorted((a, b) -> Long.compare(phaseSamples.getLong(b), phaseSamples.getLong(a)))
          .limit(top).forEach(phase -> lines.add(
              "| %s | %d | %.1f%% | %.1f | %.1f%% |".formatted(phase, phaseSamples.getLong(phase),
                  share(phaseSamples.getLong(phase), samples),
                  phaseAllocation.getLong(phase) / 1e6,
                  share(phaseAllocation.getLong(phase), allocated))));
      lines.add("");
    }
    table(lines, "Hot methods by own samples", "method", ownSamples, samples, top, false);
    table(lines, "mzmine methods by inclusive samples", "method", inclusiveSamples, samples, top,
        false);
    table(lines, "Allocation by class", "class", allocationByClass, allocated, top, true);
    table(lines, "Allocation by the innermost mzmine method", "method:line", allocationBySite,
        allocated, top, true);
    return String.join("\n", lines);
  }

  private static void table(@NotNull List<String> lines, @NotNull String title,
      @NotNull String key, @NotNull Object2LongOpenHashMap<String> values, long total, int top,
      boolean bytes) {
    lines.add(title + ":\n");
    lines.add("| %s | %s | share |".formatted(key, bytes ? "MB" : "samples"));
    lines.add("|---|---|---|");
    values.object2LongEntrySet().stream()
        .sorted((a, b) -> Long.compare(b.getLongValue(), a.getLongValue())).limit(top).forEach(
            entry -> lines.add("| %s | %s | %.1f%% |".formatted(entry.getKey(),
                bytes ? "%.1f".formatted(entry.getLongValue() / 1e6)
                    : Long.toString(entry.getLongValue()), share(entry.getLongValue(), total))));
    lines.add("");
  }

  private static double share(long value, long total) {
    return total == 0 ? 0 : 100d * value / total;
  }
}

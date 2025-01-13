/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.util.collections;

import it.unimi.dsi.fastutil.Pair;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StreamUtils {

  /**
   * Example how to stream all pairs of a list. If there are
   *
   * @param items      the list
   * @param isCanceled if task is cancelled this should be switched
   * @return a stream of all pairs
   */
  public static <T> Stream<Pair<T, T>> streamPairs(List<T> items,
      @Nullable BooleanSupplier isCanceled) {
    final int nItems = items.size();
    return IntStream.range(0, nItems - 1).boxed().<Pair<T, T>>mapMulti((i, consumer) -> {
      if (isCanceled != null && isCanceled.getAsBoolean()) {
        return;
      }
      for (int j = i + 1; j < nItems; j++) {
        consumer.accept(Pair.of(items.get(i), items.get(j)));
      }
    });
  }

  /**
   * Example how to stream all pairs of a list. If there are
   *
   * @param items       the list
   * @param isCanceled  if task is cancelled this should be switched
   * @param loopBreaker breaks the inner loop that generates pairs. the first element is early in
   *                    the list and the later element is a subsequent element.
   * @param <T>         type of list elements
   * @return a filtered stream of pairs based on a circuit breaker condition
   */
  public static <T> Stream<Pair<T, T>> streamPairs(List<T> items,
      @Nullable BooleanSupplier isCanceled, @NotNull PairLoopBreakCondition<T> loopBreaker) {
    final int nItems = items.size();
    return IntStream.range(0, nItems - 1).boxed().<Pair<T, T>>mapMulti((i, consumer) -> {
      if (isCanceled != null && isCanceled.getAsBoolean()) {
        return;
      }
      T a = items.get(i);
      for (int j = i + 1; j < nItems; j++) {
        var b = items.get(j);
        if (loopBreaker.isBreakLoop(a, b)) {
          break;
        }
        consumer.accept(Pair.of(a, b));
      }
    });
  }

  /**
   * Map all pairs in items to list of results
   *
   * @param items       the list
   * @param isCanceled  if task is cancelled this should be switched
   * @param loopBreaker breaks the inner loop that generates pairs. the first element is early in
   *                    the list and the later element is a subsequent element.
   * @param parallel    process in a parallel stream
   * @param processor   processor defines the function to map a pair to the result
   * @param <INPUT>     type of list elements
   * @param <RESULT>    the result type
   * @return a list of results for all pairs that met the optional loop breaker condition
   */
  public static <INPUT, RESULT> List<RESULT> mapPairs(List<INPUT> items,
      @Nullable BooleanSupplier isCanceled, boolean parallel,
      @Nullable PairLoopBreakCondition<INPUT> loopBreaker,
      @NotNull Function<Pair<INPUT, INPUT>, RESULT> processor) {

    var pairStream = loopBreaker == null ? streamPairs(items, isCanceled)
        : streamPairs(items, isCanceled, loopBreaker);
    if (parallel) {
      return pairStream.parallel().map(processor).toList();
    }
    return pairStream.map(processor).toList();
  }

  /**
   * Process all pairs in items. The processor could make use of a ConcurrentHashMap or similar to
   * keep track of the results - if parallel is true.
   *
   * @param items      the list
   * @param isCanceled if task is cancelled this should be switched
   * @param parallel   process in a parallel stream
   * @param processor  processor defines the function to map a pair to the result
   * @param <INPUT>    type of list elements
   * @return number of compared pairs that met the optional loop breaker condition
   */
  public static <INPUT> long processPairs(List<INPUT> items, @Nullable BooleanSupplier isCanceled,
      boolean parallel, @NotNull Consumer<Pair<INPUT, INPUT>> processor) {
    return processPairs(items, isCanceled, parallel, null, processor);
  }

  /**
   * Process all pairs in items. The processor could make use of a ConcurrentHashMap or similar to
   * keep track of the results - if parallel is true.
   *
   * @param items       the list
   * @param isCanceled  if task is cancelled this should be switched
   * @param loopBreaker breaks the inner loop that generates pairs. the first element is early in
   *                    the list and the later element is a subsequent element.
   * @param parallel    process in a parallel stream
   * @param processor   processor defines the function to map a pair to the result
   * @param <INPUT>     type of list elements
   * @return number of compared pairs that met the optional loop breaker condition
   */
  public static <INPUT> long processPairs(List<INPUT> items, @Nullable BooleanSupplier isCanceled,
      boolean parallel, @Nullable PairLoopBreakCondition<INPUT> loopBreaker,
      @NotNull Consumer<Pair<INPUT, INPUT>> processor) {

    var pairStream = loopBreaker == null ? streamPairs(items, isCanceled)
        : streamPairs(items, isCanceled, loopBreaker);
    if (parallel) {
      pairStream = pairStream.parallel();
    }
    return pairStream.mapToLong(pair -> {
      processor.accept(pair);
      return 1L;
    }).sum();
  }

  /**
   * Similar but different to: {@link Collectors#joining(CharSequence, CharSequence, CharSequence)}
   * This method will return empty String if stream was empty. The original returns the prefix +
   * suffix. This method can handle any object input and calls object.toString, whereas original
   * method requires strings.
   */
  public static Collector<Object, ?, String> joining(final CharSequence delimiter,
      final CharSequence prefix, final CharSequence suffix) {
    return new CollectorImpl<>(() -> {
      var joiner = new StringJoiner(delimiter, prefix, suffix);
      return joiner.setEmptyValue(""); // return empty if nothing added
    }, (stringJoiner, newElement) -> stringJoiner.add(Objects.toString(newElement)),
        StringJoiner::merge, StringJoiner::toString, Collections.emptySet());
  }

  record CollectorImpl<T, A, R>(Supplier<A> supplier, BiConsumer<A, T> accumulator,
                                BinaryOperator<A> combiner, Function<A, R> finisher,
                                Set<Characteristics> characteristics) implements
      Collector<T, A, R> {

    CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner,
        Set<Characteristics> characteristics) {
      this(supplier, accumulator, combiner, castingIdentity(), characteristics);
    }
  }

  /**
   * Casting types without check
   *
   * @param <I> input type
   * @param <R> result type
   */
  @SuppressWarnings("unchecked")
  public static <I, R> Function<I, R> castingIdentity() {
    return i -> (R) i;
  }
}

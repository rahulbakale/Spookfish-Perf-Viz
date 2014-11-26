/**
 * Copyright 2014 Rahul Bakale
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package spookfishperfviz;

import java.util.Objects;

/**
 * @author Rahul Bakale
 * @since Nov, 2014
 */
final class Optional<T> {

	private static final Optional<?> EMPTY = new Optional<>();

	private static <T> Optional<T> empty() {
		@SuppressWarnings("unchecked")
		final Optional<T> o = (Optional<T>) EMPTY;
		return o;
	}

	static <T> Optional<T> of(final T value) {
		return value == null ? Optional.<T> empty() : new Optional<>(value);
	}

	private final T value;

	private Optional() {
		this.value = null;
	}

	private Optional(final T value) {
		this.value = Objects.requireNonNull(value);
	}

	boolean hasValue() {
		return this.value != null;
	}

	T get() {
		if (hasValue()) {
			return this.value;
		}

		throw new UnsupportedOperationException("Value is absent");
	}
}

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

/**
 * @author Rahul Bakale
 * @since Nov, 2014
 */
final class DataPoint<C extends Comparable<C>> implements Comparable<DataPoint<C>> {

	private static enum Type {

		NEGATIVE_INFINITE(-1, "<"), FINITE(0, ""), POSITIVE_INFINITE(1, ">");

		final int val;
		final String symbol;

		private Type(final int val, final String symbol) {
			this.val = val;
			this.symbol = symbol;
		}
	}

	static <C extends Comparable<C>> DataPoint<C> createFinite(final C actualData) {
		return new DataPoint<>(actualData, Type.FINITE);
	}

	static <C extends Comparable<C>> DataPoint<C> createPositiveInfinite() {
		return new DataPoint<>(null, Type.POSITIVE_INFINITE);
	}

	static <C extends Comparable<C>> DataPoint<C> createNegativeInfinite() {
		return new DataPoint<>(null, Type.NEGATIVE_INFINITE);
	}

	private final C actualData;
	private final DataPoint.Type type;

	private DataPoint(final C actualData, final DataPoint.Type type) {
		this.actualData = actualData;
		this.type = type;
	}

	@Override
	public int compareTo(final DataPoint<C> other) {
		return ((this.type == Type.FINITE) && (other.type == Type.FINITE)) ? this.actualData.compareTo(other.actualData) : this.type.val
				- other.type.val;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((this.actualData == null) ? 0 : this.actualData.hashCode());
		result = (prime * result) + ((this.type == null) ? 0 : this.type.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final DataPoint<?> other = (DataPoint<?>) obj;
		if (this.actualData == null) {
			if (other.actualData != null) {
				return false;
			}
		} else if (!this.actualData.equals(other.actualData)) {
			return false;
		}
		if (this.type != other.type) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return toString(null);
	}

	String toString(final Function<C, String> formatter) {
		final String ret;

		if (this.type == Type.FINITE) {
			if (formatter == null) {
				ret = String.valueOf(this.actualData);
			} else {
				ret = formatter.apply(this.actualData);
			}
		} else {
			ret = this.type.symbol;
		}

		return ret;
	}
}
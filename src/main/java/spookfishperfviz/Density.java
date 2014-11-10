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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Rahul Bakale
 * @since Nov, 2014
 */
final class Density<R extends Comparable<R>, C extends Comparable<C>, V> {
	
	static final class IndexedDataPoint<C extends Comparable<C>> implements Comparable<IndexedDataPoint<C>> {
		
		static <C extends Comparable<C>> IndexedDataPoint<C> createFinite(final C actualData) {
			return new IndexedDataPoint<>(DataPoint.createFinite(actualData));
		}

		static <C extends Comparable<C>> IndexedDataPoint<C> createPositiveInfinite() {
			return new IndexedDataPoint<>(DataPoint.<C> createPositiveInfinite());
		}

		static <C extends Comparable<C>> IndexedDataPoint<C> createNegativeInfinite() {
			return new IndexedDataPoint<>(DataPoint.<C> createNegativeInfinite());
		}

		private final DataPoint<C> dataPoint;
		private int index;

		private IndexedDataPoint(final DataPoint<C> dataPoint) {
			this.dataPoint = dataPoint;
		}

		int getIndex() {
			return this.index;
		}

		void setIndex(final int index) {
			this.index = index;
		}

		@Override
		public int compareTo(final IndexedDataPoint<C> o) {
			return this.dataPoint.compareTo(o.dataPoint);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = (prime * result) + ((this.dataPoint == null) ? 0 : this.dataPoint.hashCode());
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
			final IndexedDataPoint<?> other = (IndexedDataPoint<?>) obj;
			if (this.dataPoint == null) {
				if (other.dataPoint != null) {
					return false;
				}
			} else if (!this.dataPoint.equals(other.dataPoint)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return toString(null);
		}

		String toString(final Function<C, String> formatter) {
			return this.dataPoint.toString(formatter);
		}
	}

	private static <T extends Comparable<T>> NavigableSet<IndexedDataPoint<T>> sortAndIndex(final Set<T> intervalPoints) {
		final NavigableSet<IndexedDataPoint<T>> points = new TreeSet<>();

		for (final T iPoint : intervalPoints) {
			points.add(IndexedDataPoint.createFinite(iPoint));
		}

		points.add(IndexedDataPoint.<T> createNegativeInfinite());
		points.add(IndexedDataPoint.<T> createPositiveInfinite());

		int index = 0;
		for (final IndexedDataPoint<T> p : points) {
			p.setIndex(index++);
		}

		return points;
	}

	private static <T extends Comparable<T>> int calculateIndex(final T t, final NavigableSet<IndexedDataPoint<T>> set) {
		return set.lower(IndexedDataPoint.createFinite(t)).getIndex();
	}

	static <R extends Comparable<R>, C extends Comparable<C>, V> Density<R, C, V> create(final Set<R> rowIntervalPoints,
			final Set<C> columnIntervalPoints, final V nullValue, final Class<V> valueType) {
		return new Density<>(rowIntervalPoints, columnIntervalPoints, nullValue, valueType);
	}

	private final NavigableSet<IndexedDataPoint<R>> rowIntervalPoints;
	private final NavigableSet<IndexedDataPoint<C>> columnIntervalPoints;

	private final V[][] matrix;

	private Density(final Set<R> rowIntervalPoints, final Set<C> columnIntervalPoints, final V nullValue, final Class<V> valueType) {
		
		final NavigableSet<IndexedDataPoint<C>> cip = sortAndIndex(columnIntervalPoints);
		final NavigableSet<IndexedDataPoint<R>> rip = sortAndIndex(rowIntervalPoints);

		final int rowCount = rip.size() - 1;
		final int colCount = cip.size() - 1;

		final V[][] matrix = (V[][]) Array.newInstance(valueType, rowCount, colCount);

		for (int r = 0; r < rowCount; r++) {
			for (int c = 0; c < colCount; c++) {
				matrix[r][c] = nullValue;
			}
		}

		this.columnIntervalPoints = cip;
		this.rowIntervalPoints = rip;

		this.matrix = matrix;
	}

	void apply(final R row, final C column, final UnaryOperator<V> operator) {
		final int rowNum = calculateIndex(row, this.rowIntervalPoints);
		final int columnNum = calculateIndex(column, this.columnIntervalPoints);

		this.matrix[rowNum][columnNum] = operator.apply(this.matrix[rowNum][columnNum]);
	}

	@Override
	public String toString() {
		final String NL = System.lineSeparator();

		String str = "Points on X axis=" + this.columnIntervalPoints + NL + "Points on Y axis=" + this.rowIntervalPoints + NL;

		for (final V[] row : this.matrix) {
			str += Arrays.deepToString(row) + NL;
		}
		return str;
	}

	V[][] getMatrix() {
		return this.matrix;
	}

	NavigableSet<IndexedDataPoint<R>> getRowIntervalPoints() {
		return this.rowIntervalPoints;
	}

	NavigableSet<IndexedDataPoint<C>> getColumnIntervalPoints() {
		return this.columnIntervalPoints;
	}

}

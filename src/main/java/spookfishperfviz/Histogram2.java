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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author Rahul Bakale
 * @since Nov, 2014
 * 
 * @param <C> data type
 */
final class Histogram2<C extends Comparable<C>> extends Histogram<C> {
	
	private final SortedMap<Interval<C>, Integer> histogram;

	static Histogram2<Double> newInstance(final double[] data, final double[] intervalPoints, final boolean ignoreEmptyIntervals) {
		return newInstance(Utils.asList(data), intervalPoints, ignoreEmptyIntervals);
	}

	static Histogram2<Double> newInstance(final double[] data, final int nIntervalPoints, final boolean ignoreEmptyIntervals) {
		return newInstance(Utils.asList(data), nIntervalPoints, ignoreEmptyIntervals);
	}

	static Histogram2<Double> newInstance(final Collection<Double> data, final int nIntervalPoints, final boolean ignoreEmptyIntervals) {
		return newInstance(data, Utils.createIntervalPoints(data, nIntervalPoints), ignoreEmptyIntervals);
	}

	static Histogram2<Double> newInstance(final Collection<Double> data, final double[] intervalPoints, final boolean ignoreEmptyIntervals) {
		return new Histogram2<>(data, Utils.toHashSet(intervalPoints), ignoreEmptyIntervals);
	}

	static <T extends Comparable<T>> Histogram2<T> newInstance(final Collection<T> data, final Set<T> intervalPoints,
			final boolean ignoreEmptyIntervals) {
		return new Histogram2<>(data, intervalPoints, ignoreEmptyIntervals);
	}

	private Histogram2(final Collection<C> data, final Set<C> intervalPoints, final boolean ignoreEmptyIntervals) {
		
		final Set<Interval<C>> intervals = new HashSet<>();
		boolean loop = true;
		
		final Iterator<C> itr = new TreeSet<>(intervalPoints).iterator();
		
		DataPoint<C> low = DataPoint.createNegativeInfinite();
		
		do {
			DataPoint<C> high;

			if (itr.hasNext()) {
				high = DataPoint.createFinite(itr.next());
			} else {
				high = DataPoint.<C> createPositiveInfinite();
				loop = false;
			}

			intervals.add(new Interval<>(low, high));

			low = high;
			
		} while (loop);

		final SortedMap<Interval<C>, Integer> hist = new TreeMap<>();

		if (ignoreEmptyIntervals == false) {
			final Integer ZERO = Integer.valueOf(0);
			for (final Interval<C> interval : intervals) {
				hist.put(interval, ZERO);
			}
		}

		for (final C datum : data) {
			for (final Interval<C> interval : intervals) {
				if (interval.contains(DataPoint.createFinite(datum))) {
					hist.put(interval, hist.containsKey(interval) ? Integer.valueOf(hist.get(interval).intValue() + 1) : Integer.valueOf(1));
					break;
				}
			}
		}

		this.histogram = hist;
	}

	@Override
	public String toString() {
		return toBarChart(null).toString();
	}

	@Override
	String toString(final Function<C, String> dataPointFormatter, final int maxHeight, final String mark) {
		return toBarChart(dataPointFormatter).toString(maxHeight, mark);
	}

	@Override
	String toSVG(final Function<C, String> dataPointFormatter, final boolean wrapInHtmlBody) {
		return toBarChart(dataPointFormatter).toSVG(wrapInHtmlBody, true);
	}

	private HorizontalBarChart toBarChart(final Function<C, String> dataPointFormatter) {
		final int size = this.histogram.size();
		final Entry<Interval<C>, Integer>[] entries = this.histogram.entrySet().toArray(new Entry[size]);

		final LabelMaker<C> labelMaker = new LabelMaker<>(entries, dataPointFormatter);
		final int[] data = new int[size];
		final String[] labels = new String[size];

		for (int k = 0; k < size; k++) {
			data[k] = entries[k].getValue().intValue();
			labels[k] = labelMaker.getDataLabel(k);
		}
		
		final String headerLabel = labelMaker.getHeaderLabel();

		return HorizontalBarChart.create(data, labels, headerLabel);
	}

	private static final class Interval<C extends Comparable<C>> implements Comparable<Interval<C>> {
		private final DataPoint<C> low;
		private final DataPoint<C> high;

		Interval(final DataPoint<C> low, final DataPoint<C> high) {
			
			Objects.requireNonNull(low);
			Objects.requireNonNull(high);

			if (low.compareTo(high) >= 0) {
				throw new IllegalArgumentException("Low = <" + low + ">. High = <" + high + ">.");
			}

			this.low = low;
			this.high = high;
		}

		boolean contains(final DataPoint<C> val) {
			Objects.requireNonNull(val);

			return (val.compareTo(this.low) >= 0) && (val.compareTo(this.high) < 0);
		}

		@Override
		public int compareTo(final Interval<C> other) {
			if (this.equals(other)) {
				return 0;
			}

			if (this.high.compareTo(other.low) <= 0) {
				return -1;
			}

			if (this.low.compareTo(other.high) >= 0) {
				return 1;
			}

			throw new RuntimeException("The following intervals cannot be compared: <" + this + ">, <" + other + ">");
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = (prime * result) + ((this.high == null) ? 0 : this.high.hashCode());
			result = (prime * result) + ((this.low == null) ? 0 : this.low.hashCode());
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
			final Interval<?> other = (Interval<?>) obj;
			if (this.high == null) {
				if (other.high != null) {
					return false;
				}
			} else if (!this.high.equals(other.high)) {
				return false;
			}
			if (this.low == null) {
				if (other.low != null) {
					return false;
				}
			} else if (!this.low.equals(other.low)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return toString(null);
		}

		String toString(final Function<C, String> dataPointFormatter) {
			return "[" + this.low.toString(dataPointFormatter) + ',' + this.high.toString(dataPointFormatter) + "]";
		}
	}

	private static final class LabelMaker<C extends Comparable<C>> {
		
		private static final String INTERVAL_HEADER = "Interval";
		private static final String FREQUENCY_HEADER = "Count";
		private static final String PERCENTAGE_HEADER = "%";
		private static final String CUMULATIVE_PERCENTAGE_HEADER = "Sum of %";
		
		private final Entry<Interval<C>, Integer>[] table;
		private final int intrvlpadding;
		private final int freqPadding;
		private final double[] percs;
		private final double[] cumulatives;
		private final Function<C, String> dataPointFormatter;
		private final String labelStringFormat;
		private final String headerLabel;

		LabelMaker(final Entry<Interval<C>, Integer>[] entries, final Function<C, String> dataPointFormatter) {
			
			long sumOfFrequencies = 0;
			int iPadding = -1, fPadding = -1;

			for (final Entry<Interval<C>, Integer> row : entries) {
				final Interval<C> interval = row.getKey();
				final Integer frequency = row.getValue();

				sumOfFrequencies += frequency.intValue();
				iPadding = Math.max(iPadding, interval.toString(dataPointFormatter).length());
				fPadding = Math.max(fPadding, String.valueOf(frequency).length());
			}

			iPadding = Math.max(iPadding, INTERVAL_HEADER.length());
			fPadding = Math.max(fPadding, FREQUENCY_HEADER.length());
			
			final int pPadding = Math.max(7 /*xxx.xx%*/, PERCENTAGE_HEADER.length());
			final int cPadding = Math.max(7 /*xxx.xx%*/, CUMULATIVE_PERCENTAGE_HEADER.length());

			final int size = entries.length;

			final double[] p = new double[size];
			final double[] c = new double[size];

			double cumulative = 0;

			for (int i = 0; i < size; i++) {
				final int frequency = entries[i].getValue().intValue();

				final double perc = (frequency * 100.0) / sumOfFrequencies;
				cumulative += perc;

				p[i] = perc;
				c[i] = cumulative;
			}
			
			

			this.table = entries;
			this.percs = p;
			this.cumulatives = c;
			this.intrvlpadding = iPadding;
			this.freqPadding = fPadding;
			this.dataPointFormatter = dataPointFormatter;
			
			this.labelStringFormat = 
					"%1$" + this.intrvlpadding + "s" +  "   " + 
					"%2$" + this.freqPadding + "s" + "   " + 
					"%3$" + pPadding + "s" + "   " +
					"%4$" + cPadding + "s";
			
			this.headerLabel = String.format(this.labelStringFormat, 
												INTERVAL_HEADER, 
												FREQUENCY_HEADER, 
												PERCENTAGE_HEADER, 
												CUMULATIVE_PERCENTAGE_HEADER);
		}

		String getDataLabel(final int index) {
			
			final Entry<Interval<C>, Integer> row = this.table[index];
			final Interval<C> interval = row.getKey();
			final Integer frequency = row.getValue();

			final double perc = this.percs[index];
			final double cumulative = this.cumulatives[index];

			return String.format(this.labelStringFormat, 
					interval.toString(this.dataPointFormatter), 
					frequency, 
					Utils.toDisplayString(perc, 2, false) + '%', 
					Utils.toDisplayString(cumulative, 2, false) + '%');
		}

		String getHeaderLabel() {
			return this.headerLabel;
		}
	}
}

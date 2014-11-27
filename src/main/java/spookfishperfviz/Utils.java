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

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Rahul Bakale
 * @since Nov, 2014
 */
final class Utils {
	
	private Utils() {
		//
	}

	static List<String> lines(final String multiLineText) {
		final List<String> lines = new ArrayList<>();

		try (final LineScanner scanner = new LineScanner(multiLineText);) {
			for (final String line : scanner) {
				lines.add(line);
			}
		}

		return lines;
	}

	static String toSvgText(final String text) {
		final String NL = System.lineSeparator();
		final int lineSpace = SVGConstants.LINE_GAP;

		final StringBuilder texts = new StringBuilder();

		int maxLineLength = Integer.MIN_VALUE;
		int height = lineSpace;

		try (final Scanner s = new Scanner(text);) {

			s.useDelimiter("\r\n|[\n\r\u2028\u2029\u0085]");

			while (s.hasNext()) {

				final String rawLine = s.next();

				final int rawLineLength = rawLine.length();
				if (rawLineLength > maxLineLength) {
					maxLineLength = rawLineLength;
				}

				final String line = rawLine.replace(" ", "&nbsp;");

				texts.append("<text x=\"").append(10).append("\" y=\"").append(height).append("\">").append(line).append("</text>").append(NL);

				height += lineSpace;
			}
		}

		final double width = (SVGConstants.LEFT_RIGHT_MARGIN * 2) + (maxLineLength * SVGConstants.FONT_WIDTH);

		final StringBuilder buf = new StringBuilder();
		buf.append("<svg height=\"").append(height).append("\" width=\"").append(width).append("\">").append(NL);
		buf.append("<rect height=\"").append(height).append("\" width=\"").append(width).append("\" style=\"fill:white;stroke:black;stroke-width:1\"/>").append(NL);
		buf.append("<g fill=\"black\" style=\"font-family:").append(SVGConstants.FONT_FAMILY).append(";font-size:").append(SVGConstants.FONT_SIZE).append("px;\">").append(NL);
		buf.append(texts);
		buf.append("</g>").append(NL);
		buf.append("</svg>");

		return buf.toString();
	}

	static <T, R, C extends Collection<R>> C forEach(final Collection<T> c, final Function<? super T, R> f, final Supplier<C> s) {
		final C ret = s.get();

		for (final T e : c) {
			ret.add(f.apply(e));
		}

		return ret;
	}

	static <T, C extends List<T>> C reverse(final Collection<T> c, final Supplier<C> s) {
		final C ret = s.get();
		ret.addAll(c);
		Collections.reverse(ret);

		return ret;
	}

	// TODO - use this in BarChart
	static <C extends Collection<String>> C getPaddedLabels(final Collection<? extends CharSequence> labels, final Supplier<C> s, final boolean escapeHTMLSpecialChars) {
		
		final int maxLabelLength = Collections.max(forEach(labels, CharSeqLengthFunction.INSTANCE, new ArrayListSupplier<Integer>())).intValue();
		return getPaddedLabels(labels, maxLabelLength, s, escapeHTMLSpecialChars);
	}

	static <C extends Collection<String>> C getPaddedLabels(final Collection<? extends CharSequence> labels, final int maxLabelLength, final Supplier<C> s, final boolean escapeHTMLSpecialChars) {
		
		final Function<CharSequence, String> paddingFunc = new Function<CharSequence, String>() {
			@Override
			public String apply(final CharSequence cs) {
				return getPaddedLabel(cs, maxLabelLength, escapeHTMLSpecialChars);
			}
		};

		return forEach(labels, paddingFunc, s);
	}

	static String getPaddedLabel(final CharSequence label, final int maxLabelLength, final boolean escapeHTMLSpecialChars) {
		
		final String paddedLabel = String.format("%1$" + maxLabelLength + "s", label);
		return escapeHTMLSpecialChars ? escapeHTMLSpecialChars(paddedLabel) : paddedLabel;
	}

	static String escapeHTMLSpecialChars(final String str) {
		return str.replace("&", "&amp;").replace(" ", "&nbsp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	static String stripTrailingZeroesAfterDecimal(final double d, final boolean useGrouping) {
		
		final DecimalFormat df = new DecimalFormat();
		df.setMinimumFractionDigits(0);
		df.setMaximumFractionDigits(Integer.MAX_VALUE);
		df.setGroupingUsed(useGrouping);
		return df.format(d);
	}

	static String stripTrailingZeroesAfterDecimal(final Double d, final boolean useGrouping) {
		return stripTrailingZeroesAfterDecimal(d.doubleValue(), useGrouping);
	}

	/**
	 * TODO - check if this is the right place for this method
	 */
	static Set<Long> getTimestampIntervalPoints(final long[] timestamps, final long timeIntervalInMillis) {
		
		if (timeIntervalInMillis <= 0) {
			throw new IllegalArgumentException("Invalid time interval: <" + timeIntervalInMillis + ">. Time interval must be a positive value");
		}

		long minTime = Long.MAX_VALUE;
		long maxTime = Long.MIN_VALUE;

		for (final long timestamp : timestamps) {
			if (timestamp < minTime) {
				minTime = timestamp;
			}

			if (timestamp > maxTime) {
				maxTime = timestamp;
			}
		}

		final long flooredMinTime = getStartOfHour(minTime);

		final Set<Long> timestampIntervalPoints = new HashSet<>();
		for (long point = flooredMinTime, interval = timeIntervalInMillis; point <= (maxTime + interval); point += interval) {
			timestampIntervalPoints.add(Long.valueOf(point));
		}

		return timestampIntervalPoints;
	}

	static Set<Double> toHashSet(final double[] doubles) {
		final Set<Double> set = new HashSet<>(doubles.length);
		for (final double d : doubles) {
			set.add(Double.valueOf(d));
		}
		return set;
	}

	static Long getStartOfDay(final long timestamp) {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(timestamp);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		return Long.valueOf(calendar.getTimeInMillis());
	}

	static long getStartOfHour(final long timestamp) {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(timestamp);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTimeInMillis();
	}

	static double[] primArr(final Collection<Double> x) {
		final double[] data = new double[x.size()];

		int i = 0;
		for (final double d : x) {
			data[i++] = d;
		}
		return data;
	}

	static double sum(final double[] data) {
		double sum = 0;
		for (final double d : data) {
			sum += d;
		}
		return sum;
	}

	static double[] minMax(final double[] data) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;

		for (final double d : data) {
			if (d > max) {
				max = d;
			}

			if (d < min) {
				min = d;
			}
		}

		return new double[] { min, max };
	}

	static double[] minMax(final Collection<Double> data) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;

		for (final double d : data) {
			if (d > max) {
				max = d;
			}

			if (d < min) {
				min = d;
			}
		}

		return new double[] { min, max };
	}

	static double getMedian(final double[] sortedData) {
		final int n = sortedData.length;

		final double median;
		if ((n % 2) == 0) {
			final int k = (n / 2);
			median = (sortedData[k - 1] + sortedData[k]) / 2;
		} else {
			final int k = (n + 1) / 2;
			median = sortedData[k - 1];
		}
		return median;
	}

	static double[] sort(final double[] data) {
		final double[] copy = Arrays.copyOf(data, data.length);
		Arrays.sort(copy);
		return copy;
	}

	static long[] sort(final long[] data) {
		final long[] copy = Arrays.copyOf(data, data.length);
		Arrays.sort(copy);
		return copy;
	}

	static Percentiles getPercentiles(final double[] sortedData, final double[] keys) {
		
		final double[] sortedKeys = Utils.sort(keys);

		final int n = sortedKeys.length;

		final double[] result = new double[n];
		final double[] validKeys = new double[n];
		int k = 0;

		for (int i = 0; i < n; i++) {
			final double key = sortedKeys[i];

			try {
				result[k] = Utils.getPthPercentile(sortedData, key);
			} catch (final IllegalPercentileKeyException e) {
				continue; // ignore this key and proceed to other keys
			}

			validKeys[k] = key;
			k++;
		}

		return new Percentiles(Arrays.copyOfRange(validKeys, 0, k), Arrays.copyOfRange(result, 0, k));
	}

	/**
	 * Slightly modified form of what is described here ->
	 * http://www.stanford.edu/class/archive/anthsci/anthsci192/anthsci192.1064/handouts/calculating%20percentiles.pdf
	 * 
	 * @throws IllegalArgumentException
	 *             if percentile can not be calculated for <code>p</code>
	 */
	static double getPthPercentile(final double[] sortedData, final double p) {
		final int n = sortedData.length;

		final double pos = (n * (p / 100)) + 0.5; // TODO - check if this is the correct way
		final double integerPart = Math.floor(pos);
		final int index = ((int) integerPart) - 1; // array index begins at 0

		if (index < 0) {
			throw new IllegalPercentileKeyException(n, p);
		}

		final double fraction = pos - integerPart;

		final double x = sortedData[index];

		final double result;

		if ((fraction == 0) || (index == (n - 1))) {
			result = x;
		} else {
			// interpolate

			final double y = sortedData[index + 1];
			final double diff = y - x;

			result = x + (fraction * diff);
		}

		return result;
	}

	private static final class IllegalPercentileKeyException extends RuntimeException {
		
		private static final long serialVersionUID = -2793561757886762344L;

		IllegalPercentileKeyException(final int n, final double p) {
			super("n=" + n + ", p=" + p);
		}
	}

	/**
	 * TODO implement this. 
	 * See http://www.stanford.edu/class/archive/anthsci/anthsci192/anthsci192.1064/handouts/calculating%20percentiles.pdf
	 */
	private static double getPercentileOfValue() {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	static double[] zScores(final double[] data, final double mean, final double stdDeviation) {
		final int n = data.length;

		final double[] zscores = new double[n];
		for (int i = 0; i < n; i++) {
			zscores[i] = (data[i] - mean) / stdDeviation;
		}
		return zscores;
	}

	static double[] getValuesForIndices(final int[] indices, final double[] data) {
		final int n = indices.length;
		final double[] result = new double[n];

		for (int i = 0; i < n; i++) {
			final int index = indices[i];
			result[i] = data[index];
		}
		return result;
	}

	static int[] getIndicesOfValuesGreaterThan(final double threshold, final double[] values) {
		final int n = values.length;
		final int[] indices = new int[n];

		int c = 0;
		for (int i = 0; i < n; i++) {
			if (values[i] > threshold) {
				indices[c++] = i;
			}
		}

		return Arrays.copyOf(indices, c);
	}

	static double[] toDoubles(final int[] ints) {
		final int len = ints.length;
		final double[] doubles = new double[len];

		for (int i = 0; i < len; i++) {
			doubles[i] = ints[i];
		}
		return doubles;
	}

	static double[] toDoubles(final long[] longs) {
		final int len = longs.length;
		final double[] doubles = new double[len];

		for (int i = 0; i < len; i++) {
			doubles[i] = longs[i];
		}
		return doubles;
	}

	static MultiSpanSVGText createMultiSpanSVGText(final String multiLineText, final double x, final double y, final double fontSize, final String color) {
		
		final List<String> lines = Utils.lines(multiLineText);
		final int spanCount = lines.size();

		final String NL = System.lineSeparator();

		final StringBuilder label = new StringBuilder();
		label.append("<text text-anchor=\"middle\"");
		if (color != null) {
			label.append(" fill=\"").append(color).append("\"");
		}
		label.append(">");
		label.append(NL);

		for (int i = 0; i < spanCount; i++) {
			final String line = lines.get(i);
			label.append("<tspan x=\"").append(x).append("\" y=\"").append(y + (i * fontSize)).append("\">").append(line).append("</tspan>").append(NL);
		}

		label.append("</text>").append(NL);

		return new MultiSpanSVGText(label, spanCount);
	}

	static boolean skipLabel(final int currentLabelIndex, final int totalLabelCount, final int labelSkipCount) {
		
		final boolean dontSkipLabel = 
				(currentLabelIndex == 0) || (currentLabelIndex == totalLabelCount) || (((currentLabelIndex - 1) % (labelSkipCount + 1)) == 0);
		
		return !dontSkipLabel;
	}

	static String wrapInHTMLBody(final String svgString) {
		
		final String NL = System.lineSeparator();
		return "<!DOCTYPE html>" + NL + "<html>" + NL + "  <body>" + NL + svgString + NL + "  </body>" + NL + "</html>";
	}

	static void createFile(final String content, final String filePath) throws IOException {
		try (final FileWriter fw = new FileWriter(filePath); final BufferedWriter bw = new BufferedWriter(fw);) {
			bw.write(content);
		}
	}

	static List<Double> asList(final double[] data) {

		final List<Double> l = new ArrayList<>(data.length);
		for (final double d : data) {
			l.add(Double.valueOf(d));
		}
		return l;
	}

	static double[] createIntervalPoints(final Collection<Double> data, final int nIntervalPoints) {
		final double[] minMax = minMax(data);
		final double min = minMax[0];
		final double max = minMax[1];

		return createIntervalPoints(min, max, nIntervalPoints);
	}

	static double[] createIntervalPoints(final double[] data, final int nIntervalPoints) {
		final double[] minMax = minMax(data);
		final double min = minMax[0];
		final double max = minMax[1];

		return createIntervalPoints(min, max, nIntervalPoints);
	}

	static double[] getAdjustedMinMax(final double min, final double max) {

		// TODO - also adjust for values less than one
		final double newMin = min < 1 ? min : Math.floor(min);
		// TODO - also adjust for values less than one
		final double newMax = max < 1 ? max : Math.ceil(max);

		if (newMin > newMax) {
			throw new IllegalArgumentException("min = <" + newMin + ">, max = <" + newMax + ">");
		}

		return new double[] { newMin, newMax };
	}

	/**
	 * @deprecated
	 * @see #createIntervalPoints2(double, double, int)
	 * TODO - delete later
	 */
	@Deprecated
	private static double[] createIntervalPointsOld(final double min, final double max, final int nIntervalPoints) {

		if (min < 0) {
			throw new IllegalArgumentException("min = <" + min + ">");
		}

		if (max < 0) {
			throw new IllegalArgumentException("max = <" + max + ">");
		}

		if (min > max) {
			throw new IllegalArgumentException("min = <" + min + ">, max = <" + max + ">");
		}

		if (nIntervalPoints < 1) {
			throw new IllegalArgumentException("Too few interval points <" + nIntervalPoints + ">");
		}

		/**
		 * Cases to handle
		 * 
		 * Both min and max > 1 newMin == newMax (e.g. min = 3, max = 3) newMin
		 * != newMax (e.g. min = 1.3, max = 1.3) newMin != newMax (e.g. min =
		 * 1.3, max = 1.7)
		 * 
		 * Both 0 <= (min and max) < 1 newMin != newMax (e.g. min = 0.3, max =
		 * 0.5) newMin == newMax (e.g. min = 0, max = 0)
		 * 
		 * 0 <= min < 1 and max >= 1 newMin != newMax (e.g. min = 0, max = 1.3)
		 */

		final double[] adjustedMinMax = getAdjustedMinMax(min, max);
		final double adjustedMin = adjustedMinMax[0];
		final double adjustedMax = adjustedMinMax[1];

		final int nIntervals = nIntervalPoints - 1;
		final double intervalLength = (adjustedMax - adjustedMin) / nIntervals;
		final double adjustedIntervalLength = (intervalLength > 1) ? Math.ceil(intervalLength) : intervalLength;

		// TODO - check if casting to integer is safe
		final int adjustedNIntervals = (int) Math.ceil((adjustedMax - adjustedMin) / adjustedIntervalLength);
		final int adjustedNIntervalPoints = adjustedNIntervals + 1;

		final double[] intervalPoints = new double[adjustedNIntervalPoints];

		intervalPoints[0] = adjustedMin;

		for (int i = 1; i < adjustedNIntervalPoints; i++) {
			intervalPoints[i] = intervalPoints[i - 1] + adjustedIntervalLength;
		}

		return intervalPoints;
	}

	/**
	 * TODO - test with various data samples TODO - verify mathematical
	 * precision
	 * 
	 * @see http://stackoverflow.com/questions/326679/choosing-an-attractive-linear-scale-for-a-graphs-y-axis
	 */
	static double[] createIntervalPoints(final double min, final double max, final int nIntervalPoints) {

		if (min < 0) {
			throw new IllegalArgumentException("min = <" + min + ">");
		}

		if (max < 0) {
			throw new IllegalArgumentException("max = <" + max + ">");
		}

		if (min > max) {
			throw new IllegalArgumentException("min = <" + min + ">, max = <" + max + ">");
		}

		if (nIntervalPoints < 1) {
			throw new IllegalArgumentException("Too few interval points <" + nIntervalPoints + ">");
		}

		final BigDecimal minBD = BigDecimal.valueOf(min);
		final BigDecimal maxBD;

		if (min == max) {
			if ((min > 0) && (min < 1)) {
				final int scale = minBD.scale();
				final BigDecimal pow = TEN.pow(scale);
				maxBD = minBD.multiply(pow).add(ONE).divide(pow);
			} else {
				maxBD = minBD.add(ONE);
			}
		} else {
			maxBD = BigDecimal.valueOf(max);
		}

		final BigDecimal range = maxBD.subtract(minBD);
		final BigDecimal interval = range.divide(BigDecimal.valueOf(nIntervalPoints), MathContext.DECIMAL128);

		// TODO - check if casting to int is OK.
		final int x = (int) Math.floor(Math.log10(interval.doubleValue()) + 1);

		final BigDecimal tenPowerX = x < 0 ? ONE.divide(TEN.pow(x * -1)) : TEN.pow(x);

		final double y = interval.divide(tenPowerX).doubleValue();

		// System.out.println("min=" + minBD + ", max=" + maxBD + ", range=" +
		// range + ", interval=" + interval + ", x=" + x + ", y=" + y +
		// ", tenPowerX=" + tenPowerX);

		if ((y < 0.1) || (y > 1.0)) {
			throw new RuntimeException("Internal error: " + y);
		}

		final double z;

		if (y == 0.1) {
			z = 0.1;
		} else if (y <= 0.2) {
			z = 0.2;
		} else if (y <= 0.25) {
			z = 0.25;
		} else if (y <= 0.3) {
			z = 0.3;
		} else if (y <= 0.4) {
			z = 0.4;
		} else if (y <= 0.5) {
			z = 0.5;
		} else if (y <= 0.6) {
			z = 0.6;
		} else if (y <= 0.7) {
			z = 0.7;
		} else if (y <= 0.75) {
			z = 0.75;
		} else if (y <= 0.8) {
			z = 0.8;
		} else if (y <= 0.9) {
			z = 0.9;
		} else {
			z = 1;
		}

		// Necessary to use BigDecimal to keep precision
		final BigDecimal niceInterval = BigDecimal.valueOf(z).multiply(tenPowerX);

		final double[] intervalPoints = new double[nIntervalPoints];

		/*
		 * final double niceMin = niceInterval * Math.floor((min / niceInterval)); intervalPoints[0] = niceMin;
		 * 
		 * for (int i = 1; i < nIntervalPoints; i++) { intervalPoints[i] = intervalPoints[i - 1] + niceInterval; }
		 */

		final BigDecimal niceMax = niceInterval.multiply(maxBD.divide(niceInterval, 0, RoundingMode.CEILING)); // niceInterval * Math.ceil((max / niceInterval));

		// System.out.println("niceInterval=" + niceInterval + ", niceMax=" + niceMax);

		intervalPoints[intervalPoints.length - 1] = niceMax.doubleValue();
		BigDecimal prev = niceMax;

		for (int i = intervalPoints.length - 2; i >= 0; i--) {
			final BigDecimal current = prev.subtract(niceInterval);
			intervalPoints[i] = current.doubleValue();
			prev = current;
		}

		return intervalPoints;
	}

	static <T> T parseType(final Class<T> type, final String s) throws ParseException {

		boolean internalError = false;

		try {
			final Object value;

			if ((type == Boolean.class) || (type == boolean.class)) {
				value = Boolean.valueOf(s);

			} else if ((type == Byte.class) || (type == byte.class)) {
				value = Byte.valueOf(s);

			} else if ((type == Character.class) || (type == char.class)) {

				if ((s == null) || (s.length() != 1)) {
					throw new ParseException(type, s);
				}
				value = Character.valueOf(s.charAt(0));

			} else if ((type == Short.class) || (type == short.class)) {
				value = Short.valueOf(s);

			} else if ((type == Integer.class) || (type == int.class)) {
				value = Integer.valueOf(s);

			} else if ((type == Long.class) || (type == long.class)) {
				value = Long.valueOf(s);

			} else if ((type == Float.class) || (type == float.class)) {
				value = Float.valueOf(s);

			} else if ((type == Double.class) || (type == double.class)) {
				value = Double.valueOf(s);

			} else if (type == String.class) {
				value = s;

			} else if (type == TimeUnit.class) {
				value = TimeUnit.valueOf(s);

			} else if ((type == Boolean[].class) || (type == boolean[].class) || (type == Short[].class) || (type == short[].class)
					|| (type == Integer[].class) || (type == int[].class) || (type == Long[].class) || (type == long[].class)
					|| (type == Float[].class) || (type == float[].class) || (type == Double[].class) || (type == double[].class)
					|| (type == TimeUnit[].class)) {

				final String[] elements = s.split("\\s*,\\s*", -1);
				final int len = elements.length;
				value = Array.newInstance(type.getComponentType(), len);
				for (int i = 0; i < len; i++) {
					Array.set(value, i, parseType(type.getComponentType(), elements[i]));
				}
			} else {
				internalError = true;
				throw new IllegalArgumentException("Internal error: Illegal type <" + type + ">");
			}

			return (T) value;

		} catch (final Exception e) {

			if ((e instanceof ParseException) || internalError) {
				throw e;
			}

			throw new ParseException(type, s);
		}
	}

	static String toShortForm(final TimeUnit timeUnit) {

		final String result;

		switch (timeUnit) {
		case DAYS:
			result = "days";
			break;
		case HOURS:
			result = "hours";
			break;
		case MINUTES:
			result = "minutes";
			break;
		case SECONDS:
			result = "s";
			break;
		case MILLISECONDS:
			result = "ms";
			break;
		case MICROSECONDS:
			result = "\u00B5s";
			break;
		case NANOSECONDS:
			result = "ns";
			break;

		default:
			throw new IllegalArgumentException("Internal error: Illegal time unit <" + timeUnit + ">");
		}

		return result;
	}
}

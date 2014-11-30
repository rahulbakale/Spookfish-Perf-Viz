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

import static spookfishperfviz.Utils.forEach;
import static spookfishperfviz.Utils.reverse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import spookfishperfviz.Density.IndexedDataPoint;

/**
 * @see http://www.brendangregg.com/HeatMaps/latency.html
 * 
 * @author Rahul Bakale
 * @since Nov, 2014
 */
final class TimeSeriesLatencyDensity {

	private static final String COLOR_WHITE = "#FFFFFF";
	private static final int DEFAULT_HEAT_MAP_SINGLE_AREA_HEIGHT = 20;

	/**
	 * TODO - take this as input parameter
	 */
	private static final double MAX_HEAT_MAP_HEIGHT = 400;

	private static final double X_AXIS_LABEL_FONT_SIZE = 10; // TODO - add to
																// SVGConstants.
	private static final String X_AXIS_LABEL_FONT_FAMILY = SVGConstants.MONOSPACE_FONT_FAMILY;

	/*
	 * private static final String[] COLORS = { "#FFE5E5", "#FFCCCC", "#FFB2B2",
	 * "#FF9999", "#FF7F7F", "#FF6666", "#FF4C4C", "#FF3232", "#FF1919",
	 * "#FF0000", "#E50000", "#CC0000", "#B20000", "#990000", "#7F0000",
	 * "#660000", "#4C0000", "#330000", "#190000"};
	 */

	private static final String[] COLORS = { "#FFE6E6", "#FFCCCC", "#FFB2B2", "#FF9999", "#FF8080", "#FF6666", "#FF4D4D", "#FF3333", "#FF1919",
			"#FF0000", "#E60000", "#CC0000", "#B20000", "#990000", "#800000", "#660000"/*, "#4C0000", "#330000", "#1A0000"*/};

	private static final UnaryOperator<Long> LONG_INC_OPERATOR = new UnaryOperator<Long>() {
		@Override
		public Long apply(final Long l) {
			return l == null ? Long.valueOf(0) : Long.valueOf(l.longValue() + 1);
		}
	};

	private static final Function<Long, String> TIMESTAMP_LABEL_MAKER = new Function<Long, String>() {
		@Override
		public String apply(final Long time) {
			return String.format("%1$tH:%1$tM%n%1$td/%1$tm%n%1$tY", time);
		}
	};

	private static final Function<IndexedDataPoint<Double>, String> Y_AXIS_LABEL_MAKER = new Function<Density.IndexedDataPoint<Double>, String>() {
		@Override
		public String apply(final IndexedDataPoint<Double> i) {
			return i.toString(new StripTrailingZeroesAfterDecimalFunction(true));
		}
	};

	static TimeSeriesLatencyDensity create(final double[] latencies, final long[] timestamps, final double[] latencyIntervalPoints) {
		return new TimeSeriesLatencyDensity(latencies, timestamps, Utils.toHashSet(latencyIntervalPoints));
	}

	static TimeSeriesLatencyDensity create(final double[] latencies, final long[] timestamps, final int nIntervalPoints) {
		return create(latencies, timestamps, Utils.createIntervalPoints(latencies, nIntervalPoints));
	}

	static TimeSeriesLatencyDensity create(final double[] latencies, final long[] timestamps, final double minIntervalPoint,
			final double maxIntervalPoint, final int nIntervalPoints) {
		return create(latencies, timestamps, Utils.createIntervalPoints(minIntervalPoint, maxIntervalPoint, nIntervalPoints));
	}

	/**
	 * TODO - check if some code can be moved to {@linkplain Density}
	 */
	private static String[][] getColoredHeatMap(final Density<Double, Long, Long> density, final String colorForZeroVal) {

		final Long[][] matrix = density.getMatrix();

		final int rowCount = matrix.length;
		final int columnCount = matrix[0].length;

		final double min = 1;
		long max = Long.MIN_VALUE;

		for (int r = 0; r < rowCount; r++) {
			for (int c = 0; c < columnCount; c++) {
				final long val = matrix[r][c].longValue();

				if (val > max) {
					max = val;
				}
			}
		}

		final int colorCount = COLORS.length;
		final double d = ((max - min) + 1) / colorCount;

		final String[][] colorMap = new String[rowCount][columnCount];

		for (int r = 0; r < rowCount; r++) {
			for (int c = 0; c < columnCount; c++) {
				final long val = matrix[r][c].longValue();

				final String color;

				if (val == 0) {
					color = colorForZeroVal;
				} else {
					final double i = Math.floor((val - 1) / d);

					if ((i > Integer.MAX_VALUE) || (i < Integer.MIN_VALUE)) {
						throw new RuntimeException("Internal error: " + i);
					}

					final int colorIndex = (int) i;
					color = COLORS[colorIndex];
				}

				colorMap[r][c] = color;
			}
		}

		return colorMap;
	}

	/**
	 * TODO - re-factor common code from this and BarChart.
	 */
	private static HeatMapSVG getHeatMapSVG(final String[][] heatMap, final String colorForZeroVal,
			final SortedSet<IndexedDataPoint<Double>> rowIntervalPoints, final SortedSet<IndexedDataPoint<Long>> columnIntervalPoints,
			final int xAxisLabelSkipCount, final TimeUnit yAxisUnit, final double heatMapSingleAreaWidth) {

		final int rowCount = heatMap.length;
		final int columnCount = heatMap[0].length;

		final String NL = System.lineSeparator();
		final int START_X = SVGConstants.LEFT_RIGHT_MARGIN;
		final int START_Y = SVGConstants.TOP_DOWN_MARGIN;

		final int TICK_LENGTH = 10;
		final int SPACE_BETWEEN_LABEL_AND_TICK = 10;
		final int SPACE_BETWEEN_TITLE_AND_LABEL = 10;

		final String yAxisTitle = "Latency" + " (" + Utils.toShortForm(yAxisUnit) + ")";
		final double Y_AXIS_TITLE_FONT_SIZE = SVGConstants.MONOSPACE_FONT_SIZE;
		final String Y_AXIS_TITLE_FONT_FAMILY = SVGConstants.MONOSPACE_FONT_FAMILY;
		final double Y_AXIS_TITLE_START_X = START_X;
		final double Y_AXIS_TITLE_END_X = Y_AXIS_TITLE_START_X + SVGConstants.MONOSPACE_FONT_SIZE;

		final double Y_AXIS_LABEL_FONT_SIZE = SVGConstants.MONOSPACE_FONT_SIZE;
		final String Y_AXIS_LABEL_FONT_FAMILY = SVGConstants.MONOSPACE_FONT_FAMILY;
		final double Y_AXIS_LABEL_START_X = Y_AXIS_TITLE_END_X + SPACE_BETWEEN_TITLE_AND_LABEL;

		final String X_AXIS_TITLE = "Time";
		final double X_AXIS_TITLE_FONT_SIZE = SVGConstants.MONOSPACE_FONT_SIZE;
		final String X_AXIS_TITLE_FONT_FAMILY = SVGConstants.MONOSPACE_FONT_FAMILY;

		final double BOX_START_Y = START_Y;

		final ArrayList<String> yAxisLabels = 
				forEach(reverse(rowIntervalPoints, new ArrayListSupplier<IndexedDataPoint<Double>>()), Y_AXIS_LABEL_MAKER, new ArrayListSupplier<String>());
		
		final int yAxisMaxLabelLength = 
				Collections.max(forEach(yAxisLabels, CharSeqLengthFunction.INSTANCE, new ArrayListSupplier<Integer>())).intValue();
		
		final ArrayList<String> yAxisPaddedLabels = 
				Utils.getPaddedLabels(yAxisLabels, yAxisMaxLabelLength, new ArrayListSupplier<String>(), true);

		final double yAxisMaxLabelWidth = yAxisMaxLabelLength * SVGConstants.MONOSPACE_FONT_WIDTH;

		final double yAxisMajorTickStartX = Y_AXIS_LABEL_START_X + yAxisMaxLabelWidth + SPACE_BETWEEN_LABEL_AND_TICK;
		final double yAxisTickEndX = yAxisMajorTickStartX + TICK_LENGTH;

		final double heatMapSingleAreaHeight = Math.min(MAX_HEAT_MAP_HEIGHT / rowCount, DEFAULT_HEAT_MAP_SINGLE_AREA_HEIGHT);

		final double heatMapHeight = rowCount * heatMapSingleAreaHeight;
		final double heatMapWidth = columnCount * heatMapSingleAreaWidth;

		final double heatMapBoxStartX = yAxisTickEndX;
		final double heatMapStartX = heatMapBoxStartX + /* gutter */heatMapSingleAreaWidth;
		final double heatMapStartY = BOX_START_Y + /* gutter */DEFAULT_HEAT_MAP_SINGLE_AREA_HEIGHT;
		final double heatMapBoxEndX = heatMapStartX + heatMapWidth + /* gutter */heatMapSingleAreaWidth;
		final double heatMapBoxEndY = heatMapStartY + heatMapHeight + /* gutter */DEFAULT_HEAT_MAP_SINGLE_AREA_HEIGHT;
		final double heatMapBoxHeight = heatMapBoxEndY - BOX_START_Y;
		final double heatMapBoxWidth = heatMapBoxEndX - heatMapBoxStartX;

		final double yAxisTitleStartY = BOX_START_Y + ((heatMapBoxHeight / 2.0) - (Y_AXIS_TITLE_FONT_SIZE / 2.0));

		final double xAxisTickStartY = heatMapBoxEndY;
		final double xAxisMajorTickEndY = xAxisTickStartY + TICK_LENGTH;
		final double xAxisMinorTickEndY = xAxisTickStartY + (TICK_LENGTH / 2.0);

		// TODO - check if cast to int is OK
		final int yAxisLabelSkipCount = (int) (DEFAULT_HEAT_MAP_SINGLE_AREA_HEIGHT / heatMapSingleAreaHeight);

		final double xAxisLabelStartY = xAxisMajorTickEndY + SPACE_BETWEEN_LABEL_AND_TICK;

		final StringBuilder yAxisTitleSVG;
		{
			yAxisTitleSVG = new StringBuilder();

			yAxisTitleSVG.append("<text ");
			yAxisTitleSVG.append("style=\"");
			yAxisTitleSVG.append("font-family:").append(Y_AXIS_TITLE_FONT_FAMILY).append(";");

			yAxisTitleSVG.append("font-size:").append(Y_AXIS_TITLE_FONT_SIZE).append("px;");
			yAxisTitleSVG.append("text-anchor: middle;"); // related to rotation of the title
			yAxisTitleSVG.append("dominant-baseline: middle;"); // related to rotation of the title
			yAxisTitleSVG.append("\"");
			yAxisTitleSVG.append(" x=\"").append(Y_AXIS_TITLE_START_X).append("\"");
			yAxisTitleSVG.append(" y=\"").append(yAxisTitleStartY).append("\"");
			yAxisTitleSVG.append(" transform=\"rotate(-90,").append(Y_AXIS_TITLE_START_X).append(",").append(yAxisTitleStartY).append(")\"");
			yAxisTitleSVG.append(">");
			yAxisTitleSVG.append(yAxisTitle);
			yAxisTitleSVG.append("</text>");
		}

		final StringBuilder yAxisLabelsSVG;
		final StringBuilder yAxisTicksSVG;
		{
			yAxisLabelsSVG = new StringBuilder();
			yAxisTicksSVG = new StringBuilder();

			yAxisLabelsSVG.append("<g style=\"font-family:").append(Y_AXIS_LABEL_FONT_FAMILY).append(";font-size:").append(Y_AXIS_LABEL_FONT_SIZE)
					.append("px;\">").append(NL);
			yAxisTicksSVG.append("<g style=\"stroke:black; stroke-width:1\">").append(NL);

			double yAxisLabelStartY = heatMapStartY; // BOX_START_Y;

			for (int i = 0; i <= rowCount; i++) {

				final boolean skipLabel = Utils.skipLabel(i, rowCount, yAxisLabelSkipCount);

				if (skipLabel == false) {
					yAxisLabelsSVG.append("<text style=\"dominant-baseline: central;\" x=\"").append(Y_AXIS_LABEL_START_X).append("\" y=\"")
							.append(yAxisLabelStartY).append("\">").append(yAxisPaddedLabels.get(i)).append("</text>").append(NL);
					
					yAxisTicksSVG
						.append("<line x1=\"").append(yAxisMajorTickStartX)
						.append("\" y1=\"").append(yAxisLabelStartY)
						.append("\" x2=\"").append(yAxisTickEndX)
						.append("\" y2=\"").append(yAxisLabelStartY)
						.append("\"/>").append(NL);
				}

				yAxisLabelStartY += heatMapSingleAreaHeight;
			}

			yAxisLabelsSVG.append("</g>");
			yAxisTicksSVG.append("</g>");
		}

		final double xAxisLabelEndY;
		final StringBuilder xAxisTicksSVG;
		final StringBuilder xAxisLabelsSVG;
		{
			final ArrayList<IndexedDataPoint<Long>> points = new ArrayList<>(columnIntervalPoints);

			xAxisTicksSVG = new StringBuilder();
			xAxisLabelsSVG = new StringBuilder();

			xAxisTicksSVG.append("<g style=\"stroke:black; stroke-width:1\">").append(NL);
			xAxisLabelsSVG.append("<g style=\"font-family:").append(X_AXIS_LABEL_FONT_FAMILY).append(";font-size:").append(X_AXIS_LABEL_FONT_SIZE).append("px;\">").append(NL);

			double x = heatMapStartX; // boxStartX;

			int maxXAxisLabelPartCount = Integer.MIN_VALUE;
			final double fontSize = X_AXIS_LABEL_FONT_SIZE;

			for (int i = 0; i <= columnCount; i++) {
				final boolean skipLabel = Utils.skipLabel(i, columnCount, xAxisLabelSkipCount);

				final double xAxisTickEndY = skipLabel ? xAxisMinorTickEndY : xAxisMajorTickEndY;

				xAxisTicksSVG.append("<line x1=\"").append(x).append("\" y1=\"").append(xAxisTickStartY).append("\" x2=\"").append(x)
						.append("\" y2=\"").append(xAxisTickEndY).append("\"/>").append(NL);

				if (skipLabel == false) {
					final String multiLineLabel = points.get(i).toString(TIMESTAMP_LABEL_MAKER);

					final MultiSpanSVGText label = Utils.createMultiSpanSVGText(multiLineLabel, x, xAxisLabelStartY, fontSize, null);

					xAxisLabelsSVG.append(label.getSvg());

					maxXAxisLabelPartCount = Math.max(label.getSpanCount(), maxXAxisLabelPartCount);
				}

				x += heatMapSingleAreaWidth;
			}

			xAxisLabelEndY = xAxisLabelStartY + (maxXAxisLabelPartCount * fontSize);

			xAxisTicksSVG.append("</g>");
			xAxisLabelsSVG.append("</g>");
		}

		final double xAxisTitleEndY;
		final StringBuilder xAxisTitleSVG;
		{
			final double xAxisTitleStartY = xAxisLabelEndY + SPACE_BETWEEN_TITLE_AND_LABEL;
			xAxisTitleEndY = xAxisTitleStartY + X_AXIS_TITLE_FONT_SIZE;

			xAxisTitleSVG = new StringBuilder();

			xAxisTitleSVG.append("<text ");
			xAxisTitleSVG.append("style=\"");
			xAxisTitleSVG.append("font-family:").append(X_AXIS_TITLE_FONT_FAMILY).append(";");
			xAxisTitleSVG.append("font-size:").append(X_AXIS_TITLE_FONT_SIZE).append("px;");
			xAxisTitleSVG.append("text-anchor: middle;");
			xAxisTitleSVG.append("\"");
			xAxisTitleSVG.append(" x=\"").append(heatMapBoxStartX + (heatMapBoxWidth / 2.0)).append("\"");
			xAxisTitleSVG.append(" y=\"").append(xAxisTitleStartY).append("\"");
			xAxisTitleSVG.append(">");
			xAxisTitleSVG.append(X_AXIS_TITLE);
			xAxisTitleSVG.append("</text>");
		}

		final StringBuilder boxSVG;
		{
			boxSVG = new StringBuilder();
			boxSVG.append("<rect x=\"").append(heatMapBoxStartX).append("\" y=\"").append(BOX_START_Y).append("\" width=\"").append(heatMapBoxWidth)
					.append("\" height=\"").append(heatMapBoxHeight).append("\" style=\"fill:").append(colorForZeroVal)
					.append(";stroke:black;stroke-width:1\"/>").append(NL);
		}

		final StringBuilder colorMapSVG;
		{
			colorMapSVG = new StringBuilder();

			double y = heatMapStartY; // BOX_START_Y;

			for (int i = rowCount - 1; i >= 0; i--) {
				final String[] row = heatMap[i];

				double x = heatMapStartX; // boxStartX;

				for (final String color : row) {

					if (!Objects.equals(color, colorForZeroVal)) {
						colorMapSVG.append("<rect");
						colorMapSVG.append(" x=\"").append(x).append("\"");
						colorMapSVG.append(" y=\"").append(y).append("\"");
						colorMapSVG.append(" fill=\"").append(color).append("\"");
						colorMapSVG.append(" width=\"").append(heatMapSingleAreaWidth).append("\"");
						colorMapSVG.append(" height=\"").append(heatMapSingleAreaHeight).append("\"");
						colorMapSVG.append("/>");
						colorMapSVG.append(NL);
					}

					x += heatMapSingleAreaWidth;
				}

				y += heatMapSingleAreaHeight;
			}
		}

		final StringBuilder svg = new StringBuilder();

		// Need to set the width & height of the SVG to prevent clipping of large SVGs.
		final double svgEndX = heatMapBoxEndX + SVGConstants.LEFT_RIGHT_MARGIN;
		final double svgEndY = xAxisTitleEndY + SVGConstants.TOP_DOWN_MARGIN;

		svg.append("<svg width=\"").append(svgEndX).append("\" height=\"").append(svgEndY).append("\">").append(NL);
		svg.append(xAxisTitleSVG).append(NL);
		svg.append(xAxisTicksSVG).append(NL);
		svg.append(xAxisLabelsSVG).append(NL);
		svg.append(yAxisTitleSVG).append(NL);
		svg.append(yAxisLabelsSVG).append(NL);
		svg.append(yAxisTicksSVG).append(NL);
		svg.append(boxSVG).append(NL);
		svg.append(colorMapSVG).append(NL);
		svg.append("</svg>");

		return new HeatMapSVG(svg.toString(), xAxisLabelSkipCount, heatMapBoxStartX, heatMapSingleAreaWidth);
	}

	private final Density<Double, Long, Long> density;
	private final int defaultTimeLabelSkipCount;

	private TimeSeriesLatencyDensity(final double[] latencies, final long[] timestamps, final Set<Double> responseTimeIntervalPoints) {
		this(latencies, timestamps, null, responseTimeIntervalPoints);
	}

	private TimeSeriesLatencyDensity(final double[] latencies, 
									 final long[] timestamps, 
									 final Set<Long> inputTimestampIntervalPoints, 
									 final Set<Double> responseTimeIntervalPoints) {
		
		Objects.requireNonNull(latencies);
		Objects.requireNonNull(timestamps);

		if (latencies.length != timestamps.length) {
			throw new IllegalArgumentException("Number of latencies must be same as number of timestamps");
		}

		final long[] sortedTimestamps = Utils.sort(timestamps);
		final long minTime = sortedTimestamps[0];
		final long maxTime = sortedTimestamps[sortedTimestamps.length - 1];
		final long duration = maxTime - minTime;
		final long threshold = TimeUnit.HOURS.toMillis(5);

		final int defaultTimeLabelSkipCount = (duration > threshold) ? 1 : 2;

		final Set<Long> timestampIntervalPoints;
		if (inputTimestampIntervalPoints == null) {
			final long timeIntervalInMillis = (duration > threshold) ? TimeUnit.MINUTES.toMillis(30) : TimeUnit.MINUTES.toMillis(5);
			timestampIntervalPoints = Utils.getTimestampIntervalPoints(timestamps, timeIntervalInMillis);
		} else {
			timestampIntervalPoints = inputTimestampIntervalPoints;
		}

		final Density<Double, Long, Long> d = Density.create(responseTimeIntervalPoints, timestampIntervalPoints, Long.valueOf(0), Long.class);

		for (int i = 0; i < latencies.length; i++) {
			final Double row = Double.valueOf(latencies[i]);
			final Long column = Long.valueOf(timestamps[i]);

			d.apply(row, column, LONG_INC_OPERATOR);
		}

		this.density = d;
		this.defaultTimeLabelSkipCount = defaultTimeLabelSkipCount;
	}

	private String[][] getHeatMap(final String colorForZeroVal) {
		return getColoredHeatMap(this.density, colorForZeroVal);
	}

	HeatMapSVG getHeatMapSVG(final TimeUnit latencyUnit, final double heatMapSingleAreaWidth) {
		return getHeatMapSVG(latencyUnit, this.defaultTimeLabelSkipCount, heatMapSingleAreaWidth);
	}

	HeatMapSVG getHeatMapSVG(final TimeUnit latencyUnit, final int timeLabelSkipCount, final double heatMapSingleAreaWidth) {

		final String colorForZeroVal = COLOR_WHITE;

		final String[][] heatMap = getHeatMap(colorForZeroVal);
		return getHeatMapSVG(heatMap, colorForZeroVal, this.density.getRowIntervalPoints(), this.density.getColumnIntervalPoints(),
				timeLabelSkipCount, latencyUnit, heatMapSingleAreaWidth);
	}

	String getTrxCountBarChartSVG(final int labelSkipCount, final double boxStartX, final double barWidth) {
		return getTrxCountBarChartSVG(this.density, labelSkipCount, boxStartX, barWidth);
	}

	private static String getTrxCountBarChartSVG(final Density<Double, Long, Long> density, final int labelSkipCount, final double boxStartX,
			final double barWidth) {
		final int MAX_BAR_LENGTH = 100;

		final Long[][] matrix = density.getMatrix();
		final NavigableSet<IndexedDataPoint<Long>> columnIntervalPoints = density.getColumnIntervalPoints();

		final int rowCount = matrix.length;
		final int columnCount = matrix[0].length;

		final long[] columnTotals = new long[columnCount];

		for (int column = 0; column < columnCount; column++) {
			long sum = 0;
			for (int row = 0; row < rowCount; row++) {
				sum += matrix[row][column].longValue();
			}

			columnTotals[column] = sum;
		}

		final List<String> labels = new ArrayList<>();
		for (final IndexedDataPoint<Long> columnIntervalPoint : columnIntervalPoints) {
			labels.add(columnIntervalPoint.toString(TIMESTAMP_LABEL_MAKER));
		}

		final VerticalBarChart barChart = VerticalBarChart.create(columnTotals, labels.toArray(new String[labels.size()]));

		return barChart.toSVG(MAX_BAR_LENGTH, barWidth, boxStartX, X_AXIS_LABEL_FONT_FAMILY, X_AXIS_LABEL_FONT_SIZE, labelSkipCount, true);
	}

	@Override
	public String toString() {
		return this.density.toString();
	}
}

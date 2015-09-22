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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import spookfishperfviz.Density.IndexedDataPoint;

/**
 * @see http://www.brendangregg.com/HeatMaps/latency.html
 * 
 * @author Rahul Bakale
 * @since Nov, 2014
 */
final class TimeSeriesLatencyDensity {

	private static final int DEFAULT_HEAT_MAP_SINGLE_AREA_HEIGHT = 10;

	/**
	 * TODO - take this as input parameter
	 */
	private static final int MAX_HEAT_MAP_HEIGHT = 400;

	private static final int DEFAULT_MAX_INTERBAL_POINTS_FOR_LATENCY_DENSITY = MAX_HEAT_MAP_HEIGHT / DEFAULT_HEAT_MAP_SINGLE_AREA_HEIGHT;

	private static final double X_AXIS_LABEL_FONT_SIZE = 10; // TODO - add to SVGConstants.
	private static final String X_AXIS_LABEL_FONT_FAMILY = SVGConstants.MONOSPACE_FONT_FAMILY;

	
	private static final UnaryOperator<Long> LONG_INC_OPERATOR = new UnaryOperator<Long>() {
		@Override
		public Long apply(final Long l) {
			return l == null ? Long.valueOf(0) : Long.valueOf(l.longValue() + 1);
		}
	};

	

	

	private static final Function<IndexedDataPoint<Double>, String> Y_AXIS_LABEL_MAKER = new Function<Density.IndexedDataPoint<Double>, String>() {
		@Override
		public String apply(final IndexedDataPoint<Double> i) {
			return i.toString(new StripTrailingZeroesAfterDecimalFunction(true));
		}
	};

	private static final class TimestampLabelMaker implements Function<Long, String> {

		private static final String NL = System.lineSeparator();

		private final SimpleDateFormat dayMonthFormat;
		private final SimpleDateFormat yearFormat;
		private final SimpleDateFormat timeFormat;

		TimestampLabelMaker(final TimeZone timeZone) {

			final SimpleDateFormat dayMonth = new SimpleDateFormat("dd/MM");
			dayMonth.setTimeZone(timeZone);

			final SimpleDateFormat year = new SimpleDateFormat("yyyy");
			year.setTimeZone(timeZone);

			final SimpleDateFormat time = new SimpleDateFormat("HH:mm");
			time.setTimeZone(timeZone);

			this.dayMonthFormat = dayMonth;
			this.yearFormat = year;
			this.timeFormat = time;
		}

		@Override
		public String apply(final Long time) {
			final Date d = new Date(time.longValue());
			return this.timeFormat.format(d) + NL + this.dayMonthFormat.format(d) + NL + this.yearFormat.format(d);
		}
	}

	private static final class TimestampTooltipMaker implements Function<Long, String> {

		private final SimpleDateFormat format;

		TimestampTooltipMaker(final TimeZone timeZone) {

			final SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy HH:mm");
			f.setTimeZone(timeZone);

			this.format = f;
		}

		@Override
		public String apply(final Long time) {
			return this.format.format(time);
		}
	}

	static TimeSeriesLatencyDensity create(	final double[] latencies, 
											final long[] timestamps, 
											final TimeZone outputTimeZone, 
											final Integer maxIntervalPointsForLatencyDensity) {
		
		final double[] minMax = Utils.minMax(latencies);
		final double minIntervalPoint = minMax[0];
		final double maxIntervalPoint = minMax[1];

		return create0(latencies, timestamps, outputTimeZone, minIntervalPoint, maxIntervalPoint, maxIntervalPointsForLatencyDensity);
	}

	static TimeSeriesLatencyDensity create(	final double[] latencies, 
											final long[] timestamps, 
											final TimeZone outputTimeZone, 
											final double minIntervalPointForLatencyDensity, 
											final double maxIntervalPointForLatencyDensity, 
											final Integer maxIntervalPointsForLatencyDensity) {
		
		
		if (minIntervalPointForLatencyDensity > maxIntervalPointForLatencyDensity) {
			throw new IllegalArgumentException("min = <" + minIntervalPointForLatencyDensity + ">, max = <" + maxIntervalPointForLatencyDensity + ">");
		}

		final double[] minMax = Utils.minMax(latencies);
		final double minLatency = minMax[0];
		final double maxLatency = minMax[1];

		final double minIntervalPoint;
		final double maxIntervalPoint;

		if ((maxIntervalPointForLatencyDensity < minLatency) || (minIntervalPointForLatencyDensity > maxLatency)) {
			minIntervalPoint = minIntervalPointForLatencyDensity;
			maxIntervalPoint = maxIntervalPointForLatencyDensity;
		} else {
			minIntervalPoint = Math.max(minLatency, minIntervalPointForLatencyDensity);
			maxIntervalPoint = Math.min(maxLatency, maxIntervalPointForLatencyDensity);
		}

		return create0(latencies, timestamps, outputTimeZone, minIntervalPoint, maxIntervalPoint, maxIntervalPointsForLatencyDensity);
	}
	
	private static TimeSeriesLatencyDensity create0(final double[] latencies, 
													final long[] timestamps, 
													final TimeZone outputTimeZone, 
													final double adjustedMinIntervalPointForLatencyDensity,
													final double adjustedMaxIntervalPointForLatencyDensity, 
													final Integer maxIntervalPointsForLatencyDensity) {
		
		final int maxIntervalPoints = 
				maxIntervalPointsForLatencyDensity == null ? 
						DEFAULT_MAX_INTERBAL_POINTS_FOR_LATENCY_DENSITY : maxIntervalPointsForLatencyDensity.intValue();
		
		final double[] intervalPointsForLatencyDensity = 
				createIntervalPoints(adjustedMinIntervalPointForLatencyDensity, adjustedMaxIntervalPointForLatencyDensity, maxIntervalPoints);
		
		return new TimeSeriesLatencyDensity(latencies, timestamps, outputTimeZone, intervalPointsForLatencyDensity);
	}
	
	private static double[] createIntervalPoints(final double minIntervalPoint, final double maxIntervalPoint, final int maxIntervalPoints) {

		final double adjustedMin = Math.floor(minIntervalPoint);
		final double adjustedMax = Math.ceil(maxIntervalPoint);
		
		if (adjustedMin > adjustedMax) {
			throw new IllegalArgumentException("min = <" + adjustedMin + ">, max = <" + adjustedMax + ">");
		}
		
		//adjustedMin will be equal to adjustedMax in cases like minIntervalPoint=3.0 and maxIntervalPoint=3.0. 
		//In such cases nIntervalPoints can be taken as 1. 
		
		final int nIntervalPoints = adjustedMin == adjustedMax ? 1 : Math.min(maxIntervalPoints, (int) Math.ceil(adjustedMax - adjustedMin));
		
		return Utils.createIntervalPoints(adjustedMin, adjustedMax, nIntervalPoints);
	}

	
	private final Density<Double, Long, Long> density;
	private final int defaultTimeLabelSkipCount;

	
	private final TimestampLabelMaker timestampLabelMaker;
	private final TimestampTooltipMaker timestampTooltipMaker;

	private TimeSeriesLatencyDensity(final double[] latencies, final long[] timestamps, final TimeZone outputTimeZone, final double[] responseTimeIntervalPoints) {
		this(latencies, timestamps, outputTimeZone, null, Utils.toHashSet(responseTimeIntervalPoints));
	}

	private TimeSeriesLatencyDensity(final double[] latencies, 
									 final long[] timestamps, 
									 final TimeZone outputTimeZone, 
									 final Set<Long> inputTimestampIntervalPoints, 
									 final Set<Double> responseTimeIntervalPoints) {
		
		Objects.requireNonNull(latencies);
		Objects.requireNonNull(timestamps);
		Objects.requireNonNull(outputTimeZone);
		
		this.timestampLabelMaker = new TimestampLabelMaker(outputTimeZone);
		this.timestampTooltipMaker = new TimestampTooltipMaker(outputTimeZone);

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
			timestampIntervalPoints = Utils.getTimestampIntervalPoints(timestamps, outputTimeZone, timeIntervalInMillis);
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

	HeatMapSVG getHeatMapSVG(final TimeUnit latencyUnit, final double heatMapSingleAreaWidth, final ColorRampScheme colorScheme) {
		return getHeatMapSVG(latencyUnit, this.defaultTimeLabelSkipCount, heatMapSingleAreaWidth, colorScheme);
	}

	HeatMapSVG getHeatMapSVG(final TimeUnit latencyUnit, final int timeLabelSkipCount, final double heatMapSingleAreaWidth, final ColorRampScheme colorScheme) {

		return getHeatMapSVG(this.density, colorScheme, timeLabelSkipCount, latencyUnit, this.timestampLabelMaker, this.timestampTooltipMaker, heatMapSingleAreaWidth);
	}

	/**
	 * TODO - re-factor common code from this and BarChart.
	 */
	private static HeatMapSVG getHeatMapSVG(final Density<Double, Long, Long> density, 
											final ColorRampScheme colorScheme, 
											final int timeLabelSkipCount,
											final TimeUnit latencyUnit, 
											final TimestampLabelMaker timestampLabelMaker, 
											final TimestampTooltipMaker timestampTooltipMaker, 
											final double heatMapSingleAreaWidth) {
		
		final Long[][] matrix = density.getMatrix();
		
		final String[][] heatMap = getColoredHeatMap(matrix, colorScheme);
		
		final int rowCount = heatMap.length;
		final int columnCount = heatMap[0].length;
		
		final String NL = System.lineSeparator();
		final int START_X = SVGConstants.LEFT_RIGHT_MARGIN;
		final int START_Y = SVGConstants.TOP_DOWN_MARGIN;
		
		final int TICK_LENGTH = 10;
		final int SPACE_BETWEEN_LABEL_AND_TICK = 10;
		final int SPACE_BETWEEN_TITLE_AND_LABEL = 10;
		
		final String latencyUnitShortForm = Utils.toShortForm(latencyUnit);
		final String yAxisTitle = "Latency" + " (" + latencyUnitShortForm + ")";
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
				forEach(reverse(density.getRowIntervalPoints(), new ArrayListSupplier<IndexedDataPoint<Double>>()), Y_AXIS_LABEL_MAKER, new ArrayListSupplier<String>());
		
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
		final double heatMapStartX = heatMapBoxStartX + heatMapSingleAreaWidth;
		final double heatMapStartY = BOX_START_Y + /* gutter */DEFAULT_HEAT_MAP_SINGLE_AREA_HEIGHT;
		final double heatMapBoxEndX = heatMapStartX + heatMapWidth + heatMapSingleAreaWidth;
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
		
			double yAxisLabelStartY = heatMapStartY;
		
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
		
		final ArrayList<IndexedDataPoint<Long>> timestampPoints = new ArrayList<>(density.getColumnIntervalPoints());
		
		final double xAxisLabelEndY;
		final StringBuilder xAxisTicksSVG;
		final StringBuilder xAxisLabelsSVG;
		{
			xAxisTicksSVG = new StringBuilder();
			xAxisLabelsSVG = new StringBuilder();
		
			xAxisTicksSVG.append("<g style=\"stroke:black; stroke-width:1\">").append(NL);
			xAxisLabelsSVG.append("<g style=\"font-family:").append(X_AXIS_LABEL_FONT_FAMILY).append(";font-size:").append(X_AXIS_LABEL_FONT_SIZE).append("px;\">").append(NL);
		
			double x = heatMapStartX; // boxStartX;
		
			int maxXAxisLabelPartCount = Integer.MIN_VALUE;
			final double fontSize = X_AXIS_LABEL_FONT_SIZE;
		
			for (int i = 0; i <= columnCount; i++) {
				final boolean skipLabel = Utils.skipLabel(i, columnCount, timeLabelSkipCount);
		
				final double xAxisTickEndY = skipLabel ? xAxisMinorTickEndY : xAxisMajorTickEndY;
		
				xAxisTicksSVG.append("<line x1=\"").append(x).append("\" y1=\"").append(xAxisTickStartY).append("\" x2=\"").append(x)
						.append("\" y2=\"").append(xAxisTickEndY).append("\"/>").append(NL);
		
				if (skipLabel == false) {
					final String multiLineLabel = timestampPoints.get(i).toString(timestampLabelMaker);
		
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
		
		final String colorForZeroVal = colorScheme.getBackgroundColor();
		
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
		
			double y = heatMapStartY;
		
			for (int rowNum = rowCount - 1, r = 0; rowNum >= 0; rowNum--, r++) {
				
				final String[] row = heatMap[rowNum];
				
				final String yTooltip1 = yAxisLabels.get(r + 1);
				final String yTooltip2 = yAxisLabels.get(r);
				
				double x = heatMapStartX;
		
				for (int colNum = 0; colNum < columnCount; colNum++) {
					
					final String color = row[colNum];
		
					if (!Objects.equals(color, colorForZeroVal)) {
						
						colorMapSVG.append("<rect");
						colorMapSVG.append(" x=\"").append(x).append("\"");
						colorMapSVG.append(" y=\"").append(y).append("\"");
						colorMapSVG.append(" fill=\"").append(color).append("\"");
						colorMapSVG.append(" width=\"").append(heatMapSingleAreaWidth).append("\"");
						colorMapSVG.append(" height=\"").append(heatMapSingleAreaHeight).append("\"");
						colorMapSVG.append(">");
						
						{//TOOLTIP
							
							//TODO - escape HTML special characters in tooltip text. For e.g. spaces.
							
							colorMapSVG.append("<title>");
							colorMapSVG.append("Count = ").append(matrix[rowNum][colNum]).append(", Color = ").append(color).append(NL);
							final String xTooltip1 = timestampPoints.get(colNum).toString(timestampTooltipMaker);
							final String xTooltip2 = timestampPoints.get(colNum + 1).toString(timestampTooltipMaker);
							colorMapSVG.append("Period: (").append(xTooltip1).append(" - ").append(xTooltip2).append(')').append(NL);
							colorMapSVG.append("Latency range: (").append(yTooltip1).append(" - ").append(yTooltip2).append(") ").append(latencyUnitShortForm).append(NL);
							colorMapSVG.append("</title>");
						}
						
						colorMapSVG.append("</rect>");
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
		
		return new HeatMapSVG(svg.toString(), timeLabelSkipCount, heatMapBoxStartX, heatMapSingleAreaWidth);
	}

	/**
	 * TODO - check if some code can be moved to {@linkplain Density}
	 */
	private static String[][] getColoredHeatMap(final Long[][] matrix, final ColorRampScheme colorScheme) {

		final String[] colorMapArray = ColorRampCalculator.getColorMap(Utils.toOneDimArray(matrix), colorScheme);

		final int rowCount = matrix.length;
		final int columnCount = matrix[0].length;

		final String[][] colorMapMatrix = new String[rowCount][columnCount];
		Utils.fillMatrix(colorMapArray, colorMapMatrix);

		return colorMapMatrix;
	}

	String getTrxCountBarChartSVG(final int labelSkipCount, final double boxStartX, final double barWidth, final ColorRampScheme colorRampScheme) {
		return getTrxCountBarChartSVG(this.density, labelSkipCount, this.timestampLabelMaker, boxStartX, barWidth, colorRampScheme);
	}

	private static String getTrxCountBarChartSVG(final Density<Double, Long, Long> density, final int labelSkipCount, final TimestampLabelMaker timestampLabelMaker, final double boxStartX,
			final double barWidth, final ColorRampScheme colorRampScheme) {
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
			labels.add(columnIntervalPoint.toString(timestampLabelMaker));
		}

		final VerticalBarChart barChart = VerticalBarChart.create(columnTotals, labels.toArray(new String[labels.size()]));

		return barChart.toSVG(MAX_BAR_LENGTH, barWidth, boxStartX, X_AXIS_LABEL_FONT_FAMILY, X_AXIS_LABEL_FONT_SIZE, labelSkipCount, colorRampScheme);
	}

	@Override
	public String toString() {
		return this.density.toString();
	}
}

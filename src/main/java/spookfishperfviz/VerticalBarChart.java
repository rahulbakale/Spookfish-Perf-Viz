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
final class VerticalBarChart {
	
	static VerticalBarChart create(final double[] data, final String[] labels) {
		return new VerticalBarChart(data, labels);
	}

	static VerticalBarChart create(final long[] data, final String[] labels) {
		return create(Utils.toDoubles(data), labels);
	}

	static VerticalBarChart create(final int[] data, final String[] labels) {
		return create(Utils.toDoubles(data), labels);
	}

	private final String[] labels;
	private final double[] data;
	private final double max;

	private final Double nonZeroMin;
	private final Double nonZeroMax;

	private VerticalBarChart(final double[] data, final String[] labels) {
		this.data = data;
		this.labels = labels;

		final double[] sorted = Utils.sort(data);
		final double min = sorted[0];
		final double max = sorted[sorted.length - 1];

		this.max = max;

		if (min == 0) {
			Double nonZeroMin = null;

			for (int i = 1; i < sorted.length; i++) {
				final double d = sorted[i];
				if (d != 0) {
					nonZeroMin = Double.valueOf(d);
					break;
				}
			}

			this.nonZeroMin = nonZeroMin;
		} else {
			this.nonZeroMin = Double.valueOf(min);
		}

		if (max == 0) {
			Double nonZeroMax = null;

			for (int i = sorted.length - 2; i >= 0; i--) {
				final double d = sorted[i];
				if (d != 0) {
					nonZeroMax = Double.valueOf(d);
					break;
				}
			}

			this.nonZeroMax = nonZeroMax;
		} else {
			this.nonZeroMax = Double.valueOf(max);
		}
	}

	String toSVG(	final int maxBarLength, 
					final double barWidth, 
					final double boxStartX, 
					final String labelFontFamily, 
					final double labelFontSize, 
					final int labelSkipCount, 
					final boolean useColorCoding) {
		
		final double[] data = this.data;
		final int size = data.length;
		final Double nonZeroMin = this.nonZeroMin;
		final double max = this.max;
		final Double nonZeroMax = this.nonZeroMax;
		final String[] labels = this.labels;

		final String NL = System.lineSeparator();

		final int START_Y = SVGConstants.TOP_DOWN_MARGIN;

		final double barChartStartX = boxStartX + /* gutter */barWidth;

		final int SPACE_BETWEEN_LABEL_AND_BAR = 10;

		final double barStartY = START_Y + maxBarLength;
		final double labelStartY = barStartY + SPACE_BETWEEN_LABEL_AND_BAR;

		final double boxWidth = (size + 1) * barWidth;

		final String indent1 = "  ";
		final String indent2 = "    ";

		final StringBuilder svgBars = new StringBuilder();
		svgBars.append("<g style=\"stroke:grey; stroke-width:").append(barWidth).append("\">").append(NL);

		final StringBuilder svgLabels = new StringBuilder();
		svgLabels.append("<g fill=\"black\" style=\"font-family:").append(labelFontFamily).append(";font-size:").append(labelFontSize).append("px;\">").append(NL);

		int maxXAxisLabelPartCount = Integer.MIN_VALUE;
		double x = barChartStartX;

		for (int i = 0; i < size; i++, x += barWidth) {
			final double d = data[i];
			final long scaledBarLength = scale(maxBarLength, max, d);
			final String[] lineAndLabelColor = useColorCoding ? getLineAndLabelColor(d, nonZeroMin, nonZeroMax) : null;

			svgBars.append(indent2);
			svgBars.append("<line x1=\"").append(x).append("\"");
			svgBars.append(" y1=\"").append(barStartY).append("\"");
			svgBars.append(" x2=\"").append(x).append("\"");
			svgBars.append(" y2=\"").append(barStartY - scaledBarLength).append("\"");

			final String lineColor = lineAndLabelColor == null ? null : lineAndLabelColor[0];
			if (lineColor != null) {
				svgBars.append(" style=\"stroke:").append(lineColor).append("\"");
			}
			svgBars.append("/>").append(NL);

			final boolean skipLabel = Utils.skipLabel(i, size, labelSkipCount);
			if (skipLabel == false) {

				final String label = Utils.escapeHTMLSpecialChars(labels[i]);

				final String labelColor = lineAndLabelColor == null ? null : lineAndLabelColor[1];

				final MultiSpanSVGText multiSpanSVGText = Utils.createMultiSpanSVGText(label, x, labelStartY, labelFontSize, labelColor);

				svgLabels.append(multiSpanSVGText.getSvg()).append(NL);

				maxXAxisLabelPartCount = Math.max(multiSpanSVGText.getSpanCount(), maxXAxisLabelPartCount);
			}
		}

		svgBars.append("</g>");
		svgLabels.append("</g>");

		final double boxHeight = labelStartY + (maxXAxisLabelPartCount * labelFontSize) + SVGConstants.TOP_DOWN_MARGIN;

		final String rect = "<rect x=\"" + boxStartX + "\" width=\"" + boxWidth + "\" height=\"" + boxHeight + "\" style=\"fill:white;stroke:black;stroke-width:1\"/>" + NL;

		final double svgEndX = boxStartX + boxWidth;

		final String svg = indent1 + "<svg width=\"" + svgEndX + "\" height=\"" + boxHeight + "\">" + NL + indent2 + rect + NL + svgBars + NL
				+ svgLabels + NL + indent1 + "</svg>";

		return svg;
	}

	private static String[] getLineAndLabelColor(final double data, final Double nonZeroMin, final Double nonZeroMax) {
		
		final String color;

		if (data == 0) {
			color = "grey";
		} else if (data == nonZeroMax.doubleValue()) {
			color = "red";
		} else if (data == nonZeroMin.doubleValue()) {
			color = "blue";
		} else {
			color = "green";
		}

		return new String[] { color, color };
	}

	private static long scale(final long limit, final double maxData, final double data) {
		return Math.round(Math.ceil((data * limit) / maxData));
	}
}

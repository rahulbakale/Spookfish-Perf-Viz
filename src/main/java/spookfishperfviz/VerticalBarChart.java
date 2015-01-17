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

	private VerticalBarChart(final double[] data, final String[] labels) {
		this.data = data;
		this.labels = labels;
		this.max = Utils.getMax(data);
	}

	String toSVG(	final int maxBarLength, 
					final double barWidth, 
					final double boxStartX, 
					final String labelFontFamily, 
					final double labelFontSize, 
					final int labelSkipCount, 
					final ColorRampScheme colorRampScheme) {
		
		final double[] data = this.data;
		final int size = data.length;
		final double max = this.max;
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

		final String[] colors = colorRampScheme == null ? null : ColorRampCalculator.getColorMap(data, colorRampScheme);

		final StringBuilder svgBars = new StringBuilder();
		svgBars.append("<g style=\"stroke:grey; stroke-width:").append(barWidth).append("\">").append(NL);

		final StringBuilder svgLabels = new StringBuilder();
		svgLabels.append("<g fill=\"black\" style=\"font-family:").append(labelFontFamily).append(";font-size:").append(labelFontSize).append("px;\">").append(NL);

		int maxXAxisLabelPartCount = Integer.MIN_VALUE;
		double x = barChartStartX;

		for (int i = 0; i < size; i++, x += barWidth) {
			final double d = data[i];
			final long scaledBarLength = scale(maxBarLength, max, d);
			svgBars.append(indent2);
			svgBars.append("<line x1=\"").append(x).append("\"");
			svgBars.append(" y1=\"").append(barStartY).append("\"");
			svgBars.append(" x2=\"").append(x).append("\"");
			svgBars.append(" y2=\"").append(barStartY - scaledBarLength).append("\"");

			final String lineColor = colors == null ? null : colors[i];
			if (lineColor != null) {
				svgBars.append(" style=\"stroke:").append(lineColor).append("\"");
			}
			svgBars.append(">");

			/* TOOLTIP */svgBars.append("<title>").append("Value: ").append(d).append("</title>");

			svgBars.append("</line>");
			svgBars.append(NL);

			final boolean skipLabel = Utils.skipLabel(i, size, labelSkipCount);
			if (skipLabel == false) {

				final String label = Utils.escapeHTMLSpecialChars(labels[i]);

				final MultiSpanSVGText multiSpanSVGText = Utils.createMultiSpanSVGText(label, x, labelStartY, labelFontSize, null);

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

	private static long scale(final long limit, final double maxData, final double data) {
		return Math.round(Math.ceil((data * limit) / maxData));
	}
}

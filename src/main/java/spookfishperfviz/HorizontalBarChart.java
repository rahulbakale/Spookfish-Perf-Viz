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
final class HorizontalBarChart {
	
	private static final int DEFAULT_LIMIT_FOR_TEXT = 80;
	private static final int DEFAULT_LIMIT_FOR_SVG = 500;

	static final class ChartSymbol {
		public static final String FULLWIDTH_HYPHEN = "\uFF0D";
		public static final String UNDERSCORE = "_";
		public static final String DEFAULT = UNDERSCORE;
	}

	static HorizontalBarChart create(final double[] data, final String[] labels) {
		return new HorizontalBarChart(data, labels);
	}

	static HorizontalBarChart create(final long[] data, final String[] labels) {
		return create(Utils.toDoubles(data), labels);
	}

	static HorizontalBarChart create(final int[] data, final String[] labels) {
		return create(Utils.toDoubles(data), labels);
	}

	private final String[] labels;
	private final double[] data;
	private final double max;

	private final Double nonZeroMin;
	private final Double nonZeroMax;

	private HorizontalBarChart(final double[] data, final String[] labels) {
		
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

	@Override
	public String toString() {
		return toString(DEFAULT_LIMIT_FOR_TEXT, ChartSymbol.DEFAULT);
	}

	String toString(final long limit, final String mark) {
		final double[] data = this.data;
		final int size = data.length;
		final double max = this.max;
		final CharSequence[] labels = this.labels;

		final int maxLabelLength = getMaxLabelLength(labels);

		final String NL = System.lineSeparator();

		final StringBuilder buf = new StringBuilder();

		for (int i = 0; i < size; i++) {
			final String label = Utils.getPaddedLabel(labels[i], maxLabelLength, false);

			buf.append(label).append("  ");

			final long scaled = scale(limit, max, data[i]);
			for (int k = 0; k < scaled; k++) {
				buf.append(mark);
			}

			buf.append(NL);
		}

		return buf.toString();
	}

	String toSVG(final boolean wrapInHtmlBody, final boolean useColorCoding) {
		return toSVG(DEFAULT_LIMIT_FOR_SVG, wrapInHtmlBody, useColorCoding);
	}

	private String toSVG(final int maxLineLength, final boolean wrapInHtmlBody, final boolean useColorCoding) {
		return wrapInHtmlBody ? toSVGHtml(maxLineLength, useColorCoding) : toSVG(maxLineLength, useColorCoding);
	}

	private String toSVGHtml(final int maxLineLength, final boolean useColorCoding) {
		final String NL = System.lineSeparator();
		return "<!DOCTYPE html>" + NL + "<html>" + NL + "  <body>" + NL + toSVG(maxLineLength, useColorCoding) + NL + "  </body>" + NL + "</html>";
	}

	private String toSVG(final int maxLineLength, final boolean useColorCoding) {
		final double[] data = this.data;
		final int size = data.length;
		final Double nonZeroMin = this.nonZeroMin;
		final double max = this.max;
		final Double nonZeroMax = this.nonZeroMax;
		final String[] labels = this.labels;

		final String NL = System.lineSeparator();

		final int LABEL_START_X = SVGConstants.LEFT_RIGHT_MARGIN;

		final int SPACE_BETWEEN_LABEL_AND_LINE = 10;

		final int maxLabelLength = getMaxLabelLength(labels);
		final double maxLabelWidth = maxLabelLength * SVGConstants.MONOSPACE_FONT_WIDTH;
		final double xLineStart = LABEL_START_X + maxLabelWidth + SPACE_BETWEEN_LABEL_AND_LINE;
		final double boxWidth = xLineStart + maxLineLength + SVGConstants.LEFT_RIGHT_MARGIN;
		final int boxHeight = (size + 1) * SVGConstants.LINE_GAP;

		final String indent1 = "  ";
		final String indent2 = "    ";

		final StringBuilder svgLines = new StringBuilder();
		svgLines.append("<g style=\"stroke:grey; stroke-width:5\">").append(NL);

		final StringBuilder svgLabels = new StringBuilder();
		svgLabels.append("<g fill=\"black\" style=\"font-family:").append(SVGConstants.MONOSPACE_FONT_FAMILY).append(";font-size:")
				.append(SVGConstants.MONOSPACE_FONT_SIZE).append("px;\">").append(NL);

		for (int i = 0, y1 = SVGConstants.LINE_GAP; i < size; i++, y1 += SVGConstants.LINE_GAP) {
			final double d = data[i];

			final String[] lineAndLabelColor = useColorCoding ? getLineAndLabelColor(d, nonZeroMin, nonZeroMax) : null;
			final String lineColor = lineAndLabelColor == null ? null : lineAndLabelColor[0];
			final String labelColor = lineAndLabelColor == null ? null : lineAndLabelColor[1];

			final long scaledLineLength = scale(maxLineLength, max, d);
			final String label = Utils.getPaddedLabel(labels[i], maxLabelLength, true); 
																						
			svgLines.append(indent2)
					.append("<line x1=\"").append(xLineStart).append("\"")
					.append(" y1=\"").append(y1).append("\"")
					.append(" x2=\"").append((xLineStart + scaledLineLength)).append("\"")
					.append(" y2=\"").append(y1).append("\"");
			
			if (lineColor != null) {
				svgLines.append(" style=\"stroke:").append(lineColor).append("\"");
			}
			
			svgLines.append("/>").append(NL);

			svgLabels.append(indent2)
					.append("<text x=\"").append(LABEL_START_X).append("\"")
					.append(" y=\"").append(y1).append("\"");
			
			if (labelColor != null) {
				svgLabels.append(" fill=\"").append(labelColor).append("\"");
			}
			svgLabels.append(">").append(label).append("</text>").append(NL);
		}

		svgLines.append("</g>");
		svgLabels.append("</g>");

		final String rect = "<rect width=\"" + boxWidth + "\" height=\"" + boxHeight + "\" style=\"fill:white;stroke:black;stroke-width:1\"/>" + NL;

		final String svg = indent1 + "<svg width=\"" + boxWidth + "\" height=\"" + boxHeight + "\">" + NL + 
							indent2 + rect + NL + 
							svgLines + NL + 
							svgLabels + NL + 
							indent1 + "</svg>";

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

	private static int getMaxLabelLength(final CharSequence[] c) {
		int padding = -1;
		for (final CharSequence e : c) {
			final int len = e == null ? 4 : e.length();

			if (len > padding) {
				padding = len;
			}
		}
		return padding;
	}
}

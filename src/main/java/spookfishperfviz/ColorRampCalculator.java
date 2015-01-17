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
 * @since Jan, 2015
 */
final class ColorRampCalculator {

	static String[] getColorMap(final long[] values, final ColorRampScheme colorScheme) {
		return getColorMap(Utils.toDoubles(values), colorScheme);
	}

	static String[] getColorMap(final double[] values, final ColorRampScheme colorScheme) {

		final double[] minMax = Utils.minMax(values);
		final double min = minMax[0];
		final double max = minMax[1];

		final ColorRampCalculator colorCalculator = new ColorRampCalculator(min, max, colorScheme);

		final int size = values.length;
		final String[] colorMap = new String[size];

		for (int i = 0; i < size; i++) {
			colorMap[i] = colorCalculator.getColor(values[i]);
		}

		return colorMap;
	}
	
	private final String[] colors;
	private final double binSize;
	private final String colorForZeroVal;
	private final double minVal;
	private final double maxVal;

	private ColorRampCalculator(final double minVal, final double maxVal, final ColorRampScheme colorScheme) {

		this.colors = colorScheme.getForegroundColors();
		this.binSize = (maxVal - minVal) / this.colors.length; // ((max - 1) + 1) / this.colors.length;
		this.colorForZeroVal = colorScheme.getBackgroundColor();
		this.minVal = minVal;
		this.maxVal = maxVal;
	}

	private String getColor(final double val) {

		final String color;

		if (val == 0) {
			color = this.colorForZeroVal;
		} else {

			final String[] clrs = this.colors;

			// final int binNumber = Utils.safeToInt(Math.floor((val - 1) / this.binSize));

			final int binNumber = (val == this.maxVal) ? (clrs.length - 1) : Utils.safeToInt(Math.floor((val - this.minVal) / this.binSize));

			color = clrs[binNumber];
		}

		return color;
	}

}

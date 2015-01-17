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

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Rahul Bakale
 * @since Dec, 2014
 */
public enum ColorRampScheme {

	YELLOW_OR_RED(ColorPalette.COLOR_BREWER_YELLOW_OR_RED, 9),
	BLUE_GREEN(ColorPalette.COLOR_BREWER_BLUE_GREEN, 9),
	BLUE_PURPLE(ColorPalette.COLOR_BREWER_BLUE_PURPLE, 9),
	PURPLE_BLUE(ColorPalette.COLOR_BREWER_PURPLE_BLUE, 9),
	PURPLE_RED(ColorPalette.COLOR_BREWER_PURPLE_RED, 9),
	BLUE(ColorPalette.COLOR_BREWER_BLUE, 9),
	GREEN(ColorPalette.COLOR_BREWER_GREEN, 9),
	GREY(ColorPalette.COLOR_BREWER_GREY, 8),
	PURPLE(ColorPalette.COLOR_BREWER_PURPLE, 9),
	RED(ColorPalette.COLOR_BREWER_RED, 9),
	RED_2(ColorPalette.INHOUSE_RED_1, 16),
	RED_3(ColorPalette.INHOUSE_RED_2, 19);

	static final ColorRampScheme DEFAULT = GREY;

	private static final String WHITE = "#ffffff";

	private final String[] foregroundColors;
	private final String backgroundColor;

	private ColorRampScheme(final ColorPalette palette, final int nDataClasses) {
		this(palette, nDataClasses, WHITE);
	}

	private ColorRampScheme(final ColorPalette palette, final int nDataClasses, final String backgroundColor) {
		this(palette.getColors(), nDataClasses, backgroundColor);
	}

	private ColorRampScheme(final String[] foregroundColorSet, final int nDataClasses, final String backgroundColor) {
		this(Arrays.copyOfRange(foregroundColorSet, (foregroundColorSet.length - nDataClasses), foregroundColorSet.length), backgroundColor);
	}

	private ColorRampScheme(final String[] foregroundColors, final String backgroundColor) {

		for (String fc : foregroundColors) {
			if (Objects.equals(fc, backgroundColor)) {
				throw new RuntimeException("Invalid color scheme: One of the foreground colors is same as the background color.");
			}
		}

		this.foregroundColors = foregroundColors;
		this.backgroundColor = backgroundColor;
	}

	String[] getForegroundColors() {
		return this.foregroundColors;
	}

	String getBackgroundColor() {
		return this.backgroundColor;
	}

	/**
	 * @see <a href="http://colorbrewer2.org/">Sequential color schemes from Color Brewer</a>
	 */
	private static enum ColorPalette {

		COLOR_BREWER_RED	(new String[] {"#fff5f0", "#fee0d2", "#fcbba1", "#fc9272", "#fb6a4a", "#ef3b2c", "#cb181d", "#a50f15", "#67000d"}),
		COLOR_BREWER_PURPLE	(new String[] {"#fcfbfd", "#efedf5", "#dadaeb", "#bcbddc", "#9e9ac8", "#807dba", "#6a51a3", "#54278f", "#3f007d"}),
		COLOR_BREWER_GREY	(new String[] {"#ffffff", "#f0f0f0", "#d9d9d9", "#bdbdbd", "#969696", "#737373", "#525252", "#252525", "#000000"}),
		COLOR_BREWER_GREEN	(new String[] {"#f7fcf5", "#e5f5e0", "#c7e9c0", "#a1d99b", "#74c476", "#41ab5d", "#238b45", "#006d2c", "#00441b"}),
		COLOR_BREWER_BLUE	(new String[] {"#f7fbff", "#deebf7", "#c6dbef", "#9ecae1", "#6baed6", "#4292c6", "#2171b5", "#08519c", "#08306b"}),
		COLOR_BREWER_PURPLE_RED		(new String[] {"#f7f4f9", "#e7e1ef", "#d4b9da", "#c994c7", "#df65b0", "#e7298a", "#ce1256", "#980043", "#67001f"}),
		COLOR_BREWER_PURPLE_BLUE	(new String[] {"#fff7fb", "#ece7f2", "#d0d1e6", "#a6bddb", "#74a9cf", "#3690c0", "#0570b0", "#045a8d", "#023858"}),
		COLOR_BREWER_BLUE_PURPLE	(new String[] {"#f7fcfd", "#e0ecf4", "#bfd3e6", "#9ebcda", "#8c96c6", "#8c6bb1", "#88419d", "#810f7c", "#4d004b"}),
		COLOR_BREWER_BLUE_GREEN		(new String[] {"#f7fcfd", "#e5f5f9", "#ccece6", "#99d8c9", "#66c2a4", "#41ae76", "#238b45", "#006d2c", "#00441b"}),
		COLOR_BREWER_YELLOW_OR_RED	(new String[] {"#ffffcc", "#ffeda0", "#fed976", "#feb24c", "#fd8d3c", "#fc4e2a", "#e31a1c", "#bd0026", "#800026"}),
		
		INHOUSE_RED_1(new String[] {"#FFE6E6", "#FFCCCC", "#FFB2B2", "#FF9999", "#FF8080", "#FF6666", "#FF4D4D", "#FF3333", "#FF1919", 
							   		"#FF0000", "#E60000", "#CC0000", "#B20000", "#990000", "#800000", "#660000"}),
		
		INHOUSE_RED_2(new String[] {"#FFE5E5", "#FFCCCC", "#FFB2B2", "#FF9999", "#FF7F7F", "#FF6666", "#FF4C4C", "#FF3232", "#FF1919",
				 			   		"#FF0000", "#E50000", "#CC0000", "#B20000", "#990000", "#7F0000", "#660000", "#4C0000", "#330000", 
				 			   		"#190000"});

		private final String[] colors;

		ColorPalette(String[] colors) {
			this.colors = colors;
		}

		String[] getColors() {
			return this.colors;
		}
	}
}

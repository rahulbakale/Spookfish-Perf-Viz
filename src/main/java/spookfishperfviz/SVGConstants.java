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
final class SVGConstants {
	
	static final int LEFT_RIGHT_MARGIN = 10;
	static final int TOP_DOWN_MARGIN = 10;
	static final int LINE_GAP = 18;

	static final String FONT_FAMILY = "Courier New, Courier, monospace";
	static final double FONT_SIZE = LINE_GAP - 2;
	static final double FONT_WIDTH_TO_FONT_SIZE_RATIO = 0.625; // specific to Courier New font.
	static final double FONT_WIDTH = FONT_SIZE * FONT_WIDTH_TO_FONT_SIZE_RATIO;

	static final String SERIF_FONT_FAMILY = "\"Times New Roman\", Times, Serif";
	static final double SERIF_FONT_SIZE = 16;

	private SVGConstants() {
		//
	}
}

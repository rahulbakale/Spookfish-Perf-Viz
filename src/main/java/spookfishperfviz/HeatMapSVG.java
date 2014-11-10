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
final class HeatMapSVG {
	
	private final String svg;
	private final int xAxisLabelSkipCount;
	private final double heatMapBoxStartX;

	// TODO - check if this should be a double
	private final double heatMapSingleAreaWidth;

	HeatMapSVG(final String svg, final int xAxisLabelSkipCount, final double heatMapBoxStartX, final double heatMapSingleAreaWidth) {
		this.svg = svg;
		this.xAxisLabelSkipCount = xAxisLabelSkipCount;
		this.heatMapBoxStartX = heatMapBoxStartX;
		this.heatMapSingleAreaWidth = heatMapSingleAreaWidth;
	}

	String getSvg() {
		return this.svg;
	}

	int getXAxisLabelSkipCount() {
		return this.xAxisLabelSkipCount;
	}

	double getHeatMapBoxStartX() {
		return this.heatMapBoxStartX;
	}

	double getHeatMapSingleAreaWidth() {
		return this.heatMapSingleAreaWidth;
	}

}

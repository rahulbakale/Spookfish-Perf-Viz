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
final class MultiSpanSVGText {
	
	private final CharSequence svg;
	private final int spanCount;

	MultiSpanSVGText(final CharSequence svg, final int spanCount) {
		this.svg = svg;
		this.spanCount = spanCount;
	}

	CharSequence getSvg() {
		return this.svg;
	}

	int getSpanCount() {
		return this.spanCount;
	}
}

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

import java.util.Collection;
import java.util.Set;

/**
 * @author Rahul Bakale
 * @since Nov, 2014
 */
abstract class Histogram<C extends Comparable<C>> {

	Histogram() {
		//
	}

	static Histogram<Double> create(final double[] data, final double[] intervalPoints) {
		return Histogram2.newInstance(data, intervalPoints, false);
	}

	static Histogram<Double> create(final double[] data, final int nIntervalPoints) {
		return Histogram2.newInstance(data, nIntervalPoints, false);
	}

	static <T extends Comparable<T>> Histogram<T> create(final Collection<T> data, final Set<T> intervalPoints) {
		return Histogram2.newInstance(data, intervalPoints, false);
	}

	abstract String toSVG(Function<C, String> dataPointFormatter, boolean wrapInHtmlBody, ColorRampScheme colorRampScheme);

	abstract String toString(Function<C, String> dataPointFormatter, int maxHeight, String mark);
}

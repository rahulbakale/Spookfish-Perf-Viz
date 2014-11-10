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
final class Percentiles {
	
	private final double[] keys;
	private final double[] values;

	Percentiles(final double[] keys, final double[] values) {
		
		if (keys.length != values.length) {
			throw new IllegalArgumentException();
		}

		this.keys = keys;
		this.values = values;
	}

	public String toSVG(final boolean wrapInHtmlBody) {
		return toBarChart().toSVG(wrapInHtmlBody, false);
	}

	@Override
	public String toString() {
		return toBarChart().toString();
	}

	private HorizontalBarChart toBarChart() {
		
		final int n = this.keys.length;

		final double[] keys = this.keys;
		final double[] values = this.values;

		final String[] keyStrs = new String[n];
		final String[] valueStrs = new String[n];

		final double[] valuesReversed = new double[n];

		int kPadding = -1, vPadding = -1;

		for (int i = 0, k = n - 1; i < n; i++, k--) {
			{
				final String keyStr = String.valueOf(keys[i]);

				final int kLen = keyStr.length();
				if (kLen > kPadding) {
					kPadding = kLen;
				}

				keyStrs[k] = keyStr;
			}

			{
				final double value = values[i];
				final String valueStr = String.format("%f", Double.valueOf(value));

				final int vLen = valueStr.length();
				if (vLen > vPadding) {
					vPadding = vLen;
				}

				valuesReversed[k] = value;
				valueStrs[k] = valueStr;
			}
		}

		final String[] labels = new String[n];
		for (int i = 0; i < n; i++) {
			labels[i] = String.format("%" + kPadding + "s    %" + vPadding + "s", keyStrs[i], valueStrs[i]);
		}

		return HorizontalBarChart.create(valuesReversed, labels);
	}
}
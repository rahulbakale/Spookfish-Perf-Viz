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

import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Rahul Bakale
 * @since Nov, 2014
 */
public final class SimpleRegexBasedRecordParser extends RecordParser {

	public static enum NamedGroup {
		Latency, EventName, Timestamp;

		public String getGroup(final String regex) {
			return "(?<" + this.name() + ">" + regex + ")";
		}
	}

	public static RecordParser create(final String ignorePattern, final String parsePattern, final String timestampPattern) {
		return new SimpleRegexBasedRecordParser(ignorePattern, parsePattern, timestampPattern);
	}

	private final String ignorePattern;
	private final Pattern parsePatternObj;
	private final SimpleDateFormat timestampDateFormat;

	private SimpleRegexBasedRecordParser(final String ignorePattern, final String parsePattern, final String timestampPattern) {
		this.ignorePattern = ignorePattern;
		this.parsePatternObj = Pattern.compile(parsePattern);
		this.timestampDateFormat = new SimpleDateFormat(timestampPattern);
	}

	@Override
	public final boolean isIgnore(final String line) {
		return line.matches(this.ignorePattern);
	}

	@Override
	public final Record parse(final String line) {

		try {
			final Matcher matcher = this.parsePatternObj.matcher(line);

			if (!matcher.matches()) {
				throw new RuntimeException("Pattern does not match");
			}

			final long timestamp = this.timestampDateFormat.parse(matcher.group(NamedGroup.Timestamp.name())).getTime();
			final String eventName = matcher.group(NamedGroup.EventName.name());
			final double latency = Double.parseDouble(matcher.group(NamedGroup.Latency.name()));

			return new Record(eventName, timestamp, latency);

		} catch (final Exception e) {
			throw new RuntimeException("Error while parsing line <" + line + ">", e);
		}
	}
}

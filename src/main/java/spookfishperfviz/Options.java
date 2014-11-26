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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Rahul Bakale
 * @since Nov, 2014
 */
final class Options {

	static Options create(final String[] args) throws BadOptionsException {

		final Options options = new Options();

		if ((args != null) && (args.length > 0)) {

			for (int i = 0; i < args.length; i++) {

				final String optName;
				{
					final String name = args[i];

					if (name == null) {
						throw BadOptionsException.badOptionName("Option's name is null", args, i);
					}

					final String trimmedName = name.trim();

					if (!trimmedName.startsWith("-")) {
						throw BadOptionsException.badOptionName("Option's name does not start with '-'", args, i);
					}

					optName = trimmedName.substring(1);

					if (optName.isEmpty()) {
						throw BadOptionsException.badOptionName("Option's name is invalid", args, i);
					}
				}

				final String optValue;
				{
					i++;

					if (i >= args.length) {
						throw BadOptionsException.valueNotSpecified(optName);
					}

					optValue = args[i];
				}

				options.add(optName, optValue);
			}
		}

		return options;
	}

	private static <T> T parse(final String optionName, final String value, final Class<T> valueType) throws BadOptionsException {
		try {
			return Utils.parseType(valueType, value);
		} catch (final ParseException e) {
			throw BadOptionsException.illegalValue(optionName, e.getMessage(), e);
		}
	}

	private final Map<String, Optional<String>> options;

	private Options() {
		this.options = new HashMap<>();
	}

	private void add(final String name, final String value) {
		this.options.put(name, Optional.of(value));
	}

	<T> T getMandatory(final String optionName, final Class<T> valueType) throws BadOptionsException {

		final String value = getMandatory(optionName);
		return parse(optionName, value, valueType);
	}

	<T> T getOptional(final String optionName, final Class<T> valueType, final T defaultValue) throws BadOptionsException {

		final Optional<String> optional = getOptional(optionName);
		return optional.hasValue() ? parse(optionName, optional.get(), valueType) : defaultValue;
	}

	private String getMandatory(final String optionName) throws BadOptionsException {

		final Optional<String> optional = getOptional(optionName);

		if (optional.hasValue()) {
			return optional.get();
		}

		throw BadOptionsException.illegalValue(optionName, "Value is null.", null);
	}

	private Optional<String> getOptional(final String optionName) throws BadOptionsException {

		final Optional<String> optional = this.options.get(optionName);

		if (optional == null) {
			throw BadOptionsException.noSuchOption(optionName);
		}

		return optional;
	}
}

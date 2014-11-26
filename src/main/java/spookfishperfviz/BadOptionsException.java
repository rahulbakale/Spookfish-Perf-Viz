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
final class BadOptionsException extends Exception {

	private static final long serialVersionUID = 7736583174452576492L;

	static BadOptionsException badOptionName(final String message, final String[] args, final int argNum) {
		return new BadOptionsException(message + ": args[" + argNum + "] = <" + args[argNum] + ">");
	}

	static BadOptionsException noSuchOption(final String optionName) {
		return new BadOptionsException("Option <" + optionName + "> does not exist");
	}

	static BadOptionsException valueNotSpecified(final String optionName) {
		return new BadOptionsException("Value is not specified for option <" + optionName + ">");
	}

	static BadOptionsException illegalValue(final String optionName, final String message, final Throwable cause) {
		return new BadOptionsException("Option <" + optionName + "> has illegal value. " + message, cause);
	}

	private BadOptionsException(final String message) {
		this(message, null);
	}

	private BadOptionsException(final String message, final Throwable cause) {
		super(message, cause);
	}
}

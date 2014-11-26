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
public final class Main {

	public static void main(final String[] args) throws Exception {

		final Options options = Options.create(args);
		final String task = options.getMandatory("task", String.class);

		switch (task) {

		case "GenerateLatencyReport":
			LatencyReportGenerator.run(options);
			break;

		default:
			throw BadOptionsException.illegalValue("task", "<" + task + ">", null);
		}
	}
}

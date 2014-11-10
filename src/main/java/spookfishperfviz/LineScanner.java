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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.Scanner;

/**
 * Usage: <code>
 *  <br>for(String line : new LineScanner(multilineText)
 *  <br>{
 *  <br>    //process line
 *  <br>}
 * </code>
 * 
 * @author Rahul Bakale
 * @since Nov, 2014
 */
final class LineScanner implements Iterable<String>, AutoCloseable {
	
	private static void makeLineScanner(final Scanner scanner) {
		scanner.useDelimiter("\r\n|[\n\r\u2028\u2029\u0085]");
	}

	private final Scanner scanner;

	public LineScanner(final String text) {
		final Scanner s = new Scanner(text);
		makeLineScanner(s);
		this.scanner = s;
	}

	public LineScanner(final File file) throws FileNotFoundException {
		final Scanner s = new Scanner(file);
		makeLineScanner(s);
		this.scanner = s;
	}

	@Override
	public Iterator<String> iterator() {
		return this.scanner;
	}

	@Override
	public void close() {
		this.scanner.close();
	}
}
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
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * @author Rahul Bakale
 * @since Nov, 2014
 */
public abstract class FileRecordIterator implements Iterator<Record>, AutoCloseable {
	
	private final Scanner scanner;
	private Record bufferedRecord;

	protected FileRecordIterator(final String filePath) throws FileNotFoundException {

		final Scanner s = new Scanner(new File(filePath));
		s.useDelimiter("\r\n|[\n\r\u2028\u2029\u0085]");

		this.scanner = s;
	}

	protected abstract boolean ignore(String line);

	protected abstract Record parse(String line);

	@Override
	public final boolean hasNext() {
		final boolean hasNext;

		if (this.bufferedRecord != null) {
			hasNext = true;
		} else {
			boolean noSuchElement = false;
			try {
				this.bufferedRecord = readNextRecord();
			} catch (final NoSuchElementException e) {
				noSuchElement = true;
			}

			hasNext = !noSuchElement;
		}

		return hasNext;
	}

	@Override
	public final Record next() {
		final Record next;

		if (this.bufferedRecord != null) {
			next = this.bufferedRecord;
			this.bufferedRecord = null;
		} else {
			next = readNextRecord();
		}

		return next;
	}

	/**
	 * Must never return null
	 */
	private Record readNextRecord() {
		final String line = readNextLine();
		final Record record = parse(line);

		assert record != null;

		return record;
	}

	private String readNextLine() {
		final Scanner s = this.scanner;

		while (true) {
			final String line = s.next();

			if (!ignore(line)) {
				return line;
			}
		}
	}

	@Override
	public final void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void close() {
		this.scanner.close();
		this.bufferedRecord = null;
	}

}

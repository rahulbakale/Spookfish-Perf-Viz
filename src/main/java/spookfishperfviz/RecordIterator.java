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

import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * @author Rahul Bakale
 * @since Nov, 2014
 */
final class RecordIterator implements Iterator<Record>, AutoCloseable {
	
	/*private static RecordIterator createForFileSource(final File file, final RecordParser parser) throws IOException {

		@SuppressWarnings("resource")
		final FileReader fr = new FileReader(file);

		try {
			@SuppressWarnings("resource")
			final Reader br = new BufferedReader(fr);

			try {
				final RecordIterator itr = create(br, parser);
				return itr;

			} catch (Throwable t) {

				try {
					br.close();
				} catch (Throwable e) {
					t.addSuppressed(e);
				}

				throw t;
			}
		} catch (Throwable t) {

			try {
				fr.close();
			} catch (Throwable e) {
				t.addSuppressed(e);
			}

			throw t;
		}
	}*/
	
	static RecordIterator create(final Reader source, final RecordParser parser) {
		return new RecordIterator(source, parser);
	}

	private final Scanner scanner;
	private final RecordParser parser;
	private Record bufferedRecord;

	private RecordIterator(final Reader source, final RecordParser parser) {

		final Scanner s = new Scanner(source);
		s.useDelimiter("\r\n|[\n\r\u2028\u2029\u0085]");

		this.scanner = s;
		this.parser = parser;
	}

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

	@Override
	public final void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void close() {
		this.scanner.close();
		this.bufferedRecord = null;
	}

	/**
	 * Must never return null
	 */
	private Record readNextRecord() {
		final String line = readNextLine();
		final Record record = this.parser.parse(line);

		assert record != null;

		return record;
	}

	private String readNextLine() {
		final Scanner s = this.scanner;

		while (true) {
			final String line = s.next();

			if (!this.parser.isIgnore(line)) {
				return line;
			}
		}
	}
}

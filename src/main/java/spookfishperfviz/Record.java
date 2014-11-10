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
public final class Record {
	
	private final String eventType;
	private final long timestamp;
	private final double latency;

	public Record(final String eventType, final long timestamp, final double latency) {
		this.eventType = eventType;
		this.timestamp = timestamp;
		this.latency = latency;
	}

	public String getEventType() {
		return this.eventType;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public double getLatency() {
		return this.latency;
	}

}

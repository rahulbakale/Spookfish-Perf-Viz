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

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Rahul Bakale
 * @since Nov, 2014
 */
public final class LatencyReportGenerator {
	
	private LatencyReportGenerator() {
		//
	}

	private static final AtomicInteger uniquifier = new AtomicInteger();

	static void run(final Options options) throws Exception {

		final String ignorePattern = options.getOptional("ignorePattern", String.class, null);
		final String parsePattern = options.getMandatory("parsePattern", String.class);
		final String timestampPattern = options.getMandatory("timestampPattern", String.class);
		final RecordParser parser = SimpleRegexBasedRecordParser.create(ignorePattern, parsePattern, timestampPattern);

		final TimeUnit latencyUnit = options.getMandatory("latencyUnit", TimeUnit.class);

		final double[] histogramIntervalPoints = options.getMandatory("histogramIntervalPoints", double[].class);
		final double[] percentilePoints = options.getMandatory("percentilePoints", double[].class);
		final Integer heatMapMaxIntervalPoints = options.getOptional("heatMapMaxIntervalPoints", Integer.class, null);

		final ColorRampScheme colorRampScheme = options.getOptional("colorRampScheme", ColorRampScheme.class, ColorRampScheme.DEFAULT);

		final String inFile = options.getMandatory("inFile", String.class);
		final String outFile = options.getMandatory("outFile", String.class);

		final int heatMapSingleAreaWidth = 20;

		final Path path;

		try (final Reader fr = new FileReader(inFile); final Reader source = new BufferedReader(fr);) {
			
			path = generateReport(source, parser, latencyUnit, histogramIntervalPoints, percentilePoints, 
									heatMapMaxIntervalPoints, heatMapSingleAreaWidth, colorRampScheme, outFile);
		}

		System.out.println("Report generated at <" + path + ">");
	}

	public static Path generateReport(	final Reader source,
										final RecordParser parser, 
										final TimeUnit latencyUnit,
										final double[] intervalPointsForLatencyHistogram, 
										final double[] percentileKeys, 
										final Integer maxIntervalPointsForLatencyDensity,
										final double heatMapSingleAreaWidth, 
										final ColorRampScheme colorRampScheme, 
										final String outputFilePath) throws IOException {
		
		final LatencyStatsToHtmlFunc latencyStatsToHtmlFunc = new LatencyStatsToHtmlFunc() {
			@Override
			public String[] toHtml(final LatencyStats stats) {

				final TimeSeriesLatencyDensity density = TimeSeriesLatencyDensity.create(stats.getLatencies(), stats.getTimestamps(), maxIntervalPointsForLatencyDensity);
				return stats.toHtml(intervalPointsForLatencyHistogram, percentileKeys, density, heatMapSingleAreaWidth, colorRampScheme);
			}
		};

		return generateReport(source, parser, latencyUnit, latencyStatsToHtmlFunc, outputFilePath);
	}

	public static Path generateReport(	final Reader source,
										final RecordParser parser, 
										final TimeUnit latencyUnit,
										final double[] intervalPointsForLatencyHistogram, 
										final double[] percentileKeys, 
										final double minIntervalPointForLatencyDensity, 
										final double maxIntervalPointForLatencyDensity, 
										final Integer maxIntervalPointsForLatencyDensity, 
										final double heatMapSingleAreaWidth, 
										final ColorRampScheme colorRampScheme, 
										final String outputFilePath) throws IOException {
		
		final LatencyStatsToHtmlFunc latencyStatsToHtmlFunc = new LatencyStatsToHtmlFunc() {
			@Override
			public String[] toHtml(final LatencyStats stats) {

				final TimeSeriesLatencyDensity density = TimeSeriesLatencyDensity.create(stats.getLatencies(), stats.getTimestamps(), minIntervalPointForLatencyDensity, maxIntervalPointForLatencyDensity, maxIntervalPointsForLatencyDensity);
				return stats.toHtml(intervalPointsForLatencyHistogram, percentileKeys, density, heatMapSingleAreaWidth, colorRampScheme);
			}
		};

		return generateReport(source, parser, latencyUnit, latencyStatsToHtmlFunc, outputFilePath);
	}

	private static Path generateReport(	final Reader source,
										final RecordParser parser, 
										final TimeUnit latencyUnit, 
										final LatencyStatsToHtmlFunc latencyStatsToHtmlFunc, 
										final String outputFilePath) throws IOException {

		try (RecordIterator recordIterator = RecordIterator.create(source, parser);)
		{
			final Path reportFilePath;

			if (false) {

				// TODO - enable after adding feature to read from raw file if the
				// contents of input file have not changed since last read.

				final File rawFile = createRawFile(recordIterator);
				reportFilePath = generateReport(rawFile, latencyUnit, latencyStatsToHtmlFunc, outputFilePath);

			} else {
				final Map<String, List<TimestampAndLatency>> data = new TreeMap<>();

				while (recordIterator.hasNext()) {
					final Record record = recordIterator.next();
					addRecord(record.getEventName(), record.getTimestamp(), record.getLatency(), data);
				}

				reportFilePath = generateReport(data, latencyUnit, latencyStatsToHtmlFunc, outputFilePath);
			}

			return reportFilePath;
		}
	}

	private static Path generateReport(	final File rawDataFile,
										final TimeUnit latencyUnit, 
										final LatencyStatsToHtmlFunc latencyStatsToHtmlFunc, 
										final String outputFilePath) throws IOException, FileNotFoundException {
		
		return generateReport(parseRawFile(rawDataFile), latencyUnit, latencyStatsToHtmlFunc, outputFilePath);
	}

	private static Path generateReport(	final Map<String, List<TimestampAndLatency>> data, 
										final TimeUnit latencyUnit, 
										final LatencyStatsToHtmlFunc latencyStatsToHtmlFunc, 
										final String reportFilePath) throws IOException {

		final String NL = System.lineSeparator();

		final List<TimestampAndLatency> latenciesSuperSet = new ArrayList<>();

		final StringBuilder linksHtml = new StringBuilder();
		final StringBuilder contentsHtml = new StringBuilder();

		linksHtml.append("<table style=\"border:1px solid black; font-size: 14px;\">");
		linksHtml.append("<tr>").append(NL);
		linksHtml.append("<th>Event type</th>").append(NL);
		linksHtml.append("<th>Event count</th>").append(NL);
		linksHtml.append("<th>").append(createHtmlTextWithLink("Median", "http://en.wikipedia.org/wiki/Median")).append(" latency").append("</th>").append(NL);
		linksHtml.append("<th>").append(createHtmlTextWithLink("Mean", "http://en.wikipedia.org/wiki/Mean")).append(" latency").append("</th>").append(NL);
		linksHtml.append("<th>Minimum latency</th>").append(NL);
		linksHtml.append("<th>Maximum latency</th>").append(NL);
		linksHtml.append("<th>Summary</th>").append(NL);
		linksHtml.append(createHtmlColumnHeaderWithLink("Histogram", "http://en.wikipedia.org/wiki/Histogram")).append(NL);
		linksHtml.append(createHtmlColumnHeaderWithLink("Percentiles", "http://en.wikipedia.org/wiki/Percentile")).append(NL);
		linksHtml.append(createHtmlColumnHeaderWithLink("Heat Map", "http://en.wikipedia.org/wiki/Heat_map")).append(NL);
		linksHtml.append(createHtmlColumnHeaderWithLink("Std. Deviation", "http://en.wikipedia.org/wiki/Standard_deviation")).append(NL);
		linksHtml.append(createHtmlColumnHeaderWithLink("Variance", "http://en.wikipedia.org/wiki/Variance")).append(NL);
		linksHtml.append(createHtmlColumnHeaderWithLink("Skewness", "http://en.wikipedia.org/wiki/Skewness")).append(NL);
		linksHtml.append(createHtmlColumnHeaderWithLink("Kurtosis", "http://en.wikipedia.org/wiki/Kurtosis")).append(NL);
		linksHtml.append("</tr>").append(NL);

		final TreeMap<Double, String> linkHtmlsSortedByMedian = new TreeMap<>();

		for (final Entry<String, List<TimestampAndLatency>> entry : data.entrySet()) {
			final String eventType = entry.getKey();
			final List<TimestampAndLatency> latencies = entry.getValue();

			final Stats stats = Stats.create(latencies, latencyUnit, eventType);
			final LatencyStats latencyStats = stats.getLatencyStats();
			final String[] h = latencyStatsToHtmlFunc.toHtml(latencyStats);

			contentsHtml.append(h[1]).append(NL);
			contentsHtml.append("<br/><br/>").append(NL);
			
			linkHtmlsSortedByMedian.put(Double.valueOf(latencyStats.getMedian()), h[0]);

			latenciesSuperSet.addAll(latencies);
		}

		{
			final Stats stats = Stats.create(latenciesSuperSet, latencyUnit, "All APIs combined");
			final LatencyStats latencyStats = stats.getLatencyStats();
			final String[] h = latencyStatsToHtmlFunc.toHtml(latencyStats);

			contentsHtml.append(h[1]).append(NL);
			contentsHtml.append("<br/><br/>");
			
			linksHtml.append(h[0]).append(NL);
		}

		for (final String linkHtml : linkHtmlsSortedByMedian.descendingMap().values()) {
			linksHtml.append(linkHtml).append(NL);
		}

		linksHtml.append("</table>");
		
		final String advertisementHtml = "This report was generated by <a href=\"https://github.com/rahulbakale/Spookfish-Perf-Viz\" target=\"_blank\">Spookfish-Perf-Viz, a free and open-source tool</a>, developed by Rahul Bakale.<br></br>";

		final StringBuilder perfStatsHtml = new StringBuilder();
		perfStatsHtml.append("<!DOCTYPE html>").append(NL);
		perfStatsHtml.append("<html>").append(NL);
		perfStatsHtml.append("<body>").append(NL);
		perfStatsHtml.append(advertisementHtml).append(NL);
		perfStatsHtml.append(linksHtml).append(NL);
		perfStatsHtml.append(contentsHtml).append(NL);
		perfStatsHtml.append("</html>").append(NL);
		perfStatsHtml.append("</body>").append(NL);

		return Files.write(Paths.get(reportFilePath), perfStatsHtml.toString().getBytes(), WRITE, CREATE, TRUNCATE_EXISTING);

		/*
		 * final LatencyStats statsWithoutOutliers = removeOutliers(stats, 3);
		 * statsWithoutOutliers.print(latencyIntervalPoints, percentileKeys,
		 * System.out);
		 */
	}

	private static Map<String, List<TimestampAndLatency>> parseRawFile(final File rawFile) throws IOException, FileNotFoundException {
		final Map<String, List<TimestampAndLatency>> data = new TreeMap<>();

		try (final FileInputStream fis = new FileInputStream(rawFile);
				final BufferedInputStream bis = new BufferedInputStream(fis);
				final DataInputStream dis = new DataInputStream(bis);) {

			while (true) {

				final String eventType;
				try {
					eventType = dis.readUTF();
				} catch (final EOFException e) {
					break;
				}

				final long timestamp = dis.readLong();
				final double latency = dis.readDouble();

				addRecord(eventType, timestamp, latency, data);
			}
		}

		return data;
	}

	private static void addRecord(	final String eventType, 
									final long timestamp, 
									final double latency, 
									final Map<String, List<TimestampAndLatency>> data) {

		List<TimestampAndLatency> list = data.get(eventType);
		if (list == null) {
			list = new ArrayList<>();
			data.put(eventType, list);
		}

		list.add(new TimestampAndLatency(timestamp, latency));
	}

	private static File createRawFile(final RecordIterator recordIterator) throws IOException, FileNotFoundException {
		
		final String tmpIODir = System.getProperty("java.io.tmpdir");
		final Path baseDir = Files.createDirectories(Paths.get(tmpIODir, "perfstats_jackpot"));

		final Path dir;
		{
			final String processName = ManagementFactory.getRuntimeMXBean().getName();
			final String timestamp = new SimpleDateFormat("ddMMyyyyHHmmssSSS").format(new Date());
			final String id = processName + timestamp + uniquifier.incrementAndGet() + Math.random();
			dir = Files.createDirectory(new File(baseDir.toFile(), id).toPath());

			// TODO - check for universal uniqueness of the directory.
			// Read specification of Files.createDirectory
		}

		final File rawFile = new File(dir.toFile(), "raw.dat");

		try (final FileOutputStream fw = new FileOutputStream(rawFile);
				BufferedOutputStream bos = new BufferedOutputStream(fw);
				DataOutputStream dos = new DataOutputStream(bos);) {
			while (recordIterator.hasNext()) {

				final Record record = recordIterator.next();

				final String eventName = record.getEventName();
				final long timestamp = record.getTimestamp();
				final double latency = record.getLatency();

				dos.writeUTF(eventName);
				dos.writeLong(timestamp);
				dos.writeDouble(latency);
			}
		}

		System.out.println("RAW file created : " + rawFile);

		return rawFile;
	}

	private static CharSequence createHtmlColumnHeaderWithLink(final String columnName, final String link) {
		return "<th>" + createHtmlTextWithLink(columnName, link) + "</th>";
	}

	private static CharSequence createHtmlTextWithLink(final String text, final String link) {
		return text + "<sup><a href=\"" + link + "\" target=\"_blank\">?</a></sup>";
	}

	private static final class Stats {
		
		static Stats create(final List<TimestampAndLatency> latencyData, final TimeUnit latencyUnit, final String eventType) {
			final int len = latencyData.size();

			final double[] latencies = new double[len];
			final long[] timestamps = new long[len];

			int i = 0;
			for (final TimestampAndLatency datum : latencyData) {
				latencies[i] = datum.latency;
				timestamps[i] = datum.timestamp;
				i++;
			}

			return new Stats(latencies, latencyUnit, timestamps, eventType);
		}

		private final LatencyStats latencyStats;

		// TODO - include volume stats in the report
		private final VolumeStats volumeStats;

		Stats(final double[] latencies, final TimeUnit latencyUnit, final long[] timestamps, final String eventType) {
			this.latencyStats = LatencyStats.create(latencies, latencyUnit, timestamps, eventType);
			this.volumeStats = VolumeStats.create(timestamps);
		}

		LatencyStats getLatencyStats() {
			return this.latencyStats;
		}
	}

	private static final class DailyVolumeStats {
		
		private static final long MILLIS_IN_A_DAY = TimeUnit.DAYS.toMillis(1);

		private final Long day;
		private final int[] hourlyTrxCount;
		private int totalTrxCount;

		DailyVolumeStats(final Long day) {
			this.day = day;
			this.hourlyTrxCount = new int[24]; // hourly volumeStats
		}

		void add(final long millis) {
			if ((millis < 0) || (millis > MILLIS_IN_A_DAY)) {
				throw new IllegalArgumentException("" + millis);
			}

			final int hour = (int) TimeUnit.MILLISECONDS.toHours(millis);

			final int newCount = this.hourlyTrxCount[hour] + 1;

			this.hourlyTrxCount[hour] = newCount;

			this.totalTrxCount++;
		}

		@Override
		public String toString() {
			
			final Long day = this.day;
			final String dayStr = String.format("%1$td %1$tb %1$tY", day);

			final int[] trxCounts = this.hourlyTrxCount;
			final int totalTrxCount = this.totalTrxCount;
			final int len = trxCounts.length;

			int highestTrxCount = Integer.MIN_VALUE;
			int lowestTrxCount = Integer.MAX_VALUE;

			for (int hour = 0; hour < len; hour++) {
				final int trxCount = trxCounts[hour];

				highestTrxCount = Math.max(trxCount, highestTrxCount);
				lowestTrxCount = Math.min(trxCount, lowestTrxCount);
			}

			final int trxCountPadding = String.format("%d", Integer.valueOf(highestTrxCount)).length();

			final String[] labels = new String[len];
			final List<Integer> peakHours = new ArrayList<>();
			final List<Integer> valleyHours = new ArrayList<>();

			for (int hour = 0; hour < len; hour++) {
				
				final int trxCount = trxCounts[hour];
				final double perc = (trxCount * 100.0) / totalTrxCount;

				final Integer hourBoxed = Integer.valueOf(hour);

				labels[hour] = String.format("%1s    %2$02d    %3$" + trxCountPadding + "d    %4$6.2f%%", dayStr, hourBoxed, Integer.valueOf(trxCount), Double.valueOf(perc));

				if (trxCount == highestTrxCount) {
					peakHours.add(hourBoxed);
				}

				if (trxCount == lowestTrxCount) {
					valleyHours.add(hourBoxed);
				}
			}

			final String NL = System.lineSeparator();
			final String BEGIN = " <<";
			final String END = ">>";

			return "Volumetric statistics for " + dayStr + BEGIN + NL + 
					NL + 
					"Total transaction count = " + totalTrxCount + NL + 
					"Peak hours = " + peakHours + " each with " + highestTrxCount + " transactions" + NL + 
					"Valley hours = " + valleyHours + " each with " + lowestTrxCount + " transactions" + NL + 
					NL + 
					"Hourly volume" + BEGIN + NL + 
					NL + 
					HorizontalBarChart.create(trxCounts, labels) + END + NL + 
					END;
		}
	}

	private static final class VolumeStats {
		
		static VolumeStats create(final long[] timestamps) {
			return new VolumeStats(timestamps);
		}

		private final Map<Long, DailyVolumeStats> data;

		private VolumeStats(final long[] timestamps) {
			this.data = new HashMap<>();

			for (final long ts : timestamps) {
				add(ts);
			}
		}

		private void add(final long timestamp) {
			final Long startOfDay = Utils.getStartOfDay(timestamp);

			DailyVolumeStats stats = this.data.get(startOfDay);

			if (stats == null) {
				stats = new DailyVolumeStats(startOfDay);
				this.data.put(startOfDay, stats);
			}

			stats.add(timestamp - startOfDay.longValue());
		}

		@Override
		public String toString() {
			final String NL = System.lineSeparator();

			final StringBuilder buf = new StringBuilder();

			for (final Entry<Long, DailyVolumeStats> e : this.data.entrySet()) {
				buf.append(e.getValue()).append(NL).append(NL);
			}

			return buf.toString();
		}
	}

	/**
	 * http://www.itl.nist.gov/div898/handbook/eda/section3/eda35h.htm
	 * http://docs.oracle.com/cd/E17236_01/epm.1112/cb_statistical/frameset.htm?ch07s02s10s01.html 
	 * http://www.astm.org/SNEWS/MA_2011/datapoints_ma11.html
	 * http://msdn.microsoft.com/en-us/library/bb924370.aspx
	 * http://www.stanford.edu/class/archive/anthsci/anthsci192/anthsci192.1064/handouts/calculating%20percentiles.pdf
	 * http://web.stanford.edu/~mwaskom/software/seaborn/tutorial/plotting_distributions.html
	 * 
	 * TODO - add implementations for other outlier detection techniques,
	 * especially the one based on mean absolute deviation. See
	 * http://www.itl.nist.gov/div898/handbook/eda/section3/eda35h.htm
	 */
	private static final class LatencyStats {
		
		static LatencyStats create(final double[] latencies, final TimeUnit latencyUnit, final long[] timestamps, final String eventType) {
			return new LatencyStats(latencies, latencyUnit, timestamps, eventType);
		}

		// TODO - check correctness
		private static LatencyStats removeOutliers(final LatencyStats stats, final int outlierThreshold) {
			
			final int[] outlierIndices = stats.getZScoreOutliers(outlierThreshold).getIndices();
			final int outlierCount = outlierIndices.length;

			final double[] latencies = stats.getLatencies();
			final long[] timestamps = stats.getTimestamps();

			final int maxCount = latencies.length;
			double[] latenciesWithoutOutliers = new double[maxCount];
			long[] timestampsWithoutOutliers = new long[maxCount];
			int count = 0;
			int k = 0;

			int nextOutlierIndex = (k >= outlierCount) ? Integer.MIN_VALUE : outlierIndices[k];
			k++;

			for (int i = 0; i < maxCount; i++) {
				if (i == nextOutlierIndex) {
					nextOutlierIndex = (k >= outlierCount) ? Integer.MIN_VALUE : outlierIndices[k];
					k++;
				} else {
					latenciesWithoutOutliers[count] = latencies[i];
					timestampsWithoutOutliers[count] = timestamps[i];
					count++;
				}
			}

			latenciesWithoutOutliers = Arrays.copyOf(latenciesWithoutOutliers, count);
			timestampsWithoutOutliers = Arrays.copyOf(timestampsWithoutOutliers, count);

			return LatencyStats.create(latenciesWithoutOutliers, stats.getLatencyUnit(), timestampsWithoutOutliers, stats.getEventType());
		}

		private final double[] latencies;
		private final TimeUnit latencyUnit;
		private final long[] timestamps;
		private final double[] sortedLatencies;
		private final int sampleCount;
		private final double min;
		private final double max;
		private final double mean;
		private final double median;
		private final double stdDeviation;
		private final double variance;

		/**
		 * Pearson's moment coefficient of skewness.
		 * 
		 * @see http://en.wikipedia.org/wiki/Skewness#Pearson.27s_moment_coefficient_of_skewness
		 * @see http://www.tc3.edu/instruct/sbrown/stat/shape.htm#SkewnessCompute
		 */
		private final double skewness;

		/**
		 * Pearson's moment coefficient of kurtosis.
		 * 
		 * @see http://en.wikipedia.org/wiki/Kurtosis#Pearson_moments
		 * @see http://www.tc3.edu/instruct/sbrown/stat/shape.htm#KurtosisCompute
		 */
		private final double kurtosis;

		/**
		 * @see http://en.wikipedia.org/wiki/Kurtosis#Pearson_moments
		 */
		private final double excessKurtosis;

		private final double[] zscores;
		private final String eventType;

		private LatencyStats(final double[] latencies, final TimeUnit latencyUnit, final long[] timestamps, final String eventType) {
			
			final int n = latencies.length;

			final double sum = Utils.sum(latencies);
			final double mean = sum / n;

			final double[] minMax = Utils.minMax(latencies);
			final double min = minMax[0];
			final double max = minMax[1];

			double s1 = 0;
			double s2 = 0;
			double s3 = 0;
			
			for (final double latency : latencies) {
				
				final double diff = latency - mean;

				s1 += Math.pow(diff, 2);
				s2 += Math.pow(diff, 3);
				s3 += Math.pow(diff, 4);
			}

			final double variance = s1 / n;
			final double thirdMoment = s2 / n;
			final double fourthMoment = s3 / n;

			final double skewness = thirdMoment / Math.pow(variance, 1.5);
			final double kurtosis = fourthMoment / Math.pow(variance, 2);
			final double excessKurtosis = kurtosis - 3;

			final double stdDeviation = Math.sqrt(s1 / (n - 1));
			final double[] sorted = Utils.sort(latencies);
			final double median = Utils.getMedian(sorted);

			final double[] zscores = Utils.zScores(latencies, mean, stdDeviation);

			this.sampleCount = n;
			this.latencies = latencies;
			this.sortedLatencies = sorted;
			this.latencyUnit = latencyUnit;
			this.timestamps = timestamps;
			this.min = min;
			this.max = max;
			this.mean = mean;
			this.median = median;
			this.stdDeviation = stdDeviation;
			this.variance = variance;
			this.skewness = skewness;
			this.kurtosis = kurtosis;
			this.excessKurtosis = excessKurtosis;
			this.zscores = zscores;
			this.eventType = eventType;
		}

		Outliers getZScoreOutliers(final double threshold) {
			final int[] indices = Utils.getIndicesOfValuesGreaterThan(threshold, this.zscores);
			return new Outliers(indices, Utils.getValuesForIndices(indices, this.latencies), Utils.getValuesForIndices(indices, this.zscores));
		}

		private Histogram<Double> createHistogram(final double[] intervalPoints) {
			return Histogram.create(this.latencies, intervalPoints);
		}

		private Percentiles getPercentiles(final double[] keys) {
			return Utils.getPercentiles(this.sortedLatencies, keys, Utils.toShortForm(this.latencyUnit));
		}

		private String getShortSummary() {
			
			final String NL = System.lineSeparator();
			final String IND = "    ";

			final String timeUnit = Utils.toShortForm(this.latencyUnit);

			return 	IND + "       Event count = " + this.sampleCount + NL + 
					IND + "            Median = " + toDisplayString(this.median) + ' ' + timeUnit + NL + 
					IND + "              Mean = " + toDisplayString(this.mean) + ' ' + timeUnit + NL + 
					IND + "           Minimum = " + toDisplayString(this.min) + ' ' + timeUnit + NL + 
					IND + "           Maximum = " + toDisplayString(this.max) + ' ' + timeUnit + NL + 
					IND + "Standard deviation = " + toDisplayString(this.stdDeviation) + NL + 
					IND + "          Variance = " + toDisplayString(this.variance) + NL + 
					IND + "          Skewness = " + toDisplayString(this.skewness) + NL + 
					IND + "          Kurtosis = " + toDisplayString(this.kurtosis) + NL + 
					IND + "   Excess Kurtosis = " + toDisplayString(this.excessKurtosis);
		}

		private String getShortSummaryHtml() {

			final String NL = System.lineSeparator();
			final String timeUnit = Utils.toShortForm(this.latencyUnit);
			
			final String fontFamily = SVGConstants.SERIF_FONT_FAMILY;
			final double fontSize = SVGConstants.SERIF_FONT_SIZE;
			final String columnStyle1 = "style=\"padding: 0px 0px 0px 10px; text-align: right;\"";
			final String columnStyle2 = "style=\"padding: 0px 0px 0px 30px; text-align: right;\"";
			final String columnStyle3 = "style=\"padding: 0px 10px 0px 10px; text-align: left;\"";

			//TODO - create a generic method that creates HTML table and remember to use escape HTML special characters.
			
			final String html = 
					"<table style=\"border:1px solid black; font-family: " + fontFamily + "; font-size: " + fontSize + "px;\">" + NL +
					"	<tr style=\"outline:1px solid black;\">" + NL +
					"		<th " + columnStyle1 + ">Name</th>" + NL +
					"		<th " + columnStyle2 + ">Value</th>" + NL + 
					"		<th " + columnStyle3 + ">Unit</th>" + NL + 
					"	</tr>" + NL +
					"	<tr>" + NL +
					"		<td " + columnStyle1 + ">Event count</td>" + NL +
					"		<td " + columnStyle2 + ">" + this.sampleCount + "</td>" + NL +
					"	</tr>" + NL + 
					"	<tr>" + NL +
					"		<td " + columnStyle1 + ">Median</td>" + NL +
					"		<td " + columnStyle2 + ">" + toDisplayString(this.median) + "</td>" + NL +
					"		<td " + columnStyle3 + ">" + timeUnit + "</td>" + NL +
					"	</tr>" + NL +
					"	<tr>" + NL +
					"		<td " + columnStyle1 + ">Mean</td>" + NL +
					"		<td " + columnStyle2 + ">" + toDisplayString(this.mean) + "</td>" + NL +
					"		<td " + columnStyle3 + ">" + timeUnit + "</td>" + NL +
					"	</tr>" + NL +
					"	<tr>" + NL +
					"		<td " + columnStyle1 + ">Minimum</td>" + NL +
					"		<td " + columnStyle2 + ">" + toDisplayString(this.min) + "</td>" + NL +
					"		<td " + columnStyle3 + ">" + timeUnit + "</td>" + NL +
					"	</tr>" + NL +
					"	<tr>" + NL +
					"		<td " + columnStyle1 + ">Maximum</td>" + NL +
					"		<td " + columnStyle2 + ">" + toDisplayString(this.max) + "</td>" + NL +
					"		<td " + columnStyle3 + ">" + timeUnit + "</td>" + NL +
					"	</tr>" + NL +
					"	<tr>" + NL +
					"		<td " + columnStyle1 + ">Standard deviation</td>" + NL +
					"		<td " + columnStyle2 + ">" + toDisplayString(this.stdDeviation) + "</td>" + NL +
					"	</tr>" + NL +
					"	<tr>" + NL +
					"		<td " + columnStyle1 + ">Variance</td>" + NL +
					"		<td " + columnStyle2 + ">" + toDisplayString(this.variance) + "</td>" + NL +
					"	</tr>" + NL +
					"	<tr>" + NL +
					"		<td " + columnStyle1 + ">Skewness</td>" + NL +
					"		<td " + columnStyle2 + ">" + toDisplayString(this.skewness) + "</td>" + NL +
					"	</tr>" + NL +
					"	<tr>" + NL +
					"		<td " + columnStyle1 + ">Kurtosis</td>" + NL +
					"		<td " + columnStyle2 + ">" + toDisplayString(this.kurtosis) + "</td>" + NL +
					"	</tr>" + NL +
					"	<tr>" + NL +
					"		<td " + columnStyle1 + ">Excess Kurtosis</td>" + NL +
					"		<td " + columnStyle2 + ">" + toDisplayString(this.excessKurtosis) + "</td>" + NL +
					"	</tr>" + NL +
					"</table>" + NL;

			return html;
		}

		double[] getLatencies() {
			return this.latencies;
		}
		
		TimeUnit getLatencyUnit() {
			return this.latencyUnit;
		}

		long[] getTimestamps() {
			return this.timestamps;
		}

		String getEventType() {
			return this.eventType;
		}

		public String toString(final double[] latencyIntervalPoints, final double[] percentileKeys) {
			
			final String eventType = this.eventType;

			final String NL = System.lineSeparator();
			final String BEGIN = " <<";
			final String END = ">>";

			return "Latency summary for " + eventType + BEGIN + NL + 
					NL + 
					getShortSummary() + NL + 
					END + NL + 
					NL + 
					"Latency histogram for " + eventType + BEGIN + NL + 
					NL + 
					createHistogram(latencyIntervalPoints) + NL + 
					END + NL + 
					NL + 
					"Latency percentiles for " + eventType + BEGIN + NL + 
					NL + getPercentiles(percentileKeys) + NL + 
					END;
		}

		public String[] toHtml(	final double[] intervalPointsForLatencyHistogram, 
				final double[] percentileKeys, 
				final TimeSeriesLatencyDensity density, 
				final double heatMapSingleAreaWidth, 
				final ColorRampScheme colorRampScheme) {

			final String eventType = this.eventType;

			final HeatMapSVG heatMapSVG = density.getHeatMapSVG(this.latencyUnit, heatMapSingleAreaWidth, colorRampScheme);
			final String trxCountBarChartSVG = 
					density.getTrxCountBarChartSVG(heatMapSVG.getXAxisLabelSkipCount(), heatMapSVG.getHeatMapBoxStartX(), heatMapSVG.getHeatMapSingleAreaWidth(), colorRampScheme);

			final String NL = System.lineSeparator();
			final String BR = "<br/>";

			final String style = "style=\"font-family:Courier New, Courier, monospace; font-weight:bold;\"";

			final String baseType = "Latency";

			final String typeA = "summary";
			final String typeB = "histogram";
			final String typeC = "percentiles";
			final String typeD = "heatmap";

			final String textA = baseType + " " + typeA + " | " + eventType;
			final String textB = baseType + " " + typeB + " | " + eventType;
			final String textC = baseType + " " + typeC + " | " + eventType;
			final String textD = baseType + " " + typeD + " | " + eventType;

			final String linkIdA = LinkGenerator.next("a");
			final String linkIdB = LinkGenerator.next("b");
			final String linkIdC = LinkGenerator.next("c");
			final String linkIdD = LinkGenerator.next("d");

			final String rowStyle = "style=\"outline:1px solid black;\"";
			final String columnStyle = "style=\"padding: 8px; text-align: right;\"";

			final String links = 
					"<tr " + rowStyle + ">" + NL + 
					"<td " + columnStyle + ">" + eventType + "</td>" + NL + 
					"<td " + columnStyle + ">" + this.sampleCount + "</td>" + NL + 
					"<td " + columnStyle + ">" + toDisplayString(this.median) + "</td>" + NL + 
					"<td " + columnStyle + ">" + toDisplayString(this.mean) + "</td>" + NL + 
					"<td " + columnStyle + ">" + toDisplayString(this.min) + "</td>" + NL + 
					"<td " + columnStyle + ">" + toDisplayString(this.max) + "</td>" + NL + 
					"<td " + columnStyle + ">" + linkWithRef(typeA, linkIdA) + "</td>" + NL + 
					"<td " + columnStyle + ">" + linkWithRef(typeB, linkIdB) + "</td>" + NL + 
					"<td " + columnStyle + ">" + linkWithRef(typeC, linkIdC) + "</td>" + NL + 
					"<td " + columnStyle + ">" + linkWithRef(typeD, linkIdD) + "</td>" + NL + 
					"<td " + columnStyle + ">" + toDisplayString(this.stdDeviation) + "</td>" + NL + 
					"<td " + columnStyle + ">" + toDisplayString(this.variance) + "</td>" + NL + 
					"<td " + columnStyle + ">" + toDisplayString(this.skewness) + "</td>" + NL + 
					"<td " + columnStyle + ">" + toDisplayString(this.excessKurtosis) + "</td>" + NL + 
					"</tr>" + NL;

			final String content = 
					paragraph(linkWithId(textA, linkIdA) + ':', style) + getShortSummaryHtml() + BR + BR +
					paragraph(linkWithId(textB, linkIdB) + ':', style) + createHistogram(intervalPointsForLatencyHistogram).toSVG(new StripTrailingZeroesAfterDecimalFunction(false), false, colorRampScheme) + BR + BR + 
					paragraph(linkWithId(textC, linkIdC) + ':', style) + getPercentiles(percentileKeys).toSVG(false) + BR + BR + 
					paragraph(linkWithId(textD, linkIdD) + ':', style) + trxCountBarChartSVG + BR + BR + heatMapSVG.getSvg();

			return new String[] { links, content };
		}

		private static String paragraph(final String paragraphText, final String cssStyle) {
			return "<p " + cssStyle + ">" + paragraphText + "</p>";
		}

		private static String linkWithId(final String text, final String id) {
			return "<a id=\"" + id + "\">" + text + "</a>";
		}

		private static String linkWithRef(final String text, final String ref) {
			return "<a href=\"#" + ref + "\">" + text + "</a>";
		}

		double getMedian() {
			return this.median;
		}

		private static String toDisplayString(final double d) {
			return Utils.toDisplayString(d, 3, true);
		}
	}

	private static final class Outliers {
		private final int[] indices;
		private final double[] data;
		private final double[] zscores;

		Outliers(final int[] indices, final double[] data, final double[] zscores) {
			this.indices = indices;
			this.data = data;
			this.zscores = zscores;
		}

		int[] getIndices() {
			return this.indices;
		}

		@Override
		public String toString() {
			final int n = this.indices.length;

			final StringBuilder b = new StringBuilder();

			b.append('[');
			for (int i = 0; i < n; i++) {
				b.append('(').append(this.indices[i]).append(',').append(this.data[i]).append(',').append(this.zscores[i]).append(')');
				if (i < (n - 1)) {
					b.append(',');
				}
			}
			b.append(']');

			return b.toString();
		}
	}

	private static final class LinkGenerator {
		private static int LINK_COUNTER = 0;

		static String next(final String suffix) {
			return "link" + ++LINK_COUNTER + suffix;
		}
	}
	
	private static interface LatencyStatsToHtmlFunc {
		String[] toHtml(LatencyStats stats);
	}
}

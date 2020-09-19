package fr.raksrinana.videoconverter;

import fr.raksrinana.videoconverter.utils.CLIParameters;
import fr.raksrinana.videoconverter.utils.Configuration;
import lombok.NonNull;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class BatchProcessor{
	private static final Logger LOGGER = LoggerFactory.getLogger(BatchProcessor.class);
	private static final Pattern SKIPPED_EXTENSIONS = Pattern.compile("(loc|msg|pbf|prproj|aep|ini|txt|db|dat|rtf|docx|pdf|dropbox|ds_store|js|xlsm|html|htm|gpx)");
	private static final Pattern PICTURE_EXTENSIONS = Pattern.compile("(jpg|png|jpeg|JPG|PNG|gif|svg|tiff)");
	private final Path inputHost;
	private final Path outputHost;
	private final Path batchHost;
	private final Path inputClient;
	private final Path batchClient;
	private final Configuration configuration;
	private final CLIParameters params;
	private static final List<String> ACCEPTED_CODECS = List.of("h264", "vp9", "wmv3");
	private static final List<String> USELESS_CODECS = List.of("hevc", "aac", "opus", "mp3");
	
	private BatchProcessor(@NonNull Configuration configuration, @NonNull CLIParameters params, @NonNull Path inputHost, @NonNull Path outputHost, @NonNull Path batchHost, @NonNull Path inputClient, @NonNull Path batchClient){
		this.configuration = configuration;
		this.params = params;
		this.inputHost = inputHost;
		this.outputHost = outputHost;
		this.batchHost = batchHost;
		this.inputClient = inputClient;
		this.batchClient = batchClient;
		LOGGER.trace("Created processor for {}", this.inputClient);
	}
	
	static Stream<BatchProcessor> process(@NonNull Configuration configuration, @NonNull CLIParameters params, @NonNull Path inputHost, @NonNull Path outputHost, @NonNull Path batchHost, @NonNull Path inputClient, @NonNull Path batchClient){
		try{
			final var file = inputClient.toFile();
			if(configuration.isUseless(inputClient)){
				return Stream.empty();
			}
			if(file.isFile()){
				if(shouldSkip(inputClient)){
					LOGGER.info("Skipping {}", inputClient);
					configuration.setUseless(inputClient);
					return Stream.empty();
				}
				if(isPicture(inputClient)){
					LOGGER.info("Skipping photo {}", inputClient);
					configuration.setUseless(inputClient);
					return Stream.empty();
				}
				if(file.isHidden()){
					LOGGER.warn("Path {} (H: {}) is hidden, skipping", inputClient, inputHost);
					return Stream.empty();
				}
				return Stream.of(new BatchProcessor(configuration, params, inputHost, outputHost, batchHost, inputClient, batchClient));
			}
			else if(file.isDirectory()){
				if(file.isHidden()){
					LOGGER.warn("Path {} (H: {}) is hidden, skipping", inputClient, inputHost);
					return Stream.empty();
				}
				return Optional.ofNullable(file.listFiles())
						.map(Arrays::asList)
						.orElse(List.of())
						.parallelStream()
						.flatMap(subFile -> BatchProcessor.process(configuration, params, inputHost.resolve(subFile.getName()), outputHost.resolve(subFile.getName()), batchHost, inputClient.resolve(subFile.getName()), batchClient));
			}
			else{
				LOGGER.warn("What kind if file is that? {} (H: {})", inputClient, inputHost);
				if(file.isHidden()){
					LOGGER.warn("Path {} (H: {}) is hidden, skipping", inputClient, inputHost);
					return Stream.empty();
				}
			}
			return Stream.empty();
		}
		catch(Exception e){
			LOGGER.error("Error processing {}", inputClient, e);
		}
		return Stream.empty();
	}
	
	private static boolean isPicture(Path path){
		final var filename = path.getFileName().toString();
		return Optional.of(filename.lastIndexOf("."))
				.filter(i -> i >= 0)
				.map(i -> filename.substring(i + 1))
				.map(ext -> ext.isBlank() || PICTURE_EXTENSIONS.matcher(ext).matches())
				.orElse(false);
	}
	
	private static boolean shouldSkip(Path path){
		final var filename = path.getFileName().toString();
		return Optional.of(filename.lastIndexOf("."))
				.filter(i -> i >= 0)
				.map(i -> filename.substring(i + 1))
				.map(ext -> ext.isBlank() || SKIPPED_EXTENSIONS.matcher(ext).matches())
				.orElse(false);
	}
	
	BatchProcessorResult process(){
		try{
			final var file = inputClient.toFile();
			LOGGER.info("Processing {}", this.inputClient);
			if(file.isFile()){
				try{
					final var ffprobe = new FFprobe(this.params.getFfprobePath());
					FFmpegProbeResult probeResult = ffprobe.probe(inputClient.toString());
					return probeResult.getStreams().stream()
							.filter(s -> ACCEPTED_CODECS.contains(s.codec_name))
							.findFirst()
							.map(stream -> {
								try{
									if(this.params.getItemProcessor()
											.getConstructor()
											.newInstance()
											.create(this.params, probeResult, stream, this.inputHost, this.outputHost, this.batchHost, this.batchClient)){
										return BatchProcessorResult.newCreated();
									}
								}
								catch(InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e){
									LOGGER.error("Failed to instantiate item processor", e);
									return BatchProcessorResult.newErrored();
								}
								return BatchProcessorResult.newHandled();
							}).orElseGet(() -> {
								if(probeResult.getStreams().stream().allMatch(s -> USELESS_CODECS.contains(s.codec_name))){
									configuration.setUseless(this.inputClient);
									LOGGER.debug("Codec {} is useless, marking as useless", probeResult.getStreams().stream().map(s -> s.codec_name).collect(Collectors.joining(", ")));
								}
								else{
									LOGGER.debug("No streams match the criteria (available codecs: {})", probeResult.getStreams().stream().map(s -> s.codec_name).collect(Collectors.joining(", ")));
								}
								return BatchProcessorResult.newHandled();
							});
				}
				catch(Exception e){
					LOGGER.error("Failed to get video infos {}", this.inputClient, e);
				}
				return BatchProcessorResult.newHandled();
			}
			else if(file.isDirectory()){
				LOGGER.warn("Tried to process a folder {}", this.inputClient);
			}
			else{
				LOGGER.warn("What kind if file is that? {} (H: {})", this.inputClient, this.inputHost);
			}
			return BatchProcessorResult.newScanned();
		}
		catch(Exception e){
			LOGGER.error("Error processing {}", this.inputClient, e);
		}
		return BatchProcessorResult.newScanned();
	}
}

package fr.mrcraftcod.videonormalizer;

import fr.mrcraftcod.videonormalizer.utils.CLIParameters;
import fr.mrcraftcod.videonormalizer.utils.Configuration;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collector;

class BatchProcessor{
	private static final Logger LOGGER = LoggerFactory.getLogger(BatchProcessor.class);
	private static final Pattern SKIPPED_EXTENSIONS = Pattern.compile("(loc|msg|pbf|prproj|aep|ini|txt|db|dat|rtf|docx|pdf|dropbox|ds_store|js|xlsm|webm|wmv|html|htm|gpx)");
	private static final Pattern PICTURE_EXTENSIONS = Pattern.compile("(jpg|png|jpeg|JPG|PNG|gif|svg|tiff)");
	private final Path inputHost;
	private final Path outputHost;
	private final Path batchHost;
	private final Path inputClient;
	private final Path batchClient;
	private final Configuration configuration;
	private final CLIParameters params;
	private static final List<String> ACCEPTED_FORMATS = List.of("QuickTime / MOV");
	private static final List<String> ACCEPTED_CODECS = List.of("h264");
	private static final List<String> USELESS_CODECS = List.of("hevc");
	
	BatchProcessor(@Nonnull Configuration configuration, @Nonnull CLIParameters params, @Nonnull Path inputHost, @Nonnull Path outputHost, @Nonnull Path batchHost, @Nonnull Path inputClient, @Nonnull Path batchClient){
		this.configuration = configuration;
		this.params = params;
		this.inputHost = inputHost;
		this.outputHost = outputHost;
		this.batchHost = batchHost;
		this.inputClient = inputClient;
		this.batchClient = batchClient;
		if(!this.inputClient.toFile().exists()){
			throw new IllegalArgumentException("Input client path " + this.inputClient.toAbsolutePath().toString() + " doesn't exists");
		}
	}
	
	BatchProcessorResult process(){
		try{
			if(this.configuration.isUseless(this.inputClient)){
				return BatchProcessorResult.EMPTY;
			}
			if(this.inputClient.toFile().isHidden()){
				LOGGER.warn("Path {} (H: {}) is hidden, skipping", this.inputClient, this.inputHost);
				return BatchProcessorResult.EMPTY;
			}
			LOGGER.info("Processing {}", this.inputClient);
			if(this.inputClient.toFile().isFile()){
				if(this.shouldSkip(this.inputClient)){
					LOGGER.info("Skipping {}", this.inputClient);
					this.configuration.setUseless(this.inputClient);
					return BatchProcessorResult.SCANNED_1;
				}
				if(this.isPicture(this.inputClient)){
					LOGGER.info("Skipping photo {}", this.inputClient);
					return BatchProcessorResult.SCANNED_1;
				}
				try{
					final var ffprobe = new FFprobe(this.params.getFfprobePath());
					FFmpegProbeResult probeResult = ffprobe.probe(inputClient.toString());
					if(ACCEPTED_FORMATS.contains(probeResult.getFormat().format_long_name)){
						if(probeResult.getStreams().stream().anyMatch(s -> ACCEPTED_CODECS.contains(s.codec_name))){
							if(configuration.getBatchCreator().create(probeResult, this.inputHost, this.outputHost, this.batchHost, this.batchClient)){
								return BatchProcessorResult.CREATED_1;
							}
						}
						else if(probeResult.getStreams().stream().anyMatch(s -> USELESS_CODECS.contains(s.codec_name))){
							configuration.setUseless(this.inputClient);
							LOGGER.debug("Codec is useless, marking as useless");
						}
						else{
							LOGGER.debug("No streams match the criteria");
						}
					}
					else{
						LOGGER.debug("Format {} not handled, skipping", probeResult.getFormat().format_long_name);
					}
				}
				catch(Exception e){
					LOGGER.error("Failed to get video infos {}", this.inputClient, e);
				}
				return BatchProcessorResult.SCANNED_1;
			}
			else if(this.inputClient.toFile().isDirectory()){
				return Optional.ofNullable(this.inputClient.toFile().listFiles()).stream().parallel().flatMap(Arrays::stream).map(subFile -> {
					try{
						return new BatchProcessor(this.configuration, this.params, this.inputHost.resolve(subFile.getName()), this.outputHost.resolve(subFile.getName()), this.batchHost, this.inputClient.resolve(subFile.getName()), this.batchClient).process();
					}
					catch(Exception e){
						LOGGER.error("Error processing {}", this.inputClient.resolve(subFile.getName()), e);
					}
					return BatchProcessorResult.SCANNED_1;
				}).collect(Collector.of(() -> new BatchProcessorResult(0, 0), BatchProcessorResult::add, BatchProcessorResult::add));
			}
			else{
				LOGGER.warn("What kind if file is that? {} (H: {})", this.inputClient, this.inputHost);
			}
			return BatchProcessorResult.EMPTY;
		}
		catch(Exception e){
			LOGGER.error("Error processing {}", this.inputClient, e);
		}
		return BatchProcessorResult.EMPTY;
	}
	
	private boolean isPicture(Path path){
		final var filename = path.getFileName().toString();
		return Optional.of(filename.lastIndexOf(".")).filter(i -> i >= 0).map(i -> filename.substring(i + 1)).map(ext -> ext.isBlank() || PICTURE_EXTENSIONS.matcher(ext).matches()).orElse(false);
	}
	
	private boolean shouldSkip(Path path){
		final var filename = path.getFileName().toString();
		return Optional.of(filename.lastIndexOf(".")).filter(i -> i >= 0).map(i -> filename.substring(i + 1)).map(ext -> ext.isBlank() || SKIPPED_EXTENSIONS.matcher(ext).matches()).orElse(false);
	}
}

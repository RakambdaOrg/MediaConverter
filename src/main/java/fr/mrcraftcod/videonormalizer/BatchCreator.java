package fr.mrcraftcod.videonormalizer;

import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegFormat;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

public class BatchCreator{
	private static final Logger LOGGER = LoggerFactory.getLogger(BatchCreator.class);
	private static final Pattern SKIPPED_EXTENSIONS = Pattern.compile("(loc|msg|pbf|prproj|aep|ini|txt|db|dat|rtf|docx|pdf|dropbox|ds_store|js|xlsm|webm|wmv|html|htm|gpx)");
	private static final Pattern PICTURE_EXTENSIONS = Pattern.compile("(jpg|png|jpeg|JPG|PNG|gif|svg|tiff)");
	@Nonnull
	private final Path inputHost;
	@Nonnull
	private final Path outputHost;
	@Nonnull
	private final Path batchHost;
	@Nonnull
	private final Path inputClient;
	@Nonnull
	private final Path batchClient;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final CLIParameters params;
	
	public BatchCreator(@Nonnull Configuration configuration, @Nonnull CLIParameters params, @Nonnull Path inputHost, @Nonnull Path outputHost, @Nonnull Path batchHost, @Nonnull Path inputClient, @Nonnull Path batchClient){
		this.configuration = configuration;
		this.params = params;
		this.inputHost = inputHost;
		this.outputHost = outputHost;
		this.batchHost = batchHost;
		this.inputClient = inputClient;
		this.batchClient = batchClient;
		LOGGER.debug(this.inputClient.toFile().getAbsolutePath());
		if(!this.inputClient.toFile().exists()){
			throw new IllegalArgumentException("Input client path " + this.inputClient.toAbsolutePath().toString() + " doesn't exists");
		}
	}
	
	public void process(){
		try{
			if(this.configuration.isUseless(this.inputClient)){
				return;
			}
			if(this.inputClient.toFile().isHidden()){
				LOGGER.warn("Path {} (H: {}) is hidden, skipping", this.inputClient, this.inputHost);
				return;
			}
			LOGGER.info("Processing {}", this.inputClient);
			if(this.inputClient.toFile().isFile()){
				if(this.shouldSkip(this.inputClient)){
					LOGGER.info("Skipping {}", this.inputClient);
					this.configuration.setUseless(this.inputClient);
					return;
				}
				if(this.isPicture(this.inputClient)){
					LOGGER.info("Skipping photo {}", this.inputClient);
					return;
				}
				try{
					final var ffprobe = new FFprobe(this.params.getFfprobePath());
					FFmpegProbeResult probeResult = ffprobe.probe(inputClient.toString());
					FFmpegFormat format = probeResult.getFormat();
					System.out.format("%nFile: '%s' ; Format: '%s' ; Duration: %.3fs", format.filename, format.format_long_name, format.duration);
					FFmpegStream stream = probeResult.getStreams().get(0);
					System.out.format("%nCodec: '%s' ; Width: %dpx ; Height: %dpx", stream.codec_long_name, stream.width, stream.height);
				}
				catch(Exception e){
					LOGGER.error("Failed to get video infos {}", this.inputClient, e);
				}
			}
			else if(this.inputClient.toFile().isDirectory()){
				Optional.ofNullable(this.inputClient.toFile().listFiles()).stream().flatMap(Arrays::stream).forEach(subFile -> {
					try{
						new BatchCreator(this.configuration, this.params, this.inputHost.resolve(subFile.getName()), this.outputHost.resolve(subFile.getName()), this.batchHost, this.inputClient.resolve(subFile.getName()), this.batchClient).process();
					}
					catch(Exception e){
						LOGGER.error("Error processing {}", this.inputClient.resolve(subFile.getName()), e);
					}
				});
				System.out.println();
			}
			else{
				LOGGER.warn("What kind if file is that? {} (H: {})", this.inputClient, this.inputHost);
			}
		}
		catch(Exception e){
			LOGGER.error("Error processing {}", this.inputClient, e);
		}
	}
	
	private boolean isPicture(Path path){
		final var filename = path.getFileName().toString();
		return Optional.of(filename.lastIndexOf(".")).filter(i -> i >= 0).map(i -> filename.substring(i + 1)).map(ext -> ext.isBlank() || PICTURE_EXTENSIONS.matcher(ext).matches()).orElse(false);
	}
	
	private boolean shouldSkip(Path path){
		final var filename = path.getFileName().toString();
		return Optional.of(filename.lastIndexOf(".")).filter(i -> i >= 0).map(i -> filename.substring(i + 1)).map(ext -> {
			LOGGER.debug(ext);
			return ext.isBlank() || SKIPPED_EXTENSIONS.matcher(ext).matches();
		}).orElse(false);
	}
}

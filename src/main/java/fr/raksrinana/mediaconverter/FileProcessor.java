package fr.raksrinana.mediaconverter;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.raksrinana.mediaconverter.mediaprocessor.AudioToAacMediaProcessor;
import fr.raksrinana.mediaconverter.mediaprocessor.MediaProcessor;
import fr.raksrinana.mediaconverter.mediaprocessor.TiffToJpegMediaProcessor;
import fr.raksrinana.mediaconverter.mediaprocessor.VideoToHevcMediaProcessor;
import fr.raksrinana.mediaconverter.storage.IStorage;
import lombok.extern.log4j.Log4j2;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

@Log4j2
public class FileProcessor implements FileVisitor<Path>, AutoCloseable{
	private static final Collection<String> MEDIA_EXTENSIONS = List.of(
			"mp4",
			"mov",
			"mkv",
			"avi",
			"tiff",
			"mp3",
			"m4a",
			"ts"
	);
	private final ExecutorService executor;
	private final IStorage storage;
	private final Supplier<FFmpeg> ffmpegSupplier;
	private final Supplier<FFprobe> ffprobeSupplier;
	private final Path tempDirectory;
	private final Path baseInput;
	private final Path baseOutput;
	private final Collection<MediaProcessor> processors;
	private final Collection<Path> excluded;
	private final ProgressBar progressBar;
	
	public FileProcessor(ExecutorService executor, IStorage storage, Supplier<FFmpeg> ffmpegSupplier, Supplier<FFprobe> ffprobeSupplier, Path tempDirectory, Path baseInput, Path baseOutput, Collection<Path> excluded){
		this.executor = executor;
		this.storage = storage;
		this.ffmpegSupplier = ffmpegSupplier;
		this.ffprobeSupplier = ffprobeSupplier;
		this.tempDirectory = tempDirectory;
		this.baseInput = baseInput;
		this.baseOutput = baseOutput;
		this.excluded = excluded;
		progressBar = new ProgressBarBuilder()
				.setTaskName("Scanning")
				.setUnit("File", 1)
				.build();
		processors = List.of(
				new VideoToHevcMediaProcessor(),
				new AudioToAacMediaProcessor(),
				new TiffToJpegMediaProcessor()
		);
	}
	
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException{
		if(Files.isHidden(dir) && Objects.nonNull(dir.getParent())){
			return SKIP_SUBTREE;
		}
		var isExcluded = excluded.stream().anyMatch(exclude -> Objects.equals(exclude, dir.toAbsolutePath()));
		if(isExcluded){
			return SKIP_SUBTREE;
		}
		log.debug("Entering folder {}", dir);
		progressBar.maxHint(progressBar.getMax() + Files.list(dir).count());
		return CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException{
		progressBar.step();
		try{
			if(storage.isUseless(file)){
				return CONTINUE;
			}
		}
		catch(SQLException e){
			log.error("Failed to interact with storage", e);
		}
		
		if(isNotMedia(file) || Files.isHidden(file)){
			storage.setUseless(file);
			return CONTINUE;
		}
		
		var ffprobe = ffprobeSupplier.get();
		FFprobeResult probeResult;
		try{
			log.info("Scanning file {}", file);
			probeResult = ffprobe.setShowStreams(true)
					.setShowFormat(true)
					.setInput(file.toString())
					.execute();
		}
		catch(RuntimeException e){
			log.error("Failed to probe file", e);
			return CONTINUE;
		}
		
		getProcessor(probeResult).ifPresentOrElse(processor -> {
			var outfile = buildOutFile(file, processor.getDesiredExtension());
			
			if(!Files.exists(outfile.getParent())){
				try{
					Files.createDirectories(outfile.getParent());
				}
				catch(IOException e){
					log.error("Failed to create output folder for {}", outfile, e);
					return;
				}
			}
			
			executor.submit(processor.createConvertTask(
					ffmpegSupplier.get(),
					probeResult,
					file,
					outfile,
					tempDirectory.resolve("" + file.hashCode() + file.getFileName())
			));
		}, () -> storage.setUseless(file));
		
		return CONTINUE;
	}
	
	private boolean isNotMedia(Path file){
		var filename = file.getFileName().toString();
		var dotIndex = filename.lastIndexOf('.');
		
		if(dotIndex <= 0){
			return true;
		}
		
		var extension = filename.substring(dotIndex + 1).toLowerCase();
		return !MEDIA_EXTENSIONS.contains(extension);
	}
	
	private Optional<MediaProcessor> getProcessor(FFprobeResult probeResult){
		if(Objects.isNull(probeResult)){
			return Optional.empty();
		}
		
		for(var processor : processors){
			if(processor.canHandle(probeResult)){
				log.trace("Processor {} matched", processor.getClass().getSimpleName());
				return Optional.of(processor);
			}
		}
		return Optional.empty();
	}
	
	private Path buildOutFile(Path file, String desiredExtension){
		var relative = baseInput.relativize(file);
		var outFile = baseOutput.resolve(relative);
		
		if(Objects.nonNull(desiredExtension)){
			var filename = outFile.getFileName().toString();
			var dotIndex = filename.lastIndexOf('.');
			if(dotIndex > 0){
				filename = filename.substring(0, dotIndex);
			}
			outFile = outFile.resolveSibling(filename + '.' + desiredExtension);
		}
		
		return outFile;
	}
	
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc){
		return CONTINUE;
	}
	
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc){
		log.trace("Leaving folder {}", dir);
		try{
			storage.save();
		}
		catch(SQLException | IOException e){
			log.error("Failed to save useless files after folder", e);
		}
		return CONTINUE;
	}
	
	@Override
	public void close(){
		progressBar.close();
	}
}

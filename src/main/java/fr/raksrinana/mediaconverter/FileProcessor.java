package fr.raksrinana.mediaconverter;

import fr.raksrinana.mediaconverter.codecprocessor.TiffToJpegMediaProcessor;
import fr.raksrinana.mediaconverter.codecprocessor.VideoToHevcMediaProcessor;
import fr.raksrinana.mediaconverter.utils.Storage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

@Slf4j
public class FileProcessor implements FileVisitor<Path>{
	private final ExecutorService executor;
	private final Storage storage;
	private final FFmpeg ffmpeg;
	private final FFprobe ffprobe;
	private final Path tempDirectory;
	private final Path baseInput;
	private final Path baseOutput;
	private final List<MediaProcessor> processors;
	
	public FileProcessor(ExecutorService executor, Storage storage, FFmpeg ffmpeg, FFprobe ffprobe, Path tempDirectory, Path baseInput, Path baseOutput){
		this.executor = executor;
		this.storage = storage;
		this.ffmpeg = ffmpeg;
		this.ffprobe = ffprobe;
		this.tempDirectory = tempDirectory;
		this.baseInput = baseInput;
		this.baseOutput = baseOutput;
		this.processors = List.of(
				new VideoToHevcMediaProcessor(),
				new TiffToJpegMediaProcessor()
		);
	}
	
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException{
		if(Files.isHidden(dir) && Objects.nonNull(dir.getParent())){
			return SKIP_SUBTREE;
		}
		log.info("Entering folder {}", dir);
		return CONTINUE;
	}
	
	@SneakyThrows
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs){
		if(Files.isHidden(file) || storage.isUseless(file)){
			return CONTINUE;
		}
		
		log.info("Scanning file {}", file);
		var probeResult = ffprobe.probe(file.toString());
		
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
					ffmpeg,
					probeResult,
					file,
					outfile,
					tempDirectory.resolve("" + file.hashCode() + file.getFileName())
			));
		}, () -> storage.setUseless(file));
		
		return CONTINUE;
	}
	
	private Optional<MediaProcessor> getProcessor(FFmpegProbeResult probeResult){
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
		return CONTINUE;
	}
}

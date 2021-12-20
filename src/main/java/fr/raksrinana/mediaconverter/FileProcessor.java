package fr.raksrinana.mediaconverter;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.raksrinana.mediaconverter.mediaprocessor.MediaProcessor;
import fr.raksrinana.mediaconverter.storage.IStorage;
import lombok.extern.log4j.Log4j2;
import me.tongfei.progressbar.ProgressBar;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Log4j2
public class FileProcessor implements Runnable{
	private final ExecutorService executor;
	private final IStorage storage;
	private final Supplier<FFmpeg> ffmpegSupplier;
	private final Supplier<FFprobe> ffprobeSupplier;
	private final Path tempDirectory;
	private final Path baseInput;
	private final Path baseOutput;
	private final Collection<MediaProcessor> processors;
	private final BlockingQueue<Path> queue;
	private final ProgressBar progressBar;
	private final CountDownLatch countDownLatch;
	
	private boolean shutdown;
	
	public FileProcessor(ExecutorService executor, IStorage storage, Supplier<FFmpeg> ffmpegSupplier, Supplier<FFprobe> ffprobeSupplier, Path tempDirectory, Path baseInput, Path baseOutput, Collection<MediaProcessor> processors, BlockingQueue<Path> queue, ProgressBar progressBar){
		this.executor = executor;
		this.storage = storage;
		this.ffmpegSupplier = ffmpegSupplier;
		this.ffprobeSupplier = ffprobeSupplier;
		this.tempDirectory = tempDirectory;
		this.baseInput = baseInput;
		this.baseOutput = baseOutput;
		this.processors = processors;
		this.queue = queue;
		this.progressBar = progressBar;
		
		shutdown = false;
		countDownLatch = new CountDownLatch(1);
	}
	
	@Override
	public void run(){
		try{
			do{
				var file = queue.poll(5, TimeUnit.SECONDS);
				if(Objects.nonNull(file)){
					processFile(file);
					progressBar.step();
				}
			}
			while(!shutdown || !queue.isEmpty());
		}
		catch(InterruptedException e){
			log.error("Error waiting for element", e);
		}
		finally{
			countDownLatch.countDown();
		}
	}
	
	private void processFile(Path file){
		progressBar.setExtraMessage(file.subpath(file.getNameCount() - 2, file.getNameCount()).toString());
		
		var ffprobe = ffprobeSupplier.get();
		FFprobeResult probeResult;
		try{
			log.debug("Scanning file {}", file);
			probeResult = ffprobe.setShowStreams(true)
					.setShowFormat(true)
					.setInput(file.toString())
					.execute();
		}
		catch(RuntimeException e){
			log.error("Failed to probe file {}", file, e);
			return;
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
					tempDirectory.resolve("" + file.hashCode() + outfile.getFileName())
			));
		}, () -> storage.setUseless(file));
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
	
	public void shutdown() throws InterruptedException{
		shutdown = true;
		countDownLatch.await();
	}
}

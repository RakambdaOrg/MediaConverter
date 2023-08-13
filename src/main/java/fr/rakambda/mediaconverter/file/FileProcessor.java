package fr.rakambda.mediaconverter.file;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import me.tongfei.progressbar.ProgressBar;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Log4j2
public class FileProcessor implements Runnable{
	private final ExecutorService executor;
	private final Supplier<FFmpeg> ffmpegSupplier;
	private final Path tempDirectory;
	private final Path baseInput;
	private final Path baseOutput;
	private final BlockingQueue<FileProber.ProbeResult> queue;
	private final ProgressBar progressBar;
	private final ProgressBarSupplier converterProgressBarSupplier;
	private final boolean deleteInput;
	
	private final CountDownLatch countDownLatch;
	private boolean shutdown;
	
	public FileProcessor(@NonNull ExecutorService executor,
			@NonNull Supplier<FFmpeg> ffmpegSupplier,
			@NonNull Path tempDirectory,
			@NonNull Path baseInput,
			@NonNull Path baseOutput,
			@NonNull BlockingQueue<FileProber.ProbeResult> queue,
			@NonNull ProgressBar progressBar,
			@NonNull ProgressBarSupplier converterProgressBarSupplier,
			boolean deleteInput){
		this.executor = executor;
		this.ffmpegSupplier = ffmpegSupplier;
		this.tempDirectory = tempDirectory;
		this.baseInput = baseInput;
		this.baseOutput = baseOutput;
		this.queue = queue;
		this.progressBar = progressBar;
		this.converterProgressBarSupplier = converterProgressBarSupplier;
		this.deleteInput = deleteInput;
		
		countDownLatch = new CountDownLatch(1);
		shutdown = false;
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
	
	private void processFile(@NonNull FileProber.ProbeResult probeResultt){
		var file = probeResultt.file();
		var ffProbeResult = probeResultt.fFprobeResult();
		var processor = probeResultt.processor();
		var outfile = buildOutFile(file, processor.getDesiredExtension());
		
		if(Files.exists(outfile)){
			log.warn("Skipping {}, output already exists at {}", file, outfile);
			return;
		}
		if(!Files.exists(outfile.getParent())){
			try{
				Files.createDirectories(outfile.getParent());
			}
			catch(IOException e){
				log.error("Failed to create output folder for {}", outfile, e);
				return;
			}
		}
		
		var task = processor.createConvertTask(
				ffmpegSupplier.get(),
				ffProbeResult,
				file,
				outfile,
				tempDirectory.resolve("" + file.hashCode() + outfile.getFileName()),
				converterProgressBarSupplier,
				deleteInput
		);
		
		executor.submit(task);
	}
	
	private Path buildOutFile(@NonNull Path file, @Nullable String desiredExtension){
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

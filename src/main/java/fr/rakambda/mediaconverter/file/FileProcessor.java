package fr.rakambda.mediaconverter.file;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import fr.rakambda.mediaconverter.IProcessor;
import fr.rakambda.mediaconverter.mediaprocessor.MediaProcessorTask;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import me.tongfei.progressbar.ProgressBar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Log4j2
public class FileProcessor implements Runnable, AutoCloseable, IProcessor{
	private final ExecutorService executor;
	private final Supplier<FFmpeg> ffmpegSupplier;
	private final Path tempDirectory;
	private final Path baseInput;
	private final Path baseOutput;
	private final BlockingQueue<FileProber.ProbeResult> queue;
	private final ProgressBar progressBar;
	private final ProgressBarSupplier converterProgressBarSupplier;
	private final boolean deleteInput;
	private final Integer ffmpegThreads;
	private final boolean dryRun;
	
	private final CountDownLatch countDownLatch;
	private final Collection<MediaProcessorTask> tasks;
	private boolean shutdown;
	private boolean pause;
	
	public FileProcessor(@NonNull ExecutorService executor,
			@NonNull Supplier<FFmpeg> ffmpegSupplier,
			@NonNull Path tempDirectory,
			@NonNull Path baseInput,
			@NonNull Path baseOutput,
			@NonNull BlockingQueue<FileProber.ProbeResult> queue,
			@NonNull ProgressBar progressBar,
			@NonNull ProgressBarSupplier converterProgressBarSupplier,
			boolean deleteInput,
			@Nullable Integer ffmpegThreads,
			boolean dryRun){
		this.executor = executor;
		this.ffmpegSupplier = ffmpegSupplier;
		this.tempDirectory = tempDirectory;
		this.baseInput = baseInput;
		this.baseOutput = baseOutput;
		this.queue = queue;
		this.progressBar = progressBar;
		this.converterProgressBarSupplier = converterProgressBarSupplier;
		this.deleteInput = deleteInput;
		this.ffmpegThreads = ffmpegThreads;
		this.dryRun = dryRun;
		
		countDownLatch = new CountDownLatch(1);
		tasks = new ConcurrentLinkedDeque<>();
		shutdown = false;
		pause = false;
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
				while(pause){
					try{
						Thread.sleep(10_000);
					}
					catch(InterruptedException e){
						log.error("Error while sleeping", e);
					}
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
	
	private void processFile(@NonNull FileProber.ProbeResult probeResult){
		var file = probeResult.file();
		var ffProbeResult = probeResult.fFprobeResult();
		var processor = probeResult.processor();
		var outfile = buildOutFile(file, processor.getDesiredExtension());
		
		if(!deleteInput && Files.exists(outfile)){
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
				tempDirectory.resolve("%d-%d-%s".formatted(System.nanoTime(), file.hashCode(), outfile.getFileName())),
				converterProgressBarSupplier,
				deleteInput,
				ffmpegThreads
		);
		tasks.add(task);
		task.execute(executor, dryRun);
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
	
	@Override
	public void resume(){
		pause = false;
	}
	
	@Override
	public void pause(){
		pause = true;
	}
	
	@Override
	public void close(){
		shutdown = true;
		try{
			countDownLatch.await();
		}
		catch(InterruptedException e){
			log.info("Failed to wait for latch", e);
		}
	}
	
	@Override
	@NotNull
	public Queue<?> getOutputQueue(){
		return new LinkedList<>();
	}
	
	public void cancel(){
		tasks.forEach(MediaProcessorTask::cancel);
	}
}

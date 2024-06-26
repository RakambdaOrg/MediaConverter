package fr.rakambda.mediaconverter.file;

import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.IProcessor;
import fr.rakambda.mediaconverter.mediaprocessor.MediaProcessor;
import fr.rakambda.mediaconverter.storage.IStorage;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class FileProber implements Runnable, AutoCloseable, IProcessor{
	private final ProgressBar progressBar;
	private final BlockingQueue<Path> inputQueue;
	@Getter
	private final BlockingQueue<ProbeResult> outputQueue;
	private final Supplier<FFprobe> ffprobeSupplier;
	private final Collection<MediaProcessor> processors;
	private final IStorage storage;
	private final CountDownLatch countDownLatch;
	private boolean shutdown;
	private boolean pause;
	
	public FileProber(@NonNull ProgressBar progressBar,
			@NonNull IStorage storage,
			@NonNull BlockingQueue<Path> inputQueue,
			@NonNull BlockingQueue<ProbeResult> outputQueue,
			@NonNull Supplier<FFprobe> ffprobeSupplier,
			@NonNull Collection<MediaProcessor> processors){
		this.progressBar = progressBar;
		this.inputQueue = inputQueue;
		this.outputQueue = outputQueue;
		this.ffprobeSupplier = ffprobeSupplier;
		this.processors = processors;
		this.storage = storage;
		
		shutdown = false;
		pause = false;
		countDownLatch = new CountDownLatch(1);
	}
	
	@Override
	public void run(){
		try{
			do{
				var file = inputQueue.poll(5, TimeUnit.SECONDS);
				if(Objects.nonNull(file)){
					progressBar.setExtraMessage(file.subpath(file.getNameCount() - 2, file.getNameCount()).toString());
					var probeResult = probeFile(file);
					var processor = getProcessor(probeResult, file);
					if(processor.isPresent()){
						outputQueue.put(new ProbeResult(file, probeResult, processor.get()));
					}
					else{
						storage.setUseless(file);
						progressBar.step();
					}
					progressBar.setExtraMessage("");
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
			while(!shutdown || !inputQueue.isEmpty());
		}
		catch(InterruptedException e){
			log.error("Error waiting for element", e);
		}
		finally{
			countDownLatch.countDown();
		}
	}
	
	@Nullable
	private FFprobeResult probeFile(@NonNull Path file){
		try{
			log.debug("Scanning file {}", file);
			var ffprobe = ffprobeSupplier.get();
			return ffprobe.setShowStreams(true)
					.setShowFormat(true)
					.setInput(file.toString())
					.execute();
		}
		catch(Exception e){
			log.warn("Failed to probe file {}", file);
			return null;
		}
	}
	
	@NonNull
	private Optional<MediaProcessor> getProcessor(@Nullable FFprobeResult probeResult, @NonNull Path file){
		try{
			for(var processor : processors){
				if(processor.canHandle(probeResult, file)){
					log.trace("Processor {} matched", processor.getClass().getSimpleName());
					return Optional.of(processor);
				}
			}
		}
		catch(Exception e){
			log.error("Failed to get processor for file {}", file, e);
		}
		return Optional.empty();
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
	
	public record ProbeResult(
			@NonNull Path file,
			@Nullable FFprobeResult fFprobeResult,
			@NonNull MediaProcessor processor
	){
	}
}

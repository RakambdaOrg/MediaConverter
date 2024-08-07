package fr.rakambda.mediaconverter.file;

import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.mediaprocessor.MediaProcessor;
import fr.rakambda.mediaconverter.storage.IStorage;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class FileProber implements Runnable{
	private final Path file;
	private final ProgressBar progressBar;
	private final Supplier<FFprobe> ffprobeSupplier;
	private final Collection<MediaProcessor> processors;
	private final IStorage storage;
	private final Consumer<ProbeResult> callback;
	
	public FileProber(@NonNull Path file,
			@NonNull ProgressBar progressBar,
			@NonNull IStorage storage,
			@NonNull Supplier<FFprobe> ffprobeSupplier,
			@NonNull Collection<MediaProcessor> processors,
			@NonNull Consumer<ProbeResult> callback
	){
		this.file = file;
		this.progressBar = progressBar;
		this.ffprobeSupplier = ffprobeSupplier;
		this.processors = processors;
		this.storage = storage;
		this.callback = callback;
	}
	
	@Override
	public void run(){
		progressBar.setExtraMessage(file.subpath(file.getNameCount() - 2, file.getNameCount()).toString());
		var probeResult = probeFile(file);
		var processor = getProcessor(probeResult, file);
		if(processor.isPresent()){
			callback.accept(new ProbeResult(file, probeResult, processor.get()));
		}
		else{
			storage.setUseless(file);
			progressBar.step();
		}
		progressBar.setExtraMessage("");
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
	
	public record ProbeResult(
			@NonNull Path file,
			@Nullable FFprobeResult fFprobeResult,
			@NonNull MediaProcessor processor
	){
	}
}

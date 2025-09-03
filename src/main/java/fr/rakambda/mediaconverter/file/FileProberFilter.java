package fr.rakambda.mediaconverter.file;

import fr.rakambda.mediaconverter.config.filter.ProbeFilter;
import fr.rakambda.mediaconverter.file.FileProber.ProbeResult;
import org.jspecify.annotations.NonNull;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
public class FileProberFilter implements Runnable{
	private final ProbeResult probeResult;
	private final ProgressBar progressBar;
	private final Consumer<ProbeResult> callback;
	private final Predicate<ProbeResult> filters;
	
	public FileProberFilter(@NonNull ProbeResult probeResult,
			@NonNull ProgressBar progressBar,
			@NonNull Collection<ProbeFilter> filters,
			@NonNull Consumer<ProbeResult> callback
	){
		this.probeResult = probeResult;
		this.progressBar = progressBar;
		this.callback = callback;
		this.filters = filters.isEmpty() ? (e -> true) : (e -> filters.stream().allMatch(f -> f.test(e)));
	}
	
	@Override
	public void run(){
		if(filters.test(probeResult)){
			callback.accept(probeResult);
		}
		else{
			log.debug("Skipped {} because filters did not pass", probeResult.file());
			progressBar.step();
		}
	}
}

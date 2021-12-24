package fr.raksrinana.mediaconverter.progress;

import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Slf4j
public class ProgressBarSupplier implements AutoCloseable{
	private final Function<Integer, ProgressBar> generator;
	private final AtomicInteger counter;
	
	public ProgressBarSupplier(Function<Integer, ProgressBar> generator){
		this.generator = generator;
		counter = new AtomicInteger(0);
	}
	
	public void addBack(ProgressBar progressBar){
		progressBar.close();
	}
	
	@Override
	public void close(){
	}
	
	public ProgressBarHandle get() throws InterruptedException{
		return new ProgressBarHandle(generator.apply(counter.incrementAndGet()), this);
	}
}

package fr.raksrinana.mediaconverter.progress;

import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
public class ProgressBarSupplier implements AutoCloseable{
	private final int max;
	private final BlockingQueue<ProgressBar> progressBars;
	
	public ProgressBarSupplier(int max, Function<Integer, ProgressBar> generator){
		this.max = max;
		
		progressBars = new LinkedBlockingQueue<>(max);
		for(int i = 0; i < max; i++){
			addBack(generator.apply(i));
		}
	}
	
	public void addBack(ProgressBar progressBar){
		progressBars.offer(progressBar);
	}
	
	@Override
	public void close(){
		try{
			for(int i = 0; i < max; i++){
				
				get().close();
			}
		}
		catch(InterruptedException e){
			log.error("Failed to close converter progress bars", e);
		}
	}
	
	public ProgressBarHandle get() throws InterruptedException{
		return new ProgressBarHandle(progressBars.poll(365, TimeUnit.DAYS), this);
	}
}

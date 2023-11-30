package fr.rakambda.mediaconverter.file;

import fr.rakambda.mediaconverter.config.filter.ProbeFilter;
import fr.rakambda.mediaconverter.file.FileProber.ProbeResult;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FileProberFilter implements Runnable, AutoCloseable{
	private final ProgressBar progressBar;
	private final BlockingQueue<ProbeResult> inputQueue;
	@Getter
	private final BlockingQueue<ProbeResult> outputQueue;
	private final Collection<ProbeFilter> filters;
	private final CountDownLatch countDownLatch;
	private boolean shutdown;
	
	public FileProberFilter(@NonNull ProgressBar progressBar,
			@NonNull BlockingQueue<ProbeResult> inputQueue,
			@NonNull Collection<ProbeFilter> filters){
		this.progressBar = progressBar;
		this.inputQueue = inputQueue;
		this.filters = filters;
		
		outputQueue = new LinkedBlockingDeque<>(50);
		shutdown = false;
		countDownLatch = new CountDownLatch(1);
	}
	
	@Override
	public void run(){
		try{
			do{
				var probeResult = inputQueue.poll(5, TimeUnit.SECONDS);
				if(Objects.nonNull(probeResult)){
					if(filters.stream().allMatch(f -> f.test(probeResult))){
						outputQueue.put(probeResult);
					}
					else{
						log.debug("Skipped {} because filters did not pass", probeResult.file());
						progressBar.step();
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
}

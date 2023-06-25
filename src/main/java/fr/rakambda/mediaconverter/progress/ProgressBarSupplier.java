package fr.rakambda.mediaconverter.progress;

import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ProgressBarSupplier implements AutoCloseable{
	private final ConverterProgressBarGenerator generator;
	private final AtomicInteger counter;
	
	public ProgressBarSupplier(ConverterProgressBarGenerator generator) {
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
		return new ProgressBarHandle(generator.generate(counter.incrementAndGet()), this);
	}
}

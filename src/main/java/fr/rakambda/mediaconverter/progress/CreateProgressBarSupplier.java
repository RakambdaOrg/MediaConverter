package fr.rakambda.mediaconverter.progress;

import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.jspecify.annotations.NonNull;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class CreateProgressBarSupplier implements ProgressBarSupplier{
	private final ConverterProgressBarGenerator generator;
	private final AtomicInteger counter;
	
	public CreateProgressBarSupplier(@NonNull ConverterProgressBarGenerator generator){
		this.generator = generator;
		counter = new AtomicInteger(0);
	}
	
	public void addBack(@NonNull ProgressBar progressBar){
		progressBar.close();
	}
	
	@Override
	public void close(){
	}
	
	@NonNull
	public ProgressBarHandle get(){
		return new ProgressBarHandle(generator.generate(counter.incrementAndGet()), this);
	}
}

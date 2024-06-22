
package fr.rakambda.mediaconverter.progress;

import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.jetbrains.annotations.NotNull;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ReuseProgressBarSupplier implements ProgressBarSupplier{
	private final ConverterProgressBarGenerator generator;
	private final AtomicInteger counter;
	private final Queue<ProgressBar> queue;
	
	public ReuseProgressBarSupplier(@NotNull ConverterProgressBarGenerator generator){
		this.generator = generator;
		this.counter = new AtomicInteger(0);
		this.queue = new ConcurrentLinkedQueue<>();
	}
	
	public void addBack(@NotNull ProgressBar progressBar){
		queue.offer(progressBar
				.setExtraMessage("")
				.maxHint(-1)
				.reset()
				.pause());
	}
	
	@Override
	public void close(){
		queue.forEach(ProgressBar::close);
		queue.clear();
	}
	
	@NotNull
	public ProgressBarHandle get(){
		var progressBar = Optional.ofNullable(queue.poll())
				.orElseGet(() -> generator.generate(counter.incrementAndGet()))
				.resume();
		return new ProgressBarHandle(progressBar, this);
	}
}

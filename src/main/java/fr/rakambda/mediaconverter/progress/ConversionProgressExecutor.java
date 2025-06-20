package fr.rakambda.mediaconverter.progress;

import fr.rakambda.mediaconverter.utils.FutureHelper;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.apache.commons.lang3.NotImplementedException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Log4j2
public class ConversionProgressExecutor implements ExecutorService, AutoCloseable{
	private final ExecutorService delegate;
	private final ProgressBar progressBar;
	private final Semaphore progressBarLock;
	
	public ConversionProgressExecutor(@NonNull ExecutorService executorService){
		delegate = executorService;
		progressBar = new ProgressBarBuilder()
				.setTaskName("Conversion")
				.setInitialMax(-1)
				.build();
		progressBarLock = new Semaphore(1);
	}
	
	public static ConversionProgressExecutor of(@NonNull ExecutorService executorService){
		return new ConversionProgressExecutor(executorService);
	}
	
	@Override
	public void close(){
		try{
			shutdown();
			awaitTermination(30, TimeUnit.DAYS);
		}
		catch(InterruptedException e){
			log.error("Failed to wait for executor to close");
		}
		finally{
			progressBar.close();
		}
	}
	
	@Override
	public void shutdown(){
		delegate.shutdown();
	}
	
	@NonNull
	@Override
	public List<Runnable> shutdownNow(){
		var neverRun = delegate.shutdownNow();
		stepProgressBar(neverRun.size());
		return neverRun;
	}
	
	@Override
	public boolean isShutdown(){
		return delegate.isShutdown();
	}
	
	@Override
	public boolean isTerminated(){
		return delegate.isTerminated();
	}
	
	@Override
	public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException{
		return delegate.awaitTermination(timeout, unit);
	}
	
	@NonNull
	@Override
	public <T> Future<T> submit(@NonNull Callable<T> task){
		incrementProgressbarMax();
		return FutureHelper.makeCompletableFuture(delegate.submit(task)).thenApply(val -> {
			stepProgressBar(1);
			return val;
		});
	}
	
	private void incrementProgressbarMax(){
		try{
			progressBarLock.acquire();
			progressBar.maxHint(progressBar.getMax() + 1);
		}
		catch(InterruptedException e){
			throw new RuntimeException(e);
		}
		finally{
			progressBarLock.release();
		}
	}
	
	private void stepProgressBar(long amount){
		try{
			progressBarLock.acquire();
			progressBar.stepBy(amount);
		}
		catch(InterruptedException e){
			throw new RuntimeException(e);
		}
		finally{
			progressBarLock.release();
		}
	}
	
	private <T> CompletableFuture<T> handleReturn(CompletableFuture<T> future){
		return future
				.exceptionally(throwable -> {
					stepProgressBar(1);
					throw new CompletionException(throwable);
				})
				.thenApply(val -> {
					stepProgressBar(1);
					return val;
				});
	}
	
	@NonNull
	@Override
	public <T> Future<T> submit(@NonNull Runnable task, T result){
		incrementProgressbarMax();
		return handleReturn(FutureHelper.makeCompletableFuture(delegate.submit(task, result)));
	}
	
	@NonNull
	@Override
	public Future<?> submit(@NonNull Runnable task){
		incrementProgressbarMax();
		return handleReturn(FutureHelper.makeCompletableFuture(delegate.submit(task)));
	}
	
	@NonNull
	@Override
	public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks){
		throw new NotImplementedException();
	}
	
	@NonNull
	@Override
	public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit){
		throw new NotImplementedException();
	}
	
	@NonNull
	@Override
	public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks){
		throw new NotImplementedException();
	}
	
	@Override
	public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit){
		throw new NotImplementedException();
	}
	
	@Override
	public void execute(@NonNull Runnable command){
		incrementProgressbarMax();
		handleReturn(FutureHelper.makeCompletableFuture(delegate.submit(command)));
	}
}

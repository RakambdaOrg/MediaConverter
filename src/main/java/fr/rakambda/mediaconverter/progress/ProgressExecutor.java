package fr.rakambda.mediaconverter.progress;

import lombok.NonNull;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.apache.commons.lang3.NotImplementedException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ProgressExecutor implements ExecutorService, AutoCloseable{
	private final ExecutorService delegate;
	private final ProgressBar progressBar;
	
	public ProgressExecutor(ExecutorService executorService){
		delegate = executorService;
		progressBar = new ProgressBarBuilder()
				.setTaskName("Conversion")
				.setInitialMax(-1)
				.build();
	}
	
	public static ProgressExecutor of(ExecutorService executorService){
		return new ProgressExecutor(executorService);
	}
	
	@Override
	public void close() throws InterruptedException{
		shutdown();
		awaitTermination(30, TimeUnit.DAYS);
		progressBar.close();
	}
	
	@Override
	public void shutdown(){
		delegate.shutdown();
	}
	
	@NonNull
	@Override
	public List<Runnable> shutdownNow(){
		var neverRun = delegate.shutdownNow();
		progressBar.stepBy(neverRun.size());
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
		progressBar.maxHint(progressBar.getMax() + 1);
		return makeCompletableFuture(delegate.submit(task)).thenApply(val -> {
			progressBar.step();
			return val;
		});
	}
	
	@NonNull
	@Override
	public <T> Future<T> submit(@NonNull Runnable task, T result){
		progressBar.maxHint(progressBar.getMax() + 1);
		return makeCompletableFuture(delegate.submit(task, result)).thenApply(val -> {
			progressBar.step();
			return val;
		});
	}
	
	@NonNull
	@Override
	public Future<?> submit(@NonNull Runnable task){
		progressBar.maxHint(progressBar.getMax() + 1);
		return makeCompletableFuture(delegate.submit(task)).thenApply(val -> {
			progressBar.step();
			return val;
		});
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
	
	public static <T> CompletableFuture<T> makeCompletableFuture(Future<T> future){
		if(future.isDone()){
			return transformDoneFuture(future);
		}
		return CompletableFuture.supplyAsync(() -> {
			try{
				if(!future.isDone()){
					awaitFutureIsDoneInForkJoinPool(future);
				}
				return future.get();
			}
			catch(ExecutionException e){
				throw new RuntimeException(e);
			}
			catch(InterruptedException e){
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
		});
	}
	
	private static <T> CompletableFuture<T> transformDoneFuture(Future<T> future){
		var cf = new CompletableFuture<T>();
		try{
			var result = future.get();
			cf.complete(result);
			return cf;
		}
		catch(Throwable ex){
			cf.completeExceptionally(ex);
			return cf;
		}
	}
	
	private static void awaitFutureIsDoneInForkJoinPool(Future<?> future) throws InterruptedException{
		ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker(){
			@Override
			public boolean block() throws InterruptedException{
				try{
					future.get();
				}
				catch(ExecutionException e){
					throw new RuntimeException(e);
				}
				return true;
			}
			
			@Override
			public boolean isReleasable(){
				return future.isDone();
			}
		});
	}
	
	@Override
	public void execute(@NonNull Runnable command){
		throw new NotImplementedException();
	}
}

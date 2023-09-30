package fr.rakambda.mediaconverter.utils;

import lombok.NonNull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public class FutureHelper{
	@NonNull
	public static <T> CompletableFuture<T> makeCompletableFuture(@NonNull Future<T> future){
		if(future instanceof CompletableFuture<T> completableFuture){
			return completableFuture;
		}
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
	
	@NonNull
	private static <T> CompletableFuture<T> transformDoneFuture(@NonNull Future<T> future){
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
	
	private static void awaitFutureIsDoneInForkJoinPool(@NonNull Future<?> future) throws InterruptedException{
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
}

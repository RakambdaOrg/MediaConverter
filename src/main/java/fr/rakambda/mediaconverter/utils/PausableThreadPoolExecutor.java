package fr.rakambda.mediaconverter.utils;

import org.jspecify.annotations.NonNull;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PausableThreadPoolExecutor extends ThreadPoolExecutor{
	private Continue aContinue;
	
	public PausableThreadPoolExecutor(int corePoolSize, @NonNull Continue aContinue){
		super(corePoolSize, corePoolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
		this.aContinue = aContinue;
	}
	
	protected void beforeExecute(@NonNull Thread thread, @NonNull Runnable runnable){
		try{
			aContinue.checkIn();
		}
		catch(InterruptedException e){
			throw new RuntimeException(e);
		}
		super.beforeExecute(thread, runnable);
	}
}

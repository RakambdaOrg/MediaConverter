package fr.rakambda.mediaconverter.mediaprocessor;

import org.jspecify.annotations.NonNull;
import java.io.Closeable;
import java.util.concurrent.ExecutorService;

public interface MediaProcessorTask extends Closeable{
	void addCompletionListener(@NonNull Runnable listener);
	
	void execute(@NonNull ExecutorService executor, boolean dryRun);
	
	void cancel();
}

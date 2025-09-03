package fr.rakambda.mediaconverter.progress;

import me.tongfei.progressbar.ProgressBar;
import org.jspecify.annotations.NonNull;

public interface ProgressBarSupplier extends AutoCloseable{
	void addBack(@NonNull ProgressBar progressBar);
	
	@NonNull
	ProgressBarHandle get();
}

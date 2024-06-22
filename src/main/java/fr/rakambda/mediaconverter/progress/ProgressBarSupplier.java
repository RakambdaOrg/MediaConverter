package fr.rakambda.mediaconverter.progress;

import me.tongfei.progressbar.ProgressBar;
import org.jetbrains.annotations.NotNull;

public interface ProgressBarSupplier extends AutoCloseable{
	void addBack(@NotNull ProgressBar progressBar);
	
	@NotNull
	ProgressBarHandle get();
}

package fr.rakambda.mediaconverter.mediaprocessor;

import lombok.NonNull;

public interface MediaProcessorTask extends Runnable{
	void addCompletionListener(@NonNull Runnable listener);
}

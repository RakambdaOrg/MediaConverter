package fr.rakambda.mediaconverter.mediaprocessor;

public interface MediaProcessorTask extends Runnable{
	void addCompletionListener(Runnable listener);
}

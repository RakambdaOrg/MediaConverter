package fr.raksrinana.mediaconverter.mediaprocessor;

public interface MediaProcessorTask extends Runnable{
	void addCompletionListener(Runnable listener);
}

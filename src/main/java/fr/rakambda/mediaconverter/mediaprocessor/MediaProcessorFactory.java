package fr.rakambda.mediaconverter.mediaprocessor;

import fr.rakambda.mediaconverter.config.Processor;
import org.jetbrains.annotations.NotNull;

public class MediaProcessorFactory{
	private static final VideoToHevcMediaProcessor VIDEO = new VideoToHevcMediaProcessor();
	private static final AudioToAacMediaProcessor AUDIO = new AudioToAacMediaProcessor();
	private static final TiffToJpegMediaProcessor TIFF = new TiffToJpegMediaProcessor();
	
	@NotNull
	public static MediaProcessor getMediaProcessor(@NotNull Processor processor){
		return switch(processor){
			case VIDEO -> VIDEO;
			case AUDIO -> AUDIO;
			case TIFF -> TIFF;
		};
	}
}

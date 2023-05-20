package fr.rakambda.mediaconverter.mediaprocessor;

import fr.rakambda.mediaconverter.config.Processor;
import org.jetbrains.annotations.NotNull;

public class MediaProcessorFactory{
	@NotNull
	public static MediaProcessor getMediaProcessor(@NotNull Processor processor){
		return switch(processor){
			case VIDEO_HEVC -> new VideoToHevcMediaProcessor();
			case VIDEO_AV1 -> new VideoToAv1MediaProcessor();
			case AUDIO -> new AudioToAacMediaProcessor();
			case TIFF -> new TiffToJpegMediaProcessor();
		};
	}
}

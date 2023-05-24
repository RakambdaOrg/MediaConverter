package fr.rakambda.mediaconverter.mediaprocessor;

import fr.rakambda.mediaconverter.config.Processor;
import org.jetbrains.annotations.NotNull;

public class MediaProcessorFactory{
	@NotNull
	public static MediaProcessor getMediaProcessor(@NotNull Processor processor){
		return switch(processor){
			case VIDEO_TO_HEVC -> new VideoToHevcMediaProcessor();
			case VIDEO_TO_AV1 -> new VideoToAv1MediaProcessor();
			case MP3_TO_AAC -> new Mp3ToAacMediaProcessor();
			case AUDIO_TO_OPUS -> new AudioToOpusMediaProcessor();
			case TIFF_TO_JPG -> new TiffToJpegMediaProcessor();
			case PHOTO_TO_AVIF -> new PhotoToAvifMediaProcessor();
			case PHOTO_TO_JXL -> new PhotoToJxlMediaProcessor();
		};
	}
}

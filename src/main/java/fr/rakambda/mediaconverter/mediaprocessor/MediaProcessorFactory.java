package fr.rakambda.mediaconverter.mediaprocessor;

import fr.rakambda.mediaconverter.config.Processor;
import org.jspecify.annotations.NonNull;

public class MediaProcessorFactory{
	@NonNull
	public static MediaProcessor getMediaProcessor(@NonNull Processor processor){
		return switch(processor){
			case VIDEO_TO_HEVC_MP4 -> new VideoToHevcMp4MediaProcessor();
			case VIDEO_TO_HEVC_MKV -> new VideoToHevcMkvMediaProcessor();
			case VIDEO_TO_AV1_MKV -> new VideoToAv1MediaProcessor();
			case MP3_TO_AAC -> new Mp3ToAacMediaProcessor();
			case AUDIO_TO_OPUS -> new AudioToOpusMediaProcessor();
			case TIFF_TO_JPG -> new TiffToJpegMediaProcessor();
			case PHOTO_TO_AVIF -> new PhotoToAvifMediaProcessor();
			case PHOTO_TO_JXL -> new PhotoToJxlMediaProcessor();
		};
	}
}

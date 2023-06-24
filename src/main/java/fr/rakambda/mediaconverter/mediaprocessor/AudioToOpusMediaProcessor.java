package fr.rakambda.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.itemprocessor.OpusConverter;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class AudioToOpusMediaProcessor implements MediaProcessor{
	private static final List<String> CODECS = List.of("mp3", "aac");
	
	@Override
	public boolean canHandle(FFprobeResult probeResult, Path file){
		if(Objects.isNull(probeResult)){
			return false;
		}
		return probeResult.getStreams().size() == 1
				&& probeResult.getStreams().stream()
				.allMatch(stream -> CODECS.contains(stream.getCodecName()));
	}
	
	@Override
	public MediaProcessorTask createConvertTask(FFmpeg ffmpeg, FFprobeResult probeResult, Path input, Path output, Path temporary, ProgressBarSupplier converterProgressBarSupplier){
		return new OpusConverter(ffmpeg, probeResult, input, output, temporary, converterProgressBarSupplier);
	}
	
	@Override
	public String getDesiredExtension(){
		return "opus";
	}
}

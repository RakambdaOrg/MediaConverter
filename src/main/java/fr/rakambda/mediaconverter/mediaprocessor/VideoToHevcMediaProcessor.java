package fr.rakambda.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.itemprocessor.HevcConverter;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class VideoToHevcMediaProcessor implements MediaProcessor{
	private static final List<String> CODECS = List.of("h264", "vp9", "wmv3", "mpeg2video");
	
	@Override
	public boolean canHandle(FFprobeResult probeResult, Path file){
		if(Objects.isNull(probeResult)){
			return false;
		}
		return probeResult.getStreams().stream()
				.anyMatch(stream -> isWantedCodec(stream.getCodecName()) || isOtherHevc(stream.getCodecName(), stream.getCodecTagString()));
	}
	
	private boolean isWantedCodec(String codecName){
		return CODECS.contains(codecName);
	}
	
	private boolean isOtherHevc(String codecName, String codecTagString){
		return "hevc".equals(codecName) && "hvc1".equals(codecTagString);
	}
	
	@Override
	public MediaProcessorTask createConvertTask(FFmpeg ffmpeg, FFprobeResult probeResult, Path input, Path output, Path temporary, ProgressBarSupplier progressBarSupplier){
		return new HevcConverter(ffmpeg, probeResult, input, output, temporary, progressBarSupplier);
	}
	
	@Override
	public String getDesiredExtension(){
		return "mkv";
	}
}

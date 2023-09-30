package fr.rakambda.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.itemprocessor.command.JxlConverter;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;

public class PhotoToJxlMediaProcessor extends PhotoMediaProcessor{
	@Override
	@NonNull
	public MediaProcessorTask createConvertTask(@NonNull FFmpeg ffmpeg, @Nullable FFprobeResult probeResult, @NonNull Path input, @NonNull Path output, @NonNull Path temporary, @NonNull ProgressBarSupplier converterProgressBarSupplier, boolean deleteInput){
		return new JxlConverter(input, output, temporary, deleteInput, converterProgressBarSupplier);
	}
	
	@Override
	@NonNull
	public String getDesiredExtension(){
		return "jxl";
	}
}

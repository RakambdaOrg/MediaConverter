package fr.rakambda.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.util.Locale;

public abstract class PhotoMediaProcessor implements MediaProcessor{
	@Override
	public boolean canHandle(@Nullable FFprobeResult probeResult, @NotNull Path file){
		var filename = file.getFileName().toString().toLowerCase(Locale.ROOT);
		if(filename.endsWith("." + getDesiredExtension())){
			return false;
		}
		return filename.endsWith(".heic") || filename.endsWith(".tiff") || filename.endsWith(".jpg") || filename.endsWith(".raf");
	}
}

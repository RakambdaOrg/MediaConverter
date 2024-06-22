package fr.rakambda.mediaconverter.mediaprocessor;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class VideoToHevcMkvMediaProcessor extends VideoToHevcMediaProcessor{
	@Override
	@NonNull
	public String getDesiredExtension(){
		return "mkv";
	}
	
	@Override
	@NotNull
	protected String getDesiredFormat(){
		return "matroska,webm";
	}
}

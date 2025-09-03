package fr.rakambda.mediaconverter.mediaprocessor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NonNull;

public class VideoToHevcMkvMediaProcessor extends VideoToHevcMediaProcessor{
	@Override
	@NonNull
	public String getDesiredExtension(){
		return "mkv";
	}
	
	@Override
	@NonNull
	protected String getDesiredFormat(){
		return "matroska,webm";
	}
}

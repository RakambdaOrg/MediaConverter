package fr.rakambda.mediaconverter.mediaprocessor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NonNull;

public class VideoToHevcMp4MediaProcessor extends VideoToHevcMediaProcessor{
	@Override
	@NonNull
	public String getDesiredExtension(){
		return "mp4";
	}
	
	@Override
	@NonNull
	protected String getDesiredFormat(){
		return "mov,mp4,m4a,3gp,3g2,mj2";
	}
}

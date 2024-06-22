package fr.rakambda.mediaconverter.mediaprocessor;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class VideoToHevcMp4MediaProcessor extends VideoToHevcMediaProcessor{
	@Override
	@NonNull
	public String getDesiredExtension(){
		return "mp4";
	}
	
	@Override
	@NotNull
	protected String getDesiredFormat(){
		return "mov,mp4,m4a,3gp,3g2,mj2";
	}
}

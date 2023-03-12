package fr.rakambda.mediaconverter.ffmpeg;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.process.Stopper;
import java.nio.file.Path;

public class CustomFFmpeg extends FFmpeg{
	private final Integer affinityMask;
	
	public CustomFFmpeg(Path executable){
		this(executable, null);
	}
	
	public CustomFFmpeg(Path executable, Integer affinityMask){
		super(executable);
		this.affinityMask = affinityMask;
	}
	
	@Override
	protected Stopper createStopper(){
		return new CustomStopper(super.createStopper(), affinityMask);
	}
}

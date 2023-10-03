package fr.rakambda.mediaconverter.ffmpeg;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.process.Stopper;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.util.List;

public class CustomFFmpeg extends FFmpeg{
	private final Integer affinityMask;
	
	public CustomFFmpeg(@NonNull Path executable){
		this(executable, null);
	}
	
	public CustomFFmpeg(@NonNull Path executable, @Nullable Integer affinityMask){
		super(executable);
		this.affinityMask = affinityMask;
	}
	
	@Override
	@NonNull
	protected Stopper createStopper(){
		return new CustomStopper(super.createStopper(), affinityMask);
	}
	
	@Override
	public List<String> buildArguments(){
		return super.buildArguments();
	}
}

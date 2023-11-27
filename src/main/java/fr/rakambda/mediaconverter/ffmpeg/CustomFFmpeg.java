package fr.rakambda.mediaconverter.ffmpeg;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.process.Stopper;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class CustomFFmpeg extends FFmpeg{
	private final Integer affinityMask;
	private final Path executable;
	
	public CustomFFmpeg(@NonNull Path executable){
		this(executable, null);
	}
	
	public CustomFFmpeg(@NonNull Path executable, @Nullable Integer affinityMask){
		super(executable);
		this.executable = executable;
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
	
	@NotNull
	public String getCommand(){
		return "%s %s".formatted(
				executable.toString(),
				buildArguments().stream()
						.map("\"%s\""::formatted)
						.collect(Collectors.joining(" "))
		);
	}
}

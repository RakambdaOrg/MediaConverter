package fr.rakambda.mediaconverter.utils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.nio.file.Path;
import java.nio.file.Paths;

@NoArgsConstructor
@Getter
@Command(name = "mediaconverter", mixinStandardHelpOptions = true)
public class CLIParameters{
	@Option(names = {
			"-c",
			"--config"
	},
			description = "The path to the configuration file",
			required = true)
	public Path configuration = Paths.get("config.json");
	@Option(names = {
			"-t",
			"--threads"
	}, description = "The number of threads to use (must be >= 1)")
	private int threadCount = 1;
	@Option(names = {
			"-ft",
			"--ffmpegThreads"
	}, description = "The number of threads to tell ffmpeg to use (per job) (must be >= 1)")
	private Integer ffmpegThreadCount = null;
	
	@Option(names = {"--ffprobe"}, description = "The path to ffprobe executable")
	private Path ffprobePath;
	@Option(names = {"--ffmpeg"}, description = "The path to ffmpeg executable")
	private Path ffmpegPath;
	
	@Option(names = {
			"--affinity"
	}, description = "Set an affinity mask for windows users to define on which cores to run on. THis consists on a number where bytes set to 1 activates the core. For ex 7 will run on cores 1, 2, 3.")
	private Integer affinityMask = null;
}

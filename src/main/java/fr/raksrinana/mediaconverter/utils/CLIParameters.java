package fr.raksrinana.mediaconverter.utils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
@Getter
@Command(name = "mediaconverter", mixinStandardHelpOptions = true)
public class CLIParameters{
	@CommandLine.Option(names = {
			"-t",
			"--threads"
	}, description = "The number of threads to use (must be >= 1)")
	private int threadCount = 1;
	@CommandLine.Option(names = {
			"-ft",
			"--ffmpegThreads"
	}, description = "The number of threads to tell ffmpeg to use (per job) (must be >= 1)")
	private Integer ffmpegThreadCount = null;
	@Option(names = {"--input-folder"}, description = "The folder to scan media", required = true)
	private Path input;
	@Option(names = {"--output-folder"}, description = "The folder to put converted medias", required = true)
	private Path output;
	@Option(names = {"--excluded-folder"}, description = "Folders to be excluded from scanning")
	private List<Path> excluded;
	@Option(names = {"--temp-folder"}, description = "The folder to put medias that are being converted")
	private Path temp;
	@Option(names = {"--ffprobe"}, description = "The path to ffprobe executable")
	private Path ffprobePath;
	@Option(names = {"--ffmpeg"}, description = "The path to ffmpeg executable")
	private Path ffmpegPath;
	@Option(names = {"--config-db"}, description = "The path to the db file")
	private Path databasePath;
	
	public Path createTempDirectory() throws IOException{
		var prefix = "MediaConverter";
		return Objects.isNull(temp) ? Files.createTempDirectory(prefix) : Files.createTempDirectory(temp, prefix);
	}
	
	public Set<Path> getAbsoluteExcluded(){
		if(Objects.isNull(getExcluded())){
			return Set.of();
		}
		return getExcluded().stream()
				.map(getInput()::resolve)
				.collect(Collectors.toSet());
	}
}

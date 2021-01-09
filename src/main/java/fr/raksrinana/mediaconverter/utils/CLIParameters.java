package fr.raksrinana.mediaconverter.utils;

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
	@Option(names = {"--input-folder"}, description = "The folder to scan media", required = true)
	private Path input;
	@Option(names = {"--output-folder"}, description = "The folder to put converted medias", required = true)
	private Path output;
	@Option(names = {"--ffprobe"}, description = "The path to ffprobe executable")
	private String ffprobePath = "ffprobe";
	@Option(names = {"--ffmpeg"}, description = "The path to ffmpeg executable")
	private String ffmpegPath = "ffmpeg";
	@Option(names = {"--config-db"}, description = "The path to the db file")
	private Path databasePath = Paths.get("MediaConverter.db");
}

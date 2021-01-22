package fr.raksrinana.mediaconverter.utils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@NoArgsConstructor
@Getter
@Command(name = "mediaconverter", mixinStandardHelpOptions = true)
public class CLIParameters{
	@Option(names = {"--input-folder"}, description = "The folder to scan media", required = true)
	private Path input;
	@Option(names = {"--output-folder"}, description = "The folder to put converted medias", required = true)
	private Path output;
	@Option(names = {"--temp-folder"}, description = "The folder to put medias that are being converted")
	private Path temp;
	@Option(names = {"--ffprobe"}, description = "The path to ffprobe executable")
	private Path ffprobePath;
	@Option(names = {"--ffmpeg"}, description = "The path to ffmpeg executable")
	private Path ffmpegPath;
	@Option(names = {"--config-db"}, description = "The path to the db file")
	private Path databasePath = Paths.get("MediaConverter.db");
	
	public Path createTempDirectory() throws IOException{
		var prefix = "MediaConverter";
		return Objects.isNull(temp) ? Files.createTempDirectory(prefix) : Files.createTempDirectory(temp, prefix);
	}
}

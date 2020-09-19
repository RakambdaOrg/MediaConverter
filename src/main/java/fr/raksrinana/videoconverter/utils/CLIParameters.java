package fr.raksrinana.videoconverter.utils;

import fr.raksrinana.videoconverter.itemprocessor.ItemProcessor;
import fr.raksrinana.videoconverter.itemprocessor.PS1ItemProcessor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import picocli.CommandLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@NoArgsConstructor
@Getter
@CommandLine.Command(name = "videoconverter", mixinStandardHelpOptions = true)
public class CLIParameters{
	@CommandLine.Option(names = {
			"--input-folder-host"
	}, description = "The folder to scan videos for on the host machine", required = true)
	private Path inputHost;
	@CommandLine.Option(names = {
			"--output-folder-host"
	}, description = "The folder to put converted videos on the host machine", required = true)
	private Path outputHost;
	@CommandLine.Option(names = {
			"--batch-folder-host"
	}, description = "The folder to put batches on the host machine")
	private Path batchHost = Paths.get("videoconverter");
	@CommandLine.Option(names = {
			"--input-folder-client"
	}, description = "The folder to scan videos for on the client machine")
	private Path inputClient;
	@CommandLine.Option(names = {
			"--batch-folder-client"
	}, description = "The folder to put batches on the client machine")
	private Path batchClient = Paths.get("videoconverter");
	@CommandLine.Option(names = {
			"--ffprobe"
	}, description = "The path to ffprobe executable")
	private String ffprobePath = "ffprobe";
	@CommandLine.Option(names = {
			"--ffmpeg"
	}, description = "The path to ffmpeg executable")
	private String ffmpegPath = "ffmpeg";
	@CommandLine.Option(names = {
			"--config-db"
	}, description = "The path to the db file")
	private Path databasePath = Paths.get("VideoNormalizer.db");
	@CommandLine.Option(names = {
			"--processor"
	}, description = "The name of the item processor class to use (default is fr.raksrinana.videoconverter.itemprocessor.PS1Itemprocessor")
	@Getter
	private Class<? extends ItemProcessor> itemProcessor = PS1ItemProcessor.class;
	@CommandLine.Option(names = {
			"--parallel"
	})
	private boolean runningParallel = false;
	private Path tempDirectory;
	
	public Path getInputClient(){
		return Objects.isNull(inputClient) ? getInputHost() : inputClient;
	}
	
	public Path getTempDirectory() throws IOException{
		if(Objects.isNull(tempDirectory)){
			tempDirectory = Files.createTempDirectory("VideoConverter");
		}
		return tempDirectory;
	}
}

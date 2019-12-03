package fr.raksrinana.videoconverter.utils;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.PathConverter;
import fr.raksrinana.videoconverter.itemprocessor.ItemProcessor;
import fr.raksrinana.videoconverter.itemprocessor.PS1ItemProcessor;
import lombok.Getter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class CLIParameters{
	@Parameter(names = {
			"--input-folder-host"
	}, description = "The folder to scan videos for on the host machine", converter = PathConverter.class, required = true)
	@Getter
	private Path inputHost;
	@Parameter(names = {
			"--output-folder-host"
	}, description = "The folder to put converted videos on the host machine", converter = PathConverter.class, required = true)
	@Getter
	private Path outputHost;
	@Parameter(names = {
			"--batch-folder-host"
	}, description = "The folder to put batches on the host machine", converter = PathConverter.class)
	@Getter
	private Path batchHost = Paths.get("videoconverter");
	@Parameter(names = {
			"--input-folder-client"
	}, description = "The folder to scan videos for on the client machine", converter = PathConverter.class)
	private Path inputClient;
	@Parameter(names = {
			"--batch-folder-client"
	}, description = "The folder to put batches on the client machine", converter = PathConverter.class)
	@Getter
	private Path batchClient = Paths.get("videoconverter");
	@Parameter(names = {
			"--ffprobe"
	}, description = "The path to ffprobe executable")
	@Getter
	private String ffprobePath = "ffprobe";
	@Parameter(names = {
			"--ffmpeg"
	}, description = "The path to ffmpeg executable")
	@Getter
	private String ffmpegPath = "ffmpeg";
	@Parameter(names = {
			"--config-db"
	}, description = "The path to the db file", converter = PathConverter.class)
	@Getter
	private Path databasePath = Paths.get("VideoNormalizer.db");
	@Parameter(names = {
			"-h",
			"--help"
	}, help = true)
	private boolean help = false;
	@Parameter(names = {
			"--processor"
	}, description = "The name of the item processor class to use (default is fr.raksrinana.videoconverter.itemprocessor.PS1Itemprocessor", converter = ClassConverter.class)
	@Getter
	private Class<? extends ItemProcessor> itemProcessor = PS1ItemProcessor.class;
	@Parameter(names = {
			"--parallel"
	}, help = true)
	@Getter
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

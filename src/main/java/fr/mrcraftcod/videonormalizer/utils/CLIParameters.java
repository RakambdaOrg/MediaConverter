package fr.mrcraftcod.videonormalizer.utils;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.PathConverter;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CLIParameters{
	@Parameter(names = {
			"--input-folder-host"
	}, description = "The folder to scan videos for on the host machine", converter = PathConverter.class, required = true)
	private Path inputHost;
	
	@Parameter(names = {
			"--output-folder-host"
	}, description = "The folder to put converted videos on the host machine", converter = PathConverter.class, required = true)
	private Path outputHost;
	
	@Parameter(names = {
			"--batch-folder-host"
	}, description = "The folder to put batches on the host machine", converter = PathConverter.class, required = true)
	private Path batchHost;
	
	@Parameter(names = {
			"--input-folder-client"
	}, description = "The folder to scan videos for on the client machine", converter = PathConverter.class, required = true)
	private Path inputClient;
	
	@Parameter(names = {
			"--batch-folder-client"
	}, description = "The folder to put batches on the client machine", converter = PathConverter.class, required = true)
	private Path batchClient;
	
	@Parameter(names = {
			"--ffprobe"
	}, description = "The folder to ffprobe executable")
	private String ffprobePath = "ffprobe";
	@Parameter(names = {
			"--config-db"
	}, description = "The path to the db file", converter = PathConverter.class)
	private Path configPath = Paths.get("VideoNormalizer.db");
	
	@Parameter(names = {
			"-h",
			"--help"
	}, help = true)
	private boolean help = false;
	
	public String getFfprobePath(){
		return ffprobePath;
	}
	
	public Path getInputHost(){
		return inputHost;
	}
	
	public Path getOutputHost(){
		return outputHost;
	}
	
	public Path getBatchHost(){
		return batchHost;
	}
	
	public Path getInputClient(){
		return inputClient;
	}
	
	public Path getBatchClient(){
		return batchClient;
	}
}

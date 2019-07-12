package fr.mrcraftcod.videonormalizer;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.PathConverter;
import java.nio.file.Path;

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
			"--output-folder-client"
	}, description = "The folder to put converted videos on the client machine", converter = PathConverter.class, required = true)
	private Path outputClient;
	
	@Parameter(names = {
			"--batch-folder-client"
	}, description = "The folder to put batches on the client machine", converter = PathConverter.class, required = true)
	private Path batchClient;
	
	@Parameter(names = {
			"--ffprobe"
	}, description = "The folder to ffprobe executable")
	private String ffprobePath;
	
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
	
	public Path getOutputClient(){
		return outputClient;
	}
	
	public Path getBatchClient(){
		return batchClient;
	}
}

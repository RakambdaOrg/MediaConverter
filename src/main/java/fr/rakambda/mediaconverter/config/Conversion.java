package fr.rakambda.mediaconverter.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.ext.NioPathDeserializer;
import fr.rakambda.mediaconverter.mediaprocessor.MediaProcessor;
import fr.rakambda.mediaconverter.mediaprocessor.MediaProcessorFactory;
import fr.rakambda.mediaconverter.storage.H2Storage;
import fr.rakambda.mediaconverter.storage.IStorage;
import fr.rakambda.mediaconverter.storage.NoOpStorage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@NoArgsConstructor
public class Conversion{
	private static final String TEMPORARY_DIRECTORY_PREFIX = "MediaConverter";
	private static final Collection<String> DEFAULT_EXTENSIONS_TO_SCAN = List.of(
			"mp4",
			"mov",
			"mkv",
			"avi",
			"tiff",
			"mp3",
			"m4a",
			"m4v",
			"ts",
			"m2ts",
			"mts",
			"heic"
	);
	
	@JsonProperty("input")
	@JsonDeserialize(using = NioPathDeserializer.class)
	private Path input;
	@JsonProperty("output")
	@JsonDeserialize(using = NioPathDeserializer.class)
	private Path output;
	@JsonProperty("database")
	@JsonDeserialize(using = NioPathDeserializer.class)
	private Path database;
	@JsonProperty("temp")
	@JsonDeserialize(using = NioPathDeserializer.class)
	private Path temp;
	@JsonProperty("excluded")
	@JsonDeserialize(contentUsing = NioPathDeserializer.class)
	private List<Path> excluded = new LinkedList<>();
	@JsonProperty("processors")
	private List<Processor> processors = new LinkedList<>();
	@JsonProperty("extensions")
	private List<String> extensions = new LinkedList<>();

	@NonNull
	public Path createTempDirectory() throws IOException{
		return isNull(getTemp()) ? Files.createTempDirectory(TEMPORARY_DIRECTORY_PREFIX) : Files.createTempDirectory(getTemp(), TEMPORARY_DIRECTORY_PREFIX);
	}

	@NonNull
	public Set<Path> getAbsoluteExcluded(){
		if(isNull(getExcluded())){
			return Set.of();
		}
		return getExcluded().stream()
				.map(getInput()::resolve)
				.collect(Collectors.toSet());
	}

	@NonNull
	public Collection<String> getExtensions(){
		if(isNull(extensions) || extensions.isEmpty()){
			return DEFAULT_EXTENSIONS_TO_SCAN;
		}
		return extensions;
	}

	@NonNull
	public List<MediaProcessor> getProcessors(){
		if(processors.isEmpty()){
			processors.addAll(List.of(
					Processor.VIDEO_TO_HEVC,
					Processor.MP3_TO_AAC,
					Processor.TIFF_TO_JPG
			));
		}
		
		return processors.stream()
				.map(MediaProcessorFactory::getMediaProcessor)
				.collect(Collectors.toList());
	}

	@NonNull
	public IStorage getStorage() throws SQLException, IOException{
		if(isNull(getDatabase())){
			return new NoOpStorage();
		}
		return new H2Storage(getDatabase());
	}
}

package fr.rakambda.mediaconverter.config.filter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Format;
import fr.rakambda.mediaconverter.file.FileProber;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("LengthLessThan")
@NoArgsConstructor
public class LengthLessThan implements ProbeFilter{
	@JsonProperty(value = "value", required = true)
	private long value = Long.MAX_VALUE;
	
	@Override
	public boolean test(FileProber.@NonNull ProbeResult probeResult){
		return Optional.ofNullable(probeResult.fFprobeResult())
				.map(FFprobeResult::getFormat)
				.map(Format::getDuration)
				.map(d -> d <= value)
				.orElse(false);
	}
}

package fr.rakambda.mediaconverter.config.filter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import fr.rakambda.mediaconverter.file.FileProber;
import lombok.NonNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "name")
@JsonSubTypes(value = {
		@JsonSubTypes.Type(value = LengthLessThan.class, name = "LengthLessThan"),
		@JsonSubTypes.Type(value = LengthMoreThan.class, name = "LengthMoreThan"),
})
public interface ProbeFilter{
	boolean test(@NonNull FileProber.ProbeResult probeResult);
}

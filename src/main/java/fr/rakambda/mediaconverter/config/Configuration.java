package fr.rakambda.mediaconverter.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Slf4j
@NoArgsConstructor
public class Configuration {
    private static final ObjectReader objectReader;

    static {
        var mapper = new ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig()
                .getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectReader = mapper.readerFor(Configuration.class);
    }

    @JsonProperty("conversions")
    private List<Conversion> conversions = new LinkedList<>();

    @NonNull
    public static Optional<Configuration> loadConfiguration(@NonNull Path path) {
        if (Files.isRegularFile(path)) {
            try (var fis = Files.newBufferedReader(path)) {
                return Optional.ofNullable(objectReader.readValue(fis));
            } catch (IOException e) {
                log.error("Failed to read settings in {}", path, e);
            }
        }
        return Optional.empty();
    }
}
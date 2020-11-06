package fr.raksrinana.videoconverter.itemprocessor;

import fr.raksrinana.videoconverter.itemprocessor.ffmpeg.ProgressBarNotifier;
import fr.raksrinana.videoconverter.utils.CLIParameters;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Requires the Recycle module to be installed: https://www.powershellgallery.com/packages/Recycle/1.0.2
 */
@Slf4j
public class FfmpegItemProcessor implements ItemProcessor {
    private static final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    @Override
    public boolean create(@NonNull CLIParameters params, @NonNull FFmpegProbeResult probeResult, @NonNull FFmpegStream stream, @NonNull Path inputHost, @NonNull Path outputHost, @NonNull Path batchHost, @NonNull Path batchClient) {
        final var filename = outputHost.getFileName().toString();
        final var cut = filename.lastIndexOf(".");
        outputHost = outputHost.getParent().resolve((cut >= 0 ? filename.substring(0, cut) : filename) + ".mp4");
        final var duration = Duration.ofSeconds((long) probeResult.format.duration);
        final var durationStr = String.format("%dh%dm%s", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
        log.info("Converting {} ({}) to {}", inputHost, durationStr, outputHost);
        if (!Files.exists(outputHost.getParent())) {
            try {
                Files.createDirectories(outputHost.getParent());
            } catch (IOException e) {
                log.error("Failed to create directory {}", outputHost.getParent(), e);
            }
        }
        try {
            synchronized (locks.computeIfAbsent(outputHost.getFileName().toString(), name -> new Object())) {
                final var tempFile = params.getTempDirectory().resolve(outputHost.getFileName());
                log.debug("Will convert to temp file {}", tempFile);
                final var ffmpeg = new FFmpeg(params.getFfmpegPath());
                final var ffmpegOptions = ffmpeg.builder()
                        .addInput(probeResult)
                        .overrideOutputFiles(false)
                        .addOutput(tempFile.toAbsolutePath().normalize().toString())
                        .setAudioBitRate(128000)
                        .setAudioCodec("aac")
                        .setVideoCodec("libx265")
                        .setPreset("medium")
                        .setConstantRateFactor(23d)
                        .setVideoMovFlags("use_metadata_tags")
                        .addExtraArgs("-map_metadata", "0")
                        .addExtraArgs("-max_muxing_queue_size", "512")
                        .done();
                final var frameCount = probeResult.getStreams().stream().mapToLong(s -> s.nb_frames).max().orElse(0);
                ffmpeg.run(ffmpegOptions, new ProgressBarNotifier(filename, frameCount, durationStr));
                if (Files.exists(tempFile)) {
                    Files.move(tempFile, outputHost);
                    final var baseAttributes = Files.getFileAttributeView(inputHost, BasicFileAttributeView.class).readAttributes();
                    final var attributes = Files.getFileAttributeView(outputHost, BasicFileAttributeView.class);
                    attributes.setTimes(baseAttributes.lastModifiedTime(), baseAttributes.lastAccessTime(), baseAttributes.creationTime());
                    var trashed = false;
                    if (Desktop.isDesktopSupported()) {
                        final var desktop = Desktop.getDesktop();
                        if (desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)) {
                            if (trashed = desktop.moveToTrash(inputHost.toFile())) {
                                log.info("Moved input file {} to trash", inputHost);
                            }
                        }
                    }
                    if (!trashed && Files.deleteIfExists(inputHost)) {
                        log.info("Deleted input file {}", inputHost);
                    }
                    log.info("Converted {} to {}", inputHost, outputHost);
                    return true;
                } else {
                    log.warn("Output file {} not found, something went wrong", outputHost);
                    return false;
                }
            }
        } catch (IOException e) {
            log.error("Failed to run ffmpeg on {}", inputHost, e);
        }
        return false;
    }
}

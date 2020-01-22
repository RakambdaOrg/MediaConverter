package fr.raksrinana.videoconverter.itemprocessor;

import fr.raksrinana.videoconverter.utils.CLIParameters;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;

/**
 * Requires the Recycle module to be installed: https://www.powershellgallery.com/packages/Recycle/1.0.2
 */
@Slf4j
public class PS1ItemProcessor implements ItemProcessor{
	@Override
	public boolean create(@NonNull CLIParameters params, @NonNull FFmpegProbeResult probeResult, @NonNull FFmpegStream stream, @NonNull Path inputHost, @NonNull Path outputHost, @NonNull Path batchHost, @NonNull Path batchClient){
		final var filename = outputHost.getFileName().toString();
		final var cut = filename.lastIndexOf(".");
		outputHost = outputHost.getParent().resolve((cut >= 0 ? filename.substring(0, cut) : filename) + ".mp4");
		final var duration = Duration.ofSeconds((long) probeResult.format.duration);
		final var batFilename = String.format("%dh%dm%ds %s %s %s %f.ps1", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), inputHost.getParent().getFileName().toString(), inputHost.getFileName().toString(), stream.codec_name, stream.avg_frame_rate.doubleValue());
		final var batHostPath = batchHost.resolve(batFilename);
		final var batClientPath = batchClient.resolve(batFilename);
		if(!Files.exists(batchClient)){
			try{
				Files.createDirectories(batchClient);
			}
			catch(IOException e){
				log.error("Failed to create directory {}", batchClient, e);
			}
		}
		if(Files.exists(batClientPath)){
			return false;
		}
		final var lines = List.of("\ufeff", String.format("$host.ui.RawUI.WindowTitle = \"%s\"", batFilename), String.format("if (!(Test-Path \"%s\")){", outputHost.getParent().toString()), String.format("\tmkdir \"%s\"", outputHost.getParent().toString()), "}", String.format("ffmpeg -n -i \"%s\" -c:v libx265 -preset medium -crf 23 -c:a aac -b:a 128k -movflags use_metadata_tags -map_metadata 0 \"%s\"", inputHost.toString(), outputHost.toString()), "Add-Type -AssemblyName Microsoft.VisualBasic", String.format("if (Test-Path \"%s\") {", outputHost.toString()), String.format("\t$FileCreationDate = (Get-ChildItem \"%s\").CreationTime", inputHost.toString()), String.format("\t$FileAccessDate = (Get-ChildItem \"%s\").LastAccessTime", inputHost.toString()), String.format("\tGet-ChildItem  \"%s\" | ForEach-Object {$_.CreationTime = $FileCreationDate}", outputHost.toString()), String.format("\tGet-ChildItem  \"%s\" | ForEach-Object {$_.LastAccessTime = $FileAccessDate}", outputHost.toString()), String.format("\tRemove-ItemSafely \"%s\"", inputHost.toString()), String.format("\tWrite-Output \"Deleted %s\"", inputHost.toString()), String.format("\tif (Test-Path \"%s\") {", batHostPath.toString()), String.format("\t\tRemove-ItemSafely \"%s\"", batHostPath.toString()), String.format("\t\tWrite-Output \"Deleted %s\"", batHostPath.toString()), "\t}", "}");
		try{
			Files.write(batClientPath, lines, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		}
		catch(Exception e){
			log.error("Failed to write bat into {}", batClientPath, e);
			return false;
		}
		log.info("Wrote ps1 file for {}", inputHost);
		return true;
	}
}

package fr.raksrinana.videoconverter.itemprocessor;

import fr.raksrinana.videoconverter.utils.CLIParameters;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Requires the Recycle module to be installed: https://www.powershellgallery.com/packages/Recycle/1.0.2
 */
public class PS1ItemProcessor implements ItemProcessor{
	private static final Logger LOGGER = LoggerFactory.getLogger(PS1ItemProcessor.class);
	
	@Override
	public boolean create(CLIParameters params, FFmpegProbeResult probeResult, FFmpegStream stream, Path inputHost, Path outputHost, Path batchHost, Path batchClient){
		final var filename = outputHost.getFileName().toString();
		final var cut = filename.lastIndexOf(".");
		outputHost = outputHost.getParent().resolve((cut >= 0 ? filename.substring(0, cut) : filename) + ".mp4");
		final var duration = Duration.ofSeconds((long) probeResult.format.duration);
		final var batFilename = String.format("%dh%dm%ds %s %s %s %f.ps1", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), inputHost.getParent().getFileName().toString(), inputHost.getFileName().toString(), stream.codec_name, stream.avg_frame_rate.doubleValue());
		final var batHostPath = batchHost.resolve(batFilename);
		final var batClientPath = batchClient.resolve(batFilename);
		if(!batClientPath.getParent().toFile().exists()){
			batClientPath.getParent().toFile().mkdirs();
		}
		if(batClientPath.toFile().exists())
			return false;
		try(final var pw = new PrintWriter(new FileOutputStream(batClientPath.toFile()), false, StandardCharsets.UTF_8)){
			pw.print('\ufeff');
			pw.printf("$host.ui.RawUI.WindowTitle = \"%s\"\r\n", batFilename);
			pw.printf("if (!(Test-Path \"%s\")){\r\n", outputHost.getParent().toString());
			pw.printf("\tmkdir \"%s\"\r\n", outputHost.getParent().toString());
			pw.printf("}\r\n");
			pw.printf("ffmpeg -n -i \"%s\" -c:v libx265 -preset medium -crf 23 -c:a aac -b:a 128k -movflags use_metadata_tags -map_metadata 0 \"%s\"\r\n", inputHost.toString(), outputHost.toString());
			pw.printf("Add-Type -AssemblyName Microsoft.VisualBasic\r\n");
			pw.printf("if (Test-Path \"%s\") {\r\n", outputHost.toString());
			pw.printf("\t$FileCreationDate = (Get-ChildItem \"%s\").CreationTime\r\n", inputHost.toString());
			pw.printf("\t$FileAccessDate = (Get-ChildItem \"%s\").LastAccessTime\r\n", inputHost.toString());
			pw.printf("\tGet-ChildItem  \"%s\" | ForEach-Object {$_.CreationTime = $FileCreationDate}\r\n", outputHost.toString());
			pw.printf("\tGet-ChildItem  \"%s\" | ForEach-Object {$_.LastAccessTime = $FileAccessDate}\r\n", outputHost.toString());
			pw.printf("\tRemove-ItemSafely \"%s\"\r\n", inputHost.toString());
			pw.printf("\tWrite-Output \"Deleted %s\"\r\n", inputHost.toString());
			pw.printf("\tif (Test-Path \"%s\") {\r\n", batHostPath.toString());
			pw.printf("\t\tRemove-ItemSafely \"%s\"\r\n", batHostPath.toString());
			pw.printf("\t\tWrite-Output \"Deleted %s\"\r\n", batHostPath.toString());
			pw.printf("\t}\r\n");
			pw.printf("}\r\n");
		}
		catch(FileNotFoundException e){
			LOGGER.error("Failed to write bat into {}", batClientPath, e);
			return false;
		}
		LOGGER.info("Wrote ps1 file for {}", inputHost);
		return true;
	}
}

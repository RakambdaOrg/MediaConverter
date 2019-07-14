package fr.mrcraftcod.videonormalizer.batch;

import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Duration;

public class PS1BatchCreator implements BatchCreator{
	private static final Logger LOGGER = LoggerFactory.getLogger(PS1BatchCreator.class);
	
	@Override
	public boolean create(FFmpegProbeResult probeResult, FFmpegStream stream, Path inputHost, Path outputHost, Path batchHost, Path batchClient){
		final var batFilename = String.format("%s %s %s %s %f.bat", Duration.ofSeconds((long) probeResult.format.duration).toString(), inputHost.getParent().getFileName().toString(), inputHost.getFileName().toString(), stream.codec_name, stream.avg_frame_rate.doubleValue());
		final var batHostPath = batchHost.resolve(batFilename);
		final var batClientPath = batchClient.resolve(batFilename);
		if(!batClientPath.getParent().toFile().exists())
			batClientPath.getParent().toFile().mkdirs();
		if(batClientPath.toFile().exists())
			return false;
		try(final var pw = new PrintWriter(batClientPath.toFile())){
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
			pw.printf("\t[Microsoft.VisualBasic.FileIO.FileSystem]::DeleteFile('%s','OnlyErrorDialogs','SendToRecycleBin')\r\n", inputHost.toString());
			pw.printf("\tWrite-Output \"Deleted %s\"\r\n", inputHost.toString());
			pw.printf("\tif (Test-Path \"%s\") {\r\n", batHostPath.toString());
			pw.printf("\t\t[Microsoft.VisualBasic.FileIO.FileSystem]::DeleteFile('%s','OnlyErrorDialogs','SendToRecycleBin')\r\n", batHostPath.toString());
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

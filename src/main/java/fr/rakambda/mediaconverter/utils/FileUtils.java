package fr.rakambda.mediaconverter.utils;

import lombok.extern.log4j.Log4j2;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;

@Log4j2
public class FileUtils{
	public static Timestamp getFileLastModified(Path file){
		try{
			return new Timestamp(Files.getLastModifiedTime(file).toMillis());
		}
		catch(IOException e){
			log.error("Failed to get file last modified of {}", file, e);
			return new Timestamp(System.currentTimeMillis());
		}
	}
}

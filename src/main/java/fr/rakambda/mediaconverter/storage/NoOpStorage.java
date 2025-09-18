package fr.rakambda.mediaconverter.storage;

import org.jspecify.annotations.NonNull;
import java.nio.file.Path;
import java.sql.Timestamp;

public class NoOpStorage implements IStorage{
	@Override
	public void close(){
	}
	
	@Override
	public boolean isUseless(@NonNull Path path, @NonNull Timestamp lastModified){
		return false;
	}
	
	@Override
	public void setUseless(@NonNull Path path, @NonNull Timestamp lastModified){
	}
	
	@Override
	public void save(){
	}
}

package fr.rakambda.mediaconverter.storage;

import org.jspecify.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;

public interface IStorage extends AutoCloseable{
	boolean isUseless(@NonNull Path path, @NonNull Timestamp lastModified) throws SQLException;
	
	void setUseless(@NonNull Path path, @NonNull Timestamp lastModified);
	
	void save() throws SQLException, IOException;
}

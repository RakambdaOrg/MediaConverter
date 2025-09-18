package fr.rakambda.mediaconverter.storage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;

public interface IStorage extends AutoCloseable{
	boolean isUseless(@NonNull Path path, @Nullable Timestamp lastModified) throws SQLException;
	
	void setUseless(@NonNull Path path, @Nullable Timestamp lastModified);
	
	void save() throws SQLException, IOException;
}

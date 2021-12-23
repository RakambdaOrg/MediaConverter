package fr.raksrinana.mediaconverter.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class H2Manager extends JDBCBase{
	private final Path databaseURL;
	private HikariDataSource datasource;
	private Consumer<HikariConfig> configurator;
	
	public H2Manager(@NonNull Path databaseURL) throws IOException{
		super("H2/" + databaseURL);
		Files.createDirectories(databaseURL.getParent());
		databaseURL.getParent().toFile().mkdirs();
		this.databaseURL = databaseURL;
	}
	
	@Override
	protected HikariDataSource getDatasource(){
		if(Objects.isNull(datasource)){
			var config = new HikariConfig();
			config.setDriverClassName("org.h2.Driver");
			config.setJdbcUrl("jdbc:h2:" + databaseURL.toAbsolutePath());
			config.addDataSourceProperty("cachePrepStmts", "true");
			config.addDataSourceProperty("prepStmtCacheSize", "250");
			config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
			config.setMaximumPoolSize(1);
			Optional.ofNullable(configurator).ifPresent(conf -> conf.accept(config));
			datasource = new HikariDataSource(config);
		}
		return datasource;
	}
	
	public void setConfigurator(Consumer<HikariConfig> configurator){
		this.configurator = configurator;
	}
}

package fr.mossaab.security.config;

import lombok.Getter;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
@Configuration
@Getter
public class PathConfig {
    @Value("${storage.service.folder.path}")
    private String StorageServiceFolderPath;
    @Value("${storage.type.service.folder.path}")
    private String StorageTypeServiceFolderPath;
    @Value("${swagger.url.path}")
    private String swaggerUrlPath;
    @Value("${storage.passport.service.path}")
    private String StoragePassportServiceFolderPath;
}

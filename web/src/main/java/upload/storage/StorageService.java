package upload.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface StorageService {

    void init();

    void store(MultipartFile file);

    void store(String path, MultipartFile file);

    Stream<Path> loadAll();

    Path load(String filename);

    Path load(String path, String filename);

    Resource loadAsResource(String filename);

    Resource loadAsResource(String path, String filename);

    void deleteAll();

}

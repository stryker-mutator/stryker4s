package stryker4jvm.core.files;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public interface FileIO {
    void createAndWriteFromResource(Path file, String resource) throws IOException;

    void createAndWrite(Path file, String content) throws IOException;
}

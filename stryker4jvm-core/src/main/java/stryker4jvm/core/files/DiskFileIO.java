//package stryker4jvm.core.files;
//
//import java.io.*;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//
//public class DiskFileIO implements FileIO {
//
//    @Override
//    public void createAndWriteFromResource(Path file, String resource) throws IOException {
//        InputStream stream = getClass().getResourceAsStream(resource);
//        if (stream == null)
//            throw new FileNotFoundException(String.format("Resource %s does not exist", resource));
//        Files.createDirectories(file.getParent());
//        try (stream) {
//            Files.copy(stream, file);
//        }
//    }
//
//    @Override
//    public void createAndWrite(Path file, String content) throws IOException {
//        Files.createDirectories(file.getParent());
//        FileOutputStream fos = new FileOutputStream(file.toFile());
//        OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
//        try (writer) {
//            writer.write(content);
//        }
//    }
//}

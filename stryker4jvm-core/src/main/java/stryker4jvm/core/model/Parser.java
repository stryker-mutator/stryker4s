package stryker4jvm.core.model;

import java.io.IOException;
import java.nio.file.Path;

public interface Parser<T extends AST> {
    T parse(Path p) throws IOException;
}

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class Parser {
    String commandName = "";
    String[] args = {};

    public boolean parse(String input) {
        commandName = "";
        args = new String[]{};

        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        return true;
    }

    public String getCommandName() {
        return commandName;
    }

    public String[] getArgs() {
        return args;
    }
}

public class Terminal {

    private final Parser parser;
    private Path currentPath;

    public Terminal() {
        this.parser = new Parser();

        this.currentPath = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }


    private Path resolvePath(String pathStr) {
        Path path = Paths.get(pathStr);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return currentPath.resolve(path).normalize();
    }

    public String pwd() {
        return currentPath.toString();
    }

    public void cd(String[] args) {
        if (args.length == 0) {
            currentPath = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
            return;
        }

        String pathStr = args[0];
        Path newPath = resolvePath(pathStr);

        if (!Files.exists(newPath) || !Files.isDirectory(newPath)) {
            System.err.println("Error: cd: No such file or directory: " + pathStr);
            return;
        }

        try {
            currentPath = newPath.toRealPath();
        } catch (Exception e) {
            System.err.println("Error: cd: Failed to access directory: " + e.getMessage());
        }
    }


    public static void main(String[] args) {

    }
}
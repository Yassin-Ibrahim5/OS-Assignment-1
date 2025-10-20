import java.io.File;
import java.nio.file.Files;

class Parser {
    private String commandName = "";
    private String[] args = {};

    public boolean parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            commandName = "";
            args = new String[0];
            return false;
        }
        String[] parts = input.trim().split("\\s+");
        if (parts.length > 0) {
            commandName = parts[0];
            if (parts.length > 1) {
                args = new String[parts.length - 1];
                System.arraycopy(parts, 1, args, 0, parts.length - 1);
            } else {
                args = new String[0];
            }
            return true;
        }
        commandName = "";
        args = new String[0];
        return false;
    }

    public String getCommandName() {
        return commandName;
    }

    public String[] getArgs() {
        return args;
    }
}

public class Terminal {

    private final Parser parser = new Parser();
    private File currentDir;

    public Terminal() {
        try {
            this.currentDir = new File(System.getProperty("user.dir")).getCanonicalFile();
        } catch (Exception e) {
            System.err.println("Failed to get current directory. Defaulting to user home.");
            this.currentDir = new File(System.getProperty("user.home"));
        }
    }

    private File resolvePath(String path) {
        File file = new File(path);
        if (file.isAbsolute()) {
            return file;
        } else {
            return new File(currentDir, path);
        }
    }

    public void cd(String[] args) {
        File temp;
        if (args.length == 0) {
            temp = new File(System.getProperty("user.home"));
        } else if (args.length == 1) {
            temp = resolvePath(args[0]);
        } else {
            System.err.println("Invalid number of arguments for cd command.");
            return;
        }
        if (temp.exists() && temp.isDirectory()) {
            try {
                currentDir = temp.getCanonicalFile();
            } catch (Exception e) {
                System.err.println("Failed to change directory to " + temp.getAbsolutePath());
            }
        } else {
            System.err.println("Directory " + temp.getAbsolutePath() + " does not exist.");
        }
    }

    public void mkdir(String[] args) {
        if (args.length == 0) {
            System.err.println("mkdir command requires a directory name.");
            return;
        }
        for (String dir : args) {
            File temp = resolvePath(dir);

            if (temp.exists()) {
                System.err.println("Directory " + temp.getAbsolutePath() + " already exists.");
            } else {
                try {
                    Files.createDirectory(temp.toPath());
                } catch (Exception e) {
                    System.err.println("Failed to create directory " + temp.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        }
    }

    public void rmdir(String[] args) {
        if (args.length == 0) {
            System.err.println("rmdir command requires a directory name.");
            return;
        }
        for (String dir : args) {
            File temp = resolvePath(dir);
            if (!temp.exists()) {
                System.err.println("Directory " + temp.getAbsolutePath() + " does not exist.");
            } else if (!temp.isDirectory()) {
                System.err.println("File " + temp.getAbsolutePath() + " is not a directory.");
            } else {
                try {
                    Files.deleteIfExists(temp.toPath());
                } catch (Exception e) {
                    System.err.println("Failed to delete directory " + temp.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        }
    }

    public void rm(String[] args) {
        if (args.length == 0) {
            System.err.println("rm command requires a file name.");
            return;
        }

    }

    public static void main(String[] args) {
    }
}
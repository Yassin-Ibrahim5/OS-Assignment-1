import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

import static java.util.Objects.requireNonNull;

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
        if (args.length == 1 && args[0].equals("*") && currentDir.list() != null) {
            for (File file : requireNonNull(currentDir.listFiles())) {
                if (file.isDirectory() && requireNonNull(file.list()).length == 0) {
                    try {
                        Files.deleteIfExists(file.toPath());
                    } catch (Exception e) {
                        System.err.println("Failed to delete directory " + file.getAbsolutePath() + ": " + e.getMessage());
                        return;
                    }
                }
            }
        } else {
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
    }

    public void rm(String[] args) {
        if (args.length == 0) {
            System.err.println("rm command requires a file name.");
            return;
        }
        if (args[0].equals("-d") || args[0].equals("--directory")) {
            for (int i = 1; i < args.length; i++) {
                File temp = resolvePath(args[i]);
                if (!temp.exists()) {
                    System.err.println("File " + temp.getAbsolutePath() + " does not exist.");
                } else if (!temp.isDirectory()) {
                    System.err.println("Directory " + temp.getAbsolutePath() + " is not a directory.");
                } else if (requireNonNull(temp.listFiles()).length != 0) {
                    System.err.println("Directory " + temp.getAbsolutePath() + " is not empty.");
                }
                try {
                    Files.deleteIfExists(temp.toPath());
                } catch (Exception e) {
                    System.err.println("Failed to delete file " + temp.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        }

        if (args[0].equals("-r")) {
            for (int i = 1; i < args.length; i++) {
                File temp = resolvePath(args[i]);
                if (!temp.exists()) {
                    System.err.println("File " + temp.getAbsolutePath() + " does not exist.");
                } else if (!temp.isDirectory()) {
                    System.err.println("Directory " + temp.getAbsolutePath() + " is not a directory.");
                }
                try {
                    Files.deleteIfExists(temp.toPath());
                } catch (Exception e) {
                    System.err.println("Failed to delete file " + temp.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        }

    }
    public void cp(String[] args) {
        if (args.length == 0) {
            System.err.println("cp command can not be empty.");
            return;
        }
        if(args.length != 2){
            System.err.println("Invalid => cp <sourceFile> <destinationFile> ");
            return;

        }
        File src = resolvePath(args[0]);
        File dest = resolvePath(args[1]);
        if (!src.exists()) {
            System.err.println("Source File " + src.getAbsolutePath() + " does not exist.");
            return;
        }
        if (src.isDirectory()) {
            System.err.println("Source " + dest.getAbsolutePath() + " is a directory Use cp-r.");
            return;
        }
        if (!src.isFile()) {
            System.err.println("Source File " + src.getAbsolutePath() + " is not a file.");
            return;
        }
        try {
            Files.copy(src.toPath(),dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File copied successfully.");
        } catch (Exception e) {
            System.err.println("Failed to copy file: " + e.getMessage());
        }

    }

    public void cp_r(String[] args) {
        if (args.length == 0) {
            System.err.println("cp_r command can not be empty.");
        }
        if (args.length != 3) {
            System.err.println("Invalid => cp_r <sourceDirectory> <destinationDirectory>");
        }
        File src = resolvePath(args[1]);
        File dest = resolvePath(args[2]);
        if (!src.exists()) {
            System.err.println("Source File " + src.getAbsolutePath() + " does not exist.");
        }
        if (!src.isDirectory()) {
            System.err.println("Source " + src.getAbsolutePath() + " is not a directory Use cp.");
        }
        try {
            Files.copy(src.toPath(),dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File copied successfully.");
        } catch (Exception e) {
            System.err.println("Failed to copy file: " + e.getMessage());
        }

    }
    public void zip(String[] args) {
        if (args.length == 0) {
            System.err.println("zip command can not be empty.");

            return;
        }
        if (args.length == 1 ){
            System.err.println("zip file must store some files.");

        }
    }
    public void unzip(String[] args) {}

    public void commandAction(String commandName, String[] args) {
        switch (commandName.toLowerCase()) {
            case "cd":
                cd(args);
                break;
            case "mkdir":
                mkdir(args);
                break;
            case "rmdir":
                rmdir(args);
                break;
            case "rm":
                rm(args);
                break;
            case "cp":
                cp(args);
                break;
            case "cp -r":
                cp_r(args);
                break;
            case "zip":
                zip(args);
                break;
            case "unzip":
                unzip(args);
                break;
            case "exit":
                break;
            default:
                System.err.println("Unknown command: " + commandName);
                break;
        }
    }

    public static void main(String[] args) {
        Terminal terminal = new Terminal();
        Scanner scanner = new Scanner(System.in);
        String input;

        while (true) {
            System.out.print(terminal.currentDir.getAbsolutePath());
            System.out.print("> ");
            if (scanner.hasNextLine()) {
                input = scanner.nextLine();
            } else {
                break;
            }

            if (terminal.parser.parse(input)) {
                String commandName = terminal.parser.getCommandName();
                String[] arguments = terminal.parser.getArgs();

                if (commandName.equalsIgnoreCase("exit")) {
                    System.out.println("Terminating!");
                    break;
                } else {
                    terminal.commandAction(commandName, arguments);
                }
            }
        }
        scanner.close();
    }
}
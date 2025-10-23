import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    public static void main(String[] args) {
        Terminal terminal = new Terminal();
        Scanner scanner = new Scanner(System.in);
        String input;

        while (true) {
            System.out.print(terminal.pwd());
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

    private File resolvePath(String path) {
        File file = new File(path);
        if (file.isAbsolute()) {
            return file;
        } else {
            return new File(currentDir, path);
        }
    }

    public void echo(String[] args) {
        System.out.println(String.join(" ", args));
        System.out.println();
    }

    public String pwd() {
        return currentDir.getAbsolutePath();
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

    public void ls() {
        if (!currentDir.exists() || !currentDir.isDirectory()) {
            System.err.println("Directory " + currentDir.getAbsolutePath() + " does not exist or is not a directory.");
            return;
        }

        File[] files = currentDir.listFiles();
        if (files == null) {
            System.err.println("Failed to list contents of " + currentDir.getAbsolutePath());
            return;
        }
        for (int i = 0; i < files.length; i++) {
            System.out.println(i + 1 + "-" + files[i].getName() + (files[i].isDirectory() ? "\\" : "") + "\t");
        }
        System.out.println();
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
                    } catch (DirectoryNotEmptyException e) {
                        System.err.println("Failed to delete directory " + temp.getAbsolutePath() + ": " + "Directory is not empty.");
                    } catch (IOException e) {
                        System.err.println("Failed to delete directory " + temp.getAbsolutePath() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    public void touch(String[] args) {
        if (args.length == 0) {
            System.err.println("touch command requires at least one path.");
        }
        for (String path : args) {
            File temp = resolvePath(path);

            if (temp.exists()) {
                if (temp.isDirectory()) {
                    System.err.println("Cannot create directory " + temp.getAbsolutePath() + "using 'touch'. Please use 'mkdir'.");
                }
            }
            try {
                Files.createFile(temp.toPath());
            } catch (Exception e) {
                System.err.println("Failed to create file " + temp.getAbsolutePath() + ": " + e.getMessage());
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
                    Files.delete(temp.toPath());
                } catch (Exception e) {
                    System.err.println("Failed to delete file " + temp.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        } else if (args[0].equals("-r")) {
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
        } else {
            for (String path : args) {
                File temp = resolvePath(path);
                if (!temp.exists()) {
                    System.err.println("File " + temp.getAbsolutePath() + " does not exist.");
                } else {
                    try {
                        Files.deleteIfExists(temp.toPath());
                    } catch (Exception e) {
                        System.err.println("Failed to delete file " + temp.getAbsolutePath() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    public void cp(String[] args) {

        if (args.length == 0) {
            System.err.println("cp command can not be empty.");
            return;
        }
        if (args[0].equals("-r")) {
            if (args.length != 3) {
                System.err.println("Usage: cp -r <source" + "> <destinationDirectory>");
                return;
            }
            File src = resolvePath(args[1]);
            File dest = resolvePath(args[2]);
            if (!src.exists()) {
                System.err.println("Source Directory " + src.getAbsolutePath() + " does not exist.");
                return;
            }
            if (!src.isDirectory()) {
                System.err.println("Source " + dest.getAbsolutePath() + " is not a directory Use cp");
                return;
            }

            try {
                Files.walk(src.toPath()).forEach(sourcePath -> {
                    File target = new File(dest, src.toPath().relativize(sourcePath).toString());
                    try {
                        if (sourcePath.toFile().isDirectory()) {
                            target.mkdirs();
                        } else {
                            Files.copy(sourcePath, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to copy: " + sourcePath + " -> " + e.getMessage());
                    }
                });
                System.out.println("Directory copied successfully.");
            } catch (Exception e) {
                System.err.println("Failed to copy directory: " + e.getMessage());
            }

        }
        if (args.length == 2) {
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
                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("File copied successfully.");
            } catch (Exception e) {
                System.err.println("Failed to copy file: " + e.getMessage());
            }

        }
    }

    private void addToZip(File file, String path, ZipOutputStream zipOutputStream) throws Exception {
        String zipPath = path + file.getName();
        if (file.isDirectory()) {
            if (!zipPath.endsWith("/")) {
                zipPath += "/";
            }
            zipOutputStream.putNextEntry(new ZipEntry(zipPath));
            zipOutputStream.closeEntry();

            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    addToZip(child, zipPath, zipOutputStream);
                }
            }
        } else {
            zipOutputStream.putNextEntry(new ZipEntry(zipPath));
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) > 0) {
                    zipOutputStream.write(bytes, 0, length);
                }
            }
            zipOutputStream.closeEntry();
        }
    }

    public void zip(String[] args) {

        if (!(args[0].equals("-r"))) {
            if (args.length < 2) {
                System.err.println("zip command requires at least two arguments (destination and file(s) to zip).");
                return;
            }

            for (int i = 1; i < args.length; i++) {
                File file = resolvePath(args[i]);
                if (!file.exists()) {
                    System.err.println("File " + file.getAbsolutePath() + " does not exist.");
                    return;
                }
                if (file.isDirectory()) {
                    System.err.println("File " + file.getAbsolutePath() + " is a directory, use zip -r");
                    return;
                }
            }

            File destFile = resolvePath(args[0]);
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(destFile))) {
                for (int i = 1; i < args.length; i++) {
                    File file = resolvePath(args[i]);
                    addToZip(file, "", zipOutputStream);
                }
                System.out.println("Archive created successfully: " + destFile.getName());
            } catch (Exception e) {
                System.err.println("Failed to zip file: " + e.getMessage());
            }
        }

        else {
            if (args.length < 3) {
                System.err.println("zip -r command requires at least three arguments.");
                return;
            }

            File sourceDir = resolvePath(args[2]);
            if (!sourceDir.exists()) {
                System.err.println("Directory " + sourceDir.getAbsolutePath() + " does not exist.");
                return;
            }
            if (!sourceDir.isDirectory()) {
                System.err.println(sourceDir.getAbsolutePath() + " is not a directory, use regular zip");
                return;
            }

            File destZip = resolvePath(args[1]);
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(destZip))) {

                addToZip(sourceDir, sourceDir.getName(), zipOutputStream);

                System.out.println("Archive created successfully: " + destZip.getName());
            } catch (Exception e) {
                System.err.println("Failed to zip directory: " + e.getMessage());
            }
        }
    }
    public void unzip(String[] args) {

    }

    public void commandAction(String commandName, String[] args) {
        switch (commandName.toLowerCase()) {
            case "echo":
                echo(args);
                break;
            case "pwd":
                if (args.length != 0) {
                    System.err.println("pwd command takes no arguments.");
                }
                System.out.println(pwd() + "\n");
                break;
            case "cd":
                cd(args);
                break;
            case "ls":
                if (args.length != 0) {
                    System.err.println("ls command takes no arguments.");
                }
                ls();
                break;
            case "mkdir":
                mkdir(args);
                break;
            case "rmdir":
                rmdir(args);
                break;
            case "touch":
                touch(args);
                break;
            case "rm":
                rm(args);
                break;
            case "cp":
                cp(args);
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
}
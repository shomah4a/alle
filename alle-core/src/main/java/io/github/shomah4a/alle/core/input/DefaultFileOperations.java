package io.github.shomah4a.alle.core.input;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipalLookupService;

/**
 * java.nio.file.Files を使った FileOperations の標準実装。
 */
public class DefaultFileOperations implements FileOperations {

    @Override
    public void copy(Path source, Path target) throws IOException {
        if (Files.isDirectory(source)) {
            copyDirectory(source, target);
        } else {
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    @Override
    public void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }
    }

    @Override
    public void delete(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            deleteDirectory(path);
        } else {
            Files.delete(path);
        }
    }

    @Override
    public void setOwner(Path path, String owner) throws IOException {
        UserPrincipalLookupService lookupService = path.getFileSystem().getUserPrincipalLookupService();
        var principal = lookupService.lookupPrincipalByName(owner);
        Files.setOwner(path, principal);
    }

    @Override
    public void setPermissions(Path path, String permissions) throws IOException {
        var permSet = PosixFilePermissions.fromString(permissions);
        Files.setPosixFilePermissions(path, permSet);
    }

    @Override
    public void createDirectories(Path path) throws IOException {
        Files.createDirectories(path);
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteDirectory(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}

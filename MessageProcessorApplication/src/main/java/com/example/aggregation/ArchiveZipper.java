package com.example.aggregation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ArchiveZipper zips the previous day's archive folders (for incoming and merged messages)
 * into a separate zip folder and then deletes the original directories.
 * It also maintains a maximum number of zip files.
 *
 * @author JKR3
 */
@Service
public class ArchiveZipper {

    @Value("${archive.incoming.root}")
    private String incomingArchiveRoot;

    @Value("${archive.merged.root}")
    private String mergedArchiveRoot;

    // Output folder for zipped archives.
    @Value("${archive.zip.output.root:archive/zipped}")
    private String zipOutputRoot;

    // Cron expression for running the zip job daily (e.g., "0 0 1 * * *" for 1 AM).
    @Value("${archive.zip.cron:0 0 1 * * *}")
    private String zipCron;

    // Switch to enable/disable the zip and cleanup process.
    @Value("${archive.zipper.enabled:true}")
    private boolean zipperEnabled;

    // Maximum number of zip files to maintain.
    @Value("${archive.zip.maxFiles:30}")
    private int maxZipFiles;

    /**
     * Scheduled job to zip the previous day's archive directories.
     * Runs at the time specified by the cron expression.
     */
    @Scheduled(cron = "${archive.zip.cron}")
    public void zipPreviousDayArchives() {
        if (!zipperEnabled) {
            return;
        }
        // Determine the previous day's date in "yyyyMMdd" format.
        LocalDate previousDay = LocalDate.now().minusDays(1);
        String dateStr = previousDay.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Process incoming and merged archives.
        zipAndDeleteDirectories(incomingArchiveRoot, dateStr, "incoming_" + dateStr + ".zip");
        zipAndDeleteDirectories(mergedArchiveRoot, dateStr, "merged_" + dateStr + ".zip");

        // Clean up old zip files if necessary.
        cleanupOldZipFiles();
    }

    /**
     * Zips all subdirectories of the specified root that start with the provided date prefix.
     * After zipping, deletes the source directories.
     *
     * @param rootDir the archive root directory.
     * @param datePrefix the date prefix (e.g., "20250225").
     * @param outputZipName the output zip file name.
     */
    private void zipAndDeleteDirectories(String rootDir, String datePrefix, String outputZipName) {
        try {
            Path rootPath = Paths.get(rootDir);
            if (!Files.exists(rootPath)) {
                return;
            }
            // List subdirectories whose names start with the datePrefix.
            File[] subDirs = new File(rootDir).listFiles(file ->
                    file.isDirectory() && file.getName().startsWith(datePrefix));
            if (subDirs == null || subDirs.length == 0) {
                return;
            }
            // Ensure the zip output directory exists.
            Path zipOutputPath = Paths.get(zipOutputRoot);
            if (!Files.exists(zipOutputPath)) {
                Files.createDirectories(zipOutputPath);
            }
            Path zipFilePath = zipOutputPath.resolve(outputZipName);
            try (FileOutputStream fos = new FileOutputStream(zipFilePath.toFile());
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                for (File dir : subDirs) {
                    zipDirectory(dir, dir.getName(), zos);
                }
            }
            // Delete the original directories after successful zipping.
            for (File dir : subDirs) {
                deleteDirectory(dir.toPath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Recursively zips a directory.
     *
     * @param folder the folder to zip.
     * @param parentFolder the parent folder name to use in the zip entry.
     * @param zos the ZipOutputStream.
     * @throws IOException if an I/O error occurs.
     */
    private void zipDirectory(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(file, parentFolder + "/" + file.getName(), zos);
                continue;
            }
            try (FileInputStream fis = new FileInputStream(file)) {
                String zipEntryName = parentFolder + "/" + file.getName();
                zos.putNextEntry(new ZipEntry(zipEntryName));
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
            }
        }
    }

    /**
     * Recursively deletes a directory.
     *
     * @param path the directory path.
     * @throws IOException if an I/O error occurs.
     */
    private void deleteDirectory(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Cleans up old zip files in the zip output folder, retaining only maxZipFiles.
     */
    private void cleanupOldZipFiles() {
        try {
            File zipDir = new File(zipOutputRoot);
            if (!zipDir.exists() || !zipDir.isDirectory()) {
                return;
            }
            File[] zipFiles = zipDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
            if (zipFiles == null || zipFiles.length <= maxZipFiles) {
                return;
            }
            // Sort zip files by last modified (oldest first)
            Arrays.sort(zipFiles, Comparator.comparingLong(File::lastModified));
            int filesToDelete = zipFiles.length - maxZipFiles;
            for (int i = 0; i < filesToDelete; i++) {
                if (!zipFiles[i].delete()) {
                    System.err.println("Failed to delete old zip file: " + zipFiles[i].getAbsolutePath());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

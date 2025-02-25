package com.example.aggregation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ArchiveZipper {

    @Value("${archive.incoming.hourly.root}")
    private String incomingHourlyRoot;

    @Value("${archive.incoming.minute.root}")
    private String incomingMinuteRoot;

    @Value("${archive.merged.hourly.root}")
    private String mergedHourlyRoot;

    @Value("${archive.merged.minute.root}")
    private String mergedMinuteRoot;

    // Output folder for zipped archives (default to "archive/zipped" if not provided).
    @Value("${archive.zip.output.root:archive/zipped}")
    private String zipOutputRoot;

    // This task runs daily at 1 AM.
    @Scheduled(cron = "0 0 1 * * *")
    public void zipPreviousDayArchives() {
        // Determine previous day's date in "yyyyMMdd" format.
        LocalDate previousDay = LocalDate.now().minusDays(1);
        String dateStr = previousDay.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        zipArchiveDirectories(incomingHourlyRoot, dateStr, "incoming_hourly_" + dateStr + ".zip");
        zipArchiveDirectories(incomingMinuteRoot, dateStr, "incoming_minute_" + dateStr + ".zip");
        zipArchiveDirectories(mergedHourlyRoot, dateStr, "merged_hourly_" + dateStr + ".zip");
        zipArchiveDirectories(mergedMinuteRoot, dateStr, "merged_minute_" + dateStr + ".zip");
    }

    private void zipArchiveDirectories(String rootDir, String datePrefix, String outputZipName) {
        try {
            Path rootPath = Paths.get(rootDir);
            if (!Files.exists(rootPath)) {
                return;
            }
            // List subdirectories whose names start with the previous day's date.
            File[] subDirs = new File(rootDir).listFiles(file ->
                    file.isDirectory() && file.getName().startsWith(datePrefix));
            if (subDirs == null || subDirs.length == 0) {
                return;
            }
            // Ensure output directory exists.
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
            // After successful zipping, delete the source directories.
            for (File dir : subDirs) {
                deleteDirectory(dir.toPath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void zipDirectory(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
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
}

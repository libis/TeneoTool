package be.libis.teneo.tool;

import be.libis.teneo.tool.model.FileData;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChecksumTask extends Task {

    private final File folder;
    private final FileData fileData;
    private final Map<String, ChecksumInfo> checksumInfo;
    static final String CHECKSUM_FILE = "md5sums";

    ChecksumTask(File folder, FileData fileData) {
        this.folder = folder;
        this.fileData = fileData;
        this.checksumInfo = new TreeMap<>();
        updateMessage(" >>> STARTING <<< ");
        updateProgress(-1, 100);
    }

    @Override
    protected Object call() {
        readChecksumFile();
        getFileInfos();
        return null;
    }

    private void readChecksumFile() {
        this.checksumInfo.clear();
        File checksumFile = new File(this.folder, CHECKSUM_FILE);
        try {
            Scanner scanner = new Scanner(checksumFile);
            updateMessage(" ... Reading checksum file ...");
            while (scanner.hasNextLine()) {
                String[] data = scanner.nextLine().split(" +[*]?", 2);
                if (data.length == 2) this.checksumInfo.put(data[1], new ChecksumInfo(data[0]));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger("MD5Checker").log(Level.INFO, "Checksum file not found");
        }
    }

    private void getFileInfos() {
        updateMessage(" ... Finding files ...");
        Set<String> fileList = new TreeSet<>();
        for (File file : Objects.requireNonNull(this.folder.listFiles()))
            if (file.isFile() && !file.getName().equals(CHECKSUM_FILE)) fileList.add(file.getName());
        fileList.addAll(this.checksumInfo.keySet());
        fileList.stream().map((String fileName) -> {
            File file = new File(this.folder, fileName);
            updateMessage(file.getName());
            updateProgress(fileData.getFileInfos().size(), fileList.size());
            ChecksumInfo info = this.checksumInfo.get(fileName);
            return new FileData.FileInfo(
                    file,
                    info == null ? null : info.checksum,
                    info != null && info.ignored
            );
        }).forEachOrdered(fileInfo -> Platform.runLater(() -> this.fileData.add(fileInfo)));
        updateMessage(" <<< DONE >>>");
    }

    public static class ChecksumInfo {

        String checksum;
        boolean ignored;

        ChecksumInfo(String checksum) {
            this.checksum = checksum;
            this.ignored = false;
            if (this.checksum.startsWith("#")) {
                this.checksum = this.checksum.substring(1);
                if (this.checksum.equals("null")) this.checksum = null;
                this.ignored = true;
            }
        }
    }
}

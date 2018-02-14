package be.libis.teneo.tool.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileData {

    public enum Status {OK, NEW, DELETED, CHANGED, IGNORED, IGNORE}

    @SuppressWarnings("FieldCanBeLocal")
    private final ObservableList<FileInfo> fileInfos;
    private final SimpleIntegerProperty countOK;
    private final SimpleIntegerProperty countNew;
    private final SimpleIntegerProperty countChanged;
    private final SimpleIntegerProperty countDeleted;
    private final SimpleIntegerProperty countIgnored;

    public FileData() {
        fileInfos = FXCollections.observableArrayList();
        countOK = new SimpleIntegerProperty(0);
        countNew = new SimpleIntegerProperty(0);
        countChanged = new SimpleIntegerProperty(0);
        countDeleted = new SimpleIntegerProperty(0);
        countIgnored = new SimpleIntegerProperty(0);
    }

    public ObservableList<FileInfo> getFileInfos() {
        return fileInfos;
    }

    public SimpleIntegerProperty countOKProperty() {
        return countOK;
    }

    public SimpleIntegerProperty countNewProperty() {
        return countNew;
    }

    public SimpleIntegerProperty countChangedProperty() {
        return countChanged;
    }

    public SimpleIntegerProperty countDeletedProperty() {
        return countDeleted;
    }

    public SimpleIntegerProperty countIgnoredProperty() {
        return countIgnored;
    }

    public void clear() {
        fileInfos.clear();
        resetCount();
    }

    public void cleanup() {
        removeAll(fileInfos.filtered(fileInfo -> fileInfo.getStatus().equals(Status.DELETED)));
        fileInfos.forEach(fileInfo -> {
            fileInfo.ignoreStored.set(false);
            switch (fileInfo.getStatus()) {
                case OK:
                    break;
                case IGNORED:
                    fileInfo.ignoreStored.set(true);
                    break;
                case NEW:
                    fileInfo.setStatus(Status.OK);
                    break;
                case IGNORE:
                    fileInfo.setStatus(Status.IGNORED);
                    fileInfo.ignoreStored.set(true);
                    break;
                case CHANGED:
                    fileInfo.setStatus(Status.OK);
                    break;
                case DELETED:
                    fileInfos.removeAll(fileInfo);
                    break;
            }
        });
    }

    public void add(FileInfo fileInfo) {
        fileInfos.add(fileInfo);
        incrementCount(fileInfo.getStatus());
        fileInfo.statusProperty().addListener((observable, oldValue, newValue) -> {
            decrementCount(Status.valueOf(oldValue));
            incrementCount(Status.valueOf(newValue));
        });
    }

    private void removeAll(FilteredList<FileInfo> fileInfoList) {
        ArrayList<FileInfo> fileInfoArrayList = new ArrayList<>(fileInfoList.size());
        fileInfoArrayList.addAll(fileInfoList);
        fileInfoArrayList.forEach(fileInfo -> {
            decrementCount(fileInfo.getStatus());
            fileInfos.remove(fileInfo);
        });
    }

    private int getCountOK() {
        return countOK.get();
    }

    private int getCountNew() {
        return countNew.get();
    }

    private int getCountChanged() {
        return countChanged.get();
    }

    private int getCountDeleted() {
        return countDeleted.get();
    }

    private int getCountIgnored() {
        return countIgnored.get();
    }

    private void setCountOK(int value) {
        countOK.set(value);
    }

    private void setCountNew(int value) {
        countNew.set(value);
    }

    private void setCountChanged(int value) {
        countChanged.set(value);
    }

    private void setCountDeleted(int value) {
        countDeleted.set(value);
    }

    private void setCountIgnored(int value) {
        countIgnored.set(value);
    }

    private int getCount(Status status) {
        switch (status) {
            case OK:
                return getCountOK();
            case NEW:
                return getCountNew();
            case CHANGED:
                return getCountChanged();
            case DELETED:
                return getCountDeleted();
            case IGNORED:
                return getCountIgnored();
            case IGNORE:
                return getCountIgnored();
        }
        return -1;
    }

    private void setCount(Status status, @SuppressWarnings("SameParameterValue") int value) {
        switch (status) {
            case OK:
                setCountOK(value);
                break;
            case NEW:
                setCountNew(value);
                break;
            case CHANGED:
                setCountChanged(value);
                break;
            case DELETED:
                setCountDeleted(value);
                break;
            case IGNORED:
                setCountIgnored(value);
                break;
            case IGNORE:
                setCountIgnored(value);
                break;
        }
    }

    private void changeCount(Status status, int amount) {
        switch (status) {
            case OK:
                setCountOK(getCount(status) + amount);
                break;
            case NEW:
                setCountNew(getCount(status) + amount);
                break;
            case CHANGED:
                setCountChanged(getCount(status) + amount);
                break;
            case DELETED:
                setCountDeleted(getCount(status) + amount);
                break;
            case IGNORED:
                setCountIgnored(getCount(status) + amount);
                break;
            case IGNORE:
                setCountIgnored(getCount(status) + amount);
                break;
        }
    }

    private void incrementCount(Status status) {
        changeCount(status, 1);
    }

    private void decrementCount(Status status) {
        changeCount(status, -1);
    }

    private void resetCount() {
        setCount(Status.OK, 0);
        setCount(Status.NEW, 0);
        setCount(Status.CHANGED, 0);
        setCount(Status.DELETED, 0);
        setCount(Status.IGNORED, 0);
    }

    public static class FileInfo {
        private final StringProperty file;
        private final StringProperty checksumStored;
        private final StringProperty checksumCalculated;
        private final StringProperty status;
        private final BooleanProperty ignoreStored;
        private final BooleanProperty ignoreSelected;

        public FileInfo(File file, String checksum, boolean ignore) {
            this.file = new SimpleStringProperty(file.getAbsolutePath());
            this.checksumStored = new SimpleStringProperty(checksum);
            this.ignoreStored = new SimpleBooleanProperty(ignore);
            this.checksumCalculated = new SimpleStringProperty(null);
            this.ignoreSelected = new SimpleBooleanProperty(ignore);
            this.status = new SimpleStringProperty("");
            calculateChecksum();
            updateStatus();
        }

        @SuppressWarnings({"unused", "WeakerAccess"})
        public StringProperty fileProperty() {
            return file;
        }

        @SuppressWarnings({"unused", "WeakerAccess"})
        public StringProperty checksumStoredProperty() {
            return checksumStored;
        }

        @SuppressWarnings({"unused", "WeakerAccess"})
        public StringProperty checksumCalculatedProperty() {
            return checksumCalculated;
        }

        @SuppressWarnings({"unused", "WeakerAccess"})
        public StringProperty statusProperty() {
            return status;
        }

        @SuppressWarnings({"unused", "WeakerAccess"})
        public BooleanProperty ignoreSelectedProperty() {
            return ignoreSelected;
        }

        public void toggleIgnored() {
            ignoreSelected.set(!ignoreSelected.get());
            updateStatus();
        }

        File getFile() {
            return new File(file.get());
        }

        public String getName() {
            return getFile().getName();
        }

        public String getChecksum() {
            return checksumCalculated.get();
        }

        @SuppressWarnings("WeakerAccess")
        public Status getStatus() {
            return Status.valueOf(status.get());
        }

        public boolean isIgnored() {
            return getStatus() == Status.IGNORE || getStatus() == Status.IGNORED;
        }

        public boolean isDirty() {
            Status status = getStatus();
            if (ignoreStored.get() != ignoreSelected.get()) return true;
            if (ignoreStored.get()) return false;
            return status != Status.OK;
        }

        private void updateStatus() {
            if (ignoreSelected.get() && ignoreStored.get()) {
                setStatus(Status.IGNORED);
            } else if (ignoreSelected.get() && ! ignoreStored.get()) {
                setStatus(Status.IGNORE);
            } else {
                setStatus(statusFromChecksum());
            }
        }

        private void setStatus(Status value) {
            this.status.set(value.name());
        }

        private Status statusFromChecksum() {
            if (checksumStored.get() == null) {
                if (checksumCalculated.get() == null) {
                    return Status.IGNORE;
                } else {
                    return Status.NEW;
                }
            } else {
                if (checksumCalculated.get() == null) {
                    return Status.DELETED;
                } else if (checksumStored.get().equals(checksumCalculated.get())) {
                    return Status.OK;
                } else {
                    return Status.CHANGED;
                }
            }

        }

        private void calculateChecksum() {
            checksumCalculated.set(getFileChecksum(getFile()));
        }

        private String getFileChecksum(File file) {
            try {
                if (!file.exists()) return null;

                MessageDigest md5 = MessageDigest.getInstance("MD5");
                InputStream is = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int numRead;

                while ((numRead = is.read(buffer)) != -1) {
                    md5.update(buffer, 0, numRead);
                }

                is.close();
                return bytesToHex(md5.digest());
            } catch (NoSuchAlgorithmException | IOException ex) {
                Logger.getLogger("MD5Checker").log(Level.SEVERE, null, ex);
            }
            return null;
        }

        private final char[] HEXARRAY = "0123456789abcdef".toCharArray();

        private String bytesToHex(byte[] bytes) {
            char[] hexChars = new char[bytes.length * 2];
            for (int j = 0; j < bytes.length; j++) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = HEXARRAY[v >>> 4];
                hexChars[j * 2 + 1] = HEXARRAY[v & 0x0F];
            }
            return new String(hexChars);
        }
    }

}

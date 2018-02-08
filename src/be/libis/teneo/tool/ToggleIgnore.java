package be.libis.teneo.tool;

import be.libis.teneo.tool.model.FileData;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ToggleIgnore extends Task {

    private final FileData.FileInfo fileInfo;

    ToggleIgnore(FileData.FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    @Override
    protected Object call() {
        Platform.runLater(() -> fileInfo.toggleIgnored());
        return null;
    }

}

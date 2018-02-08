package be.libis.teneo.tool;

import be.libis.teneo.tool.model.FileData;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {
    public VBox vboxMain;
    public AnchorPane apTop;
    public HBox hboxTop;
    public Label lblFolder;
    public TextField txtFolder;
    public Button btnFolder;
    public VBox vboxDetails;
    public TableView<FileData.FileInfo> tblDetails;
    public TableColumn<FileData.FileInfo, String> colFilename;
    public TableColumn<FileData.FileInfo, String> colStatus;
    public TableColumn<FileData.FileInfo, String> colChecksum;
    public HBox hboxResults;
    public HBox hboxOk;
    public Label lblOk;
    public Label lblOkCount;
    public HBox hboxNew;
    public Label lblNew;
    public Label lblNewCount;
    public HBox hboxChanged;
    public Label lblChanged;
    public Label lblChangedCount;
    public HBox hboxDeleted;
    public Label lblDeleted;
    public Label lblDeletedCount;
    public HBox hboxIgnored;
    public Label lblIgnored;
    public Label lblIgnoredCount;
    public AnchorPane apBottom;
    public StackPane stackpaneBottom;
    public VBox vboxProgress;
    public HBox hboxProgress;
    public TextField txtProgress;
    public ProgressBar pbarProgress;
    public Button btnUpdate;
    public Button btnSuccess;
    public Button btnCancel;

    private SimpleStringProperty selectedFolder;
    private FileData fileData;
    private InternalState internalState;

    private enum InternalState {
        INIT, PROCESSING, DONE, CHANGED
    }

    public MainController() {
        selectedFolder = new SimpleStringProperty("");
        fileData = new FileData();
    }

    public void initialize() {
        setState(InternalState.INIT);
        colFilename.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colChecksum.setCellValueFactory(new PropertyValueFactory<>("checksumCalculated"));
        tblDetails.setItems(fileData.getFileInfos());
        fileData.countOKProperty().addListener((observable, oldValue, newValue) -> lblOkCount.setText(newValue.toString()));
        fileData.countNewProperty().addListener((observable, oldValue, newValue) -> lblNewCount.setText(newValue.toString()));
        fileData.countChangedProperty().addListener((observable, oldValue, newValue) -> lblChangedCount.setText(newValue.toString()));
        fileData.countDeletedProperty().addListener((observable, oldValue, newValue) -> lblDeletedCount.setText(newValue.toString()));
        fileData.countIgnoredProperty().addListener((observable, oldValue, newValue) -> lblIgnoredCount.setText(newValue.toString()));
        selectedFolder.addListener((observable, oldValue, newValue) -> checkChecksums(newValue));
        btnUpdate.setOnAction(event -> writeChecksum());
        btnSuccess.setOnAction(event -> checkChecksums(this.selectedFolder.get()));
        tblDetails.setRowFactory(tableView -> {
            final TableRow<FileData.FileInfo> row = new TableRow<>();
            final ContextMenu contextMenu = new ContextMenu();
            final MenuItem toggleIgnore = new MenuItem("Toggle Ignored");
            toggleIgnore.setOnAction(event -> toggleIgnored(row.getItem()));
            contextMenu.getItems().add(toggleIgnore);
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu));
            return row;
        });
        tblDetails.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                if (event.getClickCount() == 2) {
                    toggleIgnored(tblDetails.getSelectionModel().getSelectedItem());
                }
            }
        });
        tblDetails.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.SPACE) {
                toggleIgnored(tblDetails.getSelectionModel().getSelectedItem());
            }
        });
    }

    private void toggleIgnored(FileData.FileInfo fileInfo) {
        ToggleIgnore task = new ToggleIgnore(fileInfo);
        task.setOnSucceeded(e -> Platform.runLater(() -> setState(InternalState.CHANGED)));
        task.setOnFailed(e -> Platform.runLater(() -> setState(InternalState.CHANGED)));
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void setState(InternalState state) {
        switch (state) {
            case INIT:
                btnFolder.setDisable(false);
                vboxDetails.setVisible(false);
                vboxProgress.setVisible(false);
                btnUpdate.setVisible(false);
                btnSuccess.setVisible(false);
                apBottom.setVisible(false);
                break;
            case PROCESSING:
                btnFolder.setDisable(true);
                fileData.clear();
                vboxDetails.setVisible(true);
                btnUpdate.setVisible(false);
                btnSuccess.setVisible(false);
                pbarProgress.progressProperty().unbind();
                txtProgress.textProperty().unbind();
                vboxProgress.setVisible(true);
                apBottom.setVisible(true);
                break;
            case DONE:
                btnFolder.setDisable(false);
                vboxProgress.setVisible(false);
                if (checksumsChanged()) {
                    btnUpdate.setVisible(true);
                    btnSuccess.setVisible(false);
                } else {
                    btnSuccess.setVisible(true);
                    btnUpdate.setVisible(false);
                }
                break;
            case CHANGED:
                if (internalState != InternalState.DONE) return;
                setState(InternalState.DONE);
                return;
        }
        internalState = state;
    }

    public void selectFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select directory to scan");
        if (!"".equals(selectedFolder.get())) {
            chooser.setInitialDirectory(new File(selectedFolder.get()).getParentFile());
        }
        File folder = chooser.showDialog(vboxMain.getScene().getWindow());
        if (folder != null)
            selectedFolder.set(folder.getAbsolutePath());
    }

    private boolean checksumsChanged() {
        return this.fileData.getFileInfos().stream().anyMatch((FileData.FileInfo::isDirty));
    }

    private void checkChecksums(String folder) {
        setState(InternalState.PROCESSING);
        txtFolder.setText(folder);
        ChecksumTask task = new ChecksumTask(new File(folder), fileData);
        task.setOnSucceeded(event -> setState(InternalState.DONE));
        task.setOnCancelled(event -> setState(InternalState.INIT));
        btnCancel.setOnAction(event -> task.cancel());
        pbarProgress.progressProperty().bind(task.progressProperty());
        txtProgress.textProperty().bind(task.messageProperty());
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void writeChecksum() {
        try {
            File checksum = new File(this.selectedFolder.get(), ChecksumTask.CHECKSUM_FILE);
            try (PrintWriter writer = new PrintWriter(checksum, "UTF-8")) {
                this.fileData.getFileInfos().forEach((fileInfo) -> {
                    if (fileInfo.getChecksum() != null) {
                        if (fileInfo.isIgnored()) {
                            writer.printf("#%s *%s\n", fileInfo.checksumStoredProperty().get(), fileInfo.getName());
                        } else {
                            writer.printf("%s *%s\n", fileInfo.getChecksum(), fileInfo.getName());
                        }
                    }
                });
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("MD5Checker");
            alert.setHeaderText(null);
            alert.setContentText("Checksum file saved.");
            alert.showAndWait();
            checkChecksums(this.selectedFolder.get());
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger("MD5Checker").log(Level.SEVERE, null, ex);
        }
    }

}

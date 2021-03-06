package be.libis.teneo.tool;

import be.libis.teneo.tool.model.FileData;
import be.libis.teneo.tool.model.I18N;
import be.libis.teneo.tool.model.UserSettings;
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
import javafx.util.Callback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MD5Controller {
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

    public MD5Controller() {
        selectedFolder = new SimpleStringProperty("");
        fileData = new FileData();
    }

    public void initialize() {
        setState(InternalState.INIT);
        setupI18N();

        fileData.countOKProperty().addListener((observable, oldValue, newValue) -> lblOkCount.setText(newValue.toString()));
        fileData.countNewProperty().addListener((observable, oldValue, newValue) -> lblNewCount.setText(newValue.toString()));
        fileData.countChangedProperty().addListener((observable, oldValue, newValue) -> lblChangedCount.setText(newValue.toString()));
        fileData.countDeletedProperty().addListener((observable, oldValue, newValue) -> lblDeletedCount.setText(newValue.toString()));
        fileData.countIgnoredProperty().addListener((observable, oldValue, newValue) -> lblIgnoredCount.setText(newValue.toString()));
        selectedFolder.addListener((observable, oldValue, newValue) -> checkChecksums(newValue));
        btnUpdate.setOnAction(event -> writeChecksum());
        btnSuccess.setOnAction(event -> checkChecksums(this.selectedFolder.get()));
        setupTable();
    }

    private void setupTable() {
        colFilename.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        tblDetails.setItems(fileData.getFileInfos());
        colStatus.setCellFactory(new Callback<TableColumn<FileData.FileInfo, String>, TableCell<FileData.FileInfo, String>>() {
            @Override
            public TableCell<FileData.FileInfo, String> call(TableColumn<FileData.FileInfo, String> param) {
                return new TableCell<FileData.FileInfo, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty && item == null) {
                            setTooltip(null);
                            setText("");
                            return;
                        }
                        try {
                            FileData.FileInfo fileInfo = getTableView().getItems().get(getTableRow().getIndex());
                            Tooltip tooltip = new Tooltip();
                            tooltip.setText(MessageFormat.format(
                                    "{0}: {1}\nCalculated: {2}",
                                    ChecksumTask.CHECKSUM_FILE,
                                    fileInfo.checksumStoredProperty().get(),
                                    fileInfo.getChecksum()));
                            setTooltip(tooltip);
                            setText(item);
                        } catch (IndexOutOfBoundsException ignored) {
                        }
                    }

                };
            }
        });
        tblDetails.setRowFactory(tableView -> {
            final TableRow<FileData.FileInfo> row = new TableRow<>();
            if (row.getItem() == null) return row;
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
            if (tblDetails.getSelectionModel().getSelectedItem() == null) return;
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                if (event.getClickCount() == 2) {
                    toggleIgnored(tblDetails.getSelectionModel().getSelectedItem());
                }
            }
        });
        tblDetails.setOnKeyPressed(event -> {
            if (tblDetails.getSelectionModel().getSelectedItem() == null) return;
            if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.SPACE) {
                toggleIgnored(tblDetails.getSelectionModel().getSelectedItem());
            }
        });

    }

    private void setupI18N() {
        I18N.setText(btnFolder, "btn.Folder");
        I18N.setText(btnCancel, "btn.Cancel");
        I18N.setText(lblChanged, "lbl.Changed");
        I18N.setText(lblDeleted, "lbl.Deleted");
        I18N.setText(lblIgnored, "lbl.Ignored");
        I18N.setText(lblNew, "lbl.New");
        I18N.setText(lblOk, "lbl.OK");
        I18N.setText(colFilename, "col.FileName");
        I18N.setText(colStatus, "col.Status");
        I18N.setText(btnSuccess, "btn.Success");
        I18N.setText(btnUpdate, "btn.Update");
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
        if (!UserSettings.getDir().equals("")) {
            File dir = new File(UserSettings.getDir());
            if (dir.exists()) chooser.setInitialDirectory(dir);
        }
        File folder = chooser.showDialog(vboxMain.getScene().getWindow());
        if (folder != null) {
            if (selectedFolder.get().equals(folder.getAbsolutePath())) {
                checkChecksums(folder.getAbsolutePath());
            } else {
                selectedFolder.set(folder.getAbsolutePath());
            }
            UserSettings.setDir(selectedFolder.get());
        }
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
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("MD5Checker");
            alert.setHeaderText(I18N.get("msg.ChecksumUpdateHeader"));
            Label label = new Label(I18N.get("msg.ConfirmUpdate"));
            label.setWrapText(true);
            alert.getDialogPane().setContent(label);
//            alert.setContentText(I18N.get("msg.ConfirmUpdate"));
            Optional<ButtonType> response = alert.showAndWait();
            if (response.isPresent() && response.get().equals(ButtonType.OK)) {
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
                this.fileData.cleanup();
                setState(InternalState.DONE);
            }
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger("MD5Checker").log(Level.SEVERE, null, ex);
        }
    }
}

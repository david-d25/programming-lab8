package ru.david.room.client.main;

import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import ru.david.room.CreatureModel;
import ru.david.room.client.Client;

public class CreaturesTable extends TableView<CreatureModel> {
    private TableColumn<CreatureModel, String> idColumn = new TableColumn<>("ID");
    private TableColumn<CreatureModel, String> nameColumn = new TableColumn<>();
    private TableColumn<CreatureModel, String> xColumn = new TableColumn<>("x");
    private TableColumn<CreatureModel, String> yColumn = new TableColumn<>("y");
    private TableColumn<CreatureModel, String> radiusColumn = new TableColumn<>();
    private TableColumn<CreatureModel, String> ownerColumn = new TableColumn<>();
    private TableColumn<CreatureModel, String> createdColumn = new TableColumn<>();

    public CreaturesTable() {
        idColumn.setPrefWidth(40);
        nameColumn.setPrefWidth(120);
        xColumn.setPrefWidth(40);
        yColumn.setPrefWidth(40);
        radiusColumn.setPrefWidth(50);
        ownerColumn.setPrefWidth(150);
        createdColumn.setPrefWidth(150);

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        xColumn.setCellValueFactory(new PropertyValueFactory<>("x"));
        yColumn.setCellValueFactory(new PropertyValueFactory<>("y"));
        radiusColumn.setCellValueFactory(new PropertyValueFactory<>("radius"));
        ownerColumn.setCellValueFactory(new PropertyValueFactory<>("ownerid"));
        createdColumn.setCellValueFactory(new PropertyValueFactory<>("created"));

        setMinWidth(600);
        setMinHeight(300);

        getColumns().add(idColumn);
        getColumns().add(nameColumn);
        getColumns().add(xColumn);
        getColumns().add(yColumn);
        getColumns().add(radiusColumn);
        getColumns().add(ownerColumn);
        getColumns().add(createdColumn);
        initLocalizedData();
    }

    public void initLocalizedData() {
        nameColumn.setText(Client.currentResourceBundle().getString("main.creatures-table.name-column-text"));
        radiusColumn.setText(Client.currentResourceBundle().getString("main.creatures-table.radius-column-text"));
        ownerColumn.setText(Client.currentResourceBundle().getString("main.creatures-table.owner-column-text"));
        createdColumn.setText(Client.currentResourceBundle().getString("main.creatures-table.created-column-text"));
        setPlaceholder(new Label(Client.currentResourceBundle().getString("main.creatures-table.empty-table-text")));
    }
}

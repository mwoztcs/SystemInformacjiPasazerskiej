package systeminformacjipasazerskiej.controller;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import systeminformacjipasazerskiej.converter.DayConverter;
import systeminformacjipasazerskiej.db.DeleteDBService;
import systeminformacjipasazerskiej.db.QueryDBService;
import systeminformacjipasazerskiej.model.Kurs;
import systeminformacjipasazerskiej.model.Pociag;
import systeminformacjipasazerskiej.model.Postoj;
import systeminformacjipasazerskiej.model.Stacja;

import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeleteController implements Initializable {

    DeleteDBService ddb;
    QueryDBService qdb;

    @FXML
    private Button deleteStationButton;
    @FXML
    private ComboBox<String> deleteStationBox;

    @FXML
    private Button deleteRideButton;
    @FXML
    private Button deleteRideButtonId;
    @FXML
    private ComboBox<Integer> deleteRideId;
    @FXML
    private ComboBox<String> deleteRideFromBox;
    @FXML
    private ComboBox<String> deleteRideToBox;
    @FXML
    private ComboBox<String> dayRideBox;
    @FXML
    private ListView<Kurs> rideList;

    class RideCell extends ListCell<Kurs> {
        HBox hbox = new HBox();
        Label label = new Label("");
        Pane pane = new Pane();
        Button button = new Button("Usuń");

        public RideCell() {
            super();

            hbox.getChildren().addAll(label, pane, button);
            HBox.setHgrow(pane, Priority.ALWAYS);
            button.setOnAction(event -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Potwierdź wybór");
                alert.setHeaderText("Czy na pewno chcesz usunąć ten kurs?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK){
                    Kurs toDel = getItem();
                    getListView().getItems().remove(getItem());
                    ddb.deleteRide(toDel);
                }

            });
        }

        protected void updateItem(Kurs item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            setGraphic(null);

            if (item != null && !empty) {

                ArrayList<Postoj> list = item.getListaPostojow();

                String through = "";

                Pociag pociag = qdb.getPociagById(item.getIdPociagu());
                int id_trasy = pociag.getIdTrasy();
                Stacja first = qdb.getStacjaById(qdb.getFirstStationFromTrasa(id_trasy));
                Postoj firstPostoj = qdb.getPostojByIds(item.getIdKursu(), first.getIdStacji());
                Stacja last = qdb.getStacjaById(qdb.getLastStationFromTrasa(id_trasy));
                Postoj lastPostoj = qdb.getPostojByIds(item.getIdKursu(), last.getIdStacji());

                if(!list.get(0).getNazwaStacji().equals(firstPostoj.getNazwaStacji())) {
                    through += " Przez: ";
                    through += list.get(0).getNazwaStacji();
                }
                if(!list.get(list.size() - 1).getNazwaStacji().equals(lastPostoj.getNazwaStacji())) {
                    if (through.equals("")) through += " Przez: ";
                    else through += ", ";
                    through += list.get(list.size() - 1).getNazwaStacji();
                }
                label.setText("Z: " + first.getNazwaStacji() + "(" +
                        firstPostoj.getOdjazd()+")   " + through + "    Do: " +  last.getNazwaStacji() +
                        "(" + lastPostoj.getPrzyjazd() + ")" +
                        "  Pociąg: " + pociag.getNazwaPociagu());
                setGraphic(hbox);
            }
        }
    }



    private ObservableList<String> allStationsNames = FXCollections.observableArrayList();
    private ObservableList<Kurs> allMatchingKursy = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        //Delete Station
        deleteStationBox.setItems(allStationsNames);
        deleteStationBox.setPromptText("np. Kraków Główny");
        deleteStationButton.setOnMouseClicked(event -> {
            if(deleteStationBox.getValue() == null) return;
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Potwierdź wybór");
            alert.setHeaderText("Czy na pewno chcesz usunąć tę stację?");
            alert.setContentText("Usunięcie stacji spowoduje usunięcie wszystkich postójów, odcinków, tras i pociągów z nią związanych.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                ddb.deleteStation(deleteStationBox.getValue());
                allStationsNames.clear();
                allStationsNames.addAll(
                    qdb.getAllStations()
                        .stream()
                        .map(Stacja::getNazwaStacji)
                        .collect(Collectors.toList()));
            }
        });
        //*************

        //Delete ride

        deleteRideId.setPromptText("TO DO"); // TODO

        deleteRideFromBox.setItems(allStationsNames);
        deleteRideFromBox.setPromptText("np. Poznań Główny");
        deleteRideToBox.setItems(allStationsNames);
        deleteRideToBox.setPromptText("np. Wrocław Główny");

        dayRideBox.setItems(FXCollections.observableArrayList(
            Stream.iterate(0, i -> i+1)
                .limit(7)
                .map(DayConverter::convertDay)
                .collect(Collectors.toList())
        ));
        dayRideBox.getSelectionModel().select(0);

        deleteRideButton.setOnMouseClicked(event -> {
            rideList.setVisible(false);
            allMatchingKursy.clear();

            try {
                allMatchingKursy.addAll(qdb.getConnections(
                    deleteRideFromBox.getValue(),
                    deleteRideToBox.getValue(),
                    dayRideBox.getValue(),
                    "00:00",
                    true, true, true, false));

                rideList.setVisible(true);
            } catch (QueryDBService.NoSuchStationException nss) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText("Nie znaleziono podanej stacji.");
                alert.showAndWait();
            } catch (QueryDBService.NoMatchingKursyException nmk) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText("Nie znaleziono pasujących połączeń.");
                alert.showAndWait();
            }
        });
        rideList.setItems(allMatchingKursy);
        rideList.setCursor(Cursor.HAND);
        rideList.setVisible(false);
        rideList.setCellFactory(param -> new RideCell());
        allMatchingKursy.addListener((ListChangeListener<Kurs>) change ->
            rideList.setMaxHeight(allMatchingKursy.size() * 34 + 2)
        );

    }

    public void setDB(DeleteDBService ddb) {
        this.ddb = ddb;
    }

    public void setDB(QueryDBService qdb) {
        this.qdb = qdb;

        allStationsNames.addAll(
            qdb.getAllStations()
                .stream()
                .map(Stacja::getNazwaStacji)
                .collect(Collectors.toList()));

        System.out.println("delete db ready");
    }
}

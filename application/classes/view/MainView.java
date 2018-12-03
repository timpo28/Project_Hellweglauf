 package classes.view;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

import classes.Data;
import classes.SetupUtils;
import classes.model.Competition;
import classes.repository.CompetitionRepository;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tp.dialog.StandardAlert;
import tp.dialog.StandardMessageType;

public class MainView implements Initializable {

	@FXML private GridPane root;
	@FXML private TabPane tabPane;
	@FXML private Pane competitionPane, trainingPane;
	@FXML private Label errorLabel;
	// Unterpunkte des Datei-Men�
	@FXML private MenuItem saveMenu, openMenu, printMenu;
	
	// Der MainView bekommt den ge�ffneten Wettkampf (und Repository), damit dieser
	// so gespeichert werden kann vom Benutzer...
	private Competition currentCompetition;
	private CompetitionRepository currentCompetitionRepository;
	
	public MainView(Stage primaryStage) throws IOException {
		FXMLLoader templateLoader = new FXMLLoader(getClass().getResource("/templates/mainView.fxml"));
		templateLoader.setController(this);
		primaryStage.setScene(new Scene(templateLoader.load()));
		this.afterInitialize(); // weil in der initialize die Scene noch nicht vorhanden ist 
		primaryStage.getScene().getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
		primaryStage.setMinHeight(640);
		//primaryStage.setMaxHeight(640);
		primaryStage.setMinWidth(640);
		//primaryStage.setMaxWidth(800);
		primaryStage.setTitle("Projekt Hellweglauf");
		primaryStage.show();
	}
	
	// Click-Events
	public void competitionPaneClick(Event e) throws IOException {
		// Das Auswahlfenster erstellen
		Stage stage = new Stage();
		stage.setResizable(false);
		stage.initOwner(((Parent) e.getSource()).getScene().getWindow());
		stage.initModality(Modality.WINDOW_MODAL);

		// Das Template laden und dem Controller diesen Controller
		// �bergeben, damit ein neuer Tab hinzugef�gt werden kann.
		// (sch�n ist das nicht, aber selten)
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/templates/competition/selectCompetitionView.fxml"));
		Parent parent = (Parent)loader.load();
		SelectCompetitionView scvc = (SelectCompetitionView)loader.getController();
		scvc.setMainViewController(this); 
		
		Scene scene = new Scene(parent, 300, 400);
		//scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
		stage.setScene(scene);
		stage.show();
	}

	public void settingsBtnClick(ActionEvent e) throws IOException {

		Stage stage = new Stage();
		stage.setResizable(false);
		stage.initOwner(((Button) e.getSource()).getScene().getWindow());
		stage.initModality(Modality.WINDOW_MODAL);
		
		// Wenn das Einstellungsfenster geschlossen wird, 
		// updaten, damit die Fehlernachrichten neu geschrieben
		// werden k�nnen...
		stage.setOnCloseRequest(EventHandler -> {
			check();
		});

		Parent parent = FXMLLoader.load(getClass().getResource("/templates/settings/settingsView.fxml"));
		Scene scene = new Scene(parent, 540, 400);
		scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
			
		stage.setScene(scene);
		stage.show();
	}
	
	@FXML 
	private void aboutMenuClick() {
		
		// Die HTML-Datei f�r den �ber-Tab
		File f = new File("about.html");
		// Den Tab erzeugen
		Tab tab = new Tab("�ber die Software");
		BorderPane bp = new BorderPane();
		bp.setPadding(new Insets(20));
		
		WebView aboutWebView = new WebView();
		bp.setCenter(aboutWebView);
		
		tab.setContent(bp);
		
		try {
			aboutWebView.getEngine().load(f.toURI().toURL().toString());
			// Tab wird hier hinzugef�gt, damit dieser nicht angezeigt wird,
			// wenn die Datei(about.html) nicht geladen werden konnte.
			addTab(tab);
		} catch(MalformedURLException e) {
			// Dem Benutzer anzeigen, dass etwas nicht 
			// geklappt hat.
			errorLabel.setText("Die \"�ber\"-Seite konnte nicht geladen werden.");
		}
	}
	
	@FXML 
	private void saveMenuClick(ActionEvent e) {
		FileChooser fileChooser = new FileChooser();
        // Extensionfilter setzen 
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML-Dateien (*.xml)", "*.xml"));
        // Speichern-Dialog anzeigen
        File file = fileChooser.showSaveDialog(root.getScene().getWindow());
        
        if(file != null){
            currentCompetitionRepository.setPath(file.getAbsolutePath());
            currentCompetitionRepository.write(currentCompetition, true); // true: auf Thread warten
        }
	}
	
	@FXML 
	private void openMenuClick(ActionEvent e) {
		FileChooser fileChooser = new FileChooser();
        // Extensionfilter setzen 
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML-Dateien (*.xml)", "*.xml"));
        // Speichern-Dialog anzeigen
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        
        if(file != null){
            currentCompetitionRepository = new CompetitionRepository(file.getAbsolutePath());
            try {
				currentCompetition = currentCompetitionRepository.read(true); // true: Pfad beachten
			
				// Den Tab erstellen und hinzuf�gen
				CompetitionView cv;
				if(currentCompetition.getType() == 0) {
					cv = new TimeCompetitionView(currentCompetition, currentCompetitionRepository);
					// Hier m�ssen keine Vorrausetzungen gekl�rt werden, da der Benutzer
					// lediglich einen vorhandenen Wettkampf l�dt und keinen neuen erstellen
					// m�chte...
					addTab(createTab("Wettkampf (Zeit)", "/templates/competition/competitionViewTime.fxml", cv));
				}
				else {
					cv = new DistanceCompetitionView(currentCompetition, currentCompetitionRepository);
					// s.o.
					addTab(createTab("Wettkampf (Distanz)", "/templates/competition/competitionViewDistance.fxml", cv));
				}
				
				// Ui updaten
				setCurrentCompetitionAndRepository(cv.getCompetition(), cv.getCompetitionRepository());
            } catch (IOException ioe) {
            	ioe.printStackTrace();
				new StandardAlert(StandardMessageType.ERROR).showAndWait();
			} 
        }
	}
	
	@FXML 
	private void printMenuClick(ActionEvent e) throws IOException {
		
		Stage stage = new Stage();
		stage.setTitle("Drucken");
		
		FXMLLoader templateLoader = new FXMLLoader(getClass().getResource("/templates/print/printView.fxml"));
		templateLoader.setController(new PrintView(currentCompetition));
			
		stage.setScene(new Scene(templateLoader.load()));
		stage.show();
		
	}
	
	// GETTER UND SETTER
	public void setCurrentCompetitionAndRepository(Competition c, CompetitionRepository cr) {
		currentCompetition = c;
		currentCompetitionRepository = cr;
		
		toggleCompetitionRelevantUIComponents();
	}
	
	// So muss es keinen Getter geben, der das 
	// ganze TabPane nach au�en gibt.
	public void addTab(Tab tab) {
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);	
		
		// Den Benutzer fragen, ob der Tab wirklich geschlossen werden soll.
		tab.setOnCloseRequest(e -> {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setContentText("Soll der Tab wirklich geschlossen werden?");
			alert.showAndWait().ifPresent(c -> {
				if(c.getButtonData().equals(ButtonBar.ButtonData.CANCEL_CLOSE)) {
					// Das Event konsumieren, um das Schlie�en des Tabs
					// zu verhindern.
					e.consume();
				}
			});
		});
		// Ein Event-Handler f�r das Schlie�en eines Wettkampfes einh�ngen
		tab.setOnClosed(e -> {
			if(currentCompetition != null && currentCompetition.getTimer() != null) {
				// Damit nicht noch unerwartet in Dateien 
				// geschrieben wird oder �hnliches.
				currentCompetition.getTimer().stopTimer();
			}
			setCurrentCompetitionAndRepository(null, null);
		});
	}
	
	public Tab createTab(String title, String resource, CompetitionView view) throws IOException {
		Tab tab = new Tab(title);
		// vorbereiten...
		FXMLLoader templateLoader = new FXMLLoader(getClass().getResource(resource));
		templateLoader.setController(view);
		// und laden
		tab.setContent(templateLoader.load());
		
		return tab;
	}
	
	private void check() {
		errorLabel.setText(new String());
		// Testen, ob es Chips gibt. Falls es keine Chips gibt, m�ssen der Wettkampf- und 
		// Trainingsbutton deaktiviert werden. 
		int basicChipsFile = SetupUtils.testForFile(Data.BASIC_DIR + "/" + Data.CHIPS_FILE);
		
		// Die Chipsdatei im BASIC_DIR ist die Wichtigste. Ohne diese kann das Programm kaum richtig arbeiten,
		// da bei jedem neuen Wettkampf (und auch Training) die Chips von dort aus kopiert werden.
		if(basicChipsFile == 0) {
			errorLabel.setText("Anmerkung: Es sind keine Chips vorhanden. (Einstellungen -> Chips verwalten)");
		}
		else if(basicChipsFile < 0) {
			errorLabel.setText("Fehler: Die Datei \"data/basic/chips.xml\" ist nicht vorhanden.");
			errorLabel.setTooltip(new Tooltip("Dieser Fehler kann behoben werden, indem Chips eingetragen werden. "
					+ "(Einstellungen -> Chips verwalten)"));
		}
	}
	
	private void toggleCompetitionRelevantUIComponents() {
		if(currentCompetition != null && currentCompetitionRepository != null) {
			// Nur ein Wettkampf darf ge�ffnet sein
			saveMenu.setDisable(false);
			openMenu.setDisable(true);
			printMenu.setDisable(false);
			competitionPane.setDisable(true);
			trainingPane.setDisable(true);
		}
		else {
			saveMenu.setDisable(true);
			openMenu.setDisable(false);
			printMenu.setDisable(true);
			competitionPane.setDisable(false);
			trainingPane.setDisable(false);
		}
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		check();	
		toggleCompetitionRelevantUIComponents();
	}
	
	// Eingef�hrt, da in der initialize noch keine Scene vorhanden ist.
	private void afterInitialize() {
		// Autoload-Funktion
		String path = Data.DIR + "/" + Data.BASIC_DIR + "/autoOpen";
		File autoload = new File(path);
		if(autoload.exists()) {
			try {
				List<String> compPath = Files.readAllLines(autoload.toPath());
				// In der Datei steht nur eine Zeile.
				CompetitionRepository compRepo = new CompetitionRepository(compPath.get(0));
				setCurrentCompetitionAndRepository(compRepo.read(), compRepo);
				
				// Den Tab erstellen und hinzuf�gen
				CompetitionView cv;
				if(currentCompetition.getType() == 0) {
					cv = new TimeCompetitionView(currentCompetition, currentCompetitionRepository);
					// Hier m�ssen keine Vorrausetzungen gekl�rt werden, da der Benutzer
					// lediglich einen vorhandenen Wettkampf l�dt und keinen neuen erstellen
					// m�chte...
					addTab(createTab("Wettkampf (Zeit)", "/templates/competition/competitionViewTime.fxml", cv));
				}
				else {
					cv = new DistanceCompetitionView(currentCompetition, currentCompetitionRepository);
					// s.o.
					addTab(createTab("Wettkampf (Distanz)", "/templates/competition/competitionViewDistance.fxml", cv));
				}
			} catch (IOException e1) {
				// Sollte es einen Fehler geben, ist das nicht schlimm. 
				// So muss der Benutzer den Wettkampf manuell laden.
			}
		}
		toggleCompetitionRelevantUIComponents();
		
		// Beim Schlie�en einen eventuell ge�ffneten Wettkampf abspeichern
		root.getScene().getWindow().setOnCloseRequest(e -> {	
			// ist gerade ein Wettkampf ge�ffnet so wird der Pfad zu diesem 
			// in eine Datei geschrieben, um diesen beim n�chsten Start
			// wieder des Programms wieder zu �ffnen.
			if(currentCompetition != null && currentCompetitionRepository != null) {
				try {
					PrintWriter pw = new PrintWriter(new FileWriter(path));
					pw.print(currentCompetitionRepository.getPath());
					pw.close();
				} catch (IOException ex) {
					// Wenn ein Fehler auftritt ist das nicht schlimm. 
					// Dann wird beim starten der Wettkampf eben nicht 
					// automatisch erneut ge�ffnet.
				}
			}
			else {
				// Ist kein Wettkampf ge�ffnet, so soll auch nichts ge�ffnet werden
				// beim n�chsten Start des Programms.
				try {
					Files.delete(new File(path).toPath());
				} catch (IOException e1) {
					// (Begr�ndung: siehe oben)
				}
			}
		});
	}
}

package prohell.prohell.classes.view;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

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
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import prohell.prohell.ActivationService;
import prohell.prohell.classes.Constants;
import prohell.prohell.classes.SetupUtils;
import prohell.prohell.classes.controller.CompetitionController;
import prohell.prohell.classes.model.Competition;
import prohell.prohell.classes.repository.CompetitionRepository;
import prohell.prohell.utils.dialog.StandardAlert;
import prohell.prohell.utils.dialog.StandardMessageType;
import prohell.prohell.utils.logging.ILoggingUtil;
import prohell.prohell.utils.logging.SimpleLoggingUtil;

public class MainView implements Initializable {

	//@Deprecated
	//public static Stage mainStage;
	@Getter
	private Stage stage;

	@FXML
	private BorderPane root;
	@FXML
	private TabPane tabPane;
	@FXML
	private Pane competitionPane, trainingPane;
	@FXML
	private Label errorLabel, activationLabel;
	// Unterpunkte des Datei-Menü
	@FXML
	private MenuItem saveMenu, openMenu, printMenu;

	// Der MainView bekommt den geöffneten Wettkampf(controller), damit dieser
	// so gespeichert werden kann vom Benutzer...
	private CompetitionController currentCompetitionController;

	private ILoggingUtil log;

	// Singelton
	private static MainView INSTANCE;

	public MainView(Stage primaryStage) throws IOException, InstantiationException {
		// Das MainView ist sowas wie ein Singleton, es benötigt aber einen
		// Öffentlichen Konstruktor, da die Main-Klasse diese zu Beginn einmalig
		// instanziiert und die prim�re Stage �bergibt.
		if(INSTANCE != null) {
			throw new InstantiationException("MainView already initialized!");
		}
		INSTANCE = this;
		
		log = new SimpleLoggingUtil(new File(Constants.logFilePath()));

		FXMLLoader templateLoader = new FXMLLoader();
		templateLoader.setLocation(getClass().getResource("/templates/mainView.fxml"));
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
		//mainStage = primaryStage;
		stage = primaryStage;
	}
	
	public static MainView get() {
		return INSTANCE;
	}
	
	// Click-Events
	public void competitionPaneClick(Event e) throws IOException {
		// Das Auswahlfenster erstellen
		Stage stage = new Stage();
		stage.setResizable(false);
		stage.initOwner(((Parent) e.getSource()).getScene().getWindow());
		stage.initModality(Modality.WINDOW_MODAL);

		// Das Template laden
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/templates/competition/selectCompetitionView.fxml"));
		Parent parent = (Parent)loader.load();
		
		Scene scene = new Scene(parent, 300, 400);
		//scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
		stage.setScene(scene);
		stage.show();
	}
	
	public void trainingPaneClick(Event e) throws IOException {
		addTab(createTab("Training", "/templates/competition/trainingView.fxml", new TrainingView()));
	}

	public void settingsBtnClick(ActionEvent e) throws IOException {

		Stage stage = new Stage();
		//stage.setResizable(false);
		stage.initOwner(((Button) e.getSource()).getScene().getWindow());
		stage.initModality(Modality.WINDOW_MODAL);
		
		// Wenn das Einstellungsfenster geschlossen wird, 
		// updaten, damit die Fehlernachrichten neu geschrieben
		// werden können...
		stage.setOnCloseRequest(EventHandler -> {
			check();
		});

		Parent parent = FXMLLoader.load(getClass().getResource("/templates/settings/settingsView.fxml"));
		Scene scene = new Scene(parent);
		scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
			
		stage.setScene(scene);
		stage.setWidth(620);
		stage.setHeight(580);
		stage.setMinWidth(620);
		stage.setMinHeight(580);
		stage.show();
	}
	
	@FXML 
	private void aboutMenuClick() {
		
		// Die HTML-Datei für den Über-Tab
		File f = new File("about.html");
		// Den Tab erzeugen
		Tab tab = new Tab("Über die Software");
		BorderPane bp = new BorderPane();
		bp.setPadding(new Insets(20));
		
		WebView aboutWebView = new WebView();
		bp.setCenter(aboutWebView);
		
		tab.setContent(bp);
		
		try {
			aboutWebView.getEngine().load(f.toURI().toURL().toString());
			addTab(tab);
		} catch(MalformedURLException e) {
			log.error(e);
			errorLabel.setText("Die \"Über\"-Seite konnte nicht geladen werden.");
		}
	}
	
	@FXML
	private void helpMenuClick() {
		// Die HTML-Datei für den Hilfe-Tab
		File f = new File("help.html");
		// Den Tab erzeugen
		Tab tab = new Tab("Hilfeseite");
		BorderPane bp = new BorderPane();
		bp.setPadding(new Insets(20));
		
		WebView helpWebView = new WebView();
		bp.setCenter(helpWebView);
		
		tab.setContent(bp);
		
		try {
			helpWebView.getEngine().load(f.toURI().toURL().toString());
			addTab(tab);
		} catch(MalformedURLException e) {
			log.error(e);
			errorLabel.setText("Die Hilfeseite konnte nicht geladen werden.");
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
        	currentCompetitionController.setPath(file.getAbsolutePath());
        	currentCompetitionController.saveSync();
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
            CompetitionRepository curCompRepo = new CompetitionRepository(file.getAbsolutePath());
            try {
				Competition curComp = curCompRepo.read();
			
				// Den Tab erstellen und hinzufügen
				CompetitionController compCon = new CompetitionController(curComp, curCompRepo);
				CompetitionView cv;
				if(curComp.getType() == 0) {
					cv = new TimeCompetitionView(compCon);
					addTab(createTab("Wettkampf (Zeit)", "/templates/competition/competitionViewTime.fxml", cv));
				}
				else {
					cv = new DistanceCompetitionView(compCon);
					addTab(createTab("Wettkampf (Distanz)", "/templates/competition/competitionViewDistance.fxml", cv));
				}
				
				setCurrentCompetitionController(cv.getCompetitionController());
            } catch (IOException ioe) {
            	log.error(ioe);
				new StandardAlert(StandardMessageType.ERROR).showAndWait();
			} 
        }
	}
	
	@FXML 
	private void printMenuClick(ActionEvent e) throws IOException {
		
		Stage stage = new Stage();
		stage.setTitle("Drucken");
		
		FXMLLoader templateLoader = new FXMLLoader(getClass().getResource("/templates/print/printView.fxml"));
		templateLoader.setController(new PrintView(currentCompetitionController));
			
		stage.setScene(new Scene(templateLoader.load()));
		stage.show();
		
	}
	
	// GETTER UND SETTER
	public void setCurrentCompetitionController(CompetitionController cc) {
		currentCompetitionController = cc;
		
		toggleCompetitionRelevantUIComponents();
	}
	
	// So muss es keinen Getter geben, der das 
	// ganze TabPane nach außen gibt.
	public void addTab(Tab tab) {
		tabPane.getTabs().add(tab);
		tabPane.getSelectionModel().select(tab);	
		
		// Den Benutzer fragen, ob der Tab wirklich geschlossen werden soll.
		tab.setOnCloseRequest(e -> {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setContentText("Soll der Tab wirklich geschlossen werden?");
			alert.showAndWait().ifPresent(c -> {
				if(c.getButtonData().equals(ButtonBar.ButtonData.CANCEL_CLOSE)) {
					// Das Event konsumieren, um das Schließen des Tabs
					// zu verhindern.
					e.consume();
				}
			});
		});
		// Ein Event-Handler für das Schließen eines Wettkampfes einh�ngen
		tab.setOnClosed(e -> {
			if(currentCompetitionController != null) {
				Competition currentCompetition = currentCompetitionController.getCompetition();
				if(currentCompetition != null && currentCompetition.getTimer() != null) {
					// Damit nicht noch unerwartet in Dateien 
					// geschrieben wird oder ähnliches.
					currentCompetition.getTimer().stopTimer();
				}
				setCurrentCompetitionController(null);
			}
		});
	}
	
	public Tab createTab(String title, String resource, Object view) throws IOException {
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
		// Testen, ob es Chips gibt. Falls es keine Chips gibt, müssen der Wettkampf- und 
		// Trainingsbutton deaktiviert werden. 
		int basicChipsFile = SetupUtils.testForFile(Constants.CHIPS_FILE_PATH);
		
		// Die Chipsdatei im BASIC_DIR ist die Wichtigste. Ohne diese kann das Programm kaum richtig arbeiten,
		// da bei jedem neuen Wettkampf (und auch Training) die Chips von dort aus kopiert werden.
		if(basicChipsFile == 0) {
			errorLabel.setText("Anmerkung: Es sind keine Chips vorhanden. (Einstellungen -> Chips verwalten)");
		}
		else if(basicChipsFile < 0) {
			errorLabel.setText("Fehler: Die Datei \"data/chips.csv\" ist nicht vorhanden.");
			errorLabel.setTooltip(new Tooltip("Dieser Fehler kann behoben werden, indem Chips eingetragen werden. "
					+ "(Einstellungen -> Chips verwalten)"));
		}
	}
	
	private void toggleCompetitionRelevantUIComponents() {
		if(currentCompetitionController != null) {
			// Nur ein Wettkampf darf geöffnet sein
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
		
		// Aktivierungsstatus anzeigen
		ActivationService as = ActivationService.get();
		if(as.isSoftwareActivated()) {
			Date dateUntil = as.getActivationUntil();
		    Date currentDate = new Date();
			long diff = dateUntil.getTime() - currentDate.getTime();
			
			SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
			String dateUntilStr = formatter.format(dateUntil);
			
		    activationLabel.setText("Software ist noch " 
		    		+ TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) 
		    		+ " Tage aktiviert (bis " + dateUntilStr + ").");
		}
		
		check();	
		toggleCompetitionRelevantUIComponents();
	}
	
	// Eingeführt, da in der initialize noch keine Scene vorhanden ist.
	private void afterInitialize() {
		// Autoload-Funktion
		String path = Constants.DIR+ "/autoOpen";
		File autoload = new File(path);
		if(autoload.exists()) {
			try {
				List<String> compPath = Files.readAllLines(autoload.toPath());
				// In der Datei steht nur eine Zeile.
				CompetitionRepository compRepo = new CompetitionRepository(compPath.get(0));
				setCurrentCompetitionController(new CompetitionController(compRepo.read(), compRepo));
				
				// Den Tab erstellen und hinzufügen
				CompetitionView cv;
				if(currentCompetitionController.getCompetition().getType() == 0) {
					cv = new TimeCompetitionView(currentCompetitionController);
					addTab(createTab("Wettkampf (Zeit)", "/templates/competition/competitionViewTime.fxml", cv));
				}
				else {
					cv = new DistanceCompetitionView(currentCompetitionController);
					addTab(createTab("Wettkampf (Distanz)", "/templates/competition/competitionViewDistance.fxml", cv));
				}
			} catch (IOException e1) {
				// Autoload konnte nicht durhgeführt werden. 
				log.warning(e1.getMessage());
			}
		}
		toggleCompetitionRelevantUIComponents();
		
		// Beim Schlie�en einen eventuell geöffneten Wettkampf abspeichern
		root.getScene().getWindow().setOnCloseRequest(e -> {	
			// ist gerade ein Wettkampf geöffnet so wird der Pfad zu diesem 
			// in eine Datei geschrieben, um diesen beim n�chsten Start
			// wieder des Programms wieder zu öffnen.
			if(currentCompetitionController != null) {
				try {
					PrintWriter pw = new PrintWriter(new FileWriter(path));
					pw.print(currentCompetitionController.getCompetitionRepository().getPath());
					pw.close();
				} catch (IOException ex) {
					log.warning(ex);
				}
			}
			else {
				// Ist kein Wettkampf geöffnet, so soll auch nichts geöffnet werden
				// beim nächsten Start des Programms.
				try {
					Files.delete(new File(path).toPath());
				} catch (IOException ioe) {
					log.warning(ioe.getMessage());
				}
			}
		});
	}
}

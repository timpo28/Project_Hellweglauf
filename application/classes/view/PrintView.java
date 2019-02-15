package classes.view;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import classes.Data;
import classes.controller.CompetitionController;
import classes.model.Chip;
import classes.model.Competition;
import classes.model.Lap;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.transform.Scale;
import javafx.scene.web.WebView;
import tp.dialog.StandardAlert;
import tp.dialog.StandardMessageType;

public class PrintView implements Initializable {
	
	@FXML private ChoiceBox<String> printerChoiceBox;
	@FXML private ListView<Chip> studentListView;
	@FXML private WebView previewWebView;
	

	private ObservableSet<Printer> allPrinter;
	private CompetitionController competitionController;
	
	public PrintView(CompetitionController competitionController) {
		this.competitionController = competitionController;
		competitionController.load();
	}
	
	@FXML
	private void listOfResultsPrintBtnClick() {
		// FIXMENOTNOW: Beim Drucken gehen die Styleangaben verloren
		// Die Liste rendern
		renderListOfResults();
		
        previewWebView.setZoom(1); // Damit die volle Breite des Blattes ausgenutzt wird
		PrinterJob job = PrinterJob.createPrinterJob(getSelectedPrinter());
		job.showPrintDialog(previewWebView.getScene().getWindow());
	    if (job != null) {
	    	previewWebView.getEngine().print(job);
	        job.endJob();
	    }
	    previewWebView.setZoom(0.38); // Zur�ck setzten, damit es f�r den Benutzer sch�n ist
	}
	
	@FXML 
	private void printBtnClick() {
		print();
	}
	
	@FXML
	private void printAllBtnClick() {
		// Sch�ler filtern. Bedingung: gelaufene Runden > 0
		List<Chip> chips = competitionController.getChipsController().getChips().stream()
			.filter(c -> c.getLapCount() > 0)
			.collect(Collectors.toList());
		
		// nacheinander drucken
		for(int i = chips.size() - 1; i >= 0; i--) {
			renderDocument(chips.get(i));
			print();
		}
	}
	
	private void print() {
		Printer printer = getSelectedPrinter();
		
		Node node = this.previewWebView;
		// Das WebView transformieren... bisschen unsch�n an dieser Stelle
        PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
        double scaleX = pageLayout.getPrintableWidth() / node.getBoundsInParent().getWidth();
        double scaleY = pageLayout.getPrintableHeight() / node.getBoundsInParent().getHeight();
        node.getTransforms().add(new Scale(scaleX, scaleY));
 
        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job != null) {
            boolean success = job.printPage(node);
            if (success) {
                job.endJob();
            }
        }
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		allPrinter = Printer.getAllPrinters();
		// die verf�gbaren Printer setzen
		allPrinter.stream().forEach(ps -> printerChoiceBox.getItems().add(ps.getName()));
		printerChoiceBox.getSelectionModel().select(0); // 1. Item w�hlen, um Exc zu verhindern
		
		// Alle Sch�ler anzeigen, die mind. eine Runde gelaufen sind
		// zuerst cellfactory setzen
		studentListView.setCellFactory(l -> new ListCell<Chip>() {
	        @Override
	        protected void updateItem(Chip chip, boolean empty) {
	            super.updateItem(chip, empty);
	            if (empty || chip == null) {
	                setText(null);
	            } else {
	                setText(chip.getStudentName());
	            }
	        }
		});
		// Sch�ler filtern und setzen
		competitionController.getChipsController().getChips().stream()
			.filter(c -> c.getLapCount() > 0)
			.forEach(c -> studentListView.getItems().add(c));
		
		// Wenn ein neues Item ausgew�hlt wurde: Die WebView rendern
		studentListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Chip>() {
		    @Override
		    public void changed(ObservableValue<? extends Chip> observable, Chip oldValue, Chip newValue) {
		        // Rendern mit dem ausgew�hlen Chip, welcher in newValue steht
		        renderDocument(newValue);
		    }
		});
		
		// Die Preview setzen (den ersten Chip in der Liste rendern)
		renderDocument(competitionController.getChipsController().getChips().get(0));
		previewWebView.setZoom(0.38);

	}
	
	// Rendert das HTML-Dokument mit einem entsprechendem Chip
	private void renderDocument(Chip c) {
		
		// Die Rundenliste in Form einer HTML-Tabelle vorbereiten
		LinkedList<Lap> laps = c.getLaps();
		StringBuilder tableBuilder = new StringBuilder();
		tableBuilder.append("<table>");
		// Start (0 ist Runde -1 -> somit ist 1 die 0. Runde)
		tableBuilder.append("<tr><td>Start</td><td>" + laps.get(1).getTimestampAsString() + "</td></tr>");
		for(int i = 2; i < laps.size(); i++) {
			Lap prevLap = laps.get(i - 1);
			Lap lap = laps.get(i);
			// Die Differenz zur vorherigen Runde errechnen.
			String diff = ltMinusLt(lap.getTimestamp(), prevLap.getTimestamp(), "HH:mm:ss");
			
			tableBuilder.append("<tr><td class=\"lapCol\">Runde " + lap.getNumber() + "</td><td> +" + diff + "</td></tr>");
		}	
		tableBuilder.append("</table>");
		// final, damit die Tabelle im Lambda verf�gbar ist.
		final String table = tableBuilder.toString();
		final Competition comp = competitionController.getCompetition();
		try {
			// Die Vorlage auslesen
			List<String> list = Files.readAllLines(new File(Data.DIR + "/" + Data.BASIC_DIR + "/urkunde-vorlage.html").toPath());
		
			// Zeile f�r Zeile in die Datei "letzterDruck.html" schreiben.
			// Dabei werden die entsprechenden Templatevariablen ersetzt:
			//   - <%name%>            Name des Sch�lers
			//   - <%runden%>          Anzahl der gelaufenden Runden
			//   - <%rundenliste%>     �bersicht �ber die gelaufenen Runden mit Zeitstempel
			//   - <%wettkampfdetail%> (Optional) Infos �ber den Wettkampf 
			PrintWriter pw = new PrintWriter(new FileWriter(Data.DIR + "/" + Data.BASIC_DIR + "/letzterDruck.html"));
			// Hier darf nicht gefiltert werden, da sonst HTML und CSS verloren geht
			list.stream().map(line -> { 
				String lapCount = c.getLapCount() + " Runden in " + (comp.getTime() / 60) + " Minuten";
				// Falls Wettkampf auf Distanz, muss die Ausgabe anders aussehen
				// Wegen halben Runden und genaue Zeit
				if(comp.getType() == 1) {
					lapCount = comp.getLapCount() + " Runden in " + ltMinusLt(c.getLaps().getLast().getTimestamp(), c.getLaps().get(1).getTimestamp(), "mm:ss") + " Minuten";
				}
				return line = line.replaceAll("<%name%>", c.getStudentName())
						.replaceAll("<%runden%>", "" + lapCount)
						.replaceAll("<%rundenliste%>", "" + table);
			}).forEach(pw::println);
			
			pw.close();
			
			// Anzeigen
			previewWebView.getEngine().load(new File(Data.DIR + "/" + Data.BASIC_DIR + "/letzterDruck.html").toURI().toURL().toString());
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new StandardAlert(StandardMessageType.ERROR);
		}
	}
	
	private void renderListOfResults() {
		
		// Die Erbenisliste (lor -> list of results) erstellen
		StringBuilder lorBuilder = new StringBuilder();
		lorBuilder.append("<table><tr><th>Name<th><th>Runden<th></tr>");
		
		for(Chip chip : competitionController.getChipsController().getChips()) {
			lorBuilder.append("<tr><td>" + chip.getStudentName() + "<td>"
					+ "<td>" + chip.getLapCount() + "</td></tr>");
		}
		
		lorBuilder.append("</table>");
		final String lor = lorBuilder.toString();
		
		try {
			// Die Vorlage auslesen
			List<String> list = Files.readAllLines(new File(Data.DIR + "/" + Data.BASIC_DIR + "/ergebnisliste-vorlage.html").toPath());
		
			// Zeile f�r Zeile in die Datei "letzterDruck.html" schreiben.
			// Dabei werden die entsprechenden Templatevariablen ersetzt:
			//   - <%list%>            Dort wird die Tabelle reingeschrieben
			PrintWriter pw = new PrintWriter(new FileWriter(Data.DIR + "/" + Data.BASIC_DIR + "/letzterDruck.html"));
			
			// Hier darf nicht gefiltert werden, da sonst HTML und CSS verloren geht
			list.stream().map(line -> { 
				return line = line.replaceAll("<%list%>", lor);
			}).forEach(pw::println);
			
			pw.close();
			
			// Anzeigen
			previewWebView.getEngine().load(new File(Data.DIR + "/" + Data.BASIC_DIR + "/letzterDruck.html").toURI().toURL().toString());
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			new StandardAlert(StandardMessageType.ERROR);
		}
	}
	
	private String ltMinusLt(LocalTime lt, LocalTime minus, String pattern) {
		return lt.minusHours(minus.getHour())
			.minusMinutes(minus.getMinute())
			.minusSeconds(minus.getSecond())
			.minusNanos(minus.getNano())
			.format(DateTimeFormatter.ofPattern(pattern));
	}

	// GETTER UND SETTER
	
	private Printer getSelectedPrinter() {
		// Ausgew�hlten Printer holen
		// (muss leider so kompliziert sein, anders kann man auf die
		//  Elemente des ObservableSet nicht zugreifen...)
		return allPrinter.stream().filter(p -> {
			if(p.getName().equals(printerChoiceBox.getSelectionModel().getSelectedItem())) {
				return true;
			}
			return false;
		}).findFirst().get(); // endlich das Element bekommen..
	}

}

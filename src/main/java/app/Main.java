package app;

import app.patterns.observer.StudyEventHub;
import app.service.StudyPlannerFacade;
import app.storage.PropertiesRepository;
import app.ui.PlannerFrame;
import app.ui.PlannerTheme;
import java.nio.file.Path;
import javax.swing.*;

public final class Main {
    private Main() { }
    public static void main(String[] args) {
        Path path = args.length == 2 && args[0].equals("--data") ? Path.of(args[1])
            : Path.of("data", "study-sprint.properties");
        try {
            PlannerTheme.install(true);
            StudyPlannerFacade facade = new StudyPlannerFacade(new PropertiesRepository(path), new StudyEventHub());
            SwingUtilities.invokeLater(() -> new PlannerFrame(facade).setVisible(true));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Cannot start Study Sprint: " + ex.getMessage(),
                "Startup error", JOptionPane.ERROR_MESSAGE);
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }
}

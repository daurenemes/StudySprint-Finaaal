package app;

import app.patterns.observer.StudyEventHub;
import app.service.StudyPlannerFacade;
import app.storage.PropertiesRepository;
import app.ui.PlannerFrame;
import app.ui.PlannerTheme;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import app.model.*;
import app.storage.SampleData;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public final class ScreenshotMain {
    private ScreenshotMain() { }
    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 4) throw new IllegalArgumentException("Expected data path, output PNG path, optional tab index and theme");
        PlannerTheme.install(args.length < 4 || !args[3].equals("light"));
        PropertiesRepository repository = new PropertiesRepository(Path.of(args[0]));
        if (!java.nio.file.Files.exists(Path.of(args[0]))) savePreviewData(repository);
        PlannerFrame[] frame = new PlannerFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                frame[0] = new PlannerFrame(new StudyPlannerFacade(repository, new StudyEventHub()));
                if (args.length >= 3) frame[0].selectTab(Integer.parseInt(args[2]));
                frame[0].setVisible(true);
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        Thread.sleep(1300);
        Rectangle bounds = frame[0].getContentPane().getBounds();
        Point origin = frame[0].getContentPane().getLocationOnScreen();
        bounds.setLocation(origin);
        BufferedImage image = new Robot().createScreenCapture(bounds);
        ImageIO.write(image, "png", new File(args[1]));
        SwingUtilities.invokeAndWait(frame[0]::dispose);
    }

    private static void savePreviewData(PropertiesRepository repository) throws Exception {
        Snapshot sample = SampleData.create();
        LocalDate today = LocalDate.now();
        StudyTask first = sample.tasks().get(0);
        StudyTask math = sample.tasks().get(2);
        List<StudyTask> tasks = List.of(first.toBuilder().completed(true).firstCompletion(today).build(),
            sample.tasks().get(1), math.toBuilder().firstCompletion(today.minusDays(2)).build());
        List<FocusSession> sessions = List.of(
            new FocusSession("preview-1", math.id(), 25, today.minusDays(5), true),
            new FocusSession("preview-2", math.id(), 15, today.minusDays(2), true),
            new FocusSession("preview-3", first.id(), 25, today.minusDays(1), true),
            new FocusSession("preview-4", first.id(), 25, today, true),
            new FocusSession("preview-5", first.id(), 15, today, true));
        repository.save(new Snapshot(sample.plans(), tasks, sessions));
    }
}

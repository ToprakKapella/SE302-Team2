package com.team2.examscheduler.gui;

import com.team2.examscheduler.constraints.ConstraintChecker;
import com.team2.examscheduler.io.CsvImporter;
import com.team2.examscheduler.io.ImportException;
import com.team2.examscheduler.model.*;
import com.team2.examscheduler.scheduler.ExamScheduler;
import com.team2.examscheduler.views.ScheduleViews;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Main window of the Exam Scheduler (JavaFX). */
public final class MainFrame extends Application {
    private final DataStore store = new DataStore();
    private Schedule schedule;
    private Stage stage;

    private final TableView<String[]> roomTable = table("Classroom", "Capacity");
    private final TableView<String[]> courseTable = table("Course", "Students");
    private final ListView<String> studentList = new ListView<>();
    private final Spinner<Integer> days = new Spinner<>(1, 60, 10);
    private final Spinner<Integer> slots = new Spinner<>(1, 20, 4);
    private final ComboBox<ExamScheduler.Goal> goal = new ComboBox<>(FXCollections.observableArrayList(ExamScheduler.Goal.values()));
    private final TextArea status = new TextArea();
    private final ComboBox<String> viewKind = new ComboBox<>(FXCollections.observableArrayList(
            "By classroom", "By student", "By course", "By day"));
    private final ComboBox<String> viewKey = new ComboBox<>();
    private final TableView<String[]> viewTable = new TableView<>();

    /** Opens the window (called by Main when no command-line arguments are given). */
    public static void launch() { Application.launch(MainFrame.class); }

    @Override
    public void start(Stage primary) {
        stage = primary;
        TabPane tabs = new TabPane(
                tab("Classrooms", roomsPanel()), tab("Courses", coursesPanel()),
                tab("Schedule", schedulePanel()), tab("Views", viewsPanel()));
        tabs.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> refreshViewKeys());
        BorderPane root = new BorderPane(tabs);
        root.setTop(menu());
        stage.setTitle("Exam Scheduler - Team 2");
        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    private static Tab tab(String title, javafx.scene.Node n) { Tab t = new Tab(title, n); t.setClosable(false); return t; }

    private static TableView<String[]> table(String... cols) {
        TableView<String[]> t = new TableView<>();
        setColumns(t, cols);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return t;
    }

    private static void setColumns(TableView<String[]> t, String... cols) {
        t.getColumns().clear();
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<String[], String> c = new TableColumn<>(cols[i]);
            c.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[idx]));
            c.setSortable(false);
            t.getColumns().add(c);
        }
    }

    // ---------- menu ----------
    private MenuBar menu() {
        Menu file = new Menu("File");
        file.getItems().addAll(item("Import classrooms...", () -> importFile(0)),
                item("Import registrations...", () -> importFile(1)),
                item("Import student list...", () -> importFile(2)),
                new SeparatorMenuItem(), item("Exit", Platform::exit));
        Menu help = new Menu("Help");
        help.getItems().add(item("Help contents", this::showHelp));
        return new MenuBar(file, help);
    }

    private static MenuItem item(String text, Runnable r) { MenuItem i = new MenuItem(text); i.setOnAction(e -> r.run()); return i; }

    private void showHelp() {
        String html;
        try (InputStream in = MainFrame.class.getResourceAsStream("/help.html")) {
            html = in == null ? "<html>Help file not found.</html>" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            html = "<html>Help file could not be read.</html>";
        }
        WebView w = new WebView();
        w.getEngine().loadContent(html);
        Stage s = new Stage();
        s.setTitle("Help");
        s.setScene(new Scene(w, 600, 420));
        s.show();
    }

    // ---------- import ----------
    private void importFile(int kind) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv", "*.txt"));
        File f = fc.showOpenDialog(stage);
        if (f == null) return;
        try {
            if (kind == 0) store.replaceClassrooms(CsvImporter.readClassrooms(f.toPath()));
            else if (kind == 1) store.replaceCourses(CsvImporter.readRegistrations(f.toPath()));
            else store.replaceStudentList(CsvImporter.readStudentList(f.toPath()));
            schedule = null;
            refreshTables();
            String msg = "Imported " + f.getName() + ".";
            if (kind != 0 && store.hasStudentList() && !store.getCourses().isEmpty()) {
                List<String> unknown = store.unknownStudents();
                if (!unknown.isEmpty()) msg += " Warning: " + unknown.size() + " registered student(s) are not in the student list, e.g. " + unknown.get(0) + ".";
            }
            status.setText(msg);
        } catch (ImportException ex) {
            error("Import failed (nothing was changed):\n" + ex.getMessage());
        } catch (RuntimeException ex) {
            error("Import failed: " + ex.getMessage());
        }
    }

    private void error(String m) { new Alert(Alert.AlertType.ERROR, m).showAndWait(); }

    // ---------- classrooms ----------
    private BorderPane roomsPanel() {
        Button add = new Button("Add"), edit = new Button("Edit"), rem = new Button("Remove");
        add.setOnAction(e -> ask("Add classroom", new String[]{"Name:", "Capacity:"}, new String[]{"", ""}, v -> {
            store.addClassroom(new Classroom(v[0].trim(), Integer.parseInt(v[1].trim())));
            changed();
        }));
        edit.setOnAction(e -> {
            String[] r = roomTable.getSelectionModel().getSelectedItem();
            if (r == null) { error("Select a classroom first."); return; }
            ask("Edit classroom", new String[]{"Name:", "Capacity:"}, new String[]{r[0], r[1]}, v -> {
                store.renameClassroom(r[0], v[0].trim(), Integer.parseInt(v[1].trim()));
                changed();
            });
        });
        rem.setOnAction(e -> {
            String[] r = roomTable.getSelectionModel().getSelectedItem();
            if (r == null) { error("Select a classroom first."); return; }
            store.removeClassroom(r[0]);
            changed();
        });
        return panel(roomTable, new HBox(8, add, edit, rem));
    }

    // ---------- courses ----------
    private BorderPane coursesPanel() {
        courseTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> refreshStudents());
        Button add = new Button("Add"), ren = new Button("Rename"), rem = new Button("Remove");
        add.setOnAction(e -> ask("Add course", new String[]{"Code:"}, new String[]{""}, v -> {
            store.addCourse(new Course(v[0].trim()));
            changed();
        }));
        ren.setOnAction(e -> {
            String code = selectedCourse();
            if (code == null) return;
            ask("Rename course", new String[]{"Code:"}, new String[]{code}, v -> { store.renameCourse(code, v[0].trim()); changed(); });
        });
        rem.setOnAction(e -> {
            String code = selectedCourse();
            if (code == null) return;
            store.removeCourse(code);
            changed();
        });
        Button addS = new Button("Add student"), remS = new Button("Remove student");
        addS.setOnAction(e -> {
            String code = selectedCourse();
            if (code == null) return;
            ask("Add student", new String[]{"Student ID:"}, new String[]{""}, v -> {
                String id = v[0].trim();
                if (id.isEmpty()) throw new IllegalArgumentException("Student ID cannot be empty.");
                if (!store.getCourse(code).addStudent(id)) throw new IllegalArgumentException("Student already registered.");
                changed();
            });
        });
        remS.setOnAction(e -> {
            String code = selectedCourse();
            String s = studentList.getSelectionModel().getSelectedItem();
            if (code == null || s == null) { error("Select a course and a student."); return; }
            store.getCourse(code).removeStudent(s);
            changed();
        });
        BorderPane left = panel(courseTable, new HBox(8, add, ren, rem));
        BorderPane right = panel(studentList, new HBox(8, addS, remS));
        right.setTop(new Label("Students of the selected course"));
        SplitPane sp = new SplitPane(left, right);
        return new BorderPane(sp);
    }

    private String selectedCourse() {
        String[] r = courseTable.getSelectionModel().getSelectedItem();
        if (r == null) { error("Select a course first."); return null; }
        return r[0];
    }

    private static BorderPane panel(javafx.scene.Node center, HBox buttons) {
        BorderPane p = new BorderPane(center);
        buttons.setPadding(new Insets(8));
        p.setBottom(buttons);
        return p;
    }

    // ---------- schedule ----------
    private BorderPane schedulePanel() {
        days.setEditable(true); slots.setEditable(true);
        days.setPrefWidth(80); slots.setPrefWidth(80);
        goal.getSelectionModel().selectFirst();
        Button gen = new Button("Generate");
        gen.setOnAction(e -> generate());
        HBox top = new HBox(8, new Label("Days:"), days, new Label("Slots per day:"), slots, new Label("Goal:"), goal, gen);
        top.setPadding(new Insets(8));
        status.setEditable(false);
        status.setWrapText(true);
        status.setText("Import data, choose the exam period and press Generate.");
        BorderPane p = new BorderPane(status);
        p.setTop(top);
        return p;
    }

    private void generate() {
        List<Course> courses = store.getCourses();
        List<Classroom> rooms = store.getClassrooms();
        if (courses.isEmpty() || rooms.isEmpty()) { error("Please load classrooms and registrations first."); return; }
        days.increment(0); slots.increment(0); // commit typed values
        ExamPeriod period = new ExamPeriod(days.getValue(), slots.getValue());
        ExamScheduler.Goal g = goal.getValue();
        status.setText("Generating...");
        Task<ExamScheduler.Result> task = new Task<ExamScheduler.Result>() {
            @Override protected ExamScheduler.Result call() {
                return new ExamScheduler().generate(courses, rooms, period, g, 20_000);
            }
        };
        task.setOnSucceeded(e -> {
            ExamScheduler.Result r = task.getValue();
            if (r.isSuccess()) {
                List<String> v = ConstraintChecker.check(r.getSchedule(), courses, rooms);
                if (v.isEmpty()) { schedule = r.getSchedule(); status.setText(r.getMessage() + " Open the Views tab to see it."); }
                else status.setText("Internal error, schedule rejected: " + v.get(0));
            } else {
                status.setText(r.getMessage());
            }
            refreshViewKeys();
        });
        task.setOnFailed(e -> status.setText("Generation failed: " + task.getException().getMessage()));
        Thread t = new Thread(task, "scheduler");
        t.setDaemon(true);
        t.start();
    }

    // ---------- views ----------
    private BorderPane viewsPanel() {
        viewKind.getSelectionModel().selectFirst();
        viewKind.setOnAction(e -> refreshViewKeys());
        viewKey.setOnAction(e -> refreshView());
        viewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        HBox top = new HBox(8, viewKind, viewKey);
        top.setPadding(new Insets(8));
        BorderPane p = new BorderPane(viewTable);
        p.setTop(top);
        return p;
    }

    private void refreshViewKeys() {
        viewKey.getItems().clear();
        if (schedule == null) {
            setColumns(viewTable, "No schedule generated yet");
            viewTable.getItems().clear();
            return;
        }
        int kind = viewKind.getSelectionModel().getSelectedIndex();
        if (kind == 0) for (Classroom c : store.getClassrooms()) viewKey.getItems().add(c.getName());
        else if (kind == 1) viewKey.getItems().addAll(new TreeSet<>(store.registeredStudents()));
        else if (kind == 3) for (int d = 1; d <= schedule.getPeriod().getDays(); d++) viewKey.getItems().add("Day " + d);
        if (!viewKey.getItems().isEmpty()) viewKey.getSelectionModel().selectFirst();
        refreshView();
    }

    private void refreshView() {
        if (schedule == null) return;
        int kind = viewKind.getSelectionModel().getSelectedIndex();
        String key = viewKey.getValue();
        String[] cols; List<String[]> rows = new ArrayList<>();
        if (kind == 0) { cols = ScheduleViews.CLASSROOM_COLUMNS; if (key != null) rows = ScheduleViews.forClassroom(schedule, key); }
        else if (kind == 1) { cols = ScheduleViews.STUDENT_COLUMNS; if (key != null) rows = ScheduleViews.forStudent(schedule, store.getCourses(), key); }
        else if (kind == 2) { cols = ScheduleViews.COURSE_COLUMNS; rows = ScheduleViews.forCourses(schedule); }
        else { cols = ScheduleViews.DAY_COLUMNS; if (key != null) rows = ScheduleViews.forDay(schedule, Integer.parseInt(key.substring(4))); }
        setColumns(viewTable, cols);
        viewTable.setItems(FXCollections.observableArrayList(rows));
    }

    // ---------- helpers ----------
    private void changed() { schedule = null; refreshTables(); status.setText("Data changed - generate the schedule again."); }

    private void refreshTables() {
        roomTable.getItems().clear();
        for (Classroom c : store.getClassrooms()) roomTable.getItems().add(new String[]{c.getName(), String.valueOf(c.getCapacity())});
        courseTable.getItems().clear();
        for (Course c : store.getCourses()) courseTable.getItems().add(new String[]{c.getCode(), String.valueOf(c.size())});
        refreshStudents();
        refreshViewKeys();
    }

    private void refreshStudents() {
        studentList.getItems().clear();
        String[] r = courseTable.getSelectionModel().getSelectedItem();
        if (r == null) return;
        Course c = store.getCourse(r[0]);
        if (c != null) studentList.getItems().addAll(new TreeSet<>(c.getStudents()));
    }

    /** Shows a small form dialog; the action runs on OK and its errors are shown to the user. */
    private void ask(String title, String[] labels, String[] initial, java.util.function.Consumer<String[]> action) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(title);
        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8); g.setPadding(new Insets(12));
        TextField[] fields = new TextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            fields[i] = new TextField(initial[i]);
            g.addRow(i, new Label(labels[i]), fields[i]);
        }
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> res = d.showAndWait();
        if (!res.isPresent() || res.get() != ButtonType.OK) return;
        String[] v = new String[labels.length];
        for (int i = 0; i < v.length; i++) v[i] = fields[i].getText();
        try { action.accept(v); }
        catch (NumberFormatException ex) { error("Please enter a whole number for the capacity."); }
        catch (RuntimeException ex) { error(ex.getMessage()); }
    }
}

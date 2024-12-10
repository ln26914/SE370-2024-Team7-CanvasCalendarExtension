package com.example.CalanderCanvas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class CalanderCanvasApplication {

    private static final Map<LocalDate, List<String>> assignments = new HashMap<>();
    private static final String PROPERTIES_FILE_PATH = "application.properties";

    static {
        // Adding some dummy assignments for December 2024
        assignments.put(LocalDate.of(2024, 12, 5), List.of("Math Homework", "Science Project"));
        assignments.put(LocalDate.of(2024, 12, 12), List.of("English Essay"));
        assignments.put(LocalDate.of(2024, 12, 20), List.of("History Presentation"));
    }

    public static void main(String[] args) {
		System.out.println("Headless mode: " + GraphicsEnvironment.isHeadless());
        SpringApplication.run(CalanderCanvasApplication.class, args);
        SwingUtilities.invokeLater(CalanderCanvasApplication::createMainPanel);
    }

    private static void createMainPanel() {
        // Create the main frame
        JFrame frame = new JFrame("Canvas Calendar");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        // Header panel
        JPanel headerPanel = new JPanel();
        JLabel titleLabel = new JLabel("Canvas Calendar", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerPanel.add(titleLabel);
        frame.add(headerPanel, BorderLayout.NORTH);

        // Control buttons panel
        JPanel controlsPanel = new JPanel();
        JButton loginButton = new JButton("Login");
        JButton refreshButton = new JButton("Refresh Data");
        JButton viewCalendarButton = new JButton("View Calendar");
        controlsPanel.add(loginButton);
        controlsPanel.add(refreshButton);
        controlsPanel.add(viewCalendarButton);
        frame.add(controlsPanel, BorderLayout.SOUTH);

        // Add actions to buttons
        loginButton.addActionListener(e -> handleLogin(frame));
        refreshButton.addActionListener(e -> handleRefresh());
        viewCalendarButton.addActionListener(e -> showCalendar(frame));

        // Display the frame
        frame.setVisible(true);
    }

    private static void handleLogin(JFrame parentFrame) {
        String apiKey = JOptionPane.showInputDialog(parentFrame, "Enter your API key:", "Login", JOptionPane.PLAIN_MESSAGE);
        if (apiKey != null && !apiKey.isEmpty()) {
            saveApiKey(apiKey);
            JOptionPane.showMessageDialog(parentFrame, "API key saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parentFrame, "API key cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void saveApiKey(String apiKey) {
        try (FileWriter writer = new FileWriter(PROPERTIES_FILE_PATH)) {
            writer.write("spring.application.name=CalanderCanvas\n");
            writer.write("api.key=" + apiKey + "\n");
            writer.write("canvas.url=https://canvas.beta.instructure.com/\n");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving API key: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void handleRefresh() {
        JOptionPane.showMessageDialog(null, "Data refreshed successfully.", "Refresh", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void showCalendar(JFrame parentFrame) {
        JFrame calendarFrame = new JFrame("Calendar");
        calendarFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        calendarFrame.setSize(1000, 600);

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel navigationPanel = new JPanel();
        JButton prevButton = new JButton("<");
        JButton nextButton = new JButton(">");
        JLabel monthYearLabel = new JLabel("", SwingConstants.CENTER);
        navigationPanel.add(prevButton);
        navigationPanel.add(monthYearLabel);
        navigationPanel.add(nextButton);
        mainPanel.add(navigationPanel, BorderLayout.NORTH);

        JPanel calendarPanel = new JPanel();
        mainPanel.add(calendarPanel, BorderLayout.CENTER);

        JPanel assignmentsPanel = new JPanel();
        assignmentsPanel.setLayout(new BorderLayout());
        JLabel assignmentsLabel = new JLabel("Assignments", SwingConstants.CENTER);
        assignmentsLabel.setFont(new Font("Arial", Font.BOLD, 18));
        JTextArea assignmentsArea = new JTextArea();
        assignmentsArea.setEditable(false);
        JScrollPane assignmentsScroll = new JScrollPane(assignmentsArea);
        assignmentsPanel.add(assignmentsLabel, BorderLayout.NORTH);
        assignmentsPanel.add(assignmentsScroll, BorderLayout.CENTER);
        mainPanel.add(assignmentsPanel, BorderLayout.EAST);

        YearMonth[] currentYearMonth = {YearMonth.now()};
        updateCalendar(calendarPanel, currentYearMonth[0], monthYearLabel, assignmentsArea);

        prevButton.addActionListener(e -> {
            currentYearMonth[0] = currentYearMonth[0].minusMonths(1);
            updateCalendar(calendarPanel, currentYearMonth[0], monthYearLabel, assignmentsArea);
        });

        nextButton.addActionListener(e -> {
            currentYearMonth[0] = currentYearMonth[0].plusMonths(1);
            updateCalendar(calendarPanel, currentYearMonth[0], monthYearLabel, assignmentsArea);
        });

        calendarFrame.add(mainPanel);
        calendarFrame.setVisible(true);
    }

    private static void updateCalendar(JPanel calendarPanel, YearMonth yearMonth, JLabel monthYearLabel, JTextArea assignmentsArea) {
        calendarPanel.removeAll();
        calendarPanel.setLayout(new GridLayout(0, 7));

        // Add day headers
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String day : days) {
            JLabel dayLabel = new JLabel(day, SwingConstants.CENTER);
            dayLabel.setFont(new Font("Arial", Font.BOLD, 14));
            calendarPanel.add(dayLabel);
        }

        LocalDate firstDayOfMonth = yearMonth.atDay(1);
        int daysInMonth = yearMonth.lengthOfMonth();
        int firstDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() % 7; // Adjust for Sunday start

        // Fill initial empty cells before the first day
        for (int i = 0; i < firstDayOfWeek; i++) {
            calendarPanel.add(new JLabel(""));
        }

        // Add buttons for each day of the month
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDate = yearMonth.atDay(day);
            JButton dayButton = new JButton(String.valueOf(day));

            // Highlight days with assignments by setting the background color to red
            if (assignments.containsKey(currentDate)) {
                dayButton.setOpaque(true);
                dayButton.setBackground(Color.RED);
                dayButton.setForeground(Color.WHITE);
            }

            // Add action listener to show assignments
            dayButton.addActionListener(e -> {
                List<String> dayAssignments = assignments.getOrDefault(currentDate, List.of("No assignments"));
                assignmentsArea.setText(String.join("\n", dayAssignments));
            });
            calendarPanel.add(dayButton);
        }

        // Update the month and year label
        monthYearLabel.setText(yearMonth.getMonth() + " " + yearMonth.getYear());

        calendarPanel.revalidate();
        calendarPanel.repaint();
    }
}

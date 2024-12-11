package com.example.CalanderCanvas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.net.URI;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List; 
import java.util.Map;  
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * CalanderCanvasApplication:
 * This is the main entry point of the Spring Boot application that also creates a Swing-based UI for displaying:
 * - A calendar with clickable days.
 * - Assignments fetched from a backend service.
 * - Daily and monthly aggregated points bars (graphs) for visualization.
 * 
 * The application:
 * 1. Prompts the user for a Canvas API key.
 * 2. Runs the Spring application which uses the API key in the CanvasAPIService.
 * 3. Once the backend is ready, it displays a calendar UI where selecting a date shows the assignments for that day.
 * 4. Shows bars indicating how many points are earned versus the total points for that day and for the entire month.
 */
@SpringBootApplication
public class CalanderCanvasApplication {

    // The main application frame (window)
    private static JFrame frame;
    // A map of LocalDate to a list of assignment strings. Each string holds assignment data.
    private static Map<LocalDate, List<String>> assignments = new HashMap<>();

    // UI components for displaying assignments and navigation
    private static JPanel assignmentsListPanel; // Panel that holds the assignments list and graphs
    private static JLabel assignmentsLabel;     // Label at the top of the assignments panel
    private static JPanel calendarPanel;         // Panel where the calendar (days of month) is displayed
    private static JLabel monthYearLabel;        // Label showing the current month and year displayed
    private static JButton prevButton;           // Button to go to the previous month
    private static JButton nextButton;           // Button to go to the next month

    // Maximum width in pixels for the assignment name text area, to wrap long names
    private static final int MAX_NAME_WIDTH_PX = 200;

    public static void main(String[] args) {
        System.out.println("Headless mode: " + GraphicsEnvironment.isHeadless());

        // Prompt the user for the Canvas API key once before starting the Spring context.
        String userApiKey = JOptionPane.showInputDialog(null, 
            "Please enter your Canvas API key:", 
            "API Key Required", 
            JOptionPane.QUESTION_MESSAGE);

        // If user cancels or provides no API key, exit the application.
        if (userApiKey == null || userApiKey.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, 
                "No API key entered. Exiting application.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Set the API key as a system property so that @Value("${api.key}") in CanvasAPIService can use it.
        System.setProperty("api.key", userApiKey.trim());

        // Start the Spring application
        SpringApplication.run(CalanderCanvasApplication.class, args);

        // After Spring Boot context is initialized, create the main UI panel on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            createMainPanel();
        });
    }

    /**
     * createMainPanel():
     * This method sets up the main UI components:
     * - A header with a title.
     * - A right-side panel for assignments and graphs.
     * - A center panel for the calendar and month navigation.
     * - A bottom panel with a refresh button.
     */
    private static void createMainPanel() {
        frame = new JFrame("Canvas Calendar");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Header panel with a title
        JPanel headerPanel = new JPanel();
        JLabel titleLabel = new JLabel("Canvas Calendar", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerPanel.add(titleLabel);
        frame.add(headerPanel, BorderLayout.NORTH);

        // The assignments panel on the right
        JPanel assignmentsPanel = new JPanel(new BorderLayout());
        assignmentsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Label for the assignments section
        assignmentsLabel = new JLabel("Assignments", JLabel.CENTER);
        assignmentsLabel.setFont(new Font("Arial", Font.BOLD, 18));
        assignmentsPanel.add(assignmentsLabel, BorderLayout.NORTH);

        // Panel that will list assignments vertically
        assignmentsListPanel = new JPanel();
        assignmentsListPanel.setLayout(new BoxLayout(assignmentsListPanel, BoxLayout.Y_AXIS));
        assignmentsListPanel.setBackground(Color.WHITE);
        assignmentsPanel.add(assignmentsListPanel, BorderLayout.CENTER);

        frame.add(assignmentsPanel, BorderLayout.EAST);

        // Navigation panel (previous/next month buttons) and month-year label
        JPanel navigationPanel = new JPanel();
        prevButton = new JButton("<");
        nextButton = new JButton(">");
        monthYearLabel = new JLabel("", JLabel.CENTER);
        monthYearLabel.setFont(new Font("Arial", Font.BOLD, 16));

        navigationPanel.add(prevButton);
        navigationPanel.add(monthYearLabel);
        navigationPanel.add(nextButton);

        // Calendar panel to show days of the selected month
        calendarPanel = new JPanel();
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(navigationPanel, BorderLayout.NORTH);
        leftPanel.add(calendarPanel, BorderLayout.CENTER);
        frame.add(leftPanel, BorderLayout.CENTER);

        // Control panel at the bottom with a "Refresh Assignments" button
        JPanel controlsPanel = new JPanel();
        JButton refreshButton = new JButton("Refresh Assignments");
        controlsPanel.add(refreshButton);
        frame.add(controlsPanel, BorderLayout.SOUTH);

        // Action listener to refresh assignments data from the backend
        refreshButton.addActionListener(e -> handleRefresh());

        // Initialize the calendar to the current month
        YearMonth currentYearMonth = YearMonth.now();
        updateCalendar(currentYearMonth);

        // Add actions to previous and next buttons to navigate months
        prevButton.addActionListener(e -> {
            YearMonth ym = getCurrentDisplayedYearMonth();
            ym = ym.minusMonths(1);
            updateCalendar(ym);
        });

        nextButton.addActionListener(e -> {
            YearMonth ym = getCurrentDisplayedYearMonth();
            ym = ym.plusMonths(1);
            updateCalendar(ym);
        });

        // Final frame setup
        frame.setSize(1000, 600);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * handleRefresh():
     * Fetches updated assignments data from the backend, clears the old data, and repopulates the assignments map.
     * Shows a message dialog upon successful refresh or an error if something goes wrong.
     */
    private static void handleRefresh() {
        try {
            List<String> assignmentsData = fetchAssignmentsFromBackend();

            // Clear existing assignment data
            assignments.clear();

            // Parse each assignment string returned from the backend
            for (String entry : assignmentsData) {
                String assignmentName = null;
                String dueDateStr = null;
                String totalPoints = null;
                String pointsEarned = null;

                // Each assignment line is CSV-like: "Assignment: NAME, Due Date: DATE, Total Points: TP, Points Earned: PE"
                String[] parts = entry.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.startsWith("Assignment:")) {
                        assignmentName = trimmed.replace("Assignment:", "").trim();
                    } else if (trimmed.startsWith("Due Date:")) {
                        dueDateStr = trimmed.replace("Due Date:", "").trim();
                    } else if (trimmed.startsWith("Total Points:")) {
                        totalPoints = trimmed.replace("Total Points:", "").trim();
                    } else if (trimmed.startsWith("Points Earned:")) {
                        pointsEarned = trimmed.replace("Points Earned:", "").trim();
                    }
                }

                // Only add to the map if we have a valid assignment name and due date
                if (assignmentName != null && dueDateStr != null && !dueDateStr.equals("No Due Date")) {
                    try {
                        // Parse the due date, convert to system's timezone, and get the LocalDate
                        ZonedDateTime zdt = ZonedDateTime.parse(dueDateStr)
                                .withZoneSameInstant(java.time.ZoneId.systemDefault());
                        LocalDate date = zdt.toLocalDate();

                        // Store record as "AssignmentName|ZonedDateTime|TotalPoints|PointsEarned"
                        String record = assignmentName + "|" + zdt.toString() + "|" +
                                        (totalPoints != null ? totalPoints : "N/A") + "|" +
                                        (pointsEarned != null ? pointsEarned : "");
                        assignments.computeIfAbsent(date, k -> new ArrayList<>()).add(record);
                    } catch (DateTimeParseException ex) {
                        System.err.println("Failed to parse date: " + dueDateStr);
                    }
                }
            }

            JOptionPane.showMessageDialog(frame, 
                "Assignments refreshed successfully.", 
                "Refresh", 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, 
                "Error refreshing assignments: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * fetchAssignmentsFromBackend():
     * Calls the backend endpoint "/course-assignments" to retrieve a list of assignment data strings.
     * Returns an empty list if no data is found.
     */
    private static List<String> fetchAssignmentsFromBackend() {
        RestTemplate restTemplate = new RestTemplate();
        // Add JSON converter to parse JSON arrays of strings
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

        URI uri = URI.create("http://localhost:8080/course-assignments");
        List<String> response = restTemplate.getForObject(uri, List.class);
        return response != null ? response : Collections.emptyList();
    }

    /**
     * updateCalendar(YearMonth yearMonth):
     * Updates the calendar display to show the given month. It creates day buttons for each date.
     * If a date has assignments, the button is highlighted in red.
     */
    private static void updateCalendar(YearMonth yearMonth) {
        calendarPanel.removeAll();
        calendarPanel.setLayout(new GridLayout(0, 7));

        // Days of the week header
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String day : days) {
            JLabel dayLabel = new JLabel(day, JLabel.CENTER);
            dayLabel.setFont(new Font("Arial", Font.BOLD, 14));
            calendarPanel.add(dayLabel);
        }

        LocalDate firstDayOfMonth = yearMonth.atDay(1);
        int daysInMonth = yearMonth.lengthOfMonth();
        // Determine how many empty cells before the first day (to align the calendar)
        int firstDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() % 7;

        // Add empty labels for days before the 1st of the month
        for (int i = 0; i < firstDayOfWeek; i++) {
            calendarPanel.add(new JLabel(""));
        }

        // Create a button for each day of the month
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDate = yearMonth.atDay(day);
            JButton dayButton = new JButton(String.valueOf(day));
            dayButton.setFont(new Font("Arial", Font.PLAIN, 12));

            // If assignments exist for this date, highlight the button
            if (assignments.containsKey(currentDate)) {
                dayButton.setOpaque(true);
                dayButton.setBackground(new Color(200, 0, 0));
                dayButton.setForeground(Color.WHITE);
            }

            // When a day is clicked, show the assignments for that date
            dayButton.addActionListener(e -> showAssignmentsForDate(currentDate));
            calendarPanel.add(dayButton);
        }

        // Update month-year label at top
        monthYearLabel.setText(yearMonth.getMonth() + " " + yearMonth.getYear());

        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    /**
     * showAssignmentsForDate(LocalDate date):
     * When a user clicks on a date in the calendar, this method:
     * 1. Clears the previous assignment list.
     * 2. Displays all assignments for that selected day.
     * 3. Shows a bar representing how many points earned vs total points for that day.
     * 4. Shows another bar for the entire month totals (aggregated from all assignments in the month).
     */
    private static void showAssignmentsForDate(LocalDate date) {
        assignmentsListPanel.removeAll();

        List<String> dayAssignments = assignments.getOrDefault(date, List.of("No assignments"));

        double totalPointsSum = 0;
        double earnedPointsSum = 0;

        // If no assignments for that day, show a "No assignments" message
        if (dayAssignments.size() == 1 && "No assignments".equals(dayAssignments.get(0))) {
            JLabel noAssignmentsLabel = new JLabel("No assignments for this date.");
            noAssignmentsLabel.setFont(new Font("Arial", Font.ITALIC, 14));
            noAssignmentsLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
            assignmentsListPanel.add(noAssignmentsLabel);
        } else {
            // We have assignments for this day, display each one as a "card"
            for (String record : dayAssignments) {
                String[] parts = record.split("\\|");
                String assignmentName = parts[0];
                String zonedDateStr = parts[1];
                String totalPoints = parts.length > 2 ? parts[2] : "N/A";
                String pointsEarned = parts.length > 3 ? parts[3] : "";

                // Parse the due date
                ZonedDateTime zdt = ZonedDateTime.parse(zonedDateStr);
                String dueDateFormatted = zdt.toLocalDateTime().toString().replace('T', ' ');

                // Wrap long assignment names in HTML for word-wrapping
                String wrappedAssignmentName = String.format("<html><div style='width:%dpx;'>%s</div></html>", 
                    MAX_NAME_WIDTH_PX, assignmentName);

                // Create a panel for this single assignment
                JPanel assignmentCard = new JPanel();
                assignmentCard.setLayout(new BoxLayout(assignmentCard, BoxLayout.Y_AXIS));
                assignmentCard.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                        new EmptyBorder(10, 10, 10, 10)));
                assignmentCard.setBackground(Color.WHITE);

                JLabel nameLabel = new JLabel(wrappedAssignmentName);
                nameLabel.setFont(new Font("Arial", Font.BOLD, 14));

                JLabel dueLabel = new JLabel("Due: " + dueDateFormatted);
                dueLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                dueLabel.setForeground(Color.DARK_GRAY);

                // Parse numeric values for total and earned points
                double tPoints = 0;
                double ePoints = 0;
                try {
                    if (!"N/A".equals(totalPoints)) {
                        tPoints = Double.parseDouble(totalPoints);
                    }
                    if (pointsEarned != null && !pointsEarned.isBlank() && !"Not Available".equals(pointsEarned)) {
                        ePoints = Double.parseDouble(pointsEarned);
                    }
                } catch (NumberFormatException ex) {
                    // If parsing fails, treat as zero
                }

                totalPointsSum += tPoints;
                earnedPointsSum += ePoints;

                // Label to show points info
                JLabel pointsLabel;
                if (pointsEarned != null && !pointsEarned.isBlank() && !"Not Available".equals(pointsEarned)) {
                    pointsLabel = new JLabel("Points Earned: " + pointsEarned + " / Total Points: " + totalPoints);
                } else {
                    pointsLabel = new JLabel("Total Points: " + totalPoints);
                }
                pointsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                pointsLabel.setForeground(new Color(0, 128, 0));

                // Add components to the assignment card
                assignmentCard.add(nameLabel);
                assignmentCard.add(dueLabel);
                assignmentCard.add(pointsLabel);

                // Add the assignment card to the assignments list panel
                assignmentsListPanel.add(assignmentCard);
                assignmentsListPanel.add(new JLabel(" "));
            }
        }

        // If we showed assignments and have total points, show the daily graph
        if (!(dayAssignments.size() == 1 && "No assignments".equals(dayAssignments.get(0))) && totalPointsSum > 0) {
            GraphPanel dailyGraphPanel = new GraphPanel(totalPointsSum, earnedPointsSum, "Day Total");
            dailyGraphPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
            assignmentsListPanel.add(dailyGraphPanel);
        }

        // Now compute and show a monthly graph for the entire month
        YearMonth currentMonth = YearMonth.from(date);
        double[] monthlyTotals = computeMonthlyTotals(currentMonth);
        double monthlyTotalPoints = monthlyTotals[0];
        double monthlyEarnedPoints = monthlyTotals[1];

        if (monthlyTotalPoints > 0) {
            GraphPanel monthlyGraphPanel = new GraphPanel(monthlyTotalPoints, monthlyEarnedPoints, "Month Total");
            monthlyGraphPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
            assignmentsListPanel.add(monthlyGraphPanel);
        }

        // Refresh UI
        assignmentsListPanel.revalidate();
        assignmentsListPanel.repaint();
    }

    /**
     * computeMonthlyTotals(YearMonth ym):
     * Aggregates all assignments in the given month (based on their LocalDate) and sums up
     * total points and earned points. Returns an array [totalPointsSum, earnedPointsSum].
     */
    private static double[] computeMonthlyTotals(YearMonth ym) {
        double totalPointsSum = 0;
        double earnedPointsSum = 0;

        // Iterate over all assignments in the map
        for (Map.Entry<LocalDate, List<String>> entry : assignments.entrySet()) {
            LocalDate date = entry.getKey();
            // Check if the assignment date falls within the given month and year
            if (date.getYear() == ym.getYear() && date.getMonth() == ym.getMonth()) {
                List<String> dayAssignments = entry.getValue();
                // Accumulate totals for this day's assignments
                for (String record : dayAssignments) {
                    String[] parts = record.split("\\|");
                    String totalPoints = parts.length > 2 ? parts[2] : "N/A";
                    String pointsEarned = parts.length > 3 ? parts[3] : "";

                    double tPoints = 0;
                    double ePoints = 0;
                    try {
                        if (!"N/A".equals(totalPoints)) {
                            tPoints = Double.parseDouble(totalPoints);
                        }
                        if (pointsEarned != null && !pointsEarned.isBlank() && !"Not Available".equals(pointsEarned)) {
                            ePoints = Double.parseDouble(pointsEarned);
                        }
                    } catch (NumberFormatException ex) {
                        // If parsing fails, ignore and treat as zero
                    }

                    totalPointsSum += tPoints;
                    earnedPointsSum += ePoints;
                }
            }
        }

        return new double[]{totalPointsSum, earnedPointsSum};
    }

    /**
     * getCurrentDisplayedYearMonth():
     * Parses the monthYearLabel text to determine which YearMonth is currently displayed on the calendar.
     */
    private static YearMonth getCurrentDisplayedYearMonth() {
        String text = monthYearLabel.getText().trim();
        String[] parts = text.split(" ");
        if (parts.length == 2) {
            String monthName = parts[0].toUpperCase();
            int year = Integer.parseInt(parts[1]);
            return YearMonth.of(year, java.time.Month.valueOf(monthName));
        }
        return YearMonth.now();
    }

    /**
     * GraphPanel:
     * A custom JPanel that draws a simple horizontal bar graph showing total points versus earned points.
     * The gray bar represents total points (the entire possible amount).
     * The green bar on top represents how many points have been earned (a fraction of the total).
     * A label is drawn above the bar to indicate the ratio and whether it's "Day Total" or "Month Total".
     */
    private static class GraphPanel extends JPanel {
        private final double totalPoints;
        private final double earnedPoints;
        private final String labelPrefix;

        public GraphPanel(double totalPoints, double earnedPoints, String labelPrefix) {
            this.totalPoints = totalPoints;
            this.earnedPoints = earnedPoints;
            this.labelPrefix = labelPrefix;
            setPreferredSize(new java.awt.Dimension(300, 50));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int width = getWidth();
            int height = getHeight();

            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
            // Enable anti-aliasing for smoother graphics
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw background bar (light gray) representing total points
            g2.setColor(Color.LIGHT_GRAY);
            int barHeight = height / 4;
            int barY = (height - barHeight) / 2;
            g2.fillRoundRect(10, barY, width - 20, barHeight, 10, 10);

            // If earned points are available, draw the green bar on top
            if (totalPoints > 0 && earnedPoints > 0) {
                double fraction = earnedPoints / totalPoints;
                int earnedWidth = (int) ((width - 20) * fraction);
                g2.setColor(new Color(144, 238, 144)); // Light green for earned portion
                g2.fillRoundRect(10, barY, earnedWidth, barHeight, 10, 10);
            }

            // Draw the text label showing earned vs total
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            String label = String.format("%s: Earned: %.1f / Total: %.1f", labelPrefix, earnedPoints, totalPoints);
            int textWidth = g2.getFontMetrics().stringWidth(label);
            // Position the label above the bar, centered horizontally
            g2.drawString(label, (width - textWidth) / 2, barY - 5);
        }
    }
}

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

@SpringBootApplication
public class CalanderCanvasApplication {

    private static JFrame frame;
    private static Map<LocalDate, List<String>> assignments = new HashMap<>();

    private static JPanel assignmentsListPanel;
    private static JLabel assignmentsLabel;
    private static JPanel calendarPanel;
    private static JLabel monthYearLabel;
    private static JButton prevButton;
    private static JButton nextButton;

    private static final int MAX_NAME_WIDTH_PX = 200;

    public static void main(String[] args) {
        System.out.println("Headless mode: " + GraphicsEnvironment.isHeadless());

        // Prompt once for API key
        String userApiKey = JOptionPane.showInputDialog(null, "Please enter your Canvas API key:", "API Key Required", JOptionPane.QUESTION_MESSAGE);
        if (userApiKey == null || userApiKey.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "No API key entered. Exiting application.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Set the API key system property so CanvasAPIService can pick it up
        System.setProperty("api.key", userApiKey.trim());

        // Run Spring
        SpringApplication.run(CalanderCanvasApplication.class, args);

        // Create UI after Spring is started
        SwingUtilities.invokeLater(() -> {
            createMainPanel();
        });
    }

    private static void createMainPanel() {
        frame = new JFrame("Canvas Calendar");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Header panel
        JPanel headerPanel = new JPanel();
        JLabel titleLabel = new JLabel("Canvas Calendar", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerPanel.add(titleLabel);
        frame.add(headerPanel, BorderLayout.NORTH);

        // Assignments panel (on the right)
        JPanel assignmentsPanel = new JPanel(new BorderLayout());
        assignmentsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        assignmentsLabel = new JLabel("Assignments", JLabel.CENTER);
        assignmentsLabel.setFont(new Font("Arial", Font.BOLD, 18));
        assignmentsPanel.add(assignmentsLabel, BorderLayout.NORTH);

        assignmentsListPanel = new JPanel();
        assignmentsListPanel.setLayout(new BoxLayout(assignmentsListPanel, BoxLayout.Y_AXIS));
        assignmentsListPanel.setBackground(Color.WHITE);
        assignmentsPanel.add(assignmentsListPanel, BorderLayout.CENTER);

        frame.add(assignmentsPanel, BorderLayout.EAST);

        // Calendar navigation panel (on the left)
        JPanel navigationPanel = new JPanel();
        prevButton = new JButton("<");
        nextButton = new JButton(">");
        monthYearLabel = new JLabel("", JLabel.CENTER);
        monthYearLabel.setFont(new Font("Arial", Font.BOLD, 16));

        navigationPanel.add(prevButton);
        navigationPanel.add(monthYearLabel);
        navigationPanel.add(nextButton);

        // Calendar panel (center-left)
        calendarPanel = new JPanel();
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(navigationPanel, BorderLayout.NORTH);
        leftPanel.add(calendarPanel, BorderLayout.CENTER);
        frame.add(leftPanel, BorderLayout.CENTER);

        // Control buttons panel (bottom)
        JPanel controlsPanel = new JPanel();
        JButton refreshButton = new JButton("Refresh Assignments");
        controlsPanel.add(refreshButton);
        frame.add(controlsPanel, BorderLayout.SOUTH);

        // Actions
        refreshButton.addActionListener(e -> handleRefresh());

        // Initialize the calendar
        YearMonth currentYearMonth = YearMonth.now();
        updateCalendar(currentYearMonth);

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

        frame.setSize(1000, 600);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void handleRefresh() {
        try {
            List<String> assignmentsData = fetchAssignmentsFromBackend();

            assignments.clear();

            for (String entry : assignmentsData) {
                String assignmentName = null;
                String dueDateStr = null;
                String totalPoints = null;
                String pointsEarned = null;

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

                if (assignmentName != null && dueDateStr != null && !dueDateStr.equals("No Due Date")) {
                    try {
                        ZonedDateTime zdt = ZonedDateTime.parse(dueDateStr)
                                .withZoneSameInstant(java.time.ZoneId.systemDefault());
                        LocalDate date = zdt.toLocalDate();

                        String record = assignmentName + "|" + zdt.toString() + "|" +
                                        (totalPoints != null ? totalPoints : "N/A") + "|" +
                                        (pointsEarned != null ? pointsEarned : "");
                        assignments.computeIfAbsent(date, k -> new ArrayList<>()).add(record);
                    } catch (DateTimeParseException ex) {
                        System.err.println("Failed to parse date: " + dueDateStr);
                    }
                }
            }

            JOptionPane.showMessageDialog(frame, "Assignments refreshed successfully.", "Refresh", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error refreshing assignments: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static List<String> fetchAssignmentsFromBackend() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

        URI uri = URI.create("http://localhost:8080/course-assignments");
        List<String> response = restTemplate.getForObject(uri, List.class);
        return response != null ? response : Collections.emptyList();
    }

    private static void updateCalendar(YearMonth yearMonth) {
        calendarPanel.removeAll();
        calendarPanel.setLayout(new GridLayout(0, 7));

        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String day : days) {
            JLabel dayLabel = new JLabel(day, JLabel.CENTER);
            dayLabel.setFont(new Font("Arial", Font.BOLD, 14));
            calendarPanel.add(dayLabel);
        }

        LocalDate firstDayOfMonth = yearMonth.atDay(1);
        int daysInMonth = yearMonth.lengthOfMonth();
        int firstDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() % 7;

        for (int i = 0; i < firstDayOfWeek; i++) {
            calendarPanel.add(new JLabel(""));
        }

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDate = yearMonth.atDay(day);
            JButton dayButton = new JButton(String.valueOf(day));
            dayButton.setFont(new Font("Arial", Font.PLAIN, 12));

            if (assignments.containsKey(currentDate)) {
                dayButton.setOpaque(true);
                dayButton.setBackground(new Color(200, 0, 0));
                dayButton.setForeground(Color.WHITE);
            }

            dayButton.addActionListener(e -> showAssignmentsForDate(currentDate));
            calendarPanel.add(dayButton);
        }

        monthYearLabel.setText(yearMonth.getMonth() + " " + yearMonth.getYear());

        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    /**
     * Show assignments for the selected date and also show a monthly total bar.
     */
    private static void showAssignmentsForDate(LocalDate date) {
        assignmentsListPanel.removeAll();

        List<String> dayAssignments = assignments.getOrDefault(date, List.of("No assignments"));

        double totalPointsSum = 0;
        double earnedPointsSum = 0;

        if (dayAssignments.size() == 1 && "No assignments".equals(dayAssignments.get(0))) {
            JLabel noAssignmentsLabel = new JLabel("No assignments for this date.");
            noAssignmentsLabel.setFont(new Font("Arial", Font.ITALIC, 14));
            noAssignmentsLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
            assignmentsListPanel.add(noAssignmentsLabel);
        } else {
            for (String record : dayAssignments) {
                String[] parts = record.split("\\|");
                String assignmentName = parts[0];
                String zonedDateStr = parts[1];
                String totalPoints = parts.length > 2 ? parts[2] : "N/A";
                String pointsEarned = parts.length > 3 ? parts[3] : "";

                ZonedDateTime zdt = ZonedDateTime.parse(zonedDateStr);
                String dueDateFormatted = zdt.toLocalDateTime().toString().replace('T', ' ');

                String wrappedAssignmentName = String.format("<html><div style='width:%dpx;'>%s</div></html>", MAX_NAME_WIDTH_PX, assignmentName);

                JPanel assignmentCard = new JPanel();
                assignmentCard.setLayout(new BoxLayout(assignmentCard, BoxLayout.Y_AXIS));
                assignmentCard.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                        new EmptyBorder(10, 10, 10, 10)
                ));
                assignmentCard.setBackground(Color.WHITE);

                JLabel nameLabel = new JLabel(wrappedAssignmentName);
                nameLabel.setFont(new Font("Arial", Font.BOLD, 14));

                JLabel dueLabel = new JLabel("Due: " + dueDateFormatted);
                dueLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                dueLabel.setForeground(Color.DARK_GRAY);

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
                    // If parsing fails, treat as 0
                }

                totalPointsSum += tPoints;
                earnedPointsSum += ePoints;

                JLabel pointsLabel;
                if (pointsEarned != null && !pointsEarned.isBlank() && !"Not Available".equals(pointsEarned)) {
                    pointsLabel = new JLabel("Points Earned: " + pointsEarned + " / Total Points: " + totalPoints);
                } else {
                    pointsLabel = new JLabel("Total Points: " + totalPoints);
                }
                pointsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                pointsLabel.setForeground(new Color(0, 128, 0));

                assignmentCard.add(nameLabel);
                assignmentCard.add(dueLabel);
                assignmentCard.add(pointsLabel);

                assignmentsListPanel.add(assignmentCard);
                assignmentsListPanel.add(new JLabel(" "));
            }
        }

        // Add the daily graph if we have any assignments
        if (!(dayAssignments.size() == 1 && "No assignments".equals(dayAssignments.get(0))) && totalPointsSum > 0) {
            GraphPanel dailyGraphPanel = new GraphPanel(totalPointsSum, earnedPointsSum, "Day Total");
            dailyGraphPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
            assignmentsListPanel.add(dailyGraphPanel);
        }

        // Compute monthly totals and show a monthly bar
        YearMonth currentMonth = YearMonth.from(date);
        double[] monthlyTotals = computeMonthlyTotals(currentMonth);
        double monthlyTotalPoints = monthlyTotals[0];
        double monthlyEarnedPoints = monthlyTotals[1];

        if (monthlyTotalPoints > 0) {
            GraphPanel monthlyGraphPanel = new GraphPanel(monthlyTotalPoints, monthlyEarnedPoints, "Month Total");
            monthlyGraphPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
            assignmentsListPanel.add(monthlyGraphPanel);
        }

        assignmentsListPanel.revalidate();
        assignmentsListPanel.repaint();
    }

    /**
     * Compute the total and earned points for all assignments in the specified month.
     */
    private static double[] computeMonthlyTotals(YearMonth ym) {
        double totalPointsSum = 0;
        double earnedPointsSum = 0;

        for (Map.Entry<LocalDate, List<String>> entry : assignments.entrySet()) {
            LocalDate date = entry.getKey();
            if (date.getYear() == ym.getYear() && date.getMonth() == ym.getMonth()) {
                List<String> dayAssignments = entry.getValue();
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
                        // ignore parsing errors
                    }

                    totalPointsSum += tPoints;
                    earnedPointsSum += ePoints;
                }
            }
        }

        return new double[]{totalPointsSum, earnedPointsSum};
    }

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
     * A custom panel to draw a bar chart representing total points and earned points.
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

            // Anti-aliasing for smoother graphics
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw the background bar (total points)
            g2.setColor(Color.LIGHT_GRAY);
            int barHeight = height / 4;
            int barY = (height - barHeight) / 2;
            g2.fillRoundRect(10, barY, width - 20, barHeight, 10, 10);

            // Draw the earned portion
            if (totalPoints > 0 && earnedPoints > 0) {
                double fraction = earnedPoints / totalPoints;
                int earnedWidth = (int) ((width - 20) * fraction);
                g2.setColor(new Color(144, 238, 144)); // light green
                g2.fillRoundRect(10, barY, earnedWidth, barHeight, 10, 10);
            }

            // Draw text labels
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            String label = String.format("%s: Earned: %.1f / Total: %.1f", labelPrefix, earnedPoints, totalPoints);
            int textWidth = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, (width - textWidth) / 2, barY - 5);
        }
    }
}

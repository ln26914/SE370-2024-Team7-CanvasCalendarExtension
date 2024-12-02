public class GradeCalculator {
    public double calculateGrade(int assignmentId, int earnedPoints) {
        // Retrieve total points from DAO
        int totalPoints = OrderAssignmentDAO.getInstance().getAssignment(assignmentId).getTotalPoints();

        if (totalPoints <= 0) {
            throw new IllegalArgumentException("Invalid total points for assignment ID: " + assignmentId);
        }

        return (double) earnedPoints / totalPoints * 100;
    }
}

public class AssignmentFacade {
    private final OrderAssignmentDAO assignmentDAO = OrderAssignmentDAO.getInstance();
    private final OrderCourseAttributesDAO courseDAO = OrderCourseAttributesDAO.getInstance();
    private final GradeCalculator gradeCalculator = new GradeCalculator();

    public void createAssignment(int assignmentId, String courseName, String assignmentName, int totalPoints) {
        assignmentDAO.createAssignment(new Assignment(assignmentId, courseName, assignmentName, totalPoints));
    }

    public double calculateGrade(int assignmentId, int earnedPoints) {
        return gradeCalculator.calculateGrade(assignmentId, earnedPoints);
    }

    public String getCourseName(int assignmentId){
        Assignment assignment = assignmentDAO.getAssignment(assignmentId);
        if (assignment == null){
            return null;
        }
        return assignment.getCourseName();
    }

    // ... other methods for getting assignment details, updating, deleting ...
}

public class OrderCourseAttributesDAO {
    private static OrderCourseAttributesDAO instance; // Singleton pattern
    // ... database connection details ...

    private OrderCourseAttributesDAO() {}

    public static OrderCourseAttributesDAO getInstance() {
        if (instance == null) {
            instance = new OrderCourseAttributesDAO();
        }
        return instance;
    }

    public String getCourseName(int courseId) {
        // ...database query to get course name...
        // Replace with your actual database code (e.g., using JDBC)
        return "Intro to Java";  // Placeholder
    }
    // ...other methods for managing course attributes...
}

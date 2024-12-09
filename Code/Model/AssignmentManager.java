import java.util.*;
import java.io.*;

// A 'middle man' class for presnting assignment data to the front end.

// If the Assignment objects are like dishes, this object is the waiter that brings 
// them to and from the kitchen.

public class AssignmentManager {
    // Look inside
    // It's a bunch of assignments in a trench coat
    
    ArrayList<Assignment> court = new ArrayList<Assignment>;

    // I used an ArrayList to store the assignments since I can resize it without 
    // having to create a new one.
    
    /*  ArrayLists have the following methods:
     *  add(var item): Appends the item after the last element
     *  remove(int index): removes the item at that index. 
     *    Items after the removed item have their index reduced by 1.
     *  set(int index, var newItemValue) changes the item at the given index.
     *  get(int index) returns a reference to the item at the given index.
     */
    
    // Constructors
    // Defalut Constructor--Use when you don't have a file path
    public AssignmentManager() {
        // Do nothing.
    }

    // Load Constructor -- Populate the court with the assignments contained in the cache.
    public AssignmentManager(String filename) {
        // Use a load function to populate the court from the file
        this.load(filename);
    }

    /**
     * get(int index)
     * Returns the assignment at the specified index of the 'court'
     * It uses the same name as the ArrayList method, because this class is just an ArrayList
     * with a bit of extra icing on top.
     */
    public Assignment get(int index) {
        
        // Use the ArrayList's get() method.
        return court.get(index);
    }

    /**
     * remove(int index)
     * Removes the assignment at the specified index of the 'court'
     * Returns a reference to the old element in case it needs some last-minute processing.
     * Uses the same name as the ArrayList method, because this class is just an ArrayList
     * with a bit of extra icing on top.
     */
    public Assignment remove(int index) {
        // Store the item to be removed
        Assignment oldNum = court.get(index);
        
        // Remove the item from the court
        this.court.remove(index);

        // Return the old item
        return oldNum;
    }

    /**
     * add(Assignment newItem)
     * Adds the new assignment to the end of the court.
     * If necessary, I can rewrite this function to use a 'target' parameter that adds the new
     * item after the item after the target index, leaving the remaining items in order.
     * This would be good for preserving organized assignment data.
     * Uses the same name as the ArrayList method, because this class is just an ArrayList
     * with a bit of extra icing on top.
     */
    public void add(Assignment newItem) {
        this.court.add(newItem);
    }

    /**
     * save(String filepath)
     * Serializes the assignments in the ArrayList and writes them to the file one-by-one.
     * The filepath has to be specified as a parameter in case we want multiple cache files 
     * for different users or something.
     * 
     * This save function DOES NOT clear the assignment manager's 'court' parameter and ONLY
     * saves the 'court' data to the file. Please let me know if this causes any problems.
     */
    public void save(String filepath) throws IOException {
        
        // Establish File Streams
        FileOutputStream fstream = new FileOutputStream(filepath);
        ObjectOutputStream outputFile = new ObjectOutputStream(fstream);

        // Try to Serialize this object's data
        for(int i = 0; i < court.size(); i++) {
            try {
                // Wtite only one Assignment this iteration
                outputFile.writeObject(this.get(i));
                outputFile.flush();

            } catch (IOException e) {
                System.out.println("Unable to write to file.")
            } finally {
                outputFile.close();
                break; // Leave the loop if something breaks.
            }
        }

        // We're done writing to the file.
        outputFile.close();

        // Break glass (un-comment) in case of an emergency.
        // this.court.clear() // Clears the ArrayList object that contains the stored assignments
    }

    /**
     * load(String filepath)
     * Loads and deserializes the assignments into the 'court' where the code can access them.
     * Returns -1 if the end of file has been reached.
     * 
     * When populating the assignment data from a file, the new assignments are added to the end 
     * of the court. We have to remove duplicates to ensure we don't have duplicate calendar
     * events.
     * 
     * If necessary, the 'court' for this object can be cleared before we add data, to ensure that
     * only the file data is stored.
     */
    public int load(String filepath) throws IOException {

        // Break glass (un-comment) in case of an emergency
        // this.court.clear() // Clear all the assignment data from the court before we load new data.

        int flag = 0; // If this value is -1 we know that the EOF is reached.

        // Establish File Streams
        try {
            FileInputStream fstream = new FileInputStream(filename);
            ObjectInputStream inputFile = new ObjectInputStream(fstream);
            
            System.out.println("File Found: " + filename);
            
            while(flag = fstream.read() != -1) {
                // Deserialize Object
                Assignment newAssignment = (Assignment) inputFile.readObject();
                
                // Add the object to the end of the court.
                this.court.add(newAssignment);
            }
        
        } catch (IOException e) {
            // If unable to read from the file, use the default constructor.
            System.out.println("Error when reading file " + filepath);
            return false;
        }


        // Return the flag. If nothing broke, it should return the integer -1
        // fstream.read() returns -1 if the End Of File is reached.
        return flag;
    }
}
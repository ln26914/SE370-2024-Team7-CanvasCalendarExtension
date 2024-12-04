/**
 * For the love of all that is good, make sure that this file, Assignment.java, and cache.dat are in the same folder.
 * 
 * The Assignment class has a member of type Date. To make things easier, keep this file and Assignment.java together to minimize file path shenanigans.
 * 
 */

import java.util.Scanner;
import java.io.*;

public class Date implements Serializable {

    // Data Members
    private int day;
    private int month;
    private int year;

    // Serialize Utilities
    private static final long serialVersionUID = 1L;

    //Constructors
    //Main Constructor
    public Date(int day, int month, int year) {
        this.day = day;
        this.month = month;
        this.year = year;
    }
    //Default Constructor
    public Date() {
        this.day = 1;
        this.month = 1;
        this.year = 2000;
    }
    // Copy Constructor
    public Date(Date cpy) {
        this.day = cpy.getDay();
        this.month = cpy.getMonth();
        this.year = cpy.getYear();
    }
    
    //toString()
    public String toString() {
        String outputString = String.format("%2d/%2d/%4d",this.month,this.day,this.year);
    }

    //Getters
    public int getDay() {
        return this.day;
    }
    public int getMonth() {
        return this.month;
    }
    public int getYear() {
        return this.year;
    }

    //Setters
    public void setDay(newDay) {
        this.day = newDay;
    }
    public void setMonth(newMonth) {
        this.month = newMonth;
    }
    public void setYear(newYear) {
        this.year = newYear;
    }
}

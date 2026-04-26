package com.acadcore;

import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

//  EXCEPTIONS
//added invalid password exception
class InvalidPasswordException extends Exception { //inherits in built exception class

    public InvalidPasswordException(String message) {
        super(message);
    }
}
//added Course Capacity Exception

class CourseCapacityException extends Exception {

    public CourseCapacityException(String message) {
        super(message);
    }
}
//added Schedule Conflict Exception

class ScheduleConflictException extends Exception {

    public ScheduleConflictException(String msg) {
        super(msg);
    }
}

class RoomCapacityException extends Exception {

    public RoomCapacityException(String msg) {
        super(msg);
    }
}

//added interface 
interface Displayable {

    void displayInfo();
}

abstract class User implements Displayable { //made user class abstract as it should not be instantiated directly
    //attributes

    private int id;
    private String name;
    private String email;
    private String password;

    private static int totalUsers = 0;   // counter to track total users created

    //constructor
    public User(int id, String name, String email, String password)
            throws InvalidPasswordException { //added exception to constructor

        this.id = id;
        this.name = name;
        this.email = email;
        setPassword(password);
        totalUsers++;
    }

    //methods
    //getters
    int getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getEmail() {
        return email;
    }
    //added static method to get total users

    public static int getTotalUsers() {
        return totalUsers;
    }

    //setters
    void setName(String name) {
        this.name = name;
    }

    void setEmail(String email) {
        this.email = email;
    }

    void setPassword(String password) throws InvalidPasswordException {
        if (password != null && password.length() >= 6) {
            this.password = password;
        } else {
            throw new InvalidPasswordException("Password must be at least 6 characters.");
        }
    }
    //login method

    boolean login(String inputEmail, String inputPassword) {
        return (email.equals(inputEmail) && password.equals(inputPassword));
    }

    @Override
    public void displayInfo() {
        System.out.println("ID: " + id + "\nNAME: " + name + "\nEmail: " + email);
    }
}

class Course implements Displayable {

    private int courseCode;
    private String courseName;
    private Faculty facultyAssigned; //changed to Faculty reference for better integration with faculty class
    private int creditHours;//added credithours for workload calculation

    //constructor  without faculty assigned
    public Course(int courseCode, String courseName, int creditHours) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.facultyAssigned = null; // initially no faculty assigned
        this.creditHours = creditHours;
    }

    //constructor with default credit hours
    public Course(int courseCode, String courseName) {
        //using "this" as the first line of the constructor
        this(courseCode, courseName, 3); // default 3 credit hours      
    }

    //getters
    public int getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public Faculty getFacultyAssigned() {  //changed return type to Faculty
        return facultyAssigned;
    }

    public int getCreditHours() {
        return creditHours;
    }

    //added method which is called when faculty gets assigned to this course(linked both)
    public void setAssignedFaculty(Faculty f) {
        this.facultyAssigned = f;
        System.out.println("Faculty " + f.getName() + " linked to course: " + courseName);
    }

    public boolean hasFaculty() {
        return facultyAssigned != null;
    }

    @Override
    //added more details to displayinfo method
    public void displayInfo() {
        System.out.println("Course [" + courseCode + "] " + courseName + "\nCredits: " + creditHours + "\nFaculty: " + (hasFaculty() ? facultyAssigned.getName() : "Not assigned"));
    }
}//end of class Course

class Attendance {
    // attributes

    private Course course;
    private int totalClasses;
    private int attendedClasses;
    private static final double riskThreshold = 75.0;//attendance threshold per class (static final)

    //constructor
    public Attendance(Course c) {
        course = c;
        totalClasses = 0;
        attendedClasses = 0;
    }

    //methods
    //getter
    public Course getCourse() {
        return course;
    }

    //marking a class present
    public void markAttendance(Boolean present) {
        totalClasses++;
        if (present) {
            attendedClasses++;
        }
    }

    //attendance percentage
    double getAttendancePercentage() {
        if (totalClasses == 0) {
            return 100;
        } else {
            return ((attendedClasses * 100.0) / totalClasses);
        }
    }

    //attendance rist alert 
    public boolean isAtRisk() {
        return getAttendancePercentage() < riskThreshold;
    }

    //displaying attendance
    public void displayAttendance() {
        System.out.printf("%-30s%d/%d(%.1f%%)%s%n",
                course.getCourseName(),//in 30 spaces left
                attendedClasses, totalClasses,
                getAttendancePercentage(),
                //if at risk it prints otherwise empty quotes
                isAtRisk() ? "  ⚠ AT RISK" : "");
    }
}//end of class attendance
//added Assignment class for assignment prioritizer                                                                    //ADDED

class Assignment {

    private String courseName;
    private String title;
    private int daysUntilDue;
    private int weightPercent;

    //constructor
    public Assignment(String courseName, String title, int daysUntilDue, int weightPercent) {
        this.courseName = courseName;
        this.title = title;
        this.daysUntilDue = daysUntilDue;
        this.weightPercent = weightPercent;
    }

    //getters
    String getCourseName() {
        return courseName;
    }

    String getTitle() {
        return title;
    }

    int getDaysUntilDue() {
        return daysUntilDue;
    }

    int getWeightPercent() {
        return weightPercent;
    }

    //setters
    void setDaysUntilDue(int daysUntilDue) {    //if deadline gets extended 
        this.daysUntilDue = daysUntilDue;
    }

    void setWeightPercent(int weightPercent) {
        this.weightPercent = weightPercent;
    }

    //priority score calculation based on urgency and weightage
    public double getPriorityScore() {
        double urgency = 1.0 / Math.max(daysUntilDue, 1);   // ranges from 0 to 1,used max function to avoid division by zero.
        double weightScore = weightPercent / 100.0;         // ranges from 0 to 1
        return (weightScore * 0.6 + urgency * 0.4) * 100; // 60% to weightage and 40% to urgency
    }

    public void display() {
        System.out.printf("  [%s] %-25s due in %2d days  weight: %2d%%  priority: %.2f%n", courseName, title, daysUntilDue, weightPercent, getPriorityScore());
    }
}//end of class assignment

//study group finder class                                                                                                                   
class StudyGroup {                                                                                                              //ADDED

    private Course course;
    private Student[] members;   //array to store members
    private Student leader;    //group leader(creator of the group)
    private int memberCount;
    private int maxSize;    //leader decides max size of the group
    //constructor

    public StudyGroup(Course course, int maxSize, Student leader) {
        this.course = course;
        this.maxSize = maxSize;
        this.leader = leader;
        this.members = new Student[maxSize];
        this.members[0] = leader; //leader is the first member of the group
        this.memberCount = 1;   //initially only the leader is in the group
    }

    // add a student to the group
    //HELPER METHODS FOR VALIDATIONS
    public boolean isvalidMember(Student student) {
        //to check if the student is of the same course before adding to the group
        for (Course c : student.getEnrolledCourses()) {
            if (c != null && c.getCourseCode() == course.getCourseCode()) {
                return true;
            }
        }
        return false;
    }

    //to check if the student is already in the group
    public boolean isMember(Student student) {
        for (int i = 0; i < memberCount; i++) {
            if (members[i].getId() == student.getId()) {
                return true;
            }
        }
        return false;
    }

    //main method to add member with all validations
    public boolean addMember(Student student) {

        //check if the student is enrolled in the course before adding to the group
        if (!isvalidMember(student)) {
            System.out.println("Student " + student.getName() + " is not enrolled in " + course.getCourseName() + ".");
            return false;
        } //check if the group is already full
        else if (memberCount == maxSize) {
            System.out.println("Study group for " + course.getCourseName() + " is full.");
            return false;
        } //check if the student is already in the group
        else if (isMember(student)) {
            System.out.println("Student " + student.getName() + " is already in the study group for " + course.getCourseName() + ".");
            return false;
        } //if all validations are passed add the student to the group
        else {
            members[memberCount++] = student;   //stored and incremented the member count(post increment)
            System.out.println("Student " + student.getName() + " added to the study group for " + course.getCourseName() + ".");
            return true;
        }

    }

    //getters
    public Course getCourse() {
        return course;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public Student getLeader() {
        return leader;
    }

    public int getMaxSize() {
        return maxSize;
    }

    //display the group details
    public void display() {
        System.out.println("Study Group – " + course.getCourseName() + " (" + memberCount + "/" + maxSize + " members)");
        for (int i = 0; i < memberCount; i++) {
            System.out.println("    " + (i + 1) + ". " + members[i].getName());
        }
    }
}   //end of class study group (without gpa comparison logic)                                                                                                                      
//ADDED SOME ATTRIBUTES

class Student extends User {

    private static final int MAX_COURSES = 10;
    //attributes
    private AcadClass classOfStudent;//added class and removed string major(it can be retreived from class)
    private Course[] enrolledCourses = new Course[MAX_COURSES];   //max course limit is 10
    private Attendance[] attendanceRecords = new Attendance[MAX_COURSES];
    private Assignment[] assignments = new Assignment[50];//maximum limit of assignments 50
    private int courseCount = 0;
    private int assignmentCount = 0;
    private double gpa; //added gpa attribute for study group finder
    private double cgpa; //added cgpa attribute for study group finder
    private ArrayList<SeatingAssignment> myFullExamSchedule = new ArrayList<>();

    //constructor might throw invalid password exception
    public Student(int id, String name, String email, String password, AcadClass classOfStudent) throws InvalidPasswordException {
        super(id, name, email, password);
        this.classOfStudent = classOfStudent;
        classOfStudent.addStudent(this); //automatically adds the student to the class when a student object is created with a class reference
    }

    //getter for enrolled courses  
    public Course[] getEnrolledCourses() {
        //added getter to use in study group finder
        return enrolledCourses;
    }

    //added getter for major
    public String getMajor() {
        //added getter to use in study group finder
        return classOfStudent.getMajor();
    }
    //added getter for batch number

    public int getBatchNo() {
        return classOfStudent.getBatchNo();
    }

    //added getter for section
    public char getSection() {
        return classOfStudent.getSection();
    }

    //added getter for semester
    public int getSemester() {
        return classOfStudent.getSemester();
    }

    //added getter for class
    public AcadClass getClassOfStudent() {
        return classOfStudent;
    }

    public ArrayList<SeatingAssignment> getMyFullExamSchedule() {
        return myFullExamSchedule;
    }

    //setter for AcadClass used in remove student from class method
    public void setClass(AcadClass classOfStudent) {
        this.classOfStudent = classOfStudent;
    }

    //methods
    //enroll course
    public void enrollCourse(Course c) throws CourseCapacityException {
        if (courseCount >= MAX_COURSES) {
            throw new CourseCapacityException(
                    getName() + " cannot enroll in more than " + MAX_COURSES + " courses.");
        }//throws exception is courses exceed the limit
        //check if the course is already enrolled
        for (int i = 0; i < courseCount; i++) {
            if (enrolledCourses[i] != null && enrolledCourses[i].getCourseCode() == c.getCourseCode()) {
                System.out.println(getName() + " is already enrolled in: " + c.getCourseName());
                return;
            }
        }
        enrolledCourses[courseCount] = c;
        attendanceRecords[courseCount] = new Attendance(c);
        courseCount++;
        System.out.println(getName() + " enrolled in: " + c.getCourseName());
    }

    // marking attendance
    public void markAttendance(String name, boolean present) {
        //method 1
        for (int i = 0; i < courseCount; i++) {
            if (name.equalsIgnoreCase(enrolledCourses[i].getCourseName())) {
                attendanceRecords[i].markAttendance(present);
                return;
            }
        }
        System.out.println("Invalid course index.");
    }

    //add an assignment
    public void addAssignment(Assignment a) {
        if (assignmentCount < assignments.length) {
            assignments[assignmentCount++] = a;//stores assignment on the first available index and then increments the count for next assignment
        }
    }
    // Added after the existing addAssignment() method (around line 428)

    public boolean removeAssignment(String courseName, String title) {
        for (int i = 0; i < assignmentCount; i++) {
            if (assignments[i] != null
                    && assignments[i].getCourseName().equalsIgnoreCase(courseName)
                    && assignments[i].getTitle().equalsIgnoreCase(title)) {
                for (int j = i; j < assignmentCount - 1; j++) {
                    assignments[j] = assignments[j + 1];
                }
                assignments[--assignmentCount] = null;
                return true;
            }
        }
        return false;
    }

    public Assignment[] getAssignmentsCopy() {
        Assignment[] copy = new Assignment[assignmentCount];
        System.arraycopy(assignments, 0, copy, 0, assignmentCount);
        return copy;
    }

    public int getAssignmentCount() {
        return assignmentCount;
    }

    //assignment prioritizer
    public void showPrioritizedAssignments() {
        // copy valid entries into new array
        Assignment[] sorted = new Assignment[assignmentCount];
        for (int i = 0; i < assignmentCount; i++) {
            sorted[i] = assignments[i];
        }

        // insertion sort descending by priority score
        for (int i = 1; i < assignmentCount; i++) {
            Assignment key = sorted[i];//storing 2nd element
            int j = i - 1;//storing 1st element
            while (j >= 0 && sorted[j].getPriorityScore() < key.getPriorityScore()) //if priority of 1st is less than 2nd
            {
                sorted[j + 1] = sorted[j];//storing 1st in place of 2nd
                //now 2 places are the same and higher priority one is stored in key but no where in array
                j--;//decremented
            }
            sorted[j + 1] = key;//storing higher priority in 1st place
        }

        //printing prioritized assignments if any
        System.out.println("\n── Assignment Prioritizer for " + getName() + " --");
        if (assignmentCount == 0) {
            System.out.println("  No assignments.");
            return;
        }
        for (int i = 0; i < assignmentCount; i++) {
            sorted[i].display();
        }
    }

    //get attendance report and risk alert for a course
    public boolean checkAttendanceRisks(String courseName) {
        for (int i = 0; i < courseCount; i++) {
            if (enrolledCourses[i] != null && enrolledCourses[i].getCourseName().equalsIgnoreCase(courseName)) {
                return attendanceRecords[i].isAtRisk(); // boolean flag indicating if the student attendance is below the threshold for this course
            }
        }
        return false;

    }

    //detect overlap in timeslots
    public void detectClash(TimeSlot[] mySlots) {
        System.out.println("\n── Clash Detection for " + getName() + " ──");
        boolean found = false;
        for (int i = 0; i < mySlots.length; i++) {
            for (int j = i + 1; j < mySlots.length; j++) {
                if (mySlots[i] != null && mySlots[j] != null
                        && mySlots[i].overlaps(mySlots[j])) {
                    //checks each slot with everyother slot of the time slot array after itself
                    //excludes checking itself and slots before it are already checked
                    //avoids unnecessary comparisons
                    System.out.println("  CLASH: Slot " + (i + 1)
                            + " overlaps with Slot " + (j + 1));
                    found = true;
                }
            }
        }
        if (!found) {
            System.out.println("  No clashes detected.");
        }
    }

    //view personal time table from class schedule
    public void viewMyClassSchedule() {
        if (classOfStudent == null) {
            System.out.println("No class assigned to " + getName());
            return;
        }
        classOfStudent.displayClassSchedule();
    }

    //method for admin to add exam seating assignment of the student
    public void addExamAssignment(SeatingAssignment assignment) {
        if (assignment == null) {
            return;
        }
        //validate that the assignment has a course and date before adding to the schedule
        if (assignment.getCourse() == null || assignment.getDate() == null) {
            return;
        }
        for (SeatingAssignment existing : myFullExamSchedule) {
            if (existing == null) {
                continue;
            }
            if (existing.getCourse() == null || assignment.getCourse() == null) {
                continue;
            }
            // if already have an assignment for same course on same date
            if (existing.getCourse().getCourseCode() == assignment.getCourse().getCourseCode() && existing.getDate().equals(assignment.getDate())) {
                return;
            }
        }
        //if all validations are passed add the assignment to the student's full exam schedule
        this.myFullExamSchedule.add(assignment);
    }

    public void viewExamSchedule() {
        System.out.println("\n── Exam Seating Assignments for " + getName() + " ──");
        if (myFullExamSchedule.isEmpty()) {
            System.out.println("  No exam assignments.");
            return;
        }
        for (SeatingAssignment sa : myFullExamSchedule) {
            System.out.println("  " + sa.getCourse().getCourseName() + " on " + sa.getDate() + " in Room " + sa.getRoom().getRoomNumber() + " Seat " + sa.getSeatNumber());
        }
    }

    //display info
    @Override
    public void displayInfo() {
        super.displayInfo();
        //modified display to show batch, major, and section from the class
        if (classOfStudent != null) {
            System.out.println("Batch: " + classOfStudent.getBatchNo() + " | Major: " + classOfStudent.getMajor() + " | Section: " + classOfStudent.getSection());
        }
        System.out.println("Enrolled Courses: " + courseCount);
    }

    //getters 
    public int getCourseCount() {
        return courseCount;
    }

    public Course getEnrolledCourse(int i) {
        return enrolledCourses[i];
    }

    public Attendance getAttendanceRecord(int i) {
        return attendanceRecords[i];
    }//returns particular attendance

}//end of class student

class Room implements Displayable {

    private int roomNumber;
    private int capacity;
    private String maintenanceNote; // added to report maintenance issues
    private TimeSlot[] bookedSlots = new TimeSlot[50]; // to keep track of booked slots for clash detection
    private int bookedCount = 0;

    //constructor
    public Room(int roomNumber, int capacity) {
        this.roomNumber = roomNumber;
        this.capacity = capacity;
        this.maintenanceNote = ""; // default no maintenance issues
    }

    //added getters for Admin room-utilization reporting
    public int getBookedCount() {
        return bookedCount;
    }

    public int getBookingCapacity() {
        return bookedSlots.length;
    }

    //getters
    int getRoomNumber() {
        return roomNumber;
    }

    int getCapacity() {
        return capacity;
    }

    String getMaintenanceNote() {
        return maintenanceNote;
    }

    //methods
    void reportMaintenanceIssue(String note) {
        this.maintenanceNote = note;
        System.out.println("Maintenance issue reported for Room " + roomNumber + ": " + note);
    }

    public boolean hasMaintenanceIssue() {   //no issue if the note is empty
        return !maintenanceNote.isEmpty();
    }

    //check room availability
    public boolean isAvailable(TimeSlot slot) {

        // 1. Capacity check
        if (bookedCount >= bookedSlots.length) {
            System.out.println("Room " + roomNumber + " has no remaining booking capacity.");
            return false;
        }

        // 2. Maintenance check
        if (hasMaintenanceIssue()) {
            System.out.println("Room " + roomNumber + " is under maintenance: " + maintenanceNote);
            return false;
        }

        // 3. Already booked at this time
        for (int i = 0; i < bookedCount; i++) {
            if (bookedSlots[i] != null && bookedSlots[i].overlaps(slot)) {
                return false;
            }
        }
        return true;
    }

    //book the room for a given timeslot if available
    public boolean bookRoom(TimeSlot slot) {
        if (!isAvailable(slot)) {
            return false;
        }
        bookedSlots[bookedCount++] = slot;
        System.out.println("Room " + roomNumber + " booked for " + slot.getDay() + " " + slot.getStartString() + "-" + slot.getEndString());
        return true;
    }

    @Override
    public void displayInfo() {
        System.out.println("Room " + roomNumber + " (Capacity: " + capacity + ")");
        if (hasMaintenanceIssue()) {
            System.out.println("  Maintenance Issue: " + getMaintenanceNote());
        }
    }
}//end of class room

//class for time
//removed time class
class TimeSlot {

    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm"); //formatter for displaying time in 24-hour format
    private String day;
    private LocalTime startTime;   //using LocalTime for better time handling and validation
    private LocalTime endTime;

    // constructor 
    public TimeSlot(int dayChoice, int sh, int sm, int eh, int em) {
        setDay(dayChoice);
        setStartTime(sh, sm);
        setEndTime(eh, em);
    }

    //added getter setters and validations
    void setDay(int choice) {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};   //using array to store days
        if (choice >= 1 && choice <= 5) {
            day = days[choice - 1];
        } else {
            System.out.println("Invalid number entered! day set to Monday.");
            day = "Monday";
        }
    }

    String getDay() {
        return day;
    }

    void setStartTime(int hour, int minute) {
        startTime = LocalTime.of(hour, minute);
    }

    LocalTime getStartTime() {
        return startTime;
    }

    void setEndTime(int hour, int minute) {
        endTime = LocalTime.of(hour, minute);
    }

    LocalTime getEndTime() {
        return endTime;
    }

    public String getStartString() {
        return startTime.format(TF);
    }

    public String getEndString() {
        return endTime.format(TF);
    }

    //true if this slot fully contains 'other' (same day, other inside this)
    //helper to check faculty availibility
    public boolean contains(TimeSlot other) {
        if (other == null) {
            return false;
        }
        if (day == null || other.day == null) {
            return false;
        }
        if (!day.equals(other.day)) {
            return false;
        }
        if (startTime == null || endTime == null || other.startTime == null || other.endTime == null) {
            return false;
        }
        return (!other.startTime.isBefore(this.startTime)) && (!other.endTime.isAfter(this.endTime));
    }

    //updated overlap method to use LocalTime's built in methods
    boolean overlaps(TimeSlot other) {
        // basic null checks
        if (other == null || day == null || other.day == null) {
            return false;
        }
        // slots on different days do not overlap
        if (!day.equals(other.day)) {
            return false;
        }

        if (startTime == null || endTime == null || other.startTime == null || other.endTime == null) {
            return false;
        }

        // overlap rule: start < otherEnd AND end > otherStart
        return startTime.isBefore(other.endTime) && endTime.isAfter(other.startTime);
    }
}//end of class timeslot

//added new curriculum class
// Defines the courses for a given Major + Semester
class Curriculum implements Displayable {

    private static final int MAX_COURSES = 7; // max courses per semester

    private String major;
    private int semester;
    private Course[] courses = new Course[MAX_COURSES];
    private int courseCount = 0;

    //constructor
    public Curriculum(String major, int semester) {
        if (semester < 1 || semester > 8) {
            throw new IllegalArgumentException("Semester must be between 1 and 8.");
        }
        this.major = major;
        this.semester = semester;
    }

    //getters
    public String getMajor() {
        return major;
    }

    public int getSemester() {
        return semester;
    }

    public Course[] getCourses() {
        return courses;
    }

    public int getCourseCount() {
        return courseCount;
    }

    //method to add a course to the curriculum with validations
    public boolean addCourse(Course c) {

        // Check if curriculum is full
        if (courseCount >= MAX_COURSES) {
            System.out.println("Curriculum course limit reached for " + major + " Sem " + semester);
            return false;
        }

        // Check if course is null
        if (c == null) {
            return false;
        }

        // prevent duplicates
        for (int i = 0; i < courseCount; i++) {
            if (courses[i] != null && courses[i].getCourseCode() == c.getCourseCode()) {
                System.out.println("Course already exists in curriculum: " + c.getCourseName());
                return false;
            }
        }
        //if all validations are passed add the course to the curriculum
        courses[courseCount++] = c;
        return true;
    }

    //method to remove a course from the curriculum
    public boolean removeCourse(int courseCode) {
        for (int i = 0; i < courseCount; i++) {
            if (courses[i] != null && courses[i].getCourseCode() == courseCode) {
                // Shift left to fill the gap
                for (int j = i; j < courseCount - 1; j++) {
                    courses[j] = courses[j + 1];// the element is overwritten by the next element and so on until the end of the array
                }
                courses[--courseCount] = null;  // clear last slot as last element is now duplicated after shifting
                System.out.println("Course removed from curriculum.");
                return true;
            }
        }
        System.out.println("Course not found in curriculum.");
        return false;
    }

    //method to check if a course is part of the curriculum
    public boolean containsCourse(int courseCode) {
        for (int i = 0; i < courseCount; i++) {
            if (courses[i] != null && courses[i].getCourseCode() == courseCode) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void displayInfo() {
        System.out.println("\n===== Curriculum: " + major + " | Semester " + semester + " =====");
        if (courseCount == 0) {
            System.out.println("No courses added.");
            return;
        }
        for (int i = 0; i < courseCount; i++) {
            System.out.println("  - " + courses[i].getCourseName() + " (" + courses[i].getCourseCode() + ")");
        }
    }
}

class AcadClass {

    private int batchNo;
    private String major;
    private char section;
    private int semester;

    private Student[] students = new Student[200];
    private int studentCount = 0;

    private ScheduledClass[] classSchedule = new ScheduledClass[100];
    private int scheduleCount = 0;

    private TimeSlot[] bookedSlots = new TimeSlot[100];
    private int bookedCount = 0;

    private Curriculum curriculum;

    public AcadClass(int batchNo, String major, char section, int semester) throws IllegalArgumentException {
    if (batchNo <= 0) {
        throw new IllegalArgumentException("Invalid batch number.");
    }

    if (semester < 1 || semester > 8) {
        throw new IllegalArgumentException("Semester must be between 1 and 8.");
    }

    if (major == null || major.isBlank()) {
        major = "CS";
    }

    if (section == '\0' || section == ' ') {
        section = 'A';
    }

    this.batchNo = batchNo;
    this.major = major;
    this.section = section;
    this.semester = semester;
    this.studentCount = 0;
}

    // ================= REQUIRED METHODS =================

    public boolean isAvailable(TimeSlot slot) {
        for (int i = 0; i < bookedCount; i++) {
            if (bookedSlots[i] != null && bookedSlots[i].overlaps(slot)) {
                return false;
            }
        }
        return true;
    }

    public void bookSlot(TimeSlot slot) {
        bookedSlots[bookedCount++] = slot;
    }

    public void addToClassSchedule(ScheduledClass sc) {
        classSchedule[scheduleCount++] = sc;
    }

    public ScheduledClass[] getClassSchedule() {
        return classSchedule;
    }

    public int getClassScheduleCount() {
        return scheduleCount;
    }

    public void addStudent(Student s) {
        if (studentCount < students.length) {
            students[studentCount++] = s;
        }
    }

    public Student[] getStudents() {
        return students;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public void setCurriculum(Curriculum c) {
        this.curriculum = c;
    }

    public Curriculum getCurriculum() {
        return curriculum;
    }

    public boolean hasCurriculum() {
        return curriculum != null;
    }

    public Course findCourseByName(String name) {
        if (curriculum == null) return null;
        for (Course c : curriculum.getCourses()) {
            if (c != null && c.getCourseName().equalsIgnoreCase(name)) {
                return c;
            }
        }
        return null;
    }

    public void displayClassSchedule() {
        for (int i = 0; i < scheduleCount; i++) {
            ScheduledClass sc = classSchedule[i];
            System.out.println(sc.getCourse().getCourseName() + " - " + sc.getSlot().getDay());
        }
    }

    public void showAtRiskStudentsForCourse(String courseName) {
        for (int i = 0; i < studentCount; i++) {
            Student s = students[i];
            if (s != null && s.checkAttendanceRisks(courseName)) {
                System.out.println("At risk: " + s.getName());
            }
        }
    }

    // ================= GETTERS =================

    public int getBatchNo() { return batchNo; }
    public String getMajor() { return major; }
    public char getSection() { return section; }
    public int getSemester() { return semester; }
}
//added grading deadline class for deadline reminder feature for faculty

class GradingDeadline {

    Course course;
    String taskTitle;
    String deadline;

    public GradingDeadline(Course course, String taskTitle, String deadline) {
        this.course = course;
        this.taskTitle = taskTitle;
        this.deadline = deadline;
    }

    //getters
    public Course getCourse() {
        return course;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public String getDeadline() {
        return deadline;
    }
}//end of class grading deadline

//added deadline manager class to manage grading deadlines for faculty
class DeadlineManager {

    private GradingDeadline[] gradingDeadlines;
    private int deadlineCount = 0;
    private int capacity;
    private Faculty faculty; // reference back to faculty for name and course validation

    public DeadlineManager(Faculty faculty, int capacity) {
        this.faculty = faculty;
        this.capacity = capacity;
        this.gradingDeadlines = new GradingDeadline[capacity];
    }

    //method to set a grading deadline reminder
    public void setDeadline(String courseName, String taskName, String deadline) {
        // Check if faculty is assigned to this course
        Course course = faculty.getAssignedCourse(courseName); // added helper method in Faculty

        if (course == null) {   // not assigned to this course
            System.out.println("Failed: " + faculty.getName() + " is not assigned to '" + courseName + "'.");
            return;
        }
        // Check duplicate
        for (int i = 0; i < deadlineCount; i++) {
            if (gradingDeadlines[i].getCourse().getCourseName().equalsIgnoreCase(courseName) && gradingDeadlines[i].getTaskTitle().equalsIgnoreCase(taskName)) {
                System.out.println("Failed: Deadline for '" + taskName + "' in '" + courseName + "' already exists.");
                return;
            }
        }
        // Check capacity
        if (deadlineCount >= capacity) {
            System.out.println("Failed: No more deadline slots available.");
            return;
        }
        //if all validations are passed then store the deadline reminder
        gradingDeadlines[deadlineCount++] = new GradingDeadline(course, taskName, deadline);
        try {
            FacultyDbService.insertDeadline(faculty, course, taskName, deadline);
            System.out.println("Deadline set: [" + courseName + "] " + taskName + " → " + deadline);
        } catch (SQLException e) {
            gradingDeadlines[--deadlineCount] = null;
            System.out.println("Failed to save deadline to database: " + e.getMessage());
        }

    }//end of set deadline method

    //method to mark a deadline as done and remove it from the list
    public void markDone(String courseName, String taskName) {
        for (int i = 0; i < deadlineCount; i++) {
            if (gradingDeadlines[i].getCourse().getCourseName().equalsIgnoreCase(courseName) && gradingDeadlines[i].getTaskTitle().equalsIgnoreCase(taskName)) {
                GradingDeadline removed = gradingDeadlines[i];
                // Shift left to fill the gap
                for (int j = i; j < deadlineCount - 1; j++) {
                    gradingDeadlines[j] = gradingDeadlines[j + 1];
                }

                gradingDeadlines[--deadlineCount] = null;  // clear last slot
                try {
                    FacultyDbService.markDeadlineDone(faculty, courseName, taskName);
                    System.out.println("Marked done: [" + courseName + "] " + taskName);
                    return;
                } catch (SQLException e) {
                    for (int j = deadlineCount; j > i; j--) {
                        gradingDeadlines[j] = gradingDeadlines[j - 1];
                    }
                    gradingDeadlines[i] = removed;
                    deadlineCount++;
                    System.out.println("Failed to update deadline status in database: " + e.getMessage());
                    return;
                }
            }
        }
        System.out.println("Failed: No deadline found for '" + taskName + "' in '" + courseName + "'.");
    }

    //method to show pending deadlines
    public void showPending() {
        if (deadlineCount == 0) {
            System.out.println("No pending deadlines for " + faculty.getName() + ".");
            return;
        }
        System.out.println("Pending Deadlines for " + faculty.getName() + ":");
        for (int i = 0; i < deadlineCount; i++) {
            System.out.println("  [" + gradingDeadlines[i].getCourse().getCourseName() + "] " + gradingDeadlines[i].getTaskTitle() + " → Due: " + gradingDeadlines[i].getDeadline());
        }
    }//end of show pending deadlines method

    //method to clear loaded deadlines (for example when faculty logs out or when reloading from database)
    public void clearLoadedDeadlines() {
        for (int i = 0; i < gradingDeadlines.length; i++) {
            gradingDeadlines[i] = null;
        }
        deadlineCount = 0;
    }

    public void loadDeadlineFromDatabase(Course course, String taskName, String deadline) {
        if (course == null || taskName == null || deadline == null) {
            return;
        }
        if (deadlineCount >= capacity) {
            return;
        }
        for (int i = 0; i < deadlineCount; i++) {
            if (gradingDeadlines[i] != null
                    && gradingDeadlines[i].getCourse().getCourseName().equalsIgnoreCase(course.getCourseName())
                    && gradingDeadlines[i].getTaskTitle().equalsIgnoreCase(taskName)) {
                return;
            }
        }
        gradingDeadlines[deadlineCount++] = new GradingDeadline(course, taskName, deadline);
    }
}//end of class deadline manager

class Faculty extends User {

    private static final int MAX_COURSES = 6; //added max course limit for faculty
    private static final int MAX_SLOTS = 20; //added max slot limit for faculty

    private Course[] assignedCourses = new Course[MAX_COURSES];
    private TimeSlot[] availableSlots = new TimeSlot[MAX_SLOTS]; //assuming faculty can have different slots for different courses
    private TimeSlot[] bookedSlots = new TimeSlot[20]; //to keep track of booked slots 
    private ScheduledClass[] schedule = new ScheduledClass[MAX_SLOTS];  // for viewing schedule
    private int courseCount = 0;
    private int slotCount = 0;
    private int bookedCount = 0;
    private DeadlineManager deadlineManager; // replaces all deadline fields
    //constructor

    public Faculty(int id, String name, String email, String password) throws InvalidPasswordException {
        super(id, name, email, password);
        deadlineManager = new DeadlineManager(this, MAX_COURSES * 5); // assuming max 5 deadlines per course(at a time) and passing reference of faculty to deadline manager for validation
    }

    //added get schedule for use in admin module
    public ScheduledClass[] getSchedule() {
        return schedule;
    }

    // added method that calculates total credit hrs and returns it
    public int getTotalAssignedCreditHours() {
        int sum = 0;
        for (int i = 0; i < courseCount; i++) {
            if (assignedCourses[i] != null) {
                sum += assignedCourses[i].getCreditHours();
            }
        }
        return sum;
    }

    //getters
    public int getSlotCount() {
        return slotCount;
    }

    public int getCourseCount() {
        return courseCount;
    }

    public int getBookedCount() {
        return bookedCount;
    }

    public TimeSlot[] getAvailableSlots() {
        return availableSlots;
    }

    //methods
    // updated assign course method for faculty
    public void assignCourse(Course c) throws CourseCapacityException {
        if (courseCount >= MAX_COURSES) {
            throw new CourseCapacityException(getName() + " cannot be assigned more than " + MAX_COURSES + " courses.");
        }
        assignedCourses[courseCount++] = c;
        c.setAssignedFaculty(this); // course now knows its faculty
        System.out.println(getName() + " assigned to: " + c.getCourseName());
    }

    //add available time slot for faculty
    public void addAvailableSlot(TimeSlot slot) {
        if (slotCount < MAX_SLOTS) {
            availableSlots[slotCount++] = slot;
            System.out.println("Added available slot for " + getName() + ": " + slot.getDay() + " " + slot.getStartString() + "-" + slot.getEndString());
        }
    }

    //check if faculty is available for a given timeslot
    public boolean isAvailable(TimeSlot slot) {
        // 1: if all slots are booked then it cannot be booked anymore
        if (bookedCount >= bookedSlots.length) {
            System.out.println("Booking failed: " + getName() + " has no remaining booking capacity.");
            return false;
        }
        // 2:check if the slot is within their available slots
        boolean withinAvailable = false;
        for (int i = 0; i < slotCount; i++) {
            if (availableSlots[i] != null && availableSlots[i].contains(slot)) {
                withinAvailable = true; // the slot is within this available slot
                break;
            }
        }

        if (!withinAvailable) {
            System.out.println(getName() + " is not available for slot: " + slot.getDay() + " " + slot.getStartString() + "-" + slot.getEndString());
            return false;  // no available slot covers this time
        }

        // 3:check if it overlaps with any already booked slot
        for (int i = 0; i < bookedCount; i++) {
            if (bookedSlots[i] != null && bookedSlots[i].overlaps(slot)) {
                return false;
            }
        }

        return true;

    }//end of isAvailable method

    //book a slot for a class
    public boolean bookSlot(TimeSlot slot) {
        //if available then book the slot by adding it to booked slots array
        if (!isAvailable(slot)) {
            return false;
        }
        bookedSlots[bookedCount++] = slot;
        System.out.println("Booking successful: " + getName() + " booked for " + slot.getDay() + " " + slot.getStartString() + "-" + slot.getEndString());
        return true;
    }//end of book slot method

    //helper method for displaying faculty schedule
    public void addToSchedule(ScheduledClass sc) {
        schedule[bookedCount - 1] = sc; // bookedCount already incremented in bookSlot method, so the current class should be added at bookedCount - 1 index
    }
    //method to clear loaded schedule (for example when faculty logs out or when reloading from database)

    void clearLoadedSchedule() {
        for (int i = 0; i < assignedCourses.length; i++) {
            assignedCourses[i] = null;
        }
        for (int i = 0; i < bookedSlots.length; i++) {
            bookedSlots[i] = null;
        }
        for (int i = 0; i < schedule.length; i++) {
            schedule[i] = null;
        }
        courseCount = 0;
        bookedCount = 0;
    }

    void loadScheduledClassFromDatabase(ScheduledClass sc) {
        if (sc == null) {
            return;
        }
        Course course = sc.getCourse();
        if (course != null && getAssignedCourse(course.getCourseName()) == null && courseCount < assignedCourses.length) {
            assignedCourses[courseCount++] = course;
            course.setAssignedFaculty(this);
        }
        if (bookedCount < bookedSlots.length) {
            bookedSlots[bookedCount] = sc.getSlot();
            schedule[bookedCount] = sc;
            bookedCount++;
        }
    }

    void clearLoadedDeadlines() {
        deadlineManager.clearLoadedDeadlines();
    }

    void loadDeadlineFromDatabase(Course course, String taskName, String deadline) {
        deadlineManager.loadDeadlineFromDatabase(course, taskName, deadline);
    }

    //method to view teaching schedule of the faculty
    public void viewTeachingSchedule() {
        System.out.println("\n── Teaching Schedule for " + getName() + " ──");
        //if no classes are scheduled
        if (bookedCount == 0) {
            System.out.println("  No classes scheduled.");
            return;
        }
        System.out.printf("%-25s  %-10s  %-8s  %-8s  %-5s  %s%n", "Course", "Day", "Start", "End", "Room", "Sec");
        System.out.println("─".repeat(65));
        //loop through the booked classes and display them
        for (int i = 0; i < bookedCount; i++) {
            ScheduledClass sc = schedule[i];
            System.out.printf("%-25s  %-10s  %-8s  %-8s  %-5d  %c%n",
                    sc.getCourse().getCourseName(),
                    sc.getSlot().getDay(),
                    sc.getSlot().getStartString(),
                    sc.getSlot().getEndString(),
                    sc.getRoom().getRoomNumber(),
                    sc.getSection());
        }
    }//end of view teaching schedule method

    // DeadlineManager needs this to validate course assignment(to find if faculty is assigned to the course)
    public Course getAssignedCourse(String courseName) {
        for (int i = 0; i < courseCount; i++) {
            if (assignedCourses[i].getCourseName().equalsIgnoreCase(courseName)) {
                return assignedCourses[i];
            }
        }
        return null; // not found
    }//end of get assigned course method

    public void setGradingDeadline(String course, String task, String deadline) {
        deadlineManager.setDeadline(course, task, deadline);
    }

    public void markDeadlineDone(String course, String task) {
        deadlineManager.markDone(course, task);
    }

    public void getPendingDeadlines() {
        deadlineManager.showPending();
    }

    //view attendance analytics feature
    public void showAtRiskStudents(AcadClass ac, String courseName) {
        // Check if faculty teaches this course to this class
        boolean teaches = false;
        // Loop through the class schedule to find if this faculty is teaching the specified course to this class
        for (int i = 0; i < ac.getClassScheduleCount(); i++) {
            ScheduledClass sc = ac.getClassSchedule()[i];
            if (sc == null) {
                continue;
            }
            if (sc.getCourse() != null && sc.getCourse().getCourseName().equalsIgnoreCase(courseName) && sc.getTeacher() != null && sc.getTeacher().getId() == this.getId()) {
                teaches = true;
                break;  //if found,break the loop
            }
        }
        //if not teaching this course to this class, do not show analytics
        if (!teaches) {
            System.out.println("You are not teaching '" + courseName + "' to this class.");
            return;
        }
        ac.showAtRiskStudentsForCourse(courseName);
    }//end of show at-risk students method

    //makeup request method
    public void requestMakeup(String courseCode, String reason) {
        System.out.println("Makeup request by " + getName() + " for " + courseCode + ": " + reason);
        try {
            FacultyDbService.insertMakeupRequest(this, courseCode, reason);
        } catch (SQLException e) {
            System.out.println("Failed to save makeup request to database: " + e.getMessage());
        }
    }//end of makeup request method(made it basic,more implementation to be done in admin module for approval and tracking)

    @Override
    public void displayInfo() {
        super.displayInfo();
        System.out.println("Assigned Courses: " + courseCount);

    }
}//end of class faculty(only view attendance anylitics feature left)

class ScheduledClass {  //to store output of timetable generator.

    private Course course;
    private Faculty teacher;
    private Room room;
    private TimeSlot slot;
    private char section;
    private AcadClass acadClass; // added: which class this lecture belongs to

    public ScheduledClass(Course course, Faculty teacher, Room room, TimeSlot slot, char section, AcadClass acadClass) {
        this.course = course;
        this.teacher = teacher;
        this.room = room;
        this.slot = slot;
        this.section = section;
        this.acadClass = acadClass;
    }

    Course getCourse() {
        return course;
    }

    Faculty getTeacher() {
        return teacher;
    }

    Room getRoom() {
        return room;
    }

    TimeSlot getSlot() {
        return slot;
    }

    char getSection() {
        return section;
    }

    //added getter for acad class
    public AcadClass getAcadClass() {
        return acadClass;
    }
}

class Timetable implements Displayable {   //Stores all scheduled classes.
    //attributes

    private ScheduledClass[] schedule;
    private int totalEntries;
    private static final int maxEntries = 100;

    //constructor
    public Timetable() {
        schedule = new ScheduledClass[maxEntries];
        totalEntries = 0;
    }

    //getters
    public ScheduledClass[] getSchedule() {
        return schedule;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    // add a class if no room,acadClass or faculty clash
    public void addClass(ScheduledClass sc) throws ScheduleConflictException {

        //1. Check class availability 
        if (sc.getAcadClass() == null) {
            throw new ScheduleConflictException("ScheduledClass missing AcadClass reference.");
        }
        if (!sc.getAcadClass().isAvailable(sc.getSlot())) {
            throw new ScheduleConflictException("AcadClass unavailable/booked or outside 9-5: " + sc.getAcadClass().getMajor() + "-" + sc.getAcadClass().getBatchNo() + sc.getAcadClass().getSection());
        }

        // 2. Check faculty availibility
        if (!sc.getTeacher().isAvailable(sc.getSlot())) {
            throw new ScheduleConflictException("Faculty unavailable or already booked: " + sc.getTeacher().getName());
        }
        // 3. Check room availability
        if (!sc.getRoom().isAvailable(sc.getSlot())) {
            throw new ScheduleConflictException("Room unavailable or already booked: Room " + sc.getRoom().getRoomNumber());
        }

        // 3. All confirmed available, so book them
        sc.getAcadClass().bookSlot(sc.getSlot());      //book the class for this slot to prevent future clashes
        sc.getAcadClass().addToClassSchedule(sc);      //update class schedule for viewing

        sc.getTeacher().bookSlot(sc.getSlot());
        sc.getTeacher().addToSchedule(sc);             //added to faculty schedule for viewing
        sc.getRoom().bookRoom(sc.getSlot());

        // 4. Add to master timetable
        schedule[totalEntries++] = sc;
    }//end of add class method

    @Override
    public void displayInfo() {
        System.out.println("\n============================ TIMETABLE ============================");
        System.out.printf("%-25s  %-15s  %-10s  %-10s  %-6s  %-5s  %s%n",
                "Course", "Faculty", "Day", "Start", "End", "Room", "Sec");//left aligned
        System.out.println("─".repeat(85));//creats horizontal line
        for (int i = 0; i < totalEntries; i++)//repeated loop for each scheduled class 
        {
            ScheduledClass sc = schedule[i];
            System.out.printf("%-25s  %-15s  %-10s  %-10s  %-6s  %-5d  %c%n",
                    sc.getCourse().getCourseName(),//course name
                    sc.getTeacher().getName(),//teacher name
                    sc.getSlot().getDay(),//day
                    sc.getSlot().getStartString(),//class starting time
                    sc.getSlot().getEndString(),//class ending time
                    sc.getRoom().getRoomNumber(),//room number
                    sc.getSection());//character section (A,B etc)
        }
    }
}//end of timetable class

// added exam paper class to store course and students for each paper 
class ExamPaper {

    private Course course;
    private ArrayList<Student> students = new ArrayList<>(); // to store students taking this paper
    private ArrayList<AcadClass> classes = new ArrayList<>(); // to store which classes are taking this paper for better seating arrangement
    private AcademicSystem system; // reference to the main system for any finding students and classes related to this paper
    //cosntructor(admin will only give which course's paper is being created and the system, students will be added later by system automatically) 

    public ExamPaper(Course course, AcademicSystem system) {
        this.course = course;
        this.system = system;
    }

    //loop thorough all Acadclasses in the system and find which classes are taking this course and add them in the list and also add all students of those classes in the student list for this paper
    public void FindClasses() {
        //clear previous data if any before finding again to avoid duplicates in case this method is called multiple times for the same paper
        classes.clear();
        students.clear();
        for (AcadClass ac : system.getClasses()) {
            if (ac == null) {
                continue;
            }
            //check if this class has curriculum linked and if it contains this course
            if (ac.hasCurriculum() && ac.getCurriculum().containsCourse(course.getCourseCode())) {
                classes.add(ac);
                //add all students of this class to the paper's student list
                for (Student s : ac.getStudents()) {
                    if (s != null) {
                        students.add(s);
                    }
                }
            }
        }

    }

    //getters
    public Course getCourse() {
        return course;
    }

    public ArrayList<Student> getStudents() {
        return students;
    }

    public ArrayList<AcadClass> getClasses() {
        return classes;
    }

    //count students taking this paper
    public int getStudentCount() {
        return students.size();
    }

    //count classes taking this paper
    public int getClassCount() {
        return classes.size();
    }
}

// day schedule that contains all papers held on the same date
class ExamDaySchedule {

    private static final int MAX_PAPERS = 10; // max papers that can be held on the same day
    private String examDate;    // date in string format for simplicity, can be changed later to integrate with date handling libraries
    private ExamPaper[] papers = new ExamPaper[MAX_PAPERS];
    private int paperCount = 0;

    //constructor(admin only gives the date for which the schedule is being created,papers will be added later by system automatically)
    public ExamDaySchedule(String examDate) {
        this.examDate = examDate;
    }

    //
    public String getExamDate() {
        return examDate;
    }

    public int getPaperCount() {
        return paperCount;
    }

    public ExamPaper[] getPapers() {
        return papers;
    }

    public String getDate() {
        return examDate;
    }

    public int getTotalStudentsForDay() {
        int total = 0;
        //loop through all papers of the day and count total students taking exams on this day
        for (int i = 0; i < paperCount; i++) {
            if (papers[i] != null) {
                total += papers[i].getStudentCount();
            }
        }
        return total;
    }

    //add paper to this day
    public boolean addPaper(ExamPaper paper) {
        //if paper is null or paper count exceeds limit,dont add
        if (paper == null || paperCount >= papers.length) {
            System.out.println("Cannot add paper: Invalid paper or maximum papers reached for " + examDate);
            return false;
        }
        for (int i = 0; i < paperCount; i++) {
            if (papers[i] != null
                    && papers[i].getCourse() != null
                    && papers[i].getCourse().getCourseCode() == paper.getCourse().getCourseCode()) {
                System.out.println("Paper already exists in schedule for " + examDate + ": " + paper.getCourse().getCourseName());
                return false;
            }
        }
        //if all validations are passed then add the paper to the schedule for this day
        papers[paperCount++] = paper;
        return true;
    }
}
//seating assignment class

class SeatingAssignment {

    private String date; // date of the exam
    private Course course; // course for which this seating assignment is made
    private Student student;
    private Room room;
    private int seatNumber; // to store the seat number assigned to this student in that room
    //constructor

    public SeatingAssignment(Student s, Room r, int seat, String date, Course course) {
        this.student = s;
        this.room = r;
        this.seatNumber = seat;
        this.date = date;
        this.course = course;
    }

    // getters
    public Student getStudent() {
        return student;
    }

    public Room getRoom() {
        return room;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public String getDate() {
        return date;
    }

    public Course getCourse() {
        return course;
    }

}

// added exam seating class
class ExamSeating {

    //to generate a single day seating plan
    //attributes
    private ExamDaySchedule examDaySchedule;
    private Room[] rooms;  // rooms in which exam can be held
    private ArrayList<Room> ExamRooms; // to store the rooms needed to accommodate all students of the day based on the total count and room capacities
    private ArrayList<SeatingAssignment> seatingPlan = new ArrayList<>(); // to store the final seating plan for the day

    // constructor(admin will give the day schedule for which the seating plan is being generated and the rooms available for that day)
    public ExamSeating(ExamDaySchedule examDaySchedule, Room[] rooms) {
        this.examDaySchedule = examDaySchedule;
        this.rooms = rooms;
    }

    //getters
    public ExamDaySchedule getExamDaySchedule() {
        return examDaySchedule;
    }

    public Room[] getRooms() {
        return rooms;
    }

    public ArrayList<SeatingAssignment> getSeatingPlan() {
        return seatingPlan;
    }

    //other methods
    // count all students across all papers of the day to get total no of seats needed
    private int getTotalStudentsForDay() {
        int total = 0;
        for (int i = 0; i < examDaySchedule.getPaperCount(); i++) {
            ExamPaper p = examDaySchedule.getPapers()[i];
            if (p != null) {
                total += p.getStudentCount();
            }
        }
        return total;
    }

    // get total rooms required to accommodate all students
    public ArrayList<Room> getRoomsNeeded() {
        ArrayList<Room> needed = new ArrayList<>();
        int target = getTotalStudentsForDay();// total seats needed for the day
        int capacityReached = 0;
        for (Room r : rooms) {
            // stop calculating rooms if capacity Reached is equal to or more than target
            // we have enough rooms so stop the loop immediately
            if (capacityReached >= target) {
                break;
            }
            if (r != null) {    //if room is not null, then calculate effective capacity and add to total capacity reached so far
                int effectiveCap = r.getCapacity() / 2; //half capacity of room is used to maintain distancing
                capacityReached += effectiveCap;    // add effective capacity of this room to the total capacity reached so far
                needed.add(r);  // add this room to the list of needed rooms
            }
        }
        return needed;
    }

    // day-wise seating plan generator
    public void generateSeatingPlanForDay() throws RoomCapacityException {
        if (examDaySchedule == null || examDaySchedule.getPaperCount() == 0) {
            throw new RoomCapacityException("No papers scheduled for exam day.");
        }
        if (rooms == null || rooms.length == 0) {
            throw new RoomCapacityException("No rooms provided for exam seating.");
        }

        //clear previous seating plan if any before generating again to avoid duplicates in case this method is called multiple times for the same day
        seatingPlan.clear();

        ExamRooms = getRoomsNeeded();   //store rooms needed in exam rooms for record
        if (ExamRooms == null || ExamRooms.isEmpty()) {
            throw new RoomCapacityException("No rooms available for exam seating.");
        }

        // Build list of papers that actually exist (paperCount might be <max papers that can be held on the same day)
        ArrayList<ExamPaper> papersList = new ArrayList<>();
        ExamPaper[] papers = examDaySchedule.getPapers();   // get papers from the schedule
        //transverse the papers array and add non-null papers with valid courses to the papers list
        for (int i = 0; i < examDaySchedule.getPaperCount(); i++) {
            if (papers[i] != null && papers[i].getCourse() != null) {
                papersList.add(papers[i]);
            }
        }//end of loop to build papers list

        //check if the list is empty
        if (papersList.isEmpty()) {
            throw new RoomCapacityException("No valid papers found for exam day.");
        }

        // ensure enough effective seats exist for all students
        int totalStudents = getTotalStudentsForDay(); // get total students for the day by counting students across all papers
        int totalEffectiveSeats = 0;

        // loop through the rooms needed and calculate total effective seats available by summing up half of the capacity of each room (due to distancing)
        for (Room r : ExamRooms) {
            if (r == null) {
                continue;
            }
            totalEffectiveSeats += (r.getCapacity() / 2); // effective capacity
        }//end of loop

        // if total effective seats available are less than total students, then all students cannot be accommodated, so throw an exception
        if (totalEffectiveSeats < totalStudents) {
            throw new RoomCapacityException("Not enough effective capacity. Needed " + totalStudents + " seats, but only " + totalEffectiveSeats + " available (half-capacity rule).");
        }

        final String currentExamDate = examDaySchedule.getDate();
        // We fill rooms sequentially, seat numbers increment by 1
        int roomIdx = 0;
        int seatNumber = 1;             // actual seat number we assign/print
        int usedInCurrentRoom = 0;      // how many effective seats already used in this room

        //move to next room when current room reaches effective capacity
        //Process papers in pairs(2 papers can conducted in a room)
        for (int paperPairStart = 0; paperPairStart < papersList.size(); paperPairStart += 2) {
            //store papers in this pair in paperA and paperB(paperB can be null if this is the last paper and total papers are odd)
            ExamPaper paperA = papersList.get(paperPairStart);
            ExamPaper paperB = (paperPairStart + 1 < papersList.size()) ? papersList.get(paperPairStart + 1) : null;//if paper b exists, get the paper, otherwise set it to null

            //store courses
            Course courseA = paperA.getCourse();
            Course courseB = (paperB != null) ? paperB.getCourse() : null;//if paper b exists, get the course, otherwise set it to null

            //store students of this pair(if b null,then only students of paper a will be stored and processed)
            ArrayList<Student> studentsA = paperA.getStudents();
            ArrayList<Student> studentsB = (paperB != null) ? paperB.getStudents() : null;

            //store indexes for both lists to keep track of which student to pick next from each list
            int idxA = 0;
            int idxB = 0;

            // pick students from A and B alternatively till all students from both papers are assigned seats
            while ((studentsA != null && idxA < studentsA.size()) || (studentsB != null && idxB < studentsB.size())) {

                // pick next from A if available
                if (studentsA != null && idxA < studentsA.size()) {
                    Student s = studentsA.get(idxA++);

                    if (s != null) {
                        // ensure we have a room
                        if (roomIdx >= ExamRooms.size()) {
                            throw new RoomCapacityException("Not enough rooms to seat all students for " + currentExamDate + ".");
                        }

                        // get current room and its effective capacity
                        Room currentRoom = ExamRooms.get(roomIdx);
                        int effectiveCap = currentRoom.getCapacity() / 2;

                        // if this room is full under effective capacity go to next room
                        if (usedInCurrentRoom >= effectiveCap) {
                            roomIdx++;
                            seatNumber = 1;
                            usedInCurrentRoom = 0;

                            // ensure we have a room after moving to next
                            if (roomIdx >= ExamRooms.size()) {
                                throw new RoomCapacityException("Not enough rooms to seat all students for " + currentExamDate + ".");
                            }

                            // update current room and effective capacity after moving to next
                            currentRoom = ExamRooms.get(roomIdx);
                            effectiveCap = currentRoom.getCapacity() / 2;
                        }

                        //assign seat to this student
                        SeatingAssignment assignment = new SeatingAssignment(s, currentRoom, seatNumber, currentExamDate, courseA);
                        seatingPlan.add(assignment);//add this assignment to the seating plan of the day
                        s.addExamAssignment(assignment);//update in student aswell

                        // increment seat number and used count for current room
                        seatNumber++;
                        usedInCurrentRoom++;
                    }//end of if student A not null
                }//end of if students A available

                // pick next from B if available
                if (studentsB != null && idxB < studentsB.size()) {
                    Student s = studentsB.get(idxB++);

                    //if student is not null
                    if (s != null) {
                        // ensure we have a room
                        if (roomIdx >= ExamRooms.size()) {
                            throw new RoomCapacityException("Not enough rooms to seat all students for " + currentExamDate + ".");
                        }

                        // get current room and its effective capacity
                        Room currentRoom = ExamRooms.get(roomIdx);
                        int effectiveCap = currentRoom.getCapacity() / 2;

                        // if this room is full under effective capacity go to next room
                        if (usedInCurrentRoom >= effectiveCap) {
                            roomIdx++;  // move to next room
                            seatNumber = 1; // reset seat number for new room
                            usedInCurrentRoom = 0;

                            // ensure we have a room after moving to next
                            if (roomIdx >= ExamRooms.size()) {
                                throw new RoomCapacityException("Not enough rooms to seat all students for " + currentExamDate + ".");
                            }

                            // update current room and effective capacity after moving to next
                            currentRoom = ExamRooms.get(roomIdx);
                            effectiveCap = currentRoom.getCapacity() / 2;
                        }

                        //assign seat to this student
                        SeatingAssignment assignment = new SeatingAssignment(s, currentRoom, seatNumber, currentExamDate, courseB);
                        seatingPlan.add(assignment);    //add this assignment to the seating plan of the day
                        s.addExamAssignment(assignment);    //update in student aswell
                        seatNumber++;
                        usedInCurrentRoom++;
                    }//end of if student B not null
                }//end of if students B available
            }//end of while loop to assign seats for this pair of papers
        }//end of loop to process papers in pairs
    }//end of generate seating plan method

    //display the generated seating plan room wise
    public void displaySeatingPlan() {
        System.out.println("\n── Seating Plan for " + examDaySchedule.getDate() + " ──");
        if (seatingPlan.isEmpty()) {
            System.out.println("  No seating assignments generated yet.");
            return;
        }
        //transverse the seating plan and display room-wise seating arrangement with student names and course details
        int currentRoomNumber = -1;
        for (SeatingAssignment sa : seatingPlan) {
            // safety check for nulls in seating assignment
            if (sa == null || sa.getRoom() == null || sa.getStudent() == null || sa.getCourse() == null) {
                continue;
            }

            // if we encounter a new room number, print the room header
            int assignmentRoomNumber = sa.getRoom().getRoomNumber();
            if (assignmentRoomNumber != currentRoomNumber) {
                currentRoomNumber = assignmentRoomNumber;
                System.out.println("\nRoom " + currentRoomNumber + ":");
            }
            // print the seat number, student name, student ID and course name for this assignment
            System.out.println("  Seat " + sa.getSeatNumber() + " -> " + sa.getStudent().getName() + " (ID: " + sa.getStudent().getId() + ")" + " | Course: " + sa.getCourse().getCourseName());

        }//end of loop to display seating plan

    }//end of display seating plan method

}//end of exam seating class

// added AcademicSystem
class AcademicSystem {

    //array to store classes
    AcadClass[] classes = new AcadClass[100];//max limit 50 for now
    int classCount = 0;
    //array to store curicula
    Curriculum[] curricula = new Curriculum[200];
    int curriculumCount = 0;
    //array to store faculty 
    Faculty[] faculty = new Faculty[200];
    int facultyCount = 0;
    //array to store rooms
    Room[] rooms = new Room[100];
    int roomCount = 0;
    // master timetable to store all the timetables
    Timetable timetable = new Timetable();

    //store all exam-day schedules created by admin
    private ArrayList<ExamDaySchedule> examDaySchedules = new ArrayList<>();

    //store generated seating plans (one Exam Seating object per day)
    private ArrayList<ExamSeating> generatedExamSeatingPlans = new ArrayList<>();

    //
    //adders check empty index and size or arrays
    public void addClass(AcadClass c) {
        if (c != null && classCount < classes.length) {
            classes[classCount++] = c;
        }
    }

    public void addCurriculum(Curriculum c) {
        if (c != null && curriculumCount < curricula.length) {
            curricula[curriculumCount++] = c;
        }
    }

    public void addFaculty(Faculty f) {
        if (f != null && facultyCount < faculty.length) {
            faculty[facultyCount++] = f;
        }
    }

    public void addRoom(Room r) {
        if (r != null && roomCount < rooms.length) {
            rooms[roomCount++] = r;
        }
    }

    Course findCourseByName(String courseName) {
        if (courseName == null || courseName.isEmpty()) {
            return null;
        }
        //loop through all classes and their curricula to find the course with this name and return it
        for (int i = 0; i < classCount; i++) {
            if (classes[i] != null && classes[i].findCourseByName(courseName) != null) {
                return classes[i].findCourseByName(courseName);
            }
        }
        System.out.println("Course '" + courseName + "' not found in any class.");
        return null;
    }

    public ExamPaper createExamPaperForCourse(String courseName) {
        if (courseName == null || courseName.isEmpty()) {
            return null;
        }
        Course course = findCourseByName(courseName);
        //if course is not found in any class, return null and do not create paper
        if (course == null) {
            return null;
        }
        //if course is found, create a new paper for this course 
        ExamPaper paper = new ExamPaper(course, this);  // create a new paper for this course and pass reference of system to it so that it can find students and classes for this paper
        paper.FindClasses(); // automatically find which classes and students are taking this course and add them to the paper
        return paper;
    }

    //create exam day schedule for a given date
    public boolean createExamDaySchedule(String date) {
        if (date == null || date.isEmpty()) {
            System.out.println("Invalid date for exam schedule.");
            return false;
        }
        for (ExamDaySchedule existing : examDaySchedules) {
            if (existing != null && existing.getExamDate().equals(date)) {
                System.out.println("Exam schedule already exists for date: " + date);
                return false;
            }
        }
        examDaySchedules.add(new ExamDaySchedule(date));    // create a new day schedule for this date and add it to the system storage for schedules
        return true;
    }

    //add a paper to a day schedule
    public boolean addExamPaperToDaySchedule(ExamPaper paper, String date) {
        if (paper == null || date == null || date.isEmpty()) {
            System.out.println("Invalid paper or date.");
            return false;
        }
        //find the day schedule for this date
        for (ExamDaySchedule daySchedule : examDaySchedules) {
            if (daySchedule.getExamDate().equals(date)) {
                return daySchedule.addPaper(paper); // add this paper to the found day schedule
            }
        }
        System.out.println("No exam schedule found for date: " + date);
        return false; // no schedule found for this date
    }

    //getter
    public AcadClass[] getClasses() {
        return classes;
    }

    public ArrayList<ExamDaySchedule> getExamDaySchedules() {
        return examDaySchedules;
    }

    //search for a day schedule by date
    public ExamDaySchedule findExamDayScheduleByDate(String date) {
        for (ExamDaySchedule daySchedule : examDaySchedules) {
            if (daySchedule.getExamDate().equals(date)) {
                return daySchedule;
            }
        }
        return null; // not found
    }

    // save generated seating plan for a day
    public void addGeneratedExamSeatingPlan(ExamSeating seating) {
        if (seating == null || seating.getExamDaySchedule() == null) {
            return;
        }
        String date = seating.getExamDaySchedule().getDate();
        for (int i = 0; i < generatedExamSeatingPlans.size(); i++) {
            ExamSeating existing = generatedExamSeatingPlans.get(i);
            if (existing != null
                    && existing.getExamDaySchedule() != null
                    && existing.getExamDaySchedule().getDate().equals(date)) {
                generatedExamSeatingPlans.set(i, seating);
                return;
            }
        }
        generatedExamSeatingPlans.add(seating);
    }

    //getter
    public ArrayList<ExamSeating> getGeneratedExamSeatingPlans() {
        return generatedExamSeatingPlans;
    }

    //to regenerate from scratch for next exam cycle
    public void clearGeneratedExamSeatingPlans() {
        generatedExamSeatingPlans.clear();
    }
}//end of AcademicSystem

// ====================================================
//added SemesterCoordinator Admin uses this to generate clash-free timetable.
// 
class SemesterCoordinator {

    private AcademicSystem sys;

    //constructor
    public SemesterCoordinator(AcademicSystem sys) {
        this.sys = sys;
    }

    public void linkCurricula() {
        System.out.println("\n   Linking curricula to classes   ");

        for (int i = 0; i < sys.classCount; i++) //loop to traverse through classes array
        {
            AcadClass ac = sys.classes[i];//stores the particular class in a new variable
            boolean linked = false;
            //loop to traverse through curicula array
            for (int j = 0; j < sys.curriculumCount; j++) {
                Curriculum cur = sys.curricula[j];
                if (cur != null
                        && cur.getMajor().equalsIgnoreCase(ac.getMajor())
                        && cur.getSemester() == ac.getSemester()) //checks that curriculum is not empty and then matches the major and semester
                {
                    ac.setCurriculum(cur);     //when everything matches, it sets curriculum for the particular class
                    linked = ac.hasCurriculum();
                    break;
                }
            }

            if (!linked) {
                System.out.println("WARNING: No curriculum found for "
                        + ac.getMajor() + " semester " + ac.getSemester()
                        + " (Class " + ac.getBatchNo() + ac.getSection() + ")");
            }
        }
    }

    //enroll all students of class in its curriculum courses
    public void enrollStudentsInCourses() {
        System.out.println("\n   Enrolling students in courses   ");
        //loop traversing through classes
        for (int i = 0; i < sys.classCount; i++) {
            AcadClass ac = sys.classes[i];
            //if class has no linked curriculum then skip the iteration
            if (!ac.hasCurriculum()) {
                System.out.println("Skipping enrollment for "
                        + ac.getMajor() + "-" + ac.getBatchNo() + ac.getSection()
                        + ": No curriculum linked.");
                continue;
            }
            //stores curriculum and the students array of each class
            Curriculum cur = ac.getCurriculum();
            Student[] students = ac.getStudents();
            //loop traverses through each student array
            for (int s = 0; s < ac.getStudentCount(); s++) {
                Student st = students[s];
                for (int c = 0; c < cur.getCourseCount(); c++) {
                    Course course = cur.getCourses()[c];
                    if (course == null) {
                        continue;//if no student present at the index then skip iteration
                    }
                    try {
                        st.enrollCourse(course);//try block for enroll course 
                    } catch (CourseCapacityException e) {
                        System.out.println("Enroll failed: " + e.getMessage());//catch block if enrollment fails
                    }
                }
            }
            //prompt if enrollment succeeds
            System.out.println("Enrolled " + ac.getStudentCount() + " students of "
                    + ac.getMajor() + "-" + ac.getBatchNo() + ac.getSection()
                    + " in " + cur.getCourseCount() + " courses.");
        }
    }

    //method that returns the available room
    private Room findAvailableRoom(TimeSlot slot) {
        for (int r = 0; r < sys.roomCount; r++) {
            Room room = sys.rooms[r];
            if (room != null && room.isAvailable(slot)) {
                return room;
            }
        }
        return null;
    }

    //timetable generator
    public void generateTimetable() {
        System.out.println("\n   Generating clash-free timetable   ");
        //for all classes
        for (int i = 0; i < sys.classCount; i++) {
            AcadClass ac = sys.classes[i];
            if (!ac.hasCurriculum()) {
                continue;
            }
            //gets curriculum of class
            Curriculum cur = ac.getCurriculum();
            //for everycourse
            for (int c = 0; c < cur.getCourseCount(); c++) {
                Course course = cur.getCourses()[c];
                //if no course skip iteration
                if (course == null) {
                    continue;
                }
                //if no faculty assigned for a course then skip iteration
                if (!course.hasFaculty()) {
                    System.out.println("Skipping " + course.getCourseName()
                            + " for " + ac.getMajor() + "-" + ac.getBatchNo() + ac.getSection()
                            + ": no faculty assigned.");
                    continue;
                }
                //if faculty assigned then stores it
                Faculty teacher = course.getFacultyAssigned();
                boolean scheduled = false;
                //stores the available slots of the teacher
                TimeSlot[] teacherSlots = teacher.getAvailableSlots();

                // try each available slot of teacher until we successfully add to timetable
                for (int sl = 0; sl < teacher.getSlotCount(); sl++) {
                    TimeSlot slot = teacherSlots[sl];
                    if (slot == null) {
                        continue;
                    }
                    //checks for every room for an available room
                    Room room = findAvailableRoom(slot);
                    if (room == null) {
                        continue;
                    }
                    ScheduledClass sc = new ScheduledClass(course, teacher, room, slot, ac.getSection(), ac);

                    try {
                        sys.timetable.addClass(sc); // single booking/validation gate
                        System.out.println("Scheduled: " + course.getCourseName()
                                + " | " + ac.getMajor() + "-" + ac.getBatchNo() + ac.getSection()
                                + " | " + slot.getDay() + " " + slot.getStartString() + "-" + slot.getEndString()
                                + " | Room " + room.getRoomNumber()
                                + " | " + teacher.getName());
                        scheduled = true;
                        break;
                    } catch (ScheduleConflictException e) {

                    }
                }

                if (!scheduled) {
                    System.out.println("Could not schedule: " + course.getCourseName()
                            + " for class " + ac.getMajor() + "-" + ac.getBatchNo() + ac.getSection());
                }
            }
        }
        //prompt when timetable generation is successful
        System.out.println("\n   Timetable generation complete   ");
        sys.timetable.displayInfo();
    }

    //mehod to run all the above simultaneously
    public void runSetup() {
        linkCurricula();
        enrollStudentsInCourses();
        generateTimetable();
    }
}//end of class

class WorkloadService {

    // 1) faculty workload report (FacultyModule can call this)
    public static void reportWeeklyWorkload(Faculty f) {
        if (f == null) {
            return;
        }

        System.out.println("\n── Weekly Workload for " + f.getName() + " ──");

        if (f.getBookedCount() == 0) {
            System.out.println("  No classes scheduled.");
            // still show credits even if no classes, since faculty might be assigned courses but not scheduled yet
            System.out.println("  Total lectures: 0");
            System.out.println("  Total credits:  " + f.getTotalAssignedCreditHours());
            return;
        }

        int mon = 0, tue = 0, wed = 0, thu = 0, fri = 0;

        // loop through the faculty's booked classes and count how many are on each day
        ScheduledClass[] sched = f.getSchedule();

        for (int i = 0; i < f.getBookedCount(); i++) {
            ScheduledClass sc = sched[i];
            if (sc == null || sc.getSlot() == null) {
                continue;
            }
            // get the day of the week for this class and increment the corresponding counter
            String day = sc.getSlot().getDay();
            if (day == null) {
                continue;
            }

            switch (day) {
                case "Monday":
                    mon++;
                    break;
                case "Tuesday":
                    tue++;
                    break;
                case "Wednesday":
                    wed++;
                    break;
                case "Thursday":
                    thu++;
                    break;
                case "Friday":
                    fri++;
                    break;
            }
        }
        //print the breakdown of lectures per day
        System.out.println("  Monday:    " + mon);
        System.out.println("  Tuesday:   " + tue);
        System.out.println("  Wednesday: " + wed);
        System.out.println("  Thursday:  " + thu);
        System.out.println("  Friday:    " + fri);
        //print total lectures and total credits for the faculty
        System.out.println("  Total lectures: " + f.getBookedCount());
        System.out.println("  Total credits:  " + f.getTotalAssignedCreditHours());
    }

    // 2) admin report of all faculty workload (Admin module can call this)
    public static void reportAllFacultyWorkload(AcademicSystem sys) {
        System.out.println("\n================ FACULTY WORKLOAD REPORT ================");
        if (sys == null || sys.facultyCount == 0) {
            System.out.println("No faculty registered.");
            return;
        }

        // summary list 
        System.out.printf("%-20s  %-10s  %-10s%n", "Faculty", "Credits", "Lectures");
        System.out.println("─".repeat(50));
        // loop through all faculty and print their total assigned credits and booked lectures
        for (int i = 0; i < sys.facultyCount; i++) {
            Faculty f = sys.faculty[i];
            if (f == null) {
                continue;
            }

            System.out.printf("%-20s  %-10d  %-10d%n",
                    f.getName(),
                    f.getTotalAssignedCreditHours(),
                    f.getBookedCount());
        }

        // detailed breakdown per faculty
        for (int i = 0; i < sys.facultyCount; i++) {
            Faculty f = sys.faculty[i];
            if (f == null) {
                continue;
            }
            reportWeeklyWorkload(f);    // calls the previous method to show weekly workload for each faculty
        }
    }
}

// added RoomUtilizationService
// Tracks how many slots each room has booked.
class RoomUtilizationService {

    public static void reportRoomUtilization(AcademicSystem sys) {
        System.out.println("\n================ ROOM UTILIZATION REPORT ================");
        if (sys.roomCount == 0) {
            System.out.println("No rooms registered.");
            return;
        }
        //prints report of booked rooms
        System.out.printf("%-10s  %-10s  %-10s  %-10s%n", "Room", "Capacity", "Booked", "MaxSlots");
        System.out.println("-".repeat(55));//repeates - 55 times

        for (int i = 0; i < sys.roomCount; i++) {
            Room r = sys.rooms[i];
            if (r == null) {
                continue;
            }

            System.out.printf("%-10d  %-10d  %-10d  %-10d%n",
                    r.getRoomNumber(),
                    r.getCapacity(),
                    r.getBookedCount(),
                    r.getBookingCapacity());
        }
    }
}//end of RoomUtilizationService

//added MaintenanceService
//Admin can view maintenance report 
class MaintenanceService {

    public static void showMaintenanceReports(AcademicSystem sys) {
        System.out.println("\n================ MAINTENANCE REPORTS ================");
        boolean found = false;

        for (int i = 0; i < sys.roomCount; i++) {
            Room r = sys.rooms[i];
            if (r != null && r.hasMaintenanceIssue()) {
                r.displayInfo();//displays maintenance issue report if exists
                found = true;
            }
        }
        //prompt if no issue exists
        if (!found) {
            System.out.println("No maintenance issues reported.");
        }
    }
}

// updated Admin module (backend role)
// Admin calls engines/services:
class Admin extends User {

    //constructor
    public Admin(int id, String name, String email, String password) throws InvalidPasswordException {
        super(id, name, email, password);
    }

    // Admin generates clash-free timetable
    public void generateClashFreeTimetable(AcademicSystem sys) {
        SemesterCoordinator coord = new SemesterCoordinator(sys);
        coord.runSetup();
    }

    // Admin monitors faculty workload distribution
    public void monitorFacultyWorkload(AcademicSystem sys) {
        WorkloadService.reportAllFacultyWorkload(sys);
    }

    // Admin tracks room utilization
    public void trackRoomUtilization(AcademicSystem sys) {
        RoomUtilizationService.reportRoomUtilization(sys);
    }

    // Admin views maintenance issue reports
    public void viewMaintenanceReports(AcademicSystem sys) {
        MaintenanceService.showMaintenanceReports(sys);
    }

    // admin creates an exam day in the academic system
    public void CreateExamDay(AcademicSystem sys, String date) {
        sys.createExamDaySchedule(date); // create a day schedule for this date and add it to the system
    }

    //create and add exam paper to a day schedule
    public void CreateExamPaper(AcademicSystem sys, String courseName, String date) {
        ExamPaper paper = sys.createExamPaperForCourse(courseName); // create a paper for this course and automatically find students and classes for this paper
        if (paper != null) {
            boolean added = sys.addExamPaperToDaySchedule(paper, date); // add this paper to the day schedule of this date
            if (added) {
                System.out.println("Created exam paper for " + courseName + " and added to schedule for " + date);
            } else {
                System.out.println("Failed to add exam paper for " + courseName + " to schedule for " + date);
            }
        } else {
            System.out.println("Failed to create exam paper for " + courseName);
        }
    }

    // generate and store seating for one day
    public void generateExamSeatingPlanForDay(AcademicSystem sys, String date, Room[] rooms) {
        //null checks
        if (sys == null || date == null || rooms == null) {
            System.out.println("Exam seating failed: invalid input for day-wise generation.");
            return;
        }
        try {
            // generate seating for this day
            ExamDaySchedule daySchedule = sys.findExamDayScheduleByDate(date);
            if (daySchedule == null) {
                System.out.println("Exam seating failed: No exam schedule found for date: " + date);
                return;
            }
            ExamSeating seating = new ExamSeating(daySchedule, rooms); // create a new seating object for this day with the found schedule and given rooms
            seating.generateSeatingPlanForDay();
            // store the generated seating plan in the system
            sys.addGeneratedExamSeatingPlan(seating);
            System.out.println("Generated and stored exam seating for date: " + daySchedule.getDate());
        } catch (RoomCapacityException e) {
            System.out.println("Exam seating failed for " + date + ": " + e.getMessage());
        }
    }

    // display seating plan for a day
    public void displayExamSeatingPlanForDay(AcademicSystem sys, String date) {
        if (sys == null || date == null) {
            System.out.println("Display failed: invalid input for day-wise seating display.");
            return;
        }
        // find the generated seating plan for this day
        for (ExamSeating seating : sys.getGeneratedExamSeatingPlans()) {
            if (seating != null && seating.getExamDaySchedule().getDate().equals(date)) {
                seating.displaySeatingPlan(); // display the seating plan for this day if found
                return;
            }
        }
        System.out.println("No generated seating plan found for date: " + date);
    }

}//end of admin class

public class Acadcore {

    public static void main(String[] args) {
        AcadcoreWebApp.start(3456);
    }
}

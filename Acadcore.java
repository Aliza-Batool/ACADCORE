
package com.km.kmproject1;

/**
 *
 * @author abist
 */
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

//  EXCEPTIONS
//added invalid password exception
class InvalidPasswordException extends Exception { //inherits in built exception class
    public InvalidPasswordException(String message) {
        super(message);
    }
}
//added Course Capacity Exception
class CourseCapacityException extends Exception{
    public CourseCapacityException(String message){
        super(message);
    }
}
//added Schedule Conflict Exception
class ScheduleConflictException extends Exception{
    public ScheduleConflictException (String msg){
        super(msg);
    }
}
class RoomCapacityException extends Exception{
    public RoomCapacityException(String msg){
        super(msg);
    }
}

//added interface 
interface Displayable {
    void displayInfo();
}
abstract class User implements Displayable{ //made user class abstract as it should not be instantiated directly
    //attributes
    private int id;
    private String name;
    private String email;
    private String password;

    private static int totalUsers = 0;   // counter to track total users created

    //constructor
    public User(int id,String name,String email,String password)
    throws InvalidPasswordException { //added exception to constructor

        this.id=id;
        this.name=name;
        this.email=email;
        setPassword(password);   
        totalUsers++;
    }
    //methods
    //getters
   int getId(){
       return id;
   }

    String getName(){
        return name;
    }
   
   String getEmail(){
       return email;
   }
   //added static method to get total users
    public static int getTotalUsers() 
    { 
        return totalUsers;
     }

   //setters

   void setName(String name){
       this.name=name;
   }

   void setEmail(String email){
       this.email=email;
   }
   void setPassword(String password)throws InvalidPasswordException{
    if (password !=null && password.length()>=6) {
       this.password=password;
   }
    else{
        throw new InvalidPasswordException("Password must be at least 6 characters.");
        }
    }
   //login method
   boolean login(String inputEmail,String inputPassword){
           return (email.equals(inputEmail)&& password.equals(inputPassword));
   }
   @Override
   public void displayInfo(){
       System.out.println("ID: "+id+"\nNAME: "+name+"\nEmail: "+email);
   }
}


class Course implements Displayable{
    private int courseCode;
    private String courseName;
    private Faculty facultyAssigned; //changed to Faculty reference for better integration with faculty class
    private int creditHours;//added credithours for workload calculation
    
    //constructor  without faculty assigned
    public Course(int courseCode, String courseName,int creditHours){
        this.courseCode=courseCode;
        this.courseName=courseName;
        this.facultyAssigned=null; // initially no faculty assigned
        this.creditHours= creditHours;      
    }
    //constructor with default credit hours
    public Course(int courseCode, String courseName){
        //using "this" as the first line of the constructor
        this(courseCode, courseName, 3); // default 3 credit hours      
    }
    //getters
    public int getCourseCode(){ 
        return courseCode; 
    }
    public String getCourseName(){ 
        return courseName; 
    }
    public Faculty getFacultyAssigned(){  //changed return type to Faculty
        return facultyAssigned; 
    }
    public int getCreditHours(){ 
        return creditHours; 
    }

    //added method which is called when faculty gets assigned to this course(linked both)
    public void setAssignedFaculty(Faculty f) {
        this.facultyAssigned = f;
        System.out.println("Faculty " + f.getName()+ " linked to course: " + courseName);
    }

    public boolean hasFaculty() {
        return facultyAssigned != null;
    }  

    @Override
    //added more details to displayinfo method
    public void displayInfo() {
        System.out.println("Course [" + courseCode + "] " + courseName + "\nCredits: " + creditHours+ "\nFaculty: " + (hasFaculty() ? facultyAssigned.getName() : "Not assigned"));
    } 
}//end of class Course

class Attendance{
   // attributes
    private Course course;
    private int totalClasses;
    private int attendedClasses;
    private static final double riskThreshold =75.0;//attendance threshold per class (static final)

    //constructor
    public Attendance(Course c){
        course = c;
        totalClasses = 0;
        attendedClasses = 0;
    }
    //methods
    //getter
    public Course getCourse(){ 
        return course; 
    }
    //marking a class present
    public void markAttendance(Boolean present){
        totalClasses++;
        if (present){
            attendedClasses++;
        }
    }
    //attendance percentage
    double getAttendancePercentage(){
        if(totalClasses==0){
            return 100;
        }
        else{
            return ((attendedClasses * 100.0) / totalClasses);
        }
    }
    //attendance rist alert 
    public boolean isAtRisk(){
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
    private int    daysUntilDue;   
    private int    weightPercent;  
    //constructor
    public Assignment(String courseName, String title,int daysUntilDue, int weightPercent) {
        this.courseName    = courseName;
        this.title         = title;
        this.daysUntilDue  = daysUntilDue;
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
    double weightScore   = weightPercent / 100.0;         // ranges from 0 to 1
    return (weightScore * 0.6 + urgency * 0.4) * 100; // 60% to weightage and 40% to urgency
    }
    public void display() {
        System.out.printf("  [%s] %-25s due in %2d days  weight: %2d%%  priority: %.2f%n",courseName, title, daysUntilDue, weightPercent, getPriorityScore());
    }
}//end of class assignment

//study group finder class                                                                                                                   
class StudyGroup 
{                                                                                                              //ADDED
 
    private Course course;
    private Student[] members;   //array to store members
    private Student leader;    //group leader(creator of the group)
    private int memberCount;
    private int maxSize;    //leader decides max size of the group
    //constructor
    public StudyGroup(Course course, int maxSize, Student leader) {
        this.course= course;
        this.maxSize= maxSize;
        this.leader = leader;
        this.members = new Student[maxSize];
        this.members[0] = leader; //leader is the first member of the group
        this.memberCount = 1;   //initially only the leader is in the group
    }
 
    // add a student to the group
    //HELPER METHODS FOR VALIDATIONS
    public boolean isvalidMember(Student student){ 
         //to check if the student is of the same course before adding to the group
        for(Course c: student.getEnrolledCourses()){
            if(c != null && c.getCourseCode() == course.getCourseCode()){
                return true;
            }
        }
        return false;
    }
    //to check if the student is already in the group
    public boolean isMember(Student student){
        for(int i=0; i<memberCount; i++){
            if(members[i].getId() == student.getId()){
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
        }   

        //check if the group is already full
        else if (memberCount == maxSize) {
        System.out.println("Study group for " + course.getCourseName() + " is full.");
        return false;
        }

        //check if the student is already in the group
        else if (isMember(student)) {
            System.out.println("Student " + student.getName() + " is already in the study group for " + course.getCourseName() + ".");
            return false;
        }
        //if all validations are passed add the student to the group
        else {
            members[memberCount++] = student;   //stored and incremented the member count(post increment)
            System.out.println("Student " + student.getName() + " added to the study group for " + course.getCourseName() + ".");
            return true;
        }
        
    }
    //getters
    public Course getCourse(){
        return course;
    }
    public int getMemberCount(){
        return memberCount;
    }

    public Student getLeader(){
        return leader;
    }
    public int getMaxSize(){
        return maxSize;
    }
    //display the group details
    public void display() {
        System.out.println("Study Group – " + course.getCourseName() +" (" + memberCount + "/" + maxSize + " members)");
        for (int i = 0; i < memberCount; i++) {
            System.out.println("    " + (i + 1) + ". " + members[i].getName());
        }
    }
}   //end of class study group (without gpa comparison logic)                                                                                                                      
                                                                                                                                     //ADDED SOME ATTRIBUTES
class Student extends User{
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
    
    //constructor might throw invalid password exception
    public Student(int id,String name,String email,String password,AcadClass classOfStudent)throws InvalidPasswordException{
        super(id,name,email,password);
        this.classOfStudent=classOfStudent;
        classOfStudent.addStudent(this); //automatically adds the student to the class when a student object is created with a class reference
    }
    //getter for enrolled courses  
    public Course[] getEnrolledCourses(){                                                  
        //added getter to use in study group finder
        return enrolledCourses;
    }
    //added getter for major
    public String getMajor(){                                                  
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
    //setter for AcadClass used in remove student from class method
    public void setClass(AcadClass classOfStudent){
        this.classOfStudent=classOfStudent;
    }

    //methods
    //enroll course
    public void enrollCourse(Course c) throws CourseCapacityException {
        if (courseCount >= MAX_COURSES) {
            throw new CourseCapacityException(
                getName() + " cannot enroll in more than " + MAX_COURSES + " courses.");
        }//throws exception is courses exceed the limit
        enrolledCourses[courseCount]   = c;
        attendanceRecords[courseCount] = new Attendance(c);
        courseCount++;
        System.out.println(getName() + " enrolled in: " + c.getCourseName());
    }
    
    // marking attendance
    public void markAttendance(String name, boolean present) {
        //method 1
        for(int i=0;i<courseCount;i++){
            if (name.equalsIgnoreCase(enrolledCourses[i].getCourseName()) ){
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
    
    //assignment prioritizer
    public void showPrioritizedAssignments() {
        // copy valid entries into new array
        Assignment[] sorted = new Assignment[assignmentCount];
        for (int i = 0; i < assignmentCount; i++) 
            sorted[i] = assignments[i];

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
        for (int i = 0; i < assignmentCount; i++) 
            sorted[i].display();
    }
    
    //overall attendance display
    public void checkAttendanceRisks() {
        System.out.println("\n── Attendance Report for " + getName() + " ──");
        for (int i = 0; i < courseCount; i++) {
            attendanceRecords[i].displayAttendance();
        } //displays attendance for all courses
    }
    
    //detect overlap in timeslots
    public void detectClash(TimeSlot[] mySlots) {
        System.out.println("\n── Clash Detection for " + getName() + " ──");
        boolean found = false;
        for (int i = 0; i < mySlots.length; i++) {
            for (int j = i + 1; j < mySlots.length; j++) {
                if (mySlots[i] != null && mySlots[j] != null &&
                    mySlots[i].overlaps(mySlots[j])) {
                    //checks each slot with everyother slot of the time slot array after itself
                    //excludes checking itself and slots before it are already checked
                    //avoids unnecessary comparisons
                    System.out.println("  CLASH: Slot " + (i + 1) +
                                       " overlaps with Slot " + (j + 1));
                    found = true;
                }
            }
        }
        if (!found) System.out.println("  No clashes detected.");
    }
    
    //view personal time table from class schedule
    public void viewMyClassSchedule() {
        if (classOfStudent == null) {
            System.out.println("No class assigned to " + getName());
            return;
        }
        classOfStudent.displayClassSchedule();
    }
    //display info
    @Override
    public void displayInfo(){
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
    public Course getEnrolledCourse(int i){ 
        return enrolledCourses[i]; 
    }
    public Attendance getAttendanceRecord (int i){ 
        return attendanceRecords[i]; 
    }//returns particular attendance
   
}//end of class student
    

class Room implements Displayable{ 
    private int roomNumber;
    private int capacity;
    private String maintenanceNote; // added to report maintenance issues
    private TimeSlot[] bookedSlots = new TimeSlot[50]; // to keep track of booked slots for clash detection
    private int bookedCount = 0;
    //constructor
    public Room(int roomNumber, int capacity){
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
    int getRoomNumber(){
        return roomNumber;
    }

    int getCapacity(){
        return capacity;
    }
    String getMaintenanceNote(){
        return maintenanceNote;
    }
    //methods
    void reportMaintenanceIssue(String note){
        this.maintenanceNote = note;
        System.out.println("Maintenance issue reported for Room " + roomNumber + ": " + note);
    }
    public boolean hasMaintenanceIssue(){   //no issue if the note is empty
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
            if (bookedSlots[i] != null && bookedSlots[i].overlaps(slot))
                return false;
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
    public void displayInfo(){
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
    public TimeSlot(int dayChoice, int sh, int sm, int eh, int em){
        setDay(dayChoice);
        setStartTime(sh, sm);
        setEndTime(eh, em);
}
    //added getter setters and validations
    void setDay(int choice){
        String[] days = {"Monday","Tuesday","Wednesday","Thursday","Friday"};   //using array to store days
        if (choice >= 1 && choice <= 5) {
            day = days[choice - 1];
        } 
        else{
            System.out.println("Invalid number entered! day set to Monday.");
            day="Monday";
        }
    }
    String getDay(){
        return day;
    }    
    
    void setStartTime(int hour, int minute){
        startTime=LocalTime.of(hour, minute);
    }
    
    LocalTime getStartTime(){
        return startTime;
    }
    
    void setEndTime(int hour, int minute){
        endTime=LocalTime.of(hour, minute);
    }
    
    LocalTime getEndTime(){
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
        if (other == null) return false;
        if (day == null || other.day == null) return false;
        if (!day.equals(other.day)) return false;
        if (startTime == null || endTime == null || other.startTime == null || other.endTime == null) return false;
        return ( !other.startTime.isBefore(this.startTime) ) && ( !other.endTime.isAfter(this.endTime) );
    }


    //updated overlap method to use LocalTime's built in methods
   boolean overlaps(TimeSlot other){
    // basic null checks
    if(other == null || day == null || other.day == null) return false;
    // slots on different days do not overlap
    if(!day.equals(other.day)) return false;
    
    if(startTime == null || endTime == null || other.startTime == null || other.endTime == null) return false;

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
        if (semester < 1 || semester > 8)
            throw new IllegalArgumentException("Semester must be between 1 and 8.");
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
        if (c == null) return false;

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
                for (int j = i; j < courseCount - 1; j++)
                    courses[j] = courses[j + 1];// the element is overwritten by the next element and so on until the end of the array

                courses[--courseCount] = null;  // clear last slot as last element is now duplicated after shifting
                System.out.println("Course removed from curriculum.");
                return true;
            }
        }
        System.out.println("Course not found in curriculum.");
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



class AcadClass { //renamed to AcadClass to avoid conflict with built in Class class
    private static final int maxStudents = 50; // max students per class
    private int batchNo;
    private String major;
    private char section;
    private int semester;           // moved from Student
    private int studentCount;
    private Student[] students = new Student[maxStudents];
    private Faculty[] facultyAssigned=new Faculty[12];//max limit of assigned faculty members 
    private int facultyCount=0;
    private Curriculum curriculum; // added curriculum reference to link curriculum with class
    private static final int MAX_CLASS_SCHEDULE = 100;   //max limit of scheduled classes for a class
    private ScheduledClass[] classSchedule = new ScheduledClass[MAX_CLASS_SCHEDULE]; // ADDED
    private int classScheduleCount = 0;      // to keep track of number of scheduled classes
    private TimeSlot[] bookedClassSlots = new TimeSlot[MAX_CLASS_SCHEDULE]; // to keep track of booked time slots of this class
    private int bookedClassSlotCount = 0;  
    // Constructor
    public AcadClass(int batchNo, String major, char section, int semester) throws IllegalArgumentException {
        // Validate batch number (must be less than 1000)
        if (batchNo >= 1000 || batchNo < 0) {
            throw new IllegalArgumentException("Batch number must be less than 1000.");
        }
        //validate semester (must be between 1 and 8)
        if (semester < 1 || semester > 8)
            throw new IllegalArgumentException("Semester must be between 1 and 8.");
        
        this.batchNo = batchNo;
        this.major = major;
        this.section = section;
        this.semester = semester;
        this.studentCount = 0;
    }
    
    // Getters
    public int getBatchNo() {
        return batchNo;
    }
    
    public String getMajor() {
        return major;
    }
    
    public char getSection() {
        return section;
    }
    public int getSemester() {//added getter for semester
        return semester;
    }
    public int getStudentCount() {
        return studentCount;
    }
    
    public Student[] getStudents() {
        return students;
    }
    public Curriculum getCurriculum() {
        return curriculum;
    }
    public Faculty[] getFacultyAssigned() {
        return facultyAssigned;
    }
    public ScheduledClass[] getClassSchedule() {
        return classSchedule;
    }
    public int getClassScheduleCount() {
        return classScheduleCount;
    }

    //setter for curriculum
    public void setCurriculum(Curriculum curriculum) {
        //validate that the curriculum's major and semester match this class before linking
        if (curriculum != null && curriculum.getMajor().equalsIgnoreCase(major) && curriculum.getSemester() == semester) {
            this.curriculum = curriculum;
            System.out.println("Curriculum linked to class " + major + "-" + batchNo + section);
        } else {
            System.out.println("Failed to link curriculum: Major and semester must match the class.");
        }
    }
    public boolean hasCurriculum() {
        return curriculum != null; //helper method to check if curriculum is linked to the class
    }


    //method to assign faculty to the class
    public void assignFaculty(Faculty f) {
        if (facultyCount < facultyAssigned.length) {
            facultyAssigned[facultyCount++] = f;
            System.out.println(f.getName() + " assigned to class " + major + "-" + batchNo + section);
        } else {
            System.out.println("Faculty limit reached for this class.");
        }
    }


    // Add a student to the class
    // Only adds if the student's major matches this class's major
    public boolean addStudent(Student student) throws IllegalArgumentException {

        // Check if class is full
        if (studentCount >= maxStudents) {
            System.out.println("Cannot add student: Class is full.");
            return false;
        }

        // Check if student is already in the class
        for (int i = 0; i < studentCount; i++) {
            if (students[i].getId() == student.getId()) {
                System.out.println("Cannot add student: Student already exists in this class.");
                return false;
            }
        }

        // Check if student's major,semester and section matches this class's major
        if (!student.getMajor().equalsIgnoreCase(major) || student.getSemester() != semester || student.getSection() != section) {
            System.out.println("Cannot add student: Student's details do not match class requirements.");
            return false;
        }
    
        //if all conditions are met, Set the class for the student
        student.setClass(this);
        
        // Add student to the class
        students[studentCount++] = student;
        System.out.println("Student " + student.getName() + " added to Class " + batchNo + " " + major + " " + section);
        return true;
    }//end of add student method
    
    // Remove a student from the class
    public boolean removeStudent(int studentId) {
        for (int i = 0; i < studentCount; i++) {
            if (students[i].getId() == studentId) {
                // Clear the class reference from student
                students[i].setClass(null);
                
                // Shift students to fill the gap
                for (int j = i; j < studentCount - 1; j++) {
                    students[j] = students[j + 1];
                }
                //sets last empty space to null after shifting 
                students[--studentCount] = null;
                System.out.println("Student removed from class.");
                return true;
            }
        }
        //if student not found
        System.out.println("Student not found in this class.");
        return false;
    }//end of remove student method
    
    private boolean isWithinUniversityTiming(TimeSlot slot) {
        //check day is between Monday and Friday
        String d = slot.getDay();
        //if day is null return false to avoid null pointer exception in the next line
        if (d == null) return false;

        boolean validDay = d.equals("Monday") || d.equals("Tuesday") || d.equals("Wednesday")|| d.equals("Thursday") || d.equals("Friday");
        if (!validDay) return false;    //if day is not valid return false
        if (slot.getStartTime() == null || slot.getEndTime() == null) return false; //if start or end time is null return false to avoid null pointer exception in the next lines

        //setting university timing from 9 am to 5 pm
        LocalTime uniStart = LocalTime.of(9, 0);   // 09:00
        LocalTime uniEnd   = LocalTime.of(17, 0);  // 17:00

        // start >= 09:00, end <= 17:00, and end > start
        return !slot.getStartTime().isBefore(uniStart)&& !slot.getEndTime().isAfter(uniEnd)&& slot.getEndTime().isAfter(slot.getStartTime());
    }

    public boolean isAvailable(TimeSlot slot) {
        // enforce timing first
        if (!isWithinUniversityTiming(slot)) {
            System.out.println("Class " + major + "-" + batchNo + section+ " not available: outside university timings (09:00-17:00).");
            return false;
        }
    
        // capacity for bookings
        if (bookedClassSlotCount >= bookedClassSlots.length) {
            System.out.println("Class " + major + "-" + batchNo + section+ " has no remaining booking capacity.");
            return false;
        }

        // clash check(already booked for another class at this time)
        for (int i = 0; i < bookedClassSlotCount; i++) {
            if (bookedClassSlots[i] != null && bookedClassSlots[i].overlaps(slot)) {
                System.out.println("Class " + major + "-" + batchNo + section+ " not available: clashes with existing booking.");
                return false;
            }
        }
        // if all validations are passed then the class is available for booking at this slot
        return true;
    }//end of isAvailable method

    //book the slot for this class
    public boolean bookSlot(TimeSlot slot) {
        // check availability first
        if (!isAvailable(slot)) return false;
        // if available then store the booked slot 
        bookedClassSlots[bookedClassSlotCount++] = slot;
        return true;
    }//end of book slot method

    // store scheduled class entry
    public void addToClassSchedule(ScheduledClass sc) {
        if (sc == null) return; // null check
        // Check if schedule is full
        if (classScheduleCount >= classSchedule.length) {
            System.out.println("Class schedule full for " + major + "-" + batchNo + section);
            return;
        }
        // Store the scheduled class in the schedule array
        classSchedule[classScheduleCount++] = sc;
    }//end of add to class schedule method

    //display class schedule
    public void displayClassSchedule() {
        System.out.println("\n── Class Schedule: " + major + "-" + batchNo + section + " (Sem " + semester + ") ──");
        if (classScheduleCount == 0) {
            System.out.println("  No scheduled classes yet.");
            return;
        }
        for (int i = 0; i < classScheduleCount; i++) {
            ScheduledClass sc = classSchedule[i];
            System.out.printf("  %-25s  %s  %s-%s  Room %d  Teacher: %s%n",sc.getCourse().getCourseName(),sc.getSlot().getDay(),sc.getSlot().getStartString(),sc.getSlot().getEndString(),sc.getRoom().getRoomNumber(),sc.getTeacher().getName());
        }
    }//end of display class schedule method

    // Display class information
    public void displayClassInfo() {
        System.out.println("\n======== Class Information ========");
        System.out.println("Batch Number: " + batchNo);
        System.out.println("Major: " + major);
        System.out.println("Section: " + section);
        System.out.println("Total Students: " + studentCount + "/" + maxStudents);
        System.out.println("──────────────────────────────────");
        System.out.println("Students in Class:");
        
        if (studentCount == 0) {
            System.out.println("  No students enrolled in this class yet.");
            return;
        }
        
        for (int i = 0; i < studentCount; i++) {
            System.out.println("  " + (i + 1) + ". " + students[i].getName() + 
                " (ID: " + students[i].getId() + ")");
        }
    }
}//end of class Class


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
        System.out.println("Deadline set: [" + courseName + "] " + taskName + " → " + deadline);

    }//end of set deadline method

    //method to mark a deadline as done and remove it from the list
    public void markDone(String courseName, String taskName) {
        for (int i = 0; i < deadlineCount; i++) {
            if (gradingDeadlines[i].getCourse().getCourseName().equalsIgnoreCase(courseName) && gradingDeadlines[i].getTaskTitle().equalsIgnoreCase(taskName)) {
                // Shift left to fill the gap
                for (int j = i; j < deadlineCount - 1; j++)
                    gradingDeadlines[j] = gradingDeadlines[j + 1];

                gradingDeadlines[--deadlineCount] = null;  // clear last slot
                System.out.println("Marked done: [" + courseName + "] " + taskName);
                return;
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
            System.out.println("  [" + gradingDeadlines[i].getCourse().getCourseName() + "] "+ gradingDeadlines[i].getTaskTitle() + " → Due: "+ gradingDeadlines[i].getDeadline());
        }
    }//end of show pending deadlines method
}//end of class deadline manager

class Faculty extends User {
    private static final int MAX_COURSES = 6; //added max course limit for faculty
    private static final int MAX_SLOTS = 20; //added max slot limit for faculty

    private Course[] assignedCourses= new Course[MAX_COURSES];
    private TimeSlot[] availableSlots= new TimeSlot[MAX_SLOTS]; //assuming faculty can have different slots for different courses
    private TimeSlot[] bookedSlots = new TimeSlot[20]; //to keep track of booked slots 
    private ScheduledClass[] schedule = new ScheduledClass[MAX_SLOTS];  // for viewing schedule
    private int courseCount = 0;
    private int slotCount = 0;
    private int bookedCount = 0;
    private DeadlineManager deadlineManager; // replaces all deadline fields
    //constructor
    public Faculty(int id, String name, String email, String password) throws InvalidPasswordException{
        super(id,name,email,password);
        deadlineManager = new DeadlineManager(this,MAX_COURSES * 5); // assuming max 5 deadlines per course(at a time) and passing reference of faculty to deadline manager for validation
    }
    //added get schedule for use in admin module
    public ScheduledClass[] getSchedule() {
        return schedule;
    }

    // added method that calculates total credit hrs and returns it
    public int getTotalAssignedCreditHours() {
        int sum = 0;
        for (int i = 0; i < courseCount; i++) {
            if (assignedCourses[i] != null) sum += assignedCourses[i].getCreditHours();
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
    public void assignCourse(Course c)throws CourseCapacityException {
        if (courseCount >= MAX_COURSES) {
            throw new CourseCapacityException(getName() + " cannot be assigned more than " + MAX_COURSES + " courses.");
        }
        assignedCourses[courseCount++] = c;
        c.setAssignedFaculty(this); // course now knows its faculty
        System.out.println(getName() + " assigned to: " + c.getCourseName());
    }

    //add available time slot for faculty
    public void addAvailableSlot(TimeSlot slot){
        if(slotCount < MAX_SLOTS){
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
        if (availableSlots[i] != null && availableSlots[i].contains(slot)){
            withinAvailable = true; // the slot is within this available slot
            break;
        }
    }

    if (!withinAvailable){
        System.out.println(getName() + " is not available for slot: " + slot.getDay() + " " + slot.getStartString() + "-" + slot.getEndString());
        return false;  // no available slot covers this time
        }

    // 3:check if it overlaps with any already booked slot
    for (int i = 0; i < bookedCount; i++) {
        if (bookedSlots[i] != null && bookedSlots[i].overlaps(slot))
            return false;
        }
        
        return true;

    }//end of isAvailable method

    //book a slot for a class
    public boolean bookSlot(TimeSlot slot){
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

    //method to view teaching schedule of the faculty
    public void viewTeachingSchedule() {
    System.out.println("\n── Teaching Schedule for " + getName() + " ──");
    //if no classes are scheduled
    if (bookedCount == 0) {
        System.out.println("  No classes scheduled.");
        return;
    }
    System.out.printf("%-25s  %-10s  %-8s  %-8s  %-5s  %s%n","Course", "Day", "Start", "End", "Room", "Sec");
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
            if (assignedCourses[i].getCourseName().equalsIgnoreCase(courseName))
                return assignedCourses[i];
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

    //makeup request method
    public void requestMakeup(String courseCode, String reason) {
        System.out.println("Makeup request by " + getName() +" for " + courseCode + ": " + reason);
    }//end of makeup request method(made it basic,more implementation to be done in admin module for approval and tracking)

     @Override
    public void displayInfo(){
        super.displayInfo();
        System.out.println("Assigned Courses: "+courseCount);
   
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

    Course getCourse(){
        return course;
    }

    Faculty getTeacher(){
        return teacher;
    }

    Room getRoom(){
        return room;
    }

    TimeSlot getSlot(){
        return slot;
    }

    char getSection(){
        return section;
    }
    //added getter for acad class
    public AcadClass getAcadClass() {
        return acadClass;
    }
}

class Timetable implements Displayable{   //Stores all scheduled classes.
    //attributes
    private ScheduledClass[] schedule;
    private int totalEntries;
    private static final int maxEntries=100;
    
    //constructor
    public Timetable() {
        schedule = new ScheduledClass[maxEntries];
        totalEntries = 0;
    }
    
    //getters
    public ScheduledClass[] getSchedule(){ 
        return schedule; 
    }
    public int getTotalEntries(){
        return totalEntries; 
    }
    
    // add a class if no room or faculty clash
    public void addClass(ScheduledClass sc) throws ScheduleConflictException {

        //1. Check class availability 
        if (sc.getAcadClass() == null) {
            throw new ScheduleConflictException("ScheduledClass missing AcadClass reference.");
        }
        if (!sc.getAcadClass().isAvailable(sc.getSlot())) {
        throw new ScheduleConflictException("AcadClass unavailable/booked or outside 9-5: "+ sc.getAcadClass().getMajor() + "-" + sc.getAcadClass().getBatchNo() + sc.getAcadClass().getSection());
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
            "Course","Faculty","Day","Start","End","Room","Sec");//left aligned
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

//added exam seating class
//checks if space is enough for course exam conduction an
class ExamSeating{
    //attributes
    private Course course;
    private Room[] rooms;//array of rooms alloted for exam
    private Student[] students;//array of students giving exam per course
    private int studentCount;

    //constructor
    public ExamSeating(Course course, Room[] rooms,
                       Student[] students, int studentCount) {
        this.course = course;
        this.rooms = rooms;
        this.students = students;
        this.studentCount = studentCount;
    }

    //counts the space available for students, and gives exception if no. of students exceeds space also fills students according to the array provided
    public void generateSeatingPlan() throws RoomCapacityException {
        int totalCapacity = 0;
        for (Room r : rooms) {
            if (r != null) 
                totalCapacity += (r.getCapacity())/2;//in exams every alternate row is left empty thereby reducing room capacity to half
        }

        if (totalCapacity < studentCount) {
            throw new RoomCapacityException(
                "Not enough room capacity for exam: " + course.getCourseName());
        }
        //prints seating plan
        System.out.println("\n===== Exam Seating Plan – " + course.getCourseName() + " =====");
        int studentIndex = 0;
        for (int ri = 0; ri < rooms.length && studentIndex < studentCount; ri++) //stops if either students are completed or rooms are filled
        {
            if (rooms[ri] == null) continue;//skips the iteration for empty room
            System.out.println("Room " + rooms[ri].getRoomNumber() +
                               " (capacity " + (rooms[ri].getCapacity())/2 + "):");//prints room number and capacity
            int seat = 1;//starts from 1 for each room
            while (seat <= (rooms[ri].getCapacity())/2 && studentIndex < studentCount)//while the room capacity is not reached or students are not completely filled
            {
                System.out.printf("   Seat %2d – %s (ID: %d)%n",
                    seat, students[studentIndex].getMajor(),
                    students[studentIndex].getId());//needs batch number
                
                studentIndex++;//increments index to next student
                seat++; //increments seat to next seat
            }//end of while
        }//end of for
    }//end of method

}//end of class Exam Seating

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
    //adders check empty index and size or arrays
    public void addClass(AcadClass c) {
        if (c != null && classCount < classes.length) classes[classCount++] = c;
    }
    public void addCurriculum(Curriculum c) {
        if (c != null && curriculumCount < curricula.length) curricula[curriculumCount++] = c;
    }
    public void addFaculty(Faculty f) {
        if (f != null && facultyCount < faculty.length) faculty[facultyCount++] = f;
    }
    public void addRoom(Room r) {
        if (r != null && roomCount < rooms.length) rooms[roomCount++] = r;
    }
}//end of AcademicSystem

// ═══════════════════════════════════════════════
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
                    if (course == null) continue;//if no student present at the index then skip iteration

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
            if (room != null && room.isAvailable(slot)) return room;
        }
        return null;
    }

    //timetable generator
    public void generateTimetable() {
        System.out.println("\n   Generating clash-free timetable   ");
        //for all classes
        for (int i = 0; i < sys.classCount; i++) {
            AcadClass ac = sys.classes[i];
            if (!ac.hasCurriculum()) continue;
            //gets curriculum of class
            Curriculum cur = ac.getCurriculum();
            //for everycourse
            for (int c = 0; c < cur.getCourseCount(); c++) {
                Course course = cur.getCourses()[c];
                //if no course skip iteration
                if (course == null) continue;
                //if n o faculty assigned for a course then skip iteration
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
                    //checks for every slot whether the teacher and class are available simultaneously, if not then skips iteration
                    if (slot == null) continue;
                    // quick filters
                    if (!ac.isAvailable(slot)) continue;
                    if (!teacher.isAvailable(slot)) continue;
                    //checks for every room for an available room
                    Room room = findAvailableRoom(slot);
                    if (room == null) continue;
                    //when teacher, slot, students are available simultaneously, the class is scheduled
                    ScheduledClass sc = new ScheduledClass(course, teacher, room, slot, ac.getSection(), ac);

                    try {
                        sys.timetable.addClass(sc); // books AcadClass + Faculty + Room
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

// added WorkloadServicwAdmin uses it to monitor faculty workload distribution.
// Workload metrics (simple):

class WorkloadService {

    public static void reportFacultyWorkload (AcademicSystem sys) {
        System.out.println("\n================ FACULTY WORKLOAD REPORT ================");
        if (sys.facultyCount == 0) {
            System.out.println("No faculty registered.");
            return;
        }
        //prints faculty workload in formatted way
        System.out.printf("%-20s  %-10s  %-10s%n", "Faculty", "Credits", "Lectures");
        System.out.println("─".repeat(50));

        for (int i = 0; i < sys.facultyCount; i++) {
            Faculty f = sys.faculty[i];
            if (f == null) continue;

            System.out.printf("%-20s  %-10d  %-10d%n",
                    f.getName(),
                    f.getTotalAssignedCreditHours(),
                    f.getBookedCount());
        }
    }
}// end of WorkloadService

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
            if (r == null) continue;

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
        if (!found) System.out.println("No maintenance issues reported.");
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
        WorkloadService.reportFacultyWorkload(sys);
    }

    // Admin tracks room utilization
    public void trackRoomUtilization(AcademicSystem sys) {
        RoomUtilizationService.reportRoomUtilization(sys);
    }

    // Admin views maintenance issue reports
    public void viewMaintenanceReports(AcademicSystem sys) {
        MaintenanceService.showMaintenanceReports(sys);
    }

    // Admin generates exam seating plan (calls your algorithm)
    public void generateExamSeatingPlan(Course course, Room[] rooms, Student[] students, int studentCount) {
        try {
            ExamSeating seating = new ExamSeating(course, rooms, students, studentCount);
            seating.generateSeatingPlan();
        } catch (RoomCapacityException e) {
            System.out.println("Exam seating failed: " + e.getMessage());
        }
    }
}//end of admin class

public class Acadcore {

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}
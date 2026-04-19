
package com.km.kmproject1;

/**
 *
 * @author abist
 */
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

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
    private String facultyAssigned;
    private int creditHours;//added credithours for workload calculation
    
    //constructor 
    public Course(int courseCode, String courseName, String facultyAssigned,int creditHours){
        this.courseCode=courseCode;
        this.courseName=courseName;
        this.facultyAssigned=facultyAssigned;
        this.creditHours= creditHours;      
    }
    public Course(int courseCode, String courseName, String facultyAssigned)//constructor without credithours (Overloading concept)
    {
        //using "this" as the first line of the constructor
        this(courseCode, courseName, facultyAssigned, 3); // default 3 credit hours      
    }
    //getters
    public int getCourseCode(){ 
        return courseCode; 
    }
    public String getCourseName(){ 
        return courseName; 
    }
    public String getFacultyAssigned(){ 
        return facultyAssigned; 
    }
    public int getCreditHours(){ 
        return creditHours; 
    }
      
    @Override
    public void displayInfo() {
        System.out.println("Course [" + courseCode + "] " + courseName +
                           "\nFaculty: " + facultyAssigned +
                           "\nCredits: " + creditHours);
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
    private Course[] enrolledCourses = new Course[MAX_COURSES];   //max course limit is 10
    private Attendance[] attendanceRecords = new Attendance[MAX_COURSES];
    private Assignment[] assignments = new Assignment[50];//maximum limit of assignments 50
    private int courseCount = 0;
    private int assignmentCount = 0;
    private double gpa; //added gpa attribute for study group finder
    private double cgpa; //added cgpa attribute for study group finder
    private int semester; //added semester attribute for profile management
    
    //constructor might throw invalid password exception
    public Student(int id,String name,String email,String password)throws InvalidPasswordException{
        super(id,name,email,password);
    }
    //getter for enrolled courses  
    public Course[] getEnrolledCourses(){                                                  //added getter to use in study group finder
        return enrolledCourses;
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
            assignments[assignmentCount++] = a;//stores assignment on the first available index
            assignmentCount++;//increments no. of assignments
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
    
    //view personal time schedule
    public void viewTimetable(ScheduledClass[] schedule, int total) {
        System.out.println("\n── Timetable for " + getName() + " ──");
        for (int i = 0; i < total; i++) {
            if (schedule[i] != null) {
                ScheduledClass sc = schedule[i];//stores schedule class object for following methods
                //formated printig of time schedule
                System.out.printf("  %-25s  %s  %s-%s  Room %d%n",
                    sc.getCourse().getCourseName(),
                    sc.getSlot().getDay(),
                    sc.getSlot().getStartTime().getTime(),
                    sc.getSlot().getEndTime().getTime(),
                    sc.getRoom().getRoomNumber());
            }
        }
    }
    //display info
    @Override
    public void displayInfo(){
        super.displayInfo();
        System.out.println("Enrolled Courses: "+courseCount);
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
    //constructor
    public Room(int roomNumber, int capacity){
        this.roomNumber = roomNumber;
        this.capacity = capacity;
        this.maintenanceNote = ""; // default no maintenance issues
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
    @Override
    public void displayInfo(){
        System.out.println("Room " + roomNumber + " (Capacity: " + capacity + ")");
        if (hasMaintenanceIssue()) {
            System.out.println("  Maintenance Issue: " + getMaintenanceNote());
        }
    }

//class for time
class Time{
    private int hour;
    private int minute;
    
    //constructor 
    public Time(){
        hour=0;
        minute=0;
    }
    
    //constructor
    public Time(int hour, int minute){
        this.hour=hour;
        this.minute=minute;
    }
    
    //getter setters
    void setHour(int hour){
        if(hour<=23 && hour>=0) //corrected the condition to allow 0-23 hours
            this.hour=hour;
        else{
            System.out.println("Invalid value entered!\nHour set to 00.");
        }              
    }
    
    void setMinute(int minute){
        if(minute<60 && minute>=0) //corrected the condition to allow 0-59 minutes
            this.minute=minute;
        else{
            System.out.println("Invalid value entered!\nMinute set to 00.");
        } 
    }
    String getTime(){
        return String.format("%02d:%02d", hour,minute);
    }

    int toMinutes(){
        return (hour * 60) + minute;
    }
}//end of class time

class TimeSlot {
    private String day;
    private Time startTime;
    private Time endTime;
    
    // constructor 
    public TimeSlot(int dayChoice, int sh, int sm, int eh, int em){
        setDay(dayChoice);
        setStartTime(sh, sm);
        setEndTime(eh, em);
}
    //added getter setters and validations
    void setDay(int choice){
        String[] days = {"Monday","Tuesday","Wednesday","Thursday","Friday"};   //using array to store days
        System.out.println("Enter\n1 for Monday\n2 for Tuesday\n3 for Wednesday\n4 for Thursday\n5 for Friday");
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
        startTime=new Time(hour, minute);
    }
    
    Time getStartTime(){
        return startTime;
    }
    
    void setEndTime(int hour, int minute){
        endTime=new Time(hour, minute);
    }
    
    Time getEndTime(){
        return endTime;
    }

    boolean overlaps(TimeSlot other){
        //checking null pointer exception and day mismatch before time comparison
        if(other == null || !day.equals(other.day) || startTime == null || endTime == null || other.startTime == null || other.endTime == null){
            return false;
        }
        int thisStart = startTime.toMinutes();
        int thisEnd = endTime.toMinutes();
        int otherStart = other.startTime.toMinutes();
        int otherEnd = other.endTime.toMinutes();
        
        // Check if the other meeting happens entirely AFTER this one ends
        boolean startsAfterThisEnds = otherStart >= thisEnd;

        // Check if the other meeting happens entirely BEFORE this one starts
        boolean endsBeforeThisStarts = otherEnd <= thisStart;

        // If BOTH of these are false, then they MUST overlap
        return !(startsAfterThisEnds || endsBeforeThisStarts);
    }
}//end of class timeslot

class Batch {
    private String className;
    private int studentCount;
    private Course[] courses;
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

class Faculty extends User {
    private static final int MAX_COURSES = 6; //added max course limit for faculty
    private static final int MAX_SLOTS = 20; //added max slot limit for faculty

    private Course[] assignedCourses= new Course[MAX_COURSES];
    private TimeSlot[] availableSlots= new TimeSlot[MAX_SLOTS]; //assuming faculty can have different slots for different courses
    private TimeSlot[] bookedSlots = new TimeSlot[20]; //to keep track of booked slots 
    private int courseCount = 0;
    private int slotCount = 0;
    private int bookedCount = 0;
    private GradingDeadline[] gradingDeadlines = new GradingDeadline[MAX_COURSES*5]; // 5 tasks per course max
    private int deadlineCount = 0;
    //constructor
    public Faculty(int id, String name, String email, String password) throws InvalidPasswordException{
        super(id,name,email,password);
    }
    //methods

    //assign course to faculty
    public void assignCourse(Course c)throws CourseCapacityException {
        if (courseCount >= MAX_COURSES) {
            throw new CourseCapacityException(
                getName() + " cannot be assigned more than " + MAX_COURSES + " courses.");
        }
        assignedCourses[courseCount++] = c;
        System.out.println(getName() + " assigned to: " + c.getCourseName());
    }
    //add available time slot for faculty
    public void addAvailableSlot(TimeSlot slot){
        if(slotCount < MAX_SLOTS){
            availableSlots[slotCount++] = slot;
            System.out.println("Added available slot for " + getName() + ": " + slot.getDay() + " " + slot.getStartTime().getTime() + "-" + slot.getEndTime().getTime());
        }
    }

    //check if faculty is available for a given timeslot
   public boolean isAvailable(TimeSlot slot) {
    // first check if the slot is within their available slots
    boolean withinAvailable = false;
    for (int i = 0; i < slotCount; i++) {
        if (availableSlots[i] != null && availableSlots[i].overlaps(slot)){
            withinAvailable = true; // overlap with available slot means they are available for this time
            break;
        }
    }
    if (!withinAvailable){
        System.out.println(getName() + " is not available for slot: " + slot.getDay() + " " + slot.getStartTime().getTime() + "-" + slot.getEndTime().getTime());
        return false;  // no available slot covers this time
        }
    // check if it overlaps with any already booked slot
    for (int i = 0; i < bookedCount; i++) {
        if (bookedSlots[i] != null && bookedSlots[i].overlaps(slot))
            return false;
        }
        return true;
    }

    //book a slot for a class
    public void bookSlot(TimeSlot slot){
        //check if the slot is available before booking
        if(!isAvailable(slot)){
            System.out.println("Booking failed: slot unavailable or already booked for " + getName());
            return;
        }
        //if all slots are booked then it cannot be booked anymore
        if (bookedCount >= bookedSlots.length) {
            System.out.println("Booking failed: " + getName() + " has no remaining booking capacity.");
            return;
        }
        //if available and booking capacity is not full then book the slot
        bookedSlots[bookedCount++] = slot;
        System.out.println("Booking successful: " + getName() + " booked for " + slot.getDay() + " " + slot.getStartTime().getTime() + "-" + slot.getEndTime().getTime());
    }//end of book slot method

     //deadline reminder for grading
    public void setGradingDeadline(String courseName, String taskName, String deadline) {
        // Check if the faculty is even assigned to this course
        boolean courseFound = false;
        Course course = null;
        for (int i = 0; i < courseCount; i++) {
            if (assignedCourses[i].getCourseName().equalsIgnoreCase(courseName)) {
            courseFound = true;
            course = assignedCourses[i];    //to find course from course name given by user
            break;
        }
    }
    if (!courseFound) {
        System.out.println("Failed: " + getName() + " is not assigned to course '" + courseName + "'.");
        return;
    }
    // Check for duplicate (same course + same task)
    for (int i = 0; i < deadlineCount; i++) {
        if (gradingDeadlines[i].getCourse().getCourseName().equalsIgnoreCase(courseName) && gradingDeadlines[i].getTaskTitle().equalsIgnoreCase(taskName)) {
            System.out.println("Failed: Deadline Reminder for '" + taskName + "' in '" + courseName + "' already exists.");
            return;
        }
    }
    // Check capacity
    if (deadlineCount >= gradingDeadlines.length) {
        System.out.println("Failed: No more deadline slots available.");
        return;
    }
    //if all validations are passed then store the deadline reminder
    gradingDeadlines[deadlineCount++] = new GradingDeadline(course, taskName, deadline);
    System.out.println("Deadline set: [" + courseName + "] " + taskName + " → " + deadline);
    }//end of set grading deadline method
    //method to mark deadline as done and remove it from the list
    public void markDeadlineDone(String courseName, String taskName) {
    for (int i = 0; i < deadlineCount; i++) {
        //finding the deadline reminder based on course name and task name given by user
        if (gradingDeadlines[i].getCourse().getCourseName().equalsIgnoreCase(courseName) && gradingDeadlines[i].getTaskTitle().equalsIgnoreCase(taskName)) {

            // Shift left to fill the gap
            for (int j = i; j < deadlineCount - 1; j++)
                gradingDeadlines[j] = gradingDeadlines[j + 1];

            gradingDeadlines[--deadlineCount] = null; // clear last slot
            System.out.println("Marked done and removed: [" + courseName + "] " + taskName);
            return;
        }
    }
    System.out.println("Failed: No deadline found for '" + taskName + "' in '" + courseName + "'.");
}
    public void getPendingDeadlines() {
    if (deadlineCount == 0) {
        System.out.println("No pending deadlines for " + getName() + ".");
        return;
    }
    System.out.println("Pending Deadlines for " + getName() + ":");
    for (int i = 0; i < deadlineCount; i++) {
        System.out.println("  [" + gradingDeadlines[i].getCourse().getCourseName() + "] "+ gradingDeadlines[i].getTaskTitle() + " → Due: "+ gradingDeadlines[i].getDeadline());
    }
}
}//end of class faculty(not complete)        
class ScheduledClass {  //to store output of timetable generator.
    private Course course;
    private Faculty teacher;
    private Room room;
    private TimeSlot slot;
    private char section;

    public ScheduledClass(Course course, Faculty teacher, Room room, TimeSlot slot, char section){
        this.course = course;
        this.teacher = teacher;
        this.room = room;
        this.slot = slot;
        this.section = section;
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
}

class Timetable {   //Stores all scheduled classes.

    private ScheduledClass[] schedule;
    private int totalEntries;

}

//added basic admin module
class Admin extends User {
    public Admin(int id, String name, String email, String password){
        super(id,name,email,password);
    }
}
public class Acadcore {

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}
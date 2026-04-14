
package com.km.kmproject1;

/**
 *
 * @author abist
 */
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
//updtaed user class
class User {
    protected int id;
    protected String name;
    protected String email;
    protected String password;
    protected List<Notification> notifications;

    public User(int id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        setPassword(password);
        this.notifications = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        if (password != null && password.length() >= 6) {
            this.password = password;
        } else {
            System.out.println("Error: Password must be at least 6 characters.");
        }
    }

    public boolean login(String inputEmail, String inputPassword) {
        return (email.equals(inputEmail) && password.equals(inputPassword));
    }

    public void displayInfo() {
        System.out.println("ID: " + id + "\nNAME: " + name + "\nEmail: " + email);
    }

    public void addNotification(Notification notification) {
        notifications.add(notification);
    }

    public List<Notification> getNotifications() {
        return notifications;
    }
}

//updated class course
class Course {
    private int courseCode;
    private String courseName;
    private String facultyAssigned;
    private int creditHours;
    private boolean isLabCourse;
    private int maxStudents;

    public Course(int courseCode, String courseName, String facultyAssigned, 
                  int creditHours, boolean isLabCourse, int maxStudents) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.facultyAssigned = facultyAssigned;
        this.creditHours = creditHours;
        this.isLabCourse = isLabCourse;
        this.maxStudents = maxStudents;
    }

    public int getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getFacultyAssigned() {
        return facultyAssigned;
    }

    public int getCreditHours() {
        return creditHours;
    }

    public boolean isLabCourse() {
        return isLabCourse;
    }

    public int getMaxStudents() {
        return maxStudents;
    }
}//end of class course 

class Attendance{
   // attributes
        private Course course;
        private int totalClasses;
        private int attendedClasses;

        public Attendance(Course c){
        course = c;
        totalClasses = 0;
        attendedClasses = 0;
                }
    //methods
       double getAttendancePercentage(){
           if(totalClasses==0){
               return 100;
           }
           else{
            return ((attendedClasses * 100.0) / totalClasses);
        }
      }
   }

//mltiple classes used in this code are yet to be made 
class Student extends User {
    private List<Course> enrolledCourses;
    private Map<Integer, Attendance> attendanceRecords;
    private List<StudentCourseEnrollment> courseEnrollments;
    private StudentTimetable studentTimetable;
    private int semester;

    public Student(int id, String name, String email, String password, int semester) {
        super(id, name, email, password);
        this.enrolledCourses = new ArrayList<>();
        this.attendanceRecords = new HashMap<>();
        this.courseEnrollments = new ArrayList<>();
        this.semester = semester;
    }

    public void enrollCourse(Course course) {
        enrolledCourses.add(course);
        attendanceRecords.put(course.getCourseCode(), new Attendance(course));
    }

    public List<Course> getEnrolledCourses() {
        return enrolledCourses;
    }

    public Attendance getAttendance(int courseCode) {
        return attendanceRecords.get(courseCode);
    }

    public List<StudentCourseEnrollment> getCourseEnrollments() {
        return courseEnrollments;
    }

    public void addCourseEnrollment(StudentCourseEnrollment enrollment) {
        courseEnrollments.add(enrollment);
    }

    public StudentTimetable getStudentTimetable() {
        return studentTimetable;
    }

    public void setStudentTimetable(StudentTimetable timetable) {
        this.studentTimetable = timetable;
    }

    public int getSemester() {
        return semester;
    }
}//end of student class

class Room {
    private int roomNumber;
    private int capacity;

    public Room(int roomNumber, int capacity){
        this.roomNumber = roomNumber;
        this.capacity = capacity;
    }

    int getRoomNumber(){
        return roomNumber;
    }

    int getCapacity(){
        return capacity;
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
        if(hour<=24 && hour>0)
            this.hour=hour;
        else{
            System.out.println("Invalid value entered!\nHour set to 00.");
        }              
    }
    
    void setMinute(int minute){
        if(minute<=60 && minute>0)
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

}

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
        //System.out.println("Enter\n1 for Monday\n2 for Tuesday\n3 for Wednesday\n4 for Thursday\n5 for Friday");
        if(choice<6 && choice>0){
            switch (choice){
                case 1:
                    day="Monday";
                    break;
                case 2:
                    day="Tuesday";
                    break;
                case 3:
                    day="Wednesday";
                    break;
                case 4:
                    day="Thursday";
                    break;
                case 5:
                    day="Friday";
                    break;
            }            
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
        //checking null pointer exception
        if(other == null){
            return false;
        }
         //Checking for the same day 
        if(!day.equals(other.day)){
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
}

class Batch {
    private String className;
    private int studentCount;
    private Course[] courses;
}

class Faculty extends User {
    private Course[] assignedCourses;
    private TimeSlot[] availableSlots;

    public Faculty(int id, String name, String email, String password) {
        super(id,name,email,password);
    }
}

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

//
class StudentCourseEnrollment {
    private int enrollmentId;
    private Student student;
    private Course course;
    private int semester;
    private boolean isRetake;
    private Time enrollmentDate;
    private ScheduledClass assignedClass;

    public StudentCourseEnrollment(int enrollmentId, Student student, Course course, 
                                    int semester, boolean isRetake) {
        this.enrollmentId = enrollmentId;
        this.student = student;
        this.course = course;
        this.semester = semester;
        this.isRetake = isRetake;
        //this.enrollmentDate = Time.now();
    }

    public int getEnrollmentId() {
        return enrollmentId;
    }

    public Student getStudent() {
        return student;
    }

    public Course getCourse() {
        return course;
    }

    public int getSemester() {
        return semester;
    }

    public boolean isRetake() {
        return isRetake;
    }

    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }

    public ScheduledClass getAssignedClass() {
        return assignedClass;
    }

    public void setAssignedClass(ScheduledClass scheduledClass) {
        this.assignedClass = scheduledClass;
    }
}//end of class StudentCourseEnrollment

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
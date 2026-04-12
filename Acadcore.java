
package com.km.kmproject1;

/**
 *
 * @author abist
 */
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

class User{
    //attributes
    private int id;
    private String name;
    private String email;
    private String password;
    //constructor
    public User(int id,String name,String email,String password){
        this.id=id;
        this.name=name;
        this.email=email;
        setPassword(password);   
    }
    //methods
   int getId(){
       return id;
   }

    String getName(){
        return name;
    }
   void setName(String name){
       this.name=name;
   }
   String getEmail(){
       return email;
   }
   void setEmail(String email){
       this.email=email;
   }
   void setPassword(String password){
    if (password !=null && password.length()>=6) {
       this.password=password;
   }
    else{
        System.out.println("Error: Password must be at least 6 characters.");    }
    }
   boolean login(String inputEmail,String inputPassword){
           return (email.equals(inputEmail)&& password.equals(inputPassword));
   }
   void displayInfo(){
       System.out.println("ID: "+id+"\nNAME: "+name+"\nEmail: "+email);
   }
}


class Course{
    private int courseCode;
    private String courseName;
    private String facultyAssigned;
    //constructor 
    public Course(int courseCode,String courseName,String facultyAssigned){
        this.courseCode=courseCode;
        this.courseName=courseName;
        this.facultyAssigned=facultyAssigned;
    }
    int getCourseCode(){
       return courseCode;
   }
      String getCourseName(){
       return courseName;
   }
      String getFacultyAssigned(){
       return facultyAssigned;
}
}
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
class Student extends User{
          //attributes
        private Course[] enrolledCourses = new Course[5];   //max course limit is 5
        private Attendance[] attendanceRecords = new Attendance[5];
        public Student(int id,String name,String email,String password){
            super(id,name,email,password);
        }
        
      }

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

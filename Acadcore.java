/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.km.acadcore;

/**
 *
 * @author abist
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
        this.password=password;
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

public class Acadcore {

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}

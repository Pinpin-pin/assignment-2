import java.util.List;

public class App {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
        DBMS database = new DBMS();

        Student student1 = new Student("S12345", "John Doe", 85, 90);
        Student student2 = new Student("S2", "Olivia Martinez", 75, 22);
        Student student3 = new Student("S3", "Ethan Kim", 65, 33);
        Student student4 = new Student("S4", "Sophia Patel", 15, 55);
        Student student5 = new Student("S5", "Jackson Brown", 0, 99);
        Student student6 = new Student("S6", "Mia Nguyen", 30, 68);
        database.insertStudent(student1);
        database.insertStudent(student2);
        database.insertStudent(student3);
        database.insertStudent(student4);
        database.insertStudent(student5);
        database.insertStudent(student6);

        Student queryResult1 = database.queryByStudentNumber("S12345");
        List<Student> queryResult2 = database.queryByScore(175);
        List<Student> queryResult3 = database.queryByScore(87);
        List<Student> queryResult4 = database.queryByScore(98);

        System.out.println(queryResult1);
        System.out.println(queryResult2);
        System.out.println(queryResult3);
        System.out.println(queryResult4);
    }
}

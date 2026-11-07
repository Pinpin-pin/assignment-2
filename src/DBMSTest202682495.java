import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class DBMSTest202682495 {

    @Test
    public void alwaysPass() {
        assertEquals(true, true);
    }

    @Test
    public void insertAndQuery1() {
        DBMSInterface dbms = new DBMS();
        dbms.insertStudent(new Student("201388888", "Patrick Yonge", 50, 33));
        Student student = dbms.queryByStudentNumber("201388888");
        assertEquals("201388888", student.getStudentNumber());
        assertEquals("Patrick Yonge", student.getName());
        assertEquals(83, student.getOverallScore());
    }

    @Test
    public void insertAndQueryStudent() {
        DBMSInterface dbms = new DBMS();
        dbms.insertStudent(new Student("201300001", "John Doe", 45, 55));
        Student student = dbms.queryByStudentNumber("201300001");
        assertEquals("201300001", student.getStudentNumber());
        assertEquals("John Doe", student.getName());
        assertEquals(100, student.getOverallScore());
    }

    @Test
    public void queryNonExistingStudent() {
        DBMSInterface dbms = new DBMS();
        Student student = dbms.queryByStudentNumber("201300002");
        assertNull(student);
    }

    @Test
    public void insertMultipleAndQueryByScore() {
        DBMSInterface dbms = new DBMS();
        dbms.insertStudent(new Student("201300003", "Alice Johnson", 40, 60));
        dbms.insertStudent(new Student("201300004", "Bob Smith", 50, 50));
        dbms.insertStudent(new Student("201300005", "Charlie Brown", 30, 70));

        List<Student> students = dbms.queryByScore(100);
        assertEquals(3, students.size());
        assertTrue(students.contains(new Student("201300003", "Alice Johnson", 40, 60)));
        assertTrue(students.contains(new Student("201300004", "Bob Smith", 50, 50)));
    }

    @Test
    public void deleteStudent() {
        DBMSInterface dbms = new DBMS();
        Student student = new Student("201300006", "David Green", 60, 40);
        dbms.insertStudent(student);
        dbms.deleteStudent(student);
        assertNull(dbms.queryByStudentNumber("201300006"));
    }

    @Test
    public void updateStudentScores() {
        DBMSInterface dbms = new DBMS();
        Student student = new Student("201300007", "Eva Blue", 25, 75);
        dbms.insertStudent(student);

        student.setMidtermScore(35);
        student.setFinalScore(65);
        dbms.insertStudent(student);

        Student queriedStudent = dbms.queryByStudentNumber("201300007");
        assertEquals(35, queriedStudent.getMidtermScore());
        assertEquals(65, queriedStudent.getFinalScore());
        assertEquals(100, queriedStudent.getOverallScore());
    }

    @Test
    public void queryBySpecificScore() {
        DBMSInterface dbms = new DBMS();
        dbms.insertStudent(new Student("201300008", "Fiona White", 50, 50));
        List<Student> students = dbms.queryByScore(100);
        assertEquals(1, students.size());
        assertEquals("201300008", students.get(0).getStudentNumber());
    }

    @Test
    public void insertAndQueryStudentWithSpecialChars() {
        DBMSInterface dbms = new DBMS();
        dbms.insertStudent(new Student("201300013", "Jenny O'Reilly", 50, 50));
        Student student = dbms.queryByStudentNumber("201300013");
        assertEquals("201300013", student.getStudentNumber());
        assertEquals("Jenny O'Reilly", student.getName());
        assertEquals(100, student.getOverallScore());
    }

    @Test
    public void hashStudentNumberRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> DBMS.hashStudentNumber(null));
    }

    @Test
    public void insertRejectsNullStudent() {
        DBMSInterface dbms = new DBMS();
        assertThrows(IllegalArgumentException.class, () -> dbms.insertStudent(null));
    }

    @Test
    public void insertRejectsNullStudentNumber() {
        DBMSInterface dbms = new DBMS();
        Student student = new Student(null, "No Number", 10, 20);
        assertThrows(IllegalArgumentException.class, () -> dbms.insertStudent(student));
    }

    @Test
    public void queryByNullStudentNumberReturnsNull() {
        DBMSInterface dbms = new DBMS();
        dbms.insertStudent(new Student("201300020", "Grace Hall", 40, 40));
        assertNull(dbms.queryByStudentNumber(null));
    }

    @Test
    public void queryByScoreReturnsEmptyListWhenNothingMatches() {
        DBMSInterface dbms = new DBMS();
        dbms.insertStudent(new Student("201300021", "Henry Ford", 10, 10));
        List<Student> students = dbms.queryByScore(199);
        assertNotNull(students);
        assertTrue(students.isEmpty());
    }

    @Test
    public void deleteNullStudentIsIgnored() {
        DBMSInterface dbms = new DBMS();
        dbms.insertStudent(new Student("201300022", "Ivy Chen", 30, 30));
        dbms.deleteStudent(null);
        assertNotNull(dbms.queryByStudentNumber("201300022"));
    }

    @Test
    public void reinsertingSameNumberReplacesTheOldRecord() {
        DBMSInterface dbms = new DBMS();
        dbms.insertStudent(new Student("201300024", "Old Name", 20, 20));
        Student replacement = new Student("201300024", "New Name", 70, 30);
        dbms.insertStudent(replacement);

        assertSame(replacement, dbms.queryByStudentNumber("201300024"));
        assertTrue(dbms.queryByScore(40).isEmpty());
        assertEquals(1, dbms.queryByScore(100).size());
    }

    @Test
    public void deleteRemovesFromScoreIndexWithoutTouchingOthers() {
        DBMSInterface dbms = new DBMS();
        Student kept = new Student("201300025", "Kept Student", 50, 50);
        Student removed = new Student("201300026", "Removed Student", 60, 40);
        dbms.insertStudent(kept);
        dbms.insertStudent(removed);
        dbms.deleteStudent(removed);

        List<Student> remaining = dbms.queryByScore(100);
        assertEquals(1, remaining.size());
        assertTrue(remaining.contains(kept));
        assertNull(dbms.queryByStudentNumber("201300026"));
    }

}

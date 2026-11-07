import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A simple Database Management System for student records.
 *
 * Student records are held in two skip list indices, one keyed by a hash of
 * the student number and one keyed by a hash of the overall score, giving
 * expected O(log n) insertion, search and deletion. A hash is only a fast
 * filter, so every lookup re-checks the real field value before returning.
 */
public class DBMS implements DBMSInterface {

    private static final int SCORE_MIX = 0x45d9f3b;
    private static final int STRING_HASH_BASE = 31;

    private final SkipList<Entry> studentNumberIndex = new SkipList<>();
    private final SkipList<Student> scoreIndex = new SkipList<>();

    /**
     * Computes a hash code for a student number.
     *
     * @param studentNumber the student number to hash
     * @return the hash code of the given student number
     * @throws IllegalArgumentException if {@code studentNumber} is {@code null}
     * @requires {@code studentNumber != null}
     * @ensures equal student numbers hash equally, and the position of each
     *          character affects the result, so rearrangements such as
     *          {@code "S12"} and {@code "S21"} do not collide
     */
    public static int hashStudentNumber(String studentNumber) {
        if (studentNumber == null) {
            throw new IllegalArgumentException("studentNumber must not be null");
        }
        int hash = 0;
        for (int i = 0; i < studentNumber.length(); i++) {
            hash = hash * STRING_HASH_BASE + studentNumber.charAt(i);
        }
        return hash;
    }

    /**
     * Computes a hash code for an overall score.
     *
     * @param score the overall score (midterm + final) to hash
     * @return the hash code of the given score
     * @requires nothing; every {@code int} is accepted
     * @ensures changing one input bit flips roughly half the output bits;
     *          every step is invertible, so the function is injective and two
     *          distinct scores never collide
     */
    public static int hashScore(int score) {
        int hash = score;
        hash = hash ^ (hash >>> 16);
        hash = hash * SCORE_MIX;
        hash = hash ^ (hash >>> 16);
        return hash;
    }

    /**
     * Inserts a student into the database.
     *
     * @param student the student to insert
     * @throws IllegalArgumentException if {@code student} is {@code null}, or if
     *                                  its student number is {@code null}
     * @requires {@code student != null} and
     *           {@code student.getStudentNumber() != null}
     * @ensures the student is retrievable by its student number and its
     *          overall score; any record previously stored under the same
     *          student number is removed from both indices first
     */
    @Override
    public void insertStudent(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("student must not be null");
        }
        String studentNumber = student.getStudentNumber();
        if (studentNumber == null) {
            throw new IllegalArgumentException("student number must not be null");
        }
        int numberKey = hashStudentNumber(studentNumber);
        removeEntry(numberKey, findEntry(numberKey, studentNumber));
        int score = student.getOverallScore();
        studentNumberIndex.insert(numberKey, new Entry(student, score));
        scoreIndex.insert(hashScore(score), student);
    }

    /**
     * Finds the student stored under the given student number.
     *
     * @param studentNumber the student number to look up; may be {@code null}
     * @return the matching student, or {@code null} if no such student is
     *         stored or {@code studentNumber} is {@code null}
     * @requires nothing; a {@code null} argument is accepted
     * @ensures the database is unchanged, and the result is the stored
     *          student whose number equals {@code studentNumber}, if any
     */
    @Override
    public Student queryByStudentNumber(String studentNumber) {
        if (studentNumber == null) {
            return null;
        }
        Entry entry = findEntry(hashStudentNumber(studentNumber), studentNumber);
        return entry == null ? null : entry.student;
    }

    /**
     * Finds every student whose overall score equals the given score.
     *
     * @param score the overall score (midterm + final) to look up
     * @return a new list of the matching students, empty if there are none;
     *         never {@code null}
     * @requires nothing; every {@code int} is accepted
     * @ensures the database is unchanged, and the result is a fresh list
     *          holding exactly the stored students whose overall score
     *          equals {@code score}, in no particular order
     */
    @Override
    public List<Student> queryByScore(int score) {
        List<Student> matches = new ArrayList<>();
        for (Student candidate : scoreIndex.search(hashScore(score))) {
            if (candidate != null && candidate.getOverallScore() == score) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    /**
     * Removes the given student from the database.
     *
     * @param student the student to remove; may be {@code null}
     * @requires nothing; a {@code null} argument is accepted
     * @ensures no record remains under the student's number in either index;
     *          does nothing if the student is {@code null}, carries a
     *          {@code null} student number, or is not stored; other students
     *          are never affected
     */
    @Override
    public void deleteStudent(Student student) {
        if (student == null) {
            return;
        }
        String studentNumber = student.getStudentNumber();
        if (studentNumber == null) {
            return;
        }
        int numberKey = hashStudentNumber(studentNumber);
        removeEntry(numberKey, findEntry(numberKey, studentNumber));
    }

    private Entry findEntry(int numberKey, String studentNumber) {
        for (Entry candidate : studentNumberIndex.search(numberKey)) {
            if (candidate != null && studentNumber.equals(candidate.student.getStudentNumber())) {
                return candidate;
            }
        }
        return null;
    }

    private void removeEntry(int numberKey, Entry entry) {
        if (entry == null) {
            return;
        }
        studentNumberIndex.remove(numberKey, entry);
        scoreIndex.remove(hashScore(entry.indexedScore), entry.student);
    }

    private static final class Entry {

        private final Student student;
        private final int indexedScore;

        private Entry(Student student, int indexedScore) {
            this.student = student;
            this.indexedScore = indexedScore;
        }
    }

    private static final class SkipList<V> implements StorageBackendInterface<Integer, V> {

        private static final int MAX_LEVEL = 16;
        private static final double PROMOTION_PROBABILITY = 0.5;

        private final Node<V> head = new Node<>(Integer.MIN_VALUE, MAX_LEVEL);
        private final Random random = new Random();
        private int level = 1;

        private static final class Node<V> {

            private final int key;
            private final List<V> values = new ArrayList<>();
            private final Node<V>[] forward;

            @SuppressWarnings("unchecked")
            private Node(int key, int level) {
                this.key = key;
                this.forward = (Node<V>[]) new Node[level];
            }
        }

        @Override
        public void insert(Integer key, V item) {
            if (key == null || item == null) {
                return;
            }
            int target = key;
            Node<V>[] update = newNodeArray();
            Node<V> current = descendTo(target, update);
            Node<V> next = current.forward[0];
            if (next != null && next.key == target) {
                next.values.add(item);
                return;
            }
            int height = randomLevel();
            if (height > level) {
                for (int i = level; i < height; i++) {
                    update[i] = head;
                }
                level = height;
            }
            Node<V> inserted = new Node<>(target, height);
            inserted.values.add(item);
            for (int i = 0; i < height; i++) {
                inserted.forward[i] = update[i].forward[i];
                update[i].forward[i] = inserted;
            }
        }

        @Override
        public List<V> search(Integer key) {
            if (key == null) {
                return new ArrayList<>();
            }
            Node<V> found = findNode(key);
            return found == null ? new ArrayList<>() : new ArrayList<>(found.values);
        }

        @Override
        public void delete(Integer key) {
            if (key == null) {
                return;
            }
            int target = key;
            Node<V>[] update = newNodeArray();
            Node<V> current = descendTo(target, update);
            Node<V> victim = current.forward[0];
            if (victim == null || victim.key != target) {
                return;
            }
            for (int i = 0; i < level; i++) {
                if (update[i].forward[i] == victim) {
                    update[i].forward[i] = victim.forward[i];
                }
            }
            while (level > 1 && head.forward[level - 1] == null) {
                level--;
            }
        }

        public void remove(Integer key, V item) {
            if (key == null || item == null) {
                return;
            }
            Node<V> found = findNode(key);
            if (found == null) {
                return;
            }
            for (int i = 0; i < found.values.size(); i++) {
                if (found.values.get(i) == item) {
                    found.values.remove(i);
                    break;
                }
            }
            if (found.values.isEmpty()) {
                delete(key);
            }
        }

        private Node<V> descendTo(int target, Node<V>[] update) {
            Node<V> current = head;
            for (int i = level - 1; i >= 0; i--) {
                while (current.forward[i] != null && current.forward[i].key < target) {
                    current = current.forward[i];
                }
                update[i] = current;
            }
            return current;
        }

        private Node<V> findNode(int target) {
            Node<V> current = head;
            for (int i = level - 1; i >= 0; i--) {
                while (current.forward[i] != null && current.forward[i].key < target) {
                    current = current.forward[i];
                }
            }
            Node<V> candidate = current.forward[0];
            return candidate != null && candidate.key == target ? candidate : null;
        }

        private int randomLevel() {
            int height = 1;
            while (height < MAX_LEVEL && random.nextDouble() < PROMOTION_PROBABILITY) {
                height++;
            }
            return height;
        }

        @SuppressWarnings("unchecked")
        private Node<V>[] newNodeArray() {
            return (Node<V>[]) new Node[MAX_LEVEL];
        }
    }
}

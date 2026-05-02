import java.util.*;

/**
 * Student Result Batch Processing System
 * Author: Vaishnavi Srivastava
 *
 * Simulates a batch processing pipeline:
 * - Loads student records (raw marks)
 * - Processes each record: calculates total, percentage, grade, pass/fail
 * - Generates summary report: pass rate, grade distribution, toppers, department stats
 *
 * Concepts demonstrated: OOP, Batch Processing, JDBC-style data access,
 * Collections, Generics, Encapsulation, SDLC
 */
public class StudentResultProcessor {

    // ─── Grade Scale ──────────────────────────────────────────────────────────
    static String calculateGrade(double pct) {
        if (pct >= 90) return "O";
        if (pct >= 75) return "A+";
        if (pct >= 60) return "A";
        if (pct >= 50) return "B";
        if (pct >= 40) return "C";
        return "F";
    }

    // ─── Model: Student (input) ───────────────────────────────────────────────
    static class Student {
        String rollNo, name, department;
        int semester;
        int[] marks; // marks for 5 subjects

        Student(String rollNo, String name, String dept, int sem, int... marks) {
            this.rollNo = rollNo;
            this.name = name;
            this.department = dept;
            this.semester = sem;
            this.marks = marks;
        }
    }

    // ─── Model: StudentResult (output after processing) ───────────────────────
    static class StudentResult {
        String rollNo, name, department;
        int semester, totalMarks;
        double percentage;
        String grade, status;
        int rankInDept;

        StudentResult(Student s, int total, double pct, String grade, String status) {
            this.rollNo = s.rollNo;
            this.name = s.name;
            this.department = s.department;
            this.semester = s.semester;
            this.totalMarks = total;
            this.percentage = pct;
            this.grade = grade;
            this.status = status;
        }

        @Override
        public String toString() {
            return String.format("%-10s %-20s %-6s %3d/500  %6.2f%%  Grade:%-3s  %-4s  Rank#%d",
                rollNo, name, department, totalMarks, percentage, grade, status, rankInDept);
        }
    }

    // ─── Batch ItemProcessor: Student → StudentResult ─────────────────────────
    static StudentResult process(Student s) {
        int total = 0;
        boolean anyFail = false;
        for (int m : s.marks) {
            total += m;
            if (m < 40) anyFail = true;
        }
        double pct = Math.round((total / 500.0) * 10000.0) / 100.0;
        String grade = calculateGrade(pct);
        String status = (anyFail || pct < 40) ? "FAIL" : "PASS";
        return new StudentResult(s, total, pct, grade, status);
    }

    // ─── Batch Engine: processes all records in chunks ────────────────────────
    static List<StudentResult> runBatch(List<Student> students, int chunkSize) {
        List<StudentResult> results = new ArrayList<>();
        System.out.println("\n🚀 Batch Job Started | Total records: " + students.size()
                         + " | Chunk size: " + chunkSize);

        for (int i = 0; i < students.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, students.size());
            List<Student> chunk = students.subList(i, end);

            System.out.println("\n  [Chunk " + (i / chunkSize + 1) + "] Processing records "
                             + (i + 1) + " to " + end + "...");

            // ItemReader → ItemProcessor → ItemWriter (per chunk)
            for (Student s : chunk) {
                StudentResult r = process(s);    // ItemProcessor
                results.add(r);                  // ItemWriter (in-memory)
                System.out.printf("    ✔ %s | %s | %.2f%% | %s | %s%n",
                    s.rollNo, s.name, r.percentage, r.grade, r.status);
            }
            System.out.println("  ✅ Chunk committed (" + chunk.size() + " records)");
        }

        // Post-processing: compute department ranks (simulates JDBC UPDATE)
        computeDepartmentRanks(results);

        System.out.println("\n✅ Batch Job Complete | Processed: " + results.size() + " records");
        return results;
    }

    // ─── Simulates JDBC-based rank computation ────────────────────────────────
    static void computeDepartmentRanks(List<StudentResult> results) {
        // Group by department
        Map<String, List<StudentResult>> byDept = new LinkedHashMap<>();
        for (StudentResult r : results) {
            byDept.computeIfAbsent(r.department, k -> new ArrayList<>()).add(r);
        }
        // Sort each department by percentage desc → assign rank
        for (List<StudentResult> group : byDept.values()) {
            group.sort((a, b) -> Double.compare(b.percentage, a.percentage));
            for (int i = 0; i < group.size(); i++) group.get(i).rankInDept = i + 1;
        }
    }

    // ─── Summary Report ───────────────────────────────────────────────────────
    static void printReport(List<StudentResult> results) {
        long passed = results.stream().filter(r -> r.status.equals("PASS")).count();
        long failed = results.size() - passed;

        System.out.println("\n" + "=".repeat(70));
        System.out.println("                    📊 RESULT SUMMARY REPORT");
        System.out.println("=".repeat(70));
        System.out.printf("  Total Students : %d%n", results.size());
        System.out.printf("  Passed         : %d%n", passed);
        System.out.printf("  Failed         : %d%n", failed);
        System.out.printf("  Pass Rate      : %.1f%%%n", (passed * 100.0 / results.size()));

        // Grade distribution
        System.out.println("\n  Grade Distribution:");
        Map<String, Long> gradeDist = new LinkedHashMap<>();
        for (String g : new String[]{"O", "A+", "A", "B", "C", "F"}) gradeDist.put(g, 0L);
        for (StudentResult r : results)
            gradeDist.put(r.grade, gradeDist.getOrDefault(r.grade, 0L) + 1);
        gradeDist.forEach((g, c) -> System.out.printf("    %-3s → %d students%n", g, c));

        // Department stats
        System.out.println("\n  Department Statistics:");
        Map<String, List<StudentResult>> byDept = new LinkedHashMap<>();
        for (StudentResult r : results)
            byDept.computeIfAbsent(r.department, k -> new ArrayList<>()).add(r);
        byDept.forEach((dept, list) -> {
            double avg = list.stream().mapToDouble(r -> r.percentage).average().orElse(0);
            System.out.printf("    %-6s → %d students | Avg: %.2f%%%n", dept, list.size(), avg);
        });

        // Top 3
        System.out.println("\n  🏆 Top 3 Students:");
        results.stream()
               .sorted((a, b) -> Double.compare(b.percentage, a.percentage))
               .limit(3)
               .forEach(r -> System.out.printf("    %d. %s (%s) — %.2f%% [%s]%n",
                   results.indexOf(r) + 1, r.name, r.department, r.percentage, r.grade));

        // Full result table
        System.out.println("\n" + "-".repeat(70));
        System.out.println("  Full Results (sorted by percentage):");
        System.out.println("-".repeat(70));
        results.stream()
               .sorted((a, b) -> Double.compare(b.percentage, a.percentage))
               .forEach(r -> System.out.println("  " + r));
        System.out.println("=".repeat(70));
    }

    // ─── Main ─────────────────────────────────────────────────────────────────
    public static void main(String[] args) {

        // Sample student data (simulates DB read / CSV input)
        List<Student> students = Arrays.asList(
            new Student("CSE001", "Aryan Sharma",    "CSE",  6, 88, 92, 76, 85, 90),
            new Student("CSE002", "Priya Gupta",     "CSE",  6, 45, 52, 38, 60, 55),
            new Student("CSE003", "Rohan Mehta",     "CSE",  6, 95, 98, 92, 96, 94),
            new Student("IT001",  "Sneha Verma",     "IT",   6, 72, 68, 75, 80, 70),
            new Student("IT002",  "Karan Patel",     "IT",   6, 30, 45, 28, 50, 40),
            new Student("AIML01", "Neha Singh",      "AIML", 6, 85, 90, 88, 82, 87),
            new Student("AIML02", "Vikram Joshi",    "AIML", 6, 60, 58, 65, 70, 62),
            new Student("CSE004", "Ananya Rao",      "CSE",  6, 50, 55, 48, 60, 52),
            new Student("IT003",  "Dev Malhotra",    "IT",   6, 92, 88, 95, 90, 91),
            new Student("AIML03", "Pooja Nair",      "AIML", 6, 40, 42, 38, 45, 44)
        );

        // Run batch job with chunk size 3
        List<StudentResult> results = runBatch(students, 3);

        // Print summary report
        printReport(results);
    }
}

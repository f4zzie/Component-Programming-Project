package ke.ku.sco310.qr.test;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal, dependency-free test harness used by the verification suite.
 *
 * <p>Keeping the harness in-house has a component-oriented justification: JUnit would add a
 * fourth-party dependency to a project whose purpose is to show how components are assembled and
 * checked, and the assertions needed here are a handful of comparisons. Every check records the
 * observed value, so the produced report is evidence rather than a pass/fail flag.</p>
 */
public final class TestHarness {

    /** A body of work that may fail. */
    public interface CheckedBody {
        void run() throws Exception;
    }

    /** Outcome of one check. */
    public static final class Result {
        public final String group;
        public final String name;
        public final boolean passed;
        public final String detail;
        public final long millis;

        Result(String group, String name, boolean passed, String detail, long millis) {
            this.group = group;
            this.name = name;
            this.passed = passed;
            this.detail = detail;
            this.millis = millis;
        }
    }

    private final List<Result> results = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();

    /**
     * Records the outcome of a boolean check.
     *
     * @param group   test group, e.g. {@code engine}
     * @param name    check name
     * @param passed  observed outcome
     * @param detail  observed values, printed in the report
     */
    public void check(String group, String name, boolean passed, String detail) {
        results.add(new Result(group, name, passed, detail, 0));
    }

    /** Records an equality check between two values. */
    public void checkEquals(String group, String name, Object expected, Object actual) {
        boolean passed = expected == null ? actual == null : expected.equals(actual);
        results.add(new Result(group, name, passed,
                "expected=" + expected + " actual=" + actual, 0));
    }

    /** Records a check that two values differ. */
    public void checkNotEquals(String group, String name, Object unexpected, Object actual) {
        boolean passed = unexpected == null ? actual != null : !unexpected.equals(actual);
        results.add(new Result(group, name, passed,
                "unexpected=" + unexpected + " actual=" + actual, 0));
    }

    /** Runs a body and records success, or records the thrown exception as a failure. */
    public void run(String group, String name, CheckedBody body) {
        long start = System.nanoTime();
        try {
            body.run();
            long millis = Math.round((System.nanoTime() - start) / 1_000_000.0);
            results.add(new Result(group, name, true, "completed", millis));
        } catch (Exception e) {
            long millis = Math.round((System.nanoTime() - start) / 1_000_000.0);
            results.add(new Result(group, name, false, e.getClass().getSimpleName() + ": " + e.getMessage(), millis));
        }
    }

    /** Runs a body that must fail; records a failure when nothing was thrown. */
    public void expectFailure(String group, String name, CheckedBody body) {
        long start = System.nanoTime();
        try {
            body.run();
            long millis = Math.round((System.nanoTime() - start) / 1_000_000.0);
            results.add(new Result(group, name, false, "expected an exception but the call succeeded", millis));
        } catch (Exception e) {
            long millis = Math.round((System.nanoTime() - start) / 1_000_000.0);
            results.add(new Result(group, name, true,
                    "rejected as expected: " + e.getClass().getSimpleName() + ": " + e.getMessage(), millis));
        }
    }

    /** Adds a free-text note (for example a measured value) to the report. */
    public void note(String text) {
        notes.add(text);
    }

    public List<Result> getResults() {
        return new ArrayList<>(results);
    }

    public int getTotal() {
        return results.size();
    }

    public int getFailures() {
        int failures = 0;
        for (Result result : results) {
            if (!result.passed) {
                failures++;
            }
        }
        return failures;
    }

    /** @return the transcript printed on the console and stored with the evidence. */
    public String toTextReport() {
        StringBuilder sb = new StringBuilder();
        String currentGroup = null;
        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            if (!result.group.equals(currentGroup)) {
                currentGroup = result.group;
                sb.append("\n[").append(currentGroup).append("]\n");
            }
            sb.append(String.format("  %-4s %-52s %s%n",
                    result.passed ? "PASS" : "FAIL",
                    result.name.length() > 52 ? result.name.substring(0, 49) + "..." : result.name,
                    result.detail == null ? "" : result.detail));
        }
        sb.append('\n');
        for (String note : notes) {
            sb.append("note: ").append(note).append('\n');
        }
        sb.append(String.format("%nTOTAL %d checks, %d passed, %d failed%n",
                getTotal(), getTotal() - getFailures(), getFailures()));
        return sb.toString();
    }

    /** @return the results as CSV rows for the evidence folder. */
    public String toCsv() {
        StringBuilder sb = new StringBuilder("group,check,outcome,detail,millis\n");
        for (Result result : results) {
            sb.append(csv(result.group)).append(',')
              .append(csv(result.name)).append(',')
              .append(result.passed ? "pass" : "fail").append(',')
              .append(csv(result.detail)).append(',')
              .append(result.millis).append('\n');
        }
        return sb.toString();
    }

    /** @return the results as a Markdown table for the report. */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder("| Group | Check | Outcome | Detail |\n|---|---|---|---|\n");
        for (Result result : results) {
            sb.append("| ").append(result.group)
              .append(" | ").append(result.name)
              .append(" | ").append(result.passed ? "PASS" : "**FAIL**")
              .append(" | `").append(result.detail == null ? "" : result.detail.replace("|", "\\|"))
              .append("` |\n");
        }
        return sb.toString();
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")
                ? "\"" + escaped + "\"" : escaped;
    }
}

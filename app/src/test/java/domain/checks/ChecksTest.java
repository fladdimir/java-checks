package domain.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import domain.checks.CheckTree.Check;
import domain.checks.CheckTree.CheckLeaf;
import domain.checks.CheckTree.CheckStrategies;

class ChecksTest {

    // leaf checks:
    static final Check<String> STRING_NOT_NULL = new CheckLeaf<String>(
            "NOT_NULL",
            s -> s != null,
            s -> "argument is null");

    static final Check<String> STRING_NOT_EMPTY = new CheckLeaf<>(
            "STRING_NOT_EMPTY",
            s -> !s.isEmpty(),
            s -> "argument is empty");

    static final Check<String> STRING_IF_DIGIT_THEN_EVEN = new CheckLeaf<>(
            "STRING_IF_DIGIT_THEN_EVEN",
            s -> s.chars().filter(Character::isDigit).map(c -> c - '0').allMatch(i -> (i & 1) == 0),
            s -> s + " contains non-even digits");

    static final Check<String> STRING_CONTAINS_ONLY_DIGITS = new CheckLeaf<>(
            "STRING_CONTAINS_ONLY_DIGITS",
            s -> s.chars().allMatch(Character::isDigit),
            s -> s + " is not digit-only");

    // compound checks:
    Check<String> digitChecks = new CheckTree.CompoundCheck<>(
            "digit checks",
            CheckStrategies.ACCUMULATE,
            List.of(
                    STRING_IF_DIGIT_THEN_EVEN,
                    STRING_CONTAINS_ONLY_DIGITS));

    Check<String> nonNullChecks = new CheckTree.CompoundCheck<>(
            "non-null checks",
            CheckStrategies.ACCUMULATE,
            List.of(
                    STRING_NOT_EMPTY,
                    digitChecks));

    Check<String> myCheck = new CheckTree.CompoundCheck<>(
            "root",
            CheckStrategies.FAIL_FAST,
            List.of(
                    STRING_NOT_NULL,
                    nonNullChecks));

    // tests:

    @Test
    void test_ok() {

        var r1 = myCheck.apply("24");

        assertEquals(r1.isEmpty(), true);
    }

    @Test
    void test_null() {

        var result = myCheck.apply(null);

        assertThat(result.get().message()).isEqualTo("""
                NOT_NULL:
                  argument is null""");

    }

    @Test
    void test_empty() {

        var result = myCheck.apply("");

        assertThat(result.get().message()).isEqualTo("""
                non-null checks:
                  STRING_NOT_EMPTY:
                    argument is empty""");
    }

    @Test
    void test_2failures() {

        var result = myCheck.apply("A3");

        assertThat(result.get().message()).isEqualTo("""
                non-null checks:
                  digit checks:
                    STRING_IF_DIGIT_THEN_EVEN:
                      A3 contains non-even digits
                    STRING_CONTAINS_ONLY_DIGITS:
                      A3 is not digit-only""");
    }

    @Test
    void test_2x2failures() {
        var myCheck = new CheckTree.CompoundCheck<>(
                "root",
                CheckStrategies.ACCUMULATE,
                List.of(
                        digitChecks,
                        digitChecks));

        var result = myCheck.apply("A3");

        assertThat(result.get().message()).isEqualTo("""
                digit checks:
                  STRING_IF_DIGIT_THEN_EVEN:
                    A3 contains non-even digits
                  STRING_CONTAINS_ONLY_DIGITS:
                    A3 is not digit-only
                digit checks:
                  STRING_IF_DIGIT_THEN_EVEN:
                    A3 contains non-even digits
                  STRING_CONTAINS_ONLY_DIGITS:
                    A3 is not digit-only""");
    }
}

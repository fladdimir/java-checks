package domain.checks;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import domain.checks.CheckTree.CheckStrategies.CheckStrategy;
import lombok.Data;

class CheckTree {

    static record CheckInfo(String name, String message) {
    }

    @FunctionalInterface
    static interface Check<CHECKED> {
        Optional<CheckInfo> apply(CHECKED checked);
    }

    @Data
    static class CheckLeaf<CHECKED> implements Check<CHECKED> {

        private final String name;
        private final Function<CHECKED, Boolean> check;
        private final Function<CHECKED, String> messageProvider;

        @Override
        public Optional<CheckInfo> apply(CHECKED checked) {
            return check.apply(checked) ? Optional.empty()
                    : Optional.of(new CheckInfo(name, messageProvider.apply(checked)));
        }
    }

    @Data
    static class CompoundCheck<CHECKED> implements Check<CHECKED> {

        private final String name;
        private final CheckStrategy checkStrategy;
        private final List<Check<CHECKED>> checks;

        @Override
        public Optional<CheckInfo> apply(CHECKED checked) {
            var failedChecks = checkStrategy.apply(executeChecks(checks, checked));
            return failedChecks.isEmpty() ? Optional.empty()
                    : Optional.of(aggregate(failedChecks));
        }

        static <CHECKED> Stream<CheckInfo> executeChecks(Collection<Check<CHECKED>> checks, CHECKED checked) {
            return checks.stream().map(c -> c.apply(checked)).filter(Optional::isPresent).map(Optional::get);
        }

        private CheckInfo aggregate(List<CheckInfo> failedChecks) {
            return new CheckInfo(name,
                    failedChecks.stream()
                            .map(c -> c.name + ":\n" + indent(c.message()))
                            .collect(joining("\n")));
        }

        private String indent(String s) {
            return s.lines().map(l -> "  " + l).collect(joining("\n"));
        }
    }

    static class CheckStrategies {

        @FunctionalInterface
        static interface CheckStrategy extends Function<Stream<CheckInfo>, List<CheckInfo>> {
        }

        static final CheckStrategy FAIL_FAST = stream -> stream.findFirst().map(List::of).orElse(emptyList());

        static final CheckStrategy ACCUMULATE = Stream<CheckInfo>::toList;
    }
}

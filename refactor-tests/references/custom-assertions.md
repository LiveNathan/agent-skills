# Custom AssertJ Assertions

Create domain-specific assertions for cleaner, more expressive test code.

## When to Create Custom Assertions

Create a custom assertion when you find tests repeatedly checking the same
combination of properties on a domain object, or when `assertTrue`/`assertFalse`
on domain predicates produces unhelpful failure messages.

## Template

```java
public class OrderAssert extends AbstractAssert<OrderAssert, Order> {

    // 1. Constructor
    public OrderAssert(Order actual) {
        super(actual, OrderAssert.class);
    }

    // 2. Static entry point
    public static OrderAssert assertThat(Order actual) {
        return new OrderAssert(actual);
    }

    // 3. Domain assertion methods
    public OrderAssert isPending() {
        isNotNull();
        if (actual.getStatus() != Status.PENDING) {
            failWithMessage("Expected order to be PENDING but was <%s>",
                actual.getStatus());
        }
        return this;
    }

    public OrderAssert hasTotalGreaterThan(BigDecimal minimum) {
        isNotNull();
        if (actual.getTotal().compareTo(minimum) <= 0) {
            failWithMessage(
                "Expected order total <%s> to be greater than <%s>",
                actual.getTotal(), minimum);
        }
        return this;
    }
}
```

## Pattern Matching with Sealed Types (Java 17+)

For sealed interfaces like `RichResult<T>`, custom assertions can leverage pattern matching for cleaner type narrowing:

```java
public class ResultAssert<T> extends AbstractAssert<ResultAssert<T>, RichResult<T>> {

    public ResultAssert(RichResult<T> actual) {
        super(actual, ResultAssert.class);
    }

    public static <T> ResultAssert<T> assertThat(RichResult<T> actual) {
        return new ResultAssert<>(actual);
    }

    public ResultAssert<T> isSuccess() {
        isNotNull();
        if (actual instanceof RichResult.Failure<T>(String errorMessage)) {
            failWithMessage("Expected success but found failure: %s", errorMessage);
        }
        return this;
    }

    // Return child assertions for fluent chaining
    public StringAssert failureMessage() {
        isFailure();
        if (actual instanceof RichResult.Failure<T>(String errorMessage)) {
            return new StringAssert(errorMessage);
        }
        throw new IllegalStateException("Not a failure");
    }
}
```

## Using Conditions for Reusable Predicates

For event-based assertions where you need to filter and assert within a collection:

```java
class EventsAssertion {
    private final List<GameEvent> actualEvents;

    public EventsAssertion hasExactly(Class<?> clazz, int expectedCount) {
        Condition<Object> condition = new Condition<>(
            event -> event.getClass() == clazz,
            "GameEvent is " + clazz.getSimpleName());
        int actualCount = (int) actualEvents.stream()
            .filter(condition::matches)
            .count();
        if (actualCount != expectedCount) {
            throw Failures.instance().failure(info,
                EventsShouldHaveExactly.eventsShouldHaveExactly(
                    actualEvents, expectedCount, actualCount, condition));
        }
        return this;
    }
}
```

## AssertJ Assertions Generator

For projects with many domain classes, consider using the AssertJ Assertions Generator to auto-generate custom assertion classes from your domain model. It generates assertion methods based on class properties (getters).

Maven plugin: `assertj-assertions-generator-maven-plugin` Gradle plugin: available via community plugins

Generated assertions can be enriched with custom methods and committed to source control.

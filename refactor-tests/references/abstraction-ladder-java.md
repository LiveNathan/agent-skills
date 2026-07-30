# Test Abstraction Ladder (Java)

Progressively higher levels of abstraction for test setup and assertions.
Not every test needs to climb to the top. Choose the lowest level that
keeps the test readable.

## Level 1: Inline Everything

The simplest approach. All setup, execution, and assertion in the test method.
Best for tests with minimal setup.

```java
@Test
void newEnsembleIsNotCanceled() {
    Ensemble ensemble = new Ensemble(ZonedDateTime.now());
    assertThat(ensemble.isCanceled())
        .as("A newly created ensemble should not be canceled")
        .isFalse();
}
```

## Level 2: Named Variables for Clarity

When constructor parameters are ambiguous, extract to named local variables.

```java
@Test
void ensembleStartsAtSpecifiedTime() {
    ZonedDateTime ensembleStartDateTime = ZonedDateTime.now();
    Ensemble ensemble = new Ensemble(ensembleStartDateTime);
    assertThat(ensemble.startDateTime())
        .as("Ensemble should start at the specified time")
        .isEqualTo(ensembleStartDateTime);
}
```

## Level 3: Static Factory Methods

When 2-3 tests create similar objects. Extract a method in the test class. Use domain language in the method name. Mark irrelevant parameters explicitly.

```java
private static Hand createHand(Rank... ranks) {
    Suit IRRELEVANT_SUIT = Suit.CLUBS;
    List<Card> cards = Arrays.stream(ranks)
        .map(rank -> new Card(IRRELEVANT_SUIT, rank))
        .toList();
    return new Hand(cards);
}
```

## Level 4: Shared Factory Class

When factory methods are needed across multiple test classes, move them to a `*Factory` or `*TestFactory` class.

```java
public class HandFactory {
    private static final Suit IRRELEVANT_SUIT = Suit.CLUBS;

    public static Hand createHand(Rank... ranks) { ... }
    public static Hand createBlackjack() { ... }
}
```

## Level 5: Test Builder

When 4+ variations exist and different tests need different subsets of properties. Builders provide reasonable defaults and override only what matters.

Builders live in test code. The Discord exchange between Nathan and JitterTed clarifies the progression:

- Start with static factory methods (overloaded as needed)
- Move to a builder when you have 4+ variations that require reasonable defaults
- Factory methods can call the builder internally; later inline if desired

```java
public class EnsembleBuilder {
    private String name = "Default Ensemble";
    private ZonedDateTime startTime = ZonedDateTime.now();
    private MemberId acceptedMember = null;

    public static EnsembleBuilder anEnsemble() {
        return new EnsembleBuilder();
    }

    public EnsembleBuilder named(String name) {
        this.name = name;
        return this;
    }

    public EnsembleBuilder startingAt(ZonedDateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    public EnsembleBuilder withAcceptedMember(MemberId member) {
        this.acceptedMember = member;
        return this;
    }

    public Ensemble build() {
        Ensemble ensemble = new Ensemble(name, startTime);
        if (acceptedMember != null) {
            ensemble.acceptMember(acceptedMember);
        }
        return ensemble;
    }
}
```

## Level 6: Customizer Pattern

When object construction involves complex graphs with child objects. Instead of builders-within-builders, pass a `Function<Customizer, Customizer>` to a factory method. The factory handles the full construction graph.

This pattern excels in event sourcing scenarios where creating a parent entity also requires creating related child events.

```java
public class MakeEvents {
    private final List<ConcertEvent> events = new ArrayList<>();

    public static MakeEvents with() {
        return new MakeEvents();
    }

    // Simple version: all defaults
    public MakeEvents concertScheduled(ConcertId concertId) {
        events.add(createConcertScheduled(concertId, 42,
            "Don't Care Artist Name", LocalDateTime.now()));
        return this;
    }

    // Customizer version: caller tweaks only what matters
    public MakeEvents concertScheduled(ConcertId concertId,
            Function<ConcertCustomizer, ConcertCustomizer> customizer) {
        ConcertCustomizer c = customizer.apply(new ConcertCustomizer());
        events.add(createConcertScheduled(concertId,
            c.ticketPrice, c.artistName, c.showDateTime));
        // Factory also handles child events based on customizer state
        c.ticketsSoldQuantity.stream()
            .map(qty -> new TicketsSold(concertId, nextSeq(), qty, qty * c.ticketPrice))
            .forEach(events::add);
        if (c.isTicketSalesStopped()) {
            events.add(new TicketSalesStopped(concertId, nextSeq()));
        }
        return this;
    }

    public Stream<ConcertEvent> stream() { return events.stream(); }

    public static class ConcertCustomizer {
        private int ticketPrice = 42;
        private String artistName = "Don't Care Artist Name";
        // ... more fields with defaults, plus fluent setters
    }
}
```

**Usage:**

```java
MakeEvents.with()
    .concertScheduled(concertId, concert -> concert
        .ticketPrice(35)
        .ticketsSold(6))
    .stream();
```

**Key difference from a builder:** The caller never constructs the final object. They configure preferences via the customizer, and the factory method handles the full construction graph (parent + children). This avoids nested builder-within-builder patterns.

## Level 7: Custom Assertions

See `references/custom-assertions.md`.

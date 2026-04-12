# OpenRewrite Recipes for Test Modernization

These recipes provide reliable, programmatic cleanup before manual refactoring.
All require the `rewrite-testing-frameworks` artifact.

## Dependency Setup

### Maven
```xml
<plugin>
  <groupId>org.openrewrite.maven</groupId>
  <artifactId>rewrite-maven-plugin</artifactId>
  <version>6.36.0</version>
  <configuration>
    <activeRecipes>
      <recipe>RECIPE_NAME_HERE</recipe>
    </activeRecipes>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>org.openrewrite.recipe</groupId>
      <artifactId>rewrite-testing-frameworks</artifactId>
      <version>3.34.0</version>
    </dependency>
  </dependencies>
</plugin>
```

Run: `mvn -q rewrite:run`

### Gradle
```gradle
plugins {
    id("org.openrewrite.rewrite") version("latest.release")
}
rewrite {
    activeRecipe("RECIPE_NAME_HERE")
}
dependencies {
    rewrite("org.openrewrite.recipe:rewrite-testing-frameworks:3.34.0")
}
```

Run: `./gradlew -q rewriteRun`

### One-Shot Maven Command (no pom changes)
```bash
mvn -q -U org.openrewrite.maven:rewrite-maven-plugin:run \
  --define rewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-testing-frameworks:RELEASE \
  --define rewrite.activeRecipes=RECIPE_NAME_HERE \
  --define rewrite.recipeChangeLogLevel=ERROR
```

## Tier 1: Migration Recipes (Run First)

|  Recipe |  Description |  
| ---- | ----  |
|  `org.openrewrite.java.testing.junit5.JUnit4to5Migration` |  Full JUnit 4 to JUnit 5 migration (annotations, assertions, rules to extensions) |  
|  `org.openrewrite.java.testing.assertj.JUnitToAssertj` |  Migrate JUnit assertions to AssertJ equivalents |  
|  `org.openrewrite.java.testing.hamcrest.HamcrestIsMatcherToAssertJ` |  Migrate Hamcrest `is()` matchers to AssertJ |  
|  `org.openrewrite.java.testing.hamcrest.MigrateHamcrestToJUnit5` |  Full Hamcrest to JUnit 5 migration | 

## Tier 2: Cleanup Recipes (Run After Migration)

|  Recipe |  Description |  
| ---- | ----  |
|  `org.openrewrite.java.testing.cleanup.BestPractices` |  Composite: includes assertions check + remove test prefix |  
|  `org.openrewrite.java.testing.cleanup.TestsShouldNotBePublic` |  Remove `public` from JUnit 5/6 test methods |  
|  `org.openrewrite.java.testing.cleanup.RemoveTestPrefix` |  Remove `test` prefix from method names |  
|  `org.openrewrite.java.testing.cleanup.SimplifyTestThrows` |  Simplify unnecessary throws clauses |  
|  `org.openrewrite.java.testing.cleanup.RemoveEmptyTests` |  Remove empty test methods without comments |  
|  `org.openrewrite.java.testing.cleanup.TestsShouldIncludeAssertions` |  Flag tests that have no assertion |  
|  `org.openrewrite.java.testing.cleanup.AssertionsArgumentOrder` |  Fix assertion argument ordering | 

## Tier 3: AssertJ Best Practices (Composite - Can Replace Tier 1 + 2 for AssertJ)

The big-bang recipe. If the project is already on JUnit 5/6, this single recipe
handles everything assertion-related in one pass:

| Recipe | FQ Name | What It Does |
|---|---|---|
| **AssertJ best practices** | `org.openrewrite.java.testing.assertj.Assertj` | Composite: migrates JUnit/Hamcrest/TestNG/Fest/Truth assertions to AssertJ, then applies all simplification and shortening rules |
| Shorten AssertJ assertions | `org.openrewrite.java.testing.assertj.SimplifyAssertJAssertions` | Replaces verbose assertions with dedicated ones (e.g., `.isEqualTo(true)` -> `.isTrue()`, `.isEqualTo("")` -> `.isEmpty()`, `.hasSize(0)` -> `.isEmpty()`) |
| Simplify chained assertions | `org.openrewrite.java.testing.assertj.SimplifyChainedAssertJAssertions` | Flattens unnecessarily chained assertion calls |
| Collapse consecutive assertThat | `org.openrewrite.java.testing.assertj.CollapseConsecutiveAssertThatStatements` | Merges back-to-back `assertThat(x)` calls on the same subject |
| Static imports | `org.openrewrite.java.testing.assertj.StaticImports` | Ensures `assertThat` is statically imported |

**Recommendation:** If you are already using AssertJ and just want cleanup, run
`SimplifyAssertJAssertions` + `SimplifyChainedAssertJAssertions` alone. If you
want the full migration + cleanup in one shot, run `Assertj`.

### One-shot Maven command (no pom changes):
```bash
mvn -q -U org.openrewrite.maven:rewrite-maven-plugin:run \
  --define rewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-testing-frameworks:RELEASE \
  --define rewrite.activeRecipes=org.openrewrite.java.testing.assertj.Assertj \
  --define rewrite.exportDatatables=true \
  --define rewrite.recipeChangeLogLevel=ERROR
```

## Notes

- Always run Tier 1 before Tier 2 (migration must happen before cleanup).
- After each recipe run, execute the full test suite with quiet output (`mvn -q test`) to confirm green.
- OpenRewrite 8.77.0+ includes `JavaBestPractices` targeting Java 25.
- For JUnit 6 migration recipes, check `org.openrewrite.java.testing.junit6`.

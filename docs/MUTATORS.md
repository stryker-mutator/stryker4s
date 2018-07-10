# Supported mutators

Stryker4s supports a variety of mutators, which are listed below. Do you have a suggestion for a (new) mutator? Feel free to create an [issue](https://github.com/stryker-mutator/stryker4s/issues)!

An always up-to-date reference is also available in the [MutantMatcher source](../core/src/main/scala/stryker4s/mutants/findmutants/MutantMatcher.scala).

## Conditionals

| Original | Mutated |
| --- | --- |
| `>=` | `>`, `<`, `==` |
| `>` | `<=`, `<`, `==` |
| `<=` | `<`, `>=`, `==` |
| `<` | `<=`, `>`, `==` |
| `==` | `!=` |
| `!=` | `==` |
| `&&` | `||` |
| `||` | `&&` |

## Methods

| Original | Mutated |
| --- | --- |
| `a.filter(b)` | `a.filterNot(b)` |
| `a.filterNot(b)` | `a.filter(b)` |

## Literal substitutions

| Original | Mutated |
| --- | --- |
| `true` | `false` |
| `false` | `true` |
| `"foo"` (non-empty string) | `""` (empty string) |
| `""` (empty string) | `"Stryker was here!"` |
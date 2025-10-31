# Changelog

## [0.20.0](https://github.com/stryker-mutator/stryker4s/compare/v0.19.1...v0.20.0) (2025-10-31)


### Features

* connect to testrunner over unix sockets, if available ([#1834](https://github.com/stryker-mutator/stryker4s/issues/1834)) ([3921e7c](https://github.com/stryker-mutator/stryker4s/commit/3921e7c6079b27550fe523a73e5bf1c3f33d3606))
* **report:** output HTML report as single file ([#1867](https://github.com/stryker-mutator/stryker4s/issues/1867)) ([cd60269](https://github.com/stryker-mutator/stryker4s/commit/cd60269dd8e1d05e92d4286af1619f9c7ea511ef))


### Bug Fixes

* compatibility with java 8 ([#1837](https://github.com/stryker-mutator/stryker4s/issues/1837)) ([be4590f](https://github.com/stryker-mutator/stryker4s/commit/be4590fe6e571239ce80ea26a2feb8d402bd7cd5))
* cross-compile to Scala 3 LTS, instead of for SBT2 ([#1840](https://github.com/stryker-mutator/stryker4s/issues/1840)) ([6e853ed](https://github.com/stryker-mutator/stryker4s/commit/6e853ed040987eb44296c8514a2c0c1157a54020))

## [0.19.1](https://github.com/stryker-mutator/stryker4s/compare/v0.19.0...v0.19.1) (2025-09-15)


### Bug Fixes

* **testrunner:** fix null pointer exception in testrunner when event throwable is null ([#1833](https://github.com/stryker-mutator/stryker4s/issues/1833)) ([2af192c](https://github.com/stryker-mutator/stryker4s/commit/2af192cf94b056240f127792e19658cebef71600))

## [0.19.0](https://github.com/stryker-mutator/stryker4s/compare/v0.18.1...v0.19.0) (2025-08-11)


### ⚠ BREAKING CHANGES

* **core:** Minimum sbt version is now 1.11.2

### Features

* **core:** support scala 3.7 dialect ([0e1bdb2](https://github.com/stryker-mutator/stryker4s/commit/0e1bdb2b42ff01eb017ee2cac00f95f0e28f146b))
* **sbt-plugin:** publish to sbt 2.0.0-RC2 ([0e1bdb2](https://github.com/stryker-mutator/stryker4s/commit/0e1bdb2b42ff01eb017ee2cac00f95f0e28f146b))


## [0.18.1](https://github.com/stryker-mutator/stryker4s/compare/v0.18.0...v0.18.1) (2025-08-04)


### Bug Fixes

* configure maven plugin to publish to new maven-central ([#1803](https://github.com/stryker-mutator/stryker4s/issues/1803)) ([dc6a5a9](https://github.com/stryker-mutator/stryker4s/commit/dc6a5a942de1fc16787b1de7a02486dcdcb77efa))
* use correct tasks for sonatype central release ([#1805](https://github.com/stryker-mutator/stryker4s/issues/1805)) ([ce7c317](https://github.com/stryker-mutator/stryker4s/commit/ce7c317731177efce9809c5f689b236f54e9a5d2))

## [0.18.0](https://github.com/stryker-mutator/stryker4s/compare/v0.17.2...v0.18.0) (2025-05-11)


### Features

* **console-reporter:** report score based on covered code ([#1764](https://github.com/stryker-mutator/stryker4s/issues/1764)) ([7929d03](https://github.com/stryker-mutator/stryker4s/commit/7929d03bb8fea63b53755a07ab219a7e397c9145))


### Bug Fixes

* no-coverage mutants should not be reported as static ([#1755](https://github.com/stryker-mutator/stryker4s/issues/1755)) ([779d2b4](https://github.com/stryker-mutator/stryker4s/commit/779d2b44729cbdd913dd7086ab2162b4697e40db))

## [0.17.2](https://github.com/stryker-mutator/stryker4s/compare/v0.17.1...v0.17.2) (2025-03-21)


### Bug Fixes

* **CLI:** allow multiple CLI arguments ([#1738](https://github.com/stryker-mutator/stryker4s/issues/1738)) ([f55e244](https://github.com/stryker-mutator/stryker4s/commit/f55e244dc285e18d5b2db26c4b5e31ae249cf49a))
* **sbt:** use argfile to start new java processes ([#1741](https://github.com/stryker-mutator/stryker4s/issues/1741)) ([5cdd00e](https://github.com/stryker-mutator/stryker4s/commit/5cdd00e04a79f345b5a1ee7d1e7c782c6e9d1f02))

## [0.17.1](https://github.com/stryker-mutator/stryker4s/compare/v0.17.0...v0.17.1) (2025-03-15)


### Bug Fixes

* handle relative paths correctly when fixing compile errors ([#1731](https://github.com/stryker-mutator/stryker4s/issues/1731)) ([c341c20](https://github.com/stryker-mutator/stryker4s/commit/c341c2079b30dd45a4573be289f5499d1472495a))

## [0.17.0](https://github.com/stryker-mutator/stryker4s/compare/v0.16.1...v0.17.0) (2025-03-14)


### Features

* better CLI parsing ([#1711](https://github.com/stryker-mutator/stryker4s/issues/1711)) ([27a9b4a](https://github.com/stryker-mutator/stryker4s/commit/27a9b4a21b2a6abc9b9cf0f43f37a46a28569a93))
* **config:** add CLI and build tool config support ([#1643](https://github.com/stryker-mutator/stryker4s/issues/1643)) ([9fcc1b1](https://github.com/stryker-mutator/stryker4s/commit/9fcc1b176f5dbf7a838baf025bb1a1a80a9ffbe2))
* **config:** support scala 3.4 and 3.5 dialect ([#1654](https://github.com/stryker-mutator/stryker4s/issues/1654)) ([c9ed5ac](https://github.com/stryker-mutator/stryker4s/commit/c9ed5ac6df9b597d5a7f55abc4622941d2c5bc04))
* **config:** support scala 3.6 dialect ([#1681](https://github.com/stryker-mutator/stryker4s/issues/1681)) ([299875b](https://github.com/stryker-mutator/stryker4s/commit/299875b917a4c5e94fbd6bbb57eb68f6d35123a3))
* **conf:** open report automatically ([#1430](https://github.com/stryker-mutator/stryker4s/issues/1430)) ([b58b851](https://github.com/stryker-mutator/stryker4s/commit/b58b8512aec6fc100ea3ee9e24ef822291986c32))
* cross-publish to sbt 2.x ([#1655](https://github.com/stryker-mutator/stryker4s/issues/1655)) ([b4fe26d](https://github.com/stryker-mutator/stryker4s/commit/b4fe26d493e2925f9613283f5fd69ab4d58dab92))
* cross-publish to scala 3 ([#1651](https://github.com/stryker-mutator/stryker4s/issues/1651)) ([c9c3c1d](https://github.com/stryker-mutator/stryker4s/commit/c9c3c1d1e7d49ab15136ac00e0d44cb5c0ea128a))
* read scala-dialect from sbt settings ([#1720](https://github.com/stryker-mutator/stryker4s/issues/1720)) ([9879518](https://github.com/stryker-mutator/stryker4s/commit/987951889672025f18395181274eae473fdc3c1a))


### Bug Fixes

* compatibility with sbt-crossproject ([#1648](https://github.com/stryker-mutator/stryker4s/issues/1648)) ([3879b63](https://github.com/stryker-mutator/stryker4s/commit/3879b63b8c623b8626643390847355d5d70844d9))
* disable fatal-warnings scalacOption when mutating ([#1680](https://github.com/stryker-mutator/stryker4s/issues/1680)) ([0b1db17](https://github.com/stryker-mutator/stryker4s/commit/0b1db172593c4148074587e6abfd7d0f3e821adf))
* handle relative and absolute base-dir paths ([#1725](https://github.com/stryker-mutator/stryker4s/issues/1725)) ([f357940](https://github.com/stryker-mutator/stryker4s/commit/f35794087d09a647cac726ef1465a29863d94874))
* handle WSL correctly when opening report ([#1726](https://github.com/stryker-mutator/stryker4s/issues/1726)) ([f2a79e5](https://github.com/stryker-mutator/stryker4s/commit/f2a79e5615e6e2dba463dfa9763d8562487260b1))
* mutate glob ([a933e7b](https://github.com/stryker-mutator/stryker4s/commit/a933e7b1377b160117dfd32e875f125b5ce5e885))
* **sbt:** glob expressions for mutate and files on windows ([#1727](https://github.com/stryker-mutator/stryker4s/issues/1727)) ([2901f08](https://github.com/stryker-mutator/stryker4s/commit/2901f085850f4db338ac0a0661a19b1e554e5bee))

## [0.16.1](https://github.com/stryker-mutator/stryker4s/compare/v0.16.0...v0.16.1) (2024-04-18)


### Miscellaneous Chores

* release 0.16.1 ([3785173](https://github.com/stryker-mutator/stryker4s/commit/3785173dab45ce8d98819f7a269b7e73978b8eed))

## [0.16.0](https://github.com/stryker-mutator/stryker4s/compare/v0.15.2...v0.16.0) (2023-12-27)


### ⚠ BREAKING CHANGES

* up minimum-supported sbt version to 1.7.0 ([#1476](https://github.com/stryker-mutator/stryker4s/issues/1476))

### Features

* add weapon-regex description to mutant and improve statusReason report field ([#1470](https://github.com/stryker-mutator/stryker4s/issues/1470)) ([bb8878f](https://github.com/stryker-mutator/stryker4s/commit/bb8878f5c344465f7591eda2b6cfeb23052fd9f9))
* **regex:** use correct replacement and location from weapon-regex mutations ([#1480](https://github.com/stryker-mutator/stryker4s/issues/1480)) ([fc9854b](https://github.com/stryker-mutator/stryker4s/commit/fc9854b5e87f37fb330ecbeaf69c421e29450322))
* **report:** add tests and coverage per-test to report ([#1475](https://github.com/stryker-mutator/stryker4s/issues/1475)) ([5a529c8](https://github.com/stryker-mutator/stryker4s/commit/5a529c8779754389b9e0701629c65fa302c17756))


### Bug Fixes

* regex without mutations throwing exception ([#1479](https://github.com/stryker-mutator/stryker4s/issues/1479)) ([d177986](https://github.com/stryker-mutator/stryker4s/commit/d177986a7eb2d5c72f7329491f444bd8dcfd0373))
* up minimum-supported sbt version to 1.7.0 ([#1476](https://github.com/stryker-mutator/stryker4s/issues/1476)) ([e2caf4a](https://github.com/stryker-mutator/stryker4s/commit/e2caf4a5f6d0375b22282750ac0194c63717d88f))
* update weapon-regex to 1.2.1 ([#1473](https://github.com/stryker-mutator/stryker4s/issues/1473)) ([a125824](https://github.com/stryker-mutator/stryker4s/commit/a12582478c771175e5c2e02f0928423ef711751e))


### Miscellaneous Chores

* release 0.16.0 ([bf3aeb9](https://github.com/stryker-mutator/stryker4s/commit/bf3aeb9c916b964aac3f79908ba77466ebc09cc5))

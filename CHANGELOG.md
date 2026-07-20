# Changelog

## [1.0.3](https://github.com/stryker-mutator/stryker4s/compare/v1.0.2...v1.0.3) (2026-07-20)


### Bug Fixes

* **sbt2:** detach state when running tasks to avoid errors bubbling up ([#2095](https://github.com/stryker-mutator/stryker4s/issues/2095)) ([1b8f383](https://github.com/stryker-mutator/stryker4s/commit/1b8f3832b4e977b521e0cf2ad42bc17f3d553af0))

## [1.0.2](https://github.com/stryker-mutator/stryker4s/compare/v1.0.1...v1.0.2) (2026-07-19)


### Bug Fixes

* **testrunner:** create new framework instance for each run (fixing Weaver compatibility) ([#2091](https://github.com/stryker-mutator/stryker4s/issues/2091)) ([4bdb2de](https://github.com/stryker-mutator/stryker4s/commit/4bdb2debde2d98d2184076df6574809aa5357bdf))

## [1.0.1](https://github.com/stryker-mutator/stryker4s/compare/v1.0.0...v1.0.1) (2026-07-15)


### Bug Fixes

* **rollback:** fix line-offset handling for compile error rollback ([#2088](https://github.com/stryker-mutator/stryker4s/issues/2088)) ([71c370a](https://github.com/stryker-mutator/stryker4s/commit/71c370a3900e354b7048c0e7abcbfd0ad5682760))

## [1.0.0](https://github.com/stryker-mutator/stryker4s/compare/v0.21.0...v1.0.0) (2026-07-15)

See https://stryker-mutator.io/blog/stryker4s-v1/

### Features

* **maven:** rewrite Maven plugin with testrunner and rollback support ([#2044](https://github.com/stryker-mutator/stryker4s/issues/2044)) ([053b3ce](https://github.com/stryker-mutator/stryker4s/commit/053b3ce6eaff1711fe5ef9642a22bca600e7eb44))
* **mill:** add Mill plugin ([#2042](https://github.com/stryker-mutator/stryker4s/issues/2042)) ([d18cbf7](https://github.com/stryker-mutator/stryker4s/commit/d18cbf73080d5eb0882a5849fce3e04020099c02))


### Bug Fixes

* always preserve original statement in mutation switch case ([#2045](https://github.com/stryker-mutator/stryker4s/issues/2045)) ([fff3a4a](https://github.com/stryker-mutator/stryker4s/commit/fff3a4aeebdaa5d255c651a11c4f0429060011d6))
* improve exclude-annotation matching ([#2063](https://github.com/stryker-mutator/stryker4s/issues/2063)) ([20059f0](https://github.com/stryker-mutator/stryker4s/commit/20059f075ab9f830490e4ce9fe7bd045bda14d00))
* **scala-dialect:** support Scala 3.9 ([#2039](https://github.com/stryker-mutator/stryker4s/issues/2039)) ([69afb80](https://github.com/stryker-mutator/stryker4s/commit/69afb8081896f9dba3dcb1e50dbe850809d59a1a))
* target java-output-version 17 ([#2040](https://github.com/stryker-mutator/stryker4s/issues/2040)) ([a82c4ae](https://github.com/stryker-mutator/stryker4s/commit/a82c4ae301be37eb40458693dca91ecae7cca535))
* **testrunner:** handle messages larger than socket buffer ([#2068](https://github.com/stryker-mutator/stryker4s/issues/2068)) ([86aa9ed](https://github.com/stryker-mutator/stryker4s/commit/86aa9ed1469dadae41d543874fdc2625e68d93b2))


### Performance Improvements

* multiple performance improvements during pre-testing stages ([#2070](https://github.com/stryker-mutator/stryker4s/issues/2070)) ([dba0f6a](https://github.com/stryker-mutator/stryker4s/commit/dba0f6a9acb9d592ebe270b3debcb6f8b412ae9e))
* only traverse tree once when building new mutated tree ([#2084](https://github.com/stryker-mutator/stryker4s/issues/2084)) ([a66af9b](https://github.com/stryker-mutator/stryker4s/commit/a66af9b8959da4266bb5d25b249c26f2b682cb15))
* splice mutated file instead of re-printing entire source tree ([#2083](https://github.com/stryker-mutator/stryker4s/issues/2083)) ([f8cb694](https://github.com/stryker-mutator/stryker4s/commit/f8cb694179d405ba6c9803d3a47004b7cf41c1b7))


### Miscellaneous Chores

* release 1.0.0 ([18d2660](https://github.com/stryker-mutator/stryker4s/commit/18d26606125f739fb595575c7863d3819f23d1bf))

## [0.21.0](https://github.com/stryker-mutator/stryker4s/compare/v0.20.4...v0.21.0) (2026-06-05)


### Features

* clearer logging ([#2015](https://github.com/stryker-mutator/stryker4s/issues/2015)) ([517bc58](https://github.com/stryker-mutator/stryker4s/commit/517bc58b45870b6d898467251ffb340ccb3efb5a))


### Bug Fixes

* disable Scala3 pattern match exhaustiveness check ([#2023](https://github.com/stryker-mutator/stryker4s/issues/2023)) ([a753f8f](https://github.com/stryker-mutator/stryker4s/commit/a753f8f29092bc69e6aff23089aec57ec8edce85))
* place coverage statement inside wildcard block instead of in condition ([#2024](https://github.com/stryker-mutator/stryker4s/issues/2024)) ([5d24b3e](https://github.com/stryker-mutator/stryker4s/commit/5d24b3e63ad335eeee239ea0a17ac8c76bf82a70))
* report correct location for statements with comments ([#2025](https://github.com/stryker-mutator/stryker4s/issues/2025)) ([8e187b8](https://github.com/stryker-mutator/stryker4s/commit/8e187b827bc3118b129c56e161cdc5f6df2f7fe6))
* **rollback:** use offset for compile error location when available ([#2013](https://github.com/stryker-mutator/stryker4s/issues/2013)) ([b7c6d79](https://github.com/stryker-mutator/stryker4s/commit/b7c6d79f993c7de79648d853fd533188571091f0)), closes [#2011](https://github.com/stryker-mutator/stryker4s/issues/2011)
* **sbt:** read test-runner responses on demand to avoid teardown race ([#1861](https://github.com/stryker-mutator/stryker4s/issues/1861)) ([#2010](https://github.com/stryker-mutator/stryker4s/issues/2010)) ([9585be4](https://github.com/stryker-mutator/stryker4s/commit/9585be4d316c91cc33532e529c1c6721c2b7ebd2))

## [0.20.4](https://github.com/stryker-mutator/stryker4s/compare/v0.20.3...v0.20.4) (2026-05-21)

Stryker4s now works properly on multi-module projects! Simply running `sbt <module-name>/stryker` will run Stryker4s on the specified module. The workaround of `sbt 'project <module-name>' stryker` is no longer necessary. Thanks to @rwaldvogel for the contribution!

### Bug Fixes

* **sbt:** scope task queries to the invoking project ([#1994](https://github.com/stryker-mutator/stryker4s/issues/1994)) ([bfbe167](https://github.com/stryker-mutator/stryker4s/commit/bfbe167bc378a5018b9f512a6b16f13a3579405d))
* **sbt:** strip -Werror alongside -Xfatal-warnings ([#1999](https://github.com/stryker-mutator/stryker4s/issues/1999)) ([afb2e50](https://github.com/stryker-mutator/stryker4s/commit/afb2e505d6fd598266984ed6702e897676ddc96d))

## [0.20.3](https://github.com/stryker-mutator/stryker4s/compare/v0.20.2...v0.20.3) (2026-03-25)


### Bug Fixes

* **unix-sockets:** use temp file for socket path instead of fixed path ([#1958](https://github.com/stryker-mutator/stryker4s/issues/1958)) ([f42d3ac](https://github.com/stryker-mutator/stryker4s/commit/f42d3accac45ef8546638ad8c2a79d882f25a023))

## [0.20.2](https://github.com/stryker-mutator/stryker4s/compare/v0.20.1...v0.20.2) (2026-03-19)

### Bug Fixes

* fix test-runner timeouts ([#1950](https://github.com/stryker-mutator/stryker4s/issues/1950)) ([88fb656](https://github.com/stryker-mutator/stryker4s/commit/88fb656b5fa5c08643cf87e46a421d785f43abd8))

### Miscellaneous Chores

* release 0.20.2 ([23c5fe2](https://github.com/stryker-mutator/stryker4s/commit/23c5fe2a6118b1c37f5c72ed5b8e969b7bbc4294))

## [0.20.1](https://github.com/stryker-mutator/stryker4s/compare/v0.20.0...v0.20.1) (2026-03-17)


### Bug Fixes

* fix test-runner timeouts ([#1950](https://github.com/stryker-mutator/stryker4s/issues/1950)) ([88fb656](https://github.com/stryker-mutator/stryker4s/commit/88fb656b5fa5c08643cf87e46a421d785f43abd8))

## [0.20.0](https://github.com/stryker-mutator/stryker4s/compare/v0.19.1...v0.20.0) (2026-03-16)

This release uses the new support in [FS2](https://fs2.io/#/) for Unix sockets to communicate with test-runners. Stryker4s will seamlessly use either native Unix file sockets, or the "old" TCP sockets, whichever is available. This should give slightly improved performance and stability. If you run into any issues, [please let us know](https://github.com/stryker-mutator/stryker4s/issues/new/choose)!

### Features

* connect to testrunner over unix sockets, if available ([#1834](https://github.com/stryker-mutator/stryker4s/issues/1834)) ([3921e7c](https://github.com/stryker-mutator/stryker4s/commit/3921e7c6079b27550fe523a73e5bf1c3f33d3606))
* **report:** output HTML report as single file ([#1867](https://github.com/stryker-mutator/stryker4s/issues/1867)) ([cd60269](https://github.com/stryker-mutator/stryker4s/commit/cd60269dd8e1d05e92d4286af1619f9c7ea511ef))
* **scala-dialect:** support Scala 3.8 ([#1883](https://github.com/stryker-mutator/stryker4s/issues/1883)) ([810c600](https://github.com/stryker-mutator/stryker4s/commit/810c600a4f7fe0fa4d8103fc77262e6f0b60f4fe))


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

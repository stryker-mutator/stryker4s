# Changelog

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

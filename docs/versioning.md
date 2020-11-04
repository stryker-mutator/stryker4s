---
sidebar_label: Versioning
title: Versioning strategy
custom_edit_url: https://github.com/stryker-mutator/stryker4s/edit/master/docs/versioning.md
---

This document describes how versioning is managed for the Stryker4s project.

## The strategy

We choose to have one version for the complete project. The main reason for this is because Stryker4s
will mainly be used as a plugin for your specific used build tool. When using one specific version
we will be able to make changes in the core module and release everything at once. This also makes it
easier for us to avoid backward compatibility and versions conflict issues which would be very hard to test.

## Versioning

For the actual versioning, we will be applying the standard semver guidelines.
Don't know the semver guidelines? You can find information about it [here](https://semver.org/).

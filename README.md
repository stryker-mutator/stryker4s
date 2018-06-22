# Stryker4S

[![Build Status](https://travis-ci.org/stryker-mutator/stryker4s.svg?branch=master)](https://travis-ci.org/stryker-mutator/stryker4s)

## Configuration

### stryker4s.conf

Configuration is read from the stryker4s.conf file in the root of the project. This file is read in the HOCON-format. All configuration should be in the "stryker4s" namespace.

```conf
stryker4s {
# Your configuration here
}
```

### files

Pattern of which files should be mutated.

#### Default

```conf
files = [
    "**/main/scala/**/*.scala"
]
```

### base-dir

The base directory of the project.

#### Default

The system property `user.dir` (the directory from which the program is executed).

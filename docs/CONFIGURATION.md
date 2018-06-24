# Configuration  

All configuration options can be set from the stryker4s.conf file in the root of the project. This file is read in the HOCON-format. All configuration should be in the "stryker4s" namespace.

```conf
stryker4s {
# Your configuration here
}
```

#### Files to mutate

**Config file:** `files: [ "**/main/scala/**/*.scala" ]`  
**Default value:** `[ "**/main/scala/**/*.scala" ]`  
**Mandatory**: No  
**Description:**  
With `files` you configure the subset of files to use for mutation testing. 
Generally speaking, these should be your own source files.  
The default for this will find files in the common Scala project format. 

#### base-dir

**Config file:** `base-dir: '/usr/your/project/folder/here'`  
**Default value:** The directory from which the process is started  
**Mandatory**: No  
**Description:**  
With `base-dir` you specify the directory from which stryker4s starts and searches for mutations. The default for this is the directory from which the project is ran, which should be fine in most cases.

Mutator kotlin has been converted in such a way that it works with the new stryker4jvm.
Extensive testing is required to see if it truly works.

Important note(s):
The original kotlin mutator replaces trees in the original KtFile; this means that stryker4jvm
might not have any reference to the truly original tree...

I am unsure when .equals returns true for kotlin trees, this may result in issues when using trees as keys in maps
# Stryker4s vision of the future

First of all welcome to Stryker4s, this document will describe the vision @hugo-vrijswijk and @legopiraat currently have for the future of Stryker4s.
Since this project is still in the early phase there is a lot of things we want to do and not everything fits into the issue list just yet. 
That is why the vision will be scoped into three sections, short term, mid term, long term.

## Short-term goals
In the short term, we want to release the first version of Stryker4s in the form of a 0.0.1 release. 
This release will focus on basic configuration settings, common mutators, and support for SBT.

We are getting really close for [release 0.0.1](https://github.com/stryker-mutator/stryker4s/milestone/1) there are just some small things left to do.

## Mid-term goals
When version 0.0.1 of Stryker4s is released and running we can start looking for improvements to make. 
To name some interesting features
* Maven plugin
* Break build if mutation score threshold isn't reached
* HTML Dashboard which will give more insights where the mutations did and didn't get killed.
* New mutators

All these features will improve are focused on the usability of Stryker4s. With these features, mutation testing will have a bigger impact on your code.
But usability, of course, is not the only thing that needs to be improved on, speed is something that is always in the back of our heads.
With things like multithreading and code coverage analysis we would be even faster then we would be now.

And of course last but not least, upgrading to Scala 2.13 and all its new features are clearly something we want to do.

## Long-term goals
With Dotty coming closer and closer to its release (still over a year to go at least) Stryker4s will have a whole new game on this new Scala version.
Why is this you ask? Well, that's because Scala 3 will bring a whole new Abstract Syntax Tree which we heavily rely on for finding mutations.

With this new AST (called TASTY) we will be able to gain much more information and hopefully improve insights for mutation testing. 

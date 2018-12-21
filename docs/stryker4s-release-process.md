# How to release Stryker4s to Sonatype

Releasing to Sonatype is fairly easy to do this when using this step by step approach. 
Most, if the things needed, are already configured in Stryker4s such as name, license, GitHub repository etc.

## Extra configuration

### Credentials
To be able to publish you need to configure the strykermutator credentials on your own local machine.
This can be done by adding the following code to your `~/.sbt/1.0/sonatype.sbt` or `$HOME/.sbt/1.0/sonatype.sbt` on windows.

```scala
credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org",
                           "strykermutator","(Sonatype password)")
```

This needs to be done on our machines to make sure the credentials are safely stored.

### GPG key

Don't know how to do this yet, maybe this works: [create PGP](https://support.symantec.com/en_US/article.HOWTO42070.html)

## Releasing Stryker4s

1. Change the version number in the `settings.sbt`. If the version number includes snapshot we will not be able to release to the release repository.
1. Edit the `CHANGELOG.md` so all changes and features are captured for the release.
1. Make sure `sbt clean package` works as expected so we don't publish broken versions.
1. Use the `sbt publishSigned` command to release to the staging area of Sonatype. We should be able to see if all the artifacts are correctly published here.
1. Use `sbt sonatypeRelease` to close the staging area and promote the artifact to the release repositories.

## Extra information

Extra information about the release plugin used to deploy to Sonatype can be found on the [github page]( https://github.com/xerial/sbt-sonatype).
dans-nexus-maven-plugin
=======================

Maven plugin to deploy RPMs to a Nexus Repository

SYNOPSIS
========

```xml

<plugin>
    <groupId>nl.knaw.dans</groupId>
    <artifactId>dans-nexus-maven-plugin</artifactId>
    <version>plugin-version</version>
    <configuration>
        <nexusUserName>nexus_user</nexusUserName>
        <nexusPassword>nexus_password</nexusPassword>
        <rpmRepositoryUrl>http://your.nexus.host/repository/rpm-releases</rpmRepositoryUrl>
        <snapshotRpmRepositoryUrl>http://your.nexus.host/repository/rpm-snapshots
        </snapshotRpmRepositoryUrl>
    </configuration>
</plugin>
```

```bash
mvn deploy
# OR
mvn dans-nexus:deploy-rpm
```

DESCRIPTION
===========

The [Nexus Repository Manager]{:target=_blank} support several types of repositories, among which is YUM. However, there seems to be no support for publishing your RPM
artifacts with the standard [maven-deploy-plugin]{:target=_blank}. `dans-nexus-maven-plugin` tries to fill that gap.

Usage
-----










[Nexus Repository Manager]: https://help.sonatype.com/repomanager3
[maven-deploy-plugin]: https://maven.apache.org/plugins/maven-deploy-plugin/
#!/bin/bash
# Release Guidelines: https://github.com/ashwin153/caustic/wiki/Release
read -p "Artifact version (defaults to incrementing patch version): " version
read -r -p "Are you sure? [y/N] " response

if [[ "$response" =~ ^([yY][eE][sS]|[yY])+$ ]] ; then
  # Publish Build Artifacts
  if [ -z "version" ] ; then
    ./pants publish.jar --publish-jar-no-dryrun \
      caustic-runtime/src/main/scala \
      caustic-runtime/src/main/thrift:java
  else
    ./pants publish.jar --publish-jar-no-dryrun \
      --publish-jar-override=com.madavan#caustic-runtime_2.12=$version \
      --publish-jar-override=com.madavan#caustic-thrift=$version \
      caustic-runtime/src/main/scala \
      caustic-runtime/src/main/thrift:java
  fi

  # Promote to Maven Central
  /usr/bin/open -a "/Applications/Google Chrome.app" \
    'http://www.pantsbuild.org/release_jvm.html#promoting-to-maven-central'
  /usr/bin/open -a "/Applications/Google Chrome.app" \
    'https://oss.sonatype.org/#stagingRepositories'
fi

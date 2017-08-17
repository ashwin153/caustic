#!/bin/bash
# Release Guidelines: https://github.com/ashwin153/caustic/wiki/Release
read -p "Artifact version (defaults to incrementing patch version): " version
read -r -p "Are you sure? [y/N] " response

if [[ "$response" =~ ^([yY][eE][sS]|[yY])+$ ]] ; then
  # Publish Build Artifacts
  if [ -z "version" ] ; then
    ./pants publish.jar --publish-jar-no-dryrun \
      caustic-runtime/src/main/scala \
      caustic-syntax/src/main/scala \
      caustic-mysql/src/main/scala \
      caustic-postgres/src/main/scala
  else
    ./pants publish.jar --publish-jar-no-dryrun \
      --publish-jar-override=com.madavan#caustic-runtime_2.12=$version \
      --publish-jar-override=com.madavan#caustic-syntax_2.12=$version \
      --publish-jar-override=com.madavan#caustic-mysql_2.12=$version \
      --publish-jar-override=com.madavan#caustic-postgres_2.12=$version \
      caustic-runtime/src/main/scala \
      caustic-syntax/src/main/scala \
      caustic-mysql/src/main/scala \
      caustic-postgres/src/main/scala
  fi

  # Promote to Maven Central
  /usr/bin/open -a "/Applications/Google Chrome.app" \
    'http://www.pantsbuild.org/release_jvm.html#promoting-to-maven-central'
  /usr/bin/open -a "/Applications/Google Chrome.app" \
    'https://oss.sonatype.org/#stagingRepositories'
fi

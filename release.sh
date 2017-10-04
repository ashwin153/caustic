#!/bin/bash
branch=$(git symbolic-ref --short HEAD)

# Verify Branch is Clean.
if [ -n "$(git status --porcelain)" ]; then 
  echo -e "Current branch \033[0;33m$branch\033[0m has uncommitted changes."
  exit 1
fi

# Release Guidelines: https://github.com/ashwin153/caustic/wiki/Release
read -p "Artifact version (defaults to incrementing patch version): " version
read -r -p "$(echo -e -n "Confirm release of \033[0;33m$branch\033[0;0m? [y/N] ")" response

# Publish Build Artifacts.
if [[ "$response" =~ ^([yY][eE][sS]|[yY])+$ ]] ; then
  if [ -z "version" ] ; then
    ./pants publish.jar --publish-jar-no-dryrun \
      caustic-runtime/src/main/scala \
      caustic-runtime/src/main/thrift \
      caustic-common/src/main/scala
  else
    ./pants publish.jar --publish-jar-no-dryrun \
      --publish-jar-override=com.madavan#caustic-runtime_2.12=$version \
      --publish-jar-override=com.madavan#caustic-thrift=$version \
      --publish-jar-override=com.madavan#caustic-common_2.12=$version \
      caustic-runtime/src/main/scala \
      caustic-runtime/src/main/thrift \
      caustic-common/src/main/scala
  fi

  # Promote to Maven Central.
  /usr/bin/open -a "/Applications/Google Chrome.app" \
    'http://www.pantsbuild.org/release_jvm.html#promoting-to-maven-central'
  /usr/bin/open -a "/Applications/Google Chrome.app" \
    'https://oss.sonatype.org/#stagingRepositories'
fi

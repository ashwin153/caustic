#!/bin/bash
declare -a targets=(
  "caustic-common/src/main/scala"
  "caustic-runtime/src/main/scala"
  "caustic-runtime/src/main/thrift"
  "caustic-service/src/main/scala"
)

declare -a artifacts=(
  "caustic-common_2.12"
  "caustic-runtime_2.12"
  "caustic-thrift"
  "caustic-service_2.12"
)

# Verify Branch is Clean.
branch=$(git symbolic-ref --short HEAD)
if [ -n "$(git status --porcelain)" ]; then 
  echo -e "Current branch \033[0;33m$branch\033[0m has uncommitted changes."
  exit 1
fi

# Release Guidelines: https://github.com/ashwin153/caustic/wiki/Release
read -p "Artifact version (defaults to incrementing patch version): " version
read -r -p "$(echo -e -n "Confirm release of \033[0;33m$branch\033[0;0m? [y|N] ")" response

# Publish Build Artifacts.
if [[ "$response" =~ ^([yY][eE][sS]|[yY])+$ ]] ; then
  publish="./pants publish.jar --publish-jar-no-dryrun"
  if [ -z "$version" ] ; then
    # Increment Patch Version.
    eval "$publish ${targets[@]}"
  else
    # Override Artifact Version.
    overrides=("${artifacts[@]/#/--publish-jar-override=com.madavan#}")
    overrides=("${overrides[@]/%/=$version}")
    eval "$publish ${overrides[@]} ${targets[@]}"
  fi

  # Promote to Maven Central.
  /usr/bin/open -a "/Applications/Google Chrome.app" \
    'http://www.pantsbuild.org/release_jvm.html#promoting-to-maven-central'
  /usr/bin/open -a "/Applications/Google Chrome.app" \
    'https://oss.sonatype.org/#stagingRepositories'
fi

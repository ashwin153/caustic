# Shapeless
jar_library(name='shapeless', jars=[
  jar(org='com.chuusai', name='shapeless_2.12', rev='2.3.2')
])

# Akka
jar_library(name='akka', jars=[
  jar(org='com.typesafe.akka', name='akka-actor_2.12', rev='2.4.17')
])

# Google Guava
jar_library(name='guava', jars=[
  jar(org='com.google.guava', name='guava', rev='21.0')
])

# JUnit
jar_library(name='junit', jars=[
  jar(org='junit', name='junit', rev='4.12'),
  jar(org='org.pantsbuild', name='junit-runner-annotations', rev='0.0.17')
])

# Scalatest
jar_library(name='scalatest',
  jars=[jar(org='org.scalatest', name='scalatest_2.12', rev='3.0.0', excludes=[exclude('org.scala-lang')])],
  dependencies=['//:scala-library', '//:scala-reflect']
)

# Mockito
jar_library(name='mockito', jars=[
  jar(org='org.mockito', name='mockito-core', rev='2.7.11')
])

# Scalameter
jar_library(name='scalameter', jars=[
  jar(org='com.storm-enroute', name='scalameter_2.12', rev='0.8.2')
])

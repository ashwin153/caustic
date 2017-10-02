# C3P0
jar_library(name='c3p0', jars=[
  jar(org='c3p0', name='c3p0', rev='0.9.1.2')
])

# MySQL
jar_library(name='mysql', jars=[
  jar(org='mysql', name='mysql-connector-java', rev='6.0.6'),
])

# PostgreSQL
jar_library(name='postgres', jars=[
  jar(org='postgresql', name='postgresql', rev='9.1-901.jdbc4'),
])

# Caffeine
jar_library(name='caffeine', jars=[
  jar(org='com.github.ben-manes.caffeine', name='caffeine', rev='2.5.5'),
])

# Thrift
jar_library(name='thrift', jars=[
  jar(org='org.apache.thrift', name='libthrift', rev='0.10.0'),
])

# Jackson
jar_library(name='jackson', jars=[
  jar(org='com.fasterxml.jackson.core', name='jackson-databind', rev='2.9.1'),
  jar(org='com.fasterxml.jackson.module', name='jackson-module-scala_2.12', rev='2.9.1'),
])

# Curator
jar_library(name='curator', jars=[
  jar(org='org.apache.curator', name='curator-framework', rev='4.0.0', excludes=[exclude('org.apache.zookeeper')]),
  jar(org='org.apache.curator', name='curator-recipes', rev='4.0.0', excludes=[exclude('org.apache.zookeeper')]),
  jar(org='org.apache.zookeeper', name='zookeeper', rev='3.4.10'),
  jar(org='org.slf4j', name='slf4j-simple', rev='1.7.25'),
])

# Shapeless
jar_library(name='shapeless', jars=[
  jar(org='com.chuusai', name='shapeless_2.12', rev='2.3.2'),
])

# JUnit
jar_library(name='junit', jars=[
  jar(org='junit', name='junit', rev='4.12'),
  jar(org='org.pantsbuild', name='junit-runner-annotations', rev='0.0.17'),
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

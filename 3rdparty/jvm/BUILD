# Shapeless
jar_library(name='shapeless', jars=[
  jar(org='com.chuusai', name='shapeless_2.12', rev='2.3.2')
])

# Akka
jar_library(name='akka', jars=[
  jar(org='com.typesafe.akka', name='akka-actor_2.12', rev='2.4.17')
])

# Scala ARM
jar_library(name='scala-arm', jars=[
  jar(org='com.jsuereth', name='scala-arm_2.12', rev='2.0')
])

# Cassandra
jar_library(name='cassandra', jars=[
  jar(org='org.apache.cassandra', name='cassandra-all', rev='3.10')
])

# Redis
jar_library(name='redis', jars=[
  jar(org='com.github.etaty', name='rediscala_2.12', rev='1.8.0')
])

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

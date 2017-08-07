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

# Thrift
jar_library(name='thrift', jars=[
  jar(org='org.apache.thrift', name='libthrift', rev='0.10.0')
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

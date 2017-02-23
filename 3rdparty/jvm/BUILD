# Shapeless
jar_library(name='shapeless', jars=[
  jar(org='com.chuusai', name='shapeless_2.12', rev='2.3.2')
])

# Apache Kafka
jar_library(name='kafka', jars=[
  jar(org='org.apache.kafka', name='kafka_2.12', rev='0.10.1.1')
])

# Rediscala (Redis)
jar_library(name='rediscala', jars=[
  jar(org='com.github.etaty', name='rediscala_2.12', rev='1.8')
])

# Shade (Memcached)
jar_library(name='shade', jars=[
  jar(org='io.monix', name='shade_2.12', rev='1.9.1')
])

# JUnit
jar_library(name='junit', jars=[
  jar(org='junit', name='junit', rev='4.12'),
  jar(org='org.pantsbuild', name='junit-runner-annotations', rev='0.0.11')
])

# Scalatest
jar_library(name='scalatest',
  jars=[jar(org='org.scalatest', name='scalatest_2.12', rev='3.0.0', excludes=[exclude('org.scala-lang')])],
  dependencies=['//:scala-library', '//:scala-reflect']
)

# Mockito
jar_library(name='mockito', jars=[
  jar(org='org.mockito', name='mockito-all', rev='1.10.19')
])
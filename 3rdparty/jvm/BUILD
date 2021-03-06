####################################################################################################
#                                             Compile                                              #
####################################################################################################
# Beaker Client
jar_library(name='beaker-client', jars=[
    jar(org='com.madavan', name='beaker-client_2.12', rev='2.0.1'),
])

# Shapeless
jar_library(name='shapeless', jars=[
    jar(org='com.chuusai', name='shapeless_2.12', rev='2.3.2'),
])

# SLF4J
jar_library(name='slf4j-simple', jars=[
    jar(org='org.slf4j', name='slf4j-simple', rev='1.7.25'),
])

# Spray JSON
jar_library(name='spray-json', jars=[
    jar(org='io.spray', name='spray-json_2.12', rev='1.3.3'),
])

####################################################################################################
#                                              Build                                               #
####################################################################################################
# Ammonite
jar_library(name='ammonite', jars=[
    jar(org='com.lihaoyi', name='ammonite_2.12.3', rev='1.0.5'),
])

# ANTLR
jar_library(name='antlr', jars=[
    jar(org='org.antlr', name='antlr4', rev='4.7'),
    jar(org='org.antlr', name='antlr4-runtime', rev='4.7'),
])

# Causticc
jar_library(name='caustic-compiler', jars=[
    jar(org='com.madavan', name='caustic-compiler_2.12', rev='2.0.3'),
])

# Scalac
jar_library(name='scala-compiler', dependencies=[':scala-library', ':scala-reflect'], jars=[
    jar(org='org.scala-lang', name='scala-compiler', rev='2.12.3'),
])

# Scala Library
jar_library(name='scala-library', jars=[
    jar(org='org.scala-lang', name='scala-library', rev='2.12.3'),
])

# Scala Reflection
jar_library(name='scala-reflect', jars=[
    jar(org='org.scala-lang', name='scala-reflect', rev='2.12.3', intransitive=True),
])

####################################################################################################
#                                               Test                                               #
####################################################################################################
# JUnit
jar_library(name='junit', jars=[
    jar(org='junit', name='junit', rev='4.12'),
    jar(org='org.pantsbuild', name='junit-runner-annotations', rev='0.0.17'),
])

# Mockito
jar_library(name='mockito', jars=[
    jar(org='org.mockito', name='mockito-core', rev='2.7.11'),
])

# Scalameter
jar_library(name='scalameter', jars=[
    jar(org='com.storm-enroute', name='scalameter_2.12', rev='0.8.2')
])

# Scalatest
jar_library(name='scalatest', dependencies=[':scala-library', ':scala-reflect'], jars=[
    jar(org='org.scalatest', name='scalatest_2.12', rev='3.0.0', excludes=[exclude('org.scala-lang')]),
])

scala_library(
  name = 'scala',
  sources = rglobs('*.scala'),
  dependencies = [
    'caustic-runtime/src/main/scala',
    scoped('3rdparty/jvm:mysql', scope='runtime'),
  ],
  provides = artifact(
    org = 'com.madavan',
    name = 'caustic-mysql_2.12',
    repo = public,
    publication_metadata = describe,
  ),
)

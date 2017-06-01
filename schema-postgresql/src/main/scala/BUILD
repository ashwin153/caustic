scala_library(
  name = 'scala',
  sources = rglobs('*.scala'),
  dependencies = [
    'schema-runtime/src/main/scala',
    '3rdparty/jvm:postgres',
  ],
  provides = artifact(
    org = 'com.madavan',
    name = 'schema-postgresql_2.12',
    repo = public,
    publication_metadata = describe,
  ),
)

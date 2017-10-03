scala_library(
    name='scala',
    sources=rglobs('*.scala'),
    dependencies=[
        'caustic-runtime/src/main/thrift:java',
        'caustic-common/src/main/scala',
        '3rdparty/jvm:caffeine',
        '3rdparty/jvm:curator',
        '3rdparty/jvm:jackson',
        '3rdparty/jvm:rediscala',
        scoped('3rdparty/jvm:mysql', scope='runtime'),
        scoped('3rdparty/jvm:postgres', scope='runtime'),
    ],
    provides=artifact(
        org='com.madavan',
        name='caustic-runtime_2.12',
        repo=public,
        publication_metadata=describe,
    ),
)

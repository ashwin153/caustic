scala_library(
    name='scala',
    sources=rglobs('*.scala'),
    dependencies=[
        'caustic-runtime/src/main/resources',
        'caustic-runtime/src/main/thrift',
        'caustic-common/src/main/scala',
        '3rdparty/jvm:c3p0',
        '3rdparty/jvm:caffeine',
        '3rdparty/jvm:rediscala',
        scoped('3rdparty/jvm:mysql', scope='runtime'),
        scoped('3rdparty/jvm:postgres', scope='runtime'),
        '3rdparty/jvm:slf4j-simple',
    ],
    provides=artifact(
        org='com.madavan',
        name='caustic-runtime_2.12',
        repo=public,
        publication_metadata=describe,
    ),
)

jvm_binary(
    name='bin',
    basename='caustic-runtime',
    source='caustic/runtime/Main.scala',
    main='caustic.runtime.Main',
    dependencies=[
        'caustic-runtime/src/main/scala',
    ],
)
scala_library(
    name='scala',
    sources=rglobs('*.scala'),
    dependencies=[
        'caustic-runtime/src/main/resources',
        'caustic-runtime/src/main/thrift',
        '3rdparty/jvm:c3p0',
        '3rdparty/jvm:caffeine',
        '3rdparty/jvm:curator',
        '3rdparty/jvm:pureconfig',
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

scala_library(
    name='service',
    sources=rglobs('caustic/runtime/service/*.scala'),
    dependencies=[
        'caustic-runtime/src/main/thrift',
        '3rdparty/jvm:curator',
        '3rdparty/jvm:pureconfig',
    ],
    provides=artifact(
        org='com.madavan',
        name='caustic-service_2.12',
        repo=public,
        publication_metadata=describe,
    ),
)

jvm_binary(
    name='server',
    basename='caustic-server',
    source='caustic/runtime/Server.scala',
    main='caustic.runtime.Server',
    dependencies=[
        'caustic-runtime/src/main/scala',
    ],
)
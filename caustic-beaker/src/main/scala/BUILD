scala_library(
    name='scala',
    sources=rglobs('*.scala'),
    dependencies=[
        'caustic-cluster/src/main/scala',
        'caustic-beaker/src/main/resources',
        'caustic-beaker/src/main/thrift',
        '3rdparty/jvm:c3p0',
        '3rdparty/jvm:caffeine',
        '3rdparty/jvm:curator',
        scoped('3rdparty/jvm:mysql', scope='runtime'),
        scoped('3rdparty/jvm:postgres', scope='runtime'),
        '3rdparty/jvm:pureconfig',
        '3rdparty/jvm:jedis',
    ],
)
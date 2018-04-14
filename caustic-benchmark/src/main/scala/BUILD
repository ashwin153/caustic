scala_library(
    name='scala',
    sources=rglobs('*.scala'),
    dependencies=[
        'caustic-runtime/src/main/scala',
        '3rdparty/jvm:scalameter',
    ],
)

jvm_binary(
    name='bin',
    basename='caustic-benchmark',
    source='caustic/benchmark/Suite.scala',
    main='caustic.benchmark.Suite',
    dependencies=[
        'caustic-benchmark/src/main/scala',
    ],
)



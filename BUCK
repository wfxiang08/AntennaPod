# The files and modifications provided by Facebook are for testing and evaluation purposes only.  Facebook reserves all rights not expressly granted.

import re

jar_deps = []
for jarfile in glob(['libs/*.jar']):
  name = 'jars__' + re.sub(r'^.*/([^/]+)\.jar$', r'\1', jarfile)
  jar_deps.append(':' + name)
  prebuilt_jar(
    name = name,
    binary_jar = jarfile,
  )

android_library(
  name = 'all-jars',
  exported_deps = jar_deps,
)

presto_gen_aidls = []
for aidlfile in glob(['src/com/aocate/presto/service/*.aidl']):
  name = 'presto_aidls__' + re.sub(r'^.*/([^/]+)\.aidl$', r'\1', aidlfile)
  presto_gen_aidls.append(':' + name)
  gen_aidl(
    name = name,
    aidl = aidlfile,
    import_path = 'src',
  )

android_library(
  name = 'presto-aidls',
  srcs = presto_gen_aidls,
)


android_build_config(
  name = 'build-config',
  package = 'de.danoeh.antennapod',
)

APP_CLASS_SOURCE = 'src/de/danoeh/antennapod/AppShell.java'

android_library(
  name = 'main-lib',
  srcs = glob(['src/de/danoeh/antennapod/**/*.java'], excludes = [APP_CLASS_SOURCE]),
  deps = [
    ':all-jars',
    ':dslv-lib',
    ':presto-lib',
    ':appcompat',
    ':build-config',
    ':res',
  ],
)

android_library(
  name = 'application-lib',
  srcs = [APP_CLASS_SOURCE],
  deps = [
    ':build-config',
    ':jars__buck-android-support',
  ],
)

android_resource(
  name = 'res',
  package = 'de.danoeh.antennapod',
  res = 'res',
  assets = 'assets',
  deps = [
    ':appcompat',
    ':dslv-res',
  ]
)

android_library(
  name = 'dslv-lib',
  srcs = glob(['submodules/dslv/library/src/**/*.java']),
  deps = [
    ':all-jars',
    ':dslv-res',
  ],
)

android_resource(
  name = 'dslv-res',
  package = 'com.mobeta.android.dslv',
  res = 'submodules/dslv/library/res',
  deps = [
  ]
)

android_library(
  name = 'presto-lib',
  srcs = glob(['src/com/aocate/**/*.java']),
  deps = [
    ':presto-aidls',
    ':all-jars',
  ],
)

android_manifest(
  name = 'manifest',
  skeleton = 'AndroidManifest.xml',
  deps = [
    ':main-lib',
  ],
)

keystore(
  name = 'debug_keystore',
  store = 'keystore/debug.keystore',
  properties = 'keystore/debug.keystore.properties',
)

android_binary(
  name = 'antennapod',
  manifest = ':manifest',
  keystore = ':debug_keystore',
  use_split_dex = True,
  exopackage = True,
  primary_dex_patterns = [
    '^de/danoeh/antennapod/AppShell^',
    '^de/danoeh/antennapod/BuildConfig^',
    '^com/facebook/buck/android/support/exopackage/'
  ],
  deps = [
    ':main-lib',
    ':application-lib',
  ],
)


android_prebuilt_aar(
  name = 'appcompat',
  aar = 'libs/appcompat-v7-19.1.0.aar',
)

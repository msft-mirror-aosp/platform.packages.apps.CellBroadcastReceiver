load("//wireless/android/clockwork/apps/build_defs:android.bzl", "cw_android_library")
load("//wireless/android/clockwork/apps/build_defs:empty_manifest.bzl", "empty_manifest")
load("//java/com/google/apps/tiktok/conformance/guardrails:android_guardrails.bzl", "guardrails_tests")

package(
    default_testonly = 1,
    default_visibility = ["//wireless/android/clockwork/apps/visibility:all"],
)

cw_android_library(
    name = "testing",
    srcs = glob(["*.java"]),
    manifest = ":EmptyAndroidManifest",
    deps = [
        "//third_party/java/androidx/preference",
    ],
)

empty_manifest(
    name = "EmptyAndroidManifest",
)

guardrails_tests()

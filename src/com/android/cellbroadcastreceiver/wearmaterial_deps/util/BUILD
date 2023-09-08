load("//java/com/google/android/clockwork/sysui:deps_list.bzl", "check_sysui_deps")
load("//wireless/android/clockwork/sysui/build_defs:library.bzl", "sysui_android_library")
load("//java/com/google/apps/tiktok/conformance/guardrails:android_guardrails.bzl", "guardrails_tests")

package(default_visibility = [
    "//java/com/google/android/clockwork/common/wearable/wearmaterial:users",
])

sysui_android_library(
    name = "util",
    srcs = glob(["*.java"]),
    lint_custom_test = True,
    manifest = "AndroidManifest.xml",
    resource_files = glob(
        ["res/**"],
    ),
    deps = [
        "//third_party/java/androidx/annotation",
        "//third_party/java/androidx/core",
        "//third_party/java/androidx/wear",  # buildcleaner: keep
        "//third_party/java/checker_framework_annotations",
    ],
)

check_sysui_deps()

guardrails_tests()

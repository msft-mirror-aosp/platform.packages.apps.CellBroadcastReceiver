# Package for storing animation related constants used in SysUI.

load("//java/com/google/android/clockwork/sysui:deps_list.bzl", "check_sysui_deps")
load("//wireless/android/clockwork/sysui/build_defs:library.bzl", "sysui_android_library")
load("//java/com/google/apps/tiktok/conformance/guardrails:android_guardrails.bzl", "guardrails_tests")

package(default_visibility = [
    "//java/com/google/android/clockwork/common/wearable/wearmaterial:users",
])

sysui_android_library(
    name = "animations",
    srcs = glob(["*.java"]),
    lint_custom_test = True,
    manifest = "AndroidManifest.xml",
)

check_sysui_deps()

guardrails_tests()

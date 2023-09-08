load("//java/com/google/android/clockwork/sysui:deps_list.bzl", "check_sysui_deps")
load("//wireless/android/clockwork/sysui/build_defs:library.bzl", "sysui_android_library")
load("//java/com/google/apps/tiktok/conformance/guardrails:android_guardrails.bzl", "guardrails_tests")

package(default_visibility = [
    "//java/com/google/android/clockwork/common/wearable/wearmaterial:users",
])

sysui_android_library(
    name = "swipedismiss",
    srcs = glob(["*.java"]),
    lint_custom_test = True,
    manifest = "AndroidManifest.xml",
    visibility = [
        "//java/com/google/android/apps/camera/wear/wearappv2/controller:__pkg__",
        "//java/com/google/android/apps/camera/wear/wearappv2/ui:__pkg__",
        "//java/com/google/android/wearable/safety/emergency/contacts/ui/list:__pkg__",
        "//java/com/google/android/wearable/safety/oobe/education/steps:__pkg__",
        "//java/com/google/android/wearable/safety/settings/ui:__pkg__",
        "//javatests/com/google/android/apps/camera/wear/wearappv2/controller:__pkg__",
    ],
    deps = [
        "//third_party/java/androidx/wear",
        "//third_party/java/checker_framework_annotations",
    ],
)

check_sysui_deps()

guardrails_tests()

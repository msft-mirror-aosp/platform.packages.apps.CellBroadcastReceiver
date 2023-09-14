load("//wireless/android/clockwork/apps/build_defs:android.bzl", "cw_android_library")
load("//java/com/google/apps/tiktok/conformance/guardrails:android_guardrails.bzl", "guardrails_tests")

package(default_visibility = [
    "//java/com/google/android/clockwork/common/wearable/wearmaterial:gm3-users",
])

cw_android_library(
    name = "themeappcompat_gm3",
    lint_custom_test = True,
    manifest = "AndroidManifest.xml",
    resource_files = ["res/values/themes-gm3-appcompat.xml"],
    deps = [
        "//java/com/google/android/clockwork/common/wearable/wearmaterial/alertdialog",
        "//java/com/google/android/clockwork/common/wearable/wearmaterial/button",
        "//java/com/google/android/clockwork/common/wearable/wearmaterial/card",
        "//java/com/google/android/clockwork/common/wearable/wearmaterial/color",
        "//java/com/google/android/clockwork/common/wearable/wearmaterial/drawable",
        "//java/com/google/android/clockwork/common/wearable/wearmaterial/pageindicator",
        "//java/com/google/android/clockwork/common/wearable/wearmaterial/picker",
        "//java/com/google/android/clockwork/common/wearable/wearmaterial/preference",
        "//java/com/google/android/clockwork/common/wearable/wearmaterial/rsbcontrol",
        "//java/com/google/android/clockwork/common/wearable/wearmaterial/selectioncontrol",
        "//java/com/google/android/clockwork/common/wearable/wearmaterial/slider",
        "//java/com/google/android/clockwork/common/wearable/wearmaterial/themeappcompat:theme-appcompat",
        "//third_party/java/androidx/wear",
    ],
)

guardrails_tests()

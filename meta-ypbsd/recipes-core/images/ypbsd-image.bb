SUMMARY = "Aggregate YPBSD image target"
DESCRIPTION = "Builds the selected YPBSD operating-system images: Linux, FreeBSD, or both."
LICENSE = "MIT"

inherit allarch nopackages

YPBSD_IMAGE_TARGETS ?= "linux freebsd"
YPBSD_IMAGE_TARGETS[doc] = "Whitespace-separated OS images to build. Valid values: linux, freebsd, both."

YPBSD_IMAGE_RECIPE_linux ?= "ypbsd-linux-image"
YPBSD_IMAGE_RECIPE_freebsd ?= "freebsd-image"

python __anonymous__() {
    valid = {
        "linux": d.getVar("YPBSD_IMAGE_RECIPE_linux"),
        "freebsd": d.getVar("YPBSD_IMAGE_RECIPE_freebsd"),
    }

    requested = (d.getVar("YPBSD_IMAGE_TARGETS") or "").split()
    if "both" in requested:
        requested = [target for target in requested if target != "both"]
        requested.extend(["linux", "freebsd"])

    unknown = sorted(set(requested) - set(valid))
    if unknown:
        bb.fatal("Unsupported YPBSD_IMAGE_TARGETS value(s): %s. Valid values are: linux freebsd both" %
                 " ".join(unknown))

    deps = []
    for target in sorted(set(requested)):
        deps.append("%s:do_build" % valid[target])

    if deps:
        d.appendVarFlag("do_build", "depends", " " + " ".join(deps))
}

do_configure[noexec] = "1"
do_compile[noexec] = "1"
do_install[noexec] = "1"


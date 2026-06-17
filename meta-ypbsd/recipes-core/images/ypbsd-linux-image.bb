SUMMARY = "Minimal Linux image for YPBSD"
DESCRIPTION = "A small Linux image built by the Yocto Project/OpenEmbedded stack."
LICENSE = "MIT"

IMAGE_INSTALL = "packagegroup-core-boot ${CORE_IMAGE_EXTRA_INSTALL}"
IMAGE_LINGUAS = " "

inherit core-image

IMAGE_BASENAME = "ypbsd-linux-image"
IMAGE_ROOTFS_SIZE ?= "8192"
IMAGE_ROOTFS_EXTRA_SPACE:append = "${@bb.utils.contains('DISTRO_FEATURES', 'systemd', ' + 4096', '', d)}"


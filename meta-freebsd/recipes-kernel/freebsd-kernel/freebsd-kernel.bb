SUMMARY = "FreeBSD kernel"
DESCRIPTION = "Builds and installs the FreeBSD kernel from the configured FreeBSD source tree."
HOMEPAGE = "https://www.freebsd.org/"
LICENSE = "BSD-2-Clause & BSD-3-Clause"
LIC_FILES_CHKSUM = "file://COPYRIGHT;md5=b72344678cd27c1a842962ff019ac961"

inherit freebsd-src deploy nopackages

PROVIDES += "virtual/kernel"

do_compile[depends] += "freebsd-world:do_compile"

do_compile() {
	freebsd_do_make kernel-toolchain buildkernel
}

do_install() {
	install -d ${D}
	freebsd_do_make installkernel DESTDIR=${D}
}

do_deploy() {
	install -d ${DEPLOYDIR}
	if [ -f ${D}/boot/kernel/kernel ]; then
		install -m 0644 ${D}/boot/kernel/kernel ${DEPLOYDIR}/kernel-${MACHINE}-${FREEBSD_KERNEL_CONFIG}
		ln -sf kernel-${MACHINE}-${FREEBSD_KERNEL_CONFIG} ${DEPLOYDIR}/kernel
	fi
	tar --sort=name --numeric-owner --format=posix \
		-cf ${DEPLOYDIR}/freebsd-kernel-${MACHINE}.tar \
		-C ${D} .
}

addtask deploy after do_install before do_build

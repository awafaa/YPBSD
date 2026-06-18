SUMMARY = "FreeBSD base userspace"
DESCRIPTION = "Builds and installs FreeBSD world from the configured FreeBSD source tree."
HOMEPAGE = "https://www.freebsd.org/"
LICENSE = "BSD-2-Clause & BSD-3-Clause"
LIC_FILES_CHKSUM = "file://COPYRIGHT;md5=b72344678cd27c1a842962ff019ac961"

inherit freebsd-src deploy nopackages

PROVIDES += "virtual/freebsd-world"

FREEBSD_DESTDIR = "${D}"

do_compile() {
	freebsd_do_make buildworld
}

do_install() {
	install -d ${D}
	freebsd_do_make installworld DESTDIR=${FREEBSD_DESTDIR}
	freebsd_do_make distribution DESTDIR=${FREEBSD_DESTDIR}
}

do_deploy() {
	install -d ${DEPLOYDIR}
	tar --sort=name --numeric-owner --format=posix \
		-cf ${DEPLOYDIR}/freebsd-world-${MACHINE}.tar \
		-C ${D} .
}

addtask deploy after do_install before do_build

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

	install -d ${DEPLOYDIR}/freebsd-image-tools-${MACHINE}
	for tool in makefs mkimg etdump; do
		tool_path=""
		for bindir in \
			${FREEBSD_WORLDTMP}/legacy/usr/sbin \
			${FREEBSD_WORLDTMP}/legacy/usr/bin \
			${FREEBSD_WORLDTMP}/legacy/usr/libexec \
			${FREEBSD_WORLDTMP}/legacy/sbin \
			${FREEBSD_WORLDTMP}/legacy/bin; do
			if [ -x ${bindir}/${tool} ]; then
				tool_path=${bindir}/${tool}
				break
			fi
		done
		if [ -z "${tool_path}" ] && [ -d ${FREEBSD_WORLDTMP}/legacy ]; then
			for candidate in $(find ${FREEBSD_WORLDTMP}/legacy -type f -name ${tool}); do
				if [ -x ${candidate} ]; then
					tool_path=${candidate}
					break
				fi
			done
		fi
		if [ -n "${tool_path}" ]; then
			install -m 0755 ${tool_path} ${DEPLOYDIR}/freebsd-image-tools-${MACHINE}/${tool}
		else
			bbwarn "FreeBSD bootstrap tool '${tool}' was not found under ${FREEBSD_WORLDTMP}/legacy; ISO creation may require a host-provided ${tool}"
		fi
	done
}

addtask deploy after do_install before do_build

SUMMARY = "Minimal FreeBSD image"
DESCRIPTION = "Assembles a FreeBSD userspace and kernel into deployable image artifacts."
LICENSE = "BSD-2-Clause"

inherit deploy nopackages

INHIBIT_DEFAULT_DEPS = "1"

IMAGE_BASENAME ?= "freebsd-image-minimal"
FREEBSD_IMAGE_ROOTFS = "${WORKDIR}/rootfs"
FREEBSD_IMAGE_SIZE ?= "1024m"
FREEBSD_IMAGE_FSTYPE ?= "ufs"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install[depends] += "freebsd-world:do_deploy freebsd-kernel:do_deploy"
do_install[cleandirs] = "${D} ${FREEBSD_IMAGE_ROOTFS}"
do_install() {
	tar -xf ${DEPLOY_DIR_IMAGE}/freebsd-world-${MACHINE}.tar -C ${FREEBSD_IMAGE_ROOTFS}
	tar -xf ${DEPLOY_DIR_IMAGE}/freebsd-kernel-${MACHINE}.tar -C ${FREEBSD_IMAGE_ROOTFS}

	install -d ${FREEBSD_IMAGE_ROOTFS}/dev ${FREEBSD_IMAGE_ROOTFS}/tmp ${FREEBSD_IMAGE_ROOTFS}/var/tmp
	chmod 1777 ${FREEBSD_IMAGE_ROOTFS}/tmp ${FREEBSD_IMAGE_ROOTFS}/var/tmp

	if [ ! -f ${FREEBSD_IMAGE_ROOTFS}/etc/fstab ]; then
		install -d ${FREEBSD_IMAGE_ROOTFS}/etc
		cat > ${FREEBSD_IMAGE_ROOTFS}/etc/fstab <<'EOF'
/dev/gpt/rootfs	/	ufs	rw	1	1
EOF
	fi

	cp -a ${FREEBSD_IMAGE_ROOTFS}/. ${D}/
}

do_deploy() {
	install -d ${DEPLOYDIR}

	tar --sort=name --numeric-owner --format=posix \
		-cf ${DEPLOYDIR}/${IMAGE_BASENAME}-${MACHINE}.tar \
		-C ${FREEBSD_IMAGE_ROOTFS} .

	if command -v makefs >/dev/null 2>&1; then
		makefs -t ${FREEBSD_IMAGE_FSTYPE} -s ${FREEBSD_IMAGE_SIZE} \
			${DEPLOYDIR}/${IMAGE_BASENAME}-${MACHINE}.${FREEBSD_IMAGE_FSTYPE}.img \
			${FREEBSD_IMAGE_ROOTFS}
	else
		bbwarn "makefs was not found; deployed tarball only. Install FreeBSD makefs to produce a ${FREEBSD_IMAGE_FSTYPE} image."
	fi

	ln -sf ${IMAGE_BASENAME}-${MACHINE}.tar ${DEPLOYDIR}/${IMAGE_BASENAME}.tar
}

addtask deploy after do_install before do_build

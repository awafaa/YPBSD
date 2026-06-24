SUMMARY = "Minimal FreeBSD image"
DESCRIPTION = "Assembles a FreeBSD userspace and kernel into deployable image artifacts."
LICENSE = "BSD-2-Clause"

inherit deploy nopackages

INHIBIT_DEFAULT_DEPS = "1"

IMAGE_BASENAME ?= "freebsd-image-minimal"
FREEBSD_IMAGE_ROOTFS = "${WORKDIR}/rootfs"
FREEBSD_IMAGE_ISODIR = "${WORKDIR}/iso-root"
FREEBSD_IMAGE_SIZE ?= "1024m"
FREEBSD_IMAGE_FSTYPE ?= "ufs"
FREEBSD_ISO_BOOTABLE ?= "1"
FREEBSD_ISO_LABEL ?= "FREEBSD_${FREEBSD_ABI_VERSION}_${FREEBSD_TARGET}"
FREEBSD_ISO_IMAGE ?= "${IMAGE_BASENAME}-${MACHINE}.iso"
FREEBSD_IMAGE_TOOLSDIR ?= "${DEPLOY_DIR_IMAGE}/freebsd-image-tools-${MACHINE}"

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

do_deploy[cleandirs] = "${FREEBSD_IMAGE_ISODIR}"
do_deploy() {
	install -d ${DEPLOYDIR}

	tar --sort=name --numeric-owner --format=posix \
		-cf ${DEPLOYDIR}/${IMAGE_BASENAME}-${MACHINE}.tar \
		-C ${FREEBSD_IMAGE_ROOTFS} .

	if [ ! -x ${FREEBSD_SRC_DIR}/release/${FREEBSD_TARGET}/mkisoimages.sh ]; then
		bbfatal "No FreeBSD ISO builder found for TARGET=${FREEBSD_TARGET}: ${FREEBSD_SRC_DIR}/release/${FREEBSD_TARGET}/mkisoimages.sh"
	fi

	iso_required_tools="makefs"
	if [ "${FREEBSD_ISO_BOOTABLE}" = "1" ]; then
		iso_required_tools="${iso_required_tools} mkimg etdump"
	fi

	iso_tools_path="${FREEBSD_IMAGE_TOOLSDIR}:$PATH"
	missing_iso_tools=""
	for tool in ${iso_required_tools}; do
		if ! PATH="${iso_tools_path}" command -v ${tool} >/dev/null 2>&1; then
			missing_iso_tools="${missing_iso_tools} ${tool}"
		fi
	done
	if [ -n "${missing_iso_tools}" ]; then
		bbfatal "Missing tool(s) required to create FreeBSD ISO:${missing_iso_tools}. Expected FreeBSD bootstrap tools in ${FREEBSD_IMAGE_TOOLSDIR}, or host-provided makefs, mkimg, and etdump."
	fi

	PATH="${iso_tools_path}" makefs -t ${FREEBSD_IMAGE_FSTYPE} -s ${FREEBSD_IMAGE_SIZE} \
		${DEPLOYDIR}/${IMAGE_BASENAME}-${MACHINE}.${FREEBSD_IMAGE_FSTYPE}.img \
		${FREEBSD_IMAGE_ROOTFS}

	install -d ${FREEBSD_IMAGE_ISODIR}
	cp -a ${FREEBSD_IMAGE_ROOTFS}/. ${FREEBSD_IMAGE_ISODIR}/

	if [ "${FREEBSD_ISO_BOOTABLE}" = "1" ]; then
		if [ ! -f ${FREEBSD_IMAGE_ISODIR}/boot/loader.efi ]; then
			bbfatal "Cannot create a bootable FreeBSD ISO because ${FREEBSD_IMAGE_ISODIR}/boot/loader.efi is missing"
		fi
		iso_boot_arg="-b"
	else
		iso_boot_arg=""
	fi

	PATH="${iso_tools_path}" TARGET=${FREEBSD_TARGET} MAKEFS=makefs MKIMG=mkimg ETDUMP=etdump \
		sh ${FREEBSD_SRC_DIR}/release/${FREEBSD_TARGET}/mkisoimages.sh \
		${iso_boot_arg} ${FREEBSD_ISO_LABEL} \
		${DEPLOYDIR}/${FREEBSD_ISO_IMAGE} \
		${FREEBSD_IMAGE_ISODIR}

	ln -sf ${IMAGE_BASENAME}-${MACHINE}.tar ${DEPLOYDIR}/${IMAGE_BASENAME}.tar
	ln -sf ${FREEBSD_ISO_IMAGE} ${DEPLOYDIR}/${IMAGE_BASENAME}.iso
}

addtask deploy after do_install before do_build

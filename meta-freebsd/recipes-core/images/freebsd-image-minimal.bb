SUMMARY = "Minimal FreeBSD image"
DESCRIPTION = "Assembles a FreeBSD userspace and kernel into deployable image artifacts."
LICENSE = "BSD-2-Clause"

inherit deploy nopackages

INHIBIT_DEFAULT_DEPS = "1"

IMAGE_BASENAME ?= "freebsd-image-minimal"
FREEBSD_IMAGE_ROOTFS = "${WORKDIR}/rootfs"
FREEBSD_IMAGE_ISODIR = "${WORKDIR}/iso-root"
FREEBSD_IMAGE_SIZE ?= "auto"
FREEBSD_IMAGE_EXTRA_SPACE ?= "65536"
FREEBSD_IMAGE_OVERHEAD_FACTOR ?= "1.3"
# FreeBSD makefs uses "ffs" for UFS filesystem images; keep the artifact named .ufs.img.
FREEBSD_IMAGE_FSTYPE ?= "ffs"
FREEBSD_IMAGE_EXTENSION ?= "ufs"
FREEBSD_ISO_BOOTABLE ?= "1"
FREEBSD_ISO_LABEL ?= "FREEBSD_${FREEBSD_ABI_VERSION}_${FREEBSD_TARGET}"
FREEBSD_ISO_IMAGE ?= "${IMAGE_BASENAME}-${MACHINE}.iso"
FREEBSD_IMAGE_TOOLSDIR ?= "${DEPLOY_DIR_IMAGE}/freebsd-image-tools-${MACHINE}"
FREEBSD_ISO_TOOLSDIR ?= "${WORKDIR}/iso-tools"
FREEBSD_WORLD_WORKDIR ?= "${BASE_WORKDIR}/${MULTIMACH_TARGET_SYS}/freebsd-world/1.0"
FREEBSD_WORLD_OBJROOT ?= "${FREEBSD_WORLD_WORKDIR}/build/obj${FREEBSD_SRC_DIR}/${FREEBSD_TARGET}.${FREEBSD_TARGET_ARCH}"
FREEBSD_WORLD_BOOTSTRAP_TOOLSDIR ?= "${FREEBSD_WORLD_OBJROOT}/tmp/legacy"

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

do_deploy[depends] += "freebsd-world:do_deploy freebsd-kernel:do_deploy freebsd-image-tools-native:do_populate_sysroot"
do_deploy[cleandirs] = "${DEPLOYDIR} ${FREEBSD_IMAGE_ISODIR} ${FREEBSD_ISO_TOOLSDIR}"
do_deploy() {
	install -d ${DEPLOYDIR}

	tar --sort=name --numeric-owner --format=posix \
		-cf ${DEPLOYDIR}/${IMAGE_BASENAME}-${MACHINE}.tar \
		-C ${FREEBSD_IMAGE_ROOTFS} .

	if [ ! -f ${FREEBSD_SRC_DIR}/release/${FREEBSD_TARGET}/mkisoimages.sh ]; then
		bbfatal "No FreeBSD ISO builder found for TARGET=${FREEBSD_TARGET}: ${FREEBSD_SRC_DIR}/release/${FREEBSD_TARGET}/mkisoimages.sh"
	fi

	iso_required_tools="makefs"
	if [ "${FREEBSD_ISO_BOOTABLE}" = "1" ]; then
		iso_required_tools="${iso_required_tools} mkimg etdump"
	fi

	freebsd_find_image_tool() {
		tool="$1"
		for dir in \
			${FREEBSD_IMAGE_TOOLSDIR} \
			${FREEBSD_WORLD_BOOTSTRAP_TOOLSDIR}/usr/sbin \
			${FREEBSD_WORLD_BOOTSTRAP_TOOLSDIR}/usr/bin \
			${FREEBSD_WORLD_BOOTSTRAP_TOOLSDIR}/usr/libexec \
			${FREEBSD_WORLD_BOOTSTRAP_TOOLSDIR}/sbin \
			${FREEBSD_WORLD_BOOTSTRAP_TOOLSDIR}/bin; do
			if [ -x "${dir}/${tool}" ]; then
				echo "${dir}/${tool}"
				return 0
			fi
		done
		if [ -d ${FREEBSD_WORLD_BOOTSTRAP_TOOLSDIR} ]; then
			candidate="$(find ${FREEBSD_WORLD_BOOTSTRAP_TOOLSDIR} -type f -name "${tool}" -perm /111 | head -n 1)"
			if [ -n "${candidate}" ]; then
				echo "${candidate}"
				return 0
			fi
		fi
		command -v "${tool}" || return 1
	}

	install -d ${FREEBSD_ISO_TOOLSDIR}
	missing_iso_tools=""
	for tool in ${iso_required_tools}; do
		tool_path="$(freebsd_find_image_tool ${tool} || true)"
		if [ -n "${tool_path}" ]; then
			install -m 0755 "${tool_path}" ${FREEBSD_ISO_TOOLSDIR}/${tool}
		else
			missing_iso_tools="${missing_iso_tools} ${tool}"
		fi
	done
	if [ -n "${missing_iso_tools}" ]; then
		if [ -d "${FREEBSD_IMAGE_TOOLSDIR}" ]; then
			bbnote "Contents of ${FREEBSD_IMAGE_TOOLSDIR}: $(ls -la ${FREEBSD_IMAGE_TOOLSDIR})"
		else
			bbnote "${FREEBSD_IMAGE_TOOLSDIR} does not exist"
		fi
		if [ -d "${FREEBSD_WORLD_BOOTSTRAP_TOOLSDIR}" ]; then
			bbnote "Searched FreeBSD bootstrap tools under ${FREEBSD_WORLD_BOOTSTRAP_TOOLSDIR}"
		else
			bbnote "${FREEBSD_WORLD_BOOTSTRAP_TOOLSDIR} does not exist"
		fi
		bbfatal "Missing tool(s) required to create FreeBSD ISO:${missing_iso_tools}. Expected FreeBSD bootstrap tools in ${FREEBSD_IMAGE_TOOLSDIR} or ${FREEBSD_WORLD_BOOTSTRAP_TOOLSDIR}, or host-provided makefs, mkimg, and etdump."
	fi

	iso_tools_path="${FREEBSD_ISO_TOOLSDIR}:$PATH"
	if [ "${FREEBSD_IMAGE_SIZE}" = "auto" ]; then
		rootfs_kb="$(du -sk ${FREEBSD_IMAGE_ROOTFS} | awk '{ print $1 }')"
		image_size_kb="$(awk \
			-v rootfs_kb="${rootfs_kb}" \
			-v overhead="${FREEBSD_IMAGE_OVERHEAD_FACTOR}" \
			-v extra="${FREEBSD_IMAGE_EXTRA_SPACE}" \
			'BEGIN {
				size = (rootfs_kb * overhead) + extra
				printf "%d", int((size + 16383) / 16384) * 16384
			}')"
		image_size_arg="${image_size_kb}k"
	else
		image_size_arg="${FREEBSD_IMAGE_SIZE}"
	fi

	PATH="${iso_tools_path}" makefs -t ${FREEBSD_IMAGE_FSTYPE} -s "${image_size_arg}" \
		${DEPLOYDIR}/${IMAGE_BASENAME}-${MACHINE}.${FREEBSD_IMAGE_EXTENSION}.img \
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

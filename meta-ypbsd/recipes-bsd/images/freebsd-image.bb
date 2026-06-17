SUMMARY = "FreeBSD root filesystem image"
DESCRIPTION = "Builds FreeBSD from the checked-in freebsd-src tree and deploys a rootfs tarball."
LICENSE = "BSD-2-Clause & BSD-3-Clause"
LIC_FILES_CHKSUM = "file://COPYRIGHT;md5=b72344678cd27c1a842962ff019ac961"

inherit deploy nopackages

INHIBIT_DEFAULT_DEPS = "1"

FREEBSD_SRC ?= "${@os.path.abspath(os.path.join(d.getVar('THISDIR'), '..', '..', '..', 'freebsd-src'))}"
S = "${FREEBSD_SRC}"
B = "${WORKDIR}/build"

FREEBSD_MAKE ?= "make"
FREEBSD_PARALLEL_MAKE ?= "${PARALLEL_MAKE}"
FREEBSD_BUILDWORLD ?= "1"
FREEBSD_BUILDKERNEL ?= "1"
FREEBSD_KERNEL_CONFIG ?= "GENERIC"
FREEBSD_INSTALL_OPTS ?= "DB_FROM_SRC=1 NO_FSCHG=1"
FREEBSD_IMAGE_BASENAME ?= "${PN}-${MACHINE}"

def ypbsd_freebsd_target(d):
    arch = d.getVar("TARGET_ARCH")
    return {
        "x86_64": "amd64",
        "i586": "i386",
        "i686": "i386",
        "aarch64": "arm64",
        "arm": "arm",
        "riscv64": "riscv",
        "powerpc": "powerpc",
        "powerpc64": "powerpc",
        "powerpc64le": "powerpc",
    }.get(arch, arch)

def ypbsd_freebsd_target_arch(d):
    arch = d.getVar("TARGET_ARCH")
    return {
        "x86_64": "amd64",
        "i586": "i386",
        "i686": "i386",
        "aarch64": "aarch64",
        "arm": "armv7",
        "riscv64": "riscv64",
        "powerpc": "powerpc",
        "powerpc64": "powerpc64",
        "powerpc64le": "powerpc64le",
    }.get(arch, arch)

FREEBSD_TARGET ?= "${@ypbsd_freebsd_target(d)}"
FREEBSD_TARGET_ARCH ?= "${@ypbsd_freebsd_target_arch(d)}"

do_configure() {
    if [ ! -d "${S}" ]; then
        bbfatal "FreeBSD source tree not found at ${S}"
    fi

    freebsd_make="${FREEBSD_MAKE}"
    freebsd_make_cmd="${freebsd_make%% *}"
    if ! command -v "${freebsd_make_cmd}" >/dev/null 2>&1; then
        bbfatal "Required FreeBSD make tool '${freebsd_make_cmd}' was not found. Set FREEBSD_MAKE to a BSD make implementation, such as bmake."
    fi
}

do_compile() {
    install -d "${B}/obj"

    if [ "${FREEBSD_BUILDWORLD}" = "1" ]; then
        ${FREEBSD_MAKE} -C "${S}" ${FREEBSD_PARALLEL_MAKE} \
            TARGET="${FREEBSD_TARGET}" \
            TARGET_ARCH="${FREEBSD_TARGET_ARCH}" \
            MAKEOBJDIRPREFIX="${B}/obj" \
            buildworld
    fi

    if [ "${FREEBSD_BUILDKERNEL}" = "1" ]; then
        ${FREEBSD_MAKE} -C "${S}" ${FREEBSD_PARALLEL_MAKE} \
            TARGET="${FREEBSD_TARGET}" \
            TARGET_ARCH="${FREEBSD_TARGET_ARCH}" \
            MAKEOBJDIRPREFIX="${B}/obj" \
            KERNCONF="${FREEBSD_KERNEL_CONFIG}" \
            buildkernel
    fi
}

fakeroot do_install() {
    rm -rf "${D}"
    install -d "${D}"

    if [ "${FREEBSD_BUILDWORLD}" != "1" ]; then
        bbfatal "FREEBSD_BUILDWORLD must be enabled to create a FreeBSD root filesystem"
    fi

    ${FREEBSD_MAKE} -C "${S}" \
        TARGET="${FREEBSD_TARGET}" \
        TARGET_ARCH="${FREEBSD_TARGET_ARCH}" \
        MAKEOBJDIRPREFIX="${B}/obj" \
        DESTDIR="${D}" \
        ${FREEBSD_INSTALL_OPTS} \
        installworld distribution

    if [ "${FREEBSD_BUILDKERNEL}" = "1" ]; then
        ${FREEBSD_MAKE} -C "${S}" \
            TARGET="${FREEBSD_TARGET}" \
            TARGET_ARCH="${FREEBSD_TARGET_ARCH}" \
            MAKEOBJDIRPREFIX="${B}/obj" \
            DESTDIR="${D}" \
            KERNCONF="${FREEBSD_KERNEL_CONFIG}" \
            ${FREEBSD_INSTALL_OPTS} \
            installkernel
    fi
}

fakeroot do_deploy() {
    install -d "${DEPLOYDIR}"

    tar -C "${D}" -czf "${DEPLOYDIR}/${FREEBSD_IMAGE_BASENAME}.rootfs.tar.gz" .
    ln -sf "${FREEBSD_IMAGE_BASENAME}.rootfs.tar.gz" "${DEPLOYDIR}/${PN}-${MACHINE}.rootfs.tar.gz"

    cat > "${DEPLOYDIR}/${FREEBSD_IMAGE_BASENAME}.manifest" <<EOF
PN=${PN}
MACHINE=${MACHINE}
TARGET_ARCH=${TARGET_ARCH}
FREEBSD_TARGET=${FREEBSD_TARGET}
FREEBSD_TARGET_ARCH=${FREEBSD_TARGET_ARCH}
FREEBSD_KERNEL_CONFIG=${FREEBSD_KERNEL_CONFIG}
FREEBSD_SRC=${S}
EOF
}

addtask deploy after do_install before do_build

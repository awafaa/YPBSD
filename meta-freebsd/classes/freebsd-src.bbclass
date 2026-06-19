FREEBSD_SRC_DIR ?= "${@os.path.abspath(os.path.join(d.getVar('TOPDIR'), '..', 'freebsd-src'))}"

INHIBIT_DEFAULT_DEPS = "1"

FREEBSD_TARGET ?= "${TARGET_ARCH}"
FREEBSD_TARGET_ARCH ?= "${TARGET_ARCH}"
FREEBSD_KERNEL_CONFIG ?= "GENERIC"
FREEBSD_MAKE ?= "bmake"
FREEBSD_MAKE_JOBS ?= "${@oe.utils.parallel_make_argument(d, '-j %d')}"
FREEBSD_MAKE_FLAGS ?= "-s"
FREEBSD_MAKE_CONF ?= "${WORKDIR}/make.conf"
FREEBSD_SRC_ENV ?= "MAKEOBJDIRPREFIX=${B}/obj SRCCONF=/dev/null __MAKE_CONF=${FREEBSD_MAKE_CONF}"
FREEBSD_SRC_ARGS ?= "TARGET=${FREEBSD_TARGET} TARGET_ARCH=${FREEBSD_TARGET_ARCH} KERNCONF=${FREEBSD_KERNEL_CONFIG}"
FREEBSD_ENV_UNSET ?= " \
    AR AS CC CCLD CPP CXX LD NM OBJCOPY OBJDUMP RANLIB READELF STRIP \
    XAR XAS XCC XCPP XCXX XLD XNM XOBJCOPY XOBJDUMP XRANLIB XREADELF XSTRIP \
    CFLAGS CPPFLAGS CXXFLAGS LDFLAGS CROSS_COMPILE TARGET_PREFIX \
    TARGET_CC_ARCH TARGET_LD_ARCH TARGET_AS_ARCH TOOLCHAIN_OPTIONS \
"

SRC_URI = ""
S = "${FREEBSD_SRC_DIR}"
B = "${WORKDIR}/build"

do_fetch[noexec] = "1"
do_unpack[noexec] = "1"
do_patch[noexec] = "1"

do_validate_freebsd_src() {
	if [ ! -f "${S}/Makefile" ] || [ ! -f "${S}/COPYRIGHT" ]; then
		bbfatal "FREEBSD_SRC_DIR must point to a FreeBSD source tree; got '${S}'. Set FREEBSD_SRC_DIR in conf/local.conf if your freebsd-src checkout is elsewhere."
	fi
}

addtask validate_freebsd_src after do_unpack before do_configure

do_configure[cleandirs] = "${B}"
do_configure() {
	install -d ${B}
	cat > ${FREEBSD_MAKE_CONF} <<'EOF'
WITHOUT_DEBUG_FILES=yes
WITHOUT_TESTS=yes
EOF
}

freebsd_do_make() {
	unset ${FREEBSD_ENV_UNSET}
	${FREEBSD_SRC_ENV} ${FREEBSD_MAKE} ${FREEBSD_MAKE_FLAGS} -C "${S}" ${FREEBSD_SRC_ARGS} ${FREEBSD_MAKE_JOBS} "$@"
}

do_compile[network] = "0"

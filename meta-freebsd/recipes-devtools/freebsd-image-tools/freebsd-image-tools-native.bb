SUMMARY = "FreeBSD native image creation tools"
DESCRIPTION = "Builds makefs, mkimg, and etdump from the local FreeBSD source tree for use by image recipes."
HOMEPAGE = "https://www.freebsd.org/"
LICENSE = "BSD-2-Clause & BSD-3-Clause"
LIC_FILES_CHKSUM = "file://COPYRIGHT;md5=b72344678cd27c1a842962ff019ac961"

inherit native nopackages

INHIBIT_DEFAULT_DEPS = "1"
DEPENDS += "libarchive-native libbsd-native"

FREEBSD_SRC_DIR ?= "${@os.path.abspath(os.path.join(d.getVar('TOPDIR'), '..', 'freebsd-src'))}"
FREEBSD_TARGET ?= "arm64"
FREEBSD_TARGET_ARCH ?= "aarch64"
FREEBSD_MAKE ?= "bmake"
FREEBSD_MAKE_JOBS_LIMIT ?= "1"
FREEBSD_MAKE_JOBS ?= "-j ${FREEBSD_MAKE_JOBS_LIMIT}"
FREEBSD_MAKE_CONF ?= "${WORKDIR}/make.conf"
FREEBSD_OBJROOT ?= "${B}/obj${FREEBSD_SRC_DIR}/${FREEBSD_TARGET}.${FREEBSD_TARGET_ARCH}"
FREEBSD_WORLDTMP ?= "${FREEBSD_OBJROOT}/tmp"
FREEBSD_SRC_ENV ?= "MAKEOBJDIRPREFIX=${B}/obj SRCCONF=/dev/null __MAKE_CONF=${FREEBSD_MAKE_CONF} LD_LIBRARY_PATH=${STAGING_LIBDIR_NATIVE}"
FREEBSD_SRC_ARGS ?= "TARGET=${FREEBSD_TARGET} TARGET_ARCH=${FREEBSD_TARGET_ARCH}"
FREEBSD_ENV_UNSET ?= " \
    AR AS CC CCLD CPP CXX LD NM OBJCOPY OBJDUMP RANLIB READELF STRIP \
    XAR XAS XCC XCPP XCXX XLD XNM XOBJCOPY XOBJDUMP XRANLIB XREADELF XSTRIP \
    BUILD_CFLAGS BUILD_CPPFLAGS BUILD_CXXFLAGS BUILD_LDFLAGS \
    CFLAGS CPPFLAGS CXXFLAGS LDFLAGS \
    TARGET_CFLAGS TARGET_CPPFLAGS TARGET_CXXFLAGS TARGET_LDFLAGS \
    CROSS_COMPILE DEBUG_PREFIX_MAP SELECTED_OPTIMIZATION SECURITY_CFLAGS \
    SOURCE_DATE_EPOCH \
    TARGET_PREFIX TARGET_CC_ARCH TARGET_LD_ARCH TARGET_AS_ARCH TOOLCHAIN_OPTIONS \
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
WITHOUT_CAROOT=yes
WITHOUT_CLANG=yes
WITHOUT_CLANG_BOOTSTRAP=yes
WITHOUT_DEBUG_FILES=yes
WITHOUT_ICONV=yes
WITHOUT_KERBEROS=yes
WITHOUT_LLD=yes
WITHOUT_LLD_BOOTSTRAP=yes
WITHOUT_LLDB=yes
WITHOUT_LLVM_BINUTILS=yes
WITHOUT_LLVM_BINUTILS_BOOTSTRAP=yes
WITHOUT_OPENSSL=yes
WITHOUT_TESTS=yes
WITHOUT_ZFS=yes
WITH_DISK_IMAGE_TOOLS_BOOTSTRAP=yes
EOF
}

do_compile() {
	unset ${FREEBSD_ENV_UNSET}
	${FREEBSD_SRC_ENV} ${FREEBSD_MAKE} -s -DBOOTSTRAP_ALL_TOOLS \
		-C "${S}" -f Makefile.inc1 ${FREEBSD_SRC_ARGS} ${FREEBSD_MAKE_JOBS} \
		_worldtmp _legacy _bootstrap-tools
}
do_compile[network] = "0"

do_install() {
	install -d ${D}${bindir}
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
		if [ -z "${tool_path}" ]; then
			for candidate in $(find ${B} -type f -name ${tool}); do
				if [ -x ${candidate} ]; then
					tool_path=${candidate}
					break
				fi
			done
		fi
		if [ -z "${tool_path}" ]; then
			bbfatal "FreeBSD image tool '${tool}' was not found under ${B}"
		fi
		install -m 0755 ${tool_path} ${D}${bindir}/${tool}
	done
}

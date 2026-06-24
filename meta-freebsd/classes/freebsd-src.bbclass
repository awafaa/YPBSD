FREEBSD_SRC_DIR ?= "${@os.path.abspath(os.path.join(d.getVar('TOPDIR'), '..', 'freebsd-src'))}"

INHIBIT_DEFAULT_DEPS = "1"
DEPENDS += "libarchive-native"

FREEBSD_TARGET ?= "${TARGET_ARCH}"
FREEBSD_TARGET_ARCH ?= "${TARGET_ARCH}"
FREEBSD_KERNEL_CONFIG ?= "GENERIC"
FREEBSD_MAKE ?= "bmake"
FREEBSD_MAKE_JOBS_LIMIT ?= "1"
FREEBSD_MAKE_JOBS ?= "-j ${FREEBSD_MAKE_JOBS_LIMIT}"
FREEBSD_MAKE_FLAGS ?= "-s -DBOOTSTRAP_ALL_TOOLS"
FREEBSD_MAKE_CONF ?= "${WORKDIR}/make.conf"
FREEBSD_SRC_LOCK ?= "${TMPDIR}/freebsd-src.lock"
FREEBSD_OBJROOT ?= "${B}/obj${S}/${FREEBSD_TARGET}.${FREEBSD_TARGET_ARCH}"
FREEBSD_WORLDTMP ?= "${FREEBSD_OBJROOT}/tmp"
FREEBSD_LEX ?= "flex"
FREEBSD_YACC ?= "yacc"
FREEBSD_BOOTSTRAP_CFLAGS ?= "-I${STAGING_INCDIR_NATIVE}"
FREEBSD_BOOTSTRAP_LDFLAGS ?= "-L${STAGING_LIBDIR_NATIVE} -Wl,-rpath,${STAGING_LIBDIR_NATIVE} -Wl,-rpath-link,${STAGING_LIBDIR_NATIVE}"
FREEBSD_SRC_ENV ?= "MAKEOBJDIRPREFIX=${B}/obj SRCCONF=/dev/null __MAKE_CONF=${FREEBSD_MAKE_CONF} LEX=${FREEBSD_LEX} YACC=${FREEBSD_YACC} LD_LIBRARY_PATH=${STAGING_LIBDIR_NATIVE}"
FREEBSD_SRC_ARGS ?= "TARGET=${FREEBSD_TARGET} TARGET_ARCH=${FREEBSD_TARGET_ARCH} KERNCONF=${FREEBSD_KERNEL_CONFIG}"
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

freebsd_apply_source_patches() {
	if ! grep -Fq '__ssp_real(ctermid)(char *buf)' ${S}/lib/libc/gen/ctermid.c; then
		bbnote "Applying FreeBSD ctermid SSP compatibility patch"
		patch -d ${S} -N -p1 < ${FREEBSD_LAYERDIR}/patches/freebsd-src/0001-libc-ctermid-avoid-ssp-parameter-name-collision.patch || true
	fi
	if ! grep -Fq 'return (buf);' ${S}/lib/libc/gen/ctermid.c; then
		bbnote "Rewriting FreeBSD ctermid SSP compatibility change directly"
		sed -i \
			-e '/__ssp_real(ctermid)(char \*s)/,/^}/ {
				s/__ssp_real(ctermid)(char \*s)/__ssp_real(ctermid)(char *buf)/
				s/if (s == NULL)/if (buf == NULL)/
				s/s = def/buf = def/
				s/strcpy(s,/strcpy(buf,/
				s/kern.devname", s +/kern.devname", buf +/
				s/return (s);/return (buf);/
			}' \
			-e '/__ssp_real(ctermid_r)(char \*s)/,/^}/ {
				s/__ssp_real(ctermid_r)(char \*s)/__ssp_real(ctermid_r)(char *buf)/
				s/s != NULL/buf != NULL/
				s/ctermid(s)/ctermid(buf)/
			}' \
			${S}/lib/libc/gen/ctermid.c
	fi
	if ! grep -Fq 'return (buf);' ${S}/lib/libc/gen/ctermid.c; then
		bbfatal "FreeBSD ctermid SSP compatibility patch was not applied to ${S}/lib/libc/gen/ctermid.c"
	fi
	if grep -Fq 'return (s);' ${S}/lib/libc/gen/ctermid.c; then
		bbfatal "FreeBSD ctermid.c still contains the incompatible 'return (s);' line"
	fi
}

do_configure[cleandirs] = "${B}"
do_configure() {
	install -d ${B}
	freebsd_apply_source_patches
	cat > ${FREEBSD_MAKE_CONF} <<'EOF'
WITHOUT_CAROOT=yes
WITHOUT_DEBUG_FILES=yes
WITHOUT_LIB32=yes
WITHOUT_SSP=yes
WITHOUT_TESTS=yes
FORTIFY_SOURCE=0
CFLAGS+=-U_FORTIFY_SOURCE -D_FORTIFY_SOURCE=0
CXXFLAGS+=-U_FORTIFY_SOURCE -D_FORTIFY_SOURCE=0
.if defined(BOOTSTRAPPING)
CFLAGS+=${FREEBSD_BOOTSTRAP_CFLAGS}
LDFLAGS+=${FREEBSD_BOOTSTRAP_LDFLAGS}
.endif
EOF
}

freebsd_do_make() {
	freebsd_apply_source_patches
	unset ${FREEBSD_ENV_UNSET}
	${FREEBSD_SRC_ENV} ${FREEBSD_MAKE} ${FREEBSD_MAKE_FLAGS} -C "${S}" ${FREEBSD_SRC_ARGS} ${FREEBSD_MAKE_JOBS} "$@"
}

do_compile[network] = "0"
do_compile[lockfiles] += "${FREEBSD_SRC_LOCK}"
do_install[lockfiles] += "${FREEBSD_SRC_LOCK}"

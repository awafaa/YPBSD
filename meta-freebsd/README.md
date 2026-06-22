# meta-freebsd

This layer lets a developer use the normal Yocto Project/OpenEmbedded setup and
BitBake workflow to build either a Linux image from OE-Core or a FreeBSD image
from the checked-out `freebsd-src` tree.

## Quick Start

From the repository root:

```sh
source openembedded-core/oe-init-build-env build
bitbake-layers add-layer ../meta-freebsd
```

Or start with the FreeBSD defaults:

```sh
TEMPLATECONF=../meta-freebsd/templates/default source openembedded-core/oe-init-build-env build-freebsd
```

Build Linux with the normal OE-Core workflow:

```sh
MACHINE=qemux86-64 DISTRO=nodistro bitbake core-image-minimal
```

Build FreeBSD with the same BitBake workflow:

```sh
MACHINE=freebsd-amd64 DISTRO=freebsd bitbake freebsd-image-minimal
```

The FreeBSD recipes use `FREEBSD_SRC_DIR = "${TOPDIR}/../freebsd-src"` by
default, so the local `freebsd-src` checkout next to `openembedded-core` is used
directly without fetching from the network.

The FreeBSD recipes use BSD make and FreeBSD's bootstrap toolchain. On Linux
hosts, install `bmake`, `flex`, `m4`, and `yacc`/`bison`; install `makefs` as
well if you want the `.ufs.img` artifact in addition to the tarball.
FreeBSD bootstrap tools that need libarchive use OE-Core's `libarchive-native`
sysroot instead of host distribution headers.

The default build disables FreeBSD's CA root rehash step with `WITHOUT_CAROOT=yes`
so the bootstrap does not depend on host OpenSSL development headers. It also
sets `WITHOUT_LIB32=yes` to avoid building 32-bit compatibility objects for the
initial FreeBSD image bring-up.
The FreeBSD source build is throttled to one `bmake` job by default to keep LLVM
bootstrap memory use predictable. Set `FREEBSD_MAKE_JOBS_LIMIT` in
`conf/local.conf` after the first successful build if your host has enough RAM.
World compilation is ordered before kernel compilation for image builds so a
kernel failure does not leave a long-running world build active in the
background.

## Outputs

`freebsd-image-minimal` deploys:

- `freebsd-image-minimal-${MACHINE}.tar`, containing FreeBSD world and kernel.
- `freebsd-image-minimal-${MACHINE}.ufs.img`, when the host provides `makefs`.
- `kernel-${MACHINE}-${FREEBSD_KERNEL_CONFIG}`, from the `freebsd-kernel` recipe.

## Supported Machines

- `freebsd-amd64`: `TARGET=amd64`, `TARGET_ARCH=amd64`, `KERNCONF=GENERIC`.
- `freebsd-arm64`: `TARGET=arm64`, `TARGET_ARCH=aarch64`, `KERNCONF=GENERIC`.

Override `FREEBSD_TARGET`, `FREEBSD_TARGET_ARCH`, or `FREEBSD_KERNEL_CONFIG` in
`conf/local.conf` for other FreeBSD target combinations.

`TARGET_OS` defaults to `freebsd${DISTRO_VERSION}` so GNU tooling sees a
versioned FreeBSD target tuple such as `aarch64-oe-freebsd14`.

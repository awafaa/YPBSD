# meta-freebsd

This layer lets a developer use the normal Yocto Project/OpenEmbedded setup and
BitBake workflow to build either a Linux image from OE-Core or a FreeBSD image
from the checked-out `freebsd-src` tree.

### Repositories required
[Bitbake](http://git.openembedded.org/bitbake), [OpenEmbedded Core](https://git.openembedded.org/openembedded-core) and [FreeBSD Source](https://github.com/freebsd/freebsd-src)

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

The FreeBSD image recipe uses `FREEBSD_SRC_URI = "file://${TOPDIR}/../freebsd-src"`
by default, so the local `freebsd-src` checkout next to `openembedded-core` is
used without fetching from the network.

The FreeBSD recipes use BSD make. On Linux hosts, install `bmake`; install
`makefs` as well if you want the `.ufs.img` artifact in addition to the tarball.

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

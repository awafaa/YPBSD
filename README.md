# YPBSD Yocto Layer

This requires [BitBake](https://git.openembedded.org/bitbake), [OpenEmbedded-Core](https://git.openembedded.org/openembedded-core), [FreeBSD source](https://cgit.freebsd.org/src).

## Build Setup

Run these commands on a Yocto-supported Linux build host or container.

Create a build directory with the YPBSD template:

```sh
export TEMPLATECONF=../meta-ypbsd/conf/templates/default
. openembedded-core/oe-init-build-env build
```

Then build one of the image targets:

```sh
bitbake ypbsd-linux-image
bitbake freebsd-image
bitbake ypbsd-image
```

`ypbsd-image` is an aggregate target. It builds the images listed in
`YPBSD_IMAGE_TARGETS`, which accepts `linux`, `freebsd`, or `both`:

```conf
YPBSD_IMAGE_TARGETS = "linux"
YPBSD_IMAGE_TARGETS = "freebsd"
YPBSD_IMAGE_TARGETS = "linux freebsd"
```

## Images

`ypbsd-linux-image` is a minimal OE-Core Linux image based on
`packagegroup-core-boot`.

`freebsd-image` builds the checked-in `freebsd-src` tree with FreeBSD's own
`buildworld`, `buildkernel`, `installworld`, `distribution`, and
`installkernel` targets. The deployed artifact is a root filesystem tarball in
`tmp/deploy/images/${MACHINE}`.

On hosts where `make` is not BSD make, add this to `conf/local.conf`:

```conf
FREEBSD_MAKE = "bmake"
```

The FreeBSD recipe maps common Yocto architectures to FreeBSD `TARGET` and
`TARGET_ARCH` values. Override them in `conf/local.conf` for a custom port:

```conf
FREEBSD_TARGET = "amd64"
FREEBSD_TARGET_ARCH = "amd64"
FREEBSD_KERNEL_CONFIG = "GENERIC"
```

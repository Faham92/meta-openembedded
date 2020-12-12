Summanry = "Middleware framework for smart card terminals"
HOMEPAGE = "https://github.com/OpenSC/openct/wiki"
DESCRIPTION = " \
OpenCT implements drivers for several smart card readers. \
It comes as driver in ifdhandler format for PC/SC-Lite, \
as CT-API driver, or as a small and lean middleware, \
so applications can use it with minimal overhead. \
OpenCT also has a primitive mechanism to export smart card \
readers to remote machines via TCP/IP."

DEPENDS += "libtool pcsc-lite libusb-compat"

SRC_URI = " \
    https://downloads.sourceforge.net/project/opensc/${BPN}/${BPN}-${PV}.tar.gz \
    file://etc-openct.udev.in-disablePROGRAM.patch \
    file://etc-openct_usb.in-modify-UDEVINFO.patch \
    file://openct.init \
    file://openct.sysconfig \
    file://openct.service \
"

SRC_URI[md5sum] = "a1da3358ab798f1cb9232f1dbababc21"
SRC_URI[sha256sum] = "6cd3e2933d29eb1f875c838ee58b8071fd61f0ec8ed5922a86c01c805d181a68"

LICENSE = "LGPLv2+"
LIC_FILES_CHKSUM = "file://LGPL-2.1;md5=2d5025d4aa3495befef8f17206a5b0a1"

inherit systemd
SYSTEMD_SERVICE_${PN} += "openct.service "
SYSTEMD_AUTO_ENABLE = "enable"

EXTRA_OECONF=" \
    --disable-static \
    --enable-usb \
    --enable-pcsc \
    --enable-doc \
    --enable-api-doc \
    --with-udev=${nonarch_base_libdir}/udev \
    --with-bundle=${libdir}/pcsc/drivers \
"

inherit autotools pkgconfig

FILES_${PN} += " \
    ${libdir}/ctapi \
    ${nonarch_base_libdir}/udev \
    ${libdir}/openct-ifd.so \
    ${libdir}/pcsc \
"

FILES_${PN}-dbg += " \
    ${libdir}/ctapi/.debug \
    ${libdir}/pcsc/drivers/openct-ifd.bundle/Contents/Linux/.debug \
"

INSANE_SKIP_${PN} += "dev-deps"

do_install () {
    rm -rf ${D}
    install -d ${D}/etc
    install -dm 755 ${D}${nonarch_base_libdir}/udev
    # fix up hardcoded paths
    sed -i -e 's,/etc/,${sysconfdir}/,' -e 's,/usr/sbin/,${sbindir}/,' \
        ${WORKDIR}/openct.service ${WORKDIR}/openct.init

    oe_runmake install DESTDIR=${D}
    install -dm 755 ${D}${libdir}/ctapi/
    mv ${D}${libdir}/libopenctapi.so ${D}${libdir}/ctapi/
    install -Dpm 644 etc/openct.udev ${D}/etc/udev/rules.d/60-openct.rules
    install -pm 644 etc/openct.conf ${D}/etc/openct.conf

    install -Dpm 755 ${WORKDIR}/openct.init ${D}/etc/init.d/openct
    install -Dpm 644 ${WORKDIR}/openct.sysconfig ${D}/etc/sysconfig/openct

    install -d ${D}/${systemd_unitdir}/system
    install -m 644 ${WORKDIR}/openct.service ${D}/${systemd_unitdir}/system

    so=$(find ${D} -name \*.so | sed "s|^${D}||")
    sed -i -e 's|\\(LIBPATH\\s*\\).*|\\1$so|' etc/reader.conf
    install -Dpm 644 etc/reader.conf ${D}/etc/reader.conf.d/openct.conf
}

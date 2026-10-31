package app.daiyu.android.terminal.install

import junit.framework.TestCase

/** Fixed supplied-source values only; these assertions are not download/hash verification. */
class LinuxRootfsCatalogTest : TestCase() {
    private data class Row(val distribution: String, val version: String, val abi: String, val arch: String,
        val bytes: Long, val hash: String, val primary: String, val alternate: String,
        val primaryId: String, val alternateId: String, val archive: RootfsArchive, val strip: Int)

    private val rows = listOf(
        Row("alpine", "3.24.1", "arm64-v8a", "aarch64", 4023732,
            "f55a90f69052c5bd6f92cb09a8f47065970830b194c917a006fb94028e721259",
            "https://dl-cdn.alpinelinux.org/alpine/v3.24/releases/aarch64/alpine-minirootfs-3.24.1-aarch64.tar.gz",
            "https://mirrors.aliyun.com/alpine/v3.24/releases/aarch64/alpine-minirootfs-3.24.1-aarch64.tar.gz",
            "primary", "aliyun", RootfsArchive.TAR_GZIP, 0),
        Row("alpine", "3.24.1", "x86_64", "x86_64", 3698422,
            "41f73e3cf5fa919b8aa5ca6b30dc48f0da2720776d7423e2a7748211456fe081",
            "https://dl-cdn.alpinelinux.org/alpine/v3.24/releases/x86_64/alpine-minirootfs-3.24.1-x86_64.tar.gz",
            "https://mirrors.aliyun.com/alpine/v3.24/releases/x86_64/alpine-minirootfs-3.24.1-x86_64.tar.gz",
            "primary", "aliyun", RootfsArchive.TAR_GZIP, 0),
        Row("debian", "13", "arm64-v8a", "aarch64", 35409704,
            "3834a11cbc6496935760bdc20cca7e2c25724d0cd8f5e4926da8fd5ca1857918",
            "https://github.com/termux/proot-distro/releases/download/v4.29.0/debian-trixie-aarch64-pd-v4.29.0.tar.xz",
            "https://gh-proxy.com/https://github.com/termux/proot-distro/releases/download/v4.29.0/" +
                "debian-trixie-aarch64-pd-v4.29.0.tar.xz",
            "primary", "gh-proxy", RootfsArchive.TAR_XZ, 1),
        Row("debian", "13", "x86_64", "x86_64", 36728936,
            "4b8f33b80a10d734ff935e5934588572f860c0c38a68bf91db59af0580370716",
            "https://github.com/termux/proot-distro/releases/download/v4.29.0/debian-trixie-x86_64-pd-v4.29.0.tar.xz",
            "https://gh-proxy.com/https://github.com/termux/proot-distro/releases/download/v4.29.0/" +
                "debian-trixie-x86_64-pd-v4.29.0.tar.xz",
            "primary", "gh-proxy", RootfsArchive.TAR_XZ, 1),
        Row("ubuntu", "24.04.5", "arm64-v8a", "arm64", 29936675,
            "a91d5a93010193712d346d761372b7c9db6dfcf093893161c64ca107f05914f2",
            "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04.5/release/ubuntu-base-24.04.5-base-arm64.tar.gz",
            "https://mirrors.aliyun.com/ubuntu-cdimage/ubuntu-base/releases/24.04.5/release/" +
                "ubuntu-base-24.04.5-base-arm64.tar.gz",
            "primary", "aliyun", RootfsArchive.TAR_GZIP, 0),
        Row("ubuntu", "24.04.5", "x86_64", "amd64", 30028293,
            "e77b6f10c2590cef872b33ee9f635a0e3fd1f57fb074c0e52b5c7f56147a0c86",
            "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04.5/release/ubuntu-base-24.04.5-base-amd64.tar.gz",
            "https://mirrors.aliyun.com/ubuntu-cdimage/ubuntu-base/releases/24.04.5/release/" +
                "ubuntu-base-24.04.5-base-amd64.tar.gz",
            "primary", "aliyun", RootfsArchive.TAR_GZIP, 0)
    )

    fun testExactSixPinnedArtifacts() {
        assertEquals(rows.size, LinuxRootfsCatalog.artifacts.size)
        for (row in rows) {
            val artifact = checkNotNull(LinuxRootfsCatalog.find(row.distribution, row.abi))
            assertEquals(row.version, artifact.version)
            assertEquals(row.bytes, artifact.compressedBytes)
            assertEquals(row.hash, artifact.sha256)
            assertEquals(listOf(row.primary, row.alternate), artifact.mirrors.map { it.uri.toASCIIString() })
            assertEquals(listOf(row.primaryId, row.alternateId), artifact.mirrors.map { it.id })
            assertEquals(row.archive, artifact.archive)
            assertEquals(row.strip, artifact.stripComponents)
        }
    }

    fun testDistributionsAreTheOrderedCatalogSubset() {
        assertEquals(listOf("alpine", "debian", "ubuntu"), LinuxRootfsCatalog.distributions)
        assertEquals(LinuxRootfsCatalog.distributions.distinct(), LinuxRootfsCatalog.distributions)
        for (distribution in LinuxRootfsCatalog.distributions) {
            for (abi in listOf("arm64-v8a", "x86_64")) {
                assertNotNull("$distribution/$abi", LinuxRootfsCatalog.find(distribution, abi))
            }
        }
    }

    fun testNo32BitOrUnknownFallbackAndListsAreImmutable() {
        for (distribution in listOf("alpine", "debian", "ubuntu")) {
            assertNull(LinuxRootfsCatalog.find(distribution, "armeabi-v7a"))
            assertNull(LinuxRootfsCatalog.find(distribution, "x86"))
            assertNull(LinuxRootfsCatalog.find(distribution, "aarch64"))
        }
        assertNull(LinuxRootfsCatalog.find("unknown", "arm64-v8a"))
        try {
            (LinuxRootfsCatalog.artifacts as MutableList).clear()
            fail("mutable catalog")
        } catch (_: UnsupportedOperationException) { }
        try {
            (LinuxRootfsCatalog.distributions as MutableList).clear()
            fail("mutable distributions")
        } catch (_: UnsupportedOperationException) { }
        try {
            (LinuxRootfsCatalog.artifacts.first().mirrors as MutableList).clear()
            fail("mutable mirrors")
        } catch (_: UnsupportedOperationException) { }
    }

    fun testLimitsAreFixedAndCannotBeRaisedThroughTestSeam() {
        assertEquals(3L * 1024 * 1024 * 1024, TarLinuxRootfsExtraction.MAX_EXPANDED_BYTES)
        assertEquals(200000, TarLinuxRootfsExtraction.MAX_ENTRIES)
        assertEquals(128 * 1024, TarLinuxRootfsExtraction.XZ_MEMORY_LIMIT_KIB)
        try {
            TarLinuxRootfsExtraction(maxEntries = 200001)
            fail("raised entry limit")
        } catch (_: IllegalArgumentException) { }
        try {
            TarLinuxRootfsExtraction(maxExpandedBytes = TarLinuxRootfsExtraction.MAX_EXPANDED_BYTES + 1)
            fail("raised size limit")
        } catch (_: IllegalArgumentException) { }
    }
}

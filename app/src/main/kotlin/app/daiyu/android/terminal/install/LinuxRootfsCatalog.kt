package app.daiyu.android.terminal.install

import java.net.URI
import java.util.Collections

/**
 * Fixed transport metadata, not an installed-environment registry or download approval.
 * Maintainer-owned rootfs catalog; historical input baseline 2a7e7ab92b98da5a3711611017021441e7a8053c
 * (AlpineEnvironmentInstaller / DebianEnvironmentInstaller). No network verification implied.
 * Debian payloads are Termux proot-distro releases, NOT official Debian rootfs artifacts.
 * Ubuntu payloads are official Ubuntu Base release artifacts, NOT proot-distro repacks.
 */
object LinuxRootfsCatalog {
    val artifacts: List<LinuxRootfsArtifact> = Collections.unmodifiableList(listOf(
        alpine("arm64-v8a", "aarch64", 4023732,
            "f55a90f69052c5bd6f92cb09a8f47065970830b194c917a006fb94028e721259"),
        alpine("x86_64", "x86_64", 3698422,
            "41f73e3cf5fa919b8aa5ca6b30dc48f0da2720776d7423e2a7748211456fe081"),
        debian("arm64-v8a", "aarch64", 35409704,
            "3834a11cbc6496935760bdc20cca7e2c25724d0cd8f5e4926da8fd5ca1857918"),
        debian("x86_64", "x86_64", 36728936,
            "4b8f33b80a10d734ff935e5934588572f860c0c38a68bf91db59af0580370716"),
        ubuntu("arm64-v8a", "arm64", 29936675,
            "a91d5a93010193712d346d761372b7c9db6dfcf093893161c64ca107f05914f2"),
        ubuntu("x86_64", "amd64", 30028293,
            "e77b6f10c2590cef872b33ee9f635a0e3fd1f57fb074c0e52b5c7f56147a0c86")
    ))

    /**
     * Ordered, de-duplicated distributions present in [artifacts]; the settings selector and the
     * saved install-choice validator derive from this instead of keeping their own lists.
     */
    val distributions: List<String> =
        Collections.unmodifiableList(artifacts.map { it.distribution }.distinct())

    fun find(distribution: String, abi: String): LinuxRootfsArtifact? =
        artifacts.singleOrNull { it.distribution == distribution && it.abi == abi }

    private fun alpine(abi: String, arch: String, bytes: Long, hash: String): LinuxRootfsArtifact {
        val path = "alpine/v3.24/releases/$arch/alpine-minirootfs-3.24.1-$arch.tar.gz"
        return LinuxRootfsArtifact("alpine-3.24.1-$arch", "alpine", "3.24.1", abi,
            RootfsArchive.TAR_GZIP, bytes, hash, listOf(
                RootfsMirror("primary", URI("https://dl-cdn.alpinelinux.org/$path")),
                RootfsMirror("aliyun", URI("https://mirrors.aliyun.com/$path"))
            ), stripComponents = 0)
    }

    private fun debian(abi: String, arch: String, bytes: Long, hash: String): LinuxRootfsArtifact {
        val url = "https://github.com/termux/proot-distro/releases/download/v4.29.0/" +
            "debian-trixie-$arch-pd-v4.29.0.tar.xz"
        return LinuxRootfsArtifact("debian-13-pd-v4.29.0-$arch", "debian", "13", abi,
            RootfsArchive.TAR_XZ, bytes, hash, listOf(
                RootfsMirror("primary", URI(url)),
                RootfsMirror("gh-proxy", URI("https://gh-proxy.com/$url"))
            ), stripComponents = 1)
    }

    /** Official Ubuntu Base rootfs: gzip, archive top level is the root itself (strip 0). */
    private fun ubuntu(abi: String, arch: String, bytes: Long, hash: String): LinuxRootfsArtifact {
        val version = "24.04.5"
        val file = "ubuntu-base-$version-base-$arch.tar.gz"
        val path = "ubuntu-base/releases/$version/release/$file"
        return LinuxRootfsArtifact("ubuntu-$version-$arch", "ubuntu", version, abi,
            RootfsArchive.TAR_GZIP, bytes, hash, listOf(
                RootfsMirror("primary", URI("https://cdimage.ubuntu.com/$path")),
                RootfsMirror("aliyun", URI("https://mirrors.aliyun.com/ubuntu-cdimage/$path"))
            ), stripComponents = 0)
    }
}

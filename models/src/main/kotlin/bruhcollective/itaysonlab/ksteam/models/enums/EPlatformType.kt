package bruhcollective.itaysonlab.ksteam.models.enums

enum class EPlatformType {
    k_EPlatformTypeUnknown,
    k_EPlatformTypeWin32,
    k_EPlatformTypeWin64,
    k_EPlatformTypeLinux64,
    k_EPlatformTypeOSX,
    k_EPlatformTypePS3,
    k_EPlatformTypeLinux32,
    k_EPlatformTypeAndroid32,
    k_EPlatformTypeAndroid64,
    k_EPlatformTypeIOS32,
    k_EPlatformTypeIOS64,
    k_EPlatformTypeTVOS,
    k_EPlatformTypeEmbeddedClient,
    k_EPlatformTypeBrowser,
    k_EPlatformTypeMax;

    companion object {
        fun byEncoded(num: Int) = values().getOrElse(num) { k_EPlatformTypeUnknown }
    }
}
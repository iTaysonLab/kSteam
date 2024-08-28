package bruhcollective.itaysonlab.ksteam

/**
 * Specifies a source of private IP used when creating a session.
 *
 * It's recommended to use the default value of [UsePrivateIp] to avoid being logged out if another client will try to sign in.
 */
enum class AuthPrivateIpLogic {
    /**
     * Uses current machine's private IP.
     *
     * This is the recommended approach which is used in the official client and other Steam Network libraries.
     * Note that this method can cause collisions - mostly in the situations when an official Steam client is launched on a PC with a running kSteam instance (or vice versa).
     *
     * On Apple platforms, it will fall back to [Generate] because current API usage can lead to the App Store rules violation.
     */
    UsePrivateIp,

    /**
     * Generates a random integer and passes this as an IP.
     *
     * Not recommended because there is no guarantee Steam would accept a session with "faked" IP.
     */
    Generate,

    /**
     * Do not send any private IP.
     *
     * This can be used for some "privacy", but note that any sign-in from other device/application might log you out of this kSteam instance.
     */
    None
}
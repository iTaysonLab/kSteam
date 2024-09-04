package bruhcollective.itaysonlab.ksteam.models.enums

// 131 - 134 is missed
enum class EPaymentMethod {
    None,
    CDKey,
    CreditCard,
    Giropay,
    PayPal,
    IDEAL,
    PaySafeCard,
    Sofort,
    GuestPass,
    WebMoney,
    MoneyBookers,
    AliPay,
    Yandex,
    Kiosk,
    QIWI,
    GameStop,
    HardwarePromo,
    Mopay,
    BoletoBancario,
    BoaCompraGold,
    BancoDoBrasilOnline,
    ItauOnline,
    BradescoOnline,
    Pagseguro,
    VisaBoaCompra,
    AmexBoaCompra,
    Aura,
    Hipercard,
    MastercardBoaCompra,
    DinersCardBoaCompra,
    AuthorizedDevice,
    MOLPoints,
    ClickBuy,
    Beeline,
    Konbini,
    EClubPoints,
    CreditCardDegica,
    BankTransferDegica,
    PayEasyDegica,
    Zong,
    CultureVoucher,
    BookVoucher,
    HappymoneyVoucher,
    ConvenientStoreVoucher,
    GameVoucher,
    Multibanco,
    Payshop,
    Maestro,
    OXXO,
    ToditoCash,
    Carnet,
    SPEI,
    Threepay, // 3pay
    IsBank,
    Garanti,
    Akbank,
    YapiKredi,
    Halkbank,
    BankAsya,
    Finansbank,
    DenizBank,
    PTT,
    CashU,
    SantanderRio,
    AutoGrant,
    WebMoneyDegica,
    OneCard,
    PSE,
    Exito,
    Efecty,
    Balota,
    PinValidda,
    MangirKart,
    BancoCreditoDePeru,
    BBVAContinental,
    SafetyPay,
    PagoEfectivo,
    Trustly,
    UnionPay,
    Bitcoin,
    LicensedSite,
    BitCash,
    NetCash,
    Nanaco,
    Tenpay,
    WeChat,
    CashOnDelivery,
    CreditCardNodwin,
    DebitCardNodwin,
    NetBankingNodwin,
    CashCardNodwin,
    WalletNodwin,
    MobileDegica,
    Naranja,
    Cencosud,
    Cabal,
    PagoFacil,
    Rapipago,
    BancoNacionalDeCostaRica,
    BancoPoplar,
    RedPagos,
    SPE,
    Multicaja,
    RedCompra,
    ZiraatBank,
    VakiflarBank,
    KuveytTurkBank,
    EkonomiBank,
    Pichincha,
    PichinchaCash,
    Przelewy24,
    Trustpay,
    POLi,
    MercadoPago,
    PayU,
    VTCPayWallet,
    MrCash,
    EPS,
    Interac,
    VTCPayCards,
    VTCPayOnlineBanking,
    VisaElectronBoaCompra,
    CafeFunded,
    OCA,
    Lider,
    WebMoneySteamCardJapan,
    WebMoneySteamCardTopUpJapan,
    Toss,
    Wallet,
    Valve,
    SteamPressMaster,
    StorePromotion,
    Reserved132, // kSteam reserved
    Reserved133, // kSteam reserved
    MasterSubscription,
    Payco,
    MobileWalletJapan,
    BoletoFlash,
    PIX,
    GCash,
    KakaoPay,
    Dana,
    TrueMoney,
    TouchnGo,
    LinePay,
    MerPay,
    PayPay,
    AlfaClick,
    Sberbank,
    YooMoney,
    Tinkoff,
    CashInCIS,
    AuPAY,
    AliPayHK,
    NaverPay,
    Linkaja,
    ShopeePay,
    GrabPay,
    PayNow,
    OnlineBankingThailand,
    CashOptionsThailand,
    OEMTicket, // 256
    Split, // 512
    Complimentary; // 1024

    companion object {
        fun byIndex(index: Int): EPaymentMethod {
            return when (index) {
                256 -> OEMTicket
                512 -> Split
                1024 -> Complimentary
                else -> entries.getOrNull(index) ?: EPaymentMethod.None
            }
        }
    }
}
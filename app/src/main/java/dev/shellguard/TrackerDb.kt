package dev.shellguard

object TrackerDb {
    data class Tracker(val name: String, val signature: String)

    // Subset of known tracker SDKs (cf. Exodus ETIP).
    // ponytail: sync full ETIP DB (~400 sigs) from
    // https://etip.exodus-privacy.eu.org when local subset proves insufficient
    val TRACKERS: List<Tracker> = listOf(
        Tracker("Google Ads", "com.google.ads"),
        Tracker("Google Firebase Analytics", "com.google.firebase.analytics"),
        Tracker("Google Crashlytics", "com.crashlytics"),
        Tracker("Google Tag Manager", "com.google.tagmanager"),
        Tracker("Facebook Ads", "com.facebook.ads"),
        Tracker("Facebook Analytics", "com.facebook.appevents"),
        Tracker("AppsFlyer", "com.appsflyer"),
        Tracker("Adjust", "com.adjust.sdk"),
        Tracker("Amplitude", "com.amplitude"),
        Tracker("Mixpanel", "com.mixpanel"),
        Tracker("Segment", "com.segment.analytics"),
        Tracker("Flurry", "com.flurry"),
        Tracker("AppLovin", "com.applovin"),
        Tracker("Unity Ads", "com.unity3d.ads"),
        Tracker("Chartboost", "com.chartboost"),
        Tracker("IronSource", "com.ironsource"),
        Tracker("AdColony", "com.adcolony"),
        Tracker("MoPub", "com.mopub"),
        Tracker("InMobi", "com.inmobi"),
        Tracker("StartApp", "com.startapp"),
        Tracker("Yandex Metrica", "com.yandex.metrica"),
        Tracker("Braze", "com.appboy"),
        Tracker("OneSignal", "com.onesignal"),
        Tracker("Kochava", "com.kochava"),
        Tracker("Tapjoy", "com.tapjoy"),
        Tracker("Vungle", "com.vungle"),
        Tracker("Pangle", "com.bytedance.sdk.dcx"),
        Tracker("Amazon Ads", "com.amazon.device.ads"),
    )
}

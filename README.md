# Arama Engelle (spam-arama-engelleyici)

0850, 444, gizli numara, yurt dışı ve rehber dışı aramaları **telefonun çalmadan** engelleyen
Android uygulaması. Tamamen cihazda çalışır: sunucu yok, veritabanı yok, hiçbir numara dışarı
gönderilmez.

- Paket adı: `tr.com.covolt.aramaengelleyici` (Play'e yüklendikten sonra değiştirilemez)
- Android 10+ (minSdk 29), targetSdk 36
- Kotlin + Jetpack Compose, Google Play Billing 8

## Nasıl çalışıyor?

Android 10 ile gelen **arama filtreleme rolü** (`CallScreeningService` + `RoleManager.ROLE_CALL_SCREENING`)
kullanılıyor. Kullanıcı uygulamayı "Arayan kimliği ve spam uygulaması" olarak seçince Android,
**rehberde olmayan** her numaradan gelen aramayı uygulamaya sorar; uygulama kurallara göre reddeder
ya da sessize alır.

Bu yüzden şu izinlerin **hiçbiri** istenmiyor: rehber, arama kaydı, telefon durumu, SMS, internet.
Google Play'in kısıtlı izin beyan formuna da gerek yok.

| Klasör | İçerik |
| --- | --- |
| `core/` | Numara ayrıştırma (`PhoneNumbers`) ve kural motoru (`RuleEngine`). Saf Kotlin, birim testli. |
| `screening/` | Android'in çağırdığı arama filtresi servisi ve rol yardımcıları |
| `data/` | Ayarlar ve engelleme geçmişi (SharedPreferences, yalnızca cihazda) |
| `billing/` | Play Billing: `pro` aboneliği (aylık/yıllık) ve `pro_omur_boyu` |
| `ui/` | Compose ekranları: Ana sayfa, Geçmiş, Listeler, Pro |
| `store/` | Mağaza metinleri (SEO), gizlilik politikası, Data safety cevapları |

## Ücretsiz / Pro

| Özellik | Ücretsiz | Pro |
| --- | --- | --- |
| 0850 engelleme | ✓ | ✓ |
| Gizli numara engelleme | ✓ | ✓ |
| Kara liste | 5 numara | Sınırsız |
| Beyaz liste, kargo modu, geçmiş | ✓ | ✓ |
| 444, yurt dışı, rehber dışı herkes, gece modu, önek engelleme | | ✓ |

## Otomatik derleme

Her `main` push'unda GitHub Actions birim testlerini çalıştırır, debug APK üretir ve lint
bulgularını uyarı olarak gösterir (Actions sekmesi). APK, çalıştırmanın "Artifacts" bölümünden
indirilebilir.

## Çalıştırma

1. Android Studio ile klasörü aç (File > Open). Gradle senkronizasyonu bitsin.
   Studio eklenti/AGP güncellemesi önerirse kabul edebilirsin.
2. Testler: `./gradlew test` (Windows: `gradlew.bat test`).
3. Gerçek bir telefona yükle (emülatörde arama testi zahmetli).
   Uygulamada **Korumayı aç**'a bas, "Arayan kimliği ve spam" olarak seç.
4. Başka bir telefondan arayarak dene:
   - Rehberde olmayan bir numaradan ara, kara listeye ekle, tekrar ara → reddedilmeli.
   - Gizli numarayla ara (`#31#` + numara) → reddedilmeli.
   - Ayarlar > "Rehberde olmayan herkes" testi Pro gerektirir; test için Play Console'da
     lisans test kullanıcısı ekle (aşağıda).

## Play Console'a çıkış kontrol listesi

- [ ] Kuruluş hesabı doğrulaması tamam (12 test kullanıcısı / 14 gün şartı yok)
- [ ] Yükleme anahtarı oluştur ve `keystore.properties.example` dosyasını `keystore.properties`
      olarak kopyalayıp doldur (ikisi de repoya girmez); **Play App Signing** açık kalsın
- [ ] `gradlew.bat bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`
- [ ] Uygulama içi ürünleri oluştur: `pro` (temel planlar `aylik`, `yillik`), `pro_omur_boyu`
      — fiyatlar `store/STORE_LISTING.md` içinde
- [ ] Ayarlar > Lisans testi: kendi Gmail adresini ekle (ücretsiz test satın alması için)
- [ ] `store/privacy-policy.html` dosyasındaki köşeli parantezleri doldurup şirket sitende yayınla,
      adresi Play Console'a ve `ui/Links.kt` içindeki `PRIVACY_POLICY` sabitine yaz
- [ ] Görseller: `store/graphics/icon-512.png` (uygulama simgesi) ve
      `store/graphics/feature-graphic-1024x500.png` (öne çıkan grafik); ekran görüntülerini
      gerçek telefondan al
- [ ] Mağaza girişi: `store/STORE_LISTING.md`
- [ ] Data safety: `store/STORE_LISTING.md` içindeki cevaplar
- [ ] İçerik derecelendirmesi, hedef kitle (18+), reklam yok beyanı
- [ ] Önce **Dahili test** kanalına yükle, kendi telefonunda satın alma akışını dene,
      sonra Üretim

## Sonraki sürüm fikirleri

- Haftalık "bu hafta X arama engellendi" bildirimi (paylaşılabilir kart)
- Gece modu saatlerini değiştirme
- Ana ekran widget'ı
- İngilizce mağaza sayfası ve arayüz

# Blind Assist (Görüntüden Sesli Okuma Asistanı)

**Blind Assist**, görme engelli kullanıcılar için geliştirilmiş, kamera görüntüsündeki metinleri tespit edip sesli olarak okuyan erişilebilir bir Android uygulamasıdır. Kullanıcı sadece ekrana dokunarak ortamdaki yazıları (kitap, tabela, belge vb.) dinleyebilir.

## 🌟 Özellikler (Features)

*   **Erişilebilir Arayüz (Accessible UI):** Tam ekran kamera önizlemesi ve "Tap-to-Scan" (Dokun ve Tara) özelliği. Görme engelli kullanıcılar için buton arama derdini ortadan kaldırır.
*   **Anlık Metin Tanıma (OCR):** Google ML Kit altyapısı ile görüntüden yüksek doğrulukla metin ayıklama.
*   **Sesli Okuma (TTS):** Ayıklanan metnin Android Text-To-Speech motoru ile sesli olarak kullanıcıya okunması.
*   **Geri Bildirimler:** Uygulama durumu hakkında sesli uyarılar ("Görüntü işleniyor", "Metin bulunamadı" vb.).

## 🛠️ Teknolojiler (Tech Stack)

*   **Dil:** Kotlin
*   **Minimum SDK:** API 24 (Android 7.0)
*   **Kamera:** [CameraX](https://developer.android.com/training/camerax) (Preview & ImageCapture)
*   **Yapay Zeka:** [Google ML Kit](https://developers.google.com/ml-kit/vision/text-recognition/v2) (On-device Text Recognition V2)
*   **Ses:** Android Native TextToSpeech (TTS)
*   **Mimari:** MVVM (Basitleştirilmiş) & ViewBinding

## 🚀 Kurulum ve Çalıştırma

1.  Projeyi klonlayın:
    ```bash
    git clone https://github.com/kullaniciadi/Bitirme.git
    ```
2.  Android Studio'da projeyi açın (`File > Open`).
3.  Gradle senkronizasyonunun tamamlanmasını bekleyin.
4.  Android cihazınızı USB ile bağlayın ve **Run** tuşuna basın.
    *   *Not: Emülatörde kamera performansı düşük olabilir, fiziksel cihaz önerilir.*
    
## 📸 Kullanım

1.  Uygulamayı açın ve kamera iznini onaylayın.
2.  Kamerayı okumak istediğiniz metne doğrultun.
3.  Ekranın herhangi bir yerine **bir kez dokunun**.
4.  Uygulama fotoğrafı çekecek ve metni size sesli olarak okuyacaktır.

## 📝 Lisans

Bu proje akademik/eğitim amaçlı geliştirilmiştir.

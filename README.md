# Winnn - Yapay Zeka Destekli XOX Oyunu

Winnn, Q-Learning (Pekiştirmeli Öğrenme) algoritması kullanarak kendi kendine oynamayı öğrenen gelişmiş bir Tic-Tac-Toe (XOX) oyunudur.

## 🌟 Özellikler

*   **İki Oyun Modu**:
    *   **Klasik Mod**: Standart 3x3 Tic-Tac-Toe. 3 taşı yan yana getiren kazanır.
    *   **XOX Modu**: "X-O-X" desenini oluşturmaya dayalı stratejik mod.
*   **Q-Learning Yapay Zeka**:
    *   Oyun oynadıkça öğrenen dinamik bir zeka.
    *   50,000+ oyunluk eğitim simülasyonu.
    *   Keşif (Exploration) ve Sömürü (Exploitation) dengesi.
*   **Modern Arayüz**:
    *   Jetpack Compose ile geliştirilmiş şık ve akıcı UI.
    *   Canlı eğitim istatistikleri (Epsilon, Bölüm sayısı).

## 🚀 Nasıl Başlanır?

1.  Uygulamayı açın.
2.  Menüden **Klasik** veya **XOX** modunu seçin.
3.  **"Eğit (50k)"** butonuna basın.
    *   AI saniyeler içinde binlerce oyun oynayarak strateji geliştirecektir.
    *   Eğitim sırasında ilerleme çubuğunu ve metrikleri izleyebilirsiniz.
4.  Eğitim bittikten sonra **"Sıfırla"** diyerek yapay zekaya karşı oynayın!

## 🧠 Teknik Detaylar

### Q-Learning Algoritması
Yapay zeka, `QLearningAgent` sınıfı içerisinde yönetilir.
*   **Durum (State)**: Oyun tahtasının o anki hali.
*   **Aksiyon (Action)**: Boş bir kareye hamle yapmak.
*   **Ödül (Reward)**:
    *   Kazanma: +10.0
    *   Kaybetme: -10.0
    *   Beraberlik: +0.5
    *   Tehdit Oluşturma (2'li periyot): +0.2 (Sadece Klasik Mod)
*   **Epsilon-Greedy**: AI, eğitimin başında rastgele hamleler yaparak (Keşif) ortamı tanır, sonlara doğru öğrendiği en iyi hamleleri (Sömürü) yapar.

### Mimari (MVVM)
Proje, Android'in önerdiği **Model-View-ViewModel** mimarisini takip eder.
*   **Model**: `GameState.kt` (Oyun verisi), `QLearningAgent.kt` (AI Mantığı).
*   **View**: `MainActivity.kt` (Compose UI).
*   **ViewModel**: `GameViewModel.kt` (UI ve İş mantığı arasındaki köprü).

## 🛠 Geliştirici Notları
*   **Symmetry Breaking**: AI'nın konumsal hatalarını engellemek için simetri optimizasyonu kapatılmıştır. Her tahta durumu benzersiz olarak işlenir.
*   **Reward Shaping**: XOX modu için "Reward Hacking" riskini önlemek adına ara ödüller en aza indirilmiştir.

---
**Geliştirici**: Mustafa Mert Nas


Q-Learning  algoritması kullanarak kendi kendine oynamayı öğrenen gelişmiş bir Tic-Tac-Toe (XOX) oyunudur.

##  Özellikler

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

## Teknik Detaylar

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
*   **Model**: GameState.kt, QLearningAgent.kt.
*   **View**: MainActivity.kt.
*   **ViewModel**: GameViewModel.kt.



**Geliştirici**: Mustafa Mert Nas

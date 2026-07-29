# Uygulama Planı - Modern & Premium Launcher İkonu Güncellemesi

Uygulama ikonunu "basic" görünümden kurtarıp, referans tasarımdaki yüksek kaliteli ve modern görünüme (Açık Tema versiyonu) kavuşturacağız. İkonu daha detaylı, gradyanlı ve derinlik hissi veren bir yapıya taşıyacağız.

## Önerilen Değişiklikler

### Görsel Tasarım Güncellemeleri

#### [DEĞİŞTİR] [ic_launcher_background.xml](file:///C:/Users/ardaf/AndroidStudioProjects/CineVerse/app/src/main/res/drawable/ic_launcher_background.xml)
- Arka plan rengini, açık tema kartelasındaki soft beyaz/gri tonuna (`#F7F8FC`) güncelleyeceğiz. Bu, ikonun daha ferah ve modern görünmesini sağlayacaktır.

#### [DEĞİŞTİR] [ic_launcher_foreground.xml](file:///C:/Users/ardaf/AndroidStudioProjects/CineVerse/app/src/main/res/drawable/ic_launcher_foreground.xml)
- **Detay Seviyesi**: Sadece düz bir "C" yerine, film şeridi deliklerini (punches) vektör düzeyinde ekleyerek derinlik katacağız.
- **Gradyan Kullanımı**: "C" harfine mor ve mavi tonlarında bir gradyan ekleyerek modern bir görünüm kazandıracağız.
- **Play İkonu**: Altın sarısı play butonunu, logonun odağı olacak şekilde daha keskin ve belirgin hale getireceğiz.
- **Gölge Efekti**: Vektör içerisinde hafif bir "layering" (katmanlama) kullanarak logoya hafif bir derinlik/yükseklik hissi vereceğiz.

## Doğrulama Planı

### Otomatik Testler
- XML dosyalarının geçerliliği ve projenin derlenmesi kontrol edilecek.

### Manuel Doğrulama
- Ana ekrandaki ikonun yeni "Light" temalı ve detaylı haliyle göründüğü doğrulanacak.
- Logonun keskinliği ve renk uyumu kontrol edilecek.

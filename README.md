# CoreNotifyEngine

## 1. Proje Amacı

CoreNotifyEngine, Spring Boot ile oluşturulmuş hafif, kendi kendine yeten (self-contained) bir asenkron bildirim motorudur. Java'nın `BlockingQueue` yapısını kullanarak bellek içi (in-memory) bir mesaj kuyruğu uygular ve bildirim işlemlerini tek bir JVM örneği içinde asenkron olarak yönetmek için Üretici-Tüketici (Producer-Consumer) desenini takip eder.

Bu motor, bildirim üretimini bildirim tesliminden ayırması gereken, ancak harici bir mesajlaşma altyapısı kurmak istemeyen uygulamalar için basit, bağımlılık içermeyen bir çözüm sunar. Üreticilerin (örn. REST uç noktaları, zamanlanmış görevler, olay işleyicileri) bir kuyruğa bildirim göndermesine olanak tanırken, özel tüketici thread'leri (iş parçacıkları) bu bildirimleri bağımsız olarak işleyerek bloklamayan ve ölçeklenebilir bir bildirim yönetimi sağlar.

## 2. Çözdüğü Sorunlar

- **Ayrıştırılmış İşleme (Decoupled Processing):** Bildirim üretimini bildirim tesliminden ayırarak, yavaş bildirim işlemlerinin (örn. e-posta gönderimi, SMS teslimi, push bildirimleri) ana uygulama thread'lerini bloklamasını önler.

- **Geliştirilmiş Tepkisellik (Improved Responsiveness):** Bildirimleri asenkron olarak işleyerek, bildirim kanallarında gecikmeler veya hatalar olsa bile API uç noktalarının ve iş mantığının yanıt verebilir durumda kalmasını sağlar.

- **Kaynak Verimliliği:** Thread havuzları ve sınırlandırılmış kuyruklar kullanarak kaynak tüketimini kontrol eder, böylece bildirim yığılmalarının sistemi bunaltmasını engeller.

- **Basitlik:** Harici mesaj aracılarına (RabbitMQ, Kafka veya Redis gibi) olan ihtiyacı ortadan kaldırarak operasyonel karmaşıklığı, dağıtım yükünü ve altyapı bağımlılıklarını azaltır.

- **Hata Korumalı İzolasyon:** Bildirim işleme hataları ana uygulama akışından izole edilmiştir, bu da kullanıcıya dönük operasyonları etkilemeden zarif hata yönetimi ve yeniden deneme mekanizmalarına olanak tanır.

## 3. Neden Harici Bir Broker (Aracı) Kullanılmıyor?

CoreNotifyEngine, çeşitli nedenlerle harici mesaj aracılarını bilinçli olarak kullanmaz:

- **Sıfır Altyapı Yükü:** Ek altyapı bileşenlerini (mesaj aracıları, Redis kümeleri vb.) dağıtmaya, yapılandırmaya, izlemeye veya sürdürmeye gerek yoktur; bu da operasyonel karmaşıklığı ve maliyetleri azaltır.

- **Daha Düşük Gecikme:** Bellek içi kuyruklar, ağ tabanlı mesajlaşma sistemlerine kıyasla minimal gecikme sağlar, bu da tek uygulamalı bildirim senaryoları için idealdir.

- **Dağıtım Basitliği:** Motor tamamen uygulama süreci içinde çalışır, bu da dağıtım ve ölçeklendirme sırasında ağ bağımlılıklarını, bağlantı yönetimini ve broker kullanılabilirliği endişelerini ortadan kaldırır.

- **Maliyet Verimliliği:** Harici mesajlaşma altyapısıyla ilişkili lisanslama, barındırma ve bakım maliyetlerinden kaçınır, bu da onu daha küçük uygulamalar veya bütçe kısıtlamaları olan ortamlar için uygun hale getirir.

- **Geliştirme Hızı:** Mesaj aracılarıyla geliştirme ortamları kurmaya gerek yoktur, bu da yerel geliştirmeyi ve CI/CD süreçlerini basitleştirir.

- **Kullanım Durumu Uyumu:** Bildirimlerin aynı uygulama örneği içinde tüketildiği veya dağıtık mesajlaşmanın faydalarının (uygulama yeniden başlatmalarında kalıcılık, çoklu örnek koordinasyonu) gerekli olmadığı senaryolar için tasarlanmıştır.

## 4. Temel Bileşenler

Üst düzeyde, motor aşağıdaki bileşenlerden oluşur:

- **Bildirim Kuyruğu (Notification Queue):** İsteğe bağlı kapasite sınırlarıyla thread-safe (iş parçacığı güvenli) ekleme ve çıkarma işlemleri sağlayan, üreticiler ve tüketiciler arasında tampon görevi gören thread-safe bir `BlockingQueue` uygulamasıdır.

- **Bildirim Üretici Servisi (Notification Producer Service):** Uygulama kodundan gelen bildirim isteklerini kabul eden ve bunları bloklayan kuyruğa ekleyen bir servis katmanıdır. Bu bileşen bildirim oluşturma, doğrulama ve kuyruğa gönderme işlemlerini yönetir.

- **Bildirim Tüketici İşçileri (Notification Consumer Workers):** Sürekli olarak kuyruğu yoklayan ve bildirimleri işleyen arka plan thread işçileridir (genellikle Spring'in `@Async` veya `ExecutorService` aracılığıyla yönetilir). İş hacmini artırmak için birden fazla tüketici thread'i paralel olarak çalışabilir.

- **Bildirim İşleyicisi (Notification Processor):** Gerçek bildirim teslimatını (örn. e-posta servislerini, SMS ağ geçitlerini, push bildirim API'lerini çağırmak) yürütmekten sorumlu temel iş mantığı bileşenidir. Bildirime özel işleme ve yeniden deneme mantığı burada bulunur.

- **Yapılandırma Yönetimi:** Kuyruk kapasitesi, thread havuzu boyutlandırması, tüketici thread sayısı ve diğer ayar parametreleri için Spring Boot yapılandırması; motorun iş yükü gereksinimlerine göre özelleştirilmesine olanak tanır.

- **Hata Yönetimi ve İzleme:** İşleme hatalarını yönetme, bildirim yaşam döngüsü olaylarını loglama (günlüğe kaydetme) ve isteğe bağlı olarak kuyruk derinliğini, işleme oranlarını ve hata oranlarını takip etmek için izleme sistemleriyle entegrasyon mekanizmaları.

## 6. NotificationEvent Domain Modeli

`NotificationEvent`, asenkron bildirim sistemi boyunca akan bir bildirim mesajını temsil eden temel domain modelidir. Sadece alanları ve enumları içeren, iş mantığı barındırmayan basit bir veri transfer nesnesidir (DTO).

### Alan Tanımları

`NotificationEvent` modeli aşağıdaki alanlardan oluşur:

- **`id` (UUID):** Her bildirim olayı için benzersiz bir tanımlayıcı. Bu alan, sistem genelinde bildirim yaşam döngüsü olaylarının izlenmesini, loglanmasını, hata ayıklanmasını ve ilişkilendirilmesini sağlar. Her bildirime oluşturulma anında benzersiz bir kimlik verilir, bu da üretimden tüketime kadar izlenebilirlik sağlar.

- **`notificationType` (NotificationType enum):** Bildirimin hangi kanal üzerinden teslim edileceğini belirtir. Enum birden fazla bildirim kanalını destekler: `EMAIL`, `SMS`, `PUSH`, `IN_APP` ve `WEBHOOK`. Bu alan, hangi işlemci uygulamasının bildirimi ele alacağını belirler ve sistemin bildirimleri uygun teslimat mekanizmalarına yönlendirmesini sağlar.

- **`recipient` (String):** Bildirim için hedef adres veya tanımlayıcı. `notificationType`'a bağlı olarak bir e-posta adresi, telefon numarası, kullanıcı kimliği, cihaz token'ı, webhook URL'si veya başka bir alıcı tanımlayıcısını temsil edebilir. Bu alan, bildirim teslimi için hedef varış noktasını sağlar.

- **`subject` (String):** Bildirimin başlığı veya konu satırı. Öncelikle e-posta bildirimleri ve başlıkları destekleyen bazı push bildirim sistemleri için kullanılır. Bu alan bildirim netliğini artırır ve alıcıların bildirimin amacını hızlıca anlamasına yardımcı olur.

- **`body` (String):** Bildirimin ana içeriği veya mesaj gövdesi. Alıcıya gösterilecek veya gönderilecek gerçek bildirim metnini içerir. Bu alan, bildirimin temel bilgi yükünü taşır.

- **`priority` (NotificationPriority enum):** Bildirimin önem seviyesini belirtir; değerler `LOW` (DÜŞÜK), `NORMAL`, `HIGH` (YÜKSEK) ve `URGENT` (ACİL)'dir. Motor bildirimleri FIFO (İlk Giren İlk Çıkar) sırasına göre işlese de, bu alan gelecekteki önceliklendirme özellikleri, izleme, loglama ve bildirim yönetimiyle ilgili iş kararları için kullanılabilir.

- **`createdAt` (LocalDateTime):** Bildirim olayının oluşturulduğu zamanı belirten zaman damgası. Bu alan gecikme takibi, kuyruktaki bildirim yaşının izlenmesi, zamanlama sorunlarının ayıklanması ve zamana dayalı iş kurallarının (örn. çok eski bildirimlerin atılması) uygulanmasını sağlar.

- **`metadata` (Map<String, Object>):** Standart alanlara sığmayan bağlama özgü ek verileri saklamak için esnek bir harita (map). Bu, şablon değişkenlerini, özel başlıkları, yeniden deneme sayılarını, korelasyon kimliklerini, kullanıcı tercihlerini veya işlemciler ya da iş mantığı tarafından ihtiyaç duyulan diğer genişletilebilir verileri içerebilir. Bu alan, temel model yapısını değiştirmeden genişletilebilirlik sağlar.

### Sistem Akışı

`NotificationEvent` bildirim motoru içinde şu aşamalardan geçer:

1. **Oluşturma (Creation):** Uygulama kodu, gerekli tüm alanları (id, notificationType, recipient, subject, body, priority, createdAt ve isteğe bağlı metadata) doldurulmuş bir `NotificationEvent` örneği oluşturur. Olay, genellikle iş olayları (kullanıcı kaydı, sipariş onayı, sistem uyarıları vb.) tarafından tetiklenen üretici katmanı tarafından başlatılır.

2. **Kuyruğa Ekleme (Enqueue):** Doldurulan `NotificationEvent`, olayı doğrulayan (eğer doğrulama mantığı varsa) ve `BlockingQueue<NotificationEvent>` içine ekleyen `NotificationProducerService`'e gönderilir. Bu noktada olay asenkron işleme hattına girer ve kontrol hemen çağıran koda geri döner.

3. **Kuyruk Depolama (Queue Storage):** Olay, bir tüketici thread'i müsait olana kadar bellek içi `BlockingQueue` içinde bekler. Kuyruk, thread-safe depolama sağlar ve işlemler başlayana kadar olayların tutulmasını garanti eder. Kuyruk boyutu yapılandırma ile kontrol edilerek birden fazla olay aynı anda kuyruklanabilir.

4. **Kuyruktan Çıkarma (Dequeue):** Bir `NotificationConsumerWorker` thread'i kuyruğu yoklar ve müsait olduğunda bir `NotificationEvent` alır. Bu bloklayan işlem, kuyruk boş olduğunda tüketici thread'lerinin verimli bir şekilde beklemesini ve olaylar gelene kadar CPU kaynağı tüketmemesini sağlar.

5. **İşleme (Processing):** Tüketici, `NotificationEvent`'i `notificationType` alanına göre uygun `NotificationProcessor`'a iletir. İşlemci ilgili alanları (recipient, subject, body, metadata) çıkarır ve gerçek bildirim teslimatını (örn. bir e-posta servisi API'sini, SMS ağ geçidini veya push bildirim servisini çağırmak) gerçekleştirir.

6. **Tamamlama/Atma (Completion/Discard):** İşlemeden sonra (başarılı veya başarısız), `NotificationEvent` artık referans edilmez ve Çöp Toplayıcı (Garbage Collection) için uygun hale gelir. Sistemin bellek içi doğası gereği, olaylar kalıcı olarak saklanmaz ve yaşam döngüleri tüketimden sonra sona erer.

Bu akış boyunca, `NotificationEvent` kuyruk sistemi perspektifinden değişmez (immutable) kalır—bir kez oluşturulur, kuyruktan geçirilir ve bir kez tüketilir. Herhangi bir değişiklik veya zenginleştirme kuyruk altyapısının kendisinde değil, üretici veya işlemci katmanlarında gerçekleşir.

## 7. Bellek İçi Kuyruk Yapılandırması

Bildirim motoru, bellek içi bildirim tamponlaması için temel veri yapısı olarak `LinkedBlockingQueue` aracılığıyla uygulanan sınırlı (bounded) bir `BlockingQueue<NotificationEvent>` kullanır. Bu yapılandırma, asenkron bildirim işlemenin temelini oluşturur.

### Neden Sınırlı (Bounded) Kuyruk?

Sınırsız bir kuyruk yerine sınırlı bir kuyruk (sabit maksimum kapasiteye sahip), birkaç kritik nedenden dolayı tercih edilmiştir:

- **Bellek Koruması:** Bildirim üretiminin tüketimi geçtiği durumlarda `OutOfMemoryError` hatasına yol açabilecek sınırsız bellek büyümesini önler. Sabit kapasite, kuyruklanmış bildirimler için bellek kullanımında öngörülebilir bir üst sınır sağlar.

- **Geri Basınç (Backpressure) Mekanizması:** Kuyruk kapasitesine ulaştığında, bildirim eklemeye çalışan üretici thread'ler bloklanır ve doğal bir geri basınç uygular. Bu, üreticilerin sistemi bunaltmasını önler ve tüketicilere yetişmeleri için zaman tanıyarak sistem kararlılığını korur.

- **Kaynak Farkındalığı:** Sistem tasarımcılarını, beklenen iş yükü ve işleme kapasitesine göre kuyruk boyutlandırmasını düşünmeye zorlar, bu da bilinçli kaynak planlamasını ve kapasite yönetimini teşvik eder.

- **Hızlı Hata (Fail-Fast) Davranışı:** Kuyruk dolduğunda (sınırsız büyüme yerine) üreticileri bloklayarak, sistem kapasite sorunlarını erkenden ortaya çıkarır ve sorunları sessizce bellek tüketmek yerine görünür hale getirir.

- **Performans Optimizasyonu:** Uygun boyuttaki sınırlı kuyruklar, optimal kuyruk derinliğini koruyarak iş hacmini artırabilir—çok küçük olması aşırı bloklamaya neden olur, çok büyük olması fayda sağlamadan bellek yükünü artırır. İyi boyutlandırılmış bir sınırlı kuyruk, üretici ve tüketici verimliliği arasında bir denge kurar.

### Kuyruk Yaşam Döngüsü

Kuyruk yaşam döngüsü şu aşamaları izler:

1. **Başlatma:** Spring uygulama bağlamı (context) başlatılırken, `NotificationQueueConfig` yapılandırma sınıfı, yapılandırılmış kapasiteyle (varsayılan: 1000) bir `LinkedBlockingQueue<NotificationEvent>` örneği oluşturur. Kuyruk bir Spring bean'i olarak kaydedilir ve uygulama genelinde bağımlılık enjeksiyonu için kullanılabilir hale gelir.

2. **Aktif İşleyiş:** Başlatıldıktan sonra kuyruk, uygulamanın ömrü boyunca sürekli çalışır. Üreticiler `NotificationEvent` nesnelerini kuyruğa ekler ve tüketiciler bunları işlemek için kuyruktan çıkarır. Kuyruk, bildirimlerin FIFO (İlk Giren İlk Çıkar) sırasını korur.

3. **Çalışma Zamanı Durumu:** Kuyruk üç durumda olabilir:
    - **Boş:** Kuyruklanmış bildirim yoktur; tüketiciler kuyruktan çıkarma işlemi yapmaya çalıştığında bloklanır.
    - **Kısmen Dolu:** Bazı bildirimler içerir ancak kalan kapasitesi vardır; hem üreticiler hem de tüketiciler (eşzamanlı erişim varsayılarak) bloklanmadan çalışabilir.
    - **Dolu:** Maksimum kapasiteye ulaşılmıştır; üreticiler yer açılana kadar ekleme işlemlerinde bloklanır.

4. **Kapatma:** Uygulama kapandığında, kuyruk örneği ve kalan kuyruklanmış bildirimler atılır. Kalıcılık veya zarif bir kapatma mekanizması yoktur—tüm kuyruklanmış bildirimler uygulama sonlandırıldığında kaybolur.

### Kuyruk Dolduğunda Ne Olur?

Kuyruk maksimum kapasitesine ulaştığında ve bir üretici bildirim eklemeye çalıştığında, davranış kullanılan operasyona bağlıdır:

- **Bloklayan İşlemler (`put()`):** Üretici thread, kuyrukta yer açılana kadar süresiz olarak bloklanır. Bu, doğal geri basınç sağlayan varsayılan davranıştır—üreticiler, bir tüketici bildirim alıp yer açana kadar CPU kaynağı tüketmeden bekler.

- **Bloklamayan İşlemler (`offer()`):** Üreticiler `put()` yerine `offer()` kullanırsa, kuyruk dolu olduğunda işlem hemen `false` döndürür; bu da üreticilerin bloklanmak yerine alternatif stratejiler (örn. loglama, metrikler, yedek işleme veya yeniden deneme mantığı) uygulamasına izin verir.

- **Zaman Aşımı İşlemleri (`offer(timeout)`):** Üreticiler, belirtilen süre boyunca bloklanacak şekilde zaman aşımlı `offer()` kullanabilir. Zaman aşımı süresi içinde yer açılırsa bildirim kuyruğa eklenir ve `true` döner. Zaman aşımı dolarsa `false` döner ve üreticinin hata senaryosunu yönetmesine izin verilir.

İşlem seçimi (`put()` vs `offer()`), geri basıncın nasıl yönetileceğini belirler. Bloklayan işlemler (`put()`) hiçbir bildirimin kaybolmamasını sağlar ancak üreticileri yavaşlatabilir; bloklamayan işlemler ise üreticilerin devam etmesine izin verir ancak reddedilen bildirimlerin açıkça yönetilmesini gerektirebilir.

### İş Parçacığı Güvenliği (Thread-Safety) Garantileri

`LinkedBlockingQueue` uygulaması güçlü thread-safety garantileri sağlar:

- **Eşzamanlı Erişim:** Birden fazla üretici thread, senkronizasyon sorunu yaşamadan aynı anda güvenli bir şekilde bildirim ekleyebilir. Benzer şekilde, birden fazla tüketici thread eşzamanlı olarak bildirimleri kuyruktan çıkarabilir. Kuyruk tüm senkronizasyonu dahili olarak yönetir.

- **Atomik İşlemler:** Tüm kuyruk işlemleri (ekleme, çıkarma, boyut kontrolleri) atomiktir. Tek bir bildirim kısmen eklenemez veya çıkarılamaz, bu da bozulmayı veya veri tutarsızlığını önler.

- **Görünürlük Garantileri:** Kuyruk, bir thread tarafından yapılan değişikliklerin diğer thread'ler tarafından hemen görülebilmesini sağlamak için dahili olarak `volatile` değişkenler ve `java.util.concurrent.locks` kullanır, bu da eşzamanlı erişim için Java Bellek Modeli gereksinimlerini karşılar.

- **Önce-Gerçekleşir (Happens-Before) İlişkileri:** Ekleme ve çıkarma işlemleri, uygun önce-gerçekleşir ilişkileri kurar; bu, bir üreticinin bir tüketici çıkarmaya başlamadan önce bildirim eklemesi durumunda, tüketicinin o bildirimi göreceğini garanti eder.

- **Harici Senkronizasyon Gerekmez:** Kuyruğu kullanan uygulama kodunun kuyruk işlemleri yaparken harici senkronizasyon (örn. `synchronized` blokları, açık kilitler) eklemesine gerek yoktur. Kuyruk tüm thread güvenliğini dahili olarak halleder.

- **Mümkün Olduğunda Kilit-Serbest (Lock-Free):** `LinkedBlockingQueue`, ekleme ve çıkarma işlemleri için ayrı kilitler kullanır (iki kilitli kuyruk algoritması), bu da üreticilerin ve tüketicilerin çekişme olmadan paralel çalışmasına izin vererek yüksek eşzamanlılık senaryolarında iş hacmini artırır.

Bu garantiler, kuyruğun birden fazla üretici ve tüketicinin eşzamanlı olarak çalıştığı çok thread'li bir ortamda yarış durumları (race conditions), veri bozulması veya diğer eşzamanlılık sorunları olmadan güvenli bir şekilde kullanılabilmesini sağlar.

## 8. NotificationProducer Service (Bildirim Üretici Servisi)

`NotificationProducer` servisi, asenkron işleme hattına bildirim göndermek için giriş noktasıdır. `NotificationEvent` nesnelerini kabul eder ve bunları daha sonra tüketici işçiler tarafından tüketilmek üzere `BlockingQueue` içine ekler. Üreticinin sorumluluğu kasıtlı olarak sadece kuyruklama işlemleriyle sınırlandırılmıştır—bildirimleri işlemez, doğrulamaz veya teslim etmez.

### Üretici Neden Hafif Olmalı?

Üretici servisi birkaç kritik nedenden dolayı hafif kalmalıdır:

- **Bloklamayan API Tasarımı:** Üretici genellikle kullanıcıya dönük kod yollarından (örn. REST denetleyicileri, olay işleyicileri, zamanlanmış görevler) çağrılır. Bildirimleri hızlıca kuyruğa ekleyen ve dönen hafif bir üretici, bu kod yollarının tepkisel kalmasını ve yavaş bildirim işlemleri için beklememesini sağlar.

- **Ölçeklenebilirlik:** Hafif bir üretici, yüksek hacimli bildirim gönderimlerini yönetebilir. Eğer üretici ağır olsaydı (örn. doğrulama, dönüştürme veya harici servis çağrıları yapsaydı), bir darboğaz haline gelir ve sistemin yüksek oranda bildirim kabul etme yeteneğini sınırlardı.

- **İlgi Alanlarının Ayrılması:** Üreticiyi basit tutarak ve yalnızca kuyruklamaya odaklayarak, sistem bildirim gönderimi (üreticinin sorumluluğu) ile bildirim işleme (tüketicinin sorumluluğu) arasında net bir ayrım sağlar. Bu ayrım, her bileşenin bağımsız olarak optimize edilmesine ve ölçeklenmesine olanak tanır.

- **Kuyruk Geri Basınç Yönetimi:** Kuyruk dolduğunda üretici bloklanır (`put()` işlemi kullanılıyorsa). Hafif bir üretici, bu bloklamanın etkisini en aza indirir—thread sadece bekler, minimal kaynak tüketir. Ek işlemler yapan ağır bir üretici, bloklama süreleri boyunca daha fazla kaynak israf ederdi.

- **Düşük Gecikme:** Bildirimleri tetikleyen kullanıcıya dönük işlemler (örn. kullanıcı kaydı, sipariş verme) hızlı bir şekilde tamamlanmalıdır. Hafif bir üretici, bildirim gönderme adımının bu işlemlere minimal gecikme eklemesini sağlar.

- **Kaynak Verimliliği:** Üreticideki ağır işlemler, tüketiciler için ayrılması gereken kaynakları (CPU, bellek, thread zamanı) tüketirdi. Üreticileri hafif tutarak, kaynaklar en önemli oldukları yere—bildirimlerin işlenmesi ve teslim edilmesine—tahsis edilir.

### Hata Yönetimi Stratejisi

`NotificationProducer` odaklanmış bir hata yönetimi stratejisi uygular:

- **Null Doğrulama:** Üretici, kuyruğa eklemeye çalışmadan önce `NotificationEvent`'in null olmadığını doğrular ve hemen bir `IllegalArgumentException` fırlatır. Bu hızlı hata (fail-fast) yaklaşımı, null olayların sisteme yayılmasını önler ve çağıranlara net geri bildirim sağlar.

- **InterruptedException Yönetimi:** Bloklayan `put()` işlemini kullanırken, üretici `InterruptedException`'ı şu şekilde düzgün bir şekilde yönetir:
    - Hata ayıklama ve izleme için kesilmeyi loglar.
    - Üst katmanlar için kesilme durumunu korumak adına thread üzerindeki kesme durumunu geri yükler (`Thread.currentThread().interrupt()`).
    - Çağıranların kesilmeyi uygun şekilde yönetmesine izin vermek için istisnayı (exception) yeniden fırlatır.
    
    Bu, thread kesilme sinyallerinin kaybolmamasını ve yukarı akış kodu (örn. uygulama kapatma senaryoları) tarafından yönetilebilmesini sağlar.

- **Beklenmeyen İstisna Sarma:** Kuyruklama sırasındaki diğer beklenmeyen istisnalar için (işlemin basitliği göz önüne alındığında bu nadir olmalıdır), üretici:
    - Sorun giderme için hatayı bağlamsal bilgiyle (olay ID'si) loglar.
    - İstisnayı açıklayıcı bir mesajla `RuntimeException` içine sarar.
    - İstisnanın uygun şekilde yönetilmesi için çağıranlara yayılmasına izin verir.

- **Yeniden Deneme Mantığı Yok:** Üretici, başarısız kuyruklama işlemleri için yeniden deneme mantığı uygulamaz. Yeniden denemeler, gerekirse daha üst bir seviyede (örn. çağıran tarafından veya uygulama seviyesindeki yeniden deneme mekanizmalarıyla) yönetilmelidir. Bu, üreticiyi basit tutar ve yeniden deneme politikalarına bağlanmasını önler.

- **Loglama Stratejisi:** Üretici uygun log seviyelerini kullanır:
    - Başarılı kuyruklama için `DEBUG` (prodüksiyonda log kirliliğini önlemek için).
    - İstisnalar için `ERROR` (hataların görünür ve eyleme geçirilebilir olmasını sağlamak için).

Bu hata yönetimi stratejisi, hataların üreticinin temel sorumluluğuna karmaşıklık eklemeden düzgün bir şekilde yüzeye çıkarılmasını sağlayarak basitlik ile sağlamlığı dengeler.

### Neden Bu Katman Tüketicileri Bilmemeli?

`NotificationProducer`, çeşitli mimari nedenlerle tüketicilerden habersiz olacak şekilde kasıtlı olarak tasarlanmıştır:

- **Gevşek Bağlılık (Loose Coupling):** Tüketicileri bilmeyerek, üretici tüketim mantığından ayrılmış kalır. Bu, tüketicilerin üretici kodunda değişiklik yapılmasını gerektirmeden eklenmesine, kaldırılmasına veya değiştirilmesine olanak tanır, sistem esnekliğini ve sürdürülebilirliğini artırır.

- **Tek Sorumluluk:** Üreticinin tek sorumluluğu bildirimleri kabul etmek ve kuyruğa eklemektir. Tüketiciler hakkında bilgi eklemek, sistemin diğer katmanlarına ait olan ek endişeleri (örn. tüketici kullanılabilirliği, işleme durumu, tüketici yapılandırması) beraberinde getirirdi.

- **Soyutlama Olarak Kuyruk:** `BlockingQueue`, üreticiler ve tüketiciler arasında bir soyutlama sınırı görevi görür. Üreticiler tüketici uygulamalarıyla değil, sadece kuyruk arayüzüyle etkileşime girer. Bu soyutlama, sistemin evrimleşmesine izin verir—tüketiciler üreticileri etkilemeden yeniden düzenlenebilir, değiştirilebilir veya ölçeklenebilir.

- **Ölçeklenebilirlik Bağımsızlığı:** Ayrıştırıldıklarında üreticiler ve tüketiciler bağımsız olarak ölçeklenebilir. Üretici sayısı gelen istek hacmine göre artabilirken, tüketici sayısı işleme kapasitesi ihtiyaçlarına göre ayarlanabilir ve her iki tarafın da diğerini bilmesi gerekmez.

- **Test Basitliği:** Tüketicileri bilmeyen bir üreticiyi izole bir şekilde test etmek daha kolaydır. Birim testleri, tüketici bağımlılıklarını taklit etmeye (mock) veya yapılandırmaya gerek kalmadan yalnızca kuyruklama davranışına odaklanabilir, bu da daha basit ve sürdürülebilir test paketlerine yol açar.

- **Tüketici Uygulamasında Esneklik:** Sistem, üreticinin bu detaylardan haberdar olmasına gerek kalmadan birden fazla tüketici uygulamasını (örn. farklı bildirim türleri için farklı işlemciler, paralel tüketiciler, öncelik tabanlı tüketiciler) destekleyebilir. Kuyruk, yönlendirme ve tamponlamayı halleder, tüketicilerin bağımsız olarak uygulanmasına ve geliştirilmesine izin verir.

- **Gelecekteki Genişletilebilirlik:** Üreticileri tüketicilerden ayırmak, sıkı sıkıya bağlı olsalardı zor olacak şekillerde sistemin evrimleşmesine izin verir. Örneğin, sistem daha sonra birden fazla kuyruk, kuyruk yönlendirme mantığı ekleyebilir veya hatta üretici değişiklikleri gerektirmeden dağıtık bir kuyruk uygulamasına geçebilir.

Bu tasarım, Üretici-Tüketici deseninin temel ilkesini takip eder: üreticiler ve tüketiciler birbirleriyle asla doğrudan değil, sadece paylaşılan kuyruk aracılığıyla iletişim kurar. Bu ilke, endişelerin temiz bir şekilde ayrılmasını sağlar ve daha sürdürülebilir ve ölçeklenebilir bir mimariyi teşvik eder.

## 9. NotificationConsumer Worker (Bildirim Tüketici İşçisi)

`NotificationConsumerWorker`, `BlockingQueue`'yu bildirim olayları için sürekli olarak yoklamaktan ve bunları `NotificationProcessor` aracılığıyla işlemekten sorumludur. Birden fazla tüketici işçi thread'i arka planda eşzamanlı olarak çalışır, her biri kuyruğu yoklayan, olayları alan ve bunları asenkron olarak işleyen sonsuz bir döngü yürütür.

### İş Parçacığı Modeli (Thread Model)

Tüketici işçisi, birden fazla tüketici thread'ini yönetmek için sabit boyutlu bir thread havuzu (`ExecutorService`) kullanır:

- **Thread Havuzu Oluşturma:** Başlatma sırasında sabit sayıda thread'e sahip bir `ExecutorService` (yapılandırılabilir: `notification.queue.consumer-threads`, varsayılan: 2) oluşturulur. Havuzdaki her thread ayrı bir tüketici işçi döngüsü çalıştırır.

- **İşçi Thread İsimlendirme:** Tüketici thread'leri, thread dökümlerinde, loglamada ve hata ayıklamada kolay tanımlama için "notification-consumer" olarak adlandırılır. Thread'ler, JVM'i canlı tutmalarını ve düzgün bir şekilde kapatılabilmelerini sağlamak için non-daemon (daemon olmayan) thread'ler olarak oluşturulur.

- **Bağımsız Yürütme:** Her tüketici thread'i bağımsız olarak çalışır, kuyruğu yoklar ve olayları eşzamanlı olarak işler. Bu paralel işleme, birden fazla bildirimin aynı anda işlenmesine izin vererek iş hacmini artırır.

- **Blocking Queue Etkileşimi:** Tüm tüketici thread'leri aynı `BlockingQueue` örneğini paylaşır. Kuyruğun thread-safe uygulaması, birden fazla thread'in yarış koşulları veya veri bozulması olmadan kuyruğu güvenli bir şekilde eşzamanlı olarak yoklayabilmesini sağlar. Bir thread `take()` çağırdığında, bir olay mevcut olana kadar bloklanır ve CPU kaynağı tüketmeden verimli bir şekilde bekler.

- **Thread Yaşam Döngüsü:** Tüketici thread'leri Spring bean başlatması sırasında (`NotificationConsumerWorker` bean'i oluşturulduğunda) başlatılır ve uygulama kapanana kadar çalışmaya devam eder. Thread'ler, thread oluşturma, yaşam döngüsü ve sonlandırmayı yöneten `ExecutorService` tarafından yönetilir.

- **Kaynak Tahsisi:** Her tüketici thread'i beklerken (`take()` işleminde bloklanmışken) minimal kaynak tüketir. CPU kullanımı yalnızca aktif olarak bildirim işlerken gerçekleşir. Bu verimli kaynak kullanımı, önemli bir ek yük olmadan birden fazla tüketici thread'inin çalışmasına izin verir.

### Sonsuz Döngü Burada Neden Güvenlidir?

Tüketici işçileri, bu kullanım durumu için güvenli ve uygun olan sonsuz bir `while` döngüsü kullanır (`while (running.get() && !Thread.currentThread().isInterrupted())`):

- **Bloklayan İşlemler:** Döngü, kuyruk boş olduğunda thread yürütmesini askıya alan bloklayan bir işlem (`notificationQueue.take()`) içerir. Thread bloklanmışken CPU döngüsü tüketmez, olayların gelmesini verimli bir şekilde bekler. Sonsuz döngü, CPU kaynaklarını tüketecek bir "meşgul bekleme" (busy-wait) döngüsü değildir.

- **Kontrollü Sonlandırma:** Döngü iki koşul tarafından kontrol edilir:
    - `running.get()`: Zarif kapatma sırasında `false` olarak ayarlanabilen bir `AtomicBoolean` bayrağı.
    - `Thread.currentThread().isInterrupted()`: Uygulama kapatma sırasında olduğu gibi harici sonlandırma sinyallerinin döngüyü kırmasına izin veren bir thread kesilme kontrolü.

- **İstisna (Exception) Yönetimi:** Döngü, thread `take()` üzerinde bloklanmışken kesildiğinde fırlatılan `InterruptedException`'ı düzgün bir şekilde yönetir. Kesildiğinde döngü kırılır ve thread'in temiz bir şekilde çıkmasına izin verilir. Bu, kapatma sinyallerinin (kesilmelerin) düzgün bir şekilde yönetilmesini sağlar.

- **Uygulama Ömrü Uyumu:** Sonsuz döngü, uygulamanın ömrüyle uyumludur—tüketici thread'leri uygulama çalışırken sürekli çalışmalı, bildirimler geldikçe işlemelidir. Döngü, uygulama kapandığında (kapatma mekanizması aracılığıyla) doğal olarak sonlanır, bu da sonsuz bir döngüyü doğru tasarım deseni yapar.

- **Kaynak Sızıntısı Yok:** Diğer bağlamlardaki sınırsız döngülerin aksine, bu döngü kaynak sızıntısına neden olmaz çünkü:
    - Bloklayan işlemler CPU kaynaklarını serbest bırakır.
    - Thread'ler kapatılabilen `ExecutorService` tarafından yönetilir.
    - Döngü her iterasyonda sonlandırma koşullarını kontrol eder.
    - InterruptedException düzgün bir şekilde yönetilir, temiz çıkışa izin verir.

- **Standart Desen:** Bloklayan işlemler içeren sonsuz döngüler, üretici-tüketici sistemlerinde, thread havuzlarında ve sunucu uygulamalarında standart bir desendir. Bir kuyruktan öğeleri işleyen uzun ömürlü işçi thread'lerini uygulamanın deyimsel yolu budur.

Sonsuz döngü güvenlidir çünkü verimli bir şekilde bloklanır, sonlandırma koşullarını kontrol eder, kesilmeleri düzgün bir şekilde yönetir ve sürekli işleme şeklindeki uygulamanın operasyonel modeliyle uyumludur.

### Zarif Kapatma (Graceful Shutdown) Nasıl Çalışmalı?

Zarif kapatma, uygulama sonlandırılmadan önce tüketici işçilerinin yeni olayları işlemeyi durdurmasını ve devam eden işleri tamamlamasını sağlar. `NotificationConsumerWorker`, Spring'in `@PreDestroy` yaşam döngüsü kancasını kullanarak zarif kapatmayı uygular:

1. **Kapatma Sinyali:** Spring uygulama kapanışını algıladığında (örn. SIGTERM sinyali, kapatma uç noktası veya uygulama bağlamı kapanışı), `NotificationConsumerWorker` bean'i üzerindeki `@PreDestroy` ile işaretlenmiş `shutdown()` metodunu çağırır.

2. **Çalışıyor Bayrağı Güncellemesi:** Kapatma süreci önce `running.set(false)` kullanarak `running` bayrağını `false` yapar. Bu, tüketici thread'lerinin mevcut iterasyonu tamamladıktan sonra (`while (running.get() && ...)` koşulunu kontrol ettiklerinde) döngülerinden çıkmasına neden olur.

3. **ExecutorService Kapatma:** `ExecutorService.shutdown()` metodu çağrılır, bu da:
    - Yeni görevlerin gönderilmesini engeller.
    - Devam eden görevlerin (tüketici döngülerinin) tamamlanmasına izin verir.
    - Thread'leri zorla sonlandırmaz.

4. **Zarif Bekleme:** Kod, `executorService.awaitTermination(30, TimeUnit.SECONDS)` kullanarak thread'lerin sonlanmasını bekler. Bu, tüketici thread'lerine şunları yapmaları için 30 saniyeye kadar süre verir:
    - `running` bayrağını kontrol etmek.
    - Devam eden bildirim işlemlerini tamamlamak.
    - Döngülerinden çıkmak ve sonlanmak.

5. **Zaman Aşımı Yönetimi:** Eğer thread'ler zaman aşımı süresi içinde sonlanmazsa:
    - `shutdownNow()` çağrılır, bu da çalışan tüm thread'leri keser (interrupt).
    - Bu, `take()` üzerinde bloklanmış thread'lerde `InterruptedException`'a neden olarak çıkış yapmalarını sağlar.
    - Daha kısa bir zaman aşımıyla (10 saniye) başka bir bekleme, thread'lerin sonlanması için son bir fırsat sağlar.

6. **Zorla Sonlandırma:** Zorla kapatma işleminden sonra thread'ler hala sonlanmazsa bir hata loglanır, ancak kapatma süreci devam eder. Pratikte, thread'ler kesilmeden sonra hızlıca sonlanmalıdır.

7. **Kesilme Yönetimi:** Eğer kapatma sürecinin kendisi kesilirse (olası değil ama mümkün), thread'lerin kesilmesini sağlamak için `shutdownNow()` çağrılır ve mevcut thread üzerindeki kesilme durumu geri yüklenir.

**Önemli Hususlar:**

- **Devam Eden İşleme:** Kapatma başladığında işlenmekte olan bildirimler işlemlerini tamamlayacaktır (işlemci mantığının işi hızlıca hallettiği varsayılarak). Kapatma başladığında kuyrukta bekleyen bildirimler işlenmeyecektir (bellek içi, kalıcı olmayan tasarımla tutarlı olarak kaybolurlar).

- **Kapatma Zaman Aşımı:** 30 saniyelik zaman aşımı çoğu bildirim işlemi için yeterli olmalıdır, ancak tipik işlem sürelerine göre ayarlanması gerekebilir. Çok uzun süren işlemci operasyonları kesilebilir.

- **Thread Kesilmesi (Interruption):** Kapatma mekanizması, `take()` üzerinde bloklanmış thread'leri uyandırmak için thread kesilmesine dayanır. İşlemci uygulamaları, kapatmanın doğru çalışmasını sağlamak için `InterruptedException`'ı yutmaktan kaçınmalıdır.

- **Uygulama Bağlamı:** Zarif kapatma, Spring'in uygulama bağlamı yaşam döngüsüyle birlikte çalışır. Bağlam kapatıldığında, `@PreDestroy` metotları çağrılır ve kapatma dizisini tetikler.

Bu zarif kapatma mekanizması, uygulamanın thread'leri çalışır durumda bırakmadan veya durumu bozabilecek ya da kaynakları tutarsız bir durumda bırakabilecek ani sonlandırmaya zorlamadan temiz bir şekilde kapanmasını sağlar.

## 10. Bildirim İşleyicileri (Notification Handlers)

Bildirim motoru, farklı bildirim türlerini (E-posta, SMS, Push vb.) işlemek için arayüz tabanlı bir işleyici mimarisi kullanır. Bu tasarım, mevcut kodu değiştirmeden yeni bildirim kanallarının eklenmesine olanak tanıyan genişletilebilirlik sağlar.

### Mimari Genel Bakış

İşleyici sistemi üç temel bileşenden oluşur:

- **`NotificationHandler` Arayüzü:** İki metot ile tüm bildirim işleyicileri için sözleşmeyi tanımlar:
    - `canHandle(NotificationType)`: İşleyicinin belirli bir bildirim türünü destekleyip desteklemediğini belirler.
    - `handle(NotificationEvent)`: Bildirimi işler ve teslim eder.

- **İşleyici Uygulamaları:** Her bildirim kanalı için somut uygulamalar:
    - `EmailHandler`: E-posta bildirimlerini yönetir (mock uygulama).
    - `SMSHandler`: SMS bildirimlerini yönetir (mock uygulama).
    - `PushHandler`: Push bildirimlerini yönetir (mock uygulama).
    - `NotificationHandler` arayüzü uygulanarak ek işleyiciler eklenebilir.

- **`NotificationHandlerRegistry`:** İşleyici kaydını ve seçimini yönetir, bildirim türüne göre işleyici araması sağlar.

- **`HandlerBasedNotificationProcessor`:** Kayıt defteri aracılığıyla uygun işleyiciyi seçerek ve işlemeyi ona devrederek bildirim işlemeyi koordine eder.

### İşleyici Seçim Mantığı

İşleyici seçim mantığı `NotificationHandlerRegistry.getHandler()` metodunda uygulanır ve şu adımları izler:

1. **Önbellek Araması:** Önce, önceden çözümlenmiş işleyiciler için bellek içi bir önbelleği (`ConcurrentHashMap`) kontrol eder. Bu, ilk seçimden sonra bilinen bildirim türleri için O(1) arama performansı sağlar.

2. **İşleyici İterasyonu:** Önbellekte yoksa, kayıtlı tüm işleyiciler (Spring tarafından `NotificationHandler` bean'leri listesi olarak enjekte edilir) üzerinde yineler. Her işleyici için, istenen bildirim türünü destekleyip desteklemediğini kontrol etmek üzere `canHandle(notificationType)` çağrılır.

3. **İlk Eşleşme Seçimi:** `canHandle()`'dan `true` döndüren ilk işleyiciyi döndürür. Bu, işleyicilerin öncelik sırasına göre kaydedilmesine olanak tanır, ancak tipik olarak her işleyici tam olarak bir bildirim türünü destekler.

4. **Önbellek Depolama:** Bir işleyici bulunduğunda, aynı bildirim türü için sonraki isteklerde yinelemeye gerek kalmaması için önbellekte saklanır.

5. **Hata Yönetimi:** Verilen bildirim türü için hiçbir işleyici bulunamazsa, açıklayıcı bir hata mesajıyla `IllegalArgumentException` fırlatır. Bu hızlı hata yaklaşımı, desteklenmeyen bildirim türlerinin hemen algılanmasını sağlar.

**Seçim Algoritması Sözde Kodu (Pseudocode):**
```
getHandler(notificationType):
  if önbellekte varsa: return önbellekteki işleyici
  her kayıtlı işleyici için:
    if handler.canHandle(notificationType):
      işleyiciyi önbelleğe al
      return işleyici
  throw IllegalArgumentException("İşleyici bulunamadı")
```

### Tasarım Prensipleri

İşleyici mimarisi birkaç temel tasarım prensibini takip eder:

- **Açık/Kapalı Prensibi (Open/Closed Principle):** Sistem genişlemeye açıktır (yeni işleyiciler eklenebilir) ancak değişime kapalıdır (yeni işleyiciler eklerken mevcut işleyicilerin ve kayıt defterinin değişmesine gerek yoktur).

- **Tek Sorumluluk:** Her işleyici tek bir bildirim kanalından sorumludur, bu da uygulamaları odaklanmış ve sürdürülebilir tutar.

- **Strateji Deseni:** İşleyici seçim mekanizması Strateji desenini kullanır, bu da işleyicileri seçme algoritmasının onları kullanan koddan bağımsız olarak değişmesine izin verir.

- **Bağımlılık Enjeksiyonu:** İşleyiciler Spring'in bağımlılık enjeksiyonu aracılığıyla otomatik olarak keşfedilir ve kaydedilir. Yeni işleyiciler, yapılandırma değişikliği olmadan otomatik olarak kayıt defterine dahil edilir.

- **Gevşek Bağlılık:** İşleyiciler birbirlerini veya kayıt defterinin uygulama detaylarını bilmezler. Sadece `NotificationHandler` arayüzünü uygulamaları gerekir.

### Genişletilebilirlik

Yeni bir bildirim işleyicisi eklemek basittir ve mevcut kodda değişiklik gerektirmez:

1. **İşleyici Uygulaması Oluştur:** `NotificationHandler` arayüzünü uygula:
    ```java
    @Component
    public class NewChannelHandler implements NotificationHandler {
        @Override
        public boolean canHandle(NotificationType type) {
            return NotificationType.NEW_CHANNEL == type;
        }
       
        @Override
        public void handle(NotificationEvent event) throws Exception {
            // Uygulama mantığı
        }
    }
    ```

2. **Bildirim Türü Ekle:** Gerekirse, `NotificationType` enum'ına yeni bir değer ekle.

3. **Otomatik Kayıt:** Spring, yeni işleyici bean'ini otomatik olarak keşfeder ve kayıt defterine dahil eder. Yapılandırma değişikliği gerekmez.

4. **İşleyici Seçimi:** Kayıt defteri, yeni işleyiciyi seçim mantığına otomatik olarak dahil eder. Yeni türdeki bildirimler yeni işleyiciye yönlendirilir.

Bu genişletilebilirlik, mevcut işleyicileri, kayıt defterini veya işlemciyi değiştirmeden yeni bildirim kanalları (örn. Slack, Discord, Microsoft Teams) için destek eklemeyi kolaylaştırır.

### Mock (Taklit) Uygulamalar

Mevcut işleyici uygulamaları (EmailHandler, SMSHandler, PushHandler), gösterim ve test amaçlı tasarlanmış mock uygulamalardır:

- **EmailHandler:** Loglama ve simüle edilmiş bir gecikme (150ms) ile e-posta gönderimini simüle eder. Prodüksiyonda bu, SendGrid, AWS SES veya Mailgun gibi e-posta servis sağlayıcılarıyla entegre olur.

- **SMSHandler:** Loglama ve simüle edilmiş bir gecikme (100ms) ile SMS gönderimini simüle eder. Prodüksiyonda bu, Twilio, AWS SNS veya Vonage gibi SMS ağ geçidi sağlayıcılarıyla entegre olur.

- **PushHandler:** Loglama ve simüle edilmiş bir gecikme (120ms) ile push bildirim gönderimini simüle eder. Prodüksiyonda bu, Firebase Cloud Messaging, Apple Push Notification Service veya OneSignal gibi push bildirim servisleriyle entegre olur.

Bu mock uygulamalar, harici servis entegrasyonlarına, API anahtarlarına veya ağ bağlantısına gerek kalmadan sistemin test edilmesine ve gösterilmesine olanak tanır. Bunları gerçek uygulamalarla değiştirmek basittir: aynı arayüz sözleşmesini koruyarak `handle()` metodunu gerçek servis API'sini çağıracak şekilde güncelleyin.

### Hata Yönetimi

Her işleyici, kendi bildirim kanalına özgü hataları yönetmekten sorumludur:

- **İşleyici Seviyesi Hatalar:** Bir işleyicinin `handle()` metodu bir istisna fırlatırsa, istisna `NotificationConsumerWorker`'a yayılır; o da hatayı loglar ve bir sonraki bildirimi işlemeye devam eder. Bu, başarısız olan bir bildirimin tüm tüketici thread'ini durdurmamasını sağlar.

- **Seçim Hataları:** Bir bildirim türü için hiçbir işleyici bulunamazsa, bir hata olarak loglanan `IllegalArgumentException` fırlatılır. Bu genellikle bir yapılandırma sorununu gösterir (örn. bir bildirim türü eklendi ancak hiçbir işleyici uygulanmadı).

- **Kanala Özgü Hatalar:** İşleyiciler kanala özgü istisnalar fırlatabilir (örn. `EmailDeliveryException`, `SMSGatewayException`). İşlemci ve tüketici katmanları bunları genel olarak ele alır ve uygun şekilde loglar. Prodüksiyon sistemleri için işleyiciler yeniden deneme mantığı, ölü mektup kuyrukları (dead-letter queues) veya yedek mekanizmalar uygulayabilir.

## 11. REST API Controller (REST API Denetleyicisi)

Bildirim motoru, bildirim isteklerini göndermek için bir REST API uç noktası sunar. API, isteği kabul ettikten hemen sonra bildirim teslimini beklemeden dönecek şekilde asenkron işleme için tasarlanmıştır.

### API Uç Noktası

**POST** `/api/notifications`

Asenkron işleme için bir bildirim isteği gönderir.

**İstek Gövdesi** (JSON):
```json
{
  "notificationType": "EMAIL",
  "recipient": "user@example.com",
  "subject": "Welcome",
  "body": "Welcome to our service!",
  "priority": "NORMAL",
  "metadata": {
    "campaignId": "123"
  }
}
```

**Yanıt** (HTTP 202 Accepted):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACCEPTED",
  "message": "Notification submitted for processing",
  "submittedAt": "2024-01-15T10:30:00"
}
```

### Girdi Doğrulama

API, Bean Validation notasyonlarını kullanarak gelen istekleri doğrular:

- **`notificationType`**: Zorunlu, geçerli bir `NotificationType` enum değeri olmalıdır (EMAIL, SMS, PUSH, IN_APP, WEBHOOK)
- **`recipient`**: Zorunlu, boş olmamalıdır (örn. e-posta adresi, telefon numarası, cihaz token'ı)
- **`body`**: Zorunlu, boş olmamalıdır (bildirim mesajı içeriği)
- **`subject`**: İsteğe bağlı, başlıklı e-posta ve push bildirimleri için kullanılır
- **`priority`**: İsteğe bağlı, sağlanmazsa NORMAL varsayılır (LOW, NORMAL, HIGH, URGENT)
- **`metadata`**: İsteğe bağlı, ek bağlam için anahtar-değer haritası

Doğrulama hataları, hangi alanların doğrulamadan geçemediğini belirten ayrıntılı hata mesajlarıyla birlikte HTTP 400 Bad Request döndürür.

### Asenkron Davranış Açıklaması

REST API uç noktası tamamen asenkron bir davranış sergiler:

1. **Anında Yanıt:** Denetleyici, isteği doğrulayıp bildirimi kuyruğa ekledikten hemen sonra HTTP 202 Accepted döndürür. Yanıt benzersiz bir bildirim ID'si içerir ancak bildirim teslimini beklemez.

2. **İstek İşleme Akışı:**
    - İstemci `/api/notifications` adresine POST isteği gönderir.
    - Denetleyici isteği doğrular (senkron, hızlı işlem).
    - Servis, oluşturulan bir UUID ile `NotificationEvent` yaratır.
    - Servis, olayı `BlockingQueue` içine ekler (kuyruk doluysa bloklanabilir, ancak bu bir kuyruk işlemidir, bildirim teslimi değildir).
    - Denetleyici, bildirim ID'si ile HTTP 202 Accepted döndürür.
    - İstemci yanıtı alır (istemci açısından istek işleme tamamlanmıştır).
    - **Bu arada, arka planda:** Tüketici işçi thread'leri kuyruğu yoklar ve bildirimleri asenkron olarak işler.

3. **Teslimat İçin Bekleme Yok:** API şunları beklemez:
    - Harici servislere (e-posta sağlayıcıları, SMS ağ geçitleri vb.) bildirim teslimi.
    - İşleyici işleme tamamlanması.
    - Tüketici işçisi işlemesi.
    - Harici bildirim servislerine yapılan herhangi bir ağ çağrısı.

4. **Ateşle ve Unut (Fire-and-Forget) Modeli:** Bildirim kuyruğa eklenip HTTP 202 döndürüldüğünde, API'nin sorumluluğu sona erer. Bildirim teslimi arka planda gerçekleşir, HTTP istek/yanıt döngüsünden tamamen ayrılmıştır.

5. **Kuyruk İşlemi:** Potansiyel olarak bloklayan tek işlem, sadece kuyruk dolu olduğunda (geri basınç uygulayarak) bloklanan kuyruk ekleme işlemidir (`put()`). Ancak bu yine de gerçek bildirim teslimini beklemekten kat kat daha hızlıdır ve sınırsız kuyruk büyümesini önleyerek sistem kararlılığını sağlar.

### HTTP Durum Kodları

- **202 Accepted:** Bildirim isteği kabul edildi ve başarıyla kuyruğa eklendi
- **400 Bad Request:** İstek doğrulama başarısız oldu (eksik zorunlu alanlar, geçersiz değerler)
- **500 Internal Server Error:** İstek işleme sırasında beklenmeyen bir hata oluştu
- **503 Service Unavailable:** Servis işleme sırasında kesintiye uğradı (nadir, sistemin kapandığını gösterir)

### Yanıt Süresi Karakteristikleri

- **Tipik Yanıt Süresi:** < 10ms (doğrulama + kuyruğa ekleme, kuyrukta kapasite olduğu varsayılarak)
- **Yük Altında:** Kuyruk doluysa artabilir (üretici yer açılmasını beklerken bloklanır), ancak yine de senkron bildirim tesliminden çok daha hızlıdır
- **Ağ Bağımsızlığı:** Yanıt süresi; harici bildirim servisi gecikmesinden, ağ koşullarından veya bildirim teslim başarısı/başarısızlığından bağımsızdır

### Hata Yönetimi

- **Doğrulama Hataları:** HTTP 400 ve ayrıntılı alan seviyesi hata mesajlarıyla hemen döndürülür
- **Kuyruk Dolu:** Kuyruk kapasitedeyse, `put()` işlemi yer açılana kadar bloklanır. Bu, istemcilere geri basınç uygular ancak hiçbir bildirimin kaybolmamasını sağlar
- **Kesilme (Interruption):** Servis kapanıyorsa ve thread kesilirse, HTTP 503 döndürür
- **İşleme Hataları:** Bildirim teslimi sırasındaki hatalar (HTTP 202 döndürüldükten sonra) tüketici işçileri tarafından yönetilir ve loglanır, ancak API yanıtını etkilemez

### Kullanım Durumları

Asenkron API tasarımı şunlar için idealdir:

- **Yüksek İş Hacmi Senaryoları:** Yavaş harici servisler tarafından bloklanmadan saniyede binlerce bildirim isteğini kabul edebilir
- **Kullanıcıya Dönük Operasyonlar:** Bildirimleri tetikleyen kullanıcı kaydı, sipariş verme gibi işlemler bildirim teslimini beklemez, kullanıcı deneyimini iyileştirir
- **Toplu İşlemler:** Her birinin teslim edilmesini beklemeden çok sayıda bildirimi hızlıca gönderebilir
- **Dayanıklı Sistemler:** Harici bildirim servisi kesintileri API'yi bloklamaz (servisler düzeldiğinde işlenmek üzere bildirimler kuyruğa alınır)

Asenkron doğası, istemcilerin sadece API yanıtından bildirim teslim durumunu belirleyemeyeceği anlamına gelir. Teslim durumu için, istemcilerin ayrı takip mekanizmaları (örn. webhooklar, durum sorgulama, olay logları) uygulaması gerekir.

## 12. Sistem Sınırlamaları ve Mimari Değerlendirmeler

CoreNotifyEngine'in sınırlamalarını ve sınırlarını anlamak, bilinçli mimari kararlar almak için çok önemlidir. Bu bölüm, sistemin kısıtlamalarını ana hatlarıyla belirtir, neden Kafka gibi dağıtık mesajlaşma sistemlerinin yerini alamayacağını açıklar ve gelecekteki iterasyonlar için potansiyel iyileştirmeleri tartışır.

### Sistem Sınırlamaları

#### Bellek İçi (In-Memory) Kuyruk Kısıtlamaları

Bildirim motoru, bellek içi bir `BlockingQueue` kullanır ve bu da çeşitli kısıtlamalar getirir:

- **Bellek Sınırlılığı:** Kuyruk kapasitesi mevcut heap belleği ile sınırlıdır. 10.000 kapasiteli ve ortalama bildirim boyutu 1KB olan bir kuyruk, sadece kuyruk depolama için yaklaşık 10MB heap alanı tüketir (nesne ek yükü hariç). Heap boyutu sınırları, maksimum kuyruk boyutunu etkili bir şekilde sınırlar.

- **Kalıcılık Yok:** Tüm kuyruklanmış bildirimler sadece JVM heap belleğinde bulunur. Sistem çökmeleri, beklenmeyen JVM sonlandırmaları veya bellek yetersizliği durumları, henüz işlenmemiş tüm kuyruklanmış bildirimlerin anında kaybolmasına neden olur.

- **Çöp Toplayıcı (Garbage Collection) Etkisi:** Çok sayıda bildirim nesnesi içeren büyük kuyruklar GC baskısını artırır. Sık GC duraklamaları hem ekleme hem de çıkarma işlemlerini geçici olarak yavaşlatabilir, gecikmeyi ve iş hacmini etkileyebilir.

- **Kuyruk Boyutu Ayarlama:** Optimal kuyruk boyutu birden fazla faktöre bağlıdır: beklenen bildirim hacmi, işleme oranı, mevcut bellek ve kabul edilebilir gecikme. Çok küçük bir kuyruk sık sık üretici bloklanmasına neden olur; çok büyük bir kuyruk belleği israf eder ve GC ek yükünü artırır.

#### Veri Kaybı Senaryoları

Birkaç senaryo bildirim kaybına yol açabilir:

1. **Uygulama Yeniden Başlatma:** Normal uygulama kapanışı, zarif kapatma olsa bile, henüz işlenmemiş tüm kuyruklanmış bildirimlerin kaybıyla sonuçlanır. Kapatma işlemi devam eden bildirimleri tamamlar ancak kuyruklanmış öğeleri atar.

2. **Beklenmeyen Sonlandırma:** JVM çökmeleri, `kill -9` sinyalleri, bellek yetersizliği hataları veya sistem arızaları, herhangi bir kurtarma mekanizması olmaksızın tüm kuyruklanmış bildirimlerin anında kaybına neden olur.

3. **Kuyruk Taşması (Bloklama Davranışı):** Sınırlı kuyruk, sınırsız bellek büyümesini önlese de, kuyruk dolduğunda üreticiler `put()` işlemlerinde bloklanır. Üreticiler bloklanmışken uygulama sonlandırılırsa, bu bildirimler kaybolur.

4. **İşleme Hataları:** Bir tüketici thread işleme sırasında çökerse veya yönetilmeyen bir istisna ile karşılaşırsa, o anda işlenmekte olan bildirim kaybolur (tüketici döngüsündeki istisna yönetimi ile hafifletilmiş olsa da).

Bu veri kaybı özellikleri, sistemi garantili teslimat gerektiren veya bildirim kaybının kabul edilemez olduğu kullanım durumları için uygunsuz hale getirir.

#### Ölçeklenebilirlik Sınırları

Sistemin ölçeklenebilirliği temel olarak tek örnek (single-instance) kısıtlamalarıyla sınırlıdır:

- **Sadece Dikey Ölçekleme:** Ölçeklenebilirlik yalnızca dikey ölçekleme (CPU çekirdeklerini, belleği, thread sayısını artırma) ile elde edilir. Yatay ölçekleme yeteneği yoktur—birden fazla uygulama örneği çalıştırmak genel kapasiteyi artırmaz; her örnek kendi bağımsız kuyruğunu korur.

- **Thread Havuzu Sınırlamaları:** Tüketici thread sayısı yapılandırılabilir ancak şunlarla sınırlıdır:
    - Mevcut CPU çekirdekleri (optimal thread sayısı genellikle I/O ağırlıklı iş yükleri için CPU çekirdeklerinin 1-2 katıdır)
    - JVM thread sınırları (genellikle binlerce, ancak aşırı thread bağlam değiştirme ek yüküne neden olur)
    - Thread başına bellek (her thread kendi yığınına sahiptir, genellikle thread başına 1MB)

- **Kuyruk İş Hacmi:** Maksimum iş hacmi şunlarla sınırlıdır:
    - Tüketici thread sayısı ve işleme hızı
    - Kuyruk kapasitesi (ne kadar işin tamponlanabileceğini etkiler)
    - İşleyici işleme gecikmesi (harici servislere yapılan ağ çağrıları)

- **Bellek Darboğazı:** Bildirim hacmi arttıkça, bellek birincil darboğaz haline gelir. Daha büyük kuyruklar daha fazla bellek gerektirir ve aynı anda daha fazla bildirim işlemek bellek baskısını artırır.

Pratik ölçeklenebilirlik sınırları: Sistem tipik olarak, işleme karmaşıklığına bağlı olarak tek bir örnekte dakikada binlerce ila on binlerce bildirimi işleyebilir, ancak tek bir makinenin kaynaklarının ötesine geçemez.

#### Tek Örnek (Single-Instance) Sınırlamaları

Motor tamamen tek bir JVM örneği içinde çalışır, bu da çeşitli kısıtlamalar getirir:

- **Dağıtık Koordinasyon Yok:** Birden fazla uygulama örneği iş yükünü koordine edemez veya paylaşamaz. Her örnek kendi kuyruğu, tüketicileri ve işleme mantığı ile bağımsız çalışır. Örnekler arası iletişim veya yük dengeleme yoktur.

- **Yüksek Erişilebilirlik (HA) Yok:** Sistemin yerleşik bir yüksek erişilebilirlik mekanizması yoktur. Tek örnek başarısız olursa, tüm işleme durur ve tüm kuyruklanmış bildirimler kaybolur. Yük devretme (failover) veya yedeklilik yoktur.

- **Örnek Seviyesinde İzolasyon:** Bir örneğe gönderilen bildirimler diğer örneklere asla görünmez. Bu, istemcilerin durumu kontrol etmek için her zaman aynı örneğe bağlanması gerektiği anlamına gelir (mevcut tasarımda durum kontrolü olmasa da).

- **Dağıtım Kısıtlamaları:** Yatay ölçekleme stratejileri (örnekler arasında yük dengeleme), örnek başına bağımsız kuyruklarla sonuçlanır, bu da tek tip bildirim işlemeyi garanti etmeyi veya öncelik sıralaması ya da global hız sınırlaması gibi örnekler arası özellikleri uygulamayı imkansız hale getirir.

### Neden Bu Sistem Bir Kafka Alternatifi DEĞİLDİR?

CoreNotifyEngine, Apache Kafka veya benzeri dağıtık mesajlaşma sistemlerinin yerine geçecek bir sistem olarak düşünülmemeli veya kullanılmamalıdır. Mimariler temel olarak farklı amaçlara hizmet eder:

#### Dayanıklılık (Durability) Eksikliği

- **Kafka:** Yapılandırılabilir saklama politikalarıyla dayanıklı mesaj depolaması sağlar. Mesajlar diske kaydedilir ve broker yeniden başlatmalarında, çökmelerde ve arızalarda hayatta kalır. Mesajlar saatlerce, günlerce veya süresiz olarak saklanabilir.

- **CoreNotifyEngine:** Mesajlar sadece bellekte bulunur ve yeniden başlatma veya çökme durumunda kaybolur. Kalıcılık katmanı, disk depolaması ve saklama mekanizması yoktur.

#### Dağıtık Konsensüs Yok

- **Kafka:** Küme koordinasyonu, lider seçimi, bölüm (partition) yönetimi ve metadata senkronizasyonu için dağıtık konsensüs algoritmaları (ZooKeeper/KRaft) kullanır. Birden fazla broker, otomatik yük devretme ile uyumlu bir küme olarak çalışabilir.

- **CoreNotifyEngine:** Dağıtık koordinasyon olmaksızın tek bir bağımsız örnek olarak çalışır. Küme, konsensüs mekanizması ve dağıtık durum yönetimi yoktur.

#### Tekrar Oynatma (Replay) Yeteneği Yok

- **Kafka:** Tüketici grupları ve ofset yönetimi aracılığıyla mesaj tekrarını destekler. Tüketiciler log'daki herhangi bir noktadan mesajları yeniden okuyabilir, bu da geçmiş olayların tekrarını, verilerin yeniden işlenmesini ve olay kaynaklama (event sourcing) desenlerini mümkün kılar.

- **CoreNotifyEngine:** Mesajlar bir kez tüketilir ve hemen kuyruktan kaldırılır. Geçmiş, log saklama ve tekrar oynatma yeteneği yoktur. Bir mesaj tüketildikten sonra tekrar okunamaz.

#### JVM'e Bağlı Yaşam Döngüsü

- **Kafka:** Uygulama JVM'lerinden bağımsız, kendi yaşam döngüsüne sahip bağımsız bir dağıtık sistem olarak çalışır. Kafka broker'ları, uygulama dağıtımları boyunca mesaj dayanıklılığını ve kullanılabilirliğini koruyarak uygulama örneklerinden daha uzun yaşayabilir.

- **CoreNotifyEngine:** Uygulama JVM yaşam döngüsüne sıkı sıkıya bağlıdır. Uygulama durduğunda motor da durur. Kalıcılık veya bağımsız çalışma yoktur. Kuyruk, uygulama sürecinden bağımsız olarak var olamaz.

#### Ek Farklılıklar

- **İş Hacmi:** Kafka, bir küme genelinde saniyede milyonlarca mesajı işleyebilir. CoreNotifyEngine, tek bir örnekte saniyede binlerce ila on binlerce ile sınırlıdır.

- **Bölümleme (Partitioning):** Kafka, paralel işleme ve ölçeklenebilirlik için konu bölümlemeyi destekler. CoreNotifyEngine'de bölümleme kavramı yoktur.

- **Sıralama Garantileri:** Kafka, bölümler içinde sıralama garantileri sağlar. CoreNotifyEngine, yalnızca tek bir örneğin kuyruğu içinde FIFO sıralaması sağlar.

- **Çoklu Abone Desenleri:** Kafka, aynı mesajları bağımsız olarak okuyan birden fazla tüketici grubunu destekler. CoreNotifyEngine, örnek başına tek bir tüketici havuzuna sahiptir.

**Kullanım Durumu Rehberi:** Dayanıklılık, yüksek iş hacmi, tekrar oynatma yeteneği ve çoklu abone desenleri gerektiren dağıtık sistemler için Kafka kullanın. Basitlik ve sıfır altyapı yükünün öncelikli olduğu hafif, tek örnekli bildirim işleme için CoreNotifyEngine kullanın.

### Olası Gelecek İyileştirmeleri

Mevcut uygulama basitliği ve sıfır altyapı bağımlılığını önceliklendirse de, birkaç iyileştirme sistemi prodüksiyon kullanım durumları için geliştirebilir:

#### Kalıcılık Seçenekleri

- **Veritabanı Destekli Depo:** Bildirimlerin kuyruğa eklenmeden önce kalıcı hale getirildiği veritabanı destekli bir kuyruk uygulayın. Bu, yeniden başlatmalardan sonra kurtarmayı mümkün kılar, ancak harici veritabanı altyapısı gerektirir ve gecikme ekler.

- **Önden Yazmalı Log (WAL):** Bellek kuyruğu işlemlerinden önce veya sonra bildirimleri diske yazan bir WAL uygulayın. Bu, Kafka'nın log yapısına benzer şekilde, tam veritabanı entegrasyonu gerektirmeden dayanıklılık sağlar.

- **Hibrit Yaklaşım:** Performans için bellek içi kuyruğu koruyun ancak kuyruk durumunu periyodik olarak diske kaydedin. Yeniden başlatıldığında, kuyruklanmış bildirimleri kaydedilen durumdan yeniden yükleyin.

**Ödünleşimler (Trade-offs):** Kalıcılık; karmaşıklık, gecikme ve harici bağımlılıklar ekler, bu da mevcut basitlik ve sıfır altyapı tasarım hedefleriyle çelişir.

#### Yeniden Deneme ve Ölü Mektup (Dead-Letter) Mekanizmaları

- **Yeniden Deneme Mantığı:** Başarısız bildirim işlemleri için yapılandırılabilir yeniden deneme politikaları (üstel geri çekilme, maksimum deneme sayısı) uygulayın. Başarısız bildirimler, yeniden deneme metadatalarıyla tekrar kuyruğa alınabilir.

- **Ölü Mektup Kuyruğu:** Maksimum yeniden deneme girişimini aşan bildirimleri manuel inceleme, analiz veya yeniden işleme için bir ölü mektup kuyruğuna yönlendirin.

- **Hata Sınıflandırması:** Hataları kategorize edin (geçici vs. kalıcı) ve farklı yeniden deneme stratejileri uygulayın. Geçici hatalar (ağ zaman aşımları) yeniden denenebilirken, kalıcı hatalar (geçersiz alıcı) doğrudan ölü mektuba gidebilir.

**Uygulama Hususları:** Yeniden deneme mekanizmaları durum takibi (yeniden deneme sayısı, son hata zamanı) ve potansiyel olarak ayrı kuyruklar veya veri yapıları gerektirir. Ölü mektup kuyrukları depolama ve yönetim arayüzlerine ihtiyaç duyar.

#### Metrikler ve İzleme

- **Kuyruk Derinliği Metrikleri:** Kuyruk boyutunu, kalan kapasiteyi ve kuyruk kullanım yüzdesini dışa açın. Bu, geri basınç ve kapasite sorunlarını belirlemeye yardımcı olur.

- **İş Hacmi Metrikleri:** Bildirim üretim oranını (olay/saniye), tüketim oranını (olay/saniye) ve işleme gecikmesini (kuyruğa eklemeden tamamlanmaya kadar geçen süre) izleyin.

- **Hata Metrikleri:** İşleme hatalarını bildirim türüne, işleyiciye ve hata türüne göre sayın ve kategorize edin. Bu, sorunlu kanalların veya desenlerin belirlenmesini sağlar.

- **İşleyici Performans Metrikleri:** İşleyici başına işlem süresini, işleyici başına başarı/başarısızlık oranlarını ve harici servis gecikmesini (gerçek işleyici uygulamaları için) izleyin.

- **Entegrasyon Noktaları:** Prometheus, Grafana veya diğer izleme sistemleriyle entegrasyon için Micrometer/Actuator aracılığıyla metrikleri dışa açın.

**Değer:** Kapsamlı metrikler kapasite planlamasını, performans ayarını, hata tespitini ve operasyonel görünürlüğü mümkün kılar.

#### Yatay Ölçekleme Fikirleri

- **Paylaşılan Kuyruk Arka Ucu:** Bellek içi kuyruğu, birden fazla örneğin erişebileceği paylaşılan bir arka uçla (Redis, RabbitMQ veya veritabanı) değiştirin. Bu, yatay ölçekleme ve iş yükü dağıtımını mümkün kılar.

- **Dağıtık Kuyruk Soyutlaması:** Hem bellek içi kuyrukları (tek örnek) hem de dağıtık kuyrukları (çoklu örnek) kullanabilen bir kuyruk soyutlama katmanı uygulayın, böylece aynı kod her iki modda da çalışabilir.

- **İş Çalma (Work Stealing):** Örneklerin boştayken diğer örneklerin kuyruklarından iş çalabileceği bir iş çalma algoritması uygulayın, bu da örnekler arasında yük dengelemeyi iyileştirir.

- **Bölümleme Stratejisi:** Bildirimlerin bildirim türüne veya diğer kriterlere göre bölümlenmesine izin verin, farklı örnekler farklı bölümleri yönetir.

**Mimari Etki:** Yatay ölçekleme, dağıtık koordinasyon, konsensüs mekanizmaları ve harici altyapı gerektirerek sistem mimarisini temelden değiştirir, bu da mevcut tasarım felsefesiyle çelişir.

**Öneri:** Yatay ölçekleme gerekiyorsa, belirli gereksinimler karmaşıklığı haklı çıkarmadıkça bu motoru genişletmek yerine yerleşik dağıtık mesajlaşma sistemlerini (Kafka, RabbitMQ, AWS SQS) kullanmayı düşünün.

Bu iyileştirmeler, basitlik ve özellikler arasındaki ödünleşimleri temsil eder. Her ekleme karmaşıklığı, operasyonel yükü veya altyapı bağımlılıklarını artırır. Mevcut tasarım, bu sınırlamaların kabul edilebilir olduğu belirli kullanım durumları için uygun hale getirerek basitliği ve sıfır bağımlılığı kasıtlı olarak önceliklendirir.

## 13. Hedef Dışı (Non-Goals)

Aşağıdakiler açıkça bu projenin kapsamı dışındadır:

- **Dağıtık Mesajlaşma:** Bu motor, çoklu örnek koordinasyonu veya birden fazla uygulama sunucusu arasında dağıtık mesaj kuyruklama için tasarlanmamıştır. Bildirimler uygulama örnekleri arasında paylaşılmaz veya yük dengelemesi yapılmaz.

- **Mesaj Kalıcılığı:** Bildirimler diske veya veritabanına kaydedilmez. Sadece bellekte bulunurlar ve uygulama yeniden başlatılırsa veya çökerse kaybolurlar.

- **Mesaj Dayanıklılığı:** Geleneksel mesaj aracıları aksine, bildirimlerin uygulama yeniden başlatmalarında hayatta kalacağına dair bir garanti yoktur. Motor, dayanıklılık garantileri yerine basitliği ve performansı önceliklendirir.

- **Gelişmiş Kuyruk Özellikleri:** Mesaj öncelikleri, gecikmeli teslim, ölü mektup kuyrukları, mesaj yönlendirme ve konu tabanlı abonelikler gibi özellikler temel tasarımın bir parçası değildir. Motor basit FIFO (İlk Giren İlk Çıkar) işlemeye odaklanır.

- **Harici Sistem Entegrasyonu:** Bildirimler harici servisleri (örn. e-posta sağlayıcıları) çağırabilse de, kuyruk altyapısının kendisi RabbitMQ, Kafka veya bulut mesaj kuyrukları gibi harici mesajlaşma sistemleriyle entegre olmaz.

- **İşlem (Transaction) Yönetimi:** Motor, bildirim üretimi ve tüketimi arasında işlemsel garantiler veya dağıtık işlem yöneticileriyle entegrasyon sağlamaz.

- **Mesaj Onayı (Acknowledgment):** Kurumsal mesaj aracılarının aksine, açık bir onay mekanizması yoktur. Mesajlar tüketim üzerine kuyruktan kaldırılır.

- **Örnekler Arası Yük Dengeleme:** Her uygulama örneği kendi bağımsız kuyruğunu korur. Uygulamanın çalışan birden fazla örneği arasında bildirimlerin koordinasyonu veya yük dengelemesi yoktur.
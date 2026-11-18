======================================================lab3=========================
Ти — досвідчений Android/Kotlin-архітектор. Проаналізуй мій проєкт Android Studio
(пакет com.example.lab1_thing) і розпиши як працюють колонки списки що були додані в останньому коміті, тобто хто кого
викликає, в якому порядку, і що відбувається на кожному кроці стосовно них.
---------
Аналіз роботи списків (LazyColumn + LazyRow) у вашому проєкті
Фокус виключно на списках, які були додані для лабораторної. Покажу повний ланцюжок викликів — від моменту старту до кожного пікселя картки, з акцентом саме на lazy-контейнери.

📦 Що саме додано (для контексту)
Компонент	Файл	Роль
Дані	model/MyListable.kt, Artist.kt, Track.kt, HorizontalList.kt	3 типи моделей у sealed hierarchy
Джерело даних	repository/AppRepositoryImpl.kt	myAppList: List<MyListable> — 13 об'єктів
Стан	HomeViewModel.catalogItems	StateFlow<List<MyListable>> через field
Розмітки елементів	ui/components/ArtistCard, TrackCard, ArtistHorizontalCard	Composable для одного елемента
Вертикальний список	HomeMainContent → LazyColumn	Займає весь вільний простір
Горизонтальний список	HomeMainContent → HorizontalArtistsRow → LazyRow	Вкладений у вертикальний
🔸 ЕТАП 1: Ініціалізація даних (до першого рендера)
Тригер: перший виклик HomeNavDisplay() → viewModel().
🔑 Ключові моменти етапу 1
13 об'єктів створюються один раз — при побудові AppRepositoryImpl.

HorizontalList не створює копії авторів — він містить посилання на той самий myArtistList. Тобто 5 об'єктів Artist спільні між myArtistList і обома HorizontalList.

catalogItems.value — це той самий список, що й myAppList. Не копія.

Жодного LazyColumn ще немає — це чиста ініціалізація даних.

🔸 ЕТАП 2: Підписка UI на стан
Тригер: композиція HomeNavDisplay.

kotlin
@Composable
fun HomeNavDisplay(...) {
    val catalogItems by viewModel.catalogItems.collectAsState()   // ← тут
    ...
}
Що відбувається в collectAsState():
text
[1] Підписка на StateFlow<List<MyListable>>
        ↓
[2] Отримання ПОТОЧНОГО значення:
    value = myAppList (13 об'єктів)
        ↓
[3] Обгортання у State<List<MyListable>>
        ↓
[4] Compose реєструє: "HomeNavDisplay залежить від цього State"
        ↓
[5] catalogItems = той самий List (через by-делегат)
        ↓
[6] Compose запускає рекомпозицію лише якщо State зміниться
    (зараз — НЕ змінюється, бо тестові дані незмінні)
🔑 Важливе для списків
catalogItems — це List<MyListable>, а не StateFlow. UI отримує готовий immutable-список.

Оскільки список не змінюється (немає методів на кшталт addTrack), collectAsState не emitить нових значень → LazyColumn не буде перерендерюватися через зміну даних. Тільки через скрол або інші тригери.

Якби ви додали метод fun addTrack(track: Track) → catalogItems.value = catalogItems.value + track → StateFlow emit → collectAsState оновить → LazyColumn отримає новий список → перерендерить лише нові видимі елементи (завдяки key).

🔸 ЕТАП 3: Передача списку в HomeMainContent
Тригер: NavDisplay обрав entry<HomeScreen.Main> (бо backStack = [Main]).

kotlin
entry<HomeScreen.Main> {
    HomeMainContent(
        labelText = labelText,
        onUpdateText = viewModel::updateText,
        name = name,
        items = catalogItems,                                     // ← список передано
        onNavigateToDetails = { viewModel.navigateTo(HomeScreen.Details) }
    )
}
Що відбувається:
text
[1] NavDisplay бачить: верхній елемент backStack = HomeScreen.Main
        ↓
[2] Знаходить entry<HomeScreen.Main> у entryProvider
        ↓
[3] Викликає його Composable-лямбду
        ↓
[4] Лямбда викликає HomeMainContent(
        items = catalogItems  ← посилання на той самий List із 13 об'єктів
    )
        ↓
[5] HomeMainContent отримує `items: List<MyListable>`
        ↓
[6] Починається перша композиція Column + LazyColumn
🔑 Ключове
items — той самий List, що й myAppList у репозиторії. Не копія.

Жодних mutableStateOf навколо списку — просто параметр.

HomeMainContent — stateless щодо списку: не знає про ViewModel, просто рендерить те, що дали.

🔸 ЕТАП 4: Перший рендер Column — розподіл висоти
Тригер: HomeMainContent починає композицію.

kotlin
Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text("Hello, $name")                // 1
    Spacer(8.dp)                         // 2
    Text(labelText)                      // 3
    Spacer(16.dp)                        // 4
    Button("Змінити текст")              // 5
    LazyColumn(...weight(1f)) { ... }    // 6  ← НАШ СПИСОК
    Button("Перейти далі")               // 7
}
Що робить Column:
text
[1] Column вимірює доступну висоту = повна висота екрана
    (після Scaffold.innerPadding і NavigationSuiteScaffold)
        ↓
[2] ВИМІРЮЄ природну висоту кожного дочірнього елемента:
    
    Text("Hello")         → ~20dp
    Spacer(8dp)           → 8dp (фіксована)
    Text(labelText)       → ~36dp (headlineMedium)
    Spacer(16dp)          → 16dp
    Button("Змінити")     → 64dp (фіксовано)
    LazyColumn            → ??? (weight=1f → відкладено)
    Button("Перейти")     → ~40dp + padding
        ↓
[3] Підраховує суму ФІКСОВАНИХ висот (без LazyColumn):
    ~20 + 8 + 36 + 16 + 64 + ~40 ≈ 184dp
        ↓
[4] Обчислює ВІЛЬНИЙ ПРОСТІР для weight(1f):
    залишок = повна_висота - 184dp ≈ (наприклад 800 - 184) ≈ 616dp
        ↓
[5] Встановлює для LazyColumn ВИСОТУ = 616dp
        ↓
[6] Тепер має точні розміри для кожного елемента → малює
🔑 Чому саме weight(1f) виконує «займає весь вільний простір»
weight(1f) = «забери всю частку вільного простору, що залишилась».

Якби було weight(2f) у двох елементів — кожен отримав би по половині.

Тут лише один weight(1f) → він забирає весь залишок.

Без weight: LazyColumn отримав би нескінченну висоту за замовчуванням → помилка «Constraint not satisfiable» або він би вийшов за екран.

Схема розподілу висоти:
text
┌────────────────────────────┐  ↑
│ Hello, Android             │  │ ~20dp  (фіксована)
│ (8dp spacer)               │  │ 8dp
│ Початковий текст...        │  │ ~36dp
│ (16dp spacer)              │  │ 16dp
│ [Змінити текст]            │  │ 64dp
├────────────────────────────┤  │
│                            │  │
│  LazyColumn (weight=1f)    │  │ ← 616dp (весь залишок)
│  = 616dp висоти            │  │
│                            │  │
├────────────────────────────┤  │
│ [Перейти далі]             │  │ ~40dp
└────────────────────────────┘  ↓
                          Повна висота = 800dp
🔸 ЕТАП 5: LazyColumn — перший рендер
Тригер: Column дав LazyColumn висоту 616dp.

kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize().weight(1f),
    contentPadding = PaddingValues(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
) {
    items(
        items = items,                  // ← 13 об'єктів
        key = { item -> "..." }          // ← унікальний ключ
    ) { item ->
        when (item) {
            is Artist -> ArtistCard(...)
            is Track -> TrackCard(...)
            is HorizontalList -> HorizontalArtistsRow(...)
        }
    }
}
Покроково всередині LazyColumn:
text
[1] LazyColumn отримує розміри:
    ширина = повна ширина Column (~360dp на телефоні)
    висота = 616dp (від weight)
        ↓
[2] Композиція дітей: LazyColumn бере список `items` (13)
        ↓
[3] ОБЧИСЛЮЄ, скільки елементів влізе у 616dp:
    
    Приблизно:
    - TrackCard: ~90dp (з padding)
    - ArtistCard: ~80dp
    - LazyRow (HorizontalList): ~180dp
    
    Влізе десь ~5-6 карток
        ↓
[4] РЕНДЕРИТЬ ТІЛЬКИ ВИДИМІ:
    items[0]  Track         → TrackCard     ← композиція
    items[1]  Track         → TrackCard     ← композиція
    items[2]  Track         → TrackCard     ← композиція
    items[3]  Artist        → ArtistCard    ← композиція
    items[4]  Track         → TrackCard     ← композиція (частково)
    items[5]  HorizontalList → LazyRow      ← композиція (частково видно)
    items[6..12]                            ← НЕ компонуються (за межами)
        ↓
[5] Запам'ятовує «viewport» — які індекси зараз видимі
        ↓
[6] Підписується на скрол-жести
        ↓
[7] Готово — LazyColumn намальований
🔑 Що таке «lazy» на практиці
Тип	Скільки Composable у пам'яті
Column { 13 елементів }	13 (усі одразу)
LazyColumn { 13 елементів }	~5-6 (лише видимі)
LazyColumn { 10000 елементів }	~10 (все ще видимі)
Тому LazyColumn ефективний: пам'ять не залежить від розміру списку.

🔸 ЕТАП 6: Обробка key при рендері
Тригер: items(items = items, key = { ... }).

Що відбувається для кожного елемента:
text
items[0] = Track(id=0, title="Midnight Drive", ...)
    ↓
key-функція: "${item.id}_${when(item){ is Track -> item.title }}"
    ↓
key = "0_Midnight Drive"
    ↓
LazyColumn запам'ятовує: "елемент з ключем '0_Midnight Drive' = Composable TrackCard(id=0)"

items[1] = Track(id=1, title="Silver Wings", ...)
    → key = "1_Silver Wings"

items[2] = Track(id=2, title="Ocean Eyes", ...)
    → key = "2_Ocean Eyes"

items[3] = Artist(id=4, name="Paper Clouds")
    → key = "4_Paper Clouds"

items[4] = Track(id=3, title="Fading Lights", ...)
    → key = "3_Fading Lights"

items[5] = HorizontalList(id=100, artistList=[...])
    → key = "100_h_100"
🔑 Навіщо ці ключі
Без key: LazyColumn ідентифікує елементи за позицією в списку (індекс 0, 1, 2...).

Сценарій проблеми без key: якби ви вставили новий елемент у середину — індекси б зсунулись, і LazyColumn подумав би, що елемент на позиції 5 — це вже інший об'єкт → композиція би перезапустилась із нуля для всіх наступних (втрата стану, анімацій, фокусу).

З key: LazyColumn бачить: "ключ '0_Midnight Drive' — той самий елемент, перевикористати". Новий елемент отримує свій ключ → композується лише він.

Коли key реально працює у вашому проєкті
Зараз список незмінний — тому key не проявляє себе. Але якщо ви додасте, наприклад:

kotlin
fun addTrack(track: Track) {
    catalogItems.value = catalogItems.value + track
}
Тоді при додаванні 14-го елемента:

Без key: LazyColumn подумав би, що весь список змінився (бо items — новий List) → перерендерив би все.

З key: LazyColumn побачив би, що перші 13 ключів ті самі → перевикористав би існуючі Composable → додав би лише 14-й.

🔸 ЕТАП 7: Композиція першого елемента — TrackCard
Тригер: LazyColumn викликав лямбду для items[0].

kotlin
items[0] = Track("Midnight Drive", 240, artist, id = 0)
        ↓
when(item) {
    is Track -> TrackCard(track = item)   ← виконано
}
        ↓
TrackCard(track = Track("Midnight Drive", 240, ...))
        ↓
Card(
    colors = CardDefaults.cardColors(containerColor = surfaceVariant),
    elevation = 2.dp
) {
    Column(padding 16dp) {
        Text("Midnight Drive")                    ← рендериться
        Spacer(8dp)
        Row(SpaceBetween) {
            Text(track.artist?.name ?: "...")      ← "Aurora Field"
            Text(formatDuration(240))              ← "4:00"
        }
    }
}
        ↓
Card намальована як Material 3 card з тінню 2dp
Що робить formatDuration(240):
kotlin
private fun formatDuration(seconds: Int): String {
    val min = 240 / 60 = 4
    val sec = 240 % 60 = 0
    return "%d:%02d".format(4, 0) = "4:00"
}
Ключове: виклик не в Composable, а в звичайній функції. Не створює State. Просто обчислення.

🔸 ЕТАП 8: Композиція ArtistCard (items[3])
Тригер: LazyColumn викликав лямбду для items[3].

kotlin
items[3] = Artist("Paper Clouds", "Norway", id = 4)
        ↓
when(item) {
    is Artist -> ArtistCard(artist = item)
}
        ↓
ArtistCard(artist = Artist("Paper Clouds", "Norway", 4))
        ↓
Card(colors = secondaryContainer) {
    Row {
        Box(size 48, clip=Circle) {
            Text("P")                     ← artist.name.take(1).uppercase()
        }
        Spacer(16dp)
        Column {
            Text("Paper Clouds")          ← artist.name
            Text("Країна: Norway")        ← "Країна: ${artist.country}"
        }
    }
}
Різниця з TrackCard: тут використовується artist.name.take(1) — для першої літери в аватарі. Це не стан — просто обчислення при рендері.

🔸 ЕТАП 9: Композиція HorizontalArtistsRow (items[5]) — НАЙВАЖЛИВІШЕ
Тригер: LazyColumn викликав лямбду для items[5] (HorizontalList).

kotlin
items[5] = HorizontalList(myArtistList, id = 100)
        ↓
when(item) {
    is HorizontalList -> HorizontalArtistsRow(horizontalList = item)
}
        ↓
HorizontalArtistsRow(horizontalList = HorizontalList(artistList=[5 Artist], id=100))
Що робить HorizontalArtistsRow:
kotlin
@Composable
private fun HorizontalArtistsRow(horizontalList: HorizontalList, ...) {
    LazyRow(                                          // ← вкладений контейнер
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = horizontalList.artistList,        // ← 5 Artist
            key = { artist -> "h_${artist.id}_${artist.name}" }
        ) { artist ->
            ArtistHorizontalCard(artist = artist)
        }
    }
}
Покроково всередині LazyRow:
text
[1] LazyRow отримує розміри від батька (Column, у якому лежить LazyColumn):
    ширина = ~360dp (повна ширина LazyColumn)
    висота = ??? (природна — LazyRow обчислює самостійно)
        ↓
[2] LazyRow дивиться на свій список: horizontalList.artistList = 5 Artist'ів
        ↓
[3] ОБЧИСЛЮЄ, скільки ArtistHorizontalCard влізе по ШИРИНІ:
    
    Кожен ArtistHorizontalCard: width=140dp + spacedBy(8dp) між ними
    360dp / (140+8) ≈ 2.4 → влізе 2-3 картки
        ↓
[4] РЕНДЕРИТЬ ЛИШЕ ВИДИМІ:
    artistList[0] "Aurora Field" → ArtistHorizontalCard   ← композиція
    artistList[1] "Neon Garden"  → ArtistHorizontalCard   ← композиція
    artistList[2] "Velvet Echo"  → ArtistHorizontalCard   ← композиція (частково)
    artistList[3] "Paper Clouds"                          ← НЕ композиція
    artistList[4] "Crystal Lake"                          ← НЕ композиція
        ↓
[5] LazyRow вимірює СВОЮ висоту:
    = висота однієї найвищої картки (бо horizontal)
    ≈ 140dp (фіксована від artist + spacing)
        ↓
[6] Повідомляє батькові (LazyColumn): «я займаю 140dp + contentPadding»
        ↓
[7] Підписується на ГОРИЗОНТАЛЬНІ жести
        ↓
[8] Готово
🔑 Ключові особливості вкладеного LazyRow
1. Двонаправлений скрол
text
LazyColumn (вертикальний) містить LazyRow (горизонтальний)
        ↓
Вертикальний свайп по екрану → LazyColumn scroll
        ↓
Горизонтальний свайп ПО РЯДКУ → LazyRow scroll
        ↓
Compose автоматично розділяє жести за напрямком!
Як це працює: Compose LazyColumn і LazyRow використовують nested scroll — кожен обробляє свій напрямок. Коли ви свайпаєте вертикально по рядку авторів, жест «проходить» до LazyColumn (бо LazyRow не може прокручуватись вертикально). Коли свайпаєте горизонтально — LazyRow «перехоплює» жест.

2. Фіксована ширина картки — критично
kotlin
ArtistHorizontalCard: modifier.width(140.dp)   ← обов'язково!
Що було б без width(140.dp): ArtistHorizontalCard у LazyRow мав би wrapContent → ширина залежала б від тексту (наприклад, "Aurora Field" ширше, ніж "Velvet Echo"). Це виглядало б «нерівно». Фіксована ширина = однаковий вигляд + коректний розрахунок скролу.

3. Висота LazyRow — природна, не weight
LazyRow не має weight — він займає природну висоту (висота найвищої картки). Батько (LazyColumn) це врахує при розрахунку, скільки ще елементів влізе на екран нижче.

🔸 ЕТАП 10: Завершення першого рендера
Після всіх етапів:

text
Column отримав:
├── Text("Hello")              → висота ~20dp
├── Spacer(8dp)                → 8dp
├── Text(labelText)            → ~36dp
├── Spacer(16dp)               → 16dp
├── Button("Змінити")          → 64dp
├── LazyColumn (weight=1f)     → 616dp
│    ├── TrackCard  (0)        ← видимий
│    ├── TrackCard  (1)        ← видимий
│    ├── TrackCard  (2)        ← видимий
│    ├── ArtistCard (3)        ← видимий
│    ├── TrackCard  (4)        ← видимий
│    ├── HorizontalArtistsRow  ← видимий (LazyRow з 3 картками)
│    └── ... (7 елементів НЕ композуються)
└── Button("Перейти")          → ~40dp
Перший кадр намальовано.

🔸 ЕТАП 11: Скрол вертикальний — що відбувається з елементами
Тригер: користувач свайпає вгору.

text
[1] LazyColumn отримує vertical scroll gesture
        ↓
[2] Обчислює новий viewport:
    Раніше: items[0..5] видимі
    Тепер: items[2..8] видимі (зсунулось на 2 елементи)
        ↓
[3] Елементи, що ВИЙШЛИ за екран (items[0], items[1]):
    → Composable викликається onDispose() → знищується
    → ОБ'ЄКТИ Track/Artist (дані) ЗАЛИШАЮТЬСЯ в myAppList у пам'яті
    → Пам'ять звільняється: ~2 Composable менше
        ↓
[4] Елементи, що УВІЙШЛИ на екран (items[6], items[7]):
    → LazyColumn бере дані зі списку (вже існують)
    → Створює НОВІ Composable:
        - items[6] = Track("Distant Shores") → TrackCard
        - items[7] = Track("Paper Planes") → TrackCard
    → key = "6_Distant Shores", "7_Paper Planes" → запам'ятовує
        ↓
[5] Елементи, що ВЖЕ БУЛИ видимі (items[2..5]):
    → Перевикористовуються (key збігається)
    → НЕ перекомпозуються
    → Просто переміщуються на нову позицію
        ↓
[6] Намальовано новий кадр
🔑 Що НЕ відбувається при скролі
Подія	Чи відбувається?
HomeViewModel перестворюється	❌ ні
catalogItems переприсвоюється	❌ ні
collectAsState emitить	❌ ні
NavDisplay перекомпозиція	❌ ні
HomeNavDisplay перекомпозиція	❌ ні
HomeMainContent перекомпозиція	❌ ні
LazyColumn перекомпозиція	❌ ні
Внутрішній стан LazyColumn (viewport)	✅ так
Ключове: скрол — це внутрішня операція LazyColumn. Вона не тригерує recomposition батьків. Лише сам LazyColumn вирішує, які елементи показати, і викликає їхні Composable-лямбди.

🔸 ЕТАП 12: Скрол горизонтальний — LazyRow
Тригер: користувач свайпає горизонтально по ряду авторів.

text
[1] LazyRow (всередині LazyColumn) отримує horizontal gesture
        ↓
[2] LazyColumn отримує жест ПЕРШИМ (бо він — батько),
    але розпізнає: "це горизонтальний жест → не мій напрямок"
        ↓
[3] Жест ПЕРЕДАЄТЬСЯ до LazyRow (nested scroll)
        ↓
[4] LazyRow обчислює новий viewport:
    Раніше: artistList[0..2] видимі
    Тепер: artistList[1..3] видимі
        ↓
[5] artistList[0] (Aurora Field) → onDispose() → знищено
        ↓
[6] artistList[3] (Paper Clouds) → нова ArtistHorizontalCard
        ↓
[7] artistList[1..2] — перевикористано (key)
        ↓
[8] Намальовано новий кадр (тільки для цього LazyRow)
        ↓
[9] LazyColumn НЕ перекомпозується (тільки один рядок змінився)
🔑 Що НЕ відбувається при горизонтальному скролі
❌ LazyColumn не скролиться (він «не бачить» горизонтального жесту).

❌ Інші елементи LazyColumn не перекомпозуються.

❌ HomeMainContent не перекомпозується.

✅ Тільки LazyRow змінює свій viewport.

🔸 ЕТАП 13: Recomposition — коли LazyColumn перемальовується
LazyColumn перемальовується лише в таких випадках:

Тригер	Що станеться
Скрол (внутрішній стан)	Оновлює viewport, композує нові видимі елементи
Зміна items (наприклад, catalogItems.value = новий список)	Перебудовує viewport, за key розуміє, які елементи перевикористати
Зміна розміру (поворот екрана)	Перерахує, скільки влізе, перекомпозує
Зміна стану елемента (наприклад, remember всередині TrackCard змінився)	Перекомпозує лише цей елемент
Чого LazyColumn НЕ реагує
❌ Зміна labelText (він у Text поза LazyColumn) — LazyColumn не перемальовується.

❌ Клік по кнопці «Змінити текст» — лише Text зверху перемальовується.

❌ Перемикання вкладок — LazyColumn знищується разом з HomeNavDisplay, потім створюється заново при поверненні.

🔸 ЕТАП 14: Поворот екрана — що з даними
Тригер: користувач повертає екран.

text
[1] Activity.onDestroy() (через configuration change)
        ↓
[2] Compose-дерево знищується → LazyColumn, LazyRow, усі картки — onDispose()
        ↓
[3] Activity.onCreate() → setContent → Lab1thingApp → HomeNavDisplay
        ↓
[4] viewModel() → ViewModelStore Activity ЗБЕРЕЖЕНО
        ↓
[5] Повертає ТОЙ САМИЙ HomeViewModel
    → catalogItems.value = той самий List (13 об'єктів у пам'яті)
        ↓
[6] collectAsState() → той самий список
        ↓
[7] LazyColumn створюється заново → рендерить із того самого списку
        ↓
[8] Візуально: список відновлено
🔑 Чому НЕ скидається
catalogItems — у ViewModel, який переживає configuration change.

Об'єкти Track, Artist — у пам'яті, бо myAppList — поле AppRepositoryImpl, що живе в HomeViewModel.

Скрол-позиція LazyColumn скидається (бо Composable перестворюється). Якщо треба зберегти — використовуйте rememberLazyListState() (з rememberSaveable).

Що скидається після повороту
Позиція вертикального скролу в LazyColumn → повертається на початок.

Позиція горизонтального скролу в кожному LazyRow → на початок.

Дані — залишаються.

📊 Повна мапа викликів (стосовно списків)
text
viewModel<HomeViewModel>()
  └─ HomeViewModel() (конструктор)
       ├─ AppRepositoryImpl() (конструктор)
       │    ├─ myArtistList = List<Artist>(5)         ← 5 об'єктів
       │    └─ myAppList = List<MyListable>(13)       ← 13 об'єктів
       └─ catalogItems = MutableStateFlow(myAppList)   ← той самий список
            ↓
HomeNavDisplay()
  ├─ collectAsState(catalogItems) → items: List<MyListable>
  └─ NavDisplay(backStack=[Main])
       └─ entry<Main> → HomeMainContent(items = items)
            └─ Column
                 ├─ Text, Spacer, Text, Spacer, Button    ← фіксована висота
                 ├─ LazyColumn(weight=1f)                 ← займає залишок
                 │    ├─ items(items, key)
                 │    │    ↓ для кожного ВИДИМОГО елемента:
                 │    │    ├─ is Track       → TrackCard(track)
                 │    │    ├─ is Artist      → ArtistCard(artist)
                 │    │    └─ is HorizontalList
                 │    │         └─ HorizontalArtistsRow
                 │    │              └─ LazyRow(fillMaxWidth)
                 │    │                   └─ items(artistList, key)
                 │    │                        ↓ для кожного ВИДИМОГО:
                 │    │                        └─ ArtistHorizontalCard(artist)
                 │    └─ onScroll → оновлення viewport → нові композиції
                 └─ Button("Перейти")                     ← фіксована висота









====================================================================================
Ти — досвідчений Android/Kotlin-архітектор. Проаналізуй мій проєкт Android Studio
(пакет com.example.lab1_thing) і склади ПОВНУ КАРТУ ВИКЛИКІВ — тобто хто кого
викликає, в якому порядку, і що відбувається на кожному кроці.
1. СТАРТОВИЙ ЛАНЦЮЖОК (cold start)
   Розпиши послідовність від моменту, коли користувач тапає іконку, до
   першого кадру на екрані:
   - Який клас/метод викликається першим?
   - Які методи життєвого циклу Activity проходять?
   - Коли саме створюється Compose-дерево?
   - Де і коли створюються ViewModel?
   - Коли відбувається перша композиція Lab1thingApp()?
   - Який маршрут показується першим у NavHost?

2. КАРТА COMPOSABLE-ВИКЛИКІВ (recomposition tree)
   Побудуй дерево викликів Composable-функцій від кореня до листків:
   MainActivity.setContent → Lab1thingTheme → Lab1thingApp → ...
   Покажи для КОЖНОГО вузла:
   - Що він рендерить (Text / Button / Column / NavHost / ...)
   - Від якого стану залежить (State / StateFlow)
   - Які колбеки передає дочірнім компонентам

3. СТАН ТА ЙОГО ВЛАСНИКИ
   Склади таблицю:
   | Стан | Тип | Де живе (remember / ViewModel / StateFlow) | Хто читає | Хто змінює |
   |------|-----|-------------------------------------------|-----------|------------|
   Поясни різницю між currentDestination (rememberSaveable) і labelText (ViewModel+StateFlow).

4. СЦЕНАРІЇ ВЗАЄМОДІЇ (user flows) — розпиши КОЖЕН покроково:

   Сценарій A: Клік по вкладці "Налаштування" в NavigationSuiteScaffold
   Сценарій B: Клік "Змінити текст" на Головній
   Сценарій C: Клік "Перейти далі" (forward navigation)
   Сценарій D: Клік "Повернутися назад" (back button)
    Сценарій E: Поворот екрана (configuration change)
   Сценарій F: Перемикання Головна → Налаштування → Головна (перевірка збереження стану)

   Для КОЖНОГО сценарію:
   - Тригер (що натиснув користувач)
   - Ланцюжок викликів (стрілками: A → B → C)
   - Які функції викликаються (з іменами!)
   - Який стан змінюється
   - Що перемальовується (які Composable перезапускаються, а які — ні)
   - Який кінцевий результат на екрані

5. НАВІГАЦІЯ: ДВА РІВНІ
   Поясни й покажи на коді різницю між:
   - Глобальна навігація (NavigationSuiteScaffold + when(currentDestination))
   - Вкладена навігація (NavHost усередині HomeRoute)
   Покажи, який NavController за що відповідає, і чому системна "Назад"
   поводиться по-різному на різних рівнях.

6. VIEWMODEL: ЖИТТЄВИЙ ЦИКЛ
   Поясни:
   - Коли створюється HomeViewModel (при першому viewModel() у HomeRoute)
   - Коли він знищується (при onDestroy() MainActivity)
   - Що відбувається з ним при перемиканні вкладок
   - Що відбувається при повороті екрана
   - Чому labelText не скидається (а currentDestination теж — але з іншої причини)
   - Розкажи про field (explicit backing field) і чим він кращий за _private-патерн
--------------------------------------
Ось детальний та вичерпний архітектурний аналіз проєкту com.example.lab1_thing із повною картою викликів, потоками даних та життєвими циклами компонентів.
1. СТАРТОВИЙ ЛАНЦЮЖОК (Cold Start)
Від натискання на іконку програми до появи першого кадру на екрані пристрою:
1.
Ініціалізація процесу ОС Android:
◦
Linux-ядро створює новий процес для com.example.lab1_thing через Zygote.
◦
Завантажуються класи в ART (Android Runtime). Створюється екземпляр класу Application.
2.
Запуск Activity:
◦
ActivityManagerService (AMS) передає інтент запуску до MainActivity.
◦
Створюється екземпляр класу MainActivity, викликається його конструктор за замовчуванням.
3.
Методи життєвого циклу Activity:
◦
onCreate(savedInstanceState):
▪
super.onCreate(savedInstanceState): відновлюються базові структури стану Activity.
▪
enableEdgeToEdge(): налаштовуються системні вікна (status bar, navigation bar) для повноекранного відображення без системних відступів.
▪
setContent { ... }: ініціалізує ComposeView як кореневе View у вікні Activity (decorView). Саме тут Compose прив'язується до життєвого циклу ViewTreeLifecycleOwner та ViewTreeViewModelStoreOwner.
4.
Створення Compose-дерева (Initial Composition):
◦
Усередині setContent створюється Recomposer, який слухає зміни стану.
◦
Створюється початкова композиція:  MainActivity.setContent →  Lab1thingTheme →  Lab1thingApp().
5.
Де і коли створюються ViewModel:
◦
 Lab1thingApp обирає гілку when (currentDestination) → викликає  HomeNavDisplay.
◦
У  HomeNavDisplay у параметрі за замовчуванням виконується viewModel: HomeViewModel = viewModel().
◦
viewModel() звертається до найближчого ViewModelStoreOwner (у цьому випадку це сама MainActivity). Оскільки екземпляра HomeViewModel ще немає в сховищі ViewModelStore, створюється новий екземпляр HomeViewModel.
◦
SettingsViewModel на етапі Cold Start НЕ створюється, оскільки  SettingsRoute ще не потрапляв у композицію.
6.
Подальші етапи життєвого циклу:
◦
Викликається onStart() (Activity стає видимою).
◦
Відбувається вимірювання (Measure) та розташування (Layout) нод Compose-дерева, малювання (Draw) у буфер екрана.
◦
Викликається onResume() (Activity переходить у фокус для взаємодії). Користувач бачить перший кадр.
7.
Початковий екран у внутрішній навігації:
◦
 HomeViewModel.homeBackStack ініціалізовано списком listOf(HomeScreen.Main).
◦
NavDisplay читає верхній елемент стека HomeScreen.Main, звертається до entryProvider і відображає  HomeMainContent.
2. КАРТА COMPOSABLE-ВИКЛИКІВ (Recomposition Tree)
Ієрархія компонентів від кореня до листків на момент початкового запуску:
MainActivity.setContent
└── Lab1thingTheme
    └── Lab1thingApp
        ├── rememberSaveable { mutableStateOf(AppDestinations.HOME) } -> currentDestination
        └── NavigationSuiteScaffold
            ├── navigationSuiteItems { AppDestinations.entries.forEach { ... } }
            │   ├── Item "Головна" (залежить від: currentDestination == HOME, рендерить: Icon + Text)
            │   └── Item "Налаштування" (залежить від: currentDestination == SETTINGS, рендерить: Icon + Text)
            └── Scaffold
                └── when (currentDestination)
                    ├── AppDestinations.HOME -> HomeNavDisplay
                    │   ├── viewModel: HomeViewModel (створюється/отримується з ViewModelStore)
                    │   ├── labelText by viewModel.labelText.collectAsState()
                    │   ├── backStack by viewModel.homeBackStack.collectAsState()
                    │   └── NavDisplay (залежить від: backStack)
                    │       └── entryProvider
                    │           ├── entry<HomeScreen.Main> -> HomeMainContent
                    │           │   ├── рендерить: Column, Text("Hello..."), Text(labelText), Button("Змінити"), Button("Перейти далі")
                    │           │   ├── залежить від: labelText, name
                    │           │   └── передає колбеки:
                    │           │       ├── onClick = { onUpdateText(...) } -> viewModel::updateText
                    │           │       └── onClick = onNavigateToDetails -> viewModel.navigateTo(HomeScreen.Details)
                    │           └── entry<HomeScreen.Details> (якщо активний) -> HomeDetailsContent
                    │               ├── рендерить: Scaffold, TopAppBar (Text, IconButton з Icon стрілки), Column, Text(...)
                    │               ├── залежить від: ніяких зовнішніх змінних стану
                    │               └── передає колбек:
                    │                   └── onClick = onBack -> viewModel.navigateBack()
                    │
                    └── AppDestinations.SETTINGS -> SettingsRoute (якщо вибрано)
                        ├── viewModel: SettingsViewModel
                        ├── labelText by viewModel.labelText.collectAsState()
                        └── SettingsScreen
                            ├── рендерить: Column, Text("Hello..."), Text(labelText), Button("Змінити")
                            ├── залежить від: labelText, name
                            └── передає колбек: onClick = { onUpdateText(...) } -> viewModel::updateText
3. СТАН ТА ЙОГО ВЛАСНИКИ
Стан
Тип даних
Де живе (Owner/Scope)
Хто читає
Хто змінює
currentDestination
MutableState<AppDestinations>
Lab1thingApp через rememberSaveable
 Lab1thingApp (для when і selected у вкладках)
NavigationSuiteScaffold (колбек onClick елемента панелі)
labelText (Home)
StateFlow<String>
HomeViewModel (екземпляр прив'язаний до Activity)
 HomeNavDisplay $\rightarrow$ передає значення в  HomeMainContent
HomeViewModel.updateText() (через кнопку «Змінити текст»)
homeBackStack
StateFlow<List<HomeScreen>>
HomeViewModel
 HomeNavDisplay (передає стек у NavDisplay)
HomeViewModel.navigateTo(), HomeViewModel.navigateBack()
labelText (Settings)
StateFlow<String>
SettingsViewModel
 SettingsRoute $\rightarrow$ передає значення в  SettingsScreen
SettingsViewModel.updateText() (через кнопку «Змінити»)
Різниця між currentDestination (rememberSaveable) та labelText (ViewModel + StateFlow):
1.
Рівень зберігання:
◦
currentDestination живе безпосередньо в дереві композиції  Lab1thingApp. rememberSaveable пакує його значення в системний Bundle (SavedStateRegistry), що дозволяє зберегти позицію навіть при смерті процесу ОС.
◦
labelText живе в об'єкті ViewModel, який розміщений у пам'яті (ViewModelStore на рівні Activity). Він повністю ізольований від Compose UI.
2.
Пережиття життєвого циклу:
◦
Обидва переживають поворот екрана (Configuration Change). Але rememberSaveable переживає навіть смерть процесу через нестачу пам'яті (Process Death), тоді як звичайна ViewModel без SavedStateHandle при цьому скинулася б до початкового стану.
3.
Область видимості:
◦
currentDestination — суто UI-стан глобальної навігації.
◦
labelText — стан бізнес-логіки/даних, який можна тестувати юніт-тестами без запуску Compose.
4. СЦЕНАРІЇ ВЗАЄМОДІЇ (User Flows)
Сценарій A: Клік по вкладці "Налаштування"
•
Тригер: Натискання на іконку/текст вкладки "Налаштування" в навігаційному барі.
•
Ланцюжок викликів: NavigationSuiteScaffold.onClick → currentDestination = AppDestinations.SETTINGS.
•
Зміна стану: Змінна currentDestination змінює значення з HOME на SETTINGS.
•
Що перемальовується:
◦
NavigationSuiteScaffold підсвічує активну іконку налаштувань.
◦
У  Lab1thingApp блок when(currentDestination) рекомпонується:  HomeNavDisplay видаляється з композиції (Leave Composition).
◦
Уперше викликається  SettingsRoute, ініціалізується SettingsViewModel, рендериться  SettingsScreen.
•
Результат: На екрані показується екран "Налаштування".
Сценарій B: Клік "Змінити текст" на Головній
•
Тригер: Натискання на кнопку "Змінити текст" на HomeScreen.Main.
•
Ланцюжок викликів: Button.onClick → onUpdateText("Текст змінено на Головній!") → HomeViewModel.updateText() → labelText.value = newText.
•
Зміна стану: labelText у HomeViewModel змінює внутрішнє значення.
•
Що перемальовується:
◦
collectAsState() у  HomeNavDisplay фіксує зміну потоку.
◦
Рекомпонуються лише  HomeNavDisplay та нода  HomeMainContent (зокрема Text(text = labelText)).
◦
Весь глобальний каркас  Lab1thingApp і NavigationSuiteScaffold НЕ перемальовуються.
•
Результат: Текст на екрані миттєво оновлюється на "Текст змінено на Головній!".
Сценарій C: Клік "Перейти далі" (Forward Navigation)
•
Тригер: Натискання на кнопку "Перейти далі".
•
Ланцюжок викликів: Button.onClick → onNavigateToDetails() → HomeViewModel.navigateTo(HomeScreen.Details) → homeBackStack.value = homeBackStack.value + HomeScreen.Details.
•
Зміна стану: Стек у HomeViewModel стає [HomeScreen.Main, HomeScreen.Details].
•
Що перемальовується:
◦
backStack by viewModel.homeBackStack.collectAsState() отримує новий список.
◦
NavDisplay перемикає видиму сцену на нову вершину стека (HomeScreen.Details).
◦
 HomeMainContent ховається / замінюється, викликається  HomeDetailsContent.
•
Результат: З'являється екран деталей із заголовком "Деталі" у TopAppBar та стрілкою назад.
Сценарій D: Клік "Повернутися назад" (TopAppBar back arrow)
•
Тригер: Натискання на IconButton зі стрілкою назад у TopAppBar екрана HomeDetailsContent.
•
Ланцюжок викликів: IconButton.onClick → onBack() → NavDisplay.onBack → HomeViewModel.navigateBack() → homeBackStack.value.dropLast(1).
•
Зміна стану: Стек повертається до стану [HomeScreen.Main].
•
Що перемальовується:
◦
NavDisplay отримує оновлений стек, прибирає  HomeDetailsContent і знову монтує у вивід  HomeMainContent.
•
Результат: Користувач знову на початковому екрані з текстом та двома кнопками.
Сценарій E: Поворот екрана (Configuration Change)
•
Тригер: Зміна орієнтації пристрою (Portrait ↔ Landscape).
•
Ланцюжок викликів: ОС знищує поточну Activity: onPause() → onStop() → onDestroy(). ОС створює нову Activity: onCreate() → setContent → початкова композиція.
•
Стан:
◦
currentDestination: відновлюється з savedInstanceState завдяки rememberSaveable.
◦
HomeViewModel НЕ знищується — ViewModelStore утримується системою під час зміни конфігурації.
◦
labelText та homeBackStack зберігають свої значення в пам'яті ViewModel.
•
Що перемальовується: Повністю перестворюється все дерево композиції під нові розміри вікна (якщо планшет чи landscape — NavigationSuiteScaffold може адаптивно перемістити бар у навігаційний рейл ліворуч).
•
Результат: Користувач залишається на тому ж екрані та підекрані з тим самим текстом без втрати даних.
Сценарій F: Перемикання Головна → Налаштування → Головна
•
Тригер: Клік на "Налаштування", потім знову клік на "Головна".
•
Ланцюжок викликів:
i.
currentDestination = SETTINGS →  HomeNavDisplay покидає композицію.
ii.
currentDestination = HOME →  HomeNavDisplay заново входить у композицію.
•
Зміна стану: Змінюється лише currentDestination. Стан усередині HomeViewModel залишається цілим.
•
Що відбувається зі станом:
◦
Оскільки HomeViewModel заскоуплена на MainActivity, при виході  HomeNavDisplay з композиції ViewModel НЕ вбивається.
◦
Коли ми повертаємося на вкладку "Головна", viewModel() повертає той самий збережений екземпляр HomeViewModel.
•
Результат: Змінений раніше текст і навіть відкритий підекран Details (якщо ми були на ньому) залишаються незмінними.
5. НАВІГАЦІЯ: ДВА РІВНІ
У застосунку реалізовано дворівневу архітектуру навігації:
[ РІВЕНЬ 1: Глобальний (Top-Level) ]
    NavigationSuiteScaffold + rememberSaveable (currentDestination)
    └── Без стека історії: перемикає паралельні вкладки "Головна" та "Налаштування"

         └── [ РІВЕНЬ 2: Вкладений (Nested Sub-flow) ]
                 Всередині HomeNavDisplay:
                 Navigation 3 (NavDisplay + homeBackStack у ViewModel)
                 └── Має стек історії: [HomeScreen.Main] -> [HomeScreen.Main, HomeScreen.Details]
1.
Глобальний рівень (Top-level destination switching):
◦
Побудований на NavigationSuiteScaffold та виразі when (currentDestination).
◦
Тут взагалі немає NavController чи бекстека. Це механізм перемикання незалежних робочих просторів екрана.
2.
Вкладений рівень (Nested Navigation 3 flow):
◦
Розташований виключно всередині вкладки "Головна" у функції  HomeNavDisplay.
◦
Керується сучасною бібліотекою Navigation 3 через декларативний NavDisplay та entryProvider.
◦
Бекстек є чистим списком List<HomeScreen>, винесеним у StateFlow у HomeViewModel.
3.
Поведінка системної кнопки "Назад":
◦
Коли користувач знаходиться на HomeScreen.Details, NavDisplay через свій внутрішній обробник або параметр onBack = { viewModel.navigateBack() } перехоплює системний жест або кнопку "Назад" і скидає вершину внутрішнього стека.
◦
Коли ж користувач перебуває на кореневому HomeScreen.Main, внутрішній бекстек не має куди повертатися (size <= 1). Тоді подія "Назад" не перехоплюється внутрішнім рівнем і спливає до рівня Activity, викликаючи стандартне закриття/згортання застосунку.
6. VIEWMODEL: ЖИТТЄВИЙ ЦИКЛ ТА field
1.
Момент створення HomeViewModel:
◦
Створюється при вході  HomeNavDisplay у початкову композицію під час першого обчислення аргументу viewModel: HomeViewModel = viewModel().
2.
Момент знищення HomeViewModel:
◦
Оскільки  HomeNavDisplay не задавав окремого ViewModelStoreOwner, власником є сама MainActivity.
◦
HomeViewModel знищується лише тоді, коли знищується сама MainActivity назавжди (виклик finish() або змахування застосунку з панелі нещодавніх завдань), при цьому викликається onCleared().
3.
При перемиканні вкладок (Головна ↔ Налаштування):
◦
Composable-функція  HomeNavDisplay демонтується з UI-дерева, але об'єкт HomeViewModel спокійно продовжує жити у ViewModelStore активності.
4.
При повороті екрана:
◦
Activity перестворюється, але система передає існуючий ViewModelStore новому екземпляру Activity за допомогою механізму NonConfigurationInstances. ViewModel не перестворюється.
5.
Чому labelText та currentDestination не скидаються:
◦
labelText: зберігається тому, що HomeViewModel не знищується.
◦
currentDestination: зберігається через зовсім інший механізм — механізм серіалізації rememberSaveable, який записує стан у системний Bundle ОС Android.
6.
Explicit Backing Field (field) у Kotlin 2.x:
◦
Класичний підхід (_private): Раніше вимагалося писати дві окремі змінні:
private val _labelText = MutableStateFlow("...")
val labelText: StateFlow<String> = _labelText.asStateFlow()
Це призводило до засмічення класу дубльованими іменами (_xxx та xxx) і зайвого виклику обгортки .asStateFlow().
◦
Сучасний синтаксис Explicit Backing Field:
val labelText: StateFlow<String>
    field = MutableStateFlow("...")
◦
У чому перевага:
a.
Єдине ім'я: Властивість має лише одне публічне ім'я labelText.
b.
Інкапсуляція типів без дублювання: Ззовні класу тип бачиться як незмінний StateFlow<String>, а всередині класу компілятор знає справжній тип поля (MutableStateFlow<String>), що дозволяє писати labelText.value = newText.
c.
Чистота коду: Менше шаблонного коду (boilerplate) на 50% для кожної змінної стану.
=================
@Serializable
sealed interface HomeScreen : NavKey {
    @Serializable
    data object Main : HomeScreen

    @Serializable
    data object Details : HomeScreen
} що це і що це робить
-------------
Цей фрагмент коду — це оголошення типізованих (type-safe) навігаційних маршрутів для Jetpack Navigation 3. Він описує можливі екрани (пункти призначення) у вашому застосунку не як прості рядки, а як об'єкти Kotlin, що робить навігацію безпечнішою та зручнішою.

🧩 Що означає кожен елемент
@Serializable: Ця анотація з бібліотеки kotlinx.serialization є обов'язковою вимогою Navigation 3. Вона дозволяє системі «запам'ятовувати» стан навігації (наприклад, при повороті екрана чи відновленні після смерті процесу) шляхом серіалізації цих об'єктів.

sealed interface HomeScreen : NavKey: Це оголошення закритого інтерфейсу (sealed interface), який успадковується від маркерного інтерфейсу NavKey. У Navigation 3 кожен екран (маршрут) зобов'язаний реалізовувати NavKey. Вибір sealed interface є гарною практикою, оскільки він гарантує, що всі можливі екрани для цього розділу навігації будуть визначені в одному місці (у цьому файлі), і компілятор зможе перевірити повноту обробки.

data object Main : HomeScreen та data object Details : HomeScreen: Це конкретні реалізації (екземпляри) цього інтерфейсу. Ключове слово object означає, що існує лише один екземпляр цього екрана, а data додає автоматичні методи (equals, hashCode, toString), що корисно для порівняння та серіалізації. Використання data object для екранів без параметрів є стандартним і найефективнішим підходом.

⚙️ Як це працює в Navigation 3
У класичній Navigation 2 ви використовували рядкові маршрути, як-от composable("home_details"), і навігація виглядала як navController.navigate("home_details"). Це було схильне до помилок (легко зробити друкарську помилку в рядку, і компілятор цього не помітить).

Navigation 3 пропонує інший підхід. Замість NavController ви керуєте стеком (back stack), який є звичайним списком (List) цих самих об'єктів NavKey. Навігація виглядає так:

Перехід вперед: ви просто додаєте новий об'єкт до списку, наприклад, backStack.add(HomeScreen.Details).

Перехід назад: ви видаляєте останній елемент зі списку, наприклад, backStack.removeLastOrNull().

Оскільки HomeScreen — це sealed interface, компілятор точно знає всі можливі екрани. Це уможливлює вичерпну перевірку when під час рендерингу, що унеможливлює забуття обробки якогось екрана.

💎 Головні переваги такого підходу
Безпека типів (Type Safety): Замість крихких рядків ви використовуєте об'єкти Kotlin. Компілятор гарантує, що ви не зможете перейти на неіснуючий екран або передати неправильні аргументи.

Підтримка стану (State Management): Завдяки @Serializable Navigation 3 може автоматично зберігати та відновлювати стек навігації навіть після того, як система «вбила» ваш застосунок для звільнення пам'яті. Це забезпечується функцією rememberNavBackStack.

Гнучкість та контроль: У Navigation 3 ви «володієте» своїм back stack. Це робить навігацію більш передбачуваною та простою для розуміння, оскільки вона керується звичайним станом Compose, а не прихованою логікою всередині NavController.

Цей код є фундаментом для побудови сучасної, надійної та зручної навігації у вашому застосунку. Якщо у вас є запитання щодо того, як ці об'єкти використовуються для побудови всього навігаційного графу, питайте.

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

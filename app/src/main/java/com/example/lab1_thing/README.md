AI assisted











---------------
by rememberSaveable -- що це робить
User asks specifically what by rememberSaveable does. Need to explain delegation, rememberSaveable, and how they combine. In Ukrainian.

by rememberSaveable — детально
Розберемо окремо кожну частину: rememberSaveable, by і що вони роблять разом.

1. rememberSaveable { ... } — що це
   rememberSaveable — це composable-функція з Jetpack Compose, яка:

Створює значення один раз при першій композиції (викликає лямбду { ... }).

Запам'ятовує його між рекомпозиціями (перемальовками), щоб не створювати заново.

Автоматично зберігає його в savedInstanceState (Bundle) і відновлює після перестворення Activity/процесу.

Тобто це "пам'ять", яка переживає:

рекомпозиції (як звичайний remember);

поворот екрана, зміну мови, зміну теми;

відновлення процесу після того, як система вбила застосунок у фоні (якщо Bundle не втрачено).

Порівняння remember vs rememberSaveable
remember	rememberSaveable
Переживає рекомпозицію	✅	✅
Переживає поворот екрана	❌ (втрачається)	✅
Переживає смерть процесу	❌	✅ (якщо Bundle відновлено)
Що зберігає	будь-що	лише те, що можна покласти в Bundle
rememberSaveable під капотом використовує Saver — механізм, що перетворює значення у щось, що можна записати в Bundle. Для стандартних типів (Int, String, Boolean, Parcelable, Serializable, enum тощо) це працює "з коробки".

2. by — делегування властивості
   by — це ключове слово Kotlin для делегування властивості. Воно означає: "коли хтось читає або пише цю змінну — насправді звертайся до іншого об'єкта через його getValue/setValue".

Без by код виглядав би так:

kotlin
val currentDestination = rememberSaveable { mutableStateOf(AppDestinations.HOME) }
// читання:
currentDestination.value
// запис:
currentDestination.value = AppDestinations.FAVORITES
З by:

kotlin
var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
// читання:
currentDestination
// запис:
currentDestination = AppDestinations.FAVORITES
by — це просто синтаксичний цукор, який прибирає .value. Для MutableState Compose надає спеціальні оператори getValue/setValue (розширення State<T>.getValue і MutableState<T>.setValue), завдяки яким делегування працює.

⚠️ Зверніть увагу: by не є частиною rememberSaveable. by — це мовна конструкція Kotlin, а rememberSaveable — це функція Compose. Вони просто добре поєднуються.

3. Що відбувається разом
   kotlin
   var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
   Розкладемо на кроки:

mutableStateOf(AppDestinations.HOME) — створює MutableState<AppDestinations> зі стартовим значенням HOME. Це "реактивна комірка": коли її значення змінюється, Compose перемальовує все, що його читає.

rememberSaveable { ... } — каже Compose:

"Виклич цю лямбду лише один раз, при першій композиції."

"Запам'ятай результат (MutableState) між перемальовками."

"Збережи його в Bundle перед знищенням Activity і віднови після перестворення."

by — дозволяє працювати з MutableState як зі звичайною змінною: currentDestination замість currentDestination.value.

Потік життя значення
text
Перша композиція
↓
rememberSaveable викликає лямбду → створює MutableState(HOME)
↓
currentDestination = HOME, UI малює HOME як selected
↓
Користувач тапає FAVORITES
↓
currentDestination = FAVORITES  (через by → MutableState.value = ...)
↓
Compose бачить зміну → рекомпозиція → UI підсвічує FAVORITES
↓
rememberSaveable реєструє MutableState у SaveableStateRegistry
↓
(поворот екрана → Activity знищується → Bundle містить "FAVORITES")
↓
Нова Activity → rememberSaveable читає Bundle → відновлює MutableState(FAVORITES)
↓
UI одразу показує FAVORITES як selected
4. Чому не можна просто var currentDestination = AppDestinations.HOME?
   Якби ви написали:

kotlin
var currentDestination = AppDestinations.HOME  // ❌ без remember
То при кожній рекомпозиції (перемальовці) функція Lab1thingApp() виконувалась би заново, і currentDestination скидалась би назад до HOME при будь-якій зміні іншого стану. UI ніколи б не "запам'ятав" вибір.

А якби ви написали:

kotlin
var currentDestination by remember { mutableStateOf(AppDestinations.HOME) }  // ✅ але без save
То вибір пережив би рекомпозиції, але втратився б при повороті екрана. rememberSaveable вирішує і це.

5. Обмеження rememberSaveable
   Значення має бути серіалізовним у Bundle: Int, Long, String, Boolean, Float, Double, масиви примітивів, Parcelable, Serializable, enum (через Serializable/Parcelable), List/Map з такими елементами тощо.

Для кастомних типів треба передати власний Saver:

kotlin
val myState by rememberSaveable(
stateSaver = mapSaver(
save = { mapOf("x" to it.x, "y" to it.y) },
restore = { MyData(it["x"] as Int, it["y"] as Int) }
)
) { mutableStateOf(MyData(0, 0)) }
Для складних об'єктів (бітмапи, великі списки) краще використовувати ViewModel + SavedStateHandle, а не rememberSaveable.

Підсумок одним реченням
by rememberSaveable { mutableStateOf(...) } створює реактивну змінну-стан, яку Compose:

не перестворює при рекомпозиціях (remember),

автоматично зберігає у Bundle і відновлює після перестворення Activity (Saveable),

дозволяє читати/писати як звичайну змінну завдяки делегуванню (by).



-----
selected = it == currentDestination, навіщо цей рядок
User asks about selected = it == currentDestination in the NavigationSuiteScaffold item. Need to explain purpose.

Розбір selected = it == currentDestination
kotlin
item(
...
selected = it == currentDestination,
...
)
Цей рядок визначає, чи цей конкретний пункт меню зараз вибраний (активний), і передає це рішення компоненту item. Від цього залежить, як пункт виглядає (підсвічений/ні).

Розберемо по частинах.

1. Що таке it
   it — це неявна назва параметра лямбди в Kotlin. У нашому коді:

kotlin
AppDestinations.entries.forEach {
item(
icon = { Icon(painterResource(it.icon), ...) },
label = { Text(it.label) },
selected = it == currentDestination,
onClick = { currentDestination = it }
)
}
forEach { ... } передає в лямбду один аргумент — поточний елемент списку (один із AppDestinations). Оскільки лямбда має лише один параметр і ми його не назвали явно, Kotlin автоматично називає його it.

Тобто it — це один пункт меню (наприклад, AppDestinations.HOME, AppDestinations.FAVORITES, ...). На кожній ітерації it — різний.

Можна було б написати явно:

kotlin
AppDestinations.entries.forEach { destination ->
item(
...
selected = destination == currentDestination,
onClick = { currentDestination = destination }
)
}
Це те саме, але читабельніше. it — це просто "коротка версія".

2. Що таке currentDestination
   kotlin
   var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
   currentDestination — це поточний вибраний пункт (стан). Стартово — HOME. Коли користувач тапає пункт, onClick змінює currentDestination на новий.

Тобто:

currentDestination = "що вибрав користувач" (стан).

it = "пункт, який ми зараз малюємо" (з циклу).

3. Що робить it == currentDestination
   Це порівняння на рівність:

якщо it (пункт, що малюється) дорівнює currentDestination (поточному вибору) → true;

інакше → false.

Оскільки AppDestinations — це enum, == порівнює ідентичність значення enum (кожне значення enum — єдиний екземпляр у пам'яті). Тобто AppDestinations.HOME == AppDestinations.HOME → true, а AppDestinations.HOME == AppDestinations.PROFILE → false.

Результат: для одного пункту (того, що зараз вибраний) буде true, для всіх інших — false.

4. Навіщо це передавати в item(selected = ...)
   item(...) з NavigationSuiteScaffold має параметр selected: Boolean. Він каже компоненту: "цей пункт зараз активний чи ні".

Що робить item із цим значенням:

selected	Як виглядає пункт
true	Підсвічений (інший колір іконки/тексту, індикатор-пігулка навколо, жирніший шрифт — залежно від теми Material 3)
false	Звичайний (нейтральний колір, без індикатора)
Тобто selected керує візуальним станом пункту: активний/неактивний.

Без цього рядка (або якщо завжди false):

усі пункти виглядали б однаково;

користувач не бачив би, на якому екрані він зараз;

меню виглядало б "зламаним" — наче нічого не вибрано.

Якщо завжди true:

усі пункти були б підсвічені одночасно — теж неправильно.

Тому selected = it == currentDestination — це "підсвітити рівно один пункт — той, що зараз вибраний".


...

-----

